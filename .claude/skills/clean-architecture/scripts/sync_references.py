"""Rebuild references/*.md from the Clean Architecture notes in an Obsidian vault.

Usage:
    python scripts/sync_references.py <path-to-developer-knowledge-vault>

Each reference file is a concatenation of whole notes, in reading order, with the
Obsidian-only syntax removed: frontmatter is dropped, [[wikilinks]] become plain
text (with the target reference file named when it lives in another one), and
"Related" bullets pointing at notes outside this skill are dropped, since the
reader has no way to follow them.
"""

import re
import sys
from pathlib import Path

SKILL_DIR = Path(__file__).resolve().parent.parent

# Reference file -> (title, intro, notes in book order). Paths are relative to the vault.
REFERENCES = {
    "paradigms-and-values.md": (
        "Design, values and programming paradigms",
        "Chapters 1–6: what architecture is for, and the three paradigms it is built on.",
        [
            "software-architecture/design-vs-architecture.md",
            "software-architecture/two-values-of-software.md",
            "programming-paradigms/structured-programming.md",
            "programming-paradigms/object-oriented-programming.md",
            "programming-paradigms/functional-programming.md",
        ],
    ),
    "solid.md": (
        "SOLID principles",
        "Chapters 7–11: the module-level design principles.",
        [
            "solid-principles/single-responsibility-principle.md",
            "solid-principles/open-closed-principle.md",
            "solid-principles/liskov-substitution-principle.md",
            "solid-principles/interface-segregation-principle.md",
            "solid-principles/dependency-inversion-principle.md",
        ],
    ),
    "component-principles.md": (
        "Component principles",
        "Chapters 12–14: what goes in a component (cohesion) and how components may depend on each other (coupling, with metrics).",
        [
            "component-principles/components.md",
            "component-principles/reuse-release-equivalence-principle.md",
            "component-principles/common-closure-principle.md",
            "component-principles/common-reuse-principle.md",
            "component-principles/component-cohesion.md",
            "component-principles/acyclic-dependencies-principle.md",
            "component-principles/stable-dependencies-principle.md",
            "component-principles/stable-abstractions-principle.md",
            "component-principles/main-sequence.md",
        ],
    ),
    # Chapters 15–26 are split by the question being answered rather than by chapter:
    # "where does this code go?" (needed by almost every review) vs. "should there be a
    # boundary here, and how strong?" (needed mostly for design and scaling decisions).
    "layers-and-dependency-rule.md": (
        "Layers and the Dependency Rule",
        "Chapters 16, 20, 22–23, 26: the circles, the Dependency Rule, entities vs. use cases, what crosses a boundary, Humble Object, and Main as composition root.",
        [
            "software-architecture/clean-architecture-layers.md",
            "software-architecture/dependency-rule.md",
            "software-architecture/critical-business-rules.md",
            "software-architecture/application-business-rules.md",
            "software-architecture/humble-object.md",
            "software-architecture/main-component.md",
            "software-architecture/accidental-duplication.md",
        ],
    ),
    "drawing-boundaries.md": (
        "Drawing boundaries",
        "Chapters 15–19, 24–25: policy vs. details, where boundaries go, decoupling modes from monolith to services, and how much boundary to pay for.",
        [
            "software-architecture/keep-options-open.md",
            "software-architecture/policy-and-level.md",
            "software-architecture/decoupling-layers-and-use-cases.md",
            "software-architecture/architectural-boundaries.md",
            "software-architecture/decoupling-modes.md",
            "software-architecture/partial-boundaries.md",
            "software-architecture/anticipating-boundaries.md",
        ],
    ),
    "details.md": (
        "Services, tests and details",
        "Chapters 27–32: microservices, the test boundary, embedded systems, and why the database, the web and frameworks are details.",
        [
            "software-architecture/services-are-not-architecture.md",
            "software-architecture/test-boundary.md",
            "software-architecture/clean-embedded-architecture.md",
            "software-architecture/database-is-a-detail.md",
            "software-architecture/web-is-a-detail.md",
            "software-architecture/frameworks-are-details.md",
        ],
    ),
    "packaging.md": (
        "Screaming architecture and packaging",
        "Chapters 21 and 34: what the top-level structure should say, and the four ways to package code (and how to make the compiler enforce them).",
        [
            "software-architecture/screaming-architecture.md",
            "software-architecture/packaging-strategies.md",
        ],
    ),
}

