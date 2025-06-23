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
$ cd breathing-willow

# install in editable mode
$ pip install -e .

# if you plan to run the development helper script
$ bash zzero-dev-env-setup.sh
```

There are no strict dependencies yet, but a Python environment with `pytest` installed will help you run future tests.
## CLI Quick Start

You can check the project version with:

```bash
breathing-willow version
```

## Documentation

The project now ships with a lightweight docs site powered by MkDocs.
Install the docs requirements once with:

```bash
pip install -r requirements-docs.txt
```

Then preview the docs locally with our CLI:

```bash
breathing-willow docs
```

This command runs `mkdocs serve` with live reload and prints the local URL
(`http://127.0.0.1:8000`) so you can open it in your browser.

## Shaping Log and Points

Running `breathing-willow update-net -f <file>` ingests a document into the
word network. Each run prepends an entry to the shaping log located at
`/l/obs-chaotic/willow-shaping.md` (or the path from the `WILLOW_SHAPING_LOG`
environment variable). The entry records the file path, any UUIDs found, a tag
cloud, and a points value. Points accumulate across runs and increase when new
unique words appear. The network visualization grows as more unique terms are
added, showing word nodes with a count label.
## What to Try Next

- Sketch your own agent by adding a simple Python script in `agents/` (directory coming soon).
- Experiment with the dev helper script to see the workflow.
- Open issues or pull requests with ideas, questions, or early contributions.
- Track codex prompts by running `breathing-willow log-prompt`.
- Mark quick vc steps with `breathing-willow vc-step "your note"`.

We welcome experimentation. This repo is a space to iterate quickly and capture useful patterns. Let’s keep the energy flowing!

