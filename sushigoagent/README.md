# Sushi Go BasicMCTS agent

## Origin

`BasicMCTSPlayer.java`, `BasicMCTSParams.java` and `BasicTreeNode.java` come from
the permitted [TAG BasicMCTS
implementation](https://github.com/GAIGResearch/TabletopGames/tree/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/players/basicMCTS).
Stage 1 changed the package name. Stage 2 fixed search and budget handling, and
Stage 4 added value vectors and opponent-model selection. `SearchBudget.java`,
`SushiGoHeuristic.java` and `SubmissionPlayer.java` were added for this project.
`LICENSE-TAG.txt` keeps the upstream MIT licence.

## Installation

Place the `sushigoagent` directory under TAG's `src/main/java/` to compile its six
Java classes. The agent needs no changes to `core`, `games` or `players`. For
development checks, the repository's validation script compiles against TAG's
classes without copying source files into the framework.

## Competition entry point

`competition.json` loads `SubmissionPlayer`. Its default constructor uses the
Sushi Go heuristic, PARANOID selection and four weights of 1.0.
`SubmissionPlayer(long seed)` accepts a signed 32-bit seed. Copies keep the same
player class and settings but have their own parameter objects. The 400 ms budget
and 25 ms reserve give a nominal 375 ms cutoff. Framework calls and JVM or OS
pauses can still delay the return.

```java
core.AbstractPlayer agent = players.PlayerFactory.createPlayer(
    "src/main/java/sushigoagent/competition.json");
```

The exported package includes a README with installation and constructor examples.
Export and validation commands are in the development repository's
`docs/STAGE_6.md`.

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

The research constructors are `BasicMCTSPlayer()`, `BasicMCTSPlayer(long seed)`
and `BasicMCTSPlayer(BasicMCTSParams params)`. The first two use TAG's default
evaluation, PARANOID, K = sqrt(2), rollout length 10, depth 5, a 450 ms budget and
`breakMS=25`. Seeds must fit TAG's signed 32-bit parameter. Constructors do not
load JSON files. TAG supplies the forward model and player ID before a game
starts.

Change settings through `setParameterValue`, including dotted names such as
`heuristic.setWeight`. Writing directly to public fields skips TAG's copy
registry. `setStateHeuristic` registers the heuristic too. A copied player gets
independent parameters and a new RNG starting at its configured seed; the current
RNG stream is not cloned. `resetSeedEachGame=true` restarts the stream when each
game begins.

## Configurations

The eight research configurations use seed 7032, K = sqrt(2), rollout length 10 and tree depth 5.

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

Timing starts on entry to `_getAction`. For the eight research configurations, the
reserve gives a nominal 425 ms cutoff, leaving 75 ms before the 500 ms target.
Checks run before simulation operations, but cannot interrupt a framework call or
garbage collection. Measurements are recorded in Stages 2/3 and the [Stage 5
study](../results/stage5/analysis/RESULTS.md).

The weights are `setWeight`, `wasabiWeight`, `makiWeight` and `puddingWeight`, all
initially 1.0. They must be finite and nonnegative; zero removes a feature's
contribution. With all four at zero, the values match the baseline: score/50
during play and TAG's result at termination. Feature calculations still run, so
this setting costs more than the baseline evaluation.

## Search and evaluation

The search keeps BasicMCTS's tree of sampled states, UCB selection, random
expansion and rollouts, and choice of the most-visited root action. PARANOID
evaluates the root player and makes opponents minimise that value. MAXN evaluates
every player at a leaf, backs up the vector and selects using the current mover's
component. It checks the actual mover, including repeated Chopsticks turns. Node
values are sample means rather than deterministic max-n values.

The root uses the supplied legal-action list, including any restrictions. A forced
move or exhausted budget returns the first legal action. `opponentModel` is copied
with the parameters and accepts `PARANOID` or `MAXN`, independently of the
heuristic. [Stage 4](../docs/STAGE_4.md) explains the choice and its tests.

Budgets can count time, iterations, forward-model calls, state copies, or calls
and copies together. `getLastSearchStats()` returns completed iterations, actual
simulation calls and copies, and elapsed nanoseconds. Action generation and
heuristic evaluation count towards elapsed time but not operation budgets.

The heuristic adds estimates for incomplete sets, unused Wasabi, Maki and Pudding
to points already awarded. It reads public tableaus and counters, configured card
frequencies and hand sizes. It ignores hand contents. The search still simulates
TAG's sampled hidden cards, so this is not information-set MCTS. [Stage
3](../docs/STAGE_3.md) gives the formulas and assumptions.

## Limits

The [Stage 5 results](../results/stage5/analysis/RESULTS.md) measure playing
strength under the recorded settings. Draw estimates ignore drafting choices and
correlations; Maki and Pudding use current standings. The baseline scale allows
score/50 to exceed 1 during play, then switches to -1, 0 or 1 at termination,
which affects incentives near game end. MAXN also assumes opponents use the same
evaluation and reason from the sampled state. Stage 6 records the groupAG export
and the remaining course-version checks.