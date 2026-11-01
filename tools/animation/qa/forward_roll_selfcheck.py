#!/usr/bin/env python3
"""MKM animation self-check: deterministic clip validation and forward-roll storyboard.

Not a substitute for real rendered gameplay, Blender review, collisions or mocap QA.
Produces an SVG schematic and a machine-readable JSON report for every CI build.
Uses only the Python standard library.
"""
import argparse
import json
import math
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/mkm/animations"
PI = math.pi
HALF_HEIGHT = 0.90
HALF_DEPTH = 0.38
PIVOT = 0.90
MARKERS = {"takeoff": 0.06, "shoulder_contact": 0.29, "land": 0.76}


def smooth(value):
    value = max(0.0, min(1.0, value))
    return value * value * (3 - 2 * value)


def forward_pose(t):
    u = max(0.0, min(1.0, (t - 0.06) / 0.70))
    angle = 2 * PI * smooth(u)
    correction = HALF_HEIGHT * abs(math.cos(angle)) + HALF_DEPTH * abs(math.sin(angle)) - PIVOT
    return angle, correction


def read(rel):
    return json.loads((ASSETS / rel).read_text(encoding="utf-8"))


def check_clip(clip):
    assert clip["format"] == 1 and 2 <= clip["duration_ticks"] <= 200
    for bone, keys in clip["tracks"].items():
        assert 2 <= len(keys) <= 256, bone
        assert abs(keys[0]["t"]) < 1e-8 and abs(keys[-1]["t"] - 1) < 1e-8
        last = -1
        for key in keys:
            assert last < key["t"] <= 1.0, (bone, key)
            last = key["t"]
            assert len(key["r"]) == 3 and all(
                math.isfinite(v) and abs(v) <= 2 * PI for v in key["r"]
            )


def sample_track(track, t):
    for j in range(1, len(track)):
        a, b = track[j - 1], track[j]
        if t <= b["t"]:
            f = smooth((t - a["t"]) / (b["t"] - a["t"]))
            return [a["r"][i] * (1 - f) + b["r"][i] * f for i in range(3)]
    return track[-1]["r"]


def side_outline(angle, correction):
    points = []
    for z, y in [(-HALF_DEPTH, 0), (HALF_DEPTH, 0),
                 (HALF_DEPTH, 2 * HALF_HEIGHT), (-HALF_DEPTH, 2 * HALF_HEIGHT)]:
        yy = y - PIVOT
        # Rotation about -X: Y'=Y*cos + Z*sin; Z'=-Y*sin + Z*cos
        horizontal = yy * math.sin(angle) - z * math.cos(angle)
        vertical = PIVOT + correction + yy * math.cos(angle) + z * math.sin(angle)
        points.append((horizontal, vertical))
    return points


