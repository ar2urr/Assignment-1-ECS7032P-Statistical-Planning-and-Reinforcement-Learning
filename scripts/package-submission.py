#!/usr/bin/env python3
"""Create a deterministic package-only source ZIP without changing development code."""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ('BasicMCTSParams.java', 'BasicMCTSPlayer.java', 'BasicTreeNode.java',
           'SearchBudget.java', 'SushiGoHeuristic.java', 'SubmissionPlayer.java')


def package_files(root, package, draft=False):
    if (draft and package != 'sushigoagent') or (not draft and not re.fullmatch(r'group[A-Z]+', package)):
        raise ValueError('Use --draft for sushigoagent, or the assigned package name group followed by uppercase letters')
    agent = root / 'sushigoagent'
    files = {}
    for name in SOURCES:
        text = (agent / name).read_text()
        if not text.startswith('package sushigoagent;'):
            raise ValueError('Unexpected source package: ' + name)
        files[name] = re.sub(r'\bsushigoagent\b', package, text).encode()
    config = json.loads((agent / 'competition.json').read_text())
    if config != {'class': 'sushigoagent.SubmissionPlayer'}:
        raise ValueError('competition.json must select the validated SubmissionPlayer entry point')
    files['competition.json'] = (json.dumps({'class': package + '.SubmissionPlayer'}, indent=2) + '\n').encode()
    files['LICENSE-TAG.txt'] = (agent / 'LICENSE-TAG.txt').read_bytes()
    revision = (root / 'TAG_COMMIT').read_text().strip()
    files['TAG_COMMIT'] = (revision + '\n').encode()
    status = ('Development archive. The assigned group package identifier is pending; this is not a final submission ZIP.'
              if draft else 'Package-only source archive. Course-version and submission checks are recorded separately.')
    files['README.md'] = f'''# Sushi Go heuristic MCTS

{status}

## Installation and execution

Requires Java 21 and TAG. The development revision is `{revision}`.
Extract the `{package}/` directory under TAG's `src/main/java/` and run `mvn compile`.
No framework edits or files outside this package are required by the agent.

```java
core.AbstractPlayer agent = players.PlayerFactory.createPlayer(
    "src/main/java/{package}/competition.json");
```

Direct construction uses the same policy:

```java
core.AbstractPlayer agent = new {package}.SubmissionPlayer();
core.AbstractPlayer seeded = new {package}.SubmissionPlayer(7032L);
```

The optional seed must fit a signed 32-bit integer. TAG assigns the player ID and
forward model. `copy()` returns an independent SubmissionPlayer with copied
parameters and its RNG restarted at the configured seed. There is no persistent
training data, external model, network access or file loading during play.

## Fixed deployment configuration

The entry point uses the Sushi Go heuristic and PARANOID selection, K = sqrt(2),
rollout length 10, tree depth 5, epsilon = 1e-6, seed 7032 and all four feature
weights 1.0. The time budget is 400 ms with breakMS = 25: nominal cutoff 375 ms,
leaving 125 ms against a 500 ms decision limit. This is a conservative margin,
not a hard guarantee against a long framework call, JVM pause or OS scheduling.
The stricter limit is used while the course's conflicting competition settings
remain unresolved. It also operates under a one-second limit, without using the
extra time. The separate Stage 5 time configurations used 450 ms, not 400 ms.

Stage 5 supports heuristic evaluation; MAXN gave no consistent additional gain.
The Wasabi removal result was conditional on MAXN, so it is not transferred to
this PARANOID configuration without testing. Selection is not a claim of optimality.

BasicMCTSPlayer remains the configurable research entry point; its no-argument
constructor deliberately retains the original score evaluation. Competition
loading must use SubmissionPlayer or competition.json as shown above.

## Provenance and limitations

BasicMCTSPlayer, BasicMCTSParams and BasicTreeNode adapt TAG's permitted
players.basicMCTS implementation. LICENSE-TAG.txt retains the upstream MIT licence.
SearchBudget, SushiGoHeuristic and SubmissionPlayer are project additions.
The advanced players.mcts implementation is not the development basis.

The heuristic uses public tableaus, counters, configured card frequencies and
hand sizes. Incomplete sets and unused Wasabi use approximate draw probabilities;
Maki and Pudding use discounted current standings. Search samples a closed-loop
observation and is not information-set MCTS. Hidden-state sampling is not fully
seed-controlled. Prior experimental results do not establish tournament strength
or compatibility with an unconfirmed marking revision.
'''.encode()
    return files


def build(root, package, output, draft=False):
    files = package_files(root, package, draft)
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    # Exclusive creation preserves an existing reviewed archive.
    with output.open('xb') as stream:
        with zipfile.ZipFile(stream, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in sorted(files.items()):
                info = zipfile.ZipInfo(package + '/' + name, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                archive.writestr(info, data)
    return {'package': package, 'draft': draft, 'zip_sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
            'files': {package + '/' + name: hashlib.sha256(data).hexdigest() for name, data in sorted(files.items())}}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    mode = p.add_mutually_exclusive_group(required=True)
    mode.add_argument('--package', help='Assigned identifier, e.g. groupB; never inferred')
    mode.add_argument('--draft', action='store_true', help='Retain sushigoagent while group assignment is pending')
    p.add_argument('--output', required=True, type=Path)
    args = p.parse_args()
    try:
        result = build(ROOT, 'sushigoagent' if args.draft else args.package, args.output, args.draft)
    except (ValueError, FileExistsError) as e:
        p.error(str(e))
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()
