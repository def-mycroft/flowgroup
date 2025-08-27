from __future__ import annotations

"""Wrap Vism implementation.

This module defines the ``wrap`` vism which transforms a :class:`Payload` into
an :class:`Envelope`.  The implementation follows the specification embedded in
the original prompt and provides a small CLI along with property-based checks.
"""

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Generic, Optional, Sequence, TypeVar
import argparse
import base64
import datetime as _dt
import hashlib
import json
import sys
import uuid
from contextlib import contextmanager


T = TypeVar("T")


@dataclass
class Outcome(Generic[T]):
    """Result of applying a vism."""

    ok: bool
    value: Optional[T] = None
    error: Optional[str] = None
    receipts: Dict[str, Any] = field(default_factory=dict)


@dataclass
class Payload:
    """Input payload for the ``wrap`` vism."""

    bytes_: bytes
    media_type: str
    source: str


@dataclass
class Envelope:
    """Output envelope produced by the ``wrap`` vism."""

    id: str
    content_hash: str
    created_at: str
    media_type: str
    source: str


class CryptoPort:
    """Cryptographic utilities exposed to the vism."""

    def sha256(self, data: bytes) -> str:
        return hashlib.sha256(data).hexdigest()

    def uuidv7(self) -> str:
        # Python <3.11 has no ``uuid7``; fall back to uuid4 for uniqueness.
        return uuid.uuid7().hex if hasattr(uuid, "uuid7") else uuid.uuid4().hex


class ClockPort:
    """Time-related utilities."""

    def now(self) -> _dt.datetime:
        return _dt.datetime.utcnow()


class TelemetryPort:
    """Minimal telemetry span manager."""

    @contextmanager
    def span(self, name: str, **fields: Any):
        yield


@dataclass
class Context:
    """Aggregates ports available to a vism."""

    crypto: CryptoPort
    clock: ClockPort
    telemetry: TelemetryPort


class Vism(Generic[T]):
    """Base class for all visms."""

    name: str
    version: str

    def __init__(self, ctx: Context):
        self.ctx = ctx

    def apply(self, inp: Any) -> Outcome[T]:  # pragma: no cover - abstract
        raise NotImplementedError


class WrapVism(Vism[Envelope]):
    """Wrap a :class:`Payload` into an :class:`Envelope`."""

    name = "wrap"
    version = "0.1.0"

    def apply(self, payload: Payload) -> Outcome[Envelope]:
        with self.ctx.telemetry.span(
            "wrap.apply", source=payload.source, media_type=payload.media_type
        ):
            if not payload.bytes_:
                return Outcome(False, error="empty_payload")

            h = self.ctx.crypto.sha256(payload.bytes_)
            eid = self.ctx.crypto.uuidv7()
            ts = self.ctx.clock.now().isoformat()

            envelope = Envelope(eid, h, ts, payload.media_type, payload.source)
            return Outcome(
                True,
                value=envelope,
                receipts={"content_hash": h, "created_at": ts},
            )


def _payload_from_json(data: Dict[str, Any]) -> Payload:
    """Construct a :class:`Payload` from JSON data (base64 bytes)."""

    raw = base64.b64decode(data["bytes_"])
    return Payload(bytes_=raw, media_type=data["media_type"], source=data["source"])


def _outcome_to_json(out: Outcome[Envelope]) -> Dict[str, Any]:
    return {
        "ok": out.ok,
        "value": asdict(out.value) if out.value else None,
        "error": out.error,
        "receipts": out.receipts,
    }


def default_context() -> Context:
    """Create a default :class:`Context` instance."""

    return Context(crypto=CryptoPort(), clock=ClockPort(), telemetry=TelemetryPort())


REGISTRY: Dict[str, Vism[Any]] = {
    "wrap": WrapVism(default_context()),
}


def hash_is_deterministic() -> None:
    ctx = default_context()
    vism = WrapVism(ctx)
    payload = Payload(b"abc", "text/plain", "unit-test")
    out1 = vism.apply(payload)
    out2 = vism.apply(payload)
    expected = ctx.crypto.sha256(b"abc")
    assert out1.ok and out2.ok
    assert out1.value.content_hash == out2.value.content_hash == expected
    print("hash_is_deterministic: ok")


def rejects_empty() -> None:
    ctx = default_context()
    vism = WrapVism(ctx)
    payload = Payload(b"", "text/plain", "unit-test")
    out = vism.apply(payload)
    assert not out.ok and out.error == "empty_payload"
    print("rejects_empty: ok")


def run_properties() -> None:
    hash_is_deterministic()
    rejects_empty()


def cli(argv: Sequence[str] | None = None) -> None:
    """Command line interface for running visms."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--vism")
    parser.add_argument("--input")
    parser.add_argument("--properties", action="store_true", help="run property checks")
    args = parser.parse_args(argv)

    if args.properties:
        run_properties()
        return

    if args.vism:
        vism_name = args.vism
        payload_data = json.loads(args.input) if args.input else json.loads(sys.stdin.read())
    else:
        data = json.loads(sys.stdin.read())
        vism_name = data["vism"]
        payload_data = data["input"]

    vism = REGISTRY.get(vism_name)
    if vism is None:
        raise SystemExit(f"unknown vism '{vism_name}'")

    if vism_name == "wrap":
        payload = _payload_from_json(payload_data)
        out = vism.apply(payload)
        print(json.dumps(_outcome_to_json(out)))
    else:
        raise SystemExit(f"unsupported vism '{vism_name}'")


if __name__ == "__main__":
    cli()

