# Stage 6 submission preparation

Group AG has one member. Development remains in `sushigoagent`; the source
archive exports the assigned `groupAG` namespace.

The selected deployment policy uses the Sushi Go heuristic with PARANOID
selection and all feature weights at 1.0. Stage 5 showed a clear heuristic gain,
with no consistent additional MAXN gain. The four-player Wasabi-removal result
was conditional on MAXN, so it is not transferred to PARANOID without evidence.
This is a conservative selection, not a claim of optimality.

## Entry point and time margin

`SubmissionPlayer` is the deployment entry point. Its no-argument constructor and
`competition.json` select the same policy. The optional seed constructor accepts
TAG's signed 32-bit range. Copies preserve the concrete entry-point class and
settings, with independent parameters and RNG restarted at the configured seed.

The time budget is 400 ms with a 25 ms reserve, yielding a nominal 375 ms cutoff
and 125 ms headroom against the stricter 500 ms assignment description. This
also fits within a one-second limit but does not use that extra time. Running
framework operations and JVM/OS pauses cannot be interrupted, so a hard deadline
is not guaranteed. Stage 5 used 450 ms configurations; those files and all
recorded Stage 5 results remain unchanged.

`BasicMCTSPlayer` remains the configurable research entry point. Its default
constructor retains the original score evaluation. Competition loading uses
`SubmissionPlayer` or `competition.json`, not that baseline constructor.

## Standalone export

The package builder creates a deterministic ZIP containing six Java sources,
one competition JSON, a standalone README, the TAG licence and TAG revision.
It excludes the framework, experiments, tests, reports and historical configs.
It refuses to overwrite an existing archive and does not rename development files.

```bash
python3 scripts/package-submission.py --package groupAG --output build/groupAG.zip
python3 scripts/validate-submission.py build/groupAG.zip /path/to/TabletopGames \
  --output build/submission-validation
```

The assigned group is AG, with one member. Development sources retain
`sushigoagent`; the export uses `groupAG`. The builder rewrites package declarations
and JSON class names consistently. Validation must target the exact exported ZIP.
`--draft` remains available for development archives and is not used for this
submission export.

The validator creates a fresh source tree from a committed local TAG revision,
installs only the archive, and compiles it with Maven. The integration probe has
no imports from the development agent package; its classpath contains the fresh
TAG classes, dependencies and probe only. The original TAG checkout is read-only
for this operation. `MVN` and `MAVEN_REPO` select the executable and dependency
cache. `--revision` permits an explicitly supplied course revision for a separate
compatibility run; default validation uses the archive's recorded revision.

## Validation evidence

The focused suite passes 47 Java tests. Seven Python tests cover analysis and
package export, including exact archive contents, renaming, repeatable bytes,
invalid identifiers, source preservation and overwrite refusal.

Fresh-tree validation of the exact `groupAG` ZIP passes 21 complete games
against Random: one game in every seat for three and four players, repeated for
JSON construction, seeded direct construction and copied players. All actions are
legal and all terminal results are valid. The 469 searched decisions have
median 349.293 ms, 95th percentile 372.923 ms and maximum
388.601 ms; 0 exceed 500 ms. The first cold search is included
and no separate warmup games are used.

The measurement wraps the delegate's `getAction` call and excludes forced
singleton moves and observation/action-list generation outside the player.
This small sequential functionality/timing check does not establish playing
strength, worst-case latency or compliance on the marking machine. It ran with
128 MiB initial and 512 MiB maximum Java heaps. All 3,029 original framework
files remain unchanged in the fresh tree. The archive SHA-256 and exact revision
are recorded in `results/stage6/standalone/validation.json`.

Before the group identifier was supplied, the development-namespace ZIP also
passed 21 games and 469 searched decisions, with maximum 456.107 ms and no
500 ms exceedances. That preliminary evidence is retained in
`results/stage6/development-standalone/`; the two samples are not pooled.

## Independent confirmation

`run-confirmation.py` preselects heuristic/PARANOID before collecting outcomes.
The fixed protocol compares it and baseline controls against baseline lineups,
with 504 games per cell at both three and four players: 2,016 new measured games.
Seats are balanced. Search uses 150 iterations; eight warmup games per cell are
excluded. Master seed 703206 differs from Stage 5's 703205. The validator rejects
any measured game-seed overlap with Stage 5 and checks source hashes, complete
cells, seat counts, tie credit, outcomes and effort.

```bash
python3 scripts/run-confirmation.py /path/to/TabletopGames \
  --output results/new-confirmation
```

The confirmation reuses the frozen framework JAR from Stage 5; its SHA-256 is
identical and recorded in the confirmation manifest. `TAG_JAR` can select that
archive when reproducing the build. Otherwise, the build script creates a new
framework JAR from the pinned checkout.

The analysis uses 5,000 within-seat bootstrap resamples and independent draws
between cells. Intervals are pointwise and unadjusted. TAG hidden-state sampling
is not fully seed-controlled. This is a separate replication at 150 iterations,
not a playing-strength measurement of the 400 ms deployment setting. Results
and completion status are in `results/stage6/confirmation/`.

The completed confirmation contains all 2,016 measured games and no recorded
game-seed overlap with Stage 5. Results support the selected heuristic policy
under the fixed-iteration protocol:

| Players | Variant | Games | Win share | Pointwise 95% CI |
|---|---|---:|---:|---|
| 3 | baseline | 504 | 31.45% | 27.58 to 35.42% |
| 3 | heuristic | 504 | 73.81% | 69.94 to 77.68% |
| 4 | baseline | 504 | 26.29% | 22.62 to 30.16% |
| 4 | heuristic | 504 | 59.33% | 55.16 to 63.59% |

| Players | Heuristic minus baseline | Pointwise 95% CI |
|---|---:|---|
| 3 | 42.36 pp | 36.90 to 47.92 pp |
| 4 | 33.04 pp | 27.38 to 38.79 pp |


Both difference intervals are above zero. This replicates the heuristic gain
without establishing optimality, performance against mixed agent lineups or
strength under equal wall-clock budgets.

## Report and outstanding submission details

The editable Word report and source ZIP are delivered outside the repository.
The report uses the measured Stage 5 evidence, explains the deployment choice,
and leaves one author name and student number for completion. Its main content
must remain within four two-column pages; references and AI-use notes may occupy
a fifth page. The official course template has not been supplied, so its exact
format compliance is not asserted. Pagination must be rechecked after editing.

The remaining submission dependencies are the marking TAG revision and
competition limit, the official report template,
the author details, final AI-use disclosure review, and the sole member's own
reflection of at most 300 words. No individual contribution
or personal reflection is fabricated. The course materials conflict between
three players/one second and four players/500 ms; the stricter target is used
for deployment preparation until the teaching team resolves that conflict.

Stage 6 preparation does not submit coursework or create Git commits.
