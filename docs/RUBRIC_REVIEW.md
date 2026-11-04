# Assessment rubric mapping

Source: `Assignment 1 Rubric.pdf`, ECS7032P Assignment 1. Review date:
3 November 2026.

## Experimental design

The rubric treats fewer than 500 games per agent pair as insufficient. The
principal protocol contains 504 games per reported lineup comparison:
32 fixed-iteration cells and 16,128 measured games. The baseline validation
games and experiment pilots are functionality tests only.

The top band expects more than one framework agent plus the developed agent's
base version. Stage 5 comparators are Random, OSLA and the corrected baseline.
The ablation design contains four conditions: baseline, heuristic only,
multiplayer modification only, and both modifications. Reliability fixes remain
constant across those conditions to avoid confounding the algorithmic effects.
Four further conditions remove individual heuristic features. A separate 28-game
timing study measures decisions at the 500 ms target; it is not a large-sample
playing-strength comparison at that budget. The [Stage 5 protocol](STAGE_5.md)
and [results](../results/stage5/analysis/RESULTS.md) retain this distinction.

## Required evidence

| Rubric area | Evidence or remaining deliverable | Stage |
|---|---|---|
| Abstract | Concise motivation, method, main experiments and actual findings. | 6 |
| Problem definition | Research question and game-specific challenges, with precise contributions. | 3–4, finalised in 6 |
| Background | Sushi Go, TAG and baseline algorithms; critical comparison of relevant literature. | 3–4, finalised in 6 |
| Approach | Correct, substantive search modifications with game-specific rationale and clear attribution. | 2–4 |
| Experimental rigour | Two framework references, baseline comparison, controlled ablations, adequate sample sizes and seat balancing. | 5 |
| Replication | Revisions, configurations, budgets, player counts, seed policy, hardware, commands, raw data and nondeterminism limits. | 1–5 |
| Results analysis | Uncertainty, effect sizes, explanations, failure cases and limitations. | 5–6 |
| Conclusions | Evidence-supported findings and motivated future work. | 6 |
| Presentation | Coherent narrative, readable annotated plots, informative captions and consistent references. | 6 |

## Baseline and originality

Stage 1 provides a traceable control. An unchanged framework agent would not
satisfy the originality criterion as a final approach. The implemented heuristic and
MaxN-style variant require justification against the lectures and prior work;
their empirical effects are assessed by Stage 5 and do not alone establish novelty.

## Unresolved specification details

- Competition format: 3 players / 1 second versus 4 players / 500 ms.
- Interpretation of "per pair of agents" in multiplayer comparisons.
- Exact marking-framework revision and official report template.
