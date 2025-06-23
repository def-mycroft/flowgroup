# willow_growth_v4.py

import hashlib
import json
import re
from pathlib import Path

import gensim
from gensim import corpora
from gensim.models import TfidfModel
import networkx as nx

try:
    import nltk
    nltk.download('punkt', quiet=True)
    from nltk.tokenize import word_tokenize
except ImportError:
    word_tokenize = None

class WillowGrowth:
    def __init__(self, graph_path='willow_growth_v4.json'):
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
            return [w.lower() for w in word_tokenize(text) if w.isalpha()]
        else:
            return re.findall(r'\b[a-z]{2,}\b', text.lower())

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

    def visualize(self, output='willow_net.html'):
        try:
            from pyvis.network import Network
            net = Network(height='600px', width='100%', notebook=False)
            net.barnes_hut(
                gravity=-1200,
                spring_length=200,
                spring_strength=0.01,
                damping=0.9,
            )
            net.set_options("""
                var options = {
                  "nodes": {
                    "shape": "box",
                    "color": {"background": "white", "border": "black"},
                    "font": {"color": "black"}
                  },
                  "edges": {
                    "color": {"color": "black"}
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
                if data.get('shaped'):
                    label = data.get('sentence', nid)
                else:
                    label = f"{nid}\n{','.join(data.get('terms', [])[:5])}"
                net.add_node(nid, label=label)
            for u, v, d in self.graph.edges(data=True):
                net.add_edge(u, v, value=d.get('weight', 1))
            if len(self.graph.nodes) == 0:
                print("⚠️ Graph empty — nothing to render.")
            else:
                net.write_html(output, open_browser=False)
                print(f"🌱 Visualization saved to {output}")
        except ImportError:
            print("⚠️ pyvis not installed — skipping visualization.")

    def submit_docs(self, files):
        for fp in files:
            self.submit_document(fp)
        self.visualize('/l/tmp/willow-net.html')

