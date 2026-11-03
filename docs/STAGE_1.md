# Stage 1 validation record

Status: complete. Validation date: 3 November 2026.

## Scope

The baseline consists of three official BasicMCTS Java files under
`sushigoagent`, with only their package declarations changed. Supporting files
include two JSON configurations, the upstream MIT licence, documentation, a
validation script and focused JUnit tests.

## Provenance

- Framework: https://github.com/GAIGResearch/TabletopGames
- Revision: `033232baa88bac4b6a1fe14fae62939193cdf9f7`
- Source directory: `src/main/java/players/basicMCTS/`
- Provenance check: exact comparison after package-declaration replacement.
- Framework source status after validation: unmodified.

Compatibility with other framework revisions remains unverified.

## Validation

Environment: macOS, OpenJDK 21, IntelliJ-bundled Maven 3.9.9 and JUnit 4.13.2.

| Check | Result |
|---|---|
| Build unmodified TAG | Passed |
| Compile standalone agent package against TAG | Passed |
| Source differs only by package declaration | Passed for all three Java files |
| JSON loading and copying of registered parameters | Passed |
| Three-player games, baseline in each seat | All three completed |
| Four-player games, baseline in each seat | All four completed |
| Action legality and preservation of observed fields | Passed for 156 searched decisions |
| Focused JUnit checks | `OK (3 tests)` |
| Shell and JSON syntax | Passed |

Latest output: [stage1-validation.txt](../results/stage1-validation.txt).

The seven games against Random players validate functionality. They do not
establish general playing strength. The 450 ms configuration was loaded but not
benchmarked.

## State-comparison issue

In the pinned TAG revision, `SGGameState._equals` calls `super.equals`, which calls
`_equals` again and causes a `StackOverflowError` during whole-state comparison.

The test instead snapshots public fields before and after search: turn, round,
current player, status, card choices, hand/played card identities, scores, results
and card counters. Private decks and random generators are outside this check.
No framework change is required.

## Baseline limitations

- Expansion creates a new RNG with the same seed on each call.
- Random rollouts are not controlled by the main player's seed.
- Direct heuristic assignment bypasses the registered parameter copy path.
- The root recomputes legal actions instead of using the caller's list.
- Searches with no expanded root child have no action fallback.
- Some budget modes are unsupported, and UCB selection incorrectly increments
  the forward-model call count.
- Deadline checks occur between iterations and require timing validation.

These limitations define the planned Stage 2 reliability work. The baseline
contains no custom heuristic or multiplayer modification.
