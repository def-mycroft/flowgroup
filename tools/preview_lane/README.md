# Preview Lane Safety Checks

This directory documents how to inspect `.mfme/out/` artifacts without promoting them beyond the internal sushi ritual lane.

1. **Stay Local** – Only open generated files from within this repository checkout. Do not copy artifacts to synced cloud folders or paste raw content into external tools.
2. **Use Read-Only Viewers** – Prefer `less`, `cat`, or IDE preview panes. Avoid editors that auto-save or normalize line endings on open.
3. **Checksum Before Sharing** – When collaborating, share the receipt JSON that already contains SHA-256 hashes instead of the raw artifact.
4. **Redact Secrets** – If an artifact might contain account data, mask the sensitive fields before exporting summaries.
5. **Log Your Inspection** – Append notes to `.mfme/out/logs/preview_lane.log` so future reviewers know what was reviewed.

Suggested quick-check commands:

```bash
# summarize recent bootstrap run outputs
./.mfme/bin/run_sushi.sh

tree .mfme/out -L 2
cat .mfme/out/state/debrief.sushi-20250921-0215.md
```

All preview-lane activities remain reversible until the kernel graduates to a public release channel.
