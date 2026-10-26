---
name: housekeeping
description: Perform workspace housekeeping, cleanup audits, orphaned project registry pruning, and prepare handoff/commit documentation at the end of a work session. Use when concluding tasks, preparing for commits, cleaning up ghost or temporary projects in Antigravity (agy) or Gemini, or before wrapping up/transitioning a chat.
license: MIT
metadata:
  author: bpappin
  version: "1.7"
---

# Housekeeping

## Quick start

When concluding a session: review modified files, clean up debug and orphaned
project artifacts, reconcile the work with the focused story, preserve
decisions, and prepare the Git commit message and PR description.

## Workflows

Execute the following steps in order at the end of a work session.

> [!IMPORTANT]
> **Execution Rule**: Before executing each step, ask the user if they want
> to run that specific step. If they decline, skip it and move immediately
> to the next step.

### Step 1: Code cleanliness & cleanup audit

Inspect the codebase and remove development artifacts before documenting:

- [ ] Remove temporary comments (`TODO: temporary fix`, personal notes).
- [ ] Remove development-only logs, print/console statements.
- [ ] Remove unused variables, imports, or dependencies.
- [ ] Clean up or delete scratch files created in the workspace.
- [ ] **Antigravity (`agy`) & Gemini ghost projects**: Test harnesses, probes,
      and subagent runs that open ephemeral directories (e.g., `mktemp` paths
      under `/tmp`, `/private/tmp`, or `/var/folders`) leave orphaned project
      registrations in `~/.gemini/config/projects/*.json` and
      `~/.gemini/projects.json` after the temporary directories are deleted.
      Run [scripts/prune-gemini-projects.py](scripts/prune-gemini-projects.py)
      (defaults to `--dry-run`) to audit entries whose `folderUri` paths no
      longer exist on disk, review the list with the user, and re-run with
      `--apply` to remove the orphaned entries (system sentinels such as
      `default-cli-project.json` and `outside-of-project.json` are preserved).

### Step 2: Environment & configuration check

Did the session introduce environment variables, configuration settings,
or dependencies?

- [ ] New environment variables documented (README, setup scripts, .env.example).
- [ ] Package manifests updated and dependencies documented.

### Step 3: Story reconciliation & remaining work

Compare the session's work against its objectives — which, when a story is
focused, means the story's AC checklist (story-workflow skill):

- [ ] AC items verifiably completed this session are toggled on the story.
- [ ] Deferred work, edge cases, and technical debt are logged as tracker
      issues — the discovered-work off-ramp for things found mid-session,
      or the to-issues skill for a planned batch. Never local TODO/gap files.
- [ ] If the story might be complete, run the completion check
      (story-workflow) — don't declare it done otherwise.

### Step 4: Context preservation & decisions

Preserve key discussions, setup steps, and architectural choices before
the conversation is lost. File per the project-docs taxonomy:

- [ ] Design decisions → the Architecture Decision Records section of
      `docs/knowledge/`; investigations that led to them → the Research
      section (the to-rad skill; its audit template where the record has
      to be defensible later). Run
      the project-docs sync after filing.
- [ ] Update `AGENTS.md` if project setup steps changed.
- [ ] If transitioning to another agent/session, use the `handoff` skill
      (it posts to the focused story when one exists).

### Step 5: Commit & PR documentation prep

Prepare version control and team documentation from the clean, complete
state:

- [ ] Draft a structured commit message (e.g., conventional commits),
      referencing the story ID.
- [ ] Write a PR description detailing scope, rationale, and verification
      results — the story's AC checklist is the verification summary;
      link the story rather than restating it.
