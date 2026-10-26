---
name: handoff
description: Compact the current conversation into a handoff document for another agent to pick up. Use when transitioning work to a fresh session or another agent.
license: MIT
metadata:
  author: bpappin
  version: "1.3"
  argument-hint: "What will the next session be used for?"
---

# Handoff

Write a handoff document summarising the current conversation so a fresh agent can continue the work.

**Save it to the operating system's temporary directory - never the workspace, and never a tracker comment.** A handoff is written while the conversation is still in context, which is when names, credentials and verbatim quotes slip into a file; in the repo it gets committed, and on an issue in a public repository it is world-readable the moment it posts. It is also a snapshot that goes stale by the next session. What should outlive the session already has a home on the story - AC ticked, discovered work filed as linked issues through story-workflow. Create the file portably (macOS/BSD `mktemp -t` mangles extensions, so don't use it):

```sh
f="$(mktemp "${TMPDIR:-/tmp}/handoff-XXXXXX")" && mv "$f" "$f.md"
```

Read the file before you write to it, and tell the user the final path.

**Content rules:**

- Include a "Suggested skills" section naming the skills the next session should use.
- Do not duplicate content already captured in other artifacts (PRDs, plans, ADRs, stories, commits, diffs) - reference them by ID, path, or URL instead. In particular, never restate the story's AC checklist: the story itself is the source of truth for progress.
- Redact anything sensitive: API keys, tokens, passwords, personally identifiable information.
- If the user passed arguments, treat them as a description of what the next session will focus on and tailor the doc accordingly.
