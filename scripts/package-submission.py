#!/usr/bin/env python3
"""Export a repeatable source ZIP while keeping the development package unchanged."""
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

The agent needs Java 21 and TAG revision `{revision}`.
Extract `{package}/` into TAG's `src/main/java/` and run `mvn compile`.
The package contains everything the agent adds to TAG; framework source stays unchanged.

```java
core.AbstractPlayer agent = players.PlayerFactory.createPlayer(
    "src/main/java/{package}/competition.json");
```

Direct construction uses the same policy:

```java
core.AbstractPlayer agent = new {package}.SubmissionPlayer();
core.AbstractPlayer seeded = new {package}.SubmissionPlayer(7032L);
```

The optional seed must fit a signed 32-bit integer. TAG supplies the player ID
and forward model. A copy keeps the SubmissionPlayer type and has its own
parameters and RNG, starting at the configured seed. Play needs no saved training
data, external model, network connection or file reads.

## Fixed deployment configuration

The defaults are the Sushi Go heuristic, PARANOID selection, K = sqrt(2), rollout
length 10, depth 5, epsilon = 1e-6, seed 7032 and four weights of 1.0. A 400 ms
budget with breakMS = 25 gives a nominal 375 ms cutoff, leaving 125 ms before the
500 ms target. A framework call, JVM pause or scheduling delay can still make a
decision late. The brief gives conflicting limits, so the stricter target is
used. It also leaves time under the alternative one-second limit. The Stage 5
time configurations retain their original 450 ms budgets.

Stage 5 finds a gain from the heuristic but no consistent extra benefit from
MAXN. Wasabi removal was tested with MAXN, so that change has not been assumed
to help PARANOID. Other configurations may still perform better.

BasicMCTSPlayer remains available for experiments, with the original score
evaluation in its default constructor. Use SubmissionPlayer or competition.json
for the selected competition settings.

## Provenance and limitations

BasicMCTSPlayer, BasicMCTSParams and BasicTreeNode are based on TAG's permitted
players.basicMCTS implementation. LICENSE-TAG.txt keeps the upstream MIT licence.
SearchBudget, SushiGoHeuristic and SubmissionPlayer were added for this project.
The code does not use the advanced players.mcts implementation as its basis.

The heuristic reads public tableaus and counters, configured card frequencies
and hand sizes. Sets and unused Wasabi use approximate draw probabilities; Maki
and Pudding use discounted current standings. Search builds its tree from a
sampled observation rather than using information-set MCTS. Recorded seeds do
not fully control hidden-state sampling. The experiments do not establish
tournament strength or compatibility with an unknown marking revision.
'''.encode()
    return files


def build(root, package, output, draft=False):
    files = package_files(root, package, draft)
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    # Fail if the output already exists, rather than replacing an earlier archive.
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
