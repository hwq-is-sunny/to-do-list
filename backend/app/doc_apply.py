from __future__ import annotations

from io import BytesIO
from pathlib import Path
import re
from zipfile import ZIP_DEFLATED, ZipFile

from lxml import etree

from .schemas import Change, Plan

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NSMAP = {"w": W_NS}
W = f"{{{W_NS}}}"


def _w(tag: str) -> str:
    return f"{W}{tag}"


def _apply_font_defaults(plan: Plan) -> Plan:
    for ch in plan.changes:
        if ch.font and ch.font.east_asia and not ch.font.ascii:
            ch.font.ascii = "Times New Roman"
    return plan


def apply_plan_to_docx(*, src_docx: Path, dst_docx: Path, plan: Plan) -> None:
    """
    OpenXML patch-based writer (MVP v1).
    - Reads .docx package entries
    - Applies XML-level updates on word/document.xml
    - Preserves untouched entries byte-for-byte
    """
    plan = _apply_font_defaults(plan)

    with ZipFile(src_docx, "r") as zin:
        entries = {name: zin.read(name) for name in zin.namelist()}

    styles_xml = entries.get("word/styles.xml")
    style_name_to_id, style_id_to_style = _read_styles(styles_xml)
    heading_num_ids = _read_heading_numbering_ids(entries.get("word/numbering.xml"))
    doc_xml = entries.get("word/document.xml")
    if not doc_xml:
        raise ValueError("Invalid .docx: missing word/document.xml")

    root = etree.fromstring(doc_xml)
    body = root.find("w:body", namespaces=NSMAP)
    if body is None:
        raise ValueError("Invalid .docx: missing w:body")

    # Apply to document body and related story parts (header/footer),
    # so global formatting commands remain consistent across formal templates.
    header_footer_roots: dict[str, etree._Element] = {}
    for name, data in entries.items():
        if name.startswith("word/header") and name.endswith(".xml"):
            header_footer_roots[name] = etree.fromstring(data)
        if name.startswith("word/footer") and name.endswith(".xml"):
            header_footer_roots[name] = etree.fromstring(data)

    for change in plan.changes:
        target_paras = _select_paragraphs(
            body,
            change,
            style_name_to_id,
            style_id_to_style,
            heading_num_ids=heading_num_ids,
            in_header_footer=False,
        )
        for p in target_paras:
            _apply_paragraph_patch(p, change)
            _apply_run_patch(p, change)

        for story_root in header_footer_roots.values():
            target_paras = _select_paragraphs(
                story_root,
                change,
                style_name_to_id,
                style_id_to_style,
                heading_num_ids=heading_num_ids,
                in_header_footer=True,
            )
            for p in target_paras:
                _apply_paragraph_patch(p, change)
                _apply_run_patch(p, change)

        # Prefer style-level patch for non-table scopes to reduce run-by-run mutations.
        if not change.scope.startswith("tables"):
            _apply_style_patch(change, style_name_to_id, style_id_to_style)

    entries["word/document.xml"] = etree.tostring(
        root, xml_declaration=True, encoding="UTF-8", standalone="yes"
    )
    for name, story_root in header_footer_roots.items():
        entries[name] = etree.tostring(story_root, xml_declaration=True, encoding="UTF-8", standalone="yes")
    if styles_xml and style_id_to_style:
        styles_root = next(iter(style_id_to_style.values())).getroottree().getroot()
        entries["word/styles.xml"] = etree.tostring(
            styles_root, xml_declaration=True, encoding="UTF-8", standalone="yes"
        )

    # Write rebuilt package
    buffer = BytesIO()
    with ZipFile(buffer, "w", compression=ZIP_DEFLATED) as zout:
        for name, data in entries.items():
            zout.writestr(name, data)
    dst_docx.write_bytes(buffer.getvalue())


def inspect_docx_integrity(docx_path: Path) -> list[str]:
    warnings: list[str] = []
    try:
        with ZipFile(docx_path, "r") as z:
            names = set(z.namelist())
            if "[Content_Types].xml" not in names:
                warnings.append("缺少 [Content_Types].xml")
            if "word/document.xml" not in names:
                warnings.append("缺少 word/document.xml")
            # Parse key story parts
            for n in names:
                if n == "word/document.xml" or (n.startswith("word/header") and n.endswith(".xml")) or (
                    n.startswith("word/footer") and n.endswith(".xml")
                ):
                    try:
                        etree.fromstring(z.read(n))
                    except Exception:
                        warnings.append(f"{n} 解析失败")
    except Exception as exc:
        warnings.append(f"文档完整性检查失败: {exc}")
    return warnings


