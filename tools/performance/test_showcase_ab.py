"""Contract tests on SYNTHETIC fixtures. Not Minecraft benchmark results."""
import copy
import unittest

from evaluate_runs import compare
from showcase_ab import evaluate_manifest


class ShowcaseComparatorTest(unittest.TestCase):
    def run(self, arm, phase, trial, p95=None, p99=None):
        base = arm == "baseline"
        return {
            "arm": arm, "phase": phase, "trial": trial,
            "environment": {
                "minecraft": "26.3", "java": "25", "hardware": "synthetic-test-host",
                "mkm_sha": "a" * 40 if base else "b" * 40,
                "world_seed": "20261108", "world_copy_sha256": "0" * 64,
                "scenario": "combat40", "player_count": 2, "entity_count": 40,
                "render_distance": 10, "simulation_distance": 6,
                "duration_seconds": 600, "fabric_loader": "0.20.1",
                "jvm_args": "-Xmx4G",
                "modlist": ["mkm-synthetic-baseline"] if base else ["mkm-synthetic-candidate"],
                "experiment_setting": "directPlayerScan=false" if base
                    else "directPlayerScan=true",
            },
            "metrics": {
                "tick_p50_ms": 20 if base else 18,
                "tick_p95_ms": p95 if p95 is not None else (40 if base else 34),
                "tick_p99_ms": p99 if p99 is not None else (60 if base else 58),
                "fps_average": 90 if base else 95,
                "fps_1pct_low": 60 if base else 64,
                "gc_pause_total_ms": 8,
                "allocated_bytes": 2000000,
                "ai_callback_mean_us": 25.0,
                "loaded_entities": 40,
                "loaded_chunks": 150,
                "heap_peak_mb": 1400,
                "chunks_per_second": 20 if base else 25,
            },
            "evidence": {
                "mspt_source": "spark",
                "profile_url": "https://example.invalid/TEST-SYNTHETIC-NOT-REAL",
                "flamegraph_url": "https://example.invalid/TEST-SYNTHETIC-NOT-REAL",
            },
        }

    def manifest(self):
        return {"runs": [
            self.run(arm, phase, trial)
            for phase in ("cold", "warm")
            for trial in ("A", "B")
            for arm in ("baseline", "candidate")
        ]}

    def test_distinct_commits_are_comparable(self):
        m = self.manifest()
        result = compare(m["runs"][0], m["runs"][1])
        self.assertGreater(result["p95_improvement_percent"], 10.0)

    def test_two_pairs_per_phase_with_all_reported_evidence(self):
        report = evaluate_manifest(self.manifest())
        self.assertEqual(report["pairs_total"], 4)
        self.assertTrue(report["meets_provisional_performance_gate"])
        self.assertIn("exploratory", report["phases"]["warm"]["confidence"])

    def test_single_trial_cannot_pass(self):
        m = self.manifest()
        m["runs"] = [r for r in m["runs"] if r["trial"] == "A"]
        self.assertFalse(evaluate_manifest(m)["meets_provisional_performance_gate"])

    def test_missing_cold_phase_cannot_pass(self):
        m = self.manifest()
        m["runs"] = [r for r in m["runs"] if r["phase"] == "warm"]
        self.assertFalse(evaluate_manifest(m)["meets_provisional_performance_gate"])

    def test_p99_regression_in_single_pair_fails(self):
        m = self.manifest()
        for r in m["runs"]:
            if r["arm"] == "candidate" and r["phase"] == "warm" and r["trial"] == "B":
                r["metrics"]["tick_p99_ms"] = 69
        self.assertFalse(evaluate_manifest(m)["meets_provisional_performance_gate"])

    def test_missing_spark_evidence_fails(self):
        m = self.manifest()
        m["runs"][0]["evidence"]["mspt_source"] = "mkmperf ticks"
        self.assertFalse(evaluate_manifest(m)["meets_provisional_performance_gate"])

    def test_missing_fps_low_data_fails_provisional(self):
        m = self.manifest()
        del m["runs"][0]["metrics"]["fps_1pct_low"]
        self.assertFalse(evaluate_manifest(m)["meets_provisional_performance_gate"])

    def test_wrong_world_copy_rejected(self):
        m = self.manifest()
        m["runs"][1]["environment"]["world_copy_sha256"] = "1" * 64
        with self.assertRaisesRegex(ValueError, "world_copy_sha256"):
            evaluate_manifest(m)

    def test_chunk_engines_cannot_be_combined(self):
        m = self.manifest()
        m["runs"][0]["environment"]["modlist"] = ["moonrise 26.3", "c2me 26.3"]
        with self.assertRaisesRegex(ValueError, "Moonrise and C2ME"):
            evaluate_manifest(m)

    def test_bad_percentile_monotonicity_rejected(self):
        m = self.manifest()
        m["runs"][0]["metrics"]["tick_p95_ms"] = 75
        with self.assertRaisesRegex(ValueError, "not monotonic"):
            evaluate_manifest(m)

    def test_missing_half_of_pair_rejected(self):
        m = self.manifest()
        m["runs"].pop()
        with self.assertRaisesRegex(ValueError, "one baseline and one candidate"):
            evaluate_manifest(m)

    def test_boolean_metric_rejected(self):
        m = self.manifest()
        m["runs"][0]["metrics"]["tick_p50_ms"] = True
        with self.assertRaisesRegex(ValueError, "finite number"):
            evaluate_manifest(m)


if __name__ == "__main__":
    unittest.main()
