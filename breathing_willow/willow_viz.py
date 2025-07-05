# willow_growth_v5.py

import hashlib
import json
import re
from pathlib import Path

try:
    import gensim  # type: ignore
    from gensim import corpora  # type: ignore
    from gensim.models import TfidfModel  # type: ignore
except Exception:  # pragma: no cover - optional dependency
    gensim = None  # type: ignore

    class _DummyDictionary(dict):
        def __init__(self, texts=None):
            self.token2id = {}
            if texts:
                for tokens in texts:
                    for t in tokens:
                        if t not in self.token2id:
                            self.token2id[t] = len(self.token2id)

        def doc2bow(self, tokens):
            counts = {}
            for t in tokens:
                if t in self.token2id:
                    idx = self.token2id[t]
                    counts[idx] = counts.get(idx, 0) + 1
            return list(counts.items())

    class _DummyTFIDF:
        def __init__(self, corpus=None):
            pass

    corpora = type("corpora", (), {"Dictionary": _DummyDictionary})
    TfidfModel = _DummyTFIDF  # type: ignore

try:
    import networkx as nx  # type: ignore
except Exception:  # pragma: no cover - optional dependency
    nx = None  # type: ignore

    class _SimpleGraph:
        """Minimal fallback graph when networkx is unavailable."""

        def __init__(self) -> None:
            self._nodes: dict[str, dict] = {}
            self._edges: dict[str, dict[str, dict]] = {}

        # --- node helpers -------------------------------------------------
        def add_node(self, nid: str, **data) -> None:
            if nid not in self._nodes:
                self._nodes[nid] = {}
            self._nodes[nid].update(data)

        class _NodeView:
            def __init__(self, graph: "_SimpleGraph") -> None:
                self._g = graph

            def __call__(self, data: bool = False):
                return (
                    self._g._nodes.items() if data else self._g._nodes.keys()
                )

            def __iter__(self):
                return iter(self._g._nodes.keys())

            def __len__(self):
                return len(self._g._nodes)

        @property
        def nodes(self) -> "_SimpleGraph._NodeView":
            return _SimpleGraph._NodeView(self)

        # --- edge helpers -------------------------------------------------
        def add_edge(self, a: str, b: str, **data) -> None:
            self._edges.setdefault(a, {})[b] = data
            self._edges.setdefault(b, {})[a] = data

        def edges(self, nid: str | None = None):
            if nid is None:
                seen = set()
                for src, nbrs in self._edges.items():
                    for dst in nbrs:
                        if (dst, src) not in seen:
                            seen.add((src, dst))
                            yield (src, dst)
            else:
                for dst in self._edges.get(nid, {}):
                    yield (nid, dst)

    def _to_data(graph: _SimpleGraph) -> dict:
        return {
            "nodes": [
                {"id": nid, **attrs} for nid, attrs in graph.nodes(data=True)
            ],
            "edges": [
                {"source": a, "target": b, **graph._edges[a][b]}
                for a, b in graph.edges()
            ],
        }

    def _from_data(data: dict) -> _SimpleGraph:
        g = _SimpleGraph()
        for node in data.get("nodes", []):
            nid = node.pop("id")
            g.add_node(nid, **node)
        for edge in data.get("edges", []):
            src = edge.pop("source")
            dst = edge.pop("target")
            g.add_edge(src, dst, **edge)
        return g

try:
    from sklearn.feature_extraction.text import TfidfVectorizer  # type: ignore
    from sklearn.cluster import KMeans  # type: ignore
except Exception:  # pragma: no cover - optional dependency
    TfidfVectorizer = None  # type: ignore
    KMeans = None  # type: ignore

try:
    import nltk
    nltk.download('punkt', quiet=True)
    nltk.download('stopwords', quiet=True)
    from nltk.tokenize import word_tokenize
    from nltk.corpus import stopwords as nltk_stopwords
    STOP_WORDS = set(nltk_stopwords.words('english'))
