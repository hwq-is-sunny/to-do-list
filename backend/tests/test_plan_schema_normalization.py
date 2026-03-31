from __future__ import annotations

from app.schemas import Plan


def test_plan_normalizes_common_llm_output_variants() -> None:
    plan = Plan.model_validate(
        {
            "version": "1.0",
            "changes": [
                {
                    "scope": "normal",
                    "paragraph": {"line_spacing": 1.5},
                    "assumptions": "用户要求将正文改为楷体、小四、1.5倍行距。",
                }
            ],
        }
    )

    ch = plan.changes[0]
    assert ch.scope == "body"
    assert ch.paragraph is not None
    assert ch.paragraph.line_spacing == "1.5"
    assert ch.assumptions == ["用户要求将正文改为楷体、小四、1.5倍行距。"]


def test_plan_normalizes_reference_scope_and_double_spacing() -> None:
    plan = Plan.model_validate(
        {
            "version": "1.0",
            "changes": [
                {
                    "scope": "参考文献",
                    "paragraph": {"line_spacing": 2},
                    "assumptions": None,
                }
            ],
        }
    )

    ch = plan.changes[0]
    assert ch.scope == "references"
    assert ch.paragraph is not None
    assert ch.paragraph.line_spacing == "double"
    assert ch.assumptions == []
