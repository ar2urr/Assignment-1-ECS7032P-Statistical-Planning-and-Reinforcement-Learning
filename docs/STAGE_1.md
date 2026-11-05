# Stage 1 validation record

Status: complete. Validation date: 3 November 2026. Historical checkpoint:
`a008e35` (`Add Stage 1 baseline implementation for Sushi Go agent`).

This record describes the Stage 1 commit. Later stages changed the search and
added the heuristic and opponent model. The original baseline is still available
in Git history.

## Scope

The baseline copies three official BasicMCTS Java files into `sushigoagent`,
changing only their package declarations. Two JSON configurations, the MIT
licence, documentation, a validation script and JUnit checks accompany the source.

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

Historical output: [stage1-validation.txt](../results/stage1-validation.txt).

Seven games against Random check that the baseline runs correctly. They are too
few to establish playing strength. The 450 ms configuration was loaded during
validation, but its timing was not benchmarked at this stage.

## State-comparison issue

In the pinned TAG revision, `SGGameState._equals` calls `super.equals`, which calls
`_equals` again and causes a `StackOverflowError` during whole-state comparison.

The test saves public fields before search and compares them afterwards: turn,
round, current player, status, card choices, hand and played-card identities,
scores, results and card counters. It does not inspect private decks or RNG state,
and needs no framework patch.

## Baseline limitations

- Expansion creates a new RNG with the same seed on each call.
- Random rollouts are not controlled by the main player's seed.
- Direct heuristic assignment bypasses the registered parameter copy path.
- The root recomputes legal actions instead of using the caller's list.
- Searches with no expanded root child have no action fallback.
- Some budget modes are unsupported, and UCB selection incorrectly increments
  the forward-model call count.
- Deadline checks occur between iterations and require timing validation.

Stage 2 addresses these search and budget problems. Stage 1 has neither a custom
heuristic nor the later multiplayer changes.