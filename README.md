# Breathing Willow

**Breathing Willow** is an experimental playground for rapid prototyping of flow-based AI agent systems. We are currently in phase **L0.1**, focusing on setting up the repo, bootstrapping agent definitions, and sketching out the CLI.

## Purpose

This project explores how lightweight agent tools can be composed into flexible flows. The goal is to make it easy to mix and match small utilities into powerful pipelines, eventually leading to an approachable framework for building AI-driven tasks.

## Looking Ahead to 1.0

Version 1.0 aims to provide:

- A streamlined CLI that can define and execute complex agent flows.
- Modular agent definitions with reusable templates.
- Extensible hooks for data ingestion and output formatting.
- Examples and guides to help contributors craft their own flow-based workflows.

We are not there yet, but each commit brings us closer.

## Early Setup

This repo is still rough around the edges. To get started:

```bash
# clone the repo
# (replace with your fork or clone path)
$ git clone <repo-url>
$ cd flowgroup

# install in editable mode
$ pip install -e .

# if you plan to run the development helper script
$ bash zzero-dev-env-setup.sh
```

There are no strict dependencies yet, but a Python environment with `pytest` installed will help you run future tests.

If you plan to use features that depend on NLTK tokenizers or stopwords, run the
setup helper once to download the required corpora:

```bash
python -c "from breathing_willow import setup_nltk; setup_nltk()"
```

The corpora are stored locally so subsequent runs work offline.
## CLI Quick Start

You can check the project version with:

```bash
willow version
```

Run `willow -h` to see a list of commands. Each subcommand exposes a focused
utility:

- `ccraft` – slice and prepare context windows for shaping.
- `sense` – surface recent changes with conceptual diffs.
- `module-prompt133` – bootstrap prompt stubs for the 133 process.
- `log-prompt` – append entries to the prompt log for later review.
- `history` – parse ChatGPT export files into Markdown archives.
- `vc-step` – record a short version-control loop note.
- `docs` – build and preview this documentation site with live reload.
- `update-net` – ingest a document into the network graph and render it.
- `publish-field` – publish a Markdown file to Google Docs or update an
  existing document.
- `snip-file` – trim a text file down to a manageable token count.
- `promptdev-bootstrap` – run a three-step shaping routine based on a seed
  prompt.

These commands are lightweight by design. They can be chained together or used
individually, depending on what you want to explore.

## Documentation

The project now ships with a lightweight docs site powered by MkDocs.
Install the docs requirements once with:

```bash
pip install -r requirements-docs.txt
```

Then preview the docs locally with our CLI:

```bash
willow docs
```

This command runs `mkdocs serve` with live reload and prints the local URL
(`http://127.0.0.1:8000`) so you can open it in your browser.

## Shaping Log and Points

Running `willow update-net -f <file>` ingests a document into the
document network. Each run prepends an entry to the shaping log located at
`/l/obs-chaotic/willow-shaping.md` (or the path from the `WILLOW_SHAPING_LOG`
environment variable).

On first run the graph contains one summary node per document. Each node shows
around five keyword clusters (about twenty-five words total) to keep the
visualization simple. Call `WillowGrowth.expand_node()` later to link in
similar documents based on token overlap.
## What to Try Next

- Sketch your own agent by adding a simple Python script in `agents/` (directory coming soon).
- Experiment with the dev helper script to see the workflow.
- Open issues or pull requests with ideas, questions, or early contributions.
- Track codex prompts by running `willow log-prompt`.
- Mark quick vc steps with `willow vc-step "your note"`.

We welcome experimentation. This repo is a space to iterate quickly and capture useful patterns. Let’s keep the energy flowing!



