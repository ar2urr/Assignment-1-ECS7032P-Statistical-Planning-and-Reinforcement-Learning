# Project file reference

## Folder layout

This repository contains the source, tests and supporting files for all six
stages.

```text
repository/
  sushigoagent/                   Runtime package and nine agent configurations
  experiments/
    coursework/experiments/       External Java experiment runner
    configs/                     Four feature-removal configurations
    requirements-analysis.txt    Python analysis dependencies
  tests/
    coursework/validation/       Baseline integration checks
    coursework/experiments/      Experiment harness checks
    sushigoagent/                Search, scoring and integration checks
    test_analysis.py             Bootstrap and validation checks
    test_packaging.py            Source archive checks
  scripts/                       Build, validation, study and analysis commands
  docs/                          Requirements and six-stage technical records
  results/
    stage1-validation.txt        Historical baseline validation
    stage2-validation.txt        Reliability validation
    stage3-validation.txt        Heuristic validation
    stage4-validation.txt        Multiplayer validation
    stage5-validation.txt        Combined Java validation
    stage5/
      manifest.json              Protocol, hashes, machine and runtime details
      snapshot/                  Measured runtime sources and configurations
      raw/                       Game/decision CSVs and completion metadata
      analysis/                  Validated summaries and PNG/PDF figures
  README.md
  TAG_COMMIT
  .gitignore
```

Git excludes generated build files, local IDE settings, virtual environments and
compiled classes. Saved study sources use `.java.txt` so Java editors do not treat
them as duplicate classes. These snapshots keep the exact code used for each
recorded run.

## Runtime package

For development, place `sushigoagent/` at `TAG/src/main/java/sushigoagent/`. The
submission ZIP instead contains `groupAG/`, which belongs at
`TAG/src/main/java/groupAG/`. The exporter updates package declarations and JSON
class names. Tests, experiments and documentation stay outside the submitted
package.

| File in `sushigoagent/` | Responsibility | Stage |
|---|---|---|
| `BasicMCTSPlayer.java` | Agent entry point, legal-action fallback, RNG lifecycle, copies and search statistics. | 1–2 |
| `BasicMCTSParams.java` | Registered search settings, opponent model, validation, heuristic selection and construction. | 1–2, 4 |
| `BasicTreeNode.java` | UCB selection, expansion, rollout, value-vector backup and most-visited action. | 1–2, 4 |
| `SearchBudget.java` | Wall-clock deadline and exact simulation-operation accounting. | 2 |
| `SubmissionPlayer.java` | Frozen deployment defaults and type-preserving copy. | 6 |
| `competition.json` | Loads SubmissionPlayer with the conservative time margin. | 6 |
| `SushiGoHeuristic.java` | Public scoring prospects and four independent feature weights. | 3 |
| `baseline_iterations.json` | Corrected PARANOID BasicMCTS with TAG evaluation and 150 iterations. | 1, 4 |
| `baseline_500ms.json` | Same model/evaluation with 450 ms budget and 25 ms reserve. | 1, 4 |
| `heuristic_iterations.json` | PARANOID with Sushi Go evaluation and 150 iterations. | 3–4 |
| `heuristic_500ms.json` | Same model/evaluation with the time budget. | 3–4 |
| `multiplayer_iterations.json` | MAXN with TAG evaluation and 150 iterations. | 4 |
| `multiplayer_500ms.json` | Same model/evaluation with the time budget. | 4 |
| `combined_iterations.json` | MAXN with Sushi Go evaluation and 150 iterations. | 4 |
| `combined_500ms.json` | Same model/evaluation with the time budget. | 4 |
| `README.md` | Constructors, installation, configuration and limits. | 1–6 |
| `LICENSE-TAG.txt` | Licence for the official BasicMCTS source. | 1 |

## Tests and tools