def estimate_plan_impact(docx_path: Path, plan: Plan) -> dict[str, int]:
    plan = _apply_font_defaults(plan)
    with ZipFile(docx_path, "r") as zin:
        entries = {name: zin.read(name) for name in zin.namelist()}
    styles_xml = entries.get("word/styles.xml")
    style_name_to_id, style_id_to_style = _read_styles(styles_xml)
    heading_num_ids = _read_heading_numbering_ids(entries.get("word/numbering.xml"))
    doc_xml = entries.get("word/document.xml")
    if not doc_xml:
        return {}
    root = etree.fromstring(doc_xml)
    body = root.find("w:body", namespaces=NSMAP)
    if body is None:
        return {}

    impact: dict[str, int] = {}
    for ch in plan.changes:
        selected = _select_paragraphs(
            body,
            ch,
            style_name_to_id,
            style_id_to_style,
            heading_num_ids=heading_num_ids,
            in_header_footer=False,
        )
        if ch.scope == "body":
            impact["body_paragraphs_changed_est"] = impact.get("body_paragraphs_changed_est", 0) + len(selected)
        elif ch.scope.startswith("heading_"):
            impact[f"{ch.scope}_changed_est"] = impact.get(f"{ch.scope}_changed_est", 0) + len(selected)
        elif ch.scope == "references":
            impact["references_changed_est"] = impact.get("references_changed_est", 0) + len(selected)
        elif ch.scope.startswith("tables"):
            # estimate table count by unique ancestor tbl nodes touched
            tbl_ids = {id(_ancestor_tbl(p)) for p in selected if _ancestor_tbl(p) is not None}
            impact["table_count_est"] = impact.get("table_count_est", 0) + len(tbl_ids)
    return impact


def _read_styles(styles_xml: bytes | None) -> tuple[dict[str, str], dict[str, etree._Element]]:
    if not styles_xml:
        return {}, {}
    root = etree.fromstring(styles_xml)
    name_to_id: dict[str, str] = {}
    id_to_style: dict[str, etree._Element] = {}
    for st in root.findall("w:style", namespaces=NSMAP):
        sid = st.get(_w("styleId"))
        name_elm = st.find("w:name", namespaces=NSMAP)
        if not sid:
            continue
        id_to_style[sid] = st
        if name_elm is None:
            continue
        name = name_elm.get(_w("val"))
        if name:
            name_to_id[name] = sid
    return name_to_id, id_to_style


def _scope_style_names(scope: str) -> set[str]:
    return {
        "body": {"正文", "Normal"},
        "heading_1": {"标题 1", "Heading 1"},
        "heading_2": {"标题 2", "Heading 2"},
        "heading_3": {"标题 3", "Heading 3"},
        "references": {"参考文献", "References"},
    }.get(scope, set())


def _select_paragraphs(
    story_root: etree._Element,
    change: Change,
    style_name_to_id: dict[str, str],
    style_id_to_style: dict[str, etree._Element],
    heading_num_ids: set[str],
    *,
    in_header_footer: bool,
) -> list[etree._Element]:
    paragraphs = story_root.findall(".//w:p", namespaces=NSMAP)

    if change.scope == "tables_all":
        return [p for p in paragraphs if _is_in_table(p)]
    if change.scope == "tables_header_only":
        return [p for p in paragraphs if _is_in_table_header_row(p)]
    if change.scope == "references":
        refs = _reference_region_paragraphs(story_root, change)
        if refs:
            return refs

    target_style_ids = {style_name_to_id[n] for n in _scope_style_names(change.scope) if n in style_name_to_id}
    # For header/footer story parts, treat "body" scope as broad non-table text scope
    # unless user explicitly provided style_name filters in selector.
    if in_header_footer and change.scope == "body" and not (change.selector and change.selector.style_names_any_of):
        target_style_ids = set()
    include_no_style = change.scope == "body"
    return [
        p
        for p in paragraphs
        if not _is_in_table(p)
        and _matches_selector_fallback(
            p,
            change,
            target_style_ids=target_style_ids,
            style_name_to_id=style_name_to_id,
            style_id_to_style=style_id_to_style,
            heading_num_ids=heading_num_ids,
            include_no_style=include_no_style,
        )
    ]


