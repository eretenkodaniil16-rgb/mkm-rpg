#!/usr/bin/env python3
"""Evidence-aware paired A/B gate for MKM Showcase #80.

This tool NEVER runs Minecraft or calculates MSPT from /mkmperf ticks.
All supplied timing values must originate from an external authoritative profiler.
"""
import argparse
import json
import math
import random
import statistics
from collections import defaultdict
from pathlib import Path

from evaluate_runs import compare

REQUIRED_METRICS = ("tick_p50_ms", "tick_p95_ms", "tick_p99_ms")
RECOMMENDED_METRICS = (
    "fps_average", "fps_1pct_low", "gc_pause_total_ms",
    "allocated_bytes", "ai_callback_mean_us", "loaded_entities",
    "loaded_chunks", "heap_peak_mb", "chunks_per_second",
)
REQUIRED_CONTEXT = (
    "minecraft", "java", "hardware", "mkm_sha", "world_seed",
    "scenario", "player_count", "entity_count", "render_distance",
    "simulation_distance", "duration_seconds", "world_copy_sha256",
    "fabric_loader", "jvm_args", "modlist", "experiment_setting",
)


def _finite_positive(value, key, zero=False):
    if type(value) not in (int, float) or not math.isfinite(value):
        raise ValueError(f"{key} must be a finite number (bool is not numeric)")
    if value < 0 if zero else value <= 0:
        raise ValueError(f"{key} must be {'nonnegative' if zero else 'positive'}")


def _bootstrap_median_ci(values, draws=2000):
    """Descriptive, paired bootstrap; with 2 pairs it is NOT strong inference."""
    rng = random.Random(80080)
    n = len(values)
    samples = sorted(statistics.median(
        [values[rng.randrange(n)] for _ in range(n)]
    ) for _ in range(draws))
    return [round(samples[int(draws * 0.025)], 2),
            round(samples[min(draws - 1, int(draws * 0.975))], 2)]