except Exception:  # missing nltk or corpus
    word_tokenize = None
    STOP_WORDS = {
        'a', 'an', 'the', 'and', 'or', 'but', 'if', 'while', 'of', 'at', 'by',
        'for', 'with', 'about', 'against', 'between', 'into', 'through', 'during',
        'before', 'after', 'to', 'from', 'in', 'out', 'on', 'off', 'over', 'under',
        'again', 'further', 'then', 'once', 'here', 'there', 'all', 'any', 'both',
        'each', 'few', 'more', 'most', 'other', 'some', 'such', 'no', 'nor', 'not',
        'only', 'own', 'same', 'so', 'than', 'too', 'very', 'can', 'will', 'just'
    }

class WillowGrowth:
    def __init__(self, graph_path='willow_growth_v5.json'):
        self.graph_path = Path(graph_path)
        if self.graph_path.exists():
            self.load_graph()
        else:
            if nx is not None:
                self.graph = nx.Graph()
            else:
                self.graph = _SimpleGraph()
        self.tfidf_model = None
        self.dictionary = None

    def load_graph(self):
        data = json.loads(self.graph_path.read_text())
        if nx is not None:
            self.graph = nx.node_link_graph(data)
        else:
            self.graph = _from_data(data)

    def save_graph(self):
        if nx is not None:
            data = nx.node_link_data(self.graph)
        else:
            data = _to_data(self.graph)
        self.graph_path.write_text(json.dumps(data, indent=2))

    def _hash_content(self, text):
        return hashlib.sha256(text.encode('utf-8')).hexdigest()

    def tokenize(self, text):
        if word_tokenize:
            tokens = [w.lower() for w in word_tokenize(text) if w.isalpha()]
        else:
            tokens = re.findall(r'\b[a-z]{2,}\b', text.lower())
        return [t for t in tokens if t not in STOP_WORDS]

    def train_tfidf(self, texts):
        """Train TF-IDF on provided texts."""
        tokenized = [self.tokenize(t) for t in texts]
        self.dictionary = corpora.Dictionary(tokenized)
        corpus = [self.dictionary.doc2bow(t) for t in tokenized]
        self.tfidf_model = TfidfModel(corpus)
        return corpus

    def _summary_from_text(self, text: str) -> tuple[str, list[list[str]], list[str]]:
        """Return a ~25 word summary, clusters and tokens for a document."""
        sentences = [s.strip() for s in re.split(r"[.!?]+", text) if s.strip()]
        if not sentences:
            return "", [], []
        if TfidfVectorizer is None or KMeans is None:
            tokens = self.tokenize(text)
            summary = " ".join(tokens[:25])
            return summary, [], tokens

        n_clusters = min(5, len(sentences))
        vectorizer = TfidfVectorizer(stop_words="english")
        X = vectorizer.fit_transform(sentences)
        km = KMeans(n_clusters=n_clusters, n_init="auto", random_state=42)
        km.fit(X)
        terms = vectorizer.get_feature_names_out()
        clusters: list[list[str]] = []
        for i in range(n_clusters):
            center = km.cluster_centers_[i]
            top_idx = center.argsort()[-5:][::-1]
            clusters.append([terms[idx] for idx in top_idx])
        summary_words: list[str] = []
        for c in clusters:
            summary_words.extend(c)
        summary = " ".join(summary_words[:50])
        return summary, clusters, summary_words

    def submit_document(self, doc_path):
        """Ingest a document as a single summary node."""
        text = Path(doc_path).read_text()
        uid = self._hash_content(text)[:8]
        if not self.dictionary:
            self.train_tfidf([text])

        summary, clusters, tokens = self._summary_from_text(text)
        if not tokens:
            print(f"⚠️ Document {doc_path} has no tokens — skipping.")
            return

        self.graph.add_node(
            uid,
            path=str(doc_path),
            summary=summary,
            clusters=clusters,
            tokens=tokens,
        )

        for nid, data in self.graph.nodes(data=True):
            if nid == uid:
                continue
            common = set(tokens) & set(data.get('tokens', []))
            if common:
                self.graph.add_edge(uid, nid, weight=len(common))

        self.save_graph()
        print(f"🔄 {doc_path} → {uid} — {len(tokens)} summary tokens.")

    def shape_node(self, uid, sentence="", paragraph=""):
        if uid not in self.graph:
            print(f"⚠️ Node {uid} not found.")
            return
        self.graph.nodes[uid]["sentence"] = sentence
        self.graph.nodes[uid]["paragraph"] = paragraph
        self.graph.nodes[uid]["shaped"] = True
        self.save_graph()
        print(f"✏️ Node {uid} shaped.")

    def visualize(self, output='willow_net.html'):
        """Render the document graph."""
        try:
            from pyvis.network import Network
            net = Network(
                height='600px',
                width='100%',
                notebook=False,
                bgcolor='#000000',
                font_color='white'
            )
            net.barnes_hut(
                gravity=-1200,
                spring_length=200,
                spring_strength=0.01,
                damping=0.9,
            )
            net.set_options("""
                var options = {
                  "nodes": {
                    "shape": "dot",
                    "size": 10,
                    "color": {"background": "white", "border": "white"},
                    "font": {"color": "white", "size": 10}
                  },
                  "edges": {
                    "color": {"color": "rgba(255,255,255,0.3)"},
                    "width": 1
                  },
                  "physics": {
                    "enabled": true,
                    "barnesHut": {
                      "gravitationalConstant": -1200,
                      "springLength": 200,
                      "springConstant": 0.01,
                      "damping": 0.9
                    }
                  }
                }
            """)

            for nid, data in self.graph.nodes(data=True):
                label = (data.get('summary') or '')[:100]
                title = data.get('path', '')
                net.add_node(nid, label=label, title=title)

            for a, b, edata in self.graph.edges(data=True):
                net.add_edge(a, b, value=edata.get('weight', 1))

            if len(self.graph.nodes) == 0:
                print("⚠️ Graph empty — nothing to render.")
            else:
                net.write_html(output, open_browser=False)
                print(f"🌱 Visualization saved to {output}")
        except ImportError:
            print("⚠️ pyvis not installed — skipping visualization.")
            Path(output).write_text("pyvis not installed")

    def cluster_terms(self, max_clusters: int = 5) -> list[list[str]]:
        """Return conceptual clusters of the current network."""
        texts = [" ".join(data.get("tokens", [])) for _, data in self.graph.nodes(data=True)]
        if not texts:
            return []

        if TfidfVectorizer is None or KMeans is None:
            from collections import Counter
            counts = Counter(t for text in texts for t in text.split())
            if not counts:
                return []
            top = [w for w, _ in counts.most_common(5)]
            return [top]

        n_clusters = min(max(3, len(texts)), max_clusters)
        n_clusters = min(n_clusters, len(texts))

        vectorizer = TfidfVectorizer(stop_words="english")
        X = vectorizer.fit_transform(texts)
        if n_clusters == 0:
            return []
        km = KMeans(n_clusters=n_clusters, n_init="auto", random_state=42)
        km.fit(X)
        terms = vectorizer.get_feature_names_out()
        clusters: list[list[str]] = []
        for i in range(n_clusters):
            center = km.cluster_centers_[i]
            top_ids = center.argsort()[-5:][::-1]
            clusters.append([terms[idx] for idx in top_ids])
        return clusters

    def expand_node(self, uid: str, similarity_threshold: float = 0.3) -> None:
        """Add edges from ``uid`` to similar documents based on token overlap."""
        if uid not in self.graph:
            print(f"⚠️ Node {uid} not found for expansion.")
            return

        tokens = set(self.graph.nodes[uid].get("tokens", []))
        if not tokens:
            return

        for nid, data in self.graph.nodes(data=True):
            if nid == uid or self.graph.has_edge(uid, nid):
                continue
            other = set(data.get("tokens", []))
            if not other:
                continue
            overlap = len(tokens & other)
            sim = overlap / max(len(tokens), len(other))
            if sim >= similarity_threshold:
                self.graph.add_edge(uid, nid, weight=overlap)
        self.save_graph()

    def submit_docs(self, files):
        for fp in files:
            self.submit_document(fp)
        self.visualize('/l/tmp/willow-net.html')

