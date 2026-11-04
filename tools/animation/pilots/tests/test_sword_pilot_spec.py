"""Pure-Python regression tests for the Blender sword-strike authoring pilot.

Run from repository root:
    python -m unittest discover -s tools/animation/pilots/tests -p 'test_*.py' -v
"""
from __future__ import annotations

import copy
import math
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from validate_sword_pilot import load_spec, validate_spec


class SwordPilotSpecificationTests(unittest.TestCase):
    def setUp(self):
        self.spec = load_spec()

    def test_reference_duration_matches_fourteen_server_ticks(self):
        self.assertEqual(self.spec["minecraft_action_ticks"], 14)
        self.assertEqual(self.spec["fps"], 60)
        self.assertEqual((self.spec["frame_end"] - self.spec["frame_start"]) / self.spec["fps"], 14 / 20)

    def test_impact_is_at_server_reference_tick_five(self):
        self.assertEqual(self.spec["minecraft_impact_tick"], 5)
        self.assertEqual((self.spec["impact_frame"] - self.spec["frame_start"]) / self.spec["fps"], 5 / 20)

    def test_reference_has_nine_review_frames_and_neutral_endpoints(self):
        self.assertGreaterEqual(len(self.spec["render_frames"]), 9)
        self.assertEqual(self.spec["poses"][0]["degrees"], {})
        self.assertEqual(self.spec["poses"][-1]["degrees"], {})
        self.assertIn(self.spec["impact_frame"], self.spec["render_frames"])

    def test_rejects_wrong_impact_timing(self):
        candidate = copy.deepcopy(self.spec)
        candidate["impact_frame"] += 1
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_non_finite_rotation(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"][1]["degrees"]["spine"][2] = math.nan
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_angle_over_one_hundred_twenty_degrees(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"][2]["degrees"]["upper_arm.R"][0] = 121
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_undeclared_animation_bone(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"][2]["degrees"]["secret_bone"] = [0, 0, 0]
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_missing_impact_keypose(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"] = [p for p in candidate["poses"] if p["frame"] != candidate["impact_frame"]]
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_non_neutral_recovery_endpoint(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"][-1]["degrees"] = {"spine": [0, 0, 2]}
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_too_few_review_frames(self):
        candidate = copy.deepcopy(self.spec)
        candidate["render_frames"] = candidate["render_frames"][:5]
        with self.assertRaises(ValueError):
            validate_spec(candidate)

    def test_rejects_duplicate_keypose_frame(self):
        candidate = copy.deepcopy(self.spec)
        candidate["poses"][3]["frame"] = candidate["poses"][2]["frame"]
        with self.assertRaises(ValueError):
            validate_spec(candidate)


if __name__ == "__main__":
    unittest.main()
