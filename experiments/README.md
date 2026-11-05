# Experiment support

This directory holds the game runner and configurations that remove individual
heuristic features. These files support experiments and are excluded from the
submitted agent package.

| Path | Responsibility |
|---|---|
| `coursework/experiments/ExperimentRunner.java` | Runs one balanced lineup, validates actions/results, records game outcomes and optional decision timings. |
| `configs/combined_no_sets.json` | Combined model with setWeight=0. |
| `configs/combined_no_wasabi.json` | Combined model with wasabiWeight=0. |
| `configs/combined_no_maki.json` | Combined model with makiWeight=0. |
| `configs/combined_no_pudding.json` | Combined model with puddingWeight=0. |
| `requirements-analysis.txt` | Minimum Python dependencies for numerical analysis and figures. |
| `analysis-environment.txt` | Exact package versions used for the recorded analysis. |

The build, study and analysis commands are `scripts/build-experiments.sh`,
`scripts/run-study.py` and `scripts/analyse-study.py`. [Stage
5](../docs/STAGE_5.md) explains how to repeat the study. Its records are under
`results/stage5/`, split into raw data, source snapshots and analysis outputs.