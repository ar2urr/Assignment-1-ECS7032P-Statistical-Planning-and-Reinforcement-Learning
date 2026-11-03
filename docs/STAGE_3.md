# Stage 3: Sushi Go state evaluation

Status: implemented and validated on 3 November 2026. Playing-strength evaluation
is reserved for Stage 5. All feature weights are initially 1.0 and have not been tuned.

## Research question and hypothesis

Does adding estimates of incomplete sets and delayed scoring to the same corrected
BasicMCTS improve decisions under short search budgets?

The hypothesis is that score-only cutoffs undervalue cards whose payoff arrives
after the rollout, particularly incomplete Tempura/Sashimi sets, unused Wasabi,
Maki rankings and Pudding rankings. The opposing hypothesis is that approximation
error and additional evaluation cost outweigh that information. Equal-iteration
comparisons isolate the evaluation change more closely; equal-time comparisons
also include its effect on search throughput. Neither outcome is established by
the current smoke tests.

## Implementation

`sushigoagent/SushiGoHeuristic.java` implements TAG's `IStateHeuristic` and extends
`TunableParameters`. The existing parameter loader selects it through the nested
`heuristic` object in `heuristic_iterations.json` and `heuristic_500ms.json`.
Their search parameters otherwise match the corresponding baseline JSON files.
No tree-policy or opponent-model changes are introduced in this stage.

For a nonterminal state and player p:

```text
V(p) = [awardedScore(p)
        + setWeight * setPotential(p)
        + wasabiWeight * wasabiPotential(p)
        + makiWeight * roundProgress * projectedMaki(p)
        + puddingWeight * gameProgress * projectedPudding(p)] / 50
```

At termination, `V(p)` is exactly TAG's recorded result: win 1, draw 0, loss -1.
No heuristic bonus is added. The score/50 scaling matches the baseline rather
than introducing a second changed factor. Nonterminal values are not restricted
to [-1, 1]; the class declares unbounded interface limits.

Let h be the player's current hand size and H the configured initial hand size
`max(1, nCards - nPlayers + 2)`. With `clamp` restricting values to [0, 1]:

```text
roundProgress = clamp(1 - h/H)
gameProgress = clamp((roundCounter + roundProgress) / max(1, nRounds))
```

Hand size is a rough remaining-pick horizon. It does not model extra Chopsticks
picks or pending choices awaiting simultaneous reveal.

## Feature definitions

**Incomplete sets.** For set size k and r = playedCount modulo k, only r > 0
receives potential. The required number of future cards is k-r. Known card
frequencies minus publicly played physical cards define an approximate remaining
pool. With q the relevant card's fraction of that pool:

```text
setPotential = setAward * (r/k) * P(Binomial(h, q) >= k-r)
```

The value is zero when there are too few remaining picks or known remaining
copies. Tempura uses k=2 and Sashimi k=3. The r/k factor credits only existing
progress. Already completed sets are present in `gameScore` and receive no second
award. Binomial tail probabilities use a small dynamic program.

**Unused Wasabi.** TAG's Wasabi counter tracks unconsumed cards. For X drawn from
the analogous Nigiri binomial model and u unused slots, the potential is
`(multiplierWasabi - 1) * averageNigiriPoints * E[min(X, u)]`. Slots are capped by
remaining picks and available Nigiri copies. The average is weighted by the three
Nigiri types' approximate remaining counts. Only the multiplier's extra points
are credited. Consumed Wasabi has no remaining potential.

**Maki.** Current icon counts determine the hypothetical round-end award using
TAG's rules: first-place points split with integer division, second-place points
only when first place is unique, and no award for zero icons. `roundProgress`
discounts that current-standing projection early in a round. Maki counts reset
after the actual award, preventing the previous round's points being counted twice.

**Pudding.** Current counts determine the hypothetical game-end award, including
integer splits, a zero award when every count is equal and no last-place penalty
in two-player games. `gameProgress` discounts this projection until later in the
game. Counts persist between rounds; terminal evaluation uses only the actual result.

Nigiri, Dumplings and completed sets are already scored when revealed. Their
awarded points enter only through `gameScore`. All scoring constants come from
`SGParameters` rather than duplicating fixed values in the heuristic.

