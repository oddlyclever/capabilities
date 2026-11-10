# Regulatory Standards Catalog

Obligations from the regulations projects in this suite most often track, written so an engineer can check a design against them. Only the regulations named in the project's `compliance.active_regulations` are in scope for an audit or an assessment — the rest are here so that a project knows what it is not tracking.

**This catalogue is an engineering aid, not legal advice, and not a substitute for reading the instrument.** Articles and principles are summarised to the level that decides a design question; the summaries lose detail that matters in an argument, and several of these obligations turn on facts about the business that no document in a source tree records. Where a row drives a real decision, cite the instrument and get a qualified review.

**Every entry carries its standing and the date it was last checked**, because a catalogue that does not is read as current forever. A regulation that has lapsed stays listed, marked as lapsed, so a project that mapped against it can see what happened rather than finding the row silently gone.

---

## 🇨🇦 PIPEDA (Personal Information Protection and Electronic Documents Act)

**In force.** Federal, applies to personal information handled in the course of commercial activity. Checked 2026-11-08.

The ten principles are Schedule 1 of the Act, and all ten bind — a mapping that covers three has not covered PIPEDA.

| Principle | What it requires | What it usually decides in a design |
|---|---|---|
| 4.1 Accountability | A designated person is accountable for compliance; a transfer to a third party for processing stays your responsibility, under contract | Who owns the mapping; what a processor agreement has to say |
| 4.2 Identifying Purposes | Purposes are identified before or at collection, and documented | Whether a field has a stated reason to exist |
| 4.3 Consent | Knowledge and consent are required for collection, use and disclosure, except where the Act allows otherwise; consent must be meaningful and may be withdrawn | Opt-in vs opt-out, withdrawal path, re-consent on a new purpose |
| 4.4 Limiting Collection | Collect only what the identified purposes need, by fair and lawful means | Every column holding personal data needs a purpose |
| 4.5 Limiting Use, Disclosure, Retention | Do not use or disclose for a new purpose without consent; retain only as long as the purpose needs; destroy, erase or anonymise after | Retention schedule, deletion path, what "deleted" means in a backup |
| 4.6 Accuracy | As accurate, complete and up to date as the purposes need | Correction path; staleness where a decision is made on the record |
| 4.7 Safeguards | Protection appropriate to sensitivity — physical, organisational, technological | Encryption at rest and in transit, access control, key handling |
| 4.8 Openness | Policies and practices are readily available in an understandable form | The privacy page is an obligation, not marketing |
| 4.9 Individual Access | On request, tell an individual what you hold, how it is used and to whom it has been disclosed, and allow challenge of accuracy | An access-request path that does not require an engineer |
| 4.10 Challenging Compliance | A complaint procedure, available on request | Where a complaint goes, and who answers it |

**Also binding, outside Schedule 1:**

- **s. 10.1 — breach reporting.** A breach of security safeguards involving a real risk of significant harm is reported to the Privacy Commissioner and notified to affected individuals, as soon as feasible, and a record of *every* breach is kept whether or not it met that threshold.
- **s. 4(2)(b) — personal-use exclusion.** The Act does not apply to personal information an individual collects, uses or discloses for personal or domestic purposes. A tool that runs only on its user's own machine against their own data may be outside PIPEDA entirely — which is a finding worth recording, not an assumption worth making quietly.

---

## 🇪🇺 GDPR (General Data Protection Regulation)

**In force** since 2018-05-25. Checked 2026-11-08.

### Does it apply at all

- **Art. 2(2)(c) — household exemption.** Processing by a natural person in the course of a purely personal or household activity is out of scope. Narrow: the moment the thing is offered to other people, it is gone.
- **Art. 3 — territorial scope.** Applies to an establishment in the Union regardless of where processing happens, and to a controller outside the Union that offers goods or services to, or monitors the behaviour of, people in the Union. Reachability from the Union is not by itself offering.

### Principles and basis

