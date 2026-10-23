---
name: to-prd
description: Synthesize conversation context and codebase understanding into a formal PRD, with verification living on tracker stories - and, only when asked, restate it for another audience as a pasteable summary or an outbox artifact - never a second document. Use when the user wants to formalize a plan, feature idea, or requirement discussion into a PRD, or wants an existing PRD restated for product management or business development. Triggers - "write a PRD", "formalize this plan", "turn this into requirements", "brief for the PM", "what do we tell sales", "commercial brief", "what can we promise".
license: MIT
compatibility: Standalone for the PRD document; creating the verification stories uses the to-issues skill and the project's tracker.
metadata:
  author: bpappin
  version: "1.6"
---

# Product Requirements (to-prd)

Synthesize the current context into a **PRD document** in the **Product Requirements** section of
`docs/knowledge/`. The
PRD carries requirements and intent; **verification lives on tracker
stories** (their `## Acceptance Criteria` checklists), never in a companion
AC file — the tracker is the source of truth for anything with done-ness.

Works with the project-docs skill: this skill is the authoring workflow;
project-docs owns the conventions, the template, and the KB sync.


> **A PRD is a product decision, not a capture.** If you are recording
> something you noticed - a bug, a gap, an idea, a story that needs
> writing - file it as an issue and let triage route it. Only write a PRD
> when deciding what the product does is your call to make. Nobody should
> be pushed into authoring one because a story they picked up turned out
> to be thin.

## Process

### 1. Synthesis

From the conversation and the codebase: identify the problem, the actors,
and the major modules — look for opportunities to extract deep modules
(isolatable, testable logic). Use the project's domain glossary and respect
existing ADRs. If research fed this (the Research section of `docs/knowledge/`), reference it.

### 2. Write the PRD

Create `<slug>.md` in the Product Requirements section directory of
`docs/knowledge/` (find it by its README H1; create it per project-docs if
missing) from the project-docs template (`assets/templates/prd.md` in that
skill). No id/status frontmatter — the `# Title` heading names the KB
article, and the sync gives the file its ID-prefixed name. Sections:

- **Problem** — from the user's perspective, with the discovery trail
  (link Research records that led here).
- **Goals / Non-goals** — non-goals are the requirements-level scope guard.
- **Requirements** — numbered (R1, R2 …) narrative requirements, backed by
  an extensive list of user stories ("As an <actor>, I want <feature>, so
  that <benefit>").
- **Decisions** — architectural choices, API contracts, module boundaries.
  No file paths or code snippets, unless a snippet encodes a decision
  (state machine, schema, type shape) more precisely than prose.
- **Stories** — the table of tracker story IDs. Leave it with a
  placeholder note until step 3 fills it.

Run the project-docs sync after writing — the PRD becomes a KB article
immediately (and again after step 3 fills the Stories table).

### 3. Create the verification

Offer to run the **to-issues** skill to break the PRD into tracker stories
(vertical slices, each carrying its own `## Acceptance Criteria`). When the
stories are published, fill the PRD's `## Stories` table with their IDs and
which requirements each covers. Where a requirement has strict rules or
needs test automation, note it so the story gets the `needs-gherkin` tag.

### 4. Briefing other readers - not a second document

**The PRD is the only document this skill writes.** Do not produce a brief unprompted. A brief written beside a PRD becomes a second PRD: it restates the same decisions, drifts from them within a week, and the one the business reads is the one that is wrong.

When somebody does need the PRD restated for a reader who does not follow module boundaries, ask which form they want, and default to the cheapest:

- **A short summary in the conversation** - a few sentences they can paste into chat, an email, or a story comment. This is the normal answer and needs no file.
- **An outbound artifact in `docs/outbox/`**, only if they ask for something to send. That section exists for exactly this: written FOR someone else, not project knowledge, and it does not sync anywhere. Name it after the PRD it came from.

Never in the Product Requirements section, and never in the knowledge base. A brief is a rendering of a decision for one audience at one moment; the PRD is the record.

**Where to look for the shape**, when one is asked for: `assets/templates/pm-brief.md` for a product brief (outcomes, users, non-goals in plain terms, how we will know it worked, sequencing, risks - no module names) and `assets/templates/bd-brief.md` for a commercial one. Use them as a checklist of what to cover, not as a reason to create a file.

**A commercial brief has a test worth keeping:** does this change what someone outside the company can be told, sold, or promised? A new capability, a changed limit, a new integration - yes. Refactors, tech debt, internal tooling - no. When you do write one, the section that earns its place is **what it does NOT do**, because commercial harm comes from promises made in the gap between what shipped and what someone assumed shipped. Mark availability as committed, planned, or exploratory; a reader assumes the strongest reading you leave open. Never carry story IDs, module names, or internal codenames into it.

**If briefing exposes a gap, fix the PRD.** Success signals, a firm date, a segment the PRD should have named - correct it there rather than in the restatement. That is the part of this exercise worth having, and it survives without producing a single extra file.

## Review checklist

- [ ] Are the user stories comprehensive?
- [ ] Is there a discovery trail (research/ADR links) in the Problem section?
- [ ] Are decisions decoupled from specific files?
- [ ] Are non-goals explicit?
- [ ] Is the Stories table filled (or explicitly deferred to to-issues)?
- [ ] No AC in the PRD - checklists belong to the stories.
- [ ] Does the PRD say how success is measured?
- [ ] One document, unless a brief was asked for - and if one was, is it a
      pasteable summary or an outbox artifact rather than a second PRD?
