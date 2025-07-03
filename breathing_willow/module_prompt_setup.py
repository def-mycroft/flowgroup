"""Auto-generate Codex prompt blocks from a Python module.

Extracts class and method docstrings and emits:
  - One init prompt (scaffold overview)
  - Three implementation prompts (per-class or per-method logic)
All prompts are saved to codex/prompts/

Usage:
    generate_codex_prompts(Path("path/to/module.py"), Path("output/root/"))
"""

from pathlib import Path
import ast
import textwrap


def extract_doc_items(source: str) -> list[dict]:
    """Return class and method docstring metadata from a Python source file."""
    tree = ast.parse(source)
    items = []

    for node in tree.body:
        if isinstance(node, ast.ClassDef):
            class_name = node.name
            class_doc = ast.get_docstring(node) or ""
            methods = [
                {
                    "method_name": func.name,
                    "method_doc": ast.get_docstring(func) or ""
                }
                for func in node.body
                if isinstance(func, ast.FunctionDef)
            ]
            items.append({
                "class_name": class_name,
                "class_doc": class_doc,
                "methods": methods
            })
    return items


def write_prompt(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")
    print(f"wrote '{path}'")


def make_init_prompt(module_path: Path, items: list[dict]) -> str:
    relpath = module_path.as_posix()
    lines = [f"# Init Prompt for `{relpath}`\n"]
    lines.append("This prompt generates a full module scaffold for the following structure.\n")

    for item in items:
        lines.append(f"- Class: `{item['class_name']}`")
        for m in item["methods"]:
            lines.append(f"  - Method: `{m['method_name']}()`")
    lines.append("\nEnsure consistent naming, include standard imports, and match the expected Codex style.")
    return "\n".join(lines)


def make_impl_prompt(item: dict) -> str:
    lines = [f"# Implementation Prompt for `{item['class_name']}`\n"]
    lines.append(textwrap.fill(item["class_doc"], 80))
    lines.append("\nImplement the following methods:\n")
    for m in item["methods"]:
        lines.append(f"## `{m['method_name']}`")
        lines.append(textwrap.fill(m["method_doc"], 80) + "\n")
    lines.append("Follow the Codex shaping prompt conventions for clarity and testability.")
    return "\n".join(lines)


def generate_codex_prompts(module_path: str, output_dir: str) -> None:
    module_path = Path(module_path)
    output_dir = Path(output_dir)
    source = module_path.read_text(encoding="utf-8")
    items = extract_doc_items(source)
    prompt_dir = output_dir / "codex/prompts/"

    # init prompt
    init_path = prompt_dir / "000-init.md"
    init_text = make_init_prompt(module_path, items)
    write_prompt(init_path, init_text)

    # impl prompts
    for i, item in enumerate(items[:3], 1):  # cap at 3 for v0.5
        impl_path = prompt_dir / f"{i:03d}-{item['class_name'].lower()}.md"
        impl_text = make_impl_prompt(item)
        write_prompt(impl_path, impl_text)

