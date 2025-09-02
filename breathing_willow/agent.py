from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional
import uuid
from datetime import datetime, timezone

class Agent(ABC):
    """
    Base class for Zeroist/AgentOrchestra agents (v0.5 shaping seed).

    Extensible, modular, and reflexive. Implements common lifecycle methods
    and state management to align with the orchestration layer.
    """

    def __init__(self, name: str, role: str, tools: Optional[Dict[str, Any]] = None):
        self.id = str(uuid.uuid4())
        self.name = name                   # Symbolic name (e.g., "Wú", "Yuē")
        self.role = role                   # Functional or mythic role
        self.tools = tools or {}           # Tool interfaces assigned to this agent
        self.memory: List[Dict[str, Any]] = []
        self.created_at = datetime.now(timezone.utc)
        self.last_active = None

    def observe(self, observation: Dict[str, Any]) -> None:
        """Store observation with timestamp."""
        now = datetime.now(timezone.utc)
        observation["timestamp"] = now.isoformat().replace("+00:00", "Z")
        self.memory.append(observation)
        self.last_active = now

    @abstractmethod
    def decide(self, goal: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Core reasoning step: interpret goal and context, 
        return an action plan or next step.
        """
        pass

    @abstractmethod
    def act(self, decision: Dict[str, Any]) -> Any:
        """
        Execute decision using tools and environment interfaces.
        Return results or outputs to feed back into orchestration.
        """
        pass

    def reflect(self) -> None:
        """
        Reflexive self-check: adjust strategy based on memory, 
        role constraints, and orchestration feedback.
        """
        # Placeholder: will connect to orchestration-level feedback loops
        pass

    def export_state(self) -> Dict[str, Any]:
        """Return serializable snapshot of agent state."""
        return {
            "id": self.id,
            "name": self.name,
            "role": self.role,
            "tools": list(self.tools.keys()),
            "memory": self.memory,
            "created_at": self.created_at.isoformat().replace("+00:00", "Z"),
            "last_active": self.last_active.isoformat().replace("+00:00", "Z") if self.last_active else None,
        }

