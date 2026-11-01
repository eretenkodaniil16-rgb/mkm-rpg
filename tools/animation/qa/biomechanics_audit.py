#!/usr/bin/env python3
"""MKM C3.8: independently sample authored clips and emit motion QA.

No Blender, Minecraft or third-party packages required. The SVG is a schematic
six-part SIDE-PLANE proxy, NOT Minecraft footage, foot IK or collision proof.
All result units are explicit. No gameplay state is mutated.
"""
import argparse
import json
import math
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[3]
ANIMATIONS = ROOT / "src/main/resources/assets/mkm/animations"
ATTACKS = ["sword/light_1", "sword/light_2", "sword/light_3",
           "unarmed/jab", "unarmed/cross", "unarmed/uppercut"]
DODGES = ["sword/dodge_" + str(i) for i in range(8)]
SERVER_WINDUPS = (5, 4, 6)
SERVER_RECOVERY = (8, 7, 11)
FPS = 60
N = 120


def read_clip(name):
    return json.loads((ANIMATIONS / (name + ".json")).read_text(encoding="utf-8"))


def slope(keys, i, axis):
    if i == 0 or i == len(keys)-1:
        return 0.0
    a, b, c = keys[i-1:i+2]
    h0, h1 = b["t"]-a["t"], c["t"]-b["t"]
    d0 = (b["r"][axis]-a["r"][axis])/h0
    d1 = (c["r"][axis]-b["r"][axis])/h1
    if not all(math.isfinite(x) for x in (d0, d1)) or not d0 or not d1 or (d0 > 0) != (d1 > 0):
        return 0.0
    w0, w1 = 2*h1+h0, h1+2*h0
    den = w0/d0 + w1/d1
    return (w0+w1)/den if math.isfinite(den) and den else 0.0


def sample_axis(keys, t, axis):
    if t <= 0:
        return keys[0]["r"][axis]
    if t >= 1:
        return keys[-1]["r"][axis]
    for i in range(1, len(keys)):
        b = keys[i]
        if t <= b["t"]:
            a = keys[i-1]
            h = b["t"]-a["t"]
            u = max(0.0, min(1.0, (t-a["t"])/h))
            u2, u3 = u*u, u*u*u
            v0, v1 = a["r"][axis], b["r"][axis]
            m0, m1 = slope(keys, i-1, axis), slope(keys, i, axis)
            result = (2*u3-3*u2+1)*v0+(u3-2*u2+u)*h*m0+(-2*u3+3*u2)*v1+(u3-u2)*h*m1
            return max(min(v0, v1), min(max(v0, v1), result))
    return keys[-1]["r"][axis]


def pose(clip, t):
    return {part: [sample_axis(keys, t, a) for a in range(3)]
            for part, keys in clip["tracks"].items()}


def validate(clip, name):
    assert clip["format"] == 1 and clip["id"] == "mkm:" + name
    assert 2 <= clip["duration_ticks"] <= 200
    assert clip["layer"] in ("upper_body", "full_body")
    assert 1 <= len(clip["tracks"]) <= 6
    for part, keys in clip["tracks"].items():
        assert 2 <= len(keys) <= 256 and keys[0]["t"] == 0 and keys[-1]["t"] == 1
        assert all(keys[i-1]["t"] < keys[i]["t"] for i in range(1, len(keys)))
        assert all(len(k["r"]) == 3 and all(math.isfinite(x) and abs(x) <= 2*math.pi
                   for x in k["r"]) for k in keys), (name, part)
        if clip["layer"] == "upper_body":
            assert part not in ("left_leg", "right_leg")
        for i in range(N + 1):
            t = i/N
            for axis in range(3):
                x = sample_axis(keys, t, axis)
                assert math.isfinite(x), (name, part, axis, t)
        # Each authored key must be exactly preserved, no overshoot.
        for i in range(1, len(keys)):
            left, right = keys[i-1], keys[i]
            for j in range(11):
                t = left["t"]+(right["t"]-left["t"])*j/10
                for a in range(3):
                    x = sample_axis(keys, t, a)
                    lo, hi = sorted((left["r"][a], right["r"][a]))
                    assert lo-1e-5 <= x <= hi+1e-5, (name, part, a, t)
    if name in ATTACKS:
        stage = ATTACKS.index(name) % 3
        duration = SERVER_WINDUPS[stage] + 1 + SERVER_RECOVERY[stage]
        assert clip["duration_ticks"] == duration
        marker = [m for m in clip["markers"] if m["name"] == "impact"]
        assert len(marker) == 1
        assert abs(marker[0]["t"]-SERVER_WINDUPS[stage]/duration) < 0.001
        assert clip["layer"] == "full_body" and "left_leg" in clip["tracks"] and "right_leg" in clip["tracks"]
    if name in DODGES:
        assert clip["duration_ticks"] == 10


