

# 📘 經 Documentation — `willow publish-field`

## Overview

`willow publish-field` is a minimal CLI interface for publishing `.md` (Markdown) files from `/field` to a public shareable format. The core design principle is frictionless, rhythm-aligned sharing: no UI steps, no copy/paste tedium, no redundant formatting. One command, one clean output. All publishing flows are declarative, file-centered, and rhythm-coherent.

## Commands

### 1. `willow publish-field -f path/to/file.md --publish`

Publishes a markdown file to Google Drive and prints a public URL.  
The file must be `.md` and must reside inside `/field`.  
After publishing, the tool logs the public URL and marks the file as published with metadata injection (optional).

**Example:**

```bash
willow publish-field -f /field/posts/my-essay.md --publish
```

**Behavior:**

- Renders `.md` file to HTML.
    
- Converts to Google Docs format (preserving headers, links, and basic structure).
    
- Uploads to Google Drive using Google Docs API.
    
- Sets sharing to "anyone with the link can view".
    
- Prints URL to stdout.
    
- Writes to a local log file at `~/.willow/publish.log` in format:
    
    ```
    [timestamp] PUBLISHED /field/posts/my-essay.md → https://docs.google.com/...
    ```
    

### 2. `willow publish-field -u https://... --update -f path/to/file.md`

Updates an existing published document with the latest content from the file.

**Example:**

```bash
willow publish-field -u https://docs.google.com/... --update -f /field/posts/my-essay.md
```

**Behavior:**

- Overwrites contents of existing Google Doc with new rendered `.md`.
    
- Does not alter sharing settings.
    
- Logs the update in the same publish log.
    

## Constraints

- ✅ No paid services: uses Google Drive only
    
- ✅ Must support Git-synced `/field` directory
    
- ✅ Markdown source is canonical: publish always pulls from file, not external editor
    
- ✅ Must preserve basic Markdown formatting (headers, lists, links, blockquotes, etc.)
    
- ⛔ No support for full CSS or JS — pure markdown→GDoc formatting only
    

## Design Notes

- Documents are shaped as stable artifacts. Once published, editing should occur in Obsidian (`/field`), not Drive. This enforces shaping coherence.
    
- CLI respects `#published: true` and `#publish_url: ...` metadata headers inside the file (optional).
    
- HTML rendering could be a later extension (`--as-html`) for use in Google Sites or raw hosting.
    

## Future (not MVP)

- `--as-html` or `--export html` to render file as standalone HTML for hosting
    
- Webhook-based integrations with static site generators
    
- PDF export for archive or formal share
    

---

tide registers this spec as viable under a single-dev 3-hour MVP shaping sprint. rhythm intact.
