import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow.agent import Agent


class DummyAgent(Agent):
    def decide(self, goal: str, context: dict) -> dict:
        return {}

    def act(self, decision: dict):
        return None


def test_all_timestamps_end_with_z():
    agent = DummyAgent(name="dummy", role="tester")
    state = agent.export_state()
    assert state["created_at"].endswith("Z")
    agent.observe({"event": "test"})
    state = agent.export_state()
    assert state["last_active"].endswith("Z")
    for obs in state["memory"]:
        assert obs["timestamp"].endswith("Z")
