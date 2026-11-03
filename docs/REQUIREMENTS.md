# Coursework requirements

Sources: the assignment overview, submission requirements and detailed assessment
rubric for ECS7032P Assignment 1.

## Agent

- Permitted starting points include BasicMCTS, RMHC, RHEA, MaxN and simple agents.
- The advanced `players.mcts.MCTSPlayer` implementation is excluded from the
  submission's development basis.
- The assignment discourages an entirely new MCTS implementation from scratch.
- Custom runtime code must be inside `group{GROUP_LETTER}`. The development
  package `sushigoagent` requires renaming to the assigned submission identifier.
- Required package contents include source comments explaining critical logic,
  a constructor/run README and a competition JSON configuration.
- Framework modifications outside the package cannot be assumed available during
  marking. Standalone compatibility requires validation in the course TAG version.

## Experiments

The initial prose describes 500 games per experiment as a guideline with justified
exceptions. The detailed rubric deems fewer than 500 games per agent pair
insufficient. The principal study therefore plans at least 500 games per reported
comparison, pending any teaching-team clarification. Development smoke tests are
separate from this study.

The top experimentation band requires more than one framework reference agent,
the base version of the developed agent and thorough ablations. Planned references
are Random and OSLA, alongside the corrected baseline. Some experiments at the
500 ms level are required by the assignment text.

The report requires enough configuration detail for replication, uncertainty
analysis and explanations of the observed results. Novelty and ambition are
assessed against course content and relevant literature.

## Submission

- Group submission: package-only source ZIP and report PDF.
- Report format: official Word/LaTeX template, two columns, maximum four pages.
  References and generative-AI notes may extend to a fifth page under the guidance.
- Individual submission: reflection of at most 300 words, in PDF format.
- QMplus group membership is required before submission.
- Generative systems are permitted by the assignment excerpt.

## Competition settings

| Assignment section | Players | Per-decision limit |
|---|---:|---:|
| Code / tournament description | 3 | 1 second |
| Experiments section | 4 | 500 milliseconds |

The conflict remains unresolved. The baseline is validated with both player counts.
Baseline and heuristic time configurations use a 450 ms budget and `breakMS=25`.
Stages 2–3 include measured opening-position timing checks; these samples do not
establish deadline compliance across complete games or on the marking machine.

The rubric's meaning of "per pair" also requires clarification for multiplayer
experiments. The planned protocol uses fixed opponent lineups, recorded player
counts and balanced seats, with at least 500 games per principal matchup.

## Assessment weights

| Area | Weight |
|---|---:|
| Abstract | 5% |
| Problem definition | 15% |
| Approach | 30% |
| Experimentation | 30% |
| Conclusions | 10% |
| General presentation | 10% |

The detailed mapping is in [RUBRIC_REVIEW.md](RUBRIC_REVIEW.md). Final compatibility
and formatting checks depend on the exact course TAG revision and report template.
