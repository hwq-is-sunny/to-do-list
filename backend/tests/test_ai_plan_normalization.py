from __future__ import annotations

from app.main import _coerce_plan, _normalize_plan_payload


def test_normalize_plan_payload_handles_dict_change_and_alias_values() -> None:
    out = _normalize_plan_payload(
        {
            "changes": {
                "scope": "normal",
                "paragraph": {"line_spacing": 1.5},
                "assumptions": "自动修正字段类型",
            }
        }
    )
    assert out["version"] == "1.0"
    assert isinstance(out["changes"], list)
    assert out["changes"][0]["scope"] == "body"
    assert out["changes"][0]["paragraph"]["line_spacing"] == "1.5"
    assert out["changes"][0]["assumptions"] == ["自动修正字段类型"]


def test_normalize_plan_payload_falls_back_to_safe_empty_plan() -> None:
    out = _normalize_plan_payload("not-a-json-object")
    assert out == {"version": "1.0", "changes": []}


def test_coerce_plan_accepts_raw_variants_used_by_preview_apply() -> None:
    plan = _coerce_plan(
        {
            "changes": [
                {
                    "scope": "body",
                    "paragraph": {"line_spacing": 1.5},
                    "assumptions": "小四对应12pt",
                }
            ]
        }
    )
    assert plan.version == "1.0"
    assert plan.changes[0].paragraph is not None
    assert plan.changes[0].paragraph.line_spacing == "1.5"
    assert plan.changes[0].assumptions == ["小四对应12pt"]
