# Stage 3: Sushi Go state evaluation

Implemented and tested on 3 November 2026. This record covers the heuristic before
the Stage 5 strength study. The initial feature weights are all 1.0; no weight
tuning was done.

## Research question and hypothesis

Does adding estimates of incomplete sets and delayed scoring to the same corrected
BasicMCTS improve decisions under short search budgets?

A rollout may stop before a set is completed or Maki and Pudding points are
awarded. The hypothesis is that estimating these rewards, along with unused
Wasabi, improves a short search. Approximation errors and the cost of evaluation
could instead make it worse. Equal-iteration comparisons focus on the value
estimates; equal-time comparisons also capture any loss of search throughput. The
smoke tests alone cannot decide between these outcomes.

## Implementation

`sushigoagent/SushiGoHeuristic.java` implements `IStateHeuristic` and extends
`TunableParameters`. TAG loads it through the nested `heuristic` object in
`heuristic_iterations.json` and `heuristic_500ms.json`. The other settings match
their baseline configurations. This stage leaves tree selection and the opponent
model unchanged.

For a nonterminal state and player p:

```text
V(p) = [awardedScore(p)
        + setWeight * setPotential(p)
        + wasabiWeight * wasabiPotential(p)
        + makiWeight * roundProgress * projectedMaki(p)
        + puddingWeight * gameProgress * projectedPudding(p)] / 50
```

At termination, `V(p)` returns TAG's result: 1 for a win, 0 for a draw and -1 for
a loss, with no heuristic bonus. During play, dividing by 50 keeps the baseline
scale so scaling is not another experimental change. Values can fall outside [-1,
1], and the interface declares unbounded limits.

Let h be the player's current hand size and H the configured initial hand size
`max(1, nCards - nPlayers + 2)`. With `clamp` restricting values to [0, 1]:

```text
roundProgress = clamp(1 - h/H)
gameProgress = clamp((roundCounter + roundProgress) / max(1, nRounds))
```

Hand size approximates the remaining picks. It leaves out extra Chopsticks picks
and choices waiting for a simultaneous reveal.

## Feature definitions

### Incomplete sets

For an incomplete set of size k, let r = playedCount modulo k. Only r > 0 receives
potential, and k-r more cards are needed. The remaining pool is estimated from
configured card frequencies minus publicly played cards. Let q be the fraction of
that pool belonging to the needed card type:

```text
setPotential = setAward * (r/k) * P(Binomial(h, q) >= k-r)
```

Potential is zero if too few picks or copies remain. Tempura uses k=2 and Sashimi
k=3. The r/k factor credits progress already made, while completed sets are
counted only through `gameScore`. A small dynamic program computes the binomial
tail probability.

### Unused Wasabi

TAG's Wasabi counter records unused cards. For X from the corresponding Nigiri
binomial model and u unused slots, potential is `(multiplierWasabi - 1) *
averageNigiriPoints * E[min(X, u)]`. Slots are capped by remaining picks and
Nigiri copies. The average weights the three Nigiri types by their estimated
remaining counts. Only the extra multiplier points are added; consumed Wasabi has
no potential left.

### Maki

Current Maki icons give the points that would be awarded if the round ended now.
TAG splits first-place points with integer division, awards second place only if
first place is unique, and awards nothing for zero icons. Multiplying by
`roundProgress` discounts early standings. Counts reset after the real award, so
the previous round's points are not counted twice.

### Pudding

Current Pudding counts give the points that would be awarded if the game ended
now. This includes integer splits, no points when all counts are equal, and no
last-place penalty in two-player games. `gameProgress` discounts early estimates.
Pudding persists between rounds; terminal evaluation uses only the actual result.

Nigiri, Dumplings and completed sets are already scored when revealed. Their
awarded points enter only through `gameScore`. All scoring constants come from
`SGParameters` rather than duplicating fixed values in the heuristic.

## Assumptions and limits

The binomial model assumes independent draws. Real drafting involves competing
choices, correlated hands passed between players, Chopsticks and limited card
availability. The pool also ignores earlier discards and mixes undealt cards with
circulating hands. Its probabilities are rough estimates rather than measured
likelihoods or exact expectations. Progress discounts are design choices, not
calibrated confidence values.

The heuristic reads public played cards and counters, configured deck composition
and hand size. It ignores all hand contents, even the player's known cards. Maki
subtraction counts physical cards rather than icons. This keeps the model simple
but discards information the player could use.

Search keeps BasicMCTS's tree based on one sampled observation rather than
averaging several hidden-state samples at the root. At this stage, every opponent
still minimises the root player's value; Stage 4 adds the alternative model. The
baseline's change of scale at termination remains a limitation. These changes
alone establish neither optimality nor novelty.

## Rationale and sources

UCT balances exploration with observed return, and BasicMCTS follows this
approach. Kocsis and Szepesvari (2006), [Bandit based Monte-Carlo
Planning](https://sites.ualberta.ca/~szepesva/papers/ecml06.pdf), describe the
method. Its theory does not give guarantees for this truncated search in a
multiplayer game with hidden information.

Evaluating intermediate states is an established MCTS extension; see section 8.4.3
of Browne et al. (2012), [A Survey of Monte Carlo Tree Search
Methods](https://cs.gettysburg.edu/~tneller/cs371/mcts-survey.pdf). The formulas
here are project choices. Whether they help in Sushi Go must be tested separately.

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

These games check legal moves, finite values and unchanged observed fields. They
are not a controlled strength comparison. Hidden-state sampling can still change
outcomes when the recorded seeds are the same.

## Ablations and Git checkpoint

The baseline and heuristic JSON files switch the full evaluation. Setting one
weight to 0.0 removes that feature; setting all weights to zero recovers baseline
values. Stage 5 tests these variants with balanced seats and larger samples. This
stage does not tune depth or run a tournament.

The original Stage 3 staging example follows. Stages 2 and 3 were ultimately saved
together at `db219f5`.

```bash
git add sushigoagent/SushiGoHeuristic.java \
  sushigoagent/heuristic_iterations.json sushigoagent/heuristic_500ms.json \
  tests/sushigoagent/SushiGoHeuristicTest.java tests/sushigoagent/Stage3IntegrationTest.java \
  docs/STAGE_3.md results/stage3-validation.txt \
  README.md sushigoagent/README.md docs/FILE_GUIDE.md docs/STAGES.md docs/REQUIREMENTS.md
git diff --cached --stat
git commit -m "stage 3: add configurable Sushi Go state evaluation"
```
