# Sushi Go BasicMCTS agent

## Origin

`BasicMCTSPlayer.java`, `BasicMCTSParams.java` and `BasicTreeNode.java` are adapted
from the permitted [official TAG BasicMCTS implementation](https://github.com/GAIGResearch/TabletopGames/tree/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/players/basicMCTS).
Stage 1 changed only the package declaration; Stage 2 adds reliability fixes.
Stage 4 adds per-player value statistics and configurable opponent selection.
`SearchBudget.java` and `SushiGoHeuristic.java` are coursework additions.
`LICENSE-TAG.txt` preserves the upstream MIT licence.

## Installation

The entire `sushigoagent` directory belongs under `src/main/java/` in a TAG
checkout, producing `src/main/java/sushigoagent/`. Standard TAG compilation then
includes the five Java classes. No changes to `core`, `games` or `players` are
required. The standalone project's validation script avoids copying files into
the framework by compiling against TAG's existing classes.

## Construction

Configuration loading after installation inside TAG:

```java
core.AbstractPlayer agent = players.PlayerFactory.createPlayer(
    "src/main/java/sushigoagent/combined_iterations.json");
```

Explicit construction with equivalent settings:

```java
sushigoagent.BasicMCTSParams params = new sushigoagent.BasicMCTSParams();
params.setParameterValue("budgetType", players.PlayerConstants.BUDGET_ITERATIONS);
params.setParameterValue("budget", 150);
params.setParameterValue("randomSeed", 7032);
params.setParameterValue("heuristic", new sushigoagent.SushiGoHeuristic());
params.setParameterValue("opponentModel", sushigoagent.BasicMCTSParams.OpponentModel.MAXN);
sushigoagent.BasicMCTSPlayer agent = params.instantiate();
```

Available constructors are `BasicMCTSPlayer()`, `BasicMCTSPlayer(long seed)` and
`BasicMCTSPlayer(BasicMCTSParams params)`. The first two use TAG's default state
heuristic, K = sqrt(2), rollout length 10, tree depth 5, time budget 450 ms and
`breakMS=25`, and the PARANOID opponent model. The seed constructor accepts only
the signed 32-bit range supported
by TAG's registered `randomSeed` parameter. No constructor loads a JSON file
implicitly. TAG assigns the forward model and player ID before play.

Parameter changes use `setParameterValue`, including dotted feature names such as
`heuristic.setWeight`. Direct writes to public parameter fields bypass TAG's copy
registry. `setStateHeuristic` registers its argument. Copying creates independent
parameters and resets the new player's RNG to its configured seed; it does not
clone the current RNG stream. `resetSeedEachGame=true` also restarts that stream
on initialisation.

## Configurations

All configurations use seed 7032, K = sqrt(2), rollout length 10 and tree depth 5.

| File | Evaluation | Opponent model | Budget |
|---|---|---|---|
| `baseline_iterations.json` | TAG default | PARANOID | 150 iterations |
| `heuristic_iterations.json` | Sushi Go features | PARANOID | 150 iterations |
| `multiplayer_iterations.json` | TAG default | MAXN | 150 iterations |
| `combined_iterations.json` | Sushi Go features | MAXN | 150 iterations |
| `baseline_500ms.json` | TAG default | PARANOID | 450 ms, 25 ms reserve |
| `heuristic_500ms.json` | Sushi Go features | PARANOID | 450 ms, 25 ms reserve |
| `multiplayer_500ms.json` | TAG default | MAXN | 450 ms, 25 ms reserve |
| `combined_500ms.json` | Sushi Go features | MAXN | 450 ms, 25 ms reserve |

The time deadline starts on entry to `_getAction`. The reserve gives a nominal
425 ms search cutoff and 75 ms headroom against a 500 ms external limit. Checks
occur before simulation operations; a running framework call or GC pause cannot
be interrupted. Timing measurements are in the Stage 2/3 validation records and the
[Stage 5 study](../results/stage5/analysis/RESULTS.md).

The four heuristic weights are `setWeight`, `wasabiWeight`, `makiWeight` and
`puddingWeight`, each initially 1.0. Setting a weight to 0.0 removes that component.
All weights must be finite and nonnegative. With all four at zero, the evaluation
matches TAG's score/50 for nonterminal states and its result value at termination.
This variant still executes feature calculations, so it is not a runtime-cost
equivalent of the baseline configuration.

## Search and evaluation

The adapted BasicMCTS retains closed-loop UCB selection, random expansion and
rollouts, and most-visited root selection. PARANOID evaluates only the root
player's return; opponents minimise that return. MAXN evaluates every player at
the leaf, backs up a vector of values and selects each mover's own component.
The actual current player determines the component, including repeated Chopsticks
turns. Node values are sample means, not deterministic max-n backups.

The supplied root action list is respected, including restrictions. Forced actions
and exhausted budgets return its first legal action. Parameter copying retains
`opponentModel`; valid values are `PARANOID` and `MAXN`. The model is independent
of the selected heuristic. [Stage 4](../docs/STAGE_4.md) documents the rationale,
regression checks and limits.

Supported budgets are time, iterations, forward-model calls, state copies and
combined forward-model/copy calls. `getLastSearchStats()` reports completed
iterations, actual simulation calls/copies and elapsed nanoseconds. Action-list
generation and heuristic evaluation are excluded from operation counts but
included in elapsed time.

The custom heuristic adds discounted incomplete-set, unused-Wasabi, Maki and
Pudding prospects to already-awarded points. It reads public tableaus, counters,
configured card frequencies and hand size. Hidden hand contents are not used by
the heuristic. Search still simulates the hidden-card sample supplied in the
observation; it is not information-set MCTS. Detailed equations, assumptions and
research hypotheses are in [Stage 3](../docs/STAGE_3.md).

## Limits

Playing-strength evidence is reported separately in the
[Stage 5 results](../results/stage5/analysis/RESULTS.md). Approximate probabilities
ignore drafting choices and correlations; rank estimates use current standings.
The nonterminal score/50 scale may exceed 1, while terminal values are -1, 0 and 1,
matching the baseline convention. This affects search incentives near game end.
MAXN assumes opponents share the selected evaluation and can reason from the
sampled state; neither assumption need match their actual behaviour. Final course
compatibility, package renaming and submission preparation belong to Stage 6.
