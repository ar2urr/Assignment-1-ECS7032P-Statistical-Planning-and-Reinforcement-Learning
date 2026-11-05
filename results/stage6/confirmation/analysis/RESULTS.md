# Stage 6 independent confirmation

Heuristic/PARANOID was selected from Stage 5 before this run began. At each player count, it and the baseline control face baseline opponents with new game seeds and 150 iterations per decision. These games are separate from the main study. They test the chosen policy at that iteration budget, not its strength under the deployment time limit.

| Players | Variant | Games | Win share | Pointwise 95% CI |
|---|---|---:|---:|---|
| 3 | baseline | 504 | 31.45% | 27.58 to 35.42% |
| 3 | heuristic | 504 | 73.81% | 69.94 to 77.68% |
| 4 | baseline | 504 | 26.29% | 22.62 to 30.16% |
| 4 | heuristic | 504 | 59.33% | 55.16 to 63.59% |

| Players | Heuristic minus baseline | Pointwise 95% CI |
|---|---:|---|
| 3 | 42.36 pp | 36.90 to 47.92 pp |
| 4 | 33.04 pp | 27.38 to 38.79 pp |

Intervals use 5,000 bootstrap resamples within seats and independent draws between cells. They are pointwise and unadjusted for multiple comparisons. This run tests a policy chosen in advance; it does not compare every alternative. Recorded seeds still do not fully control TAG's hidden-state sampling. Timings from these iteration runs stay separate from the sequential deployment check.