def svg_storyboard(clip, output):
    width, height = 1470, 520
    ts = [0, .06, .13, .22, .29, .39, .50, .62, .76, .88, 1.0]
    scale = 84.0
    sx = 132
    baseline = 400
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="#101721"/>',
        '<text x="35" y="45" font-family="Arial,sans-serif" font-size="24" fill="#e9edf5">MKM C3 — forward roll geometry self-check</text>',
        '<text x="35" y="74" font-family="Arial,sans-serif" font-size="15" fill="#9bb0c5">Schematics only: corrected rotation axis, projected ground contact and sampled shoulder tuck.</text>',
        f'<line x1="24" y1="{baseline}" x2="{width-24}" y2="{baseline}" stroke="#66d6aa" stroke-width="4"/>'
    ]
    for i, t in enumerate(ts):
        angle, dy = forward_pose(t)
        cx = sx + i * 118
        poly = side_outline(angle, dy)
        coords = " ".join(f"{cx + x*scale:.2f},{baseline-y*scale:.2f}" for x,y in poly)
        parts.append(f'<polygon points="{coords}" fill="#3f80bb" fill-opacity=".23" stroke="#8ad2f5" stroke-width="2"/>')
        # Shoulder / head: torso/head local +Y, rotated forward, drawn to side.
        hy = PIVOT + dy + .65 * math.cos(angle)
        hx = .65 * math.sin(angle)
        parts.append(f'<circle cx="{cx + hx*scale:.2f}" cy="{baseline-hy*scale:.2f}" r="11" fill="#ffd49a" stroke="#f3ae64" stroke-width="2"/>')
        # Arms are track-controlled; illustrate tuck amplitude separately.
        right = sample_track(clip["tracks"]["right_arm"], t)[0]
        left = sample_track(clip["tracks"]["left_arm"], t)[0]
        arm_tuck = min(1.0, (abs(right) + abs(left)) / (2 * math.pi))
        parts.append(f'<rect x="{cx-33}" y="{baseline+14}" width="{66*arm_tuck:.2f}" height="4" fill="#f3ae64"/>')
        parts.append(f'<text x="{cx-34}" y="{baseline+40}" font-family="Arial,sans-serif" font-size="13" fill="#d9e3f1">{t:.0%}</text>')
        parts.append(f'<text x="{cx-34}" y="{baseline+57}" font-family="Arial,sans-serif" font-size="11" fill="#8aa4b6">{math.degrees(angle):.0f}°</text>')
    parts.extend([
        '<text x="38" y="503" font-family="Arial,sans-serif" font-size="14" fill="#8ab8c4">Green = reference ground; blue = body bounding proxy; orange = head/tuck reference (not actual Minecraft render).</text>',
        '</svg>'
    ])
    output.write_text("\n".join(parts), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="build/reports/animation-qa")
    args = parser.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    index = read("index.json")
    clip_map = {}
    for filename in index["clips"]:
        clip = read(filename)
        check_clip(clip)
        assert clip["id"] not in clip_map
        clip_map[clip["id"]] = clip
    assert len(clip_map) >= 15
    forward = clip_map["mkm:sword/dodge_0"]
    assert forward["duration_ticks"] == 10
    markers = {m["name"]: m["t"] for m in forward["markers"]}
    assert all(k in markers and abs(markers[k] - v) <= .0001
               for k, v in MARKERS.items())
    assert abs(sample_track(forward["tracks"]["head"], .29)[0]) > .2
    assert abs(sample_track(forward["tracks"]["right_leg"], .29)[0]
               - sample_track(forward["tracks"]["left_leg"], .29)[0]) > .1

    previous = -1.0
    max_step = 0.0
    floor_error = 0.0
    for i in range(201):
        t = i / 200
        angle, correction = forward_pose(t)
        assert angle >= previous - 1e-8, "Roll angle reversed"
        if i:
            max_step = max(max_step, angle - previous)
        previous = angle
        lowest = min(y for _, y in side_outline(angle, correction))
        floor_error = max(floor_error, abs(lowest))
        assert abs(lowest) < 1e-6, f"Forward roll clips ground at t={t:.3f}: {lowest:.5f}"
        assert math.isfinite(angle) and math.isfinite(correction)
    assert max_step < .08, "Excessively abrupt 5ms-normalized model rotation"
    assert abs(forward_pose(0)[0]) < 1e-9
    assert abs(forward_pose(.76)[0] - 2 * PI) < 1e-6
    assert abs(forward_pose(1)[1]) < 1e-7

    report = {
        "schema": 1, "status": "PASS",
        "scope": "C3 static keyframes + corrected forward-roll proxy; no actual gameplay physics",
        "clips_validated": len(clip_map),
        "forward_roll_samples": 201,
        "max_rotation_increment_rad_per_sample": round(max_step, 5),
        "max_reference_ground_error_blocks": round(floor_error, 7),
        "forward_roll_axis_local": [-1, 0, 0],
        "duration_server_ticks": forward["duration_ticks"],
        "markers": markers,
        "remaining_manual_checks": [
            "F5 Minecraft real render, camera, armor, weapons",
            "actual collision and terrain; no hitbox alteration",
            "lag/multiplayer movement and root displacement",
            "visual shoulder contact and recovery quality"
        ]
    }
    (out / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    svg_storyboard(forward, out / "forward-roll-storyboard.svg")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
