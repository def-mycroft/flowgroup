import re
from pathlib import Path
from datetime import datetime, timedelta, timezone
import pytest

from w_cli import diff as wdiff


def test_word_cloud_stability():
    text = "alpha beta gamma"
    c1 = wdiff.word_cloud(text)
    c2 = wdiff.word_cloud(text)
    assert c1 == c2


def test_word_cloud_small_edit():
    base = "alpha beta gamma"
    c1 = wdiff.word_cloud(base)
    c2 = wdiff.word_cloud(base + " gamma")
    assert set(c1) == set(c2)


def test_diff_metric_zero():
    cloud = {"a":1.0, "b":0.5}
    assert wdiff._jaccard(cloud, cloud) == 0.0


def test_parse_duration():
    assert wdiff.parse_duration("2h") == timedelta(hours=2)
    assert wdiff.parse_duration("3d") == timedelta(days=3)


def test_cli_help(capsys):
    parser = wdiff.build_parser()
    with pytest.raises(SystemExit):
        parser.parse_args(["diff", "--help"])
    help_text = capsys.readouterr().out
    assert "w diff" in help_text
    assert "--window" in help_text


def test_find_modified_texts_root_missing():
    start = datetime.now(timezone.utc) - timedelta(hours=1)
    end = datetime.now(timezone.utc)
    with pytest.raises(FileNotFoundError):
        wdiff.find_modified_texts("/does/not/exist", start, end)


def test_find_modified_texts_max_depth(tmp_path):
    (tmp_path / "a.txt").write_text("hi")
    sub = tmp_path / "sub"
    sub.mkdir()
    (sub / "b.txt").write_text("hi")
    start = datetime.now(timezone.utc) - timedelta(hours=1)
    end = datetime.now(timezone.utc)
    paths = wdiff.find_modified_texts(str(tmp_path), start, end, max_depth=0)
    assert (tmp_path / "a.txt") in paths
    assert (sub / "b.txt") not in paths


def test_find_modified_texts_exclude(tmp_path):
    keep = tmp_path / "keep.txt"
    keep.write_text("hi")
    skip = tmp_path / "skip.txt"
    skip.write_text("hi")
    start = datetime.now(timezone.utc) - timedelta(hours=1)
    end = datetime.now(timezone.utc)
    paths = wdiff.find_modified_texts(str(tmp_path), start, end, exclude=["*/skip.txt"])
    assert keep in paths
    assert skip not in paths