def _matches_selector_fallback(
    p: etree._Element,
    change: Change,
    *,
    target_style_ids: set[str],
    style_name_to_id: dict[str, str],
    style_id_to_style: dict[str, etree._Element],
    heading_num_ids: set[str],
    include_no_style: bool,
) -> bool:
    para_style_id = _paragraph_style_id(p)
    if target_style_ids:
        if para_style_id is None and include_no_style:
            pass
        elif para_style_id not in target_style_ids:
            return False

    sel = change.selector
    if sel is None:
        # If we had style targets, they already matched; otherwise default body fallback
        return bool(target_style_ids) or change.scope == "body"

    if sel.style_names_any_of:
        allowed = {style_name_to_id[n] for n in sel.style_names_any_of if n in style_name_to_id}
        if allowed and para_style_id not in allowed:
            return False
    if sel.bold is True and not _para_has_bold_run(p):
        return False
    if sel.bold is False and _para_has_bold_run(p):
        return False
    if sel.centered is True and _paragraph_alignment(p) != "center":
        return False
    if sel.centered is False and _paragraph_alignment(p) == "center":
        return False
    if sel.numbered_like_heading is True and not _has_heading_like_numbering(p, heading_num_ids):
        return False
    if sel.numbered_like_heading is False and _has_heading_like_numbering(p, heading_num_ids):
        return False
    if sel.min_font_size_pt is not None and _effective_paragraph_font_size_pt(p, para_style_id, style_id_to_style) < sel.min_font_size_pt:
        return False
    return True


def _paragraph_style_id(p: etree._Element) -> str | None:
    ppr = p.find("w:pPr", namespaces=NSMAP)
    if ppr is None:
        return None
    pstyle = ppr.find("w:pStyle", namespaces=NSMAP)
    if pstyle is None:
        return None
    return pstyle.get(_w("val"))


def _is_in_table(p: etree._Element) -> bool:
    cur = p.getparent()
    while cur is not None:
        if cur.tag == _w("tbl"):
            return True
        cur = cur.getparent()
    return False


def _ancestor_tbl(p: etree._Element) -> etree._Element | None:
    cur = p.getparent()
    while cur is not None:
        if cur.tag == _w("tbl"):
            return cur
        cur = cur.getparent()
    return None


def _is_in_table_header_row(p: etree._Element) -> bool:
    cur = p
    tr = None
    tbl = None
    while cur is not None:
        if cur.tag == _w("tr") and tr is None:
            tr = cur
        if cur.tag == _w("tbl"):
            tbl = cur
            break
        cur = cur.getparent()
    if tr is None or tbl is None:
        return False
    return tr in _detect_table_header_rows(tbl)


def _detect_table_header_rows(tbl: etree._Element) -> set[etree._Element]:
    """
    Heuristic header detection:
    1) explicit w:tblHeader row property
    2) otherwise score top rows by bold/centered ratio and keep contiguous header block
    """
    rows = tbl.findall("w:tr", namespaces=NSMAP)
    if not rows:
        return set()

    explicit: list[etree._Element] = []
    for row in rows:
        if row.find("w:trPr/w:tblHeader", namespaces=NSMAP) is not None:
            explicit.append(row)
    if explicit:
        return set(explicit)

    picked: list[etree._Element] = []
    for idx, row in enumerate(rows):
        score = _score_row_header_like(row)
        # Keep first contiguous header-like rows from top.
        if idx == 0 and score >= 1:
            picked.append(row)
            continue
        if picked and score >= 1:
            picked.append(row)
            continue
        if picked:
            break
    if picked:
        return set(picked)
    # Fallback: first row only
    return {rows[0]}


def _score_row_header_like(row: etree._Element) -> int:
    cells = row.findall("w:tc", namespaces=NSMAP)
    if not cells:
        return 0
    para_count = 0
    bold_count = 0
    center_count = 0
    for tc in cells:
        for p in tc.findall(".//w:p", namespaces=NSMAP):
            para_count += 1
            if _para_has_bold_run(p):
                bold_count += 1
            if _paragraph_alignment(p) == "center":
                center_count += 1
    if para_count == 0:
        return 0
    score = 0
    if bold_count / para_count >= 0.5:
        score += 1
    if center_count / para_count >= 0.5:
        score += 1
    return score


