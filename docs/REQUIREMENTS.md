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

The assignment text treats 500 games per experiment as a guideline and allows
justified exceptions. The detailed rubric calls fewer than 500 games per agent
pair insufficient. The main study therefore uses 504 games per lineup, with
balanced seats at both player counts. Smoke tests and pilots are counted
separately.

The top experimentation band asks for more than one framework reference agent, the
developed agent's baseline and thorough ablations. Stage 5 uses Random, OSLA and
the corrected baseline, tests both algorithmic changes and removes each heuristic
feature in turn. The brief also requires some experiments at the 500 ms level. The
smaller timing study addresses observed latency at that target; it does not
measure playing strength at 500 ms or replace any larger timed comparison required
by the teaching team.

The report must give enough detail to repeat the experiments, explain the
uncertainty in the results and discuss what those results mean. Claims about
novelty must be supported by comparison with course material and relevant
literature.

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

The two competition descriptions conflict. Stage 5 tests both player counts, using
450 ms time configurations with `breakMS=25`. Its 28 timing games provide observed
latencies, not a deadline guarantee on the marking machine.

For multiplayer experiments, "per pair" needs clarification. The study defines
each comparison by its opponent lineup and player count, then runs 504 games with
balanced seats. [Stage 5](STAGE_5.md) records the protocol and its limits.

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
