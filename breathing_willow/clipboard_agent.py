from __future__ import annotations

"""ClipboardAgent implementation.

This simple agent persists its context to a markdown file and performs
minimal decision and action steps. Intended as an example or placeholder
for more complex agents.
"""

from pathlib import Path
from typing import Any, Dict

from .agent import Agent


class ClipboardAgent(Agent):
    """An ``Agent`` that stores and echoes clipboard context."""

    def __init__(self, context_path: Path) -> None:
        self.context_path = Path(context_path)
        name = self.context_path.stem
        super().__init__(name=name, role="clipboard")

    # ------------------------------------------------------------------
    # Context helpers
    def load_context(self) -> str:
        """Return the current context file contents."""
        if not self.context_path.exists():
            return ""
        return self.context_path.read_text(encoding="utf-8")

    def save_context(self, content: str) -> None:
        """Write ``content`` to the context file."""
        self.context_path.write_text(content, encoding="utf-8")

    # ------------------------------------------------------------------
    # Agent lifecycle
    def decide(self, goal: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """Minimal decision step that echoes stored context."""
        return {"goal": goal, "context": self.load_context()}

    def act(self, decision: Dict[str, Any]) -> Any:
        """Return the decision as the action result."""
        return decision
