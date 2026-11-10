---
name: regulatory-compliance
description: Manage and track regulatory requirements (PIPEDA, GDPR, Law 25, EU AI Act). Use this to add a regulation, map a design against the active ones before implementation, audit the workspace for exposure, or maintain the project's compliance mapping documents. Produces an engineering assessment, never a compliance certification. Triggers - "add a regulation", "audit for compliance", "does this meet GDPR", "privacy assessment", "are we allowed to store this", "what does PIPEDA require here".
license: MIT
metadata:
  author: bpappin
  version: "1.3"
---

# Regulatory Compliance & Audit

## Objective

Keep a written, current map between the project's active regulations and what the code actually does: maintain the per-regulation mapping documents, map a design before it is built, audit the workspace for exposure, and flag a proposal that contravenes an obligation.

## What this skill does NOT do

**It does not certify compliance, and nothing it writes may say that it does.** Every output is an engineering assessment: an agent's reading of a summarised obligation against a design. It is unreviewed until somebody qualified has reviewed it, and every mapping carries that line. An agent that writes "compliant" or marks an obligation as passed has overstated what it can know — the status vocabulary in [references/alignment-template.md](references/alignment-template.md) exists to make that hard to do by accident.

It is also not a substitute for reading the instrument. The catalogue summarises obligations to the level that decides a design question, which is not the level that settles an argument.

## Activation guard

Read `.agents/config/story-tools.json` at the workspace root:

```json
"compliance": {
  "enabled": true,
  "active_regulations": ["PIPEDA", "GDPR"],
  "deployments": [
    { "name": "self-hosted", "role": "processor" },
    { "name": "hosted", "role": "controller" }
  ]
}
```

If `compliance.enabled` is absent or false, say compliance tracking is not enabled for this project and stop. All audits and assessments are restricted to the regulations named in `compliance.active_regulations`; the catalogue in [references/standards.md](references/standards.md) holds the rest so a project can see what it is not tracking.

**Legacy location.** Earlier versions of this skill read `.agents/config/project.json`, a file that existed for this skill alone and that the installer never wrote. If that file is present, move the two keys under `compliance` in `story-tools.json`, delete it, and say so — one pointer file per project, holding everything the workflow needs.

## Deployments and roles

**Who answers an obligation depends on which deployment is in question, and this is the step most mappings skip.** The same product self-hosted by its users and offered as a hosted service is a processor in one and a controller in the other; under GDPR that changes the lawful basis, who gives the notices, who answers an access request, who reports a breach and to whom, and whether Chapter V transfers arise at all. Under the EU AI Act the equivalent axis is provider versus deployer, and a deployer that puts its name on a high-risk system becomes its provider.

So before mapping anything:

1. **Ask which deployments exist** if `compliance.deployments` is not set, and record the answer there. Self-hosted, hosted, on-premises for a single customer, embedded in someone else's product — whatever the project actually ships.
2. **Name the project's role in each**, in that regulation's own vocabulary. Where it is genuinely unclear, record **Needs review** rather than guessing; joint controllership in particular is a legal question about who determines purposes, not a question about who runs the servers.
3. **Map each deployment separately.** One table per deployment, not one table with a column for each — the rows diverge, and a merged table hides which one a reader is looking at.

A project with one deployment says so once and moves on.

## Where things live

- **Per-regulation mapping documents: `<NAME>.md` in the `compliance/` section of `docs/knowledge/`** — "Mandates & Compliance" in the taxonomy, which exists for exactly this. An auditor reads these in the knowledge base; the project-docs sync keeps both sides current.
- Index: that section's `README.md`.
- Compliance mappings for a design use [references/alignment-template.md](references/alignment-template.md), and attach to the record they belong to — never a document of their own.
- Remediation work found by an audit goes to the tracker, never to local TODO lists (see Workspace audit below).

**Never create a top-level section for this, and never a `regulations/` directory.** The taxonomy forbids a new top-level section outright — it is a claim about the project and a person's call — and the installer's KB migration maps `regulations/` onto `compliance/`, so a directory created under that name gets renamed out from under the mapping documents at the next install. Where `compliance/` has grown enough to need subdividing, a sub-group inside it inherits the section's meaning and audience and is free; give it a `README.md` like any other.

**The rule and the project's response are two documents.** "GDPR Art. 17 requires erasure" is a mandate; "how this project deletes an account" is the project's mapping of it. Both live in `compliance/` and cite each other — an auditor reads them together, and a mapping filed away from the rule it maps is a register nobody finds. The *procedure* for running a check is a how-to and belongs in `guides/`.

## Core workflows

### 1. Add a regulation

When asked to "add a regulation" (e.g. "Add HIPAA"):

1. Create `<NAME>.md` in the `compliance/` section of `docs/knowledge/`, using the table format from the alignment template: | Regulation | Obligation | Implementation | Status |. One table per deployment.
2. Add `<NAME>` to `compliance.active_regulations` in `.agents/config/story-tools.json`.
3. If the regulation is not in [references/standards.md](references/standards.md), add it there in the shape that file sets out — standing and a checked-on date first.
4. Update the section's `README.md` index, then run the project-docs sync.

### 2. Pre-implementation assessment

Before starting work on a new ADR, PRD or feature:

1. Read the active regulations and the existing mapping files in `compliance/`.
2. Analyse the proposed design against them: is there a lawful basis for each purpose? Is collection limited to it? Is there a deletion path, and an export path? What is told to whom, and when? Does anything cross a border, and does any decision get made automatically?
3. Generate a **Compliance Mapping** from [references/alignment-template.md](references/alignment-template.md) for each active regulation and each deployment, and attach it to the record.
4. Where the honest answer is that an obligation needs a qualified opinion, mark it **Needs review** and put the question in the record's Open questions. An agent's confident "Addressed" on a row it could not actually assess is the failure mode this vocabulary is built to catch.

### 3. Workspace audit

When asked to "audit for compliance":

1. Search `docs/` and the source tree for personal data in places it should not be: real addresses, phone numbers, cleartext identifiers, fixtures built from live data.
2. **Review the ADRs that touch personal data or automated decision-making** — those should carry a `## Regulatory Alignment` section. Do not expect one on every ADR: the ADR template's sections are Context, Decision, Consequences and Corrections, and Regulatory Alignment is an optional fifth that a decision earns by reaching regulated data. An audit that demands it everywhere fails every record and teaches the reader to ignore the audit.
3. Update the mapping files in `compliance/`, then sync docs.
4. Route findings that need code or design changes to the tracker: the discovered-work off-ramp (story-workflow) for items found while a story is focused, or to-issues for a planned remediation batch. Audit findings are work, not documentation.

### 4. Risk alerting

If a proposed change contravenes an active regulation's obligations:

1. Pause and say so before it is built.
2. Cite the specific article or principle, from the project's mapping document or [references/standards.md](references/standards.md), and say which deployment it bites in.
3. Propose a privacy-by-design alternative — a keyed digest instead of a raw email address, a purpose-scoped field instead of a general one, a retention window instead of indefinite storage.
4. Say plainly where the objection is an engineering judgement rather than a settled reading. "This looks like an Art. 6 problem and should be reviewed" is honest; "this is illegal" is not a claim this skill can make.

## Reference material

- [Regulatory standards catalog](references/standards.md) — obligations, standing and checked-on dates
- [Alignment template](references/alignment-template.md) — the mapping format and the status vocabulary
- Project mapping documents: the `compliance/` section of `docs/knowledge/`
