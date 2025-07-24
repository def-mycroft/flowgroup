# `willow` Command Overview

`willow` is the main entry point to the Breathing Willow toolkit. The CLI wraps a
collection of small utilities that can be mixed and matched as you explore agent
workflows. Running `willow -h` prints a concise list of commands and options.

```bash
python -m breathing_willow_cli.breathing_willow -h
```

The output looks like this:

```
usage: breathing-willow [-h] [--version]
                        {ccraft,sense,module-prompt133,log-prompt,history,vc-step,docs,update-net,publish-field,snip-file,promptdev-bootstrap} ...
```

Below is a quick summary of what each subcommand does.

## Subcommands

### `ccraft`
Tools for slicing context windows and preparing them for shaping.

### `sense`
Optionally run a conceptual diff to surface how recent writing differs from
earlier work. Pass `--diff` to generate a log via the `w diff` utility.

### `module-prompt133`
Bootstrap the files needed for the "133" prompt development cycle.

### `log-prompt`
Record a codex prompt entry in `meta/prompt-log.md`.

### `history`
Convert ChatGPT export files into a tidy Markdown archive.

### `vc-step`
Append a short note about your current version-control loop.

### `docs`
Build and serve the MkDocs documentation with live reload.

### `update-net`
Ingest a document into the Willow graph and render a visualization.
Snapshots of the source can be stored alongside the file.

### `publish-field`
Publish a Markdown file from `/field` to Google Docs or update an existing
document via its share URL.

### `snip-file`
Truncate a text file to the last set of useful tokens. Handy for keeping prompts
under model limits.

### `promptdev-bootstrap`
Run a three-stage shaping routine starting from a seed prompt. This can generate
objective and value drafts, surface key phrases, and package surfacings for
comparison.

Use these commands individually or chain them together as your workflow evolves.
