from __future__ import annotations

import shutil
import subprocess
from pathlib import Path


def convert_doc_to_docx_bytes(*, source_doc: Path, work_dir: Path) -> bytes:
    """
    Convert legacy .doc to .docx via LibreOffice headless mode.
    Raises RuntimeError when conversion tool is unavailable or conversion fails.
    """
    work_dir.mkdir(parents=True, exist_ok=True)
    out_dir = work_dir / "converted"
    out_dir.mkdir(parents=True, exist_ok=True)

    cmd = _resolve_libreoffice_command()
    if cmd is None:
        raise RuntimeError(
            "未检测到 LibreOffice。请安装 LibreOffice 并确保 soffice 可执行文件可用。"
        )

    process = subprocess.run(
        [cmd, "--headless", "--convert-to", "docx", "--outdir", str(out_dir), str(source_doc)],
        capture_output=True,
        text=True,
        timeout=180,
    )
    if process.returncode != 0:
        raise RuntimeError(
            f".doc 转换失败（LibreOffice 返回码 {process.returncode}）：{(process.stderr or process.stdout).strip()}"
        )

    out_file = out_dir / f"{source_doc.stem}.docx"
    if not out_file.exists():
        # LibreOffice occasionally normalizes file names; fallback: pick latest .docx
        candidates = sorted(out_dir.glob("*.docx"), key=lambda p: p.stat().st_mtime, reverse=True)
        if not candidates:
            raise RuntimeError("转换失败：未生成 .docx 文件。")
        out_file = candidates[0]

    return out_file.read_bytes()


def _resolve_libreoffice_command() -> str | None:
    # 1) PATH
    for name in ("soffice", "soffice.exe", "libreoffice", "libreoffice.exe"):
        if shutil.which(name):
            return name
    # 2) Common Windows install paths
    candidates = [
        Path("C:/Program Files/LibreOffice/program/soffice.exe"),
        Path("C:/Program Files (x86)/LibreOffice/program/soffice.exe"),
    ]
    for p in candidates:
        if p.exists():
            return str(p)
    return None

