# Stage 4: multiplayer value selection

Implemented and tested on 3 November 2026. Stages 4 and 5 share commit `26b8981`,
following the Stage 2/3 checkpoint `db219f5`.

## Research question

Does modelling each opponent as maximising its own return improve Sushi Go play
relative to assuming all opponents minimise the focal player's return?

Each player competes for its own points, which motivates the self-interested
model. It may still make worse decisions if the evaluation is inaccurate, hidden
cards matter, or the extra evaluations reduce useful search. Stage 5 tests it with
both TAG's default evaluation and the custom heuristic.

## Implementation

`BasicMCTSParams.OpponentModel` has two registered settings:

- `PARANOID` remains the default and preserves the previous opponent model.
- `MAXN` stores and backs up one evaluation value for each player.

Each node stores accumulated values for every player. MAXN evaluates the same leaf
once per player, then adds the whole vector to each ancestor. Backup preserves
player indices and does not negate values.

For a child with N visits and accumulated value S[p], selection uses:

```text
MAXN:     S[actual mover] / (N + epsilon) + exploration
PARANOID: sign * S[root player] / (N + epsilon) + exploration
```

`sign` is +1 on the root player's turn and -1 on an opponent's turn. Both modes
keep the UCB exploration term and choose the most-visited root action. The state
identifies whose turn it is; depth parity would fail with multiple players and
consecutive Chopsticks choices.

PARANOID needs only the root player's value. MAXN checks every evaluated component
is finite. If time expires partway through a vector, that vector is discarded so
no player receives a partial backup.

Node statistics remain MCTS sample means. The change uses the max-n opponent
assumption within the permitted BasicMCTS structure; it does not replace the tree
with deterministic max-n search or use the advanced `players.mcts` implementation.

## Configurations

| Evaluation | PARANOID | MAXN |
|---|---|---|
| TAG default | `baseline_iterations.json` | `multiplayer_iterations.json` |
| Sushi Go features | `heuristic_iterations.json` | `combined_iterations.json` |

The matching `_500ms.json` files use a 450 ms budget and 25 ms reserve. Every
configuration names its opponent model. Depth, rollout length, exploration
constant and seed policy otherwise match, and parameter copies keep the selected
model.

## Validation and development findings

The [Stage 4 log](../results/stage4-validation.txt) records 40 passing JUnit tests.
The subsequent [Stage 5 log](../results/stage5-validation.txt) adds three harness
tests and records 43 passing tests. Both include all prior scoring and reliability
regressions.

In the synthetic game, an opponent can help the root player while earning more
itself, or hurt the root while earning less. MAXN and PARANOID choose different
root actions as expected across all seats, both player counts and two seeds. Other
tests cover consecutive turns, parameter copying, nonfinite values, incomplete
vectors and combined operation budgets. Both variants also complete games from
every seat with three and four players.

The first synthetic test used K=0.4 and failed after 2,000 iterations: a poor
early sample discouraged further exploration. The test now uses the production
value sqrt(2). Passing it does not guarantee convergence at every finite budget.

An early development run recorded 594.752 ms with no explicit JVM heap cap. Later
runs use `-Xms128m -Xmx512m` to limit heap growth and possible collection pauses.
The successful rerun cannot identify heap size as the sole cause. Deadline checks
cannot interrupt framework calls, heuristic calls, scheduling pauses or garbage
collection. The timing study keeps every slow decision in its records.

## Background and limits

Sturtevant's [A Comparison of Algorithms for Multi-player
Games](https://cs.du.edu/~sturtevant/papers/comparison_algorithms.pdf) (CG 2002;
proceedings published 2003) shows that max-n and paranoid search can perform
differently across games and discusses tie breaking. It studies other games, so it
does not settle which model works better in Sushi Go.

The search still uses seeded random tie breaking and a tree built from a sampled
observation. MAXN does not resolve imperfect information: opponents reason from
that sample, and some choices happen before a simultaneous reveal. The heuristic
is approximate, and real opponents may use different evaluations. These
assumptions limit how far the Stage 5 findings can be generalised.