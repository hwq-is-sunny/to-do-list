from __future__ import annotations

import json
import os
import shutil
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from .settings import settings


UTC = timezone.utc


def _now() -> datetime:
    return datetime.now(tz=UTC)


def _safe_mkdir(p: Path) -> None:
    p.mkdir(parents=True, exist_ok=True)


def _read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


@dataclass(frozen=True)
class TaskPaths:
    task_dir: Path
    input_dir: Path
    versions_dir: Path
    tmp_dir: Path
    meta_json: Path
    history_json: Path


def get_task_paths(task_id: str) -> TaskPaths:
    base = settings.data_dir
    task_dir = base / "tasks" / task_id
    return TaskPaths(
        task_dir=task_dir,
        input_dir=task_dir / "input",
        versions_dir=task_dir / "versions",
        tmp_dir=task_dir / "tmp",
        meta_json=task_dir / "meta.json",
        history_json=task_dir / "history.json",
    )


def create_task() -> str:
    task_id = uuid.uuid4().hex
    tp = get_task_paths(task_id)
    _safe_mkdir(tp.input_dir)
    _safe_mkdir(tp.versions_dir)
    _safe_mkdir(tp.tmp_dir)
    _write_json(
        tp.meta_json,
        {
            "task_id": task_id,
            "created_at": _now().isoformat(),
            "active_version": 0,
            "last_exported_version": 0,
        },
    )
    _write_json(tp.history_json, {"entries": []})
    return task_id


def save_input_docx(task_id: str, filename: str, content: bytes) -> Path:
    tp = get_task_paths(task_id)
    _safe_mkdir(tp.input_dir)
    # Keep original name for UI, but store as fixed file
    input_path = tp.input_dir / "original.docx"
    input_path.write_bytes(content)
    meta = _read_json(tp.meta_json)
    meta["original_filename"] = filename
    meta["updated_at"] = _now().isoformat()
    _write_json(tp.meta_json, meta)
    # Also seed version v0 as "original"
    v0 = tp.versions_dir / "v0.docx"
    if not v0.exists():
        shutil.copyfile(input_path, v0)
    return input_path


def get_meta(task_id: str) -> dict[str, Any]:
    tp = get_task_paths(task_id)
    return _read_json(tp.meta_json)


def append_history(task_id: str, entry: dict[str, Any]) -> None:
    tp = get_task_paths(task_id)
    hist = _read_json(tp.history_json)
    hist.setdefault("entries", []).append(entry)
    _write_json(tp.history_json, hist)


def next_version_path(task_id: str, version: int) -> Path:
    tp = get_task_paths(task_id)
    _safe_mkdir(tp.versions_dir)
    return tp.versions_dir / f"v{version}.docx"


def get_active_version_path(task_id: str) -> Path:
    meta = get_meta(task_id)
    v = int(meta.get("active_version", 0))
    return next_version_path(task_id, v)


def set_active_version(task_id: str, version: int) -> None:
    tp = get_task_paths(task_id)
    meta = _read_json(tp.meta_json)
    meta["active_version"] = int(version)
    meta["updated_at"] = _now().isoformat()
    _write_json(tp.meta_json, meta)


def set_last_exported_version(task_id: str, version: int) -> None:
    tp = get_task_paths(task_id)
    meta = _read_json(tp.meta_json)
    meta["last_exported_version"] = int(version)
    meta["updated_at"] = _now().isoformat()
    _write_json(tp.meta_json, meta)


def cleanup_task_files(task_id: str) -> None:
    """
    Best-effort cleanup. Never block main flow.
    Rules:
      - keep newest N versions
      - delete versions older than max_age_days (except active + last_exported)
      - delete tmp older than max_age_days
    """
    try:
        tp = get_task_paths(task_id)
        if not tp.task_dir.exists():
            return

        meta = _read_json(tp.meta_json)
        active_v = int(meta.get("active_version", 0))
        last_exported_v = int(meta.get("last_exported_version", active_v))
        protected = {active_v, last_exported_v, 0}

        cutoff = _now() - timedelta(days=settings.retain_max_age_days)

        # Versions cleanup
        versions = []
        if tp.versions_dir.exists():
            for p in tp.versions_dir.glob("v*.docx"):
                try:
                    v = int(p.stem[1:])
                except Exception:
                    continue
                versions.append((v, p))
        versions.sort(key=lambda x: x[0])

        # Keep newest N versions
        keep_by_count = {v for v, _ in versions[-settings.retain_max_versions :]}
        keep = keep_by_count | protected

        for v, p in versions:
            if v in keep:
                continue
            try:
                mtime = datetime.fromtimestamp(p.stat().st_mtime, tz=UTC)
                if mtime < cutoff or v not in keep_by_count:
                    p.unlink(missing_ok=True)
            except Exception:
                continue

        # Tmp cleanup
        if tp.tmp_dir.exists():
            for p in tp.tmp_dir.glob("**/*"):
                if p.is_dir():
                    continue
                try:
                    mtime = datetime.fromtimestamp(p.stat().st_mtime, tz=UTC)
                    if mtime < cutoff:
                        p.unlink(missing_ok=True)
                except Exception:
                    continue
    except Exception:
        # Never fail user operations because cleanup failed.
        return


def get_user_friendly_output_name(original_filename: str | None, version: int) -> str:
    base = "document"
    if original_filename:
        base = Path(original_filename).stem
    return f"{base}_格式修改版_v{version}.docx"

