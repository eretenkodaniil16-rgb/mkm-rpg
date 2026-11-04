"""Offline regression gates for R02 motion-plan and Blender source specification."""
from __future__ import annotations

import copy
import math
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from validate_r02 import load,validate


class SwordR02Tests(unittest.TestCase):
    def setUp(self):
        self.data=load()

    def test_duration_and_contact_match_server_reference(self):
        a=self.data
        self.assertEqual((a["frame_end"]-a["frame_start"])/a["fps"],a["action_ticks"]/20)
        self.assertEqual((a["contact_frame"]-a["frame_start"])/a["fps"],a["visual_contact_tick"]/20)

    def test_expected_ten_phases_and_review_frames(self):
        self.assertEqual(len(self.data["keys"]),10)
        self.assertGreaterEqual(len(self.data["review_frames"]),9)
        self.assertEqual(self.data["keys"][5]["frame"],16)

    def test_source_only_right_hand_and_license_references(self):
        self.assertTrue(self.data["source_only"])
        self.assertEqual(self.data["weapon_family"],"one_handed_sword")
        self.assertEqual(self.data["mainhand"],"RIGHT")
        self.assertGreaterEqual(len(self.data["source_refs"]),3)

    def test_repeat_ready_pose_and_wrist_location(self):
        a=self.data["keys"][0]
        b=self.data["keys"][-1]
        self.assertEqual(a["right_hand_target"],b["right_hand_target"])
        self.assertEqual(a["sword_tip_direction"],b["sword_tip_direction"])

    def test_right_to_left_tip_path_contract(self):
        a=self.data["keys"]
        self.assertGreater(a[3]["right_hand_target"][0],a[5]["right_hand_target"][0])
        self.assertGreater(a[3]["sword_tip_direction"][2],0.7)
        self.assertLess(a[5]["sword_tip_direction"][0],-0.7)

    def test_windup_head_clearance_strategy(self):
        a=self.data["keys"][3]
        self.assertGreater(a["right_hand_target"][0],0.52)
        self.assertGreater(a["sword_tip_direction"][0],0.4)

    def test_reject_wrong_contact(self):
        x=copy.deepcopy(self.data);x["contact_frame"]=17
        with self.assertRaises(ValueError):validate(x)

    def test_reject_invalid_hand(self):
        x=copy.deepcopy(self.data);x["mainhand"]="OFFHAND"
        with self.assertRaises(ValueError):validate(x)

    def test_reject_bad_direction(self):
        x=copy.deepcopy(self.data);x["keys"][2]["sword_tip_direction"]=[0,0,10]
        with self.assertRaises(ValueError):validate(x)

    def test_reject_missing_phase(self):
        x=copy.deepcopy(self.data);x["keys"].pop(3)
        with self.assertRaises(ValueError):validate(x)

    def test_reject_nonfinite_target(self):
        x=copy.deepcopy(self.data);x["keys"][4]["right_hand_target"][2]=math.nan
        with self.assertRaises(ValueError):validate(x)

    def test_reject_bad_end_pose(self):
        x=copy.deepcopy(self.data);x["keys"][-1]["spine_euler_deg"]=[0,2,0]
        with self.assertRaises(ValueError):validate(x)

    def test_reject_less_than_nine_samples(self):
        x=copy.deepcopy(self.data);x["review_frames"]=[1,4,12,16,27,43]
        with self.assertRaises(ValueError):validate(x)

    def test_reject_keyframe_order(self):
        x=copy.deepcopy(self.data);x["keys"][6]["frame"]=14
        with self.assertRaises(ValueError):validate(x)


if __name__=="__main__":
    unittest.main()
