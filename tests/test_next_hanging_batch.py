"""Movement-specific screens for the next hanging-core source batch."""
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.next_hanging import audit as audit_next_hanging


def _norm(v):
    return math.sqrt(sum(x*x for x in v))


def _angle(a, b, c):
    u = [a[i]-b[i] for i in range(3)]
    v = [c[i]-b[i] for i in range(3)]
    return math.degrees(math.acos(max(-1., min(1., sum(x*y for x, y in zip(u, v)) /
                                                 (_norm(u)*_norm(v))))))


def _foot_segment(frame, side):
    ankle, knee, toe = (frame[name+"_"+side] for name in ("ankle", "knee", "toe"))
    fd = [x/_norm([toe[i]-ankle[i] for i in range(3)]) for x in
          [toe[i]-ankle[i] for i in range(3)]]
    away = [ankle[i]-knee[i] for i in range(3)]
    projection = sum(a*b for a, b in zip(away, fd))
    normal = [away[i]-projection*fd[i] for i in range(3)]
    nl = _norm(normal)
    normal = [x/nl for x in normal] if nl > 1e-8 else [0., 0., -1.]
    heel = [ankle[i]-.045*fd[i]+.040*normal[i] for i in range(3)]
    tip = [ankle[i]+.165*fd[i]+.045*normal[i] for i in range(3)]
    return heel, tip


def _foot_bar_gap(frame, side, bar):
    """Signed capsule gap to the finite Y-axis bar, using renderer foot geometry."""
    heel, tip = _foot_segment(frame, side)
    x, half, z = bar.get("x", 0.0), bar["half"], bar["z"]
    delta = [tip[i]-heel[i] for i in range(3)]
    def gap(s):
        point = [heel[i]+s*delta[i] for i in range(3)]
        nearest = (x, max(-half, min(half, point[1])), z)
        radius = .036-.010*s
        return math.dist(point, nearest)-bar["r"]-radius
    lo, hi = 0., 1.
    ratio = (math.sqrt(5.)-1.)/2.
    a, b = hi-ratio*(hi-lo), lo+ratio*(hi-lo)
    ga, gb = gap(a), gap(b)
    for _ in range(32):
        if ga <= gb:
            hi, b, gb = b, a, ga
            a = hi-ratio*(hi-lo); ga = gap(a)
        else:
            lo, a, ga = a, b, gb
            b = lo+ratio*(hi-lo); gb = gap(b)
    return min(gap(0.), gap(1.), ga, gb)


def _max_speed(frames, joints):
    return max(math.dist(frames[i][joint], frames[i+1][joint])*24
               for i in range(len(frames)-1) for joint in joints)


def _torso_bench_margin(frame):
    """Sample the torso ring envelope over the bench footprint, independently."""
    sections = ((-.10,.12,.095),(0,.155,.10),(.09,.145,.095),(.20,.135,.09),
                (.29,.15,.10),(.39,.175,.112),(.45,.185,.12),(.48,.185,.11),
                (.51,.16,.095),(.57,.055,.055))
    p, spine = frame["pelvis"], frame["spine"]
    u1, u2 = frame["pelvis_up"], frame["chest_up"]
    l1, l2 = frame["pelvis_left"], frame["chest_left"]
    lowest = math.inf
    for length, width, depth in sections:
        if length <= .24:
            center = [p[i]+u1[i]*length for i in range(3)]
            up, left = u1, l1
        else:
            center = [spine[i]+u2[i]*(length-.24) for i in range(3)]
            up, left = u2, l2
        forward = [left[1]*up[2]-left[2]*up[1],
                   left[2]*up[0]-left[0]*up[2],
                   left[0]*up[1]-left[1]*up[0]]
        norm = _norm(forward)
        forward = [v/norm for v in forward]
        for j in range(64):
            theta = 2*math.pi*j/64
            point = [center[k] + left[k]*width*math.cos(theta)
                     + forward[k]*depth*math.sin(theta) for k in range(3)]
            if -.55 <= point[0] <= .4 and -.325 <= point[1] <= .325:
                lowest = min(lowest, point[2])
    return lowest - .55


class NextHangingBatchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data = {key: poses.build(key)[0]
                    for key in ("toes-to-bar", "heel-hook", "dragon-flag-negative")}

    def test_toes_to_bar_reaches_bar_with_straight_legs_and_controlled_hang(self):
        data = self.data["toes-to-bar"]
        frames = data["frames"]
        bar = next(p for p in data["props"] if p["kind"] == "bar")
        peak = max((f["ankle_l"][2]+f["ankle_r"][2])*.5 for f in frames)
        top = [f for f in frames if (f["ankle_l"][2]+f["ankle_r"][2])*.5 >= peak-.03]
        for side in ("l", "r"):
            contacts = [f for f in top if abs(_foot_bar_gap(f, side, bar)) <= .006]
            self.assertTrue(contacts, f"{side} toe/foot misses the bar")
            self.assertGreaterEqual(min(_angle(f["hip_"+side], f["knee_"+side],
                                                f["ankle_"+side]) for f in contacts), 165.)
        self.assertLessEqual(_max_speed(frames, ("knee_l", "knee_r")), 4.0)
        for side in ("l", "r"):
            self.assertLessEqual(max(math.dist(f["wrist_"+side], frames[0]["wrist_"+side])
                                     for f in frames), .005)

    def test_heel_hook_crosses_both_feet_above_bar_and_returns_with_control(self):
        data = self.data["heel-hook"]
        frames = data["frames"]
        bar = next(p for p in data["props"] if p["kind"] == "bar")
        initial_order = frames[0]["ankle_l"][1] > frames[0]["ankle_r"][1]
        high_crossed = [f for f in frames
                        if (f["ankle_l"][1] < f["ankle_r"][1]) == initial_order
                        and min(f["ankle_l"][2], f["ankle_r"][2]) >= bar["z"]+bar["r"]]
        self.assertTrue(high_crossed, "ankles never cross on the far side of the bar")
        for side in ("l", "r"):
            self.assertLessEqual(min(abs(_foot_bar_gap(f, side, bar)) for f in high_crossed), .006,
                                 f"{side} heel/foot does not hook the bar")
        self.assertLessEqual(_max_speed(frames, ("knee_l", "knee_r")), 4.0)
        for side in ("l", "r"):
            self.assertLessEqual(max(math.dist(f["wrist_"+side], frames[0]["wrist_"+side])
                                     for f in frames), .005)

    def test_dragon_flag_negative_stays_straight_on_lower_and_has_a_controlled_tuck_roll(self):
        data = self.data["dragon-flag-negative"]
        frames = data["frames"]
        # Before the tuck recovery, the torso frame stays aligned and knees stay extended.
        angles = [_angle(f["hip_l"], f["knee_l"], f["ankle_l"]) for f in frames]
        tuck_start = next((i for i, value in enumerate(angles) if value < 160.), len(frames))
        pre_tuck = frames[:tuck_start]
        for f in pre_tuck:
            torso_angle = math.degrees(math.acos(max(-1., min(1., sum(
                a*b for a, b in zip(f["pelvis_up"], f["chest_up"])) ))))
            self.assertLessEqual(torso_angle, 8.)
        knee_angles = angles[:tuck_start]
        self.assertGreaterEqual(min(knee_angles), 165.)
        later_angles = angles[tuck_start:]
        self.assertLess(min(later_angles), 140., "recovery has no readable knee tuck")
        self.assertGreaterEqual(later_angles[-1], 165., "recovery does not return to straight legs")
        self.assertLessEqual(_max_speed(frames, ("knee_l", "knee_r")), 4.0)
        self.assertGreaterEqual(min(_torso_bench_margin(f) for f in frames), .005,
                                "torso envelope sinks through the bench")

    def test_generation_audit_reports_bounded_metrics_for_all_three_variants(self):
        for exercise_id, data in self.data.items():
            result = audit_next_hanging(data)
            self.assertEqual(result["failures"], [], exercise_id)
            self.assertTrue(all(metric["ok"] for metric in result["metrics"].values()))
            self.assertTrue(all("min" in metric and "max" in metric
                                for metric in result["metrics"].values()))

    def test_generation_audit_rejects_lost_toes_bar_contact(self):
        import copy
        damaged = copy.deepcopy(self.data["toes-to-bar"])
        bar = next(p for p in damaged["props"] if p["kind"] == "bar")
        mean_z = [(f["ankle_l"][2]+f["ankle_r"][2])*.5 for f in damaged["frames"]]
        peak = max(mean_z)
        for i, frame in enumerate(damaged["frames"]):
            if mean_z[i] >= peak-.03:
                frame["ankle_l"][2] += .08
                frame["toe_l"][2] += .08
        checks = {failure["check"] for failure in audit_next_hanging(damaged)["failures"]}
        self.assertIn("l_bar_contact", checks)

    def test_generation_audit_rejects_foot_shifted_off_bar_axis(self):
        import copy
        damaged = copy.deepcopy(self.data["toes-to-bar"])
        mean_z = [(f["ankle_l"][2]+f["ankle_r"][2])*.5 for f in damaged["frames"]]
        peak = max(mean_z)
        for i, frame in enumerate(damaged["frames"]):
            if mean_z[i] >= peak-.03:
                frame["ankle_l"][0] += .08
                frame["toe_l"][0] += .08
        checks = {failure["check"] for failure in audit_next_hanging(damaged)["failures"]}
        self.assertIn("l_bar_contact", checks)

    def test_strict_toes_keeps_long_legs_for_the_entire_raise_and_lower(self):
        import gzip, json
        result = audit_next_hanging(self.data["toes-to-bar"])
        self.assertTrue(result["metrics"]["strict_full_cycle_knee_min_deg"]["ok"])
        self.assertTrue(result["metrics"]["hip_knee_length_error_m"]["ok"])
        self.assertTrue(result["metrics"]["knee_ankle_length_error_m"]["ok"])
        with gzip.open(ROOT / "tests/fixtures/motion/toes-to-bar-bent-transition.json.gz", "rt") as f:
            before = json.load(f)
        failures = {x["check"] for x in audit_next_hanging(before)["failures"]}
        self.assertIn("strict_full_cycle_knee_min_deg", failures)

    def test_collinear_half_length_knees_cannot_fake_strict_leg_form(self):
        import copy
        data = copy.deepcopy(self.data["toes-to-bar"])
        for f in data["frames"]:
            for side in ("l", "r"):
                f["knee_"+side] = [(a+b)*.5 for a,b in zip(f["hip_"+side],f["ankle_"+side])]
        failures = {x["check"] for x in audit_next_hanging(data)["failures"]}
        self.assertIn("hip_knee_length_error_m", failures)

    def test_tapered_foot_contact_stays_clear_through_the_full_cycle(self):
        for exercise in ("toes-to-bar", "heel-hook"):
            result = audit_next_hanging(self.data[exercise])
            self.assertEqual(result["failures"], [], exercise)
            minimum = result["metrics"]["minimum_foot_bar_gap_m"]["value"]
            self.assertGreaterEqual(minimum, .0005, exercise)
            for side in ("l", "r"):
                key = (side+"_top_foot_bar_gap_m" if exercise == "toes-to-bar"
                       else side+"_hook_foot_gap_m")
                self.assertLessEqual(result["metrics"][key]["value"], .006, exercise)

    def test_generation_audit_rejects_heel_hook_without_crossed_feet(self):
        import copy
        damaged = copy.deepcopy(self.data["heel-hook"])
        bar = next(p for p in damaged["props"] if p["kind"] == "bar")
        for frame in damaged["frames"]:
            if min(frame["ankle_l"][2], frame["ankle_r"][2]) >= bar["z"]+bar["r"]:
                frame["ankle_l"][1] = abs(frame["ankle_l"][1]) + .15
                frame["toe_l"][1] = abs(frame["toe_l"][1]) + .15
        checks = {failure["check"] for failure in audit_next_hanging(damaged)["failures"]}
        self.assertTrue({"both_ankles_crossed_above_bar_frames", "l_hook_foot_gap_m"} & checks)

    def test_generation_audit_rejects_dragon_flag_bench_intersection(self):
        import copy
        damaged = copy.deepcopy(self.data["dragon-flag-negative"])
        frames = damaged["frames"]
        index = min(range(len(frames)), key=lambda i: _torso_bench_margin(frames[i]))
        frames[index]["pelvis"][2] -= .03
        frames[index]["spine"][2] -= .03
        checks = {failure["check"] for failure in audit_next_hanging(damaged)["failures"]}
        self.assertIn("torso_bench_proxy_clearance_m", checks)


if __name__ == "__main__":
    unittest.main()
