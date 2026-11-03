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

The existing BasicMCTS selection, expansion, rollout, scalar backup and
most-visited decision rule remain. Domain evaluation and opponent modelling are
separate experimental factors.

## Decisions

The first action in the supplied legal list is the fallback when no search can be
completed. A forced action requires no simulation. An empty list is an explicit
error because no legal result can be returned. Zero depth and zero budgets are
valid configurations.

Every simulation state copy and `forwardModel.next` invocation is counted exactly
where it occurs. UCB selection does not spend a forward-model call. Expansion
requires one available copy and advance; a rollout reserves its own copy and each
advance. An operation-budget search exits if an iteration spends no operations,
preventing a fully terminal/depth-limited tree from looping indefinitely.

Time uses `System.nanoTime`, starting at the player entry point. The 450 ms JSON
budget minus `breakMS=25` gives a 425 ms nominal search cutoff. New iterations also
require more than twice the longest completed iteration's duration. Checks inside
selection and rollout avoid knowingly starting further work after that cutoff.
Individual framework calls, heuristic calls and GC pauses are not preemptible.

Expansion and rollout draw from the player's seeded RNG. Simulation copies receive
seeds from that stream without reseeding the input observation. `LinkedHashMap`
preserves legal-action order. This supports repeatable iteration searches for an
identical observation and seed; upstream observation sampling and time-dependent
iteration counts still prevent guaranteed full-game replay.

Parameter edits use TAG's registration mechanism. A player copy receives a fresh
RNG with the configured seed, and an optional per-game reset is supported.
Stateful third-party heuristics would require their own copy policy; Stage 3 uses
a deeply copied tunable heuristic.

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

These are functional checks. They do not measure statistical playing strength or
establish deadline compliance on other machines. The Stage 3 run repeats these
regressions with the custom evaluation tests included.

## Git checkpoint

Stage 2 paths and commands from the repository root:

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

The Stage 1 record is historical; its original provenance check remains available
at `a008e35`. The current runner no longer requires the adapted source to match the
original byte for byte. The root README, package README, shared file guide, stage
map and requirements update belong to the subsequent Stage 3 checkpoint.
