# Stage 4: multiplayer value selection

Status: implemented and validated on 3 November 2026. Stages 4 and 5 are prepared for one combined
Git checkpoint, following the Stage 2/3 checkpoint `db219f5`.

## Research question

Does modelling each opponent as maximising its own return improve Sushi Go play
relative to assuming all opponents minimise the focal player's return?

The self-interest hypothesis fits independent competition for points, but does
not imply better decisions: evaluation error, hidden cards, finite search and
extra evaluation cost may offset the benefit. Stage 5 tests this factor both with
TAG's state evaluation and with the custom heuristic.

## Implementation

`BasicMCTSParams.OpponentModel` has two registered settings:

- `PARANOID` remains the default and preserves the previous opponent model.
- `MAXN` stores and backs up one evaluation value for each player.

Each node now contains a vector of accumulated values. At a leaf, MAXN evaluates
the same state once for each player. Backup adds the entire vector at every
ancestor; it does not negate values or change their player index.

For a child with N visits and accumulated value S[p], selection uses:

```text
MAXN:     S[actual mover] / (N + epsilon) + exploration
PARANOID: sign * S[root player] / (N + epsilon) + exploration
```

`sign` is +1 on the root player's turns and -1 on other turns. Exploration keeps
the existing UCB formula, and the most-visited root action remains the decision.
The current state determines the mover; depth parity is unsuitable for multiple
players and consecutive Chopsticks choices.

PARANOID evaluates only the root player's component. MAXN checks every component
for finite values. A deadline expiry partway through evaluation discards the
incomplete vector instead of backing up some players' values but not others'.

This is MaxN-style MCTS: node statistics are sample means. It is not a replacement
with classical deterministic max-n search, and the permitted BasicMCTS structure
remains the development basis. No advanced `players.mcts` implementation is used.

## Configurations

| Evaluation | PARANOID | MAXN |
|---|---|---|
| TAG default | `baseline_iterations.json` | `multiplayer_iterations.json` |
| Sushi Go features | `heuristic_iterations.json` | `combined_iterations.json` |

Matching `_500ms.json` files use 450 ms budgets with a 25 ms reserve. Every JSON
file states its opponent model explicitly. Search depth, rollout length,
exploration constant and seed policy are otherwise matched. Parameter copying
retains the selected model.

## Validation and development findings

The [Stage 4 log](../results/stage4-validation.txt) records 40 passing JUnit tests.
The subsequent [Stage 5 log](../results/stage5-validation.txt) adds three harness
tests and records 43 passing tests. Both include all prior scoring and reliability
regressions.

The synthetic game has a branch where an opponent benefits by helping the root
player and another branch that harms the root while giving the opponent less.
MAXN and PARANOID correctly select different root actions across every seat in
three- and four-player games and two seeds. Separate tests cover repeated turns,
configuration copying, opponent values that are not finite, partial vector expiry
and combined operation budgets. Both new variants also complete games in every
seat for both player counts.

An initial synthetic test used K=0.4 and failed at 2,000 iterations because a
poor first sample suppressed further exploration. The fixture now uses the
production exploration constant sqrt(2). No universal finite-budget convergence
claim follows from this test.

The first development run also recorded a 594.752 ms sample with an unbounded
default JVM heap. Later tests and experiments use `-Xms128m -Xmx512m` to limit
heap growth and potential collection pauses. The successful rerun does not prove
that heap size was the sole cause. Deadline checks cannot interrupt a framework
call, heuristic call, OS scheduling pause or garbage collection. The full timing
study records every observed overrun rather than dropping slow decisions.

## Background and limits

Self-interested max-n and paranoid assumptions can perform differently across
games. Sturtevant's [A Comparison of Algorithms for Multi-player Games](https://cs.du.edu/~sturtevant/papers/comparison_algorithms.pdf)
(CG 2002; proceedings published 2003) gives examples of this dependence and
discusses tie-breaking limitations. That work concerns other games and does not
establish which assumption is stronger in Sushi Go.

The current implementation keeps seeded random tie-breaking and a closed-loop
tree based on the sampled observation. MAXN does not make the search
information-set aware: simulated opponents can still act on the sampled state,
and several decisions occur before a simultaneous reveal. The heuristic remains
approximate, and modelling all opponents with the same evaluation may be wrong.
These limitations apply to interpreting the Stage 5 evidence.
