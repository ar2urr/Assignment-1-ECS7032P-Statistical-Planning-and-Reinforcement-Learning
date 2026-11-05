# Coursework requirements

Sources: the assignment overview, submission requirements and detailed assessment
rubric for ECS7032P Assignment 1.

## Agent

- Permitted starting points include BasicMCTS, RMHC, RHEA, MaxN and simple agents.
- The advanced `players.mcts.MCTSPlayer` implementation is excluded from the
  submission's development basis.
- The assignment discourages an entirely new MCTS implementation from scratch.
- Custom runtime code must be inside `group{GROUP_LETTER}`. The development
  package `sushigoagent` is exported as the assigned identifier `groupAG` by
  the Stage 6 packaging tool.
- Required package contents include source comments explaining critical logic,
  a constructor/run README and a competition JSON configuration.
- Framework modifications outside the package cannot be assumed available during
  marking. Standalone compatibility requires validation in the course TAG version.

## Experiments

The initial prose describes 500 games per experiment as a guideline with justified
exceptions. The detailed rubric deems fewer than 500 games per agent pair
insufficient. The principal protocol uses 504 games per reported lineup
comparison, with balanced seats at both player counts. Development smoke tests
and pilots are separate from this study.

The top experimentation band requires more than one framework reference agent,
the base version of the developed agent and thorough ablations. Stage 5 includes
Random and OSLA, alongside the corrected baseline, and tests both algorithmic
factors plus four feature removals. Some experiments at the 500 ms level are
required by the assignment text. The selected scope uses a smaller latency study
at that limit; it does not establish 500 ms playing strength or replace any
full-size timed comparison the teaching team may require.

The report requires enough configuration detail for replication, uncertainty
analysis and explanations of the observed results. Novelty and ambition are
assessed against course content and relevant literature.

## Submission

- Group submission: package-only source ZIP and report PDF.
- Report format: official Word/LaTeX template, two columns, maximum four pages.
  References and generative-AI notes may extend to a fifth page under the guidance.
- Individual submission: reflection of at most 300 words, in PDF format.
- QMplus group membership is required before submission. The declared group is
  AG, with one member.
- Generative systems are permitted by the assignment excerpt.

## Competition settings

| Assignment section | Players | Per-decision limit |
|---|---:|---:|
| Code / tournament description | 3 | 1 second |
| Experiments section | 4 | 500 milliseconds |

The conflict remains unresolved. All four main variants are tested with both
player counts. Time configurations use a 450 ms budget and `breakMS=25`.
Stage 5 measures searched decisions across 28 complete games. These samples do
not establish a hard deadline guarantee or compliance on the marking machine.

The rubric's meaning of "per pair" also requires clarification for multiplayer
experiments. The implemented protocol uses fixed opponent lineups, recorded
player counts and balanced seats, with 504 games per principal matchup.
The [Stage 5 record](STAGE_5.md) defines the protocol and its limitations.

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
