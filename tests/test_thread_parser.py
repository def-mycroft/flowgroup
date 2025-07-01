import json
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow.export_kernel import ThreadParser
from datetime import datetime


def test_simple_thread_parsing():
    ts = 1700000000
    thread = {
        "create_time": ts,
        "messages": [
            {"author": "user", "content": "hello"},
            {"author": "assistant", "content": "hi"},
        ],
    }
    md = ThreadParser(thread).parse()
    expected_date = datetime.fromtimestamp(ts).date().isoformat()
    assert f"date: {expected_date}" in md
    assert "## zero:" in md
    assert "## tide:" in md


def test_skip_malformed_entries():
    thread = [
        {"author": "assistant", "content": ""},
        {"content": "no author"},
        {"author": "user", "content": "good"},
    ]
    md = ThreadParser(thread).parse()
    lines = [l for l in md.splitlines() if l.startswith("##")]
    assert lines == ["## zero:"]


def test_json_string_input():
    data = json.dumps([
        {"author": "user", "content": "x"},
        {"author": "assistant", "content": "y"},
    ])
    md = ThreadParser(data).parse()
    assert "## zero:" in md and "## tide:" in md

