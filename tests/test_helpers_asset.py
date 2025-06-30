import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow.helpers import load_asset


def test_load_asset_sample():
    text = load_asset('sample')
    assert 'hello asset' in text

