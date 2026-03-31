from __future__ import annotations

import json
import os
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


def _default_app_data_dir() -> Path:
    # Windows desktop conventions:
    # - Prefer %APPDATA% (Roaming) for user-scoped app data.
    appdata = os.environ.get("APPDATA")
    if appdata:
        return Path(appdata) / "DocFormatAI"
    # Fallback: home directory
    return Path.home() / ".docformatai"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="DOCFORMAT_", extra="ignore")

    data_dir: Path = _default_app_data_dir()
    port: int = 17860

    # Retention policy (MVP defaults, later configurable)
    retain_max_versions: int = 20
    retain_max_age_days: int = 7

    # LLM provider (OpenAI-compatible by default)
    llm_provider: str = "openai_compatible"
    llm_base_url: str | None = None
    llm_api_key: str | None = None
    llm_model: str = "gpt-4.1-mini"


settings = Settings()


def _load_external_llm_config() -> dict[str, str]:
    appdata = os.environ.get("APPDATA")
    candidates: list[Path] = []
    if appdata:
        candidates.append(Path(appdata) / "DocFormatAI" / "app-config.json")
        candidates.append(Path(appdata) / "DocFormat AI" / "app-config.json")
    # Optional local fallback for debugging
    candidates.append(Path.cwd() / "app-config.json")

    for p in candidates:
        try:
            if not p.exists():
                continue
            data = json.loads(p.read_text(encoding="utf-8-sig"))
            if isinstance(data, dict):
                return {str(k): str(v) for k, v in data.items() if v is not None}
        except Exception:
            continue
    return {}


_external_cfg = _load_external_llm_config()
if not settings.llm_api_key and _external_cfg.get("llm_api_key"):
    settings.llm_api_key = _external_cfg["llm_api_key"]
if not settings.llm_base_url and _external_cfg.get("llm_base_url"):
    settings.llm_base_url = _external_cfg["llm_base_url"]
if settings.llm_provider == "openai_compatible" and _external_cfg.get("llm_provider"):
    settings.llm_provider = _external_cfg["llm_provider"]
if settings.llm_model == "gpt-4.1-mini" and _external_cfg.get("llm_model"):
    settings.llm_model = _external_cfg["llm_model"]

