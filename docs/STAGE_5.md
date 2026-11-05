# Stage 5: controlled experiments

Stages 4 and 5 are saved together at `26b8981`. The protocol was fixed before the
main run, which completed 16,128 games with iteration budgets and 28 timing games
with 633 measured decisions. [Results and
figures](../results/stage5/analysis/RESULTS.md) contain the analysis outputs.
[Stage 5 findings](STAGE_5_FINDINGS.md) discusses the effects, the Wasabi removal
and the baseline timing overrun.

## Principal design

The four conditions form a 2 × 2 design:

| Variant | Leaf evaluation | Opponent model |
|---|---|---|
| baseline | TAG default | PARANOID |
| heuristic | Sushi Go features | PARANOID |
| multiplayer | TAG default | MAXN |
| combined | Sushi Go features | MAXN |

Each variant faces Random, OSLA and corrected-baseline lineups with three and four
players. A game has one focal agent and n-1 opponents of the same type. Each cell
has 504 games: 168 per seat with three players or 126 per seat with four. The 24
main cells contain 12,096 games.

Four further variants remove sets, Wasabi, Maki or Pudding from the combined
agent, one feature at a time. They face corrected-baseline opponents at both
player counts, with 504 games per cell. These eight cells add 4,032 games,
bringing the total to 16,128.

Each comparison is a multiplayer lineup. The teaching team still needs to confirm
whether this satisfies the brief's use of "per pair".

## Controls and seed policy

Search variants and baseline opponents use 150 iterations, K=sqrt(2), depth 5,
rollout length 10 and epsilon=1e-6. Feature weights stay at 1.0 except when a
removal sets one to zero. The study does not tune parameters or stop early based
on outcomes.

Random uses TAG's `RandomPlayer`; OSLA uses the unmodified `OSLAPlayer` with
default evaluation and a recorded seeded RNG. Each keeps its native policy, so
OSLA is not made to spend 150 iterations. The control uses the corrected baseline
with PARANOID and default evaluation. It includes the reliability fixes, unlike
the original Stage 1 source.

Master seed 703205 creates a separate deterministic stream for each cell. Records
include the game seed, player seeds and focal seat. Warmups use separate seeds.
TAG also samples hidden states with an unseeded generator, so recorded seeds do
not guarantee identical games. The analysis treats cells as independent samples
rather than paired deals.

Partial observability and competition mode are enabled; event history is disabled.
Players receive normal TAG observations. The harness reads final scores after play
and fails on an illegal focal action or abnormal result. Two JVM workers run the
iteration cells, each with a 128-512 MiB heap. Their timings are diagnostic
because the workers share machine resources.

## Timing study

Timing starts after the iteration workers finish. Eight cells run one at a time:
four variants against Random at each player count. Each variant plays once per
seat, for 28 games in total. A 450 ms budget with `breakMS=25` gives a nominal 425
ms search cutoff against the 500 ms target.

Each searched focal decision records elapsed time, round, hand size, legal-action
count, iterations, forward-model calls and state copies. The analysis retains
quantiles, maximum time, throughput and every overrun. Forced single-action turns
and external observation construction are outside the measured entry point. Each
cell begins with eight separate iteration-budget warmup games.

The timing study covers all rounds but has too few games to establish playing
strength at 500 ms. Full 500-game time-budget comparisons were outside the chosen
scope. This limit matters when assessing the brief's requirement for experiments
at the 500 ms level.

## Data and analysis

The main outcome is win share: 1 for a sole winner, 1/k for each of k tied winners
after TAG's Pudding tiebreak, and 0 otherwise. Mean score, rank and the gap to the
strongest opponent give further context. Baseline against baseline should be near
1/n, allowing for sampling variation and seat effects.

The analyser checks cell sizes, unique seeds and game indices, balanced seats,
snapshot hashes, finite metrics, scores, ranks, tie credit and search effort. It
also checks that game-level timings agree with decision records. Incomplete
studies are rejected; samples below 500 games need an explicit pilot flag.

Pointwise 95% percentile intervals use 5,000 bootstrap resamples within each seat,
keeping the same seat proportions. Independent draws between cells estimate
heuristic effects, opponent-model effects, the combined effect and the
interaction. Feature contrasts compare the full combined agent with each removal.
Intervals are exploratory and unadjusted for multiple comparisons; contrasts
sharing a control are correlated. They do not establish significance for the whole
family of comparisons. Identical observed outcomes give a zero-width bootstrap
interval, which reflects the sample rather than certainty about the population.

Figures are saved as PNG and vector PDF. `experiments/analysis-environment.txt`
lists the exact dependency versions; analysis metadata records Python and the
script hash. Raw outcomes, source and configuration snapshots, runtime details and
hashes are retained. Java snapshots use `.java.txt` to avoid duplicate IDE
classes. They preserve the bytes used in the recorded runs, even when current
comments or documentation later change.

## Reproduction

From the repository root, with Java 21, Maven and the pinned TAG checkout:

```bash
bash scripts/check.sh /path/to/TabletopGames
python3 scripts/run-study.py /path/to/TabletopGames --games 504 --output results/new-study
python3 -m venv .venv
.venv/bin/python -m pip install -r experiments/requirements-analysis.txt
.venv/bin/python -m unittest discover -s tests -p 'test_analysis.py'
.venv/bin/python scripts/analyse-study.py results/new-study
```

The recorded run used an Apple M2 with 8 GiB RAM, macOS 26.1 and OpenJDK 21.
The study driver used Python 3.9.6; numerical analysis used Python 3.12.14.
The exact analysis packages can be restored with
`pip install -r experiments/analysis-environment.txt` in a compatible environment.

`MVN` chooses Maven and `MAVEN_REPO` chooses its cache. The builder creates a
separate framework JAR before starting games. `TAG_JAR` can supply an existing JAR
from the pinned revision; its SHA-256 is recorded. The main run used the
previously validated local bundle.

Resuming requires the same source and configuration hashes, framework JAR hash and
protocol. A cell is skipped only if both its data and completion metadata exist
with the expected number of rows. Partial files are kept for diagnosis, and
prevent silent reruns over old evidence. Use a new output directory for a new
study. Pilots use `--games 12 --skip-timing` and their own output directory.

## Validation and limits

The Stage 5 suite passed 43 Java tests and three Python analysis tests. Its
successful pilot completed 384 games across all 32 cells and passed the data
checks. Pilot games are excluded from the main results.

An earlier pilot failed when a concurrent Maven build temporarily removed
framework classes. Giving each run its own JAR removed that dependency on the
build directory. Main data collection began only after the revised pilot passed.

The pinned TAG version may differ from the marking version. Equal iteration counts
do not mean equal cost: MAXN evaluates more players, and the heuristic adds work
at each leaf. Fixed lineups and settings, approximate evaluation and hidden-state
assumptions limit the conclusions. Stage 6 tests the selected configuration on new
games and prepares the export; compatibility with the marking version still needs
confirmation.