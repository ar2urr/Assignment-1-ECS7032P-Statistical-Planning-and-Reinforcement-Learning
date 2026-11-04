# Experiment support

This directory contains the external evaluation harness and feature-ablation
configurations. It is outside the agent's runtime submission package.

| Path | Responsibility |
|---|---|
| `coursework/experiments/ExperimentRunner.java` | Runs one balanced lineup, validates actions/results, records game outcomes and optional decision timings. |
| `configs/combined_no_sets.json` | Combined model with setWeight=0. |
| `configs/combined_no_wasabi.json` | Combined model with wasabiWeight=0. |
| `configs/combined_no_maki.json` | Combined model with makiWeight=0. |
| `configs/combined_no_pudding.json` | Combined model with puddingWeight=0. |
| `requirements-analysis.txt` | Minimum Python dependencies for numerical analysis and figures. |
| `analysis-environment.txt` | Exact package versions used for the recorded analysis. |

The parent `scripts/` directory contains `build-experiments.sh`, `run-study.py`
and `analyse-study.py`. The complete protocol and reproduction commands are in
[Stage 5](../docs/STAGE_5.md). The principal study is stored under
`results/stage5/` with separate raw, snapshot and analysis subdirectories.