# Files longer than this get a table of contents (repo convention, see AGENTS.md).
TOC_THRESHOLD_LINES = 100

WIKILINK = re.compile(r"\[\[([^\]|#]*)(#[^\]|]*)?(?:\\?\|([^\]]*))?\]\]")


def split_note(text):
    """Return (title, body) with frontmatter, the H1 and the Source line removed."""
    if text.startswith("---"):
        end = text.find("\n---", 3)
        if end == -1:
            raise ValueError("unterminated frontmatter")
        text = text[end + 4 :]
    lines = text.strip().splitlines()
    title_idx = next((i for i, l in enumerate(lines) if l.startswith("# ")), None)
    if title_idx is None:
        raise ValueError("no '# ' title")
    title = lines[title_idx][2:].strip()
    body = lines[title_idx + 1 :]
    while body and not body[0].strip():
        body.pop(0)
    return title, body


def build_index(vault):
    """Map note stem -> (title, reference file) for every note this skill includes."""
    index = {}
    for ref_file, (_, _, notes) in REFERENCES.items():
        for rel in notes:
            path = vault / rel
            title, _ = split_note(path.read_text(encoding="utf-8"))
            index[path.stem] = (title, ref_file)
    return index


def humanize(stem):
    return stem.replace("-", " ")


def render_links(line, index, current_file):
    def repl(m):
        target, anchor, alias = m.group(1).strip(), m.group(2), m.group(3)
        if not target and anchor:  # same-note anchor like [[#Conway's law|...]]
            return alias or anchor[1:]
        label = alias.strip() if alias else (index[target][0] if target in index else humanize(target))
        if target in index and index[target][1] != current_file:
            return f"{label} (`{index[target][1]}`)"
        return label

    return WIKILINK.sub(repl, line)


def links_in(line):
    return [m.group(1).strip() for m in WIKILINK.finditer(line)]


def convert_note(body, index, current_file):
    out = []
    in_related = False
    in_code = False
    for line in body:
        if line.startswith("```"):
            in_code = not in_code
        if in_code or line.startswith("```"):
            out.append(line)
            continue
        if line.startswith("**Source:**"):
            chapter = re.search(r"\*,\s*(.+)$", line)
            if chapter:
                out.append(f"*{chapter.group(1).strip()}*")
            continue
        if line.startswith("## "):
            was_related = in_related
            in_related = line.strip() == "## Related"
            if in_related and was_related:
                continue  # a note with a duplicated Related heading
        if in_related and line.startswith("- "):
            targets = links_in(line)
            if not targets or targets[0] not in index:
                continue
        if line.startswith("#"):
            line = "#" + line  # every note heading drops one level under the file's H1
        out.append(render_links(line, index, current_file))
    # Drop an empty Related section left over after filtering.
    while out and not out[-1].strip():
        out.pop()
    if out and out[-1].strip() == "### Related":
        out.pop()
    return out


def anchor(title):
    slug = re.sub(r"[^\w\- ]", "", title.lower()).strip()
    return re.sub(r"\s+", "-", slug)


def build_file(vault, ref_file, index):
    title, intro, notes = REFERENCES[ref_file]
    sections, titles = [], []
    for rel in notes:
        note_title, body = split_note((vault / rel).read_text(encoding="utf-8"))
        titles.append(note_title)
        sections.append([f"## {note_title}", ""] + convert_note(body, index, ref_file))

    content = []
    for s in sections:
        content.extend(s + [""])
    head = [f"# {title}", "", intro, ""]
    if len(content) > TOC_THRESHOLD_LINES:
        head += ["## Contents", ""] + [f"- [{t}](#{anchor(t)})" for t in titles] + [""]
    return "\n".join(head + content).rstrip() + "\n"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    vault = Path(sys.argv[1]).expanduser()
    missing = [rel for _, _, notes in REFERENCES.values() for rel in notes if not (vault / rel).is_file()]
    if missing:
        sys.exit("Notes not found in the vault:\n  " + "\n  ".join(missing))

    index = build_index(vault)
    out_dir = SKILL_DIR / "references"
    out_dir.mkdir(exist_ok=True)
    for ref_file in REFERENCES:
        text = build_file(vault, ref_file, index)
        (out_dir / ref_file).write_text(text, encoding="utf-8")
        print(f"{ref_file}: {text.count(chr(10))} lines")


if __name__ == "__main__":
    main()
