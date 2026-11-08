import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parent))
import capture_f5_comparison as frames

class F5Protocol(unittest.TestCase):
    def test_synchronized_server_hit(self):
        self.assertEqual(9,len(frames.KEY_FRAMES))
        self.assertEqual(5.0,frames.frame_to_server_tick(16))
        self.assertAlmostEqual(.25,frames.frame_offsets()[16])
        self.assertAlmostEqual(.70,frames.frame_offsets()[43])

    def test_increasing_fixed_frame_offsets(self):
        offsets=list(frames.frame_offsets().values())
        self.assertEqual(0,offsets[0])
        self.assertEqual(sorted(offsets),offsets)
        self.assertEqual(len(set(offsets)),9)
        self.assertEqual(3*9,27)

if __name__=="__main__":
    unittest.main()
