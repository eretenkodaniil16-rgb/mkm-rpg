#!/usr/bin/env python3
"""Compare externally measured MKM benchmark JSON runs; never invent metrics."""
import argparse
import json
import math
from pathlib import Path

MATCH_FIELDS = (
    'minecraft', 'java', 'hardware', 'mkm_sha', 'world_seed', 'scenario',
    'player_count', 'entity_count', 'render_distance', 'simulation_distance',
    'duration_seconds',
)


def compare(baseline: dict, candidate: dict) -> dict:
    a, b = baseline['environment'], candidate['environment']
    missing = [key for key in MATCH_FIELDS if key not in a or key not in b]
    if missing:
        raise ValueError('Missing benchmark identity fields: ' + ', '.join(missing))
    different = [key for key in MATCH_FIELDS if a[key] != b[key]
    if different:
        raise ValueError('Workloads are not comparable: ' + ', '.join(different))
    if a.get('minecraft') != '26.3':
        raise ValueError('Benchmark requires Minecraft 26.3')
    am, bm = baseline['metrics'], candidate['metrics']
    for key in ('tick_p95_ms', 'tick_p99_ms'):
        if not isinstance(am.get(key), (int, float)) or not isinstance(bm.get(key), (int, float)):
            raise ValueError(f'Missing numeric metric: {key}')
        if not math.isfinite(am[key]) or not math.isfinite(bm[key]) or am[key] <= 0 or bm[key] <= 0:
            raise ValueError(f'Non-positive timing metric: {key}')
    p95_gain = (am['tick_p95_ms'] - bm['tick_p95_ms']) / am['tick_p95_ms'] * 100
    p99_gain = (am['tick_p99_ms'] - bm['tick_p99_ms']) / am['tick_p99_ms'] * 100
    chunk_gain = None
    if 'chunks_per_second' in am and 'chunks_per_second' in bm:
        if (math.isfinite(am['chunks_per_second']) and math.isfinite(bm['chunks_per_second'])
                and am['chunks_per_second'] > 0 and bm['chunks_per_second'] >= 0):
            chunk_gain = (bm['chunks_per_second'] - am['chunks_per_second']) / am['chunks_per_second'] * 100
    improved = p95_gain >= 10.0 or (chunk_gain is not None and chunk_gain >= 20.0)
    safe_p99 = p99_gain >= -5.0
    return {
        'p95_improvement_percent': round(p95_gain, 2),
        'p99_improvement_percent': round(p99_gain, 2),
        'chunk_throughput_improvement_percent': None if chunk_gain is None else round(chunk_gain, 2),
        'meets_provisional_performance_gate': improved and safe_p99,
        'warning': 'Reported-data check only; real profiling provenance, save safety and gameplay correctness require QA.',
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline', type=Path)
    parser.add_argument('candidate', type=Path)
    args = parser.parse_args()
    baseline = json.loads(args.baseline.read_text(encoding='utf-8'))
    candidate = json.loads(args.candidate.read_text(encoding='utf-8'))
    print(json.dumps(compare(baseline, candidate), ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
