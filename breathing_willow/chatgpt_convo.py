"""Convenience functions for interacting with OpenAI chat completions.

This module mirrors a small chat helper used in a different project but is
packaged for use inside :mod:`breathing_willow`.  It exposes a single public
function, :func:`chat`, which returns the updated conversation along with the
model reply.

The OpenAI API key is read from :data:`FP_API_KEY` when the module is imported.
"""

from __future__ import annotations

from copy import copy
from pathlib import Path
from typing import List, Tuple

from openai import OpenAI

# Hard coded path to the file containing the OpenAI API key.
# Adjust the path to match the environment where Willow runs.
FP_API_KEY = Path.home() / ".ssh" / "fp-openai.key"

with FP_API_KEY.open() as f:  # pragma: no cover - file system interaction
    _client = OpenAI(api_key=f.read().strip())

CONVO: List[dict] = [{"role": "system", "content": "you are a helpful assistant"}]
# Default model used for chat completions within Willow.
MODEL = "gpt-4o-mini"


def init_convo(message: str = "you are a helpful assistant") -> List[dict]:
    """Return a new conversation seeded with a system prompt."""
    return [{"role": "system", "content": message}]


def new_message(message: str, conversation: List[dict], model: str = "") -> Tuple[List[dict], str]:
    """Send ``message`` to the model and append the reply to ``conversation``.

    Parameters
    ----------
    message:
        The user's message to send.
    conversation:
        The conversation history to append to.
    model:
        Identifier of the OpenAI model to use.
    """
    if not model:
        raise Exception("must have model")

    conversation.append({"role": "user", "content": message})
    response = _client.chat.completions.create(model=model, messages=conversation)
    reply = response.choices[0].message.content
    conversation.append({"role": "assistant", "content": reply})
    return conversation, reply


def chat(prompt: str, convo: List[dict] | None = None, model: str = MODEL) -> Tuple[List[dict], str]:
    """Given a prompt, return an updated conversation and the model's reply."""
    if convo is None:
        convo = copy(CONVO)
    convo, reply = new_message(prompt, convo, model=model)
    return convo, reply


__all__ = ["chat", "init_convo", "new_message", "FP_API_KEY"]