| File | Responsibility |
|---|---|
| `scripts/package-submission.py` | Deterministic package-only ZIP, namespace export and overwrite protection. |
| `scripts/validate-submission.py` | Fresh TAG compilation and isolated archive execution. |
| `scripts/run-confirmation.py` | Separate preselected-policy replication on new seeds. |
| `tests/sushigoagent/SubmissionPlayerTest.java` | Deployment defaults, JSON construction, copying and seed bounds. |
| `tests/coursework/validation/SubmissionProbe.java` | External standalone archive probe with no development-package imports. |
| `tests/test_packaging.py` | Archive membership, namespace conversion, repeatability and preservation. |
| `scripts/check.sh` | Checks the TAG revision, compiles sources and runs seven focused JUnit classes. |
| `scripts/check-stage1.sh` | Compatibility alias for the current runner. |
| `scripts/build-experiments.sh` | Builds the external harness with a frozen framework JAR. |
| `scripts/run-study.py` | Runs balanced comparison cells, saves provenance and supports guarded resumption. |
| `scripts/analyse-study.py` | Validates raw records, estimates uncertainty and exports tables/figures. |
| `experiments/coursework/experiments/ExperimentRunner.java` | Creates seeded games, collects final outcomes and measures focal-agent decisions. |
| `experiments/configs/combined_no_sets.json` | Combined agent with incomplete-set weight zero. |
| `experiments/configs/combined_no_wasabi.json` | Combined agent with unused-Wasabi weight zero. |
| `experiments/configs/combined_no_maki.json` | Combined agent with Maki weight zero. |
| `experiments/configs/combined_no_pudding.json` | Combined agent with Pudding weight zero. |
| `tests/coursework/validation/Stage1SmokeTest.java` | JSON loading/copying and baseline games with observation checks. |
| `tests/sushigoagent/SearchReliabilityTest.java` | Legal actions, budgets, RNG behaviour, copies, Chopsticks and timing. |
| `tests/sushigoagent/SushiGoHeuristicTest.java` | Scoring cases, small rank standings, terminal evaluation and weights. |
| `tests/sushigoagent/Stage3IntegrationTest.java` | Heuristic games, legal actions, unchanged observations and timing. |
| `tests/sushigoagent/MultiplayerSearchTest.java` | Opponent selection, repeated turns, vector expiry, copying and complete games. |
| `tests/coursework/experiments/ExperimentRunnerTest.java` | Seat balance, seed streams and tie-credit accounting. |
| `tests/test_analysis.py` | Stratified bootstrap properties and incomplete-cell rejection. |

## Documents and evidence

`docs/STAGES.md` lists all six stages; `STAGE_1.md` through `STAGE_6.md` record
their changes and checks. The commits are `a008e35` for Stage 1, `db219f5` for
Stages 2/3, `26b8981` for Stages 4/5 and `fa473bb` for Stage 6. Earlier documents
retain their original staging examples.

`docs/STAGE_5_FINDINGS.md` explains the results and timing limits.
`docs/REQUIREMENTS.md` records coursework rules and unresolved details;
`docs/RUBRIC_REVIEW.md` maps the evidence to marking criteria. The results summary
is in `results/stage5/analysis/RESULTS.md`. Exact values are kept in
`summary.csv`, `contrasts.csv`, `feature-effects.csv` and `timing-summary.csv`.
`validation.json` records the analysis version, hash and checked counts. The
figures are `main-comparisons`, `feature-ablations` and `decision-timing`, each
saved as PNG and vector PDF.

`results/stage5-validation.txt` records 43 passing Java tests.
`stage5-analysis-tests.txt` records three Python tests, and
`stage5-analysis-validation.txt` records the full data check. Earlier logs
describe their own development stages. Each study cell has raw data, completion
metadata and a progress log. Warmup and pilot games are excluded from the measured
results.

## Build and placement

From the repository root:

```bash
bash scripts/check.sh /path/to/TabletopGames
```

The scripts need Java 21 and Maven. Set `MVN` to choose Maven and `MAVEN_REPO` to
choose a dependency cache. Validation compiles local agent files against a
separate TAG checkout, so source files do not need to be copied into TAG.

When using TAG as the IDE project, put Java tests under `TAG/src/test/java/` with
their existing package paths. The experiment harness can go under
`TAG/src/main/java/coursework/experiments/` for local runs; it is excluded from
the submission. The VM option `-Dcoursework.project=/path/to/repository` locates
test JSON files. The standalone scripts already supply these paths.

In `results/stage6/`, `standalone/` contains the groupAG package checks and
timings. `development-standalone/` keeps the earlier check using the development
namespace, while `confirmation/` contains the separate strength study. The test
logs are `results/stage6-validation.txt` and `stage6-python-tests.txt`. The ZIP
and Word report are stored outside Git. `docs/STAGE_6.md` lists the remaining
submission details.