# Assessment rubric mapping

Source: `Assignment 1 Rubric.pdf`, ECS7032P Assignment 1. Review date:
4 November 2026.

## Experimental design

The rubric calls fewer than 500 games per agent pair insufficient. The main study
has 504 games per lineup: 32 cells and 16,128 measured games in total. Baseline
checks and pilot games test functionality and are kept separate.

The top band asks for more than one framework agent and the developed agent's
baseline. Stage 5 compares against Random, OSLA and the corrected baseline. Four
conditions isolate the heuristic, the multiplayer change and their combination.
All share the same reliability fixes. Four more conditions remove heuristic
features individually. A separate 28-game study checks latency at the 500 ms
target. Its sample does not support a claim about playing strength at that budget;
see the [protocol](STAGE_5.md) and
[results](../results/stage5/analysis/RESULTS.md).

## Required evidence

| Rubric area | Evidence or remaining deliverable | Stage |
|---|---|---|
| Abstract | Concise motivation, method, main experiments and actual findings. | 6 |
| Problem definition | Research question and game-specific challenges, with precise contributions. | 3–4, finalised in 6 |
| Background | Sushi Go, TAG and baseline algorithms; critical comparison of relevant literature. | 3–4, finalised in 6 |
| Approach | Correct, substantive search modifications with game-specific rationale and clear attribution. | 2–4 |
| Experimental rigour | Two framework references, baseline comparison, controlled ablations, adequate sample sizes and seat balancing. | 5 |
| Replication | Revisions, configurations, budgets, player counts, seed policy, hardware, commands, raw data and nondeterminism limits. | 1–6 |
| Results analysis | Uncertainty, effect sizes, explanations, failure cases and limitations. | 5–6 |
| Conclusions | Evidence-supported findings and motivated future work. | 6 |
| Presentation | Coherent narrative, readable annotated plots, informative captions and consistent references. | 6 |

## Baseline and originality

Stage 1 gives the project a traceable baseline. Submitting that unchanged agent
would not meet the originality criterion. The report must explain why the
heuristic and MAXN variant suit Sushi Go, using the lectures and prior work. Stage
5 measures their effects, but a performance gain alone does not make an algorithm
novel.

## Stage 6 evidence

The editable report stays outside Git. Its four pages of main content cover the
motivation, search changes, background, experiments and deployment choice.
References and AI-use notes occupy page five. The layout still needs checking
against the official course template.

Stage 6 tests the selected heuristic/PARANOID policy on 2,016 further games: 504
per cell, with new recorded seeds and bootstrap intervals that preserve seat
proportions. Its raw data and completion records are separate from Stage 5. The
groupAG ZIP is also compiled in a fresh TAG tree and tested through JSON loading,
direct construction and copied players. The [Stage 6 record](STAGE_6.md) gives the
results.

These checks support the submission's evidence. They do not predict a mark,
establish algorithmic novelty, measure strength under equal time budgets or verify
an unknown marking revision.

## Unresolved specification details

- Competition format: 3 players / 1 second versus 4 players / 500 ms.
- Interpretation of "per pair of agents" in multiplayer comparisons.
- Exact marking-framework revision and official report template.
