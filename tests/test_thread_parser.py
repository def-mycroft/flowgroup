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
    html = ThreadParser(thread, export_id="exp").parse()
    expected_date = datetime.fromtimestamp(ts).date().isoformat()
    assert f"<div class=\"date\">{expected_date}</div>" in html
    assert "<div class=\"user-turn\">" in html
    assert "<div class=\"assistant-turn\">" in html


def test_skip_malformed_entries():
    thread = [
        {"author": "assistant", "content": ""},
        {"content": "no author"},
        {"author": "user", "content": "good"},
    ]
    html = ThreadParser(thread, export_id="exp").parse()
    assert html.count("user-turn") == 1


def test_json_string_input():
    data = json.dumps([
        {"author": "user", "content": "x"},
        {"author": "assistant", "content": "y"},
    ])
    html = ThreadParser(data, export_id="exp").parse()
    assert "user-turn" in html and "assistant-turn" in html

