# Using `willow agentic --load-clipboard-agent`

This command retrieves a registered clipboard agent by matching a partial UUID in its name, then writes the agent’s full Markdown context to a specified output file. You can then copy and paste the file’s content directly into any LLM session, or use it for offline reference.

## 1. Command Syntax

```bash
willow agentic --load-clipboard-agent <agent id> -o <output path>
```

**Arguments:**

* `<agent id>` — A partial UUID string from the agent’s name (e.g., `85165e39`).
* `-o <output path>` — The path where the agent’s Markdown context will be saved.

## 2. Example

Assume your `~/.willow/agents.json` contains:

```json
{
  "clipboard agent test cresting-cloud 85165e39": "/field/clipboard agent test cresting-cloud 85165e39.md"
}
```

Running:

```bash
willow agentic --load-clipboard-agent 85165e39 -o /tmp/myagent-context.md
```

Will:

1. Search all registered agent names for a substring match with `85165e39`.
2. Confirm there is **exactly one** match; otherwise, raise an error.
3. Copy the source file (`/field/clipboard agent test cresting-cloud 85165e39.md`) to `/tmp/myagent-context.md`.

## 3. Output

After success, you’ll see:

```
Loaded Clipboard Agent 'clipboard agent test cresting-cloud 85165e39'
Context written to /tmp/myagent-context.md
```

You can now open `/tmp/myagent-context.md` and paste its contents into any LLM prompt. Whatever is in the Markdown file becomes the injected context.

## 4. Error Handling

* **No Matches** — If no agent name contains the `<agent id>` substring, an error is displayed and no file is written.
* **Multiple Matches** — If more than one agent matches the substring, an error is displayed prompting you to use a longer identifier.

## 5. Tips

* Use a longer `<agent id>` substring if you have many agents with similar names.
* You can use the `cat ~/.willow/agents.json` command to inspect all available agents and their stored paths.
