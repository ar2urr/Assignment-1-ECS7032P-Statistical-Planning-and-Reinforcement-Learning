# Stage 5: controlled experiments

Stages 4 and 5 are prepared for one combined Git checkpoint. The study protocol was fixed before
the principal run. The final evidence and machine-readable summaries are in
[the results directory](../results/stage5/analysis/RESULTS.md). The completed
run contains 16,128 fixed-iteration games, 28 timing games and 633 timed decisions.
[Stage 5 findings](STAGE_5_FINDINGS.md) interprets the effects, including the
Wasabi removal result and the baseline timing overrun.

## Principal design

The four conditions form a 2 × 2 design:

| Variant | Leaf evaluation | Opponent model |
|---|---|---|
| baseline | TAG default | PARANOID |
| heuristic | Sushi Go features | PARANOID |
| multiplayer | TAG default | MAXN |
| combined | Sushi Go features | MAXN |

Every main variant plays against homogeneous Random, OSLA and corrected-baseline
opponent lineups for both three and four players. A game contains one focal agent
and n−1 opponents of the stated type. There are 504 complete games per cell:
168 in each three-player seat, or 126 in each four-player seat. The 24 main cells
therefore contain 12,096 games.

Four additional conditions remove one feature at a time from the combined agent:
sets, Wasabi, Maki or Pudding. These play against corrected-baseline opponents
at both player counts, again with 504 games per cell. The eight feature cells
contain 4,032 games, making 16,128 fixed-iteration study games in total.

This operationalises comparisons as multiplayer lineups rather than head-to-head
duels. The assignment's interpretation of "per pair" still requires teaching-team
confirmation before final submission.

## Controls and seed policy

Search variants and baseline opponents use 150 iterations, K=sqrt(2), maximum tree
depth 5, rollout length 10 and epsilon=1e-6. The four feature weights are fixed
at 1.0 except for the component removed in each feature ablation. No parameter
tuning or outcome-dependent stopping is part of the study.

Random uses TAG's `RandomPlayer`. OSLA uses the unmodified `OSLAPlayer` with its
default game evaluation and a recorded seeded RNG. Reference agents retain their
native policies; OSLA's one-step evaluations are not padded to consume 150 searches.
The corrected baseline is the same reliability-corrected implementation with
PARANOID and default evaluation, not the historical Stage 1 source with known bugs.

Each cell receives a distinct deterministic seed stream derived from master seed
703205. Every recorded game has a game seed, player seeds and focal seat. Separate
warmup seeds precede measurement. TAG also uses an unseeded generator for hidden
state sampling, so identical full-game replay is not guaranteed. The cells are
analysed as independent samples; common seeds are not claimed to produce paired
deals or common random numbers.

Partial observability and competition mode are enabled. Event history is disabled.
Agents receive normal TAG observations; the external harness reads final scores
only after play. Illegal focal actions and abnormal final results fail the run.
Fixed-iteration cells run in two JVM processes, each with a 128–512 MiB heap.
Their latencies are operational diagnostics rather than controlled timing results.

## Timing study

After all fixed-iteration workers finish, eight timing cells run sequentially:
four main variants at each player count, against Random. Each variant plays once
in every seat, giving 28 complete games. The time configurations set 450 ms and
`breakMS=25`, leaving a nominal 425 ms search cutoff for the 500 ms target.

Every searched focal decision records wall time, round, hand size, legal-action
count, completed iterations, forward-model calls and state copies. Quantiles,
maximum time, throughput and all overruns are retained. Forced singleton actions
and framework observation construction are outside the measured entry point.
Each cell has eight separate fixed-iteration warmup games.

This study measures latency through all rounds. Its small game count is not used
to claim playing strength at 500 ms. Full 500-game time-budget comparisons are
outside the selected scope. This distinction matters when interpreting the
rubric's expectation of experiments at the 500 ms level.

## Data and analysis

The primary outcome is win share: 1 for an outright winner, 1/k for each of k tied
winners after TAG's Pudding tiebreak, and 0 otherwise. Mean score, ordinal position
and score gap to the strongest opponent provide secondary context. Baseline
against baseline is a control for approximately 1/n win share; sampling variation
and seat effects remain possible.

The analyser validates complete cell counts, unique seeds and game indices,
balanced seats, source snapshot hashes, finite metrics, scores/ranks/tie credit,
search effort and consistency between per-game and per-decision timing records.
It refuses incomplete studies and requires an explicit pilot flag below 500 games.

Pointwise 95% percentile intervals use 5,000 bootstrap resamples within each seat,
preserving the seat proportions. Unpaired bootstrap differences estimate the
heuristic effect under each model, the model effect under each evaluation, the
combined effect and the factorial interaction. Feature effects compare the full
combined agent with each removal. These are exploratory intervals without
multiple-comparison adjustment; shared controls make some contrasts correlated.
No family-wide significance claim is made. If every sampled outcome is identical,
the percentile bootstrap interval is degenerate; that is not certainty about the
underlying population rate.

The figures are exported as PNG and vector PDF. Exact analysis dependency versions
are recorded in `experiments/analysis-environment.txt`; the analysis metadata also
records its Python version and script hash. Raw records, configuration/source
snapshots, machine/runtime details and hashes remain available for inspection.
Source snapshots use `.java.txt` to avoid duplicate classes in Java editors;
their contents are byte-identical to the measured source files.

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

`MVN` selects a Maven executable; `MAVEN_REPO` selects a dependency cache. The
experiment builder freezes TAG bytecode in a per-run JAR before starting games.
`TAG_JAR` may provide an already-built JAR from the pinned revision; its SHA-256 is
recorded. The principal run used the previously validated local TAG bundle.

`--resume` requires the same runtime/configuration hashes, framework JAR hash and
protocol. Completed cells are skipped only when both data and completion metadata
exist with the expected row count. Partial files are retained for diagnosis and
cause a resumed cell to stop rather than silently overwrite evidence. A new output
directory starts an independent run. Pilot commands use `--games 12 --skip-timing`
and a separate output directory.

## Validation and limits

The combined Java suite passed 43 tests; the analysis has three additional unit
tests. The successful pilot completed all 32 cells, 384 measured games, and passed
the full raw-data analyser. Pilot samples do not enter the principal results.

An earlier pilot was disrupted when a concurrent Maven build temporarily removed
framework class files. The immutable JAR resolves that runner dependency. Principal
data collection started only after the revised pilot completed successfully.

The pinned public TAG revision may differ from the marking environment. Fixed
iteration counts do not equalise computation: MAXN evaluates multiple player
values and the heuristic has its own cost. The homogeneous opponents, fixed search
settings, approximate evaluation and remaining hidden-information limitations
restrict generalisation. Selecting a final configuration from these data needs a
held-out confirmation before a final strength claim. Stage 6 covers final course
compatibility, report preparation and submission packaging.
