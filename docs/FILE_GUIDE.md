# Project file reference

## Agent package

Development package: `sushigoagent`. Installation path inside TAG:
`src/main/java/sushigoagent/`. Final submission requires the assigned group
package identifier and matching imports, JSON class names and paths.

| File in `sushigoagent/` | Role |
|---|---|
| `BasicMCTSPlayer.java` | Search entry point, parameter access and action selection. |
| `BasicMCTSParams.java` | Exploration constant, rollout length, tree depth, heuristic and player factory. |
| `BasicTreeNode.java` | Selection, expansion, rollout, backup and root action selection. |
| `baseline_iterations.json` | 150 iterations, K = sqrt(2), rollout length 10, tree depth 5 and seed 7032. |
| `baseline_500ms.json` | Provisional timing configuration: 450 ms budget and 25 ms early-stop margin. |
| `README.md` | Installation, constructors, configuration and limitations. |
| `LICENSE-TAG.txt` | Upstream MIT licence. |

The three Java files originate from official TAG. Their only modification is the
declaration `package sushigoagent;`. Both JSON files instantiate
`sushigoagent.BasicMCTSParams` through TAG's configuration loader.

## Development support

These files remain outside the agent package in the standalone coursework project.

| File | Role |
|---|---|
| `README.md` | Project overview, setup and baseline checkpoint commands. |
| `TAG_COMMIT` | Exact framework revision. |
| `.gitignore` | Exclusions for build output, dependencies and local IDE settings. |
| `scripts/check-stage1.sh` | Source-provenance check, compilation and focused test execution. |
| `tests/coursework/validation/Stage1SmokeTest.java` | Configuration loading, parameter copying, completed games, action legality and preservation of observed fields. |
| `docs/STAGES.md` | Six-stage implementation plan and completion criteria. |
| `docs/REQUIREMENTS.md` | Assignment constraints and unresolved specification details. |
| `docs/RUBRIC_REVIEW.md` | Assessment criteria mapped to planned evidence. |
| `docs/FILE_GUIDE.md` | File inventory and responsibilities. |
| `docs/STAGE_1.md` | Baseline provenance, validation results and known limitations. |
| `results/stage1-validation.txt` | Latest focused validation log. |

## Validation

Command from the standalone coursework project root:

```bash
bash scripts/check-stage1.sh /path/to/TabletopGames
```

The script expects the standalone layout above and compiles the local agent
package against a separate TAG checkout. Dependencies are Java 21 and Maven.
`MVN` selects an alternative Maven executable; `MAVEN_REPO` selects an alternative
dependency-cache directory.

An IntelliJ installation inside TAG places the package under `src/main/java`.
JUnit tests installed under `src/test/java` require the VM option
`-Dcoursework.project=/path/to/coursework` to locate the JSON configurations.

## Stage boundary

Stage 1 includes baseline source, configurations, documentation and validation.
Custom evaluation and multiplayer search belong to later implementation stages.
