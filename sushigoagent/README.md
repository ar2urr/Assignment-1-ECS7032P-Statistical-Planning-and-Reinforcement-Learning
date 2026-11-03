# Sushi Go BasicMCTS agent

## Origin

`BasicMCTSPlayer.java`, `BasicMCTSParams.java` and `BasicTreeNode.java` are adapted
from the permitted [official TAG BasicMCTS implementation](https://github.com/GAIGResearch/TabletopGames/tree/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/players/basicMCTS).
Stage 1 changed only the package declaration; Stage 2 adds reliability fixes.
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
    "src/main/java/sushigoagent/heuristic_iterations.json");
```

Explicit construction with equivalent settings:

```java
sushigoagent.BasicMCTSParams params = new sushigoagent.BasicMCTSParams();
params.setParameterValue("budgetType", players.PlayerConstants.BUDGET_ITERATIONS);
params.setParameterValue("budget", 150);
params.setParameterValue("randomSeed", 7032);
params.setParameterValue("heuristic", new sushigoagent.SushiGoHeuristic());
sushigoagent.BasicMCTSPlayer agent = params.instantiate();
```

Available constructors are `BasicMCTSPlayer()`, `BasicMCTSPlayer(long seed)` and
`BasicMCTSPlayer(BasicMCTSParams params)`. The first two use TAG's default state
heuristic, K = sqrt(2), rollout length 10, tree depth 5, time budget 450 ms and
`breakMS=25`. The seed constructor accepts only the signed 32-bit range supported
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

| File | Evaluation | Budget |
|---|---|---|
| `baseline_iterations.json` | TAG default score/result | 150 iterations |
| `heuristic_iterations.json` | Sushi Go features | 150 iterations |
| `baseline_500ms.json` | TAG default score/result | 450 ms, 25 ms reserve |
| `heuristic_500ms.json` | Sushi Go features | 450 ms, 25 ms reserve |

The time deadline starts on entry to `_getAction`. The reserve gives a nominal
425 ms search cutoff and 75 ms headroom against a 500 ms external limit. Checks
occur before simulation operations; a running framework call or GC pause cannot
be interrupted. Timing measurements are in the Stage 2 and 3 records.

The four heuristic weights are `setWeight`, `wasabiWeight`, `makiWeight` and
`puddingWeight`, each initially 1.0. Setting a weight to 0.0 removes that component.
All weights must be finite and nonnegative. With all four at zero, the evaluation
matches TAG's score/50 for nonterminal states and its result value at termination.
This variant still executes feature calculations, so it is not a runtime-cost
equivalent of the baseline configuration.

## Search and evaluation

The adapted BasicMCTS retains closed-loop UCB selection, random expansion and
rollouts, scalar backup and most-visited root selection. Opponent nodes minimise
the root player's value. The supplied root action list is respected, including
restrictions. Forced actions and exhausted budgets return its first legal action.

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

No playing-strength improvement has been established. The approximate probabilities
ignore drafting choices and correlations; rank estimates use current standings.
The nonterminal score/50 scale may exceed 1, while terminal values are -1, 0 and 1,
matching the baseline convention. This affects search incentives near game end.
Multiplayer modelling, systematic experiments and final packaging are later stages.
