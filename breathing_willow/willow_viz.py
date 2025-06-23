# willow_growth_v5.py

import hashlib
import json
import re
from pathlib import Path

import gensim
from gensim import corpora
from gensim.models import TfidfModel
import networkx as nx
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

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
            self.graph = nx.Graph()
        self.tfidf_model = None
        self.dictionary = None

    def load_graph(self):
        data = json.loads(self.graph_path.read_text())
        self.graph = nx.node_link_graph(data)

    def save_graph(self):
        data = nx.node_link_data(self.graph)
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
        tokenized = [self.tokenize(t) for t in texts]
        self.dictionary = corpora.Dictionary(tokenized)
        corpus = [self.dictionary.doc2bow(t) for t in tokenized]
        self.tfidf_model = TfidfModel(corpus)
        return corpus

    def submit_document(self, doc_path):
        text = Path(doc_path).read_text()
        uid = self._hash_content(text)[:8]
        tokens = self.tokenize(text)
        if not tokens:
            print(f"⚠️ Document {doc_path} has no tokens — skipping.")
            return

        if not self.dictionary:
            self.train_tfidf([text])

        bow = self.dictionary.doc2bow(tokens)
        tfidf = self.tfidf_model[bow]

        if tfidf:
            top_terms = sorted(tfidf, key=lambda x: -x[1])[:20]
            terms = [self.dictionary[id] for id, _ in top_terms]
        else:
            # fallback: raw word count
            freq = {}
            for t in tokens:
                freq[t] = freq.get(t, 0) + 1
            terms = sorted(freq, key=freq.get, reverse=True)[:20]

        self.graph.add_node(
            uid,
            path=str(doc_path),
            terms=terms,
            sentence="",
            paragraph="",
            shaped=False,
        )

        for nid, data in self.graph.nodes(data=True):
            if nid == uid:
                continue
            common = set(terms) & set(data.get('terms', []))
            if common:
                self.graph.add_edge(uid, nid, weight=len(common))

        self.save_graph()
        print(f"🔄 {doc_path} → {uid} — {len(terms)} terms extracted.")

    def shape_node(self, uid, sentence="", paragraph=""):
        if uid not in self.graph:
            print(f"⚠️ Node {uid} not found.")
            return
        self.graph.nodes[uid]["sentence"] = sentence
        self.graph.nodes[uid]["paragraph"] = paragraph
        self.graph.nodes[uid]["shaped"] = True
        self.save_graph()
        print(f"✏️ Node {uid} shaped.")

    def visualize(self, output='willow_net.html', max_words: int = 150):
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
                    "shape": "circle",
                    "size": 5,
                    "color": {"background": "white", "border": "white"},
                    "font": {"color": "white", "size": 8}
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
            word_counts = {}
            for _, data in self.graph.nodes(data=True):
                for w in data.get('terms', []):
                    word_counts[w] = word_counts.get(w, 0) + 1

            top_words = sorted(word_counts.items(), key=lambda x: x[1], reverse=True)[:max_words]
            allowed = {w for w, _ in top_words}

            word_edges = {}
            for _, data in self.graph.nodes(data=True):
      
                for i in range(len(terms)):
                    for j in range(i + 1, len(terms)):
                        pair = tuple(sorted((terms[i], terms[j])))
                        word_edges[pair] = word_edges.get(pair, 0) + 1

            for word, count in top_words:
                net.add_node(word, label=f"{word} ({count})", value=count)

            for (a, b), weight in word_edges.items():
                if a in allowed and b in allowed:
                    net.add_edge(a, b, value=weight)


            if len(self.graph.nodes) == 0:
                print("⚠️ Graph empty — nothing to render.")
            else:
                net.write_html(output, open_browser=False)
                print(f"🌱 Visualization saved to {output}")
        except ImportError:
            print("⚠️ pyvis not installed — skipping visualization.")

    def cluster_terms(self, max_clusters: int = 5) -> list[list[str]]:
        """Return conceptual clusters of the current network."""
        texts = [" ".join(data.get("terms", [])) for _, data in self.graph.nodes(data=True)]
        if not texts:
            return []

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

    def submit_docs(self, files):
        for fp in files:
            self.submit_document(fp)
        self.visualize('/l/tmp/willow-net.html')