def measure(name, clip):
    duration_s = clip["duration_ticks"]/20.0
    data = {"clip": name, "duration_ticks": clip["duration_ticks"], "duration_s": duration_s,
            "layer": clip["layer"], "bones": {}, "notes": []}
    dt = duration_s / N
    for bone, keys in clip["tracks"].items():
        peak = 0.0
        peak_vel = 0.0
        peak_accel = 0.0
        for axis in range(3):
            angles = [sample_axis(keys, i/N, axis) for i in range(N+1)]
            peak = max(peak, *(abs(v) for v in angles))
            speeds = [(angles[i+1]-angles[i])/dt for i in range(N)]
            peak_vel = max(peak_vel, *(abs(v) for v in speeds))
            accelerations = [(speeds[i+1]-speeds[i])/dt for i in range(N-1)]
            peak_accel = max(peak_accel, *(abs(v) for v in accelerations))
        data["bones"][bone] = {"max_abs_rotation_deg": round(math.degrees(peak), 2),
                "max_angular_velocity_deg_s": round(math.degrees(peak_vel), 2),
                "max_angular_acceleration_deg_s2": round(math.degrees(peak_accel), 2)}
    if name in ATTACKS:
        stage = ATTACKS.index(name) % 3
        impact = SERVER_WINDUPS[stage]/clip["duration_ticks"]
        p = pose(clip, impact)
        data["impact_phase"] = round(impact, 6)
        data["leg_pitch_sum_deg_at_impact"] = round(math.degrees(
            abs(p["left_leg"][0])+abs(p["right_leg"][0])), 2)
        data["notes"].append("Leg animation is angular-only: no root COM or grounded foot constraint.")
    else:
        data["notes"].append("Ground contact not established by JSON: requires world-space test.")
    return data


def line(x1, y1, x2, y2, stroke, width=4):
    return f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="round"/>'


def skeleton(x, y, clip, t):
    # Side-plane cartoon based on pitch rotations of the 6-part vanilla rig.
    # Explicitly not the game model: no skin, armor, attachments or geometry.
    p = pose(clip, t)
    theta = p.get("body", (0, 0, 0))[0]
    hip_x = x + 36 * math.sin(theta)
    hip_y = y + 6
    shoulder_x = hip_x + 38 * math.sin(theta)
    shoulder_y = hip_y - 39 * math.cos(theta)
    drawing = [line(x-46, y+43, x+49, y+43, "#9aabb7", 1),
               line(hip_x, hip_y, shoulder_x, shoulder_y, "#d9e1e8", 12)]
    for part, dx, col in (("left_leg", -8, "#56bfc8"), ("right_leg", 8, "#e9af67")):
        a = p.get(part, (0, 0, 0))[0]
        drawing.append(line(hip_x+dx, hip_y, hip_x+dx+30*math.sin(a), hip_y+30*math.cos(a), col, 8))
    for part, dx, col in (("left_arm", -8, "#56bfc8"), ("right_arm", 8, "#e9af67")):
        a = p.get(part, (0, 0, 0))[0]
        drawing.append(line(shoulder_x+dx, shoulder_y, shoulder_x+dx+34*math.sin(a),
                            shoulder_y+34*math.cos(a), col, 7))
    drawing.append(f'<circle cx="{shoulder_x:.1f}" cy="{shoulder_y-11:.1f}" r="11" fill="#d9e1e8"/>')
    return "".join(drawing)


def generate_sheet(attacks, output):
    labels = ("start", "anticipation", "impact", "follow-through", "settle")
    w, h, cellw, rowh = 1420, 1040, 248, 154
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">',
           '<rect width="100%" height="100%" fill="#17202b"/>',
           '<text x="24" y="38" font-family="sans-serif" font-size="25" fill="#fafafa">MKM C3.8 / 6 attack pose sequences</text>',
           '<text x="24" y="60" font-family="sans-serif" font-size="12" fill="#b7c2ce">SCHEMATIC side-plane proxy, NOT Minecraft footage. Orange: right / Teal: left. No actual ground contact measured.</text>']
    for row, (name, clip) in enumerate(attacks):
        stage = row % 3
        hit = SERVER_WINDUPS[stage] / clip["duration_ticks"]
        times = (0, hit*.48, hit, min(1, hit+.17), 1)
        top = 83 + row * rowh
        svg.append(f'<text x="16" y="{top+55}" font-size="14" fill="#f0f4f8" font-family="monospace">{escape(name)}</text>')
        for col, (tag, t) in enumerate(zip(labels, times)):
            cellx = 170 + col*cellw
            svg.append(f'<rect x="{cellx}" y="{top}" width="{cellw-11}" height="{rowh-8}" rx="6" fill="#222f3c"/>')
            svg.append(skeleton(cellx+118, top+95, clip, t))
            svg.append(f'<text x="{cellx+12}" y="{top+20}" font-size="12" fill="#e3eaf2">{tag} / {t:.2f}</text>')
    svg.append('<text x="22" y="1020" fill="#bec9d5" font-family="sans-serif" font-size="13">Review real game F5 capture for foot skating, weapon grip, armor clipping and camera occlusion.</text>')
    svg.append('</svg>')
    output.write_text("\n".join(svg)+"\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path("build/reports/animation-qa"))
    opt = parser.parse_args()
    opt.out.mkdir(parents=True, exist_ok=True)
    names = ATTACKS + DODGES + ["sword/combat_idle"]
    records = []
    attacks = []
    for name in names:
        clip = read_clip(name)
        validate(clip, name)
        records.append(measure(name, clip))
        if name in ATTACKS:
            attacks.append((name, clip))
    output = {"status": "PASS", "scope": "static six-part pose proxy; not actual rendered gameplay",
              "sampling": {"intervals": N, "server_tps_assumption": 20},
              "assets_checked": len(records), "clips": records}
    (opt.out / "biomechanics-report.json").write_text(
        json.dumps(output, indent=2)+"\n", encoding="utf-8")
    generate_sheet(attacks, opt.out / "c3-8-attack-contact-sheet.svg")
    print("C3.8 motion QA PASS:", len(records), "clips, 6 visual strips")


if __name__ == "__main__":
    main()