def _apply_paragraph_patch(p: etree._Element, change: Change) -> None:
    if change.paragraph is None and change.scope != "references":
        return
    ppr = _ensure_child(p, "pPr")

    if change.paragraph and change.paragraph.alignment is not None:
        jc = _ensure_child(ppr, "jc")
        jc.set(_w("val"), change.paragraph.alignment)

    if (
        change.paragraph
        and (
            change.paragraph.line_spacing is not None
            or change.paragraph.space_before_pt is not None
            or change.paragraph.space_after_pt is not None
        )
    ):
        spacing = _ensure_child(ppr, "spacing")
        if change.paragraph and change.paragraph.space_before_pt is not None:
            spacing.set(_w("before"), str(int(change.paragraph.space_before_pt * 20)))
        if change.paragraph and change.paragraph.space_after_pt is not None:
            spacing.set(_w("after"), str(int(change.paragraph.space_after_pt * 20)))
        if change.paragraph and change.paragraph.line_spacing is not None:
            line_twips = {"single": 240, "1.5": 360, "double": 480}[change.paragraph.line_spacing]
            spacing.set(_w("line"), str(line_twips))
            spacing.set(_w("lineRule"), "auto")

    if change.paragraph and change.paragraph.first_line_indent_chars is not None:
        ind = _ensure_child(ppr, "ind")
        # Better than fixed twips for CJK docs: 1 char = 100 units in Word char metric.
        ind.set(_w("firstLineChars"), str(int(change.paragraph.first_line_indent_chars * 100)))
        ind.attrib.pop(_w("hangingChars"), None)
    elif change.scope == "references":
        # References default: hanging indent 2 chars, unless explicitly overridden.
        ind = _ensure_child(ppr, "ind")
        ind.set(_w("hangingChars"), "200")
        ind.attrib.pop(_w("firstLineChars"), None)


def _apply_run_patch(p: etree._Element, change: Change) -> None:
    if change.font is None:
        return
    for r in p.findall("w:r", namespaces=NSMAP):
        rpr = _ensure_child(r, "rPr")

        if change.font.east_asia or change.font.ascii:
            rfonts = _ensure_child(rpr, "rFonts")
            if change.font.east_asia:
                rfonts.set(_w("eastAsia"), change.font.east_asia)
            if change.font.ascii:
                rfonts.set(_w("ascii"), change.font.ascii)
                rfonts.set(_w("hAnsi"), change.font.ascii)
                rfonts.set(_w("cs"), change.font.ascii)

        if change.font.size_pt is not None:
            half_points = str(int(change.font.size_pt * 2))
            sz = _ensure_child(rpr, "sz")
            sz.set(_w("val"), half_points)
            szcs = _ensure_child(rpr, "szCs")
            szcs.set(_w("val"), half_points)

        if change.font.bold is not None:
            if change.font.bold:
                _ensure_child(rpr, "b")
                _ensure_child(rpr, "bCs")
            else:
                b = _ensure_child(rpr, "b")
                b.set(_w("val"), "false")
                bcs = _ensure_child(rpr, "bCs")
                bcs.set(_w("val"), "false")


def _ensure_child(parent: etree._Element, local_name: str) -> etree._Element:
    child = parent.find(f"w:{local_name}", namespaces=NSMAP)
    if child is None:
        child = etree.SubElement(parent, _w(local_name))
    return child


def _para_has_bold_run(p: etree._Element) -> bool:
    for b in p.findall(".//w:r/w:rPr/w:b", namespaces=NSMAP):
        if b.get(_w("val")) in (None, "true", "1"):
            return True
    return False


def _paragraph_alignment(p: etree._Element) -> str | None:
    jc = p.find("w:pPr/w:jc", namespaces=NSMAP)
    if jc is None:
        return None
    return jc.get(_w("val"))


def _has_numbering(p: etree._Element) -> bool:
    return p.find("w:pPr/w:numPr", namespaces=NSMAP) is not None


def _num_id(p: etree._Element) -> str | None:
    num = p.find("w:pPr/w:numPr/w:numId", namespaces=NSMAP)
    if num is None:
        return None
    return num.get(_w("val"))


def _has_heading_like_numbering(p: etree._Element, heading_num_ids: set[str]) -> bool:
    nid = _num_id(p)
    if not nid:
        return False
    if heading_num_ids:
        return nid in heading_num_ids
    return True


