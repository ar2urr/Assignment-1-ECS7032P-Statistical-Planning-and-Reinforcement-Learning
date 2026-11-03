# BasicMCTS baseline

## Origin

`BasicMCTSPlayer.java`, `BasicMCTSParams.java` and `BasicTreeNode.java` originate
from the permitted `players/basicMCTS` directory in official TAG:

https://github.com/GAIGResearch/TabletopGames/tree/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/players/basicMCTS

The only change is the package declaration: `package sushigoagent;`.
`LICENSE-TAG.txt` preserves the upstream MIT licence.

## Installation

TAG package path: `src/main/java/sushigoagent/`. Standard TAG compilation includes
these classes without modifications to `core`, `games` or `players`.

## Construction

Configuration-based construction:

```java
core.AbstractPlayer agent = players.PlayerFactory.createPlayer(
    "src/main/java/sushigoagent/baseline_iterations.json");
```

Explicit parameter construction:

```java
sushigoagent.BasicMCTSParams params = new sushigoagent.BasicMCTSParams();
params.setParameterValue("budgetType", players.PlayerConstants.BUDGET_ITERATIONS);
params.setParameterValue("budget", 150);
params.setParameterValue("rolloutLength", 10);
params.setParameterValue("maxTreeDepth", 5);
params.setParameterValue("randomSeed", 7032);
sushigoagent.BasicMCTSPlayer agent = params.instantiate();
```

The no-argument and `long`-seed constructors are also available. They do not load
the JSON files and use different default budgets. Controlled comparisons use
JSON or explicit registered parameters.

## Configurations

Both configurations use K = sqrt(2), rollout length 10 and maximum tree depth 5.

| Configuration | Budget |
|---|---|
| `baseline_iterations.json` | 150 iterations. |
| `baseline_500ms.json` | 450 ms with a 25 ms early-stop margin. |

The time configuration is provisional; deadline compliance has not been measured.

## Algorithm

Selection uses UCB, expansion chooses an unexpanded action, and rollouts use
random actions. TAG's existing game heuristic evaluates the rollout state. The
most visited root action is returned. Opponent nodes minimise the root player's
value in this baseline.

## Limitations

Known issues include expansion RNG reseeding, uncontrolled rollout seeding, a
heuristic setter that bypasses parameter copying, unsupported budget modes and
no fallback when the root has no expanded child. Zero tree depth and unsupported
budget modes are not valid baseline configurations.

This package is the Stage 1 development baseline. Custom evaluation and
multiplayer search are planned for later stages.
