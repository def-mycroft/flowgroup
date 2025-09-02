import json
import zipfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow.export_kernel import ChatExportArchiver
# To use NLTK stopwords during tests, ensure corpora are installed:
# from breathing_willow import setup_nltk
# setup_nltk()


def make_conversation():
    return {
        "mapping": {
            "client-created-root": {
                "id": "client-created-root",
                "parent": None,
                "children": ["abc"],
            },
            "abc": {
                "id": "abc",
                "parent": "client-created-root",
                "children": ["def"],
                "message": {
                    "author": {"role": "user"},
                    "content": {"parts": ["hello"]},
                    "create_time": 1700000000,
                },
            },
            "def": {
                "id": "def",
                "parent": "abc",
                "children": [],
                "message": {
                    "author": {"role": "assistant"},
                    "content": {"parts": ["hi"]},
                },
            },
        }
    }


def test_archiver_conversations_json(tmp_path: Path):
    convo = [make_conversation()]
    convo_path = tmp_path / "conversations.json"
    convo_path.write_text(json.dumps(convo))

    zip_path = tmp_path / "exp.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.write(convo_path, arcname="conversations.json")

    out_dir = tmp_path / "out"
    arch = ChatExportArchiver(zip_path, out_dir)
    arch.run()

    files = list(out_dir.glob("*.html"))
    assert (out_dir / "001-conversation.html").exists()
    assert (out_dir / "index.html").exists()
    text = (out_dir / "001-conversation.html").read_text()
    assert "user-turn" in text
    assert "assistant-turn" in text


def test_archiver_index_page(tmp_path: Path):
    convo1 = make_conversation()
    convo2 = make_conversation()
    convo2["mapping"]["abc"]["message"]["content"]["parts"] = ["second thread"]
    convo2["mapping"]["abc"]["message"]["create_time"] = 1700001000
    convos = [convo1, convo2]
    convo_path = tmp_path / "conversations.json"
    convo_path.write_text(json.dumps(convos))

    zip_path = tmp_path / "exp.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.write(convo_path, arcname="conversations.json")

    out_dir = tmp_path / "out"
    arch = ChatExportArchiver(zip_path, out_dir)
    arch.run()

    index_file = out_dir / "index.html"
    assert index_file.exists()
    index_text = index_file.read_text()
    assert "001-conversation.html" in index_text
    assert "002-conversation.html" in index_text
    assert "hello" in index_text
    assert "second thread" in index_text

