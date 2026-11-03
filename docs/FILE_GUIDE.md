# Project file reference

## Folder layout

The standalone repository contains these paths. All Stage 2/3 source and support
files are present; no additional empty files are required.

```text
repository/
  sushigoagent/                 Runtime package and agent configurations
  tests/
    coursework/validation/     Baseline integration checks
    sushigoagent/              Reliability, heuristic and integration checks
  scripts/                     Local validation runner
  docs/                        Requirements and six-stage technical records
  results/                     Captured validation logs
  README.md
  TAG_COMMIT
  .gitignore
```

## Runtime package

For installation into TAG, the complete `sushigoagent/` directory belongs at
`TAG/src/main/java/sushigoagent/`. The assigned submission package identifier is
still required for final packaging, with matching imports, JSON class names and
paths. Development tests and documents remain outside the runtime package.

| File in `sushigoagent/` | Responsibility | Stage |
|---|---|---|
| `BasicMCTSPlayer.java` | Agent entry point, legal-action fallback, RNG lifecycle, copies and search statistics. | 1, modified in 2 |
| `BasicMCTSParams.java` | Registered search settings, validation, heuristic selection and player construction. | 1, modified in 2 |
| `BasicTreeNode.java` | UCB selection, expansion, rollout, scalar backup and most-visited action. | 1, modified in 2 |
| `SearchBudget.java` | Wall-clock deadline and exact simulation-operation accounting. | 2 |
| `SushiGoHeuristic.java` | Public scoring prospects and four independent feature weights. | 3 |
| `baseline_iterations.json` | Corrected BasicMCTS with TAG evaluation and 150 iterations. | 1 |
| `baseline_500ms.json` | Corrected BasicMCTS with 450 ms budget and 25 ms reserve. | 1 |
| `heuristic_iterations.json` | Same iteration settings with Sushi Go evaluation. | 3 |
| `heuristic_500ms.json` | Same timing settings with Sushi Go evaluation. | 3 |
| `README.md` | Constructors, installation, parameter configuration and limits. | 1–3 |
| `LICENSE-TAG.txt` | Licence for the official BasicMCTS source. | 1 |

## Tests and support

| File | Responsibility |
|---|---|
| `scripts/check.sh` | Checks the TAG revision, compiles sources and runs the four coursework test classes. |
| `scripts/check-stage1.sh` | Compatibility alias for the current runner. |
| `tests/coursework/validation/Stage1SmokeTest.java` | JSON loading/copying and seven baseline games with observation checks. |
| `tests/sushigoagent/SearchReliabilityTest.java` | Legal actions, all budgets, RNG behaviour, copies, Chopsticks and timing. |
| `tests/sushigoagent/SushiGoHeuristicTest.java` | Scoring cases, exhaustive small rank standings, terminal evaluation and weights. |
| `tests/sushigoagent/Stage3IntegrationTest.java` | Seven heuristic games, legal actions, unchanged observations and timing. |
| `docs/STAGES.md` | All six stages with status and completion criteria. |
| `docs/STAGE_1.md` | Historical baseline at commit `a008e35`. |
| `docs/STAGE_2.md` | Reliability changes, validation and separate Git staging commands. |
| `docs/STAGE_3.md` | Heuristic equations, hypothesis, assumptions, sources, tests and Git commands. |
| `docs/REQUIREMENTS.md` | Assignment constraints and unresolved details. |
| `docs/RUBRIC_REVIEW.md` | Assessment criteria mapped to planned evidence. |
| `results/stage1-validation.txt` | Historical three-test baseline output. |
| `results/stage2-validation.txt` | Seventeen-test reliability output. |
| `results/stage3-validation.txt` | Thirty-three-test combined validation output. |
| `TAG_COMMIT` | Exact framework revision. |
| `.gitignore` | Excludes generated classes, dependencies and local IDE files. |

## Validation

From the repository root:

```bash
bash scripts/check.sh /path/to/TabletopGames
```

Dependencies are Java 21 and Maven. `MVN` selects a Maven executable and
`MAVEN_REPO` selects a dependency cache. The script compiles local files against a
separate TAG checkout, so validation needs no source copies into TAG.

For IDE installation, Java test paths map under `TAG/src/test/java/` with the same
package subdirectories. The VM option `-Dcoursework.project=/path/to/repository`
locates JSON files. The runtime package remains under `TAG/src/main/java/`.

Stages 4–6 remain planned. No multiplayer extension, long experiment or final
submission archive is included in the current files.