def _read_heading_numbering_ids(numbering_xml: bytes | None) -> set[str]:
    if not numbering_xml:
        return set()
    root = etree.fromstring(numbering_xml)
    abstract_heading: set[str] = set()
    for a in root.findall("w:abstractNum", namespaces=NSMAP):
        aid = a.get(_w("abstractNumId"))
        if not aid:
            continue
        name = a.find("w:name", namespaces=NSMAP)
        mlt = a.find("w:multiLevelType", namespaces=NSMAP)
        if (name is not None and "heading" in (name.get(_w("val")) or "").lower()) or (
            mlt is not None and (mlt.get(_w("val")) or "") in {"hybridMultilevel", "multilevel"}
        ):
            abstract_heading.add(aid)
            continue
        for lvl in a.findall("w:lvl", namespaces=NSMAP):
            pstyle = lvl.find("w:pStyle", namespaces=NSMAP)
            if pstyle is not None and (pstyle.get(_w("val")) or "").lower().startswith("heading"):
                abstract_heading.add(aid)
                break
    out: set[str] = set()
    for n in root.findall("w:num", namespaces=NSMAP):
        nid = n.get(_w("numId"))
        aid = n.find("w:abstractNumId", namespaces=NSMAP)
        if not nid or aid is None:
            continue
        if (aid.get(_w("val")) or "") in abstract_heading:
            out.add(nid)
    return out


def _reference_region_paragraphs(story_root: etree._Element, change: Change) -> list[etree._Element]:
    paragraphs = story_root.findall(".//w:p", namespaces=NSMAP)
    if not paragraphs:
        return []
    idx = None
    # 1) explicit heading text
    for i, p in enumerate(paragraphs):
        txt = _paragraph_text(p).strip().lower()
        if txt in {"参考文献", "references", "参考文献：", "references:"}:
            idx = i
            break
    # 2) style-name based heading
    if idx is None:
        for i, p in enumerate(paragraphs):
            sid = _paragraph_style_id(p) or ""
            if "reference" in sid.lower():
                idx = i
                break
    # 3) trailing reference-like block heuristic
    if idx is None:
        start = _guess_reference_block_start(paragraphs, change)
        if start is not None:
            idx = start
    if idx is None:
        return []
    after = [p for p in paragraphs[idx + 1 :] if not _is_in_table(p)]
    block = _leading_reference_like_block(after, change)
    if block:
        return block
    return after


def _guess_reference_block_start(paragraphs: list[etree._Element], change: Change) -> int | None:
    # Scan last 40 paragraphs, find first of 3+ consecutive reference-like lines.
    max_scan = 40
    if change.selector and change.selector.reference_max_scan_paragraphs:
        max_scan = int(change.selector.reference_max_scan_paragraphs)
    start_scan = max(0, len(paragraphs) - max_scan)
    window = paragraphs[start_scan:]
    streak = 0
    first_idx = None
    for i, p in enumerate(window):
        txt = _paragraph_text(p).strip()
        if _looks_like_reference_item(txt):
            streak += 1
            if first_idx is None:
                first_idx = i
            if streak >= 3 and first_idx is not None:
                return start_scan + first_idx - 1 if (start_scan + first_idx - 1) >= 0 else 0
        else:
            streak = 0
            first_idx = None
    return None


def _leading_reference_like_block(paragraphs: list[etree._Element], change: Change) -> list[etree._Element]:
    """
    Take the leading contiguous references block and stop before appendix/ack sections.
    Rules:
    - include reference-like items
    - allow a single empty separator line
    - stop on heading-like paragraph or two consecutive non-reference lines
    """
    out: list[etree._Element] = []
    non_ref_streak = 0
    allow_blank = True
    if change.selector and change.selector.reference_allow_blank_lines is False:
        allow_blank = False
    for p in paragraphs:
        txt = _paragraph_text(p).strip()
        if not txt:
            if out and allow_blank:
                out.append(p)
            continue
        if _is_section_heading_like(p, txt):
            break
        if _looks_like_reference_item(txt):
            out.append(p)
            non_ref_streak = 0
            continue
        if not out:
            # Haven't entered references block yet
            continue
        non_ref_streak += 1
        if non_ref_streak >= 2:
            break
        out.append(p)
    # trim trailing empty paragraphs
    while out and not _paragraph_text(out[-1]).strip():
        out.pop()
    return out


def _looks_like_reference_item(text: str) -> bool:
    if not text:
        return False
    t = text.strip()
    patterns = [
        r"^\[\d+\]",
        r"^\d+\.",
        r"^\d+\s+",
        r".+\(\d{4}\).+",
        r".+doi[:\s].+",
    ]
    return any(re.search(p, t, flags=re.IGNORECASE) is not None for p in patterns)


def _is_section_heading_like(p: etree._Element, text: str) -> bool:
    sid = (_paragraph_style_id(p) or "").lower()
    if sid.startswith("heading") or "title" in sid:
        return True
    t = text.strip().lower().rstrip(":：")
    if t in {"附录", "appendix", "致谢", "acknowledgements", "acknowledgments"}:
        return True
    return False


