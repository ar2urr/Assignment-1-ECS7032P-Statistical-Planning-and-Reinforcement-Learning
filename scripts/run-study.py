#!/usr/bin/env python3
"""Run comparison cells with balanced seats and save the results and build details."""
import argparse
import concurrent.futures
import csv
import hashlib
import json
import os
import platform
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VARIANTS = ['baseline', 'heuristic', 'multiplayer', 'combined']
FEATURES = ['combined_no_sets', 'combined_no_wasabi', 'combined_no_maki', 'combined_no_pudding']

def stamp():
    return datetime.now(timezone.utc).isoformat()

def fingerprint():
    paths = sorted([*(ROOT / 'sushigoagent').glob('*.java'), *(ROOT / 'sushigoagent').glob('*.json'),
                    *(ROOT / 'experiments').rglob('*.java'), *(ROOT / 'experiments/configs').glob('*.json')])
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('tag', type=Path)
    parser.add_argument('--games', type=int, default=504)
    parser.add_argument('--workers', type=int, default=2)
    parser.add_argument('--seed', type=int, default=703205)
    parser.add_argument('--output', type=Path, default=ROOT / 'results/stage5')
    parser.add_argument('--skip-timing', action='store_true')
    parser.add_argument('--resume', action='store_true')
    args = parser.parse_args()
    if args.games <= 0 or args.games % 12 or not 1 <= args.workers <= 4:
        parser.error('games must be a positive multiple of 12 and workers must be 1..4')
    tag, output = args.tag.resolve(), args.output.resolve()
    protocol = {'games_per_cell': args.games, 'workers': args.workers, 'master_seed': args.seed,
                'main_variants': VARIANTS, 'feature_ablations': FEATURES,
                'opponents': ['random', 'osla', 'baseline'], 'players': [3, 4],
                'timing_games_per_cell': 'one per seat', 'skip_timing': args.skip_timing,
                'warmup_games_per_cell': 8, 'java_options': ['-Xms128m', '-Xmx512m'],
                'seed_policy': 'independent streams across cells; upstream hidden sampling is not fully seed-controlled',
                'analysis': 'unpaired estimates; seats balanced within each cell; no outcome-based stopping'}
    hashes = fingerprint()
    manifest_path = output / 'manifest.json'
    if output.exists() and not args.resume:
        parser.error('output exists; use a new directory or --resume')
    if args.resume:
        old = json.loads(manifest_path.read_text())
        if old['protocol'] != protocol or old['source_sha256'] != hashes:
            parser.error('resume requires unchanged protocol and runtime/runner/configuration hashes')
    else:
        output.mkdir(parents=True)
        revision = subprocess.check_output(['git', '-C', str(tag), 'rev-parse', 'HEAD'], text=True).strip()
        if revision != (ROOT / 'TAG_COMMIT').read_text().strip():
            parser.error('wrong TAG revision')
        hardware = subprocess.run(['sysctl', '-n', 'machdep.cpu.brand_string', 'hw.memsize'],
                                  capture_output=True, text=True).stdout.strip()
        manifest = {'started_utc': stamp(), 'tag_revision': revision,
                    'project_base_commit': subprocess.check_output(['git', '-C', str(ROOT), 'rev-parse', 'HEAD'], text=True).strip(),
                    'protocol': protocol, 'source_sha256': hashes, 'snapshot_java_suffix': '.txt', 'platform': platform.platform(),
                    'hardware': hardware, 'python': platform.python_version(),
                    'java_version': subprocess.run(['java', '-version'], capture_output=True, text=True).stderr.strip()}
        manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
        for rel in hashes:
            destination = output / 'snapshot' / (rel + '.txt' if rel.endswith('.java') else rel)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / rel, destination)
    subprocess.run(['bash', str(ROOT / 'scripts/build-experiments.sh'), str(tag)], check=True)
    classpath = (ROOT / 'build/study-classpath.txt').read_text().strip()
    framework_jar = Path(classpath.split(os.pathsep)[0]) / 'framework.jar'
    framework_hash = hashlib.sha256(framework_jar.read_bytes()).hexdigest()
    manifest = json.loads(manifest_path.read_text())
    if args.resume and manifest.get('framework_jar_sha256') != framework_hash:
        parser.error('resume requires identical framework bytecode')
    manifest['framework_jar_sha256'] = framework_hash
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
    raw = output / 'raw'; raw.mkdir(exist_ok=True)

    def run_cell(variant, opponent, players, games, mode):
        name = f'{mode}_{players}p_{variant}_vs_{opponent}'
        csv_path, meta = raw / (name + '.csv'), raw / (name + '.json')
        if csv_path.exists() and meta.exists():
            with csv_path.open() as stream:
                count = sum(1 for _ in csv.DictReader(stream))
            if count != games:
                raise RuntimeError(f'Unexpected row count in {csv_path}')
            print(f'SKIP completed {name}', flush=True)
            return
        print(f'START {name} games={games}', flush=True)
        command = ['java', *protocol['java_options'], '-Djava.awt.headless=true', '-cp', classpath,
                   'coursework.experiments.ExperimentRunner', str(ROOT), str(raw), variant, opponent,
                   str(players), str(games), str(args.seed), mode]
        with (raw / (name + '.log')).open('w') as log:
            result = subprocess.run(command, cwd=tag, stdout=log, stderr=subprocess.STDOUT)
            if result.returncode:
                raise RuntimeError(f'Cell {name} failed; inspect {raw / (name + ".log")}')
        print(f'DONE {name}', flush=True)

    cells = [(v, o, n, args.games, 'iterations') for n in (3, 4)
             for o in ('random', 'osla', 'baseline') for v in VARIANTS]
    cells += [(v, 'baseline', n, args.games, 'iterations') for n in (3, 4) for v in FEATURES]
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(run_cell, *cell) for cell in cells]
        for future in concurrent.futures.as_completed(futures):
            future.result()
    if not args.skip_timing:
        # Wait for the iteration runs to finish, then run one timing cell at a time.
        for n in (3, 4):
            for variant in VARIANTS:
                run_cell(variant, 'random', n, n, '500ms')
    if fingerprint() != hashes:
        raise RuntimeError('Runtime sources/configurations changed during the run')
    manifest = json.loads(manifest_path.read_text())
    manifest['finished_utc'] = stamp()
    manifest['completed'] = True
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
    print(f'COMPLETE {output}', flush=True)

if __name__ == '__main__':
    main()
