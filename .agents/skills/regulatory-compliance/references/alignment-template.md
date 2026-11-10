# Compliance Mapping Template

Use this for a design that touches personal data, automated decision-making, or anything else an active regulation reaches. It attaches to the ADR, PRD or specification it belongs to — it is not a document of its own.

## Status vocabulary

Four values, and they describe the state of the *engineering*, not the state of the law:

| Status | Means |
|---|---|
| **Addressed** | The design does what the obligation asks, and the Implementation column says where |
| **Partly** | Some of it is in place; the rest is named, with what is missing |
| **Gap** | The obligation applies and nothing in the design meets it yet |
| **Needs review** | Whether it applies, or whether the design meets it, is a question for somebody qualified |

**No value in this table certifies compliance**, and none is a pass. "Addressed" says an engineer believes the design covers the obligation as they understood it. That is useful and it is not the same claim.

Where an obligation plainly does not reach the design, write **n/a** with the reason in one line — an empty row reads as unfinished, and a silently dropped row reads as covered.

---

## [Record title] — Compliance Mapping

> Engineering assessment, not legal advice. Prepared by an agent against
> the project's active regulations and unreviewed until somebody qualified
> has reviewed it. Reviewed: <name or "not yet"> · <YYYY-MM-DD>

**Deployment:** <which deployment this mapping covers, and the project's role in it — see the skill's deployments step. A product that is self-hosted by its users and also offered as a service needs one mapping per deployment, because the role changes and the role decides most of the rows.>

| Regulation | Obligation | Implementation | Status |
| :--- | :--- | :--- | :--- |
| <REG> | <article or principle, cited> | <what the design does, or what is missing> | Addressed / Partly / Gap / Needs review |

### Risks & mitigations

- **Risk**: <the obligation at stake and how the design could fall short of it>
- **Mitigation**: <the technical or procedural safeguard, and where it lives>

### Open questions

- <anything marked Needs review, as a question somebody can answer — not a placeholder>
