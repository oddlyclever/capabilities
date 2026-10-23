# Skill Standards

Normative requirements for a skill, derived from the [Agent Skills specification](https://agentskills.io/specification) and its guides on [optimizing descriptions](https://agentskills.io/skill-creation/optimizing-descriptions), [using scripts](https://agentskills.io/skill-creation/using-scripts) and [client implementation](https://agentskills.io/client-implementation/adding-skills-support). The closer a skill stays to the standard, the cheaper it is to maintain and the more agents it works in unmodified.

**Gate: a skill should pass `skills-ref validate <skill-dir>` before it lands.** ([skills-ref](https://github.com/agentskills/agentskills/tree/main/skills-ref))

## 1. Structure and frontmatter

A skill is a directory containing `SKILL.md`, optionally plus `scripts/`, `references/` and `assets/`.

| Field | Required | Constraints |
|---|---|---|
| `name` | yes | 1-64 chars; lowercase `a-z0-9-`; no leading, trailing or consecutive hyphens; MUST match the directory name |
| `description` | yes | 1-1024 chars; what the skill does AND when to use it |
| `license` | no | short - a license name or bundled file reference |
| `compatibility` | no | 1-500 chars; ONLY when the skill has real environment requirements (most skills should omit it) |
| `metadata` | no | string→string map; use reasonably unique keys |
| `allowed-tools` | no | experimental; space-separated pre-approved tools |

**No other top-level frontmatter keys.** Anything custom goes under `metadata` - a version, an author, the upstream a skill was derived from. Custom top-level keys may be ignored or rejected outright by a conforming client.

**1024 characters is a hard limit, not a budget.** A description over it is a skill a strict client may refuse to load, and the failure looks like the skill not existing.

## 2. Portability

- Everything a skill needs ships inside its own directory: scripts in `scripts/`, docs in `references/`, templates in `assets/`.
- All file references are **relative paths from the skill root**, one level deep. Never `../` out of the skill, never absolute paths, never paths relative to the repository root - a skill must survive being copied alone into any project's `.agents/skills/`.
- The only external paths a skill may name are well-known locations documented by the system it belongs to.
- Shared prose is **duplicated** into each skill that needs it, never cross-referenced between skills. Two copies that can drift beat one reference that breaks the moment a skill is copied out on its own.
- Skills NEVER ask for or accept credentials in conversation.

## 3. Progressive disclosure

Agents load skills in three tiers; structure content accordingly.

1. **Metadata** (~100 tokens) - `name` + `description`, loaded at startup for every installed skill. This is all the agent sees until it activates.
2. **Instructions** - the full `SKILL.md` body, loaded on activation. Keep it under **500 lines** (< ~5000 tokens). One skill = one job.
3. **Resources** - `scripts/`, `references/`, `assets/`, loaded only when the instructions point at them.

Move detail out of `SKILL.md` into focused reference files - smaller files mean less context per load. List bundled scripts in the body so the agent knows they exist; do not inline their contents.

## 4. Descriptions

The description carries the entire burden of activation. An agent decides to load a skill from the description alone.

- **Imperative phrasing**: "Use when …", not "This skill does …".
- **Cover both halves**: what it does, and the situations that call for it.
- **User intent, not mechanics**: describe what the user is trying to achieve; include trigger phrases users actually say, including cases where they do not name the domain.
- **Be pushy but bounded**: list the contexts where the skill applies, and where a skill has near neighbours, state the boundary so the wrong one does not fire.
- **Say what it is NOT for** where a word is ambiguous. "Design" meaning architecture and "design" meaning UI are different skills; the description is the only place that can keep them apart.
- ≤ 1024 characters; a few sentences is usually right.

When tuning a description, use the eval-query loop from the guide: ~20 realistic queries (8-10 should-trigger with varied phrasing and explicitness, 8-10 near-miss negatives), 3 runs each, train/validation split (~60/40) to avoid overfitting, and revise by generalizing rather than pasting keywords from failed queries.

## 5. Scripts

Bundled scripts and one-off commands MUST be designed for a non-interactive agent reading stdout and stderr:

- **Never block on input** - no TTY prompts, confirmations or password dialogs. All input via flags, environment variables or stdin. A missing argument produces a usage error, not a prompt.
- **`--help` / usage text** - brief description, flags, examples. This is how the agent learns the interface; keep it concise.
- **Helpful errors** - say what went wrong, what was expected, and what to try. Distinct exit codes for distinct failure types.
- **Structured output** - JSON/CSV/TSV on stdout; progress and diagnostics on stderr. Composable with `jq`, `cut`, `awk`.
- **Idempotent** - agents retry; "create if not exists" beats "fail on duplicate".
- **Safe defaults** - `--dry-run` for destructive or stateful operations; explicit `--force` or `--confirm` where the risk warrants it.
- **Bounded output** - default to a summary or limit; support `--offset` or an `--output` file when results can be large, because harnesses truncate.
- **Dependencies** - prefer self-contained scripts (bash plus standard tools; Python with PEP 723 inline metadata run via `uv run`). For one-off commands, pin versions (`npx tool@x.y.z`, `uvx tool@x.y.z`) and state prerequisites in `SKILL.md`; runtime-level requirements go in the `compatibility` field.
- Reference scripts by relative path from the skill root; agents execute from there.

## 6. What may be assumed from clients

Conforming clients scan `<project>/.agents/skills/` and `~/.agents/skills/` (plus their own native locations), load only name and description at startup, and activate by reading `SKILL.md` - so a skill must work through file-read activation alone, with no client-specific machinery.

- Project-level skills override user-level ones on name collision.
- Clients may strip frontmatter on activation: **never put load-bearing instructions in frontmatter.** Anything the agent must follow belongs in the body, or, for triggering, in the description.
- Custom frontmatter beyond the spec may be ignored or rejected - hence rule 1.
- Untrusted-repo gating means project skills might not load at all in some clients; a skill should degrade gracefully rather than assume it ran.