- **Art. 5 — principles.** Lawfulness, fairness and transparency; purpose limitation; data minimisation (5(1)(c)); accuracy; storage limitation; integrity and confidentiality; and **accountability** (5(2)) — the controller must be able to *demonstrate* compliance, which is what makes the written mapping an obligation rather than a nicety.
- **Art. 6 — lawful basis.** Processing needs one of six: consent, contract, legal obligation, vital interests, public interest, or legitimate interests. Each personal-data purpose needs a named basis, and legitimate interests needs a balancing assessment recorded.
- **Art. 9 — special categories.** Health, biometrics used for identification, race, politics, religion, union membership, sex life or orientation are prohibited unless an Art. 9(2) condition applies. Treat as a hard gate, not a sensitivity label.

### What has to be told, and to whom

- **Art. 13** — information to give when data comes from the individual; **Art. 14** — when it comes from somewhere else. Identity of the controller, purposes, basis, recipients, transfers, retention, rights, and the source where applicable.

### Individual rights

- **Art. 15 — access.** A copy of the data plus the Art. 13/14 information, normally within one month.
- **Art. 16 — rectification.**
- **Art. 17 — erasure.** Not absolute: it bites where the basis has fallen away, consent is withdrawn, or the processing was unlawful.
- **Art. 18 — restriction**, **Art. 21 — objection**, and **Art. 22 — automated decision-making**, which carries a right not to be subject to a solely automated decision with legal or similarly significant effect.
- **Art. 20 — portability.** Where processing is by consent or contract and carried out by automated means, the data goes out in a structured, commonly used, machine-readable form. This is an export format decision, and the only right on this list that is cheaper to design in than to retrofit.

### Who is responsible

- **Art. 24 — controller.** Implements appropriate measures and can demonstrate them.
- **Art. 26 — joint controllers.** Where two parties jointly determine purposes and means, the arrangement between them must be written down and its essence made available.
- **Art. 28 — processor.** Processes only on documented instructions, under a contract carrying the Art. 28(3) terms; a sub-processor needs authorisation. **A product that is both self-hosted and offered as a service is usually a processor in one deployment and a controller in the other, and the whole mapping changes with it** — see the deployments step in the skill.

### Organisational obligations

- **Art. 30 — records of processing.** The register. Purposes, categories, recipients, transfers, retention, measures.
- **Art. 32 — security of processing.** Appropriate measures having regard to risk; pseudonymisation and encryption are named as examples, not as a checklist that discharges the duty.
- **Art. 33 — breach notification to the supervisory authority**, within 72 hours of becoming aware where there is a risk to individuals; **Art. 34 — notification to the individuals** where the risk is high. All breaches are documented regardless.
- **Art. 35 — data protection impact assessment.** Required where processing is likely to result in a high risk — large-scale special categories, systematic monitoring of a public area, systematic automated evaluation with significant effects.
- **Art. 25 — data protection by design and by default.** Technical and organisational measures from the design stage, and by default only the data necessary for each purpose.

### Transfers

- **Chapter V (Arts. 44–50) — transfers out of the EEA.** Need an adequacy decision, or appropriate safeguards (standard contractual clauses, binding corporate rules), or a narrow derogation. **A cloud region is a transfer question**: which region a bucket is in, and which support staff can reach it, decides this row. Note that the safeguards themselves are what moves under legal challenge — this is the row most likely to be out of date.

---

## ⚖️ Law 25 (Quebec — An Act to modernize legislative provisions as regards the protection of personal information)

**In force**, phased 2022 through 2024, including the data-portability right from 2024-09-22. Applies to a private-sector enterprise carrying on an activity in Quebec. Checked 2026-11-08.

Kept in this catalogue whether or not a project tracks it, because a project with Canadian users very often falls under it without having asked.

