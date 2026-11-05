# How This Documentation Works

Everything about this repository lives in one of four kinds of place. This file explains them; it is never published anywhere itself.

**GitHub Issues owns work.** Stories, bugs, ideas, anything with a status that will someday be "done" is an issue on `oddlyclever/capabilities`, never a document. `docs/stories/` is a generated snapshot of those issues for agents to read; it is git-ignored, so don't edit it. [`WORKFLOW.md`](../WORKFLOW.md) at the repo root describes how work moves.

**The knowledge base owns knowledge**, and `docs/knowledge/` is where it is written. Decisions, requirements, research, specifications, guides, reference material: anything a person would look up rather than work through. Each section folder's `README.md` says what belongs in it.

- To add a document, create a `.md` file in the right section folder. It opens with a `# Title` (the title alone, with no prefix), then an identifier line and a keywords line.
- Documents are numbered per record type, not per folder: `ADR-` for decisions, `PRD-` for requirements, `RAD-` for research, and `DOC-` for specifications, guides and anything else, as one shared sequence.
- Link with ordinary relative paths. The sync rewrites them for the wiki and back.

**The knowledge base is mirrored to the repository's GitHub wiki.** The local tree owns the layout: reorganize by moving files here, and the wiki's page names and sidebar follow on the next sync. Edits made in the wiki pull back down, and edits made on both sides merge; a true collision gets git conflict markers and is never pushed until someone resolves it. `docs/knowledge/.gh-wiki-sync/` is the sync's memory, so commit it and never edit it by hand. Until the wiki has its first page, `docs/knowledge/` is plain git with no mirror.

**The wiki is as readable as the repository.** Once the repository is public, every synced page is on the open web the moment it syncs, and deleting it later does not take it back. Everything under `docs/knowledge/` is held to the same rule as the code: no private references, and examples use placeholders.

**Plain git owns the machinery.** `AGENTS.md` and `WORKFLOW.md` at the repo root, this file, `docs/outbox/` for artifacts written for someone outside the project, and `docs/_archive/` for retired files. None of that syncs.

**Each module is documented in three places, each with its own job.** Its `README.md`, beside the code, is the basics: what the module is for, how to install it, and a first use, ending with a link to its section of the knowledge base. Its section, `docs/knowledge/<module>/`, is the full documentation: every feature in detail, platform notes, deployment and packaging, what goes wrong and why, and the decisions and research behind its shape. Its KDoc is the API reference, read at the moment someone writes the call. A detail belongs in one of them, not copied into all three. What spans several modules goes in the sections filed by kind. A new module gets its section when it is added, as one of the four places [`AGENTS.md`](../AGENTS.md) says a module is registered.

Git is the version history for all of it. Diffs, blame and merges work on knowledge exactly as they do on code.
