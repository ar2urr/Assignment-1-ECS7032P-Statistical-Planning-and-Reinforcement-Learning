# Implementation stages

Stages 1-6 are implemented and tested. The report's author details, official
template and marking-framework checks remain to be completed. The saved
checkpoints are `a008e35` for Stage 1, `db219f5` for Stages 2/3, `26b8981` for
Stages 4/5 and `fa473bb` for Stage 6.

Validation records: [Stage 1](STAGE_1.md), [Stage 2](STAGE_2.md), [Stage
3](STAGE_3.md), [Stage 4](STAGE_4.md), [Stage 5](STAGE_5.md) and [Stage
6](STAGE_6.md). The separate staging commands in the early records are historical
examples; Stages 2/3 were committed together. Build and experiment scripts do not
create commits.

| Stage | Scope | Completion criteria | Commit message |
|---|---|---|---|
| 1. Baseline | Pinned official TAG, permitted BasicMCTS package, JSON configurations, README and smoke checks. | Compilation, JSON loading, provenance check and complete 3/4-player games. | `stage 1: establish official BasicMCTS baseline and validation` |
| 2. Reliability | Root legal-action handling, exhausted-budget fallback, RNG seeding, parameter copies, wall-clock deadlines and accurate budget accounting. | Regression checks, valid actions, preserved observations, correct copying and measured timing. | `stage 2: fix search correctness and decision budgets` |
| 3. Sushi Go evaluation | Research question and hypothesis; configurable heuristic for incomplete sets, delayed Maki/Pudding rewards and useful Wasabi. | Scoring cases, terminal handling, finite values, no double counting and a rationale grounded in game rules and literature. | `stage 3: add configurable Sushi Go state evaluation` |
| 4. Multiplayer search | MaxN-style selection and backup for self-interested opponents, retaining the original opponent model as an ablation. | Both models complete 3/4-player games; opponent-node value selection is checked; hypotheses and imperfect-information limits are documented. | Combined Stage 4/5 checkpoint |
| 5. Experiments | Baseline, heuristic-only, multiplayer-only and combined variants; comparisons against Random, OSLA and the corrected baseline. | At least 500 games per principal comparison, balanced seats, saved configurations/raw data, uncertainty analysis and a separate 500 ms latency study. | Combined Stage 4/5 checkpoint |
| 6. Submission | Frozen configuration, fresh course-TAG validation, package ZIP, README and report in the official template. | Standalone execution, format/page limits, evidence-supported claims, clear figures/references and required individual components. | `stage 6: prepare validated submission package and report materials` |

## Dependencies

Stages 1-2 provide the comparison baseline. Stage 3 adds the heuristic, and Stage
4 adds the opponent model. Stage 5 tests each change separately and together.
Stage 6 selects the submission settings and checks the exported package.

The report compares the approach with the lectures and prior work. Using a known
heuristic or MAXN variant does not by itself show originality.

## Experimental protocol

The [rubric mapping](RUBRIC_REVIEW.md) calls for at least 500 games per main
comparison, two framework reference agents and the developed agent's baseline.
Development and timing pilots are recorded separately.

Each multiplayer matchup fixes the opponent lineup, player count and seat
allocation. The study runs 504 games per lineup with balanced seats at both player
counts. Pooling games from different lineups would not provide 500 games against
each opponent. The teaching team's meaning of "per pair" still needs confirmation
for multiplayer play.

## Outstanding dependencies

- Exact course TAG revision.
- Competition player count and decision limit.
- Official report template and complete generative-AI guidance.
- The sole member's contribution record and individual reflection for final submission.
