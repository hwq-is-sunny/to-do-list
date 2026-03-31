from __future__ import annotations

import json
from abc import ABC, abstractmethod
from typing import Any

import httpx

from ..settings import settings


class LLMProvider(ABC):
    @abstractmethod
    async def plan(self, *, user_text: str, doc_summary_json: dict[str, Any]) -> dict[str, Any]:
        raise NotImplementedError


class OpenAICompatibleProvider(LLMProvider):
    """
    Minimal OpenAI-compatible client for:
      POST {base_url}/chat/completions
    Works with OpenAI, Azure OpenAI (with compatible gateway), and other vendors
    exposing an OpenAI-like API.
    """

    def __init__(self) -> None:
        self.base_url = (settings.llm_base_url or "https://api.openai.com/v1").rstrip("/")
        self.api_key = settings.llm_api_key
        self.model = settings.llm_model

    async def plan(self, *, user_text: str, doc_summary_json: dict[str, Any]) -> dict[str, Any]:
        if not self.api_key:
            raise RuntimeError("LLM API key is not configured (DOCFORMAT_LLM_API_KEY).")

        system = (
            "You are a document formatting planner. "
            "Return ONLY valid JSON that matches the provided schema intent: "
            "{version:'1.0', changes:[{scope, selector?, font?, paragraph?, assumptions?}]}. "
            "Do not include markdown, code fences, or extra text."
        )

        # Keep privacy: no full body text; only summary stats.
        user_payload = {
            "user_text": user_text,
            "doc_summary": doc_summary_json,
            "defaults": {
                "font_ascii_default": "Times New Roman",
                "split_east_asia_and_ascii": True,
                "supported_scopes": [
                    "body",
                    "heading_1",
                    "heading_2",
                    "heading_3",
                    "tables_all",
                    "tables_header_only",
                    "references",
                ],
            },
        }

        body = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": json.dumps(user_payload, ensure_ascii=False)},
            ],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        }

        headers = {"Authorization": f"Bearer {self.api_key}"}
        url = f"{self.base_url}/chat/completions"

        async with httpx.AsyncClient(timeout=60) as client:
            r = await client.post(url, headers=headers, json=body)
            r.raise_for_status()
            data = r.json()

        content = data["choices"][0]["message"]["content"]
        return json.loads(content)


def get_provider() -> LLMProvider:
    name = (settings.llm_provider or "").lower()
    if name in {"openai_compatible", "openai", "azure_openai"}:
        return OpenAICompatibleProvider()
    raise ValueError(f"Unsupported LLM provider: {settings.llm_provider}")

