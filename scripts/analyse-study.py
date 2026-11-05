#!/usr/bin/env python3
"""Check the game records and produce confidence intervals, tables and figures."""
import argparse
import csv
import hashlib
import json
import platform
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.ticker import PercentFormatter

MAIN = ['baseline', 'heuristic', 'multiplayer', 'combined']
FEATURES = ['sets', 'wasabi', 'maki', 'pudding']
COLORS = ['#737B86', '#0072B2', '#E69F00', '#009E73']

def records(path):
    with path.open(newline='') as stream:
        return list(csv.DictReader(stream))

def write_csv(path, rows):
    if not rows:
        return
    with path.open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

def stratified_bootstrap(values, seats, rng, repetitions=5000):
    """Resample games within each seat without changing the proportion from each seat."""
    values, seats = np.asarray(values, dtype=float), np.asarray(seats, dtype=int)
    result = np.zeros(repetitions)
    for seat in sorted(set(seats)):
        group = values[seats == seat]
        sampled = rng.integers(0, len(group), size=(repetitions, len(group)))
        result += group[sampled].mean(axis=1) * len(group) / len(values)
    return result

def interval(samples):
    return tuple(float(x) for x in np.quantile(samples, [0.025, 0.975]))

def validate_cell(rows, n, games, variant, opponent, mode, seeds):
    if len(rows) != games:
        raise ValueError(f'Incomplete cell {mode}/{n}/{variant}/{opponent}: {len(rows)} != {games}')
    if [int(row['game_index']) for row in rows] != list(range(games)):
        raise ValueError('Duplicate, missing or out-of-order game indices')
    counts = [0] * n
    for row in rows:
        if (row['variant'], row['opponent'], row['mode'], int(row['players'])) != (variant, opponent, mode, n):
            raise ValueError('Cell label mismatch')
        index, seat, seed = int(row['game_index']), int(row['seat']), int(row['game_seed'])
        if seat != index % n or seed in seeds:
            raise ValueError('Seat schedule mismatch or reused game seed')
        seeds.add(seed)
        counts[seat] += 1
        scores, ranks = json.loads(row['all_scores']), json.loads(row['all_ranks'])
        if len(scores) != n or len(ranks) != n or len(json.loads(row['player_seeds'])) != n:
            raise ValueError('Incomplete player results')
        if not all(np.isfinite(scores)) or float(row['score']) != scores[seat] or int(row['rank']) != ranks[seat]:
            raise ValueError('Score/rank mismatch')
        share = 1 / ranks.count(1) if ranks[seat] == 1 else 0
        if not np.isclose(float(row['win_share']), share) or not 0 <= share <= 1:
            raise ValueError('Incorrect tie credit')
        if not np.isclose(float(row['score_gap']), scores[seat] - max(scores[:seat] + scores[seat+1:])):
            raise ValueError('Incorrect opponent score gap')
        if row['result'] not in ('WIN_GAME', 'LOSE_GAME', 'DRAW_GAME'):
            raise ValueError('Disqualified or incomplete game')
        if (ranks[seat] == 1) != (row['result'] in ('WIN_GAME', 'DRAW_GAME')):
            raise ValueError('Rank/result disagreement')
        decisions = int(row['decisions'])
        if decisions <= 0 or (mode == 'iterations' and int(row['iterations']) != 150 * decisions):
            raise ValueError('Unexpected search effort')
        for key in ('fm_calls', 'copy_calls', 'total_decision_ms', 'max_decision_ms', 'game_wall_ms'):
            if not np.isfinite(float(row[key])) or float(row[key]) < 0:
                raise ValueError('Invalid effort/timing measurement')
    if len(set(counts)) != 1:
        raise ValueError('Unbalanced seats')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('study', type=Path)
    parser.add_argument('--allow-pilot', action='store_true')
    args = parser.parse_args()
    root = args.study.resolve()
    manifest = json.loads((root / 'manifest.json').read_text())
    protocol = manifest['protocol']
    if not manifest.get('completed'):
        raise ValueError('Study is not complete')
    games = protocol['games_per_cell']
    if games < 500 and not args.allow_pilot:
        raise ValueError('Principal comparisons require at least 500 games; --allow-pilot labels a small validation run')
    for relative, expected in manifest['source_sha256'].items():
        snapshot_name = relative + manifest.get('snapshot_java_suffix', '') if relative.endswith('.java') else relative
        if hashlib.sha256((root / 'snapshot' / snapshot_name).read_bytes()).hexdigest() != expected:
            raise ValueError('Snapshot hash mismatch: ' + relative)
    raw, out = root / 'raw', root / 'analysis'
    out.mkdir(exist_ok=True)
    rng = np.random.default_rng(703205)
    summaries, contrasts, feature_effects, timings = [], [], [], []
    cells, bootstrap, used_seeds = {}, {}, set()
    for n in (3, 4):
        for opponent in ('random', 'osla', 'baseline'):
            variants = MAIN + (['combined_no_' + name for name in FEATURES] if opponent == 'baseline' else [])
            for variant in variants:
                stem = f'iterations_{n}p_{variant}_vs_{opponent}'
                rows = records(raw / (stem + '.csv'))
                meta = json.loads((raw / (stem + '.json')).read_text())
                if meta['games'] != games or meta['master_seed'] != protocol['master_seed']:
                    raise ValueError('Cell metadata mismatch')
                validate_cell(rows, n, games, variant, opponent, 'iterations', used_seeds)
                cells[n, opponent, variant] = rows
                seats = [int(row['seat']) for row in rows]
                win = np.array([float(row['win_share']) for row in rows])
                score = np.array([float(row['score']) for row in rows])
                win_samples = stratified_bootstrap(win, seats, rng)
                score_samples = stratified_bootstrap(score, seats, rng)
                bootstrap[n, opponent, variant] = win_samples
                low, high = interval(win_samples)
                score_low, score_high = interval(score_samples)
                summaries.append({'players': n, 'opponent': opponent, 'variant': variant, 'games': games,
                                  'games_per_seat': games // n, 'wins': sum(row['result'] == 'WIN_GAME' for row in rows),
                                  'draws': sum(row['result'] == 'DRAW_GAME' for row in rows),
                                  'win_share': float(win.mean()), 'win_share_low95': low, 'win_share_high95': high,
                                  'score': float(score.mean()), 'score_low95': score_low, 'score_high95': score_high,
                                  'mean_rank': float(np.mean([int(row['rank']) for row in rows])),
                                  'mean_score_gap': float(np.mean([float(row['score_gap']) for row in rows])),
                                  'decisions': sum(int(row['decisions']) for row in rows),
                                  'total_fm_calls': sum(int(row['fm_calls']) for row in rows),
                                  'over_500ms': sum(int(row['over_500ms']) for row in rows)})
            definitions = {
                'heuristic_with_paranoid': {'heuristic': 1, 'baseline': -1},
                'maxn_with_default': {'multiplayer': 1, 'baseline': -1},
                'heuristic_with_maxn': {'combined': 1, 'multiplayer': -1},
                'maxn_with_heuristic': {'combined': 1, 'heuristic': -1},
                'combined_vs_baseline': {'combined': 1, 'baseline': -1},
                'interaction': {'combined': 1, 'heuristic': -1, 'multiplayer': -1, 'baseline': 1},
            }
            for name, weights in definitions.items():
                samples = sum(weight * bootstrap[n, opponent, v] for v, weight in weights.items())
                estimate = sum(weight * np.mean([float(row['win_share']) for row in cells[n, opponent, v]])
                               for v, weight in weights.items())
                low, high = interval(samples)
                contrasts.append({'players': n, 'opponent': opponent, 'contrast': name,
                                  'difference': float(estimate), 'low95': low, 'high95': high})
            if opponent == 'baseline':
                for name in FEATURES:
                    samples = bootstrap[n, opponent, 'combined'] - bootstrap[n, opponent, 'combined_no_' + name]
                    estimate = np.mean([float(row['win_share']) for row in cells[n, opponent, 'combined']]) - np.mean(
                        [float(row['win_share']) for row in cells[n, opponent, 'combined_no_' + name]])
                    low, high = interval(samples)
                    feature_effects.append({'players': n, 'feature': name, 'full_minus_removed': float(estimate),
                                           'low95': low, 'high95': high})

    decision_cells = {}
    if not protocol['skip_timing']:
        for n in (3, 4):
            for variant in MAIN:
                stem = f'500ms_{n}p_{variant}_vs_random'
                game_rows = records(raw / (stem + '.csv'))
                validate_cell(game_rows, n, n, variant, 'random', '500ms', used_seeds)
                decisions = records(raw / (stem + '.decisions.csv'))
                if len(decisions) != sum(int(row['decisions']) for row in game_rows):
                    raise ValueError('Decision and game counts disagree')
                for index in range(n):
                    subset = [row for row in decisions if int(row['game_index']) == index]
                    if [int(row['decision']) for row in subset] != list(range(1, len(subset) + 1)):
                        raise ValueError('Duplicate or missing decision records')
                    if any(row['variant'] != variant or int(row['players']) != n or int(row['seat']) != index
                           for row in subset):
                        raise ValueError('Decision labels disagree')
                elapsed = np.array([float(row['elapsed_ms']) for row in decisions])
                if not np.all(np.isfinite(elapsed)) or np.any(elapsed < 0):
                    raise ValueError('Invalid timing sample')
                over = int(np.sum(elapsed > 500))
                if over != sum(int(row['over_500ms']) for row in game_rows):
                    raise ValueError('Overrun totals disagree')
                decision_cells[n, variant] = elapsed
                timings.append({'players': n, 'variant': variant, 'games': n, 'decisions': len(elapsed),
                                'mean_ms': float(elapsed.mean()), 'p50_ms': float(np.quantile(elapsed, .5)),
                                'p95_ms': float(np.quantile(elapsed, .95)), 'p99_ms': float(np.quantile(elapsed, .99)),
                                'max_ms': float(elapsed.max()), 'over_500ms': over,
                                'mean_iterations': float(np.mean([int(row['iterations']) for row in decisions])),
                                'fm_calls_per_second': sum(int(row['fm_calls']) for row in decisions) / (elapsed.sum()/1000)})

    write_csv(out / 'summary.csv', summaries)
    write_csv(out / 'contrasts.csv', contrasts)
    write_csv(out / 'feature-effects.csv', feature_effects)
    write_csv(out / 'timing-summary.csv', timings)
    plot_results(out, summaries, feature_effects, decision_cells, games)
    report = ['# Stage 5 results', '',
              f'Validated {len(summaries) * games:,} fixed-iteration games across {len(summaries)} cells '
              f'({games} games per cell). Each cell has equal numbers in every seat.', '',
              'Each game has one focal agent and opponents of the same type in the other '
              'seats. These comparisons use multiplayer lineups. Win share is 1 for a sole '
              "winner, 1/k for each of k tied winners after TAG's Pudding tiebreak, and 0 "
              'otherwise.', '',
              'Pointwise 95% percentile intervals use 5,000 bootstrap resamples within each '
              'seat. Cells have independent seed streams, and contrasts use independent '
              'bootstrap draws. The intervals are exploratory and unadjusted for multiple '
              'comparisons. Shared controls make some contrasts correlated, so the analysis '
              'makes no significance claim for the whole family of tests. If all sampled '
              'outcomes are identical, the interval has zero width; the population rate can '
              'still be uncertain.', '',
              '## Main comparisons', '',
              '![Win share by variant, opponent and player count](main-comparisons.png)', '',
              '[Vector PDF](main-comparisons.pdf). Base is the corrected baseline, '
              'Heuristic adds the custom evaluation, MaxN changes opponent selection, and '
              'Both combines the changes. The dashed 1/n line shows the reference for equal '
              'players. Expected win share differs against stronger or weaker opponents.', '',
              '| Players | Opponents | Variant | Win share (95% CI) | Mean score |',
              '|---:|---|---|---:|---:|']
    if games < 500:
        report.insert(2, '**Pilot only: sample size is below the principal-study requirement.**\n')
    for row in summaries:
        if row['variant'] in MAIN:
            report.append(f"| {row['players']} | {row['opponent']} | {row['variant']} | "
                          f"{row['win_share']:.1%} ({row['win_share_low95']:.1%}–{row['win_share_high95']:.1%}) | {row['score']:.2f} |")
    report += ['', '## Combined versus corrected baseline', '',
               '| Players | Opponents | Difference in win share | Pointwise 95% CI |', '|---:|---|---:|---:|']
    for row in contrasts:
        if row['contrast'] == 'combined_vs_baseline':
            report.append(f"| {row['players']} | {row['opponent']} | {100*row['difference']:+.2f} pp | "
                          f"{100*row['low95']:+.2f} to {100*row['high95']:+.2f} pp |")
    report += ['', '[contrasts.csv](contrasts.csv) contains every conditional effect and the '
                   'factorial interaction.', '', '## Feature removals', '',
               'A positive full-minus-removed difference favours keeping the feature. An '
               'interval crossing zero leaves the direction unresolved. Each main-study '
               'condition has 504 games against baseline opponents. All removals keep MAXN '
               'and the other weights at 1.0, so other feature interactions remain untested.', '',
               '![Win-share effects of individual heuristic features](feature-ablations.png)', '',
               '[Vector PDF](feature-ablations.pdf) · [Exact estimates](feature-effects.csv)', '',
               '| Players | Feature | Full minus removed | Pointwise 95% CI |', '|---:|---|---:|---:|']
    for row in feature_effects:
        report.append(f"| {row['players']} | {row['feature']} | {100*row['full_minus_removed']:+.2f} pp | "
                      f"{100*row['low95']:+.2f} to {100*row['high95']:+.2f} pp |")
    report += ['', '## Timing study', '']
    if timings:
        count = sum(row['decisions'] for row in timings)
        overruns = sum(row['over_500ms'] for row in timings)
        report += [f'The sequential timing study contains 28 complete games and {count} searched decisions. '
                   f'Decisions exceeding 500 ms: {overruns}. The largest was '
                   f"{max(row['max_ms'] for row in timings):.3f} ms.", '',
                   '| Players | Variant | Decisions | Median ms | P95 ms | Max ms | Over 500 ms |',
                   '|---:|---|---:|---:|---:|---:|---:|']
        for row in timings:
            report.append(f"| {row['players']} | {row['variant']} | {row['decisions']} | {row['p50_ms']:.2f} | "
                          f"{row['p95_ms']:.2f} | {row['max_ms']:.2f} | {row['over_500ms']} |")
        report += ['', '![Distribution of searched-decision latencies](decision-timing.png)', '',
                   '[Vector PDF](decision-timing.pdf) · [Exact timing summaries](timing-summary.csv)', '',
                   'The measurements cover searched decisions in all three rounds and Chopsticks '
                   'when encountered. They exclude external observation and action-list '
                   'generation, logging and forced single-action turns. The configuration uses '
                   '450 ms with a 25 ms reserve; warmups and measured games use a 128-512 MiB '
                   'JVM heap. This small sample describes observed latency. It cannot establish '
                   'playing strength at 500 ms or guarantee that every decision meets the '
                   'deadline.']
    else:
        report.append('Timing was skipped for this pilot.')
    report += ['', '## Replication and limits', '',
               'The manifest records the TAG revision, source and configuration hashes, '
               'machine, runtime and seed policy. Snapshots preserve the sources and '
               'settings used with the raw data. Equal iteration counts do not equalise '
               'elapsed time: MaxN evaluates more players per leaf. Recorded seeds also '
               'cannot reproduce every hidden-state sample in TAG. The results apply to the '
               'tested opponents and budgets; they do not rank all agents or establish '
               'optimality.', '',
               'Weights were fixed before the study. A configuration chosen from these '
               'results should be checked on new games before making a final strength claim.', '']
    (out / 'RESULTS.md').write_text('\n'.join(report))
    metadata = {'bootstrap_repetitions': 5000, 'bootstrap_seed': 703205,
                'intervals': 'pointwise 95% seat-stratified percentile bootstrap; no multiple-comparison adjustment',
                'python': platform.python_version(), 'numpy': np.__version__, 'matplotlib': matplotlib.__version__,
                'analysis_script_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                'validated_fixed_games': len(summaries) * games,
                'validated_timing_games': sum(row['games'] for row in timings),
                'validated_timing_decisions': sum(row['decisions'] for row in timings)}
    (out / 'validation.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print(json.dumps(metadata, indent=2))

def plot_results(out, summaries, feature_effects, decision_cells, games):
    plt.rcParams.update({'font.size': 10, 'axes.spines.top': False, 'axes.spines.right': False,
                         'pdf.fonttype': 42, 'figure.facecolor': 'white'})
    index = {(row['players'], row['opponent'], row['variant']): row for row in summaries}
    fig, axes = plt.subplots(2, 3, figsize=(10.5, 6), sharey=True, layout='constrained')
    for i, n in enumerate((3, 4)):
        for j, opponent in enumerate(('random', 'osla', 'baseline')):
            ax = axes[i, j]
            for x, variant in enumerate(MAIN):
                row = index[n, opponent, variant]
                mean = row['win_share']
                ax.errorbar(x, mean, yerr=[[max(0, mean-row['win_share_low95'])],
                                          [max(0, row['win_share_high95']-mean)]],
                            fmt='o', color=COLORS[x], markersize=7, capsize=4, linewidth=1.6)
            ax.axhline(1/n, color='#AAAAAA', linestyle='--', linewidth=.8)
            ax.set_xticks(range(4), ['Base', 'Heuristic', 'MaxN', 'Both'], rotation=20)
            ax.set_title(f'{n} players · {opponent.upper() if opponent == "osla" else opponent} opponents')
            ax.set_ylim(-.03, 1.03); ax.set_yticks(np.linspace(0, 1, 6)); ax.set_xlim(-.5, 3.5)
            ax.yaxis.set_major_formatter(PercentFormatter(1))
            ax.grid(axis='y', alpha=.18)
            if j == 0: ax.set_ylabel('Win share (ties split)')
    fig.suptitle(f'150 iterations per searched decision · {games} games per cell\n'
                 'Pointwise 95% bootstrap intervals; dashed line = 1 / player count', fontsize=12)
    for extension in ('png', 'pdf'): fig.savefig(out / ('main-comparisons.' + extension), dpi=180)
    plt.close(fig)

    fig, axes = plt.subplots(1, 2, figsize=(9, 3.8), sharex=True, layout='constrained')
    for ax, n in zip(axes, (3, 4)):
        for y, name in enumerate(FEATURES):
            row = next(row for row in feature_effects if row['players'] == n and row['feature'] == name)
            value = 100 * row['full_minus_removed']
            ax.errorbar(value, y, xerr=[[max(0, value-100*row['low95'])], [max(0, 100*row['high95']-value)]],
                        fmt='o', color='#0072B2', capsize=4)
        ax.axvline(0, color='#777777', linestyle='--', linewidth=.8)
        ax.set_yticks(range(4), ['Sets', 'Wasabi', 'Maki', 'Pudding']); ax.invert_yaxis()
        ax.set_title(f'{n} players against baseline opponents')
        ax.set_xlabel('Full minus feature removed (percentage points)'); ax.grid(axis='x', alpha=.18)
    fig.suptitle('Individual feature contributions · pointwise 95% bootstrap intervals', fontsize=12)
    for extension in ('png', 'pdf'): fig.savefig(out / ('feature-ablations.' + extension), dpi=180)
    plt.close(fig)

    if decision_cells:
        fig, axes = plt.subplots(1, 2, figsize=(9, 4), sharey=True, layout='constrained')
        for ax, n in zip(axes, (3, 4)):
            values = [decision_cells[n, variant] for variant in MAIN]
            plot = ax.boxplot(values, tick_labels=['Base', 'Heuristic', 'MaxN', 'Both'], patch_artist=True,
                              showfliers=True, flierprops={'markersize': 2, 'alpha': .6})
            for box, color in zip(plot['boxes'], COLORS): box.set_facecolor(color); box.set_alpha(.55)
            ax.axhline(500, color='#D55E00', linestyle='--', linewidth=1.2)
            ax.set_title(f'{n} players · one complete game per seat')
            ax.set_ylabel('Decision latency (ms)'); ax.grid(axis='y', alpha=.18); ax.set_ylim(bottom=0)
        fig.suptitle('500 ms target · 450 ms budget and 25 ms reserve\nSequential runs; 512 MiB maximum JVM heap', fontsize=12)
        for extension in ('png', 'pdf'): fig.savefig(out / ('decision-timing.' + extension), dpi=180)
        plt.close(fig)

if __name__ == '__main__':
    main()
