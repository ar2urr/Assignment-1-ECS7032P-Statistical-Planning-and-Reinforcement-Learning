# Project file reference

## Folder layout

All Stage 1–6 source and support files are included in the standalone repository.

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

`build/`, local IDE settings, virtual environments and compiled classes are
generated locally and excluded from Git. Study source snapshots use `.java.txt`
to preserve exact source bytes without creating duplicate Java classes in IDEs.

## Runtime package

For installation into TAG, the complete `sushigoagent/` directory belongs at
`TAG/src/main/java/sushigoagent/`. The submission export uses
`groupAG/`, installed at `TAG/src/main/java/groupAG/`, with matching imports,
JSON class names and paths. Development tests, experiments and documents remain outside this package.

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

`docs/STAGES.md` tracks all six stages. `STAGE_1.md` through `STAGE_6.md` document
the respective implementation or experiment. Stage 1 records commit `a008e35`;
Stages 2/3 share `db219f5`; Stages 4/5 share `26b8981`. Historical
per-stage staging examples do not change those actual checkpoints.

`docs/STAGE_5_FINDINGS.md` interprets the measured effects and timing limitations.
`docs/REQUIREMENTS.md` retains the coursework constraints and unresolved course
details. `docs/RUBRIC_REVIEW.md` maps criteria to evidence and remaining report work.
`results/stage5/analysis/RESULTS.md` contains generated findings tables, with
`summary.csv`, `contrasts.csv`, `feature-effects.csv` and `timing-summary.csv`
preserving numeric precision. `validation.json` records the analysis version,
hash and validated counts. The three figure pairs are `main-comparisons`,
`feature-ablations` and `decision-timing`, each in PNG and vector PDF format.

The 43-test Java suite is captured in `results/stage5-validation.txt`.
`results/stage5-analysis-tests.txt` records the three Python tests;
`results/stage5-analysis-validation.txt` records full-study data validation. Earlier
validation files preserve the evidence from their development stages. Raw study
data includes per-cell metadata and progress logs; separate warmup and pilot games
are excluded from the measured results.

## Build and placement

From the repository root:

```bash
bash scripts/check.sh /path/to/TabletopGames
```

Dependencies are Java 21 and Maven. `MVN` selects a Maven executable and
`MAVEN_REPO` selects a dependency cache. The script compiles local files against a
separate TAG checkout, so validation needs no source copies into TAG.

If using TAG as the IDE project, Java tests belong under `TAG/src/test/java/`
with the same package subdirectories. The external experiment harness belongs
under `TAG/src/main/java/coursework/experiments/` only for local experimentation;
it is not part of the submitted agent. The VM option
`-Dcoursework.project=/path/to/repository` locates test JSON files. The standalone
scripts already provide these paths and require no manual relocation.

Stage 6 evidence is in `results/stage6/`: `standalone/` records fresh-framework
checks and decision measurements for `groupAG`; `development-standalone/` retains
the earlier development-namespace check; `confirmation/` contains the independent
replication. `results/stage6-validation.txt` and `stage6-python-tests.txt` record
the current test suites. Exported ZIP and Word report deliverables are kept
outside the repository; `docs/STAGE_6.md` records remaining submission details.
