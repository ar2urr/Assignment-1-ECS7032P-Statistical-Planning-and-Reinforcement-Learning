# Sushi Go coursework

Stage 1 of a six-stage development plan: a BasicMCTS baseline for TAG. The Java
implementation is copied from official TAG with only its package declaration
changed.

## Documentation

- [File reference](docs/FILE_GUIDE.md)
- [Implementation stages](docs/STAGES.md)
- [Stage 1 validation record](docs/STAGE_1.md)
- [Coursework requirements](docs/REQUIREMENTS.md)
- [Assessment rubric mapping](docs/RUBRIC_REVIEW.md)
- [Agent construction and configuration](sushigoagent/README.md)

## Structure

| Path | Contents |
|---|---|
| `sushigoagent/` | Agent source, JSON configurations, README and upstream licence. |
| `tests/` | Focused JUnit integration checks. |
| `scripts/` | Compilation and validation helpers. |
| `docs/` | Requirements, development plan and validation records. |
| `results/` | Validation output; no playing-strength study has been completed. |
| `TAG_COMMIT` | Pinned official TAG revision. |

The development package is `sushigoagent`. The final submission requires the
assigned group package identifier, with matching imports, JSON class names and
paths.

## Build and validation

Dependencies: Java 21, Maven and a separate TAG checkout. Commands from the project
root:

```bash
git clone https://github.com/GAIGResearch/TabletopGames.git ../TabletopGames
git -C ../TabletopGames checkout "$(cat TAG_COMMIT)"
bash scripts/check-stage1.sh ../TabletopGames
```

`MVN` selects an alternative Maven executable, including IntelliJ's bundled
Maven. `MAVEN_REPO` selects an alternative dependency-cache directory. Initial
setup downloads TAG's dependency tree.

The script builds TAG, compiles the standalone agent, checks source provenance
and runs JUnit tests. TAG source files remain unmodified.

## Baseline checkpoint

Git commands from the project root:

```bash
git init
git add .gitignore README.md TAG_COMMIT sushigoagent scripts tests docs results
git commit -m "stage 1: establish official BasicMCTS baseline and validation"
```

## Limitations

The baseline has no custom Sushi Go heuristic or demonstrated improvement in
playing strength. Known search limitations are recorded in the Stage 1 validation
record. The time-based configuration has been loaded but not benchmarked.

Compatibility is verified against the revision in `TAG_COMMIT`; compatibility
with the course's marking environment remains unverified. Recorded seeds do not
guarantee exact replay because upstream rollouts and state copying use additional
random generators.
