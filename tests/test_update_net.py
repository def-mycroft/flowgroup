import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import breathing_willow.willow_viz as willow_viz
# To run with full tokenization, ensure NLTK corpora are installed:
# from breathing_willow import setup_nltk
# setup_nltk()

# Avoid NLTK downloads during tests
willow_viz.word_tokenize = None

from breathing_willow.willow_viz import WillowGrowth


def create_initial_graph(tmp_path: Path) -> Path:
    gpath = tmp_path / "graph.json"
    wg = WillowGrowth(graph_path=gpath)
    doc1 = tmp_path / "doc1.txt"
    doc1.write_text("alpha beta gamma")
    wg.submit_document(doc1)
    return gpath


def test_update_net(tmp_path: Path):
    graph_path = create_initial_graph(tmp_path)
    before_nodes = set(WillowGrowth(graph_path=graph_path).graph.nodes)

    doc2 = tmp_path / "doc2.txt"
    doc2.write_text("alpha delta")
    output_html = tmp_path / "out.html"

    from breathing_willow_cli.breathing_willow import main as cli_main

    argv = [
        "update-net",
        "--graph",
        str(graph_path),
        "--visual-archive",
        str(output_html),
        "-f",
        str(doc2),
    ]
    cli_main(argv)

    wg = WillowGrowth(graph_path=graph_path)
    after_nodes = set(wg.graph.nodes)

    # graph should grow by one node
    assert len(after_nodes) == len(before_nodes) + 1

    new_nodes = after_nodes - before_nodes
    assert len(new_nodes) == 1
    new_node = next(iter(new_nodes))

    # new node should have at least one edge (shared term 'alpha')
    assert len(list(wg.graph.edges(new_node))) >= 1

    # visualization written
    assert output_html.exists()
