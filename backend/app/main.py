from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
import traceback

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.responses import FileResponse

from .doc_analyzer import analyze_docx
from .doc_convert import convert_doc_to_docx_bytes
from .doc_apply import apply_plan_to_docx, estimate_plan_impact, inspect_docx_integrity
from .llm.provider import get_provider
from .schemas import (
    AIPlanRequest,
    AnalyzeRequest,
    ApplyRequest,
    ApplyResult,
    DocumentModelSummary,
    Plan,
    PreviewReport,
    PreviewRequest,
    UploadResponse,
)
from .settings import settings
from .storage import (
    append_history,
    cleanup_task_files,
    create_task,
    get_active_version_path,
    get_meta,
    get_task_paths,
    get_user_friendly_output_name,
    next_version_path,
    save_input_docx,
    set_active_version,
    set_last_exported_version,
)


UTC = timezone.utc

app = FastAPI(title="DocFormat AI Backend", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _normalize_plan_payload(raw_plan: object) -> dict:
    """
    Convert LLM output into a schema-valid plan whenever possible.
    If the payload is badly malformed, fall back to a safe empty plan.
    """
    if isinstance(raw_plan, dict):
        payload = dict(raw_plan)
    else:
        payload = {}

    if "version" not in payload:
        payload["version"] = "1.0"

    changes = payload.get("changes")
    if changes is None:
        payload["changes"] = []
    elif isinstance(changes, dict):
        payload["changes"] = [changes]
    elif not isinstance(changes, list):
        payload["changes"] = []

    try:
        return Plan.model_validate(payload).model_dump(mode="json")
    except Exception:
        # Try to salvage valid changes one-by-one; ignore invalid ones.
        raw_changes = payload.get("changes")
        salvaged_changes: list[dict] = []
        if isinstance(raw_changes, list):
            for item in raw_changes:
                try:
                    single = Plan.model_validate({"version": "1.0", "changes": [item]})
                    salvaged_changes.append(single.changes[0].model_dump(mode="json"))
                except Exception:
                    continue
        return {"version": "1.0", "changes": salvaged_changes}


def _coerce_plan(raw_plan: object) -> Plan:
    """
    Normalize and coerce arbitrary plan payload into a valid Plan model.
    """
    if isinstance(raw_plan, Plan):
        return raw_plan
    normalized = _normalize_plan_payload(raw_plan)
    return Plan.model_validate(normalized)


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    # Keep backend alive and return readable error payload instead of opaque 500.
    # Traceback stays in server logs for debugging.
    traceback.print_exc()
    return JSONResponse(
        status_code=500,
        content={
            "detail": {
                "code": "INTERNAL_ERROR",
                "message": f"服务内部异常: {exc}",
                "path": str(request.url.path),
            }
        },
    )


@app.get("/api/health")
def health():
    return {"ok": True, "time": datetime.now(tz=UTC).isoformat()}


@app.post("/api/doc/upload", response_model=UploadResponse)
async def upload_doc(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="缺少文件名。")
    lower_name = file.filename.lower()
    if not (lower_name.endswith(".docx") or lower_name.endswith(".doc")):
        raise HTTPException(status_code=400, detail="仅支持 .docx 或 .doc 文件。")

    task_id = create_task()
    content = await file.read()
    if lower_name.endswith(".docx"):
        save_input_docx(task_id, file.filename, content)
    else:
        tp = get_task_paths(task_id)
        source_doc = tp.tmp_dir / "upload_source.doc"
        source_doc.write_bytes(content)
        try:
            converted = convert_doc_to_docx_bytes(source_doc=source_doc, work_dir=tp.tmp_dir)
        except RuntimeError as exc:
            msg = str(exc)
            if "LibreOffice" in msg or "soffice" in msg:
                raise HTTPException(
                    status_code=400,
                    detail={
                        "code": "LIBREOFFICE_MISSING",
                        "message": msg,
                    },
                ) from exc
            raise HTTPException(
                status_code=400,
                detail={
                    "code": "DOC_CONVERT_FAILED",
                    "message": msg,
                },
            ) from exc
        # Keep original filename for UI/history, but store converted docx for processing pipeline.
        save_input_docx(task_id, file.filename, converted)

    cleanup_task_files(task_id)
    return UploadResponse(task_id=task_id)


@app.post("/api/doc/analyze", response_model=DocumentModelSummary)
async def analyze(req: AnalyzeRequest):
    tp = get_task_paths(req.task_id)
    v0 = tp.versions_dir / "v0.docx"
    if not v0.exists():
        raise HTTPException(status_code=404, detail="Task not found or no document uploaded.")
    summary = analyze_docx(req.task_id, v0)
    cleanup_task_files(req.task_id)
    return summary


@app.post("/api/ai/plan")
async def ai_plan(req: AIPlanRequest):
    try:
        provider = get_provider()
        plan_json = await provider.plan(
            user_text=req.user_text,
            doc_summary_json=req.doc_summary.model_dump(mode="json"),
        )
        return _normalize_plan_payload(plan_json)
    except RuntimeError as exc:
        msg = str(exc)
        code = "LLM_ERROR"
        if "API key is not configured" in msg:
            code = "LLM_CONFIG_MISSING"
        raise HTTPException(
            status_code=400,
            detail={
                "code": code,
                "message": msg,
            },
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=502,
            detail={
                "code": "LLM_PROVIDER_ERROR",
                "message": f"LLM 服务调用失败: {exc}",
            },
        ) from exc


@app.post("/api/doc/preview", response_model=PreviewReport)
async def preview(req: PreviewRequest):
    # MVP: compute impact estimates without mutating file
    impact: dict[str, int] = {}
    diff_summary: list[str] = []
    per_change: list[str] = []
    risk_hints: list[str] = []
    plan = _coerce_plan(req.plan)

    v_active = get_active_version_path(req.task_id)
    if not v_active.exists():
        raise HTTPException(status_code=404, detail="Task not found.")
    impact = estimate_plan_impact(v_active, plan)
    integrity_warnings = inspect_docx_integrity(v_active)

    for ch in plan.changes:
        if ch.scope == "tables_header_only":
            risk_hints.append("表头识别使用规则推断，建议预览后确认")
        if ch.scope == "body":
            risk_hints.append("正文修改会同时影响页眉页脚中的普通文本")
        if not ch.scope.startswith("tables"):
            risk_hints.append("本次将同步更新样式定义（styles.xml）")
        if ch.scope == "references":
            risk_hints.append("参考文献区域采用规则识别（标题/样式/条目特征）")

        if ch.font:
            if ch.font.east_asia:
                diff_summary.append(f"中文字体 -> {ch.font.east_asia}")
            if ch.font.ascii:
                diff_summary.append(f"英文字体 -> {ch.font.ascii}")
            if ch.font.size_pt:
                diff_summary.append(f"字号(pt) -> {ch.font.size_pt}")
            if ch.font.bold is not None:
                diff_summary.append(f"加粗 -> {ch.font.bold}")
        if ch.paragraph:
            if ch.paragraph.alignment:
                diff_summary.append(f"对齐 -> {ch.paragraph.alignment}")
            if ch.paragraph.line_spacing:
                diff_summary.append(f"行距 -> {ch.paragraph.line_spacing}")
            if ch.paragraph.space_before_pt is not None or ch.paragraph.space_after_pt is not None:
                diff_summary.append("段前/段后 -> 已设置")
            if ch.paragraph.first_line_indent_chars is not None:
                diff_summary.append(f"首行缩进(字符) -> {ch.paragraph.first_line_indent_chars}")

        per_change.append(f"{ch.scope}: 将应用 {len([x for x in [ch.font, ch.paragraph] if x is not None])} 类属性修改")

    cleanup_task_files(req.task_id)
    return PreviewReport(
        task_id=req.task_id,
        impact=impact,
        diff_summary=list(dict.fromkeys(diff_summary)),
        per_change_summaries=per_change,
        integrity_warnings=integrity_warnings,
        risk_hints=list(dict.fromkeys(risk_hints)),
    )


@app.post("/api/doc/apply", response_model=ApplyResult)
async def apply(req: ApplyRequest):
    plan = _coerce_plan(req.plan)
    meta = get_meta(req.task_id)
    active_v = int(meta.get("active_version", 0))
    new_v = active_v + 1

    src = get_active_version_path(req.task_id)
    if not src.exists():
        raise HTTPException(status_code=404, detail="Task not found.")

    dst = next_version_path(req.task_id, new_v)
    apply_plan_to_docx(src_docx=src, dst_docx=dst, plan=plan)
    integrity_warnings = inspect_docx_integrity(dst)

    set_active_version(req.task_id, new_v)
    set_last_exported_version(req.task_id, new_v)

    output_name = get_user_friendly_output_name(meta.get("original_filename"), new_v)
    entry = {
        "time": datetime.now(tz=UTC).isoformat(),
        "version": new_v,
        "plan": plan.model_dump(mode="json"),
        "output_filename": output_name,
    }
    append_history(req.task_id, entry)

    cleanup_task_files(req.task_id)
    return ApplyResult(
        task_id=req.task_id,
        new_version=new_v,
        output_filename=output_name,
        history_entry=entry,
        integrity_warnings=integrity_warnings,
    )


@app.get("/api/doc/download")
async def download(task_id: str, version: int):
    p = next_version_path(task_id, version)
    if not p.exists():
        raise HTTPException(status_code=404, detail="Version not found.")
    meta = get_meta(task_id)
    output_name = get_user_friendly_output_name(meta.get("original_filename"), version)
    cleanup_task_files(task_id)
    return FileResponse(path=str(p), filename=output_name, media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document")


@app.on_event("startup")
def on_startup():
    # Ensure data dir exists in user AppData (not install dir)
    Path(settings.data_dir).mkdir(parents=True, exist_ok=True)

