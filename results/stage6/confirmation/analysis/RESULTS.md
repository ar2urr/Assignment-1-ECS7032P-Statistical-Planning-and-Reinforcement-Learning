# Stage 6 independent confirmation

The heuristic/PARANOID policy was selected from Stage 5 before this run. Both cells at each player count use new game seeds, 150 iterations and baseline opponents. The control is baseline versus baseline. These games are separate from the primary study and do not estimate playing strength under the deployment time budget.

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

Intervals use 5,000 within-seat bootstrap resamples with independent cell draws. They are pointwise and unadjusted for multiple comparisons. This confirms or challenges a preselected policy; it does not compare it with every alternative. TAG hidden-state sampling remains incompletely seed-controlled. Fixed-iteration latency diagnostics are not pooled with the separate sequential deployment check.
