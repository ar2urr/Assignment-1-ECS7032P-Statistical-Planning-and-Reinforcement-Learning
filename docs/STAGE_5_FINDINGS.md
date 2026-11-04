# Stage 5 findings

The Sushi Go heuristic provides the clearest improvement at the tested budget.
MAXN does not show a consistent additional benefit. The Wasabi feature is a
candidate for revision in four-player games, and the measured time configurations
do not establish universal compliance with a 500 ms limit.

Evidence: [protocol](STAGE_5.md), [generated results and figures](../results/stage5/analysis/RESULTS.md),
[exact contrasts](../results/stage5/analysis/contrasts.csv),
[feature effects](../results/stage5/analysis/feature-effects.csv) and
[timing summaries](../results/stage5/analysis/timing-summary.csv).

## Scope and validation

The principal study contains 16,128 completed games: 32 cells with 504 games each,
balanced across seats. Each game contains one focal agent and homogeneous Random,
OSLA or corrected-baseline opponents. All search agents in these cells use 150
iterations per searched decision. A separate sequential timing study contains
28 complete games and 633 searched decisions.

All records passed the completeness, seat-balance, seed-uniqueness, source-hash,
score/rank, tie-credit, search-effort and timing-consistency checks. No incomplete
or disqualified game was accepted. The Java suite passed 43 tests; the Python
analysis suite passed three. Pilot games and warmups are excluded from these
study counts.

The baseline-versus-baseline controls produced win shares of 32.64% in three-player
games and 26.39% in four-player games. Their pointwise intervals include the
equal-agent references of one-third and one-quarter. This is a useful harness
check, not proof that every source of bias has been eliminated.

## Main effects

The strongest reference lineup consists of corrected-baseline opponents:

| Focal variant | Three-player win share | Four-player win share |
|---|---:|---:|
| Baseline | 32.64% | 26.39% |
| Heuristic only | 72.52% | 60.42% |
| MAXN only | 31.94% | 24.31% |
| Combined | 75.10% | 58.93% |

With PARANOID held constant, adding the heuristic raises win share against that
lineup by 39.88 percentage points in three-player games (95% CI 34.23 to 45.63)
and 34.03 points in four-player games (28.17 to 39.78). Adding it under MAXN also
helps. Across both opponent models, all twelve conditional heuristic-effect
intervals favour the heuristic in the tested contexts.

The combined agent exceeds the corrected baseline by 42.46 points in three-player
games (36.71 to 48.02) and 32.54 points in four-player games (26.79 to 38.39).
However, adding MAXN to the heuristic changes win share by only +2.58 points
in three-player games (-2.78 to 8.04) and -1.49 points in four-player games
(-7.44 to 4.46). All twelve conditional MAXN-effect intervals, and all six
factorial-interaction intervals, include zero. These results do not establish
equivalence, but they do not support a consistent MAXN improvement at 150 iterations.

Heuristic and combined variants achieve approximately 97–99% win share against
Random and OSLA. These near-ceiling results leave little room to distinguish
the two variants; the baseline lineups are more informative for that comparison.

A plausible explanation for the heuristic gains is its explicit treatment of
delayed rewards: the default leaf value contains awarded points, whereas sets,
Maki and Pudding can matter before their points are awarded. The feature removals
support this explanation. The weak MAXN effects could reflect limited search,
approximate opponent evaluation or hidden-state assumptions; this experiment does
not isolate which mechanism is responsible.

## Feature removals

These effects are conditional on MAXN and the other three feature weights staying
at 1.0. They are not estimates of every interaction between features.

- Pudding has the largest observed contribution: keeping it adds 27.78 points
  in three-player games (21.92 to 33.53) and 19.15 in four-player games
  (13.29 to 25.10).
- Maki adds 6.94 points (1.39 to 12.40) and 13.19 points (7.14 to 19.25),
  respectively.
- Incomplete sets add 11.21 points in three-player games (5.65 to 16.87).
  The four-player estimate is +6.15 points, but its interval touches zero
  (0.00 to 12.10), so the direction is less securely resolved.
- Wasabi has no clear three-player effect: +0.99 points (-4.27 to 6.55).
  In four-player games, keeping it reduces win share by 7.64 points
  (-13.69 to -1.68). Removing it produces 66.57% win share versus 58.93%
  for the full combined agent.

The Wasabi estimate uses independent-draw probabilities and ignores opponents'
drafting choices. Overvaluing future opportunities to fill Wasabi is one possible
explanation for its four-player result; the experiment does not demonstrate that
causal mechanism. The observed removal benefit identifies a candidate for a
separate confirmation study. The original weights remain unchanged so the
recorded primary comparisons retain their predefined configurations.

## Timing findings

Median decision times range from 399.55 to 403.83 ms; cell-level 95th percentiles
range from 422.43 to 423.83 ms. Of 633 searched decisions, one exceeds 500 ms:
the three-player baseline takes 509.870 ms in game index 2, seat 2, decision 2,
round index 0, with eight cards and eight actions. Its complete record is in
`results/stage5/raw/500ms_3p_baseline_vs_random.decisions.csv`.

The heuristic, MAXN and combined variants have no overrun in this timing sample;
their maxima are 478.753, 439.166 and 438.342 ms, respectively. These small
samples do not prove deadline compliance. Framework calls and runtime or OS
pauses cannot be interrupted by the current deadline checks. No pause trace was
recorded, so the cause of the baseline overrun is unresolved.

Separately, the parallel fixed-iteration runs record two decisions above 500 ms:
504.993 ms for the four-player heuristic against Random and 893.066 ms for the
four-player combined agent against the baseline. Those runs have iteration
budgets and concurrent load, not time budgets. Their diagnostic timings remain
in the raw records and summary; they are not pooled with the sequential timing
study or used to claim a 500 ms strength result.

## Interpretation and next stage

The pointwise bootstrap intervals are exploratory and unadjusted for multiple
comparisons. Opponent lineups, search depth, rollout length, weights and iteration
budget are fixed. The study does not establish tournament performance, a global
ranking of all agents or performance at other budgets. Hidden-state sampling in
TAG is not fully seed-controlled, and MAXN still uses a sampled closed-loop tree.

The evidence supports retaining the heuristic as a strong candidate for Stage 6.
The combined agent without Wasabi is another candidate, particularly for four
players. Selecting between them requires held-out confirmation rather than
reusing these same games as an independent test. Final preparation also requires
the marking-framework revision, group package identifier and report template.
A stricter deployment time margin and timing validation on the target environment
remain necessary before claiming every decision satisfies the competition limit.
