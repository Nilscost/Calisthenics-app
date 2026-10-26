"""Scoped contact/phase checks for the next handstand candidates."""
import gzip
import hashlib
import json
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.next_handstand import audit as audit_next_handstand

FIXTURES = ROOT / "tests/fixtures/motion/next-handstand"
WORLD_JOINTS = ("pelvis", "spine", "chest", "neck", "head") + tuple(
    f"{name}_{side}" for name in ("hip", "knee", "ankle", "toe", "shoulder", "elbow", "wrist", "hand")
    for side in ("l", "r")
)


def toe_wall_error(frame):
    """Distance of each +X toe anchor from the wall's x=0 near face."""
    return max(abs(frame[side][0]) for side in ("toe_l", "toe_r"))


def kick_stagger(frames):
    # The catalog's kick-up phase must show one leg rising ahead of its mate.
    sample = frames[int(.16 * len(frames)):int(.32 * len(frames))]
    return max(abs(frame["ankle_l"][2] - frame["ankle_r"][2]) for frame in sample)


def max_joint_step(frames, joint):
    return max(math.dist(a[joint], b[joint]) for a, b in zip(frames, frames[1:]))


def max_loop_step(frames, joint):
    return max(math.dist(a[joint], b[joint]) for a, b in zip(frames, frames[1:] + frames[:1]))


def max_joint_speed(frames, fps, joint):
    return max_joint_step(frames, joint) * fps


def max_loop_speed(frames, fps):
    return max(max_loop_step(frames, joint) * fps for joint in WORLD_JOINTS)


class NextHandstandBatchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.wall, cls.wall_reach = poses.build("handstand-wall-walk-up")
        cls.kick, cls.kick_reach = poses.build("handstand-kick-up")

    def test_both_candidates_pass_reach_and_pose_geometry(self):
        self.assertEqual([], self.wall_reach)
        self.assertEqual([], self.kick_reach)
        self.assertEqual([], poses.checks(self.wall))
        self.assertEqual([], poses.checks(self.kick))
        self.assertLessEqual(max_loop_speed(self.wall["frames"], self.wall["fps"]), 2.5)
        self.assertLessEqual(max_loop_speed(self.kick["frames"], self.kick["fps"]), 2.5)

    def test_wide_straddle_press_pose_output_is_pinned(self):
        expected_digest = "525eb3e7da36062aab42cb81d9b2134342700b07a0b71b77193b986e402314e0"
        data, warnings = poses.build("handstand-press-straddle")
        digest = hashlib.sha256(json.dumps(data, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
        self.assertEqual([], warnings)
        self.assertEqual(expected_digest, digest)

    def test_wall_walk_only_toe_anchors_touch_wall_and_hands_stay_planted(self):
        errors = [toe_wall_error(frame) for frame in self.wall["frames"]]
        self.assertLess(max(errors), .006)
        wrists = [frame["wrist_l"] for frame in self.wall["frames"]]
        self.assertLess(wrists[0][0], wrists[len(wrists)//2][0])
        self.assertGreater(wrists[len(wrists)//2][0], wrists[int(.75 * len(wrists))][0])
        # Hand swings are real steps now (<= the 2.5 m/s gate); the loop seam must not pop.
        self.assertLess(max_loop_step(self.wall["frames"], "wrist_l"), 2.5 / self.wall["fps"])
        seam = math.dist(self.wall["frames"][-1]["wrist_l"], self.wall["frames"][0]["wrist_l"])
        self.assertLess(seam, .002)

    def test_wall_walk_hands_alternate_planted_steps_without_sliding(self):
        frames = self.wall["frames"]
        up = {side: [f[side][2] > .036 for f in frames] for side in ("wrist_l", "wrist_r")}
        self.assertEqual(0, sum(1 for l, r in zip(up["wrist_l"], up["wrist_r"]) if l and r))
        for side in ("wrist_l", "wrist_r"):
            self.assertGreaterEqual(sum(1 for i, v in enumerate(up[side]) if v and not up[side][i - 1]), 6)
            for a, b in zip(frames, frames[1:] + frames[:1]):
                if a[side][2] <= .036 and b[side][2] <= .036:
                    self.assertLess(math.hypot(b[side][0] - a[side][0], b[side][1] - a[side][1]), .001)
            self.assertGreaterEqual(min(f[side][2] for f in frames), .034)
        self.assertLess(min(f["ankle_l"][2] for f in frames), .09)  # starts as a floor plank
        self.assertGreaterEqual(len(frames) / self.wall["fps"], 6)
        self.assertLessEqual(len(frames) / self.wall["fps"], 9)

    def test_wall_walk_head_ellipsoid_clears_floor_and_rejects_frozen_baseline(self):
        current = audit_next_handstand(self.wall)
        self.assertTrue(current["metrics"]["head_ellipsoid_floor_proxy_m"]["ok"])
        self.assertGreater(current["metrics"]["head_ellipsoid_floor_proxy_m"]["value"], .02)
        path = FIXTURES / "handstand-wall-walk-up-baseline.json.gz"
        with gzip.open(path, "rt") as stream:
            frozen = json.load(stream)
        old = audit_next_handstand(frozen)
        self.assertFalse(old["metrics"]["head_ellipsoid_floor_proxy_m"]["ok"])

    def test_frozen_pre_fix_wall_walk_is_a_negative_contact_control(self):
        path = FIXTURES / "handstand-wall-walk-up-baseline.json.gz"
        with gzip.open(path, "rt") as stream:
            frozen = json.load(stream)
        self.assertGreater(max(toe_wall_error(frame) for frame in frozen["frames"]), .15)
        self.assertGreater(max_loop_step(frozen["frames"], "wrist_l"), .5)
        self.assertLess(max(toe_wall_error(frame) for frame in self.wall["frames"]), .006)

    def test_frozen_pre_fix_kickup_has_knee_pole_flip_and_current_path_is_continuous(self):
        path = FIXTURES / "handstand-kick-up-baseline.json.gz"
        with gzip.open(path, "rt") as stream:
            frozen = json.load(stream)
        self.assertGreater(max_joint_step(frozen["frames"], "knee_l"), .5)
        self.assertLessEqual(max_joint_speed(self.kick["frames"], self.kick["fps"], "knee_l"), 2.5)

    def test_kick_up_keeps_asymmetric_follow_leg_transition_and_vertical_hold(self):
        self.assertGreaterEqual(kick_stagger(self.kick["frames"]), .30)
        frames = self.kick["frames"]
        vertical = [f for f in frames if abs(f["ankle_l"][2] - 1.965) < .005
                    and abs(f["ankle_r"][2] - 1.965) < .005]
        self.assertGreaterEqual(len(vertical) / self.kick["fps"], .70)
        top = [frames[i] for i in range(int(.46 * len(frames)), int(.55 * len(frames)))]
        self.assertTrue(top)
        self.assertLess(max(abs(f["ankle_l"][2] - 1.96) for f in top), .04)
        self.assertLess(max(abs(f["ankle_r"][2] - 1.96) for f in top), .04)

    def test_symmetric_kick_transition_is_rejected(self):
        bad = json.loads(json.dumps(self.kick))
        for frame in bad["frames"][int(.16 * len(bad["frames"])):int(.32 * len(bad["frames"]))]:
            frame["ankle_r"][2] = frame["ankle_l"][2]
        self.assertLess(kick_stagger(bad["frames"]), .01)


if __name__ == "__main__":
    unittest.main()
