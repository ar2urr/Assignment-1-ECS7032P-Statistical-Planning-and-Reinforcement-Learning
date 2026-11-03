# Sushi Go coursework

Stages 1–3 of a six-stage development plan are implemented: the official TAG
BasicMCTS baseline, search reliability fixes and configurable Sushi Go evaluation.
The current comparison baseline includes the Stage 2 fixes. Stage 3 changes only
the leaf evaluation selected by configuration.

## Documentation

- [File and folder reference](docs/FILE_GUIDE.md)
- [Six-stage plan](docs/STAGES.md)
- [Stage 1 historical baseline](docs/STAGE_1.md)
- [Stage 2 changes and Git checkpoint](docs/STAGE_2.md)
- [Stage 3 heuristic, assumptions and Git checkpoint](docs/STAGE_3.md)
- [Coursework requirements](docs/REQUIREMENTS.md)
- [Assessment rubric mapping](docs/RUBRIC_REVIEW.md)
- [Agent constructors and configuration](sushigoagent/README.md)

## Structure

| Path | Contents |
|---|---|
| `sushigoagent/` | Five Java runtime files, four JSON configurations, README and upstream licence. |
| `tests/` | Focused JUnit scoring, reliability and integration tests. |
| `scripts/` | Compilation and validation helpers. |
| `docs/` | Requirements, six-stage plan and technical records. |
| `results/` | Validation logs; no playing-strength study has been completed. |
| `TAG_COMMIT` | Pinned official TAG revision. |

The development package is `sushigoagent`. Final submission requires the assigned
group package identifier with matching imports, paths and JSON class names.

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
Initial setup downloads TAG's dependencies.

The script checks the framework revision, builds TAG, compiles the local agent and
runs the four coursework JUnit classes. It leaves TAG source unmodified. The older
`check-stage1.sh` name delegates to the same current test runner.

## Validation and limits

The Stage 3 run passed 33 tests, including seven baseline and seven heuristic
games covering every seat in both three- and four-player games. Twelve measured
decisions were below 500 ms; the largest was 491.902 ms. These samples establish
functionality on the tested machine, not a hard deadline guarantee or stronger play.

The heuristic estimates are untuned. Opponent nodes still minimise the root
player's scalar value; the multiplayer modification belongs to Stage 4.
Controlled experiments belong to Stage 5. Compatibility with the course's marking
revision remains unverified. Fixed observations and iteration budgets are
repeatable with fixed seeds, but full-game replay also depends on upstream hidden
state sampling. No Git commit is created by the validation scripts.