- **Privacy by default.** Where a technological product or service is offered to the public, the highest level of confidentiality must apply by default, without any intervention by the user.
- **Privacy impact assessment.** Required for a project to acquire, develop or overhaul an information system involving personal information, and before communicating personal information outside Quebec.
- **Transparency and consent.** Purposes stated at collection in clear and simple language; consent for sensitive information must be express and given separately.
- **Breach ("confidentiality incident").** Notify the Commission d'accès à l'information and affected individuals where there is a risk of serious injury; keep a register of all incidents.
- **Individual rights.** Access, rectification, de-indexing, and portability — a computerised copy in a structured, commonly used technological format.
- **Accountability.** A designated person in charge of the protection of personal information, with governance policies published on the website.
- **Automated decisions.** Where a decision is based exclusively on automated processing, the individual must be informed and able to submit observations to a person able to review it.

---

## 🤖 EU AI Act (Regulation (EU) 2024/1689)

**In force**, entered into force 2024-08-01, with obligations phasing in — prohibited practices and AI literacy from 2025-02-02, general-purpose AI model obligations from 2025-08-02, and the bulk of the high-risk regime following. Checked 2026-11-08; **the phase-in dates are the part of this entry most likely to have moved, and amendments to the timetable have been under discussion — verify against the current consolidated text before relying on a date.**

Risk-tiered, and the tier decides everything:

- **Prohibited (Art. 5).** Manipulative or exploitative techniques causing significant harm, exploitation of vulnerability, social scoring, certain predictive policing, untargeted scraping of facial images, emotion inference in the workplace or in education, biometric categorisation inferring protected characteristics, and real-time remote biometric identification in public for law enforcement outside narrow exceptions.
- **High-risk (Art. 6, Annex III).** Includes biometrics, critical infrastructure, education, employment and worker management, access to essential services including creditworthiness, law enforcement, migration, and justice. Brings a risk management system, data governance, technical documentation, logging, human oversight, accuracy and robustness, a conformity assessment and registration.
- **Transparency obligations (Art. 50).** People must be told they are interacting with an AI system where it is not obvious; synthetic audio, image, video and text must be marked machine-readably; deep fakes and AI-generated text published on matters of public interest must be disclosed.
- **General-purpose AI models (Chapter V).** Technical documentation, information to downstream providers, a copyright policy and a training-content summary; models presenting systemic risk carry evaluation, incident reporting and cybersecurity duties on top.
- **Roles.** Provider, deployer, importer, distributor — and **a deployer that puts its name on a high-risk system, or substantially modifies it, becomes its provider.** A project that fine-tunes or re-badges somebody else's model should read this row before anything else.

Most work in this suite lands outside the high-risk annexes, and the honest mapping for it is the Art. 50 transparency row plus the AI-literacy duty. Say that explicitly rather than leaving the section empty.

---

## 🇨🇦 AIDA (Artificial Intelligence and Data Act) — LAPSED

**Not law, and not pending.** AIDA was Part 3 of Bill C-27, which died on the Order Paper when Parliament was prorogued on 2025-01-06, during clause-by-clause review at the Standing Committee on Industry and Technology. It had not been reintroduced as of this entry; commentary at the time was that C-27 was unlikely to return in its original form because AIDA was the contested part. Checked 2026-11-08.

Kept as history: a project that mapped against AIDA before 2025 has rows citing a statute that does not exist, and that is worth being able to see rather than having the entry quietly disappear.

**What to do instead.** There is no federal Canadian AI statute to map to. An AI system handling personal information in Canada is governed by PIPEDA (and Law 25 in Quebec) on the data side; voluntary instruments exist but bind nobody. If a project needs an AI-specific obligation set, the EU AI Act above is the live one, and it applies by the Art. 3 reachability test rather than by where the team sits.

---

## Adding a regulation to this catalogue

Follow the shape above: standing and a checked-on date first, scope and exemptions second, then obligations grouped so an engineer can find the one a design touches. Summarise to the level that decides something. Name the instrument and the article, never a paraphrase on its own, so a reader can go and check — and say plainly where an entry is uncertain rather than smoothing it over.
