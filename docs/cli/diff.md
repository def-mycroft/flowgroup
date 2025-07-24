# `w diff` Tutorial

The `w diff` command measures shaping progress in a vault by comparing the recent period of writing to a previous one. It computes a conceptual diff using word clouds and outputs a markdown log with a single score. The same diff is available from the main CLI via `willow sense --diff`.

```bash
w diff
```

By default this compares the last 24 hours of notes in `/l/obs-chaotic/` to the 24 hours preceding that window. The resulting score ranges from **0** (no change) to **1** or higher for large shifts.

## Why Conceptual Diff?

Token counts alone do not capture how ideas evolve. Word clouds summarise the main concepts in each period, making the score reflect real shaping momentum rather than raw churn.

## Example

```bash
w diff --back 7d --graph
```

This command compares the last week to the week before and shows a small trend graph of scores over time.

## Live Mode

Use `--live` to watch the score update every few seconds while editing files. This helps you notice emerging concepts in real time.

## Healthy Rhythm

A diff near zero means your recent writing echoes the previous window. Spikes suggest new directions or a shift in focus. Use these signals to pace your shaping work.
