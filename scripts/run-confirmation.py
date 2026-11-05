#!/usr/bin/env python3
"""Check the selected heuristic policy on new games with a fixed iteration budget."""
import argparse
import concurrent.futures
import csv
import hashlib
import importlib.util
import json
import os
import platform
import subprocess
from datetime import datetime, timezone
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('study_analysis', ROOT/'scripts/analyse-study.py')
analysis = importlib.util.module_from_spec(spec); spec.loader.exec_module(analysis)


def now(): return datetime.now(timezone.utc).isoformat()
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('tag', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--games', type=int, default=504)
    p.add_argument('--seed', type=int, default=703206)
    args = p.parse_args()
    if args.games < 500 or args.games % 12: p.error('games must be at least 500 and divisible by 12')
    tag, out = args.tag.resolve(), args.output.resolve()
    revision = subprocess.check_output(['git','-C',str(tag),'rev-parse','HEAD'],text=True).strip()
    if revision != (ROOT/'TAG_COMMIT').read_text().strip(): p.error('Wrong TAG revision')
    if out.exists(): p.error('Output already exists; preserve prior evidence and use a new directory')
    primary = ROOT/'results/stage5'
    original_manifest = json.loads((primary/'manifest.json').read_text())
    if args.seed == original_manifest['protocol']['master_seed']: p.error('Use a new master seed')
    primary_seeds = set()
    for path in (primary/'raw').glob('*.csv'):
        if '.decisions.' not in path.name:
            primary_seeds.update(int(row['game_seed']) for row in analysis.records(path))
    out.mkdir(parents=True); raw=out/'raw'; raw.mkdir()
    measured = [*(ROOT/'sushigoagent').glob('*.java'), ROOT/'sushigoagent/baseline_iterations.json',
                ROOT/'sushigoagent/heuristic_iterations.json', ROOT/'experiments/coursework/experiments/ExperimentRunner.java',
                ROOT/'scripts/run-confirmation.py', ROOT/'scripts/analyse-study.py']
    hashes={str(path.relative_to(ROOT)):sha(path) for path in measured}
    for rel in hashes:
        target=out/'snapshot'/(rel+'.txt' if rel.endswith('.java') else rel)
        target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes((ROOT/rel).read_bytes())
    protocol={'variants':['baseline','heuristic'],'opponent':'baseline','players':[3,4],
              'games_per_cell':args.games,'master_seed':args.seed,'iterations':150,'workers':2,'warmup_games_per_cell':8,
              'selection':'Heuristic/PARANOID with all weights 1 selected from Stage 5 before these games; no re-selection or tuning',
              'purpose':'Independent replication of heuristic effect at 150 iterations; not a timed-strength study',
              'analysis':'Unpaired, seat-stratified bootstrap; 5000 samples; pointwise 95 percent intervals',
              'seed_limit':'New recorded game seeds, but upstream hidden-state sampling is not fully seed-controlled'}
    manifest={'started_utc':now(),'tag_revision':revision,'protocol':protocol,'source_sha256':hashes,
              'python':platform.python_version(),'numpy':np.__version__,'platform':platform.platform()}
    (out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    subprocess.run(['bash',str(ROOT/'scripts/build-experiments.sh'),str(tag)],check=True)
    cp=(ROOT/'build/study-classpath.txt').read_text().strip()
    manifest['framework_jar_sha256']=sha(Path(cp.split(os.pathsep)[0])/'framework.jar')
    (out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    def cell(n,variant):
        stem=f'iterations_{n}p_{variant}_vs_baseline'
        print('START '+stem,flush=True)
        with (raw/(stem+'.log')).open('w') as log:
            subprocess.run(['java','-Xms128m','-Xmx512m','-Djava.awt.headless=true','-cp',cp,
                            'coursework.experiments.ExperimentRunner',str(ROOT),str(raw),variant,'baseline',
                            str(n),str(args.games),str(args.seed),'iterations'],cwd=tag,stdout=log,stderr=subprocess.STDOUT,check=True)
        print('DONE '+stem,flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(cell,n,v) for n in (3,4) for v in ('baseline','heuristic')]
        for future in concurrent.futures.as_completed(futures):future.result()
    used=set(primary_seeds);rng=np.random.default_rng(args.seed);summaries=[];effects=[]
    for n in (3,4):
        boot={};mean={}
        for variant in ('baseline','heuristic'):
            stem=f'iterations_{n}p_{variant}_vs_baseline'
            rows=analysis.records(raw/(stem+'.csv'))
            meta=json.loads((raw/(stem+'.json')).read_text())
            if meta['games']!=args.games or meta['master_seed']!=args.seed:raise ValueError('Metadata mismatch')
            analysis.validate_cell(rows,n,args.games,variant,'baseline','iterations',used)
            values=[float(row['win_share']) for row in rows];seats=[int(row['seat']) for row in rows]
            boot[variant]=analysis.stratified_bootstrap(values,seats,rng);mean[variant]=float(np.mean(values))
            low,high=analysis.interval(boot[variant])
            summaries.append({'players':n,'variant':variant,'games':len(rows),'win_share':mean[variant],
                              'low95':low,'high95':high,'over_500ms':sum(int(row['over_500ms']) for row in rows)})
        low,high=analysis.interval(boot['heuristic']-boot['baseline'])
        effects.append({'players':n,'heuristic_minus_baseline':mean['heuristic']-mean['baseline'],'low95':low,'high95':high})
    for rel,h in hashes.items():
        if sha(ROOT/rel)!=h:raise ValueError('Source changed during confirmation: '+rel)
    report=out/'analysis';report.mkdir()
    analysis.write_csv(report/'summary.csv',summaries);analysis.write_csv(report/'effects.csv',effects)
    lines=['# Stage 6 independent confirmation','','Heuristic/PARANOID was selected from Stage 5 before this run began. At each '
                                                   'player count, it and the baseline control face baseline opponents with new '
                                                   'game seeds and 150 iterations per decision. These games are separate from '
                                                   'the main study. They test the chosen policy at that iteration budget, not '
                                                   'its strength under the deployment time limit.','',
           '| Players | Variant | Games | Win share | Pointwise 95% CI |','|---|---|---:|---:|---|']
    for row in summaries:lines.append(f"| {row['players']} | {row['variant']} | {row['games']} | {100*row['win_share']:.2f}% | {100*row['low95']:.2f} to {100*row['high95']:.2f}% |")
    lines+=['','| Players | Heuristic minus baseline | Pointwise 95% CI |','|---|---:|---|']
    for row in effects:lines.append(f"| {row['players']} | {100*row['heuristic_minus_baseline']:.2f} pp | {100*row['low95']:.2f} to {100*row['high95']:.2f} pp |")
    lines+=['','Intervals use 5,000 bootstrap resamples within seats and independent draws '
               'between cells. They are pointwise and unadjusted for multiple comparisons. '
               'This run tests a policy chosen in advance; it does not compare every '
               "alternative. Recorded seeds still do not fully control TAG's hidden-state "
               'sampling. Timings from these iteration runs stay separate from the '
               'sequential deployment check.','']
    (report/'RESULTS.md').write_text('\n'.join(lines))
    manifest.update({'completed':True,'finished_utc':now(),'measured_games':4*args.games,'no_game_seed_overlap_with_stage5':True})
    (out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('COMPLETE '+str(out),flush=True)

if __name__=='__main__':main()
