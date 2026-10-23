---
name: write-a-skill
description: Write a new agent skill, or bring an existing one up to the Agent Skills standard - structure, frontmatter, a description that actually triggers, progressive disclosure and bundled resources. Use when someone wants to create, write, package or publish a skill, when a skill is not firing or is firing on the wrong requests, when a client rejects or ignores one, or when an existing skill needs checking against the spec. The normative rules ship with this skill in references/skill-standards.md - read them before writing frontmatter, since limits like the 1024-character description are hard and a client may refuse the skill. NOT for writing a document, a slash command, an MCP server or a plugin. Triggers - "write a skill", "create a skill", "make a skill for this", "turn this into a skill", "package this up", "my skill is not triggering", "why did it not load my skill", "check this skill", "does this skill conform", "validate the skill", "the description is too long".
license: MIT
metadata:
  author: bpappin
  version: "2.0"
  derived-from: https://github.com/mattpocock/skills (MIT, (c) 2026 Matt Pocock) - heavily modified
---

# Writing Skills

A skill is a directory holding a `SKILL.md`, which an agent loads on its own judgement from the description alone. That last part is what makes skill authoring unlike writing a document: **nobody invokes it, so a skill that is perfect and never triggers has shipped nothing.**

**Read [references/skill-standards.md](references/skill-standards.md) before writing any frontmatter.** It is the normative set of rules - field limits, portability, progressive disclosure, description rules, script contracts, what may be assumed of a client. The limits in it are hard: a description over 1024 characters or a custom top-level frontmatter key is a skill a strict client may refuse to load, and that failure looks exactly like the skill not existing.

## Process

### 1. Gather requirements

Ask, and do not guess:

- What task or domain does the skill cover, and what is the ONE job it does?
- **What will someone be saying when they need it?** Collect their actual phrasings - this is the raw material for the description, and it is the part you cannot invent later.
- Where would it fire wrongly? Which existing skill is its nearest neighbour?
- Does it need executable scripts, or only instructions?
- Any reference material or templates to bundle?

### 2. Draft it

- `SKILL.md` - the instructions, under 500 lines.
- `references/` - detail the instructions point at, loaded only when needed.
- `assets/` - templates the skill tells the agent to fill in.
- `scripts/` - deterministic operations, built to the script contract in the standard.

### 3. Check it against the standard

Walk [references/skill-standards.md](references/skill-standards.md) and the checklist at the foot of this file. Measure the description rather than eyeballing it; a few sentences of prose plus a trigger list passes 1024 characters more easily than it looks.

Where `skills-ref` is available, `skills-ref validate <skill-dir>` is the mechanical gate.

### 4. Review with the user

Present the draft and ask: does this cover your cases, is anything missing or unclear, should any section be more or less detailed? Read the description back to them as the agent will see it - on its own, with no body under it - and ask whether they would expect it to fire on the requests they named in step 1.

## Structure

```
skill-name/
├── SKILL.md              # Instructions (required)
├── references/           # Detail, loaded on demand
│   └── some-topic.md
├── assets/               # Templates the skill fills in
│   └── templates/thing.md
└── scripts/              # Deterministic operations
    └── helper.sh
```

Everything a skill needs lives inside its own directory, referenced by a relative path one level deep. A skill that reaches outside itself - `../`, an absolute path, a path relative to the repository root - breaks the moment somebody copies it alone into a project, which is the normal way skills travel.

## The SKILL.md template

```md
---
name: skill-name
description: What it does. Use when <the situations that call for it>. NOT for <the near neighbour>. Triggers - "<what someone says>", "<another>".
license: MIT
metadata:
  author: <you>
  version: "1.0"
---

# Skill Name

<One or two lines - what this produces, and the one thing an agent must not get wrong.>

## Process

<Numbered steps. Each step says what to do and what to ask the user.>

## <The rules that matter>

<The content. Prose, not bullet fragments, wherever the reasoning matters.>

## <Detail>

See [references/some-topic.md](references/some-topic.md).
```

Nothing load-bearing goes in frontmatter beyond the description: a client may strip it on activation, so anything the agent must follow belongs in the body.

## The description is the whole activation surface

The agent sees `name` and `description` for every installed skill at startup, and nothing else until it decides to load one. So the description is not a summary - it is the trigger.

Write what the user is trying to achieve, in the words they use, and bound it so the wrong skill does not fire:

```
Extract text and tables from PDF files, fill forms, merge documents. Use when working with PDF files, or when someone mentions PDFs, forms or document extraction.
```

```
Helps with documents.
```

The second one gives the agent no way to tell this from every other document skill, so it either never fires or fires on everything. Both failures are silent.

Two things earn their space more than anything else:

- **The trigger phrases.** What someone actually types, including the cases where they do not name the domain at all.
- **The boundary.** Where a word is ambiguous or a near neighbour exists, say what the skill is NOT for. "Design" meaning architecture and "design" meaning UI are different skills, and the description is the only place that can keep them apart.

Full rules, including the eval-query loop for tuning a description that mis-fires, are in [references/skill-standards.md](references/skill-standards.md).

## When to add scripts

Add one when the operation is deterministic (validation, formatting, a repeated transformation), when the same code would otherwise be generated again every session, or when errors need handling an agent should not improvise. Scripts save tokens and are more reliable than generated code - but they must obey the script contract in the standard: no prompts, `--help`, structured output, useful exit codes, idempotent, safe defaults.

List them in the body so the agent knows they exist. Never paste their contents in.

## When to split into references

Split when `SKILL.md` approaches 500 lines, when the content covers genuinely distinct domains, or when a section is rarely needed - a reference is only loaded if the instructions point at it, so moving rare detail out is a straight saving on every activation that does not need it.

Do not split a short skill for tidiness. Two files the agent must read to do one job cost more than one file.

## Review checklist

- [ ] `name` matches the directory, lowercase `a-z0-9-`
- [ ] `description` measured, at most 1024 characters
- [ ] Description says what it does AND when to use it, in the user's words
- [ ] Trigger phrases included; boundary stated against the nearest neighbour
- [ ] No top-level frontmatter keys beyond the six in the standard - custom ones under `metadata`
- [ ] Nothing load-bearing in frontmatter
- [ ] `SKILL.md` under 500 lines
- [ ] Every path relative to the skill root, one level deep; no `../`, no absolute paths
- [ ] Bundled scripts listed in the body, not inlined
- [ ] Scripts non-interactive, with `--help` and distinct exit codes
- [ ] No credentials requested in conversation
- [ ] No time-sensitive claims, consistent terminology, concrete examples
- [ ] `skills-ref validate <skill-dir>` passes, where it is available
