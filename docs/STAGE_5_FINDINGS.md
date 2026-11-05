# Stage 5 findings

The heuristic gives the clearest improvement at 150 iterations. MAXN adds no
consistent benefit. Removing Wasabi looks promising in four-player games, but
needs a separate test. The timing sample also includes an overrun, so it cannot
guarantee that every decision meets 500 ms.

Evidence: [protocol](STAGE_5.md), [generated results and figures](../results/stage5/analysis/RESULTS.md),
[exact contrasts](../results/stage5/analysis/contrasts.csv),
[feature effects](../results/stage5/analysis/feature-effects.csv) and
[timing summaries](../results/stage5/analysis/timing-summary.csv).

## Scope and validation

The main study completes 16,128 games in 32 cells of 504, with balanced seats.
Each game has one focal agent against Random, OSLA or corrected-baseline opponents
of the same type. All search agents use 150 iterations per decision. A separate
sequential study measures 633 decisions across 28 timing games.

All records pass checks for completeness, balanced seats, unique seeds, source
hashes, scores, ranks, tie credit, search effort and timing consistency. No
incomplete or disqualified game is accepted. The corresponding suites pass 43 Java
tests and three Python tests. These game counts exclude pilots and warmups.

Baseline against baseline gives 32.64% win share with three players and 26.39%
with four. Both pointwise intervals include the equal-agent reference, one-third
or one-quarter. This supports the harness check, although it cannot rule out every
source of bias.

## Main effects

The strongest reference lineup consists of corrected-baseline opponents:

| Focal variant | Three-player win share | Four-player win share |
|---|---:|---:|
| Baseline | 32.64% | 26.39% |
| Heuristic only | 72.52% | 60.42% |
| MAXN only | 31.94% | 24.31% |
| Combined | 75.10% | 58.93% |

With PARANOID held fixed, the heuristic raises win share against baseline lineups
by 39.88 percentage points with three players (95% CI 34.23 to 45.63) and 34.03
with four (28.17 to 39.78). It also helps under MAXN. All twelve conditional
heuristic-effect intervals favour the heuristic in these tests.

The combined agent beats the baseline by 42.46 points with three players (36.71 to
48.02) and 32.54 with four (26.79 to 38.39). Adding MAXN to the heuristic alone
changes win share by +2.58 points (-2.78 to 8.04) and -1.49 (-7.44 to 4.46). Every
conditional MAXN interval and all six interaction intervals include zero. This
does not prove equivalence, but the study finds no consistent extra gain from MAXN
at 150 iterations.

The heuristic and combined agents reach about 97-99% win share against Random and
OSLA. With little room left to improve, these opponents reveal less about the
difference between the two variants than the baseline lineups do.

The heuristic may help because it values rewards before they appear in the score.
A short rollout can end before a set is completed or Maki and Pudding points are
awarded, and the feature removals support the usefulness of these estimates.
MAXN's weaker effect could come from limited search, inaccurate opponent
evaluation or assumptions about hidden cards. The experiment does not separate
those explanations.

## Feature removals

Each removal keeps MAXN and the other three weights at 1.0. The results therefore
describe that setup, rather than every possible interaction between features.

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

Wasabi assumes independent draws and ignores opponents' drafting choices. It may
overestimate later Nigiri opportunities, but the experiment does not establish
that cause. Removing it is worth testing on new games. The original weights and
primary results are kept unchanged.

## Timing findings

Median decision times range from 399.55 to 403.83 ms; cell-level 95th percentiles
range from 422.43 to 423.83 ms. Of 633 searched decisions, one exceeds 500 ms:
the three-player baseline takes 509.870 ms in game index 2, seat 2, decision 2,
round index 0, with eight cards and eight actions. Its complete record is in
`results/stage5/raw/500ms_3p_baseline_vs_random.decisions.csv`.

No heuristic, MAXN or combined decision exceeds 500 ms in this timing sample;
their maxima are 478.753, 439.166 and 438.342 ms. A small sample cannot prove
deadline compliance. The checks cannot interrupt a framework call or runtime/OS
pause, and no pause trace was captured to explain the baseline overrun.

The parallel iteration runs also record 504.993 ms for the four-player heuristic
against Random and 893.066 ms for the four-player combined agent against baseline.
Those runs use iteration budgets and concurrent workers. Their times stay in the
raw records, but are not pooled with the sequential time-budget study or treated
as evidence of strength at 500 ms.

## Interpretation and next stage

The bootstrap intervals are exploratory and unadjusted for multiple comparisons.
Lineups, depth, rollout length, weights and iteration budget are fixed. The study
cannot establish tournament performance or a ranking at other settings. TAG's
hidden-state sampling is not fully controlled by recorded seeds, and MAXN still
reasons from one sampled tree.

These results made heuristic/PARANOID the candidate retained in Stage 6. The
combined variant without Wasabi remains another option, especially with four
players, but that change has not been tested under PARANOID. Stage 6 records a
separate confirmation of the retained policy, the groupAG export and a larger
timing margin. The marking TAG version and official template are still unresolved,
and timing on another machine may differ.