def evaluate_manifest(manifest):
    if not isinstance(manifest, dict) or not isinstance(manifest.get("runs"), list):
        raise ValueError("Expected an object with a 'runs' array")
    pairs = defaultdict(dict)
    missing_observations = []
    missing_optional = set()
    flamegraphs = 0
    for index, run in enumerate(manifest["runs"]):
        arm, phase, trial = run.get("arm"), run.get("phase"), str(run.get("trial", ""))
        if arm not in ("baseline", "candidate") or phase not in ("cold", "warm") or not trial:
            raise ValueError(f"Invalid arm/phase/trial at run {index}")
        key = (phase, trial)
        if arm in pairs[key]:
            raise ValueError(f"Duplicate run for {key} / {arm}")
        env = run.get("environment", {})
        absent = [k for k in REQUIRED_CONTEXT if k not in env]
        if absent:
            raise ValueError(f"Run {index} missing context: {', '.join(absent)}")
        if str(env["java"]) != "25" or env["minecraft"] != "26.3":
            raise ValueError(f"Run {index} must target Java 25 and Minecraft 26.3")
        if env["player_count"] not in (1, 2) or env["entity_count"] not in (0, 20, 40, 80):
            raise ValueError(f"Run {index} has an unapproved Showcase cohort")
        if not isinstance(env["modlist"], list) or not env["modlist"]:
            raise ValueError(f"Run {index} needs a versioned modlist")
        mods = " ".join(str(item).lower() for item in env["modlist"])
        if "moonrise" in mods and "c2me" in mods:
            raise ValueError("Moonrise and C2ME must NEVER share a benchmark profile")
        if not env["mkm_sha"] or not env["world_copy_sha256"]:
            raise ValueError(f"Run {index} needs exact build/world provenance")
        metrics = run.get("metrics", {})
        for k in REQUIRED_METRICS:
            _finite_positive(metrics.get(k), k)
        if metrics["tick_p50_ms"] > metrics["tick_p95_ms"] or metrics["tick_p95_ms"] > metrics["tick_p99_ms"]:
            raise ValueError(f"Run {index} tick percentiles are not monotonic")
        for k in RECOMMENDED_METRICS:
            if k not in metrics:
                missing_optional.add(k)
            elif metrics[k] is not None:
                _finite_positive(metrics[k], k, zero=k in (
                    "gc_pause_total_ms", "allocated_bytes", "loaded_entities",
                    "loaded_chunks", "chunks_per_second", "ai_callback_mean_us"))
        evidence = run.get("evidence", {})
        if evidence.get("mspt_source") != "spark" or not evidence.get("profile_url"):
            missing_observations.append(index)
        if evidence.get("flamegraph_url"):
            flamegraphs += 1
        pairs[key][arm] = run

    if not pairs:
        raise ValueError("No benchmark runs")
    if any(set(ab.keys()) != {"baseline", "candidate"} for ab in pairs.values()):
        raise ValueError("Every phase/trial must have one baseline and one candidate")
    phases = {}
    for phase in ("cold", "warm"):
        group = [(trial, entry) for (p, trial), entry in sorted(pairs.items()) if p == phase]
        if not group:
            phases[phase] = {"pairs": 0, "status": "missing"}
            continue
        gains = []
        p99_gains = []
        chunk_gains = []
        versions = {"baseline": set(), "candidate": set()}
        for trial, pair in group:
            a, b = pair["baseline"], pair["candidate"]
            # Same hardware, seed, world copy, conditions, and run length.
            # MKM SHA, optional optimizer modlist and experiment flag may differ.
            for key in ("world_copy_sha256", "fabric_loader", "jvm_args"):
                if a["environment"][key] != b["environment"][key]:
                    raise ValueError(f"Mismatch in {key} for {phase}/{trial}")
            outcome = compare(a, b)
            gains.append(outcome["p95_improvement_percent"])
            p99_gains.append(outcome["p99_improvement_percent"])
            if outcome["chunk_throughput_improvement_percent"] is not None:
                chunk_gains.append(outcome["chunk_throughput_improvement_percent"])
            for arm in versions:
                versions[arm].add((pair[arm]["environment"]["mkm_sha"],
                                   json.dumps(pair[arm]["environment"]["modlist"], sort_keys=True),
                                   str(pair[arm]["environment"]["experiment_setting"])))
        if any(len(items) != 1 for items in versions.values()):
            raise ValueError(f"Mixed build/mod/setting identities inside {phase} cohort")
        n = len(group)
        median_p95 = statistics.median(gains)
        median_p99 = statistics.median(p99_gains)
        median_chunks = statistics.median(chunk_gains) if len(chunk_gains) == n else None
        stable_p99 = all(gain >= -5.0 for gain in p99_gains)
        phases[phase] = {
            "pairs": n,
            "p95_gain_percent_median": median_p95,
            "p95_paired_bootstrap_95pct_interval": _bootstrap_median_ci(gains),
            "p99_gain_percent_median": median_p99,
            "worst_p99_gain_percent": min(p99_gains),
            "chunk_throughput_gain_percent_median": median_chunks,
            "no_pair_has_p99_regression_over_5_percent": stable_p99,
            "threshold_met": n >= 2 and stable_p99 and (
                median_p95 >= 10.0 or (median_chunks is not None and median_chunks >= 20.0)),
            "confidence": "exploratory (n<5, bootstrap descriptive only)" if n < 5
                          else "replicated (bootstrap descriptive; gameplay QA required)",
        }
    provenance = not missing_observations and flamegraphs >= 1
    provisional = provenance and not missing_optional and all(
        phases[phase].get("threshold_met") for phase in ("cold", "warm"))
    return {
        "pairs_total": len(pairs),
        "phases": phases,
        "profiling_evidence_complete": provenance,
        "missing_provenance_run_indices": missing_observations,
        "flamegraph_count": flamegraphs,
        "missing_recommended_metrics": sorted(missing_optional),
        "meets_provisional_performance_gate": bool(provisional),
        "approval": "REQUIRES manual provenance review, repeatability and in-game correctness",
        "notice": "Input metrics are unverified external measurements; this script never profiles or runs Minecraft. /mkmperf ticks is not authoritative MSPT.",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="JSON with paired actual profile runs")
    parser.add_argument("--out", type=Path, help="Write machine-readable report JSON")
    args = parser.parse_args()
    result = evaluate_manifest(json.loads(args.manifest.read_text(encoding="utf-8")))
    encoded = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(encoded, encoding="utf-8")
    print(encoded, end="")


if __name__ == "__main__":
    main()
