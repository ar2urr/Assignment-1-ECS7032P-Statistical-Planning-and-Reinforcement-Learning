# Assessment rubric mapping

Source: `Assignment 1 Rubric.pdf`, ECS7032P Assignment 1. Review date:
3 November 2026.

## Experimental design

The rubric treats fewer than 500 games per agent pair as insufficient. The
principal study is planned around at least 500 games per reported comparison,
rather than 500 games pooled across the study. The seven baseline validation
games are functionality tests only.

The top band expects more than one framework agent plus the developed agent's
base version. Planned comparators are Random, OSLA and the corrected baseline.
The ablation design contains four conditions: baseline, heuristic only,
multiplayer modification only, and both modifications. Reliability fixes remain
constant across those conditions to avoid confounding the algorithmic effects.

## Required evidence

| Rubric area | Planned evidence | Stage |
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
satisfy the originality criterion as a final approach. The planned heuristic and
MaxN-style variant require justification against the lectures and prior work;
their value must be established experimentally.

## Unresolved specification details

- Competition format: 3 players / 1 second versus 4 players / 500 ms.
- Interpretation of "per pair of agents" in multiplayer comparisons.
- Exact marking-framework revision and official report template.
