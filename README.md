# Sushi Go coursework

The agent extends TAG BasicMCTS with search fixes, a Sushi Go heuristic and a
choice of PARANOID or MAXN opponent models. Stages 1-6 are implemented and tested.
Stages 4/5 share commit `26b8981`; Stage 6 is saved at `fa473bb`. The groupAG
export is ready for review. The marking TAG revision, competition settings and
official report template still need confirmation.

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
- [Stage 6 submission preparation](docs/STAGE_6.md)
- [Coursework requirements](docs/REQUIREMENTS.md)
- [Assessment rubric mapping](docs/RUBRIC_REVIEW.md)
- [Agent constructors and configuration](sushigoagent/README.md)

## Structure

| Path | Contents |
|---|---|
| `sushigoagent/` | Six Java runtime files, nine JSON configurations, README and upstream licence. |
| `experiments/` | External Java game runner and four feature-removal configurations. |
| `tests/` | JUnit search/scoring/integration/harness tests and Python analysis tests. |
| `scripts/` | Build, validation, study and analysis commands. |
| `docs/` | Requirements, six-stage plan and technical records. |
| `results/` | Validation logs, study protocol, source snapshots, raw games and analysis. |
| `TAG_COMMIT` | Pinned official TAG revision. |

Development uses the `sushigoagent` package. The export tool puts the submission
in `groupAG` and updates the package declarations and JSON class name. Tests,
experiments and analysis scripts stay outside that package.

## Build and validation

Dependencies: Java 21, Maven and a separate TAG checkout. Commands from the project
root:

```bash
git clone https://github.com/GAIGResearch/TabletopGames.git ../TabletopGames
git -C ../TabletopGames checkout "$(cat TAG_COMMIT)"
bash scripts/check.sh ../TabletopGames
```

If TAG is already checked out at the recorded revision, run only the last command.
`MVN` chooses the Maven executable; `MAVEN_REPO` chooses its dependency cache. The
check script builds TAG and runs seven JUnit test classes without editing
framework source. `check-stage1.sh` calls the same script.

The latest recorded runs pass 47 Java tests and 7 Python tests. Stage 5's earlier
records contain 43 and 3 tests. Its pilot covered all 32 comparison cells and
completed 384 games; those games are excluded from the main results.

## Experiments

```bash
python3 scripts/run-study.py ../TabletopGames --games 504 --output results/new-study
python3 -m venv .venv
.venv/bin/python -m pip install -r experiments/requirements-analysis.txt
.venv/bin/python -m unittest discover -s tests -p 'test_analysis.py'
.venv/bin/python scripts/analyse-study.py results/new-study
```

The main study has 16,128 games in 32 cells of 504 games. It compares four
variants against Random, OSLA and corrected-baseline lineups, then removes four
heuristic features individually. Both three- and four-player games have balanced
seats. Search agents use 150 iterations per decision. A separate timing study runs
28 games sequentially with a 450 ms budget and 25 ms reserve. Stage 5 records the
full protocol and seed policy.

The analysis checks the raw records and produces tables, pointwise 95% bootstrap
intervals and PNG/PDF figures. Against baseline lineups, the combined agent
reaches 75.10% win share with three players and 58.93% with four; the controls
reach 32.64% and 26.39%. The heuristic accounts for most of the gain. MAXN adds no
consistent improvement, and the Wasabi-removal result suggests a change worth
testing separately in four-player games.

The timing study records 633 searched decisions. One baseline decision takes
509.870 ms, exceeding the 500 ms target. These measurements describe observed
latency; they do not measure playing strength at 500 ms or guarantee a deadline on
another machine. The Stage 5 findings discuss the overrun.

## Limits

Weights and search settings were fixed before the study. The search uses a sampled
observation rather than information-set MCTS, and recorded seeds do not fully
control TAG's hidden-state sampling. Equal iteration counts also mean different
amounts of computation across variants. A framework call, scheduling pause or
garbage collection can delay a timed search. Compatibility with the marking
revision and compliance with the official report template are still unverified.

## Submission preparation

`SubmissionPlayer` and `competition.json` select heuristic/PARANOID with all four
weights at 1.0, a 400 ms budget and 25 ms reserve. The research baseline and the
eight Stage 5 configurations remain unchanged.

```bash
python3 scripts/package-submission.py --package groupAG --output build/groupAG.zip
python3 scripts/validate-submission.py build/groupAG.zip /path/to/TabletopGames \
  --output build/submission-validation
```

Group AG has one member. The export leaves development sources in place, and
report files are stored outside Git. Revalidate after changing the archive or the
marking framework. A separate confirmation with 2,016 new games gives the
heuristic 73.81% win share against baseline lineups with three players and 59.33%
with four. The gains over the controls are 42.36 and 33.04 percentage points, with
both pointwise 95% intervals above zero. These results use 150 iterations and are
separate from the package timing check. See the [Stage 6 record](docs/STAGE_6.md)
for details.