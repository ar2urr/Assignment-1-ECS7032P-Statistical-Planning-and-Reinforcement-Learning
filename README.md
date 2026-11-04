# Sushi Go coursework

Stages 1–5 are implemented and validated.
The permitted TAG BasicMCTS baseline has reliability fixes, configurable Sushi Go
evaluation and switchable PARANOID/MAXN opponent models. Stages 4 and 5 are prepared for
one combined Git checkpoint. Stage 6 covers the final report and submission package.

## Documentation

- [File and folder reference](docs/FILE_GUIDE.md)
- [Six-stage plan](docs/STAGES.md)
- [Stage 1 historical baseline](docs/STAGE_1.md)
- [Stage 2 reliability](docs/STAGE_2.md)
- [Stage 3 Sushi Go evaluation](docs/STAGE_3.md)
- [Stage 4 multiplayer search](docs/STAGE_4.md)
- [Stage 5 experiment protocol](docs/STAGE_5.md)
- [Stage 5 findings and interpretation](docs/STAGE_5_FINDINGS.md)
- [Study results and figures](results/stage5/analysis/RESULTS.md)
- [Coursework requirements](docs/REQUIREMENTS.md)
- [Assessment rubric mapping](docs/RUBRIC_REVIEW.md)
- [Agent constructors and configuration](sushigoagent/README.md)

## Structure

| Path | Contents |
|---|---|
| `sushigoagent/` | Five Java runtime files, eight JSON configurations, README and upstream licence. |
| `experiments/` | External Java game runner and four feature-removal configurations. |
| `tests/` | JUnit search/scoring/integration/harness tests and Python analysis tests. |
| `scripts/` | Build, validation, study and analysis commands. |
| `docs/` | Requirements, six-stage plan and technical records. |
| `results/` | Validation logs, study protocol, source snapshots, raw games and analysis. |
| `TAG_COMMIT` | Pinned official TAG revision. |

The development package is `sushigoagent`. Final submission requires the assigned
group package identifier with matching imports, paths and JSON class names.
The experiment harness, tests and analysis tools remain outside the agent package.

## Build and validation

Dependencies: Java 21, Maven and a separate TAG checkout. Commands from the project
root:

```bash
git clone https://github.com/GAIGResearch/TabletopGames.git ../TabletopGames
git -C ../TabletopGames checkout "$(cat TAG_COMMIT)"
bash scripts/check.sh ../TabletopGames
```

For an existing pinned checkout, only the final command is required. `MVN` selects
an alternative Maven executable; `MAVEN_REPO` selects a dependency-cache directory.
Initial setup downloads TAG's dependencies. The script checks the framework
revision, builds TAG and runs six focused JUnit classes. It leaves TAG source
unmodified. `check-stage1.sh` delegates to the same current runner.

The combined Java suite passed **43 tests**. The Python analysis suite passed
**3 tests**. The successful experiment pilot completed all 32 fixed-iteration
cells, with 384 measured games. Pilot data is excluded from the principal study.

## Experiments

```bash
python3 scripts/run-study.py ../TabletopGames --games 504 --output results/new-study
python3 -m venv .venv
.venv/bin/python -m pip install -r experiments/requirements-analysis.txt
.venv/bin/python -m unittest discover -s tests -p 'test_analysis.py'
.venv/bin/python scripts/analyse-study.py results/new-study
```

The completed principal study contains **16,128 games**, in 32 cells of 504 games: four main variants against
Random, OSLA and corrected-baseline lineups, plus four feature removals against
the baseline, at both three and four players. Every cell has balanced seats.
All search agents use 150 iterations per searched decision. A separate sequential
timing study covers 28 complete games with the 450 ms configurations and a 25 ms
reserve. Full protocol, seed policy and reproduction details are in Stage 5.

The analyser validated all records and exported summary tables, pointwise 95%
bootstrap intervals, and PNG/PDF figures. Against baseline lineups, the combined
agent achieved 75.10% win share with three players and 58.93% with four, compared
with baseline controls of 32.64% and 26.39%. The heuristic drove the improvement;
MAXN had no consistent additional benefit. Feature removals identify Wasabi as a
candidate for revision in four-player games.

The timing study recorded 633 searched decisions, including one baseline overrun
of 509.870 ms. It measures latency and does not establish playing strength at
500 ms or a universal deadline guarantee. Full interpretation and limitations
are in the Stage 5 findings.

## Limits

Heuristic weights and search settings are fixed, not tuned. The search uses a
sampled observation and is not information-set MCTS. Recorded seeds do not fully
control TAG's hidden-state sampling. Fixed iterations do not equalise wall time.
Deadline checks cannot interrupt framework calls, scheduling or garbage collection;
sampled timings do not establish a hard deadline guarantee. Compatibility with
the course's marking revision remains unverified. Final packaging and report work
belong to Stage 6.