def _paragraph_text(p: etree._Element) -> str:
    return "".join(t.text or "" for t in p.findall(".//w:t", namespaces=NSMAP))


def _effective_paragraph_font_size_pt(
    p: etree._Element, para_style_id: str | None, style_id_to_style: dict[str, etree._Element]
) -> float:
    # 1) direct run size
    for sz in p.findall(".//w:r/w:rPr/w:sz", namespaces=NSMAP):
        val = sz.get(_w("val"))
        if val and val.isdigit():
            return int(val) / 2.0
    # 2) style run size
    if para_style_id and para_style_id in style_id_to_style:
        st = style_id_to_style[para_style_id]
        sz = st.find("w:rPr/w:sz", namespaces=NSMAP)
        if sz is not None:
            val = sz.get(_w("val"))
            if val and val.isdigit():
                return int(val) / 2.0
    return 0.0


def _apply_style_patch(
    change: Change, style_name_to_id: dict[str, str], style_id_to_style: dict[str, etree._Element]
) -> None:
    style_names = _scope_style_names(change.scope)
    style_ids = [style_name_to_id[n] for n in style_names if n in style_name_to_id]
    for sid in style_ids:
        st = style_id_to_style.get(sid)
        if st is None:
            continue
        if change.paragraph:
            ppr = _ensure_child(st, "pPr")
            _apply_paragraph_patch_to_ppr(ppr, change)
        if change.font:
            rpr = _ensure_child(st, "rPr")
            _apply_font_patch_to_rpr(rpr, change)


def _apply_paragraph_patch_to_ppr(ppr: etree._Element, change: Change) -> None:
    if change.paragraph is None and change.scope != "references":
        return
    if change.paragraph and change.paragraph.alignment is not None:
        jc = _ensure_child(ppr, "jc")
        jc.set(_w("val"), change.paragraph.alignment)
    if (
        change.paragraph
        and (
            change.paragraph.line_spacing is not None
            or change.paragraph.space_before_pt is not None
            or change.paragraph.space_after_pt is not None
        )
    ):
        spacing = _ensure_child(ppr, "spacing")
        if change.paragraph and change.paragraph.space_before_pt is not None:
            spacing.set(_w("before"), str(int(change.paragraph.space_before_pt * 20)))
        if change.paragraph and change.paragraph.space_after_pt is not None:
            spacing.set(_w("after"), str(int(change.paragraph.space_after_pt * 20)))
        if change.paragraph and change.paragraph.line_spacing is not None:
            spacing.set(_w("line"), str({"single": 240, "1.5": 360, "double": 480}[change.paragraph.line_spacing]))
            spacing.set(_w("lineRule"), "auto")
    if change.paragraph and change.paragraph.first_line_indent_chars is not None:
        ind = _ensure_child(ppr, "ind")
        ind.set(_w("firstLineChars"), str(int(change.paragraph.first_line_indent_chars * 100)))
        ind.attrib.pop(_w("hangingChars"), None)
    elif change.scope == "references":
        ind = _ensure_child(ppr, "ind")
        ind.set(_w("hangingChars"), "200")
        ind.attrib.pop(_w("firstLineChars"), None)


def _apply_font_patch_to_rpr(rpr: etree._Element, change: Change) -> None:
    if change.font is None:
        return
    if change.font.east_asia or change.font.ascii:
        rfonts = _ensure_child(rpr, "rFonts")
        if change.font.east_asia:
            rfonts.set(_w("eastAsia"), change.font.east_asia)
        if change.font.ascii:
            rfonts.set(_w("ascii"), change.font.ascii)
            rfonts.set(_w("hAnsi"), change.font.ascii)
            rfonts.set(_w("cs"), change.font.ascii)
    if change.font.size_pt is not None:
        half_points = str(int(change.font.size_pt * 2))
        sz = _ensure_child(rpr, "sz")
        sz.set(_w("val"), half_points)
        szcs = _ensure_child(rpr, "szCs")
        szcs.set(_w("val"), half_points)
    if change.font.bold is not None:
        if change.font.bold:
            _ensure_child(rpr, "b")
            _ensure_child(rpr, "bCs")
        else:
            b = _ensure_child(rpr, "b")
            b.set(_w("val"), "false")
            bcs = _ensure_child(rpr, "bCs")
            bcs.set(_w("val"), "false")

