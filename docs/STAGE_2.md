# Stage 2: search reliability

Status: implemented and validated on 3 November 2026. Basis: Stage 1 commit
`a008e35`, with official TAG pinned by `TAG_COMMIT`.

## Changes

| File | Change and purpose |
|---|---|
| `sushigoagent/BasicMCTSPlayer.java` | Uses the caller's legal list, provides a fallback, registers heuristic changes, preserves configured seeds on copies and exposes search counters. |
| `sushigoagent/BasicMCTSParams.java` | Consistent constructor defaults, registered parameter copying and validation of budgets/depths/numeric constants. |
| `sushigoagent/BasicTreeNode.java` | Stable child order, shared seeded action sampling, copied actions, seeded simulation RNGs and resource checks inside search. |
| `sushigoagent/SearchBudget.java` | Central accounting for all five TAG budget modes and monotonic elapsed time. |
| `scripts/check.sh` | Compiles all package files and focused coursework tests against the pinned framework in a temporary class directory. |
| `scripts/check-stage1.sh` | Compatibility wrapper for the current runner. |
| `tests/sushigoagent/SearchReliabilityTest.java` | Fourteen reliability regressions. |

The changes keep BasicMCTS's selection, expansion, rollout, scalar backup and
choice of the most-visited action. The heuristic and opponent model are added and
tested in later stages.

## Decisions

If search cannot complete, the player returns the first action in the supplied
legal list. A forced move needs no simulation. An empty list raises an error
because there is no legal action to return. Zero depth and zero budgets remain
valid settings.

A state copy or `forwardModel.next` call is counted where it happens. UCB
selection uses neither. Expansion needs a copy and an advance; a rollout needs its
own copy plus each advance. An operation-budget search stops if an iteration
spends no operations, so a terminal tree or depth limit cannot cause an endless
loop.

Timing starts at the player entry point and uses `System.nanoTime`. A 450 ms
budget with `breakMS=25` gives a nominal 425 ms cutoff. Another iteration starts
only if more than twice the longest completed iteration remains. Selection and
rollout also check the deadline before further work. A running framework call,
heuristic call or garbage collection cannot be interrupted.

Expansion and rollout share the player's seeded RNG. Simulation copies take seeds
from that stream; the input observation is not reseeded. `LinkedHashMap` keeps
action order stable. Iteration searches can therefore repeat for the same
observation and seed. Full games can still differ because TAG samples observations
separately and time budgets permit different iteration counts.

Settings go through TAG's parameter registry. A copied player gets a new RNG
starting at its configured seed, with an optional reset before each game. A
stateful external heuristic would need its own copy handling. The Stage 3 tunable
heuristic is deeply copied.

## Validation

Environment: macOS, OpenJDK 21, IntelliJ-bundled Maven 3.9.9 and JUnit 4.13.2.
The framework checkout remained unmodified.

The [Stage 2 log](../results/stage2-validation.txt) records `OK (17 tests)`:

- Seven complete games: the corrected baseline in every seat for three/four players.
- Legal restricted-root actions, forced moves, zero limits and preserved input fields.
- All operation budgets checked against an independently counted forward model.
- Seed/copy consistency, repeated searches, reset behaviour and Chopsticks action isolation.
- A controlled monotonic-clock test at the deadline and invalid-input checks.
- Six wall-clock samples: 194.105–393.303 ms with the time configuration.

These checks test search behaviour rather than playing strength. They also cannot
establish deadline compliance on another machine. Stage 3 reruns them alongside
the heuristic tests.

## Git checkpoint

The original Stage 2 staging example is shown below. The actual Stage 2/3
checkpoint is `db219f5`.

```bash
git add sushigoagent/BasicMCTSPlayer.java sushigoagent/BasicMCTSParams.java \
  sushigoagent/BasicTreeNode.java sushigoagent/SearchBudget.java \
  scripts/check.sh scripts/check-stage1.sh \
  tests/coursework/validation/Stage1SmokeTest.java \
  tests/sushigoagent/SearchReliabilityTest.java \
  docs/STAGE_1.md docs/STAGE_2.md results/stage2-validation.txt
git diff --cached --stat
git commit -m "stage 2: fix search correctness and decision budgets"
```

The original source-comparison check is saved at `a008e35`. Later checks allow the
developed agent to differ from upstream. Shared readmes, file guides and
requirements were updated with Stage 3.