# Stage 6 submission preparation

Group AG has one member. Development remains in `sushigoagent`; the source
archive exports the assigned `groupAG` namespace.

The submission uses the Sushi Go heuristic with PARANOID and four weights of 1.0.
Stage 5 finds a clear heuristic gain but no consistent extra benefit from MAXN.
The Wasabi-removal result was measured with MAXN, so it is not assumed to carry
over to PARANOID. This choice follows the evidence available; it does not
establish the best possible configuration.

## Entry point and time margin

`SubmissionPlayer` is the competition entry point. Its default constructor and
`competition.json` use the same settings. The optional seed must fit TAG's signed
32-bit range. A copy keeps the player class and settings, gets independent
parameters and restarts its RNG at the configured seed.

The 400 ms budget and 25 ms reserve give a nominal 375 ms cutoff, leaving 125 ms
before the stricter 500 ms limit in the brief. This also leaves time under the
alternative one-second limit. A running framework operation or JVM/OS pause can
still delay the return. The Stage 5 configurations keep their original 450 ms
budgets, and their measurements remain tied to those settings.

`BasicMCTSPlayer` is still available for experiments, with the original score
evaluation in its default constructor. Load `SubmissionPlayer` or
`competition.json` for the selected competition policy.

## Standalone export

The builder exports six Java files, the competition JSON, a README, the TAG
licence and the TAG revision. Identical inputs produce identical ZIP bytes. The
archive excludes framework source, experiments, tests, reports and older
configurations. Existing ZIPs are never overwritten, and development files keep
their names.

```bash
python3 scripts/package-submission.py --package groupAG --output build/groupAG.zip
python3 scripts/validate-submission.py build/groupAG.zip /path/to/TabletopGames \
  --output build/submission-validation
```

The export changes `sushigoagent` to `groupAG` in package declarations and the
JSON class name. Validate the exact ZIP intended for submission. `--draft` remains
available for development exports and is not used for groupAG.

The validator extracts a committed TAG revision into a new tree, adds the exported
package and builds it with Maven. The probe loads only the fresh TAG classes,
dependencies and its own test class; it cannot use development agent classes by
accident. The original checkout is read-only. `MVN` chooses Maven, `MAVEN_REPO`
chooses its cache, and `--revision` selects an explicitly supplied course
revision. Without that override, validation uses the revision stored in the ZIP.

## Validation evidence

The recorded suite passes 47 Java tests and seven Python tests. The Python checks
cover analysis and export, including archive contents, package renaming,
repeatable bytes, invalid identifiers, source preservation and refusal to
overwrite an existing ZIP.

The recorded groupAG ZIP passes 21 games against Random in a fresh TAG tree: one
game per seat with three and four players, for JSON loading, seeded construction
and copied players. Every action and terminal result is valid. Across 469 searched
decisions, the median is 349.293 ms, P95 is 372.923 ms and the maximum is 388.601
ms. None exceeds 500 ms. The first cold search is included, with no separate
warmup games.

Timing wraps the delegate's `getAction` call. It excludes forced single-action
turns and external observation/action-list generation. These sequential games
check functionality and observed latency, not worst-case delay or playing strength
on the marking machine. The JVM uses 128 MiB initially and 512 MiB at most. All
3,029 original framework files remain unchanged.
`results/stage6/standalone/validation.json` identifies the tested archive by
SHA-256 and records the exact revision.

The earlier export under the development namespace also passed 21 games and 469
searched decisions. Its maximum was 456.107 ms, with none above 500 ms. That
sample is saved in `results/stage6/development-standalone/` and is kept separate
from the groupAG run.

## Independent confirmation

The confirmation selects heuristic/PARANOID before collecting results. It then
compares that agent and baseline controls against baseline lineups at both player
counts: four cells of 504 games, or 2,016 measured games. Seats are balanced,
searches use 150 iterations and eight warmups per cell are excluded. Master seed
703206 differs from Stage 5's 703205. Checks reject any recorded game-seed overlap
and verify source hashes, cell sizes, seats, tie credit, outcomes and search
effort.

```bash
python3 scripts/run-confirmation.py /path/to/TabletopGames \
  --output results/new-confirmation
```

The run reuses Stage 5's saved framework JAR, with the same SHA-256 recorded in
the confirmation manifest. `TAG_JAR` can select that archive for a repeat run.
Otherwise the build script creates a JAR from the pinned checkout.

The analysis draws 5,000 bootstrap samples within seats, independently between
cells. Intervals are pointwise and have no multiple-comparison adjustment.
Recorded seeds still do not fully control TAG's hidden-state sampling. These games
test the policy at 150 iterations; they do not measure strength with the 400 ms
deployment budget. Data and completion records are in
`results/stage6/confirmation/`.

All 2,016 games completed, with no recorded game seeds shared with Stage 5. The
results support the selected heuristic at the tested iteration budget:

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

Both gain intervals stay above zero, repeating the heuristic improvement seen in
Stage 5. The comparison still leaves mixed lineups, equal-time strength and other
configurations untested.

## Report and outstanding submission details

The Word report and exported ZIP are stored outside the repository. The report
covers the Stage 5 results and submission choice, with space for one author name
and student number. Main content must fit four two-column pages; references and
AI-use notes may extend to page five. The official template has not been supplied,
so template compliance and pagination need checking after final edits.

Submission still needs the confirmed marking TAG revision and competition limit,
the official template, author details, a reviewed AI-use disclosure and the sole
member's own reflection of at most 300 words. That reflection must describe the
member's actual work. The brief conflicts between three players/one second and
four players/500 ms. Deployment uses the stricter target pending clarification.

Stage 6 is saved at `fa473bb`. The scripts build and check files; they do not
submit coursework or create Git commits.