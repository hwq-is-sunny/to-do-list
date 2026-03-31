from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field, field_validator


class UploadResponse(BaseModel):
    task_id: str


class AnalyzeRequest(BaseModel):
    task_id: str


class StyleStat(BaseModel):
    style_name: str
    paragraph_count: int = 0
    sample_font_east_asia: str | None = None
    sample_font_ascii: str | None = None
    sample_font_size_pt: float | None = None
    bold_ratio: float | None = None
    center_ratio: float | None = None


class HeadingCandidate(BaseModel):
    paragraph_id: str
    style_name: str | None = None
    inferred_level: int = Field(ge=1, le=9)
    confidence: float = Field(ge=0, le=1)


class DocumentModelSummary(BaseModel):
    task_id: str
    created_at: datetime
    page_estimate: int | None = None
    has_headers: bool = False
    has_footers: bool = False
    has_sections: bool = False
    paragraph_count: int = 0
    table_count: int = 0

    style_stats: list[StyleStat] = Field(default_factory=list)
    heading_candidates: list[HeadingCandidate] = Field(default_factory=list)
    supported_style_name_map: dict[str, str] = Field(
        default_factory=lambda: {
            # CN
            "标题 1": "heading_1",
            "标题 2": "heading_2",
            "标题 3": "heading_3",
            "正文": "normal",
            "参考文献": "references",
            # EN
            "Heading 1": "heading_1",
            "Heading 2": "heading_2",
            "Heading 3": "heading_3",
            "Normal": "normal",
            "References": "references",
        }
    )


ScopeType = Literal[
    "body",
    "heading_1",
    "heading_2",
    "heading_3",
    "tables_all",
    "tables_header_only",
    "references",
]


class SelectorRule(BaseModel):
    # Style-name first, with heuristics fallback
    style_names_any_of: list[str] | None = None
    min_font_size_pt: float | None = None
    bold: bool | None = None
    centered: bool | None = None
    numbered_like_heading: bool | None = None
    # References-specific optional controls
    reference_max_scan_paragraphs: int | None = Field(default=None, ge=5, le=200)
    reference_allow_blank_lines: bool | None = None


class FontPatch(BaseModel):
    east_asia: str | None = None
    ascii: str | None = None
    size_pt: float | None = None
    bold: bool | None = None


class ParagraphPatch(BaseModel):
    alignment: Literal["left", "center", "right", "justify"] | None = None
    line_spacing: Literal["single", "1.5", "double"] | None = None
    space_before_pt: float | None = None
    space_after_pt: float | None = None
    first_line_indent_chars: float | None = None

    @field_validator("line_spacing", mode="before")
    @classmethod
    def normalize_line_spacing(cls, value: Any):
        if value is None:
            return value
        if isinstance(value, (int, float)):
            if abs(float(value) - 1.0) < 1e-6:
                return "single"
            if abs(float(value) - 1.5) < 1e-6:
                return "1.5"
            if abs(float(value) - 2.0) < 1e-6:
                return "double"
        if isinstance(value, str):
            v = value.strip().lower()
            if v in {"single", "1", "1.0", "单倍", "单倍行距"}:
                return "single"
            if v in {"1.5", "1.5x", "1.5倍", "1.5倍行距"}:
                return "1.5"
            if v in {"double", "2", "2.0", "两倍", "双倍", "双倍行距"}:
                return "double"
        return value


class Change(BaseModel):
    scope: ScopeType
    selector: SelectorRule | None = None
    font: FontPatch | None = None
    paragraph: ParagraphPatch | None = None
    assumptions: list[str] = Field(default_factory=list)

    @field_validator("scope", mode="before")
    @classmethod
    def normalize_scope(cls, value: Any):
        if not isinstance(value, str):
            return value
        v = value.strip().lower()
        scope_aliases = {
            "normal": "body",
            "正文": "body",
            "body": "body",
            "heading1": "heading_1",
            "heading_1": "heading_1",
            "heading 1": "heading_1",
            "标题1": "heading_1",
            "标题 1": "heading_1",
            "heading2": "heading_2",
            "heading_2": "heading_2",
            "heading 2": "heading_2",
            "标题2": "heading_2",
            "标题 2": "heading_2",
            "heading3": "heading_3",
            "heading_3": "heading_3",
            "heading 3": "heading_3",
            "标题3": "heading_3",
            "标题 3": "heading_3",
            "tables_all": "tables_all",
            "table_all": "tables_all",
            "all_tables": "tables_all",
            "tables_header_only": "tables_header_only",
            "table_header_only": "tables_header_only",
            "references": "references",
            "reference": "references",
            "参考文献": "references",
        }
        return scope_aliases.get(v, value)

    @field_validator("assumptions", mode="before")
    @classmethod
    def normalize_assumptions(cls, value: Any):
        if value is None:
            return []
        if isinstance(value, str):
            t = value.strip()
            return [t] if t else []
        if isinstance(value, list):
            return [str(v) for v in value if str(v).strip()]
        return [str(value)]


class Plan(BaseModel):
    version: Literal["1.0"] = "1.0"
    changes: list[Change]


class AIPlanRequest(BaseModel):
    task_id: str
    user_text: str
    # Minimal context only (no full text)
    doc_summary: DocumentModelSummary


class PreviewRequest(BaseModel):
    task_id: str
    plan: dict[str, Any] | Plan


class PreviewReport(BaseModel):
    task_id: str
    impact: dict[str, int]
    diff_summary: list[str]
    per_change_summaries: list[str]
    integrity_warnings: list[str] = Field(default_factory=list)
    risk_hints: list[str] = Field(default_factory=list)


class ApplyRequest(BaseModel):
    task_id: str
    plan: dict[str, Any] | Plan


class ApplyResult(BaseModel):
    task_id: str
    new_version: int
    output_filename: str
    history_entry: dict[str, Any]
    integrity_warnings: list[str] = Field(default_factory=list)

