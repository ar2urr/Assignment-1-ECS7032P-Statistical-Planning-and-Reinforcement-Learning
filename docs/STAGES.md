# Implementation stages

Status: Stage 1 complete; Stages 2–6 planned. Each stage has a defined scope,
validation criteria and a separate Git checkpoint.

| Stage | Scope | Completion criteria | Commit message |
|---|---|---|---|
| 1. Baseline | Pinned official TAG, permitted BasicMCTS package, JSON configurations, README and smoke checks. | Compilation, JSON loading, provenance check and complete 3/4-player games. | `stage 1: establish official BasicMCTS baseline and validation` |
| 2. Reliability | Root legal-action handling, exhausted-budget fallback, RNG seeding, parameter copies, wall-clock deadlines and accurate budget accounting. | Regression checks, valid actions, preserved observations, correct copying and measured timing. | `stage 2: fix search correctness and decision budgets` |
| 3. Sushi Go evaluation | Research question and hypothesis; configurable heuristic for incomplete sets, delayed Maki/Pudding rewards and useful Wasabi. | Scoring cases, terminal handling, finite values, no double counting and a rationale grounded in game rules and literature. | `stage 3: add configurable Sushi Go state evaluation` |
| 4. Multiplayer search | MaxN-style selection and backup for self-interested opponents, retaining the original opponent model as an ablation. | Both models complete 3/4-player games; opponent-node value selection is checked; hypotheses and imperfect-information limits are documented. | `stage 4: add switchable multiplayer value selection` |
| 5. Experiments | Baseline, heuristic-only, multiplayer-only and combined variants; comparisons against Random, OSLA and the corrected baseline. | At least 500 games per principal comparison, balanced seats, saved configurations/raw data, uncertainty analysis and experiments at the 500 ms level. | `stage 5: add ablation experiments and statistical analysis` |
| 6. Submission | Frozen configuration, fresh course-TAG validation, package ZIP, README and report in the official template. | Standalone execution, format/page limits, evidence-supported claims, clear figures/references and required individual components. | `stage 6: prepare validated submission package and report materials` |

## Dependencies

Stages 1–2 establish the comparison baseline. Stage 3 isolates domain evaluation;
Stage 4 isolates the opponent model. Stage 5 measures their effects separately and
together. Stage 6 uses those findings in the report.

Research contributions require comparison with the lectures and existing methods.
A known heuristic or MaxN variant alone does not establish originality.

## Experimental protocol

The [rubric mapping](RUBRIC_REVIEW.md) specifies at least 500 games per principal
comparison and two framework reference agents in addition to the baseline.
Short development or timing pilots remain separate from the principal study.

For multiplayer experiments, a matchup has a fixed opponent lineup, player count
and seat-balancing policy. A pooled set of 500 games does not establish 500
independently played games against each distinct opponent. The rubric's meaning
of "per pair" requires confirmation before finalising long runs.

## Outstanding dependencies

- Exact course TAG revision.
- Competition player count and decision limit.
- Assigned submission package identifier.
- Official report template and complete generative-AI guidance.
- Individual contribution records and reflections for final submission.