## Assumptions and limits

The binomial model treats future opportunities as independent draws. Actual Sushi
Go drafting involves selection, competition for cards, correlated passing hands,
Chopsticks and limited availability. The pool ignores previous-round discards and
does not distinguish undealt cards from circulating hands. Probabilities are
heuristic estimates, not measured likelihoods or exact expected values. The
progress multipliers are design assumptions rather than calibrated confidence.

The heuristic reads public played cards/counters, configured deck composition and
hand size. It does not read any hand's contents, including the player's own known
cards. Maki pool subtraction counts physical cards rather than icons. Ignoring
known hand contents simplifies the feature model but loses useful information.

The search retains BasicMCTS's closed-loop use of the sampled observation. It does
not integrate over multiple hidden-state samples at the root. Opponents still
minimise the root player's value, pending Stage 4. The inherited terminal/nonterminal
scale discontinuity is another limitation. No optimality or novelty claim follows
from these modifications alone.

## Rationale and sources

UCT's selection rule balances exploration and observed return; the retained
BasicMCTS search follows that family of methods. See Kocsis and Szepesvari (2006),
[Bandit based Monte-Carlo Planning](https://sites.ualberta.ca/~szepesva/papers/ecml06.pdf).
Its theoretical setting does not establish guarantees for this multiplayer,
partially observable, truncated implementation.

Heuristic evaluation of intermediate states is an established MCTS enhancement,
discussed in Browne et al. (2012), [A Survey of Monte Carlo Tree Search Methods](https://cs.gettysburg.edu/~tneller/cs371/mcts-survey.pdf),
especially section 8.4.3. The particular feature formulas above are project design
choices; the paper is not evidence that they improve Sushi Go performance.

Scoring is checked against the pinned TAG implementation:
[SGCard](https://github.com/GAIGResearch/TabletopGames/blob/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/games/sushigo/cards/SGCard.java)
and [SGForwardModel](https://github.com/GAIGResearch/TabletopGames/blob/033232baa88bac4b6a1fe14fae62939193cdf9f7/src/main/java/games/sushigo/SGForwardModel.java).

## Validation

The [Stage 3 log](../results/stage3-validation.txt) records `OK (33 tests)` on the
same Java 21/macOS environment as Stage 2, with TAG source unchanged.

- Fourteen Stage 3 unit tests cover incomplete/completed sets, impossible completions,
  Wasabi consumption, scoring boundaries, terminal results, feature isolation,
  copied configuration weights and invalid weights.
- Maki and Pudding helpers are compared against actual TAG scoring for all counts
  0–3 across 2, 3 and 4 players: 336 standings per scoring rule, including ties.
- A real forward-model round transition checks awarded Maki and persistent Pudding.
- Changing hand contents while preserving size leaves the heuristic unchanged.
- Seven heuristic games complete with every seat represented for 3/4 players.
  The seven baseline games and all Stage 2 regressions also pass.
- Six heuristic timing samples: 268.337–491.902 ms. Six repeated baseline samples:
  298.080–425.751 ms. These are observed opening-position timings, not deadline guarantees.

The games test legality, finite evaluation and preservation of observed fields.
Their scores and outcomes are not a controlled strength comparison. Different
hidden-state samples can produce different results despite identical recorded seeds.

## Ablations and Git checkpoint

Baseline versus heuristic JSON files switch the whole evaluation. Setting one
weight to 0.0 removes that feature; all-zero weights recover baseline values.
The Stage 5 protocol will compare these conditions with balanced seats and adequate
sample sizes. No search-depth tuning or long tournament is included here.

After the Stage 2 checkpoint, the Stage 3 paths and commands are:

```bash
git add sushigoagent/SushiGoHeuristic.java \
  sushigoagent/heuristic_iterations.json sushigoagent/heuristic_500ms.json \
  tests/sushigoagent/SushiGoHeuristicTest.java tests/sushigoagent/Stage3IntegrationTest.java \
  docs/STAGE_3.md results/stage3-validation.txt \
  README.md sushigoagent/README.md docs/FILE_GUIDE.md docs/STAGES.md docs/REQUIREMENTS.md
git diff --cached --stat
git commit -m "stage 3: add configurable Sushi Go state evaluation"
```
