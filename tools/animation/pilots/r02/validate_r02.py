#!/usr/bin/env python3
"""Validate R02 reference-first motion spec without Blender dependencies."""
from __future__ import annotations

import json
import math
from pathlib import Path

SPEC = Path(__file__).with_name("sword_slash_r02.json")
REQUIRED_PHASES = ("ready", "load", "windup", "high_guard", "acceleration", "contact", "follow_through", "brake", "recovery", "ready_end")
VECTORS = ("pelvis_euler_deg", "spine_euler_deg", "neck_euler_deg", "head_euler_deg", "upper_arm_L_deg", "forearm_L_deg",
           "right_hand_target", "sword_tip_direction", "left_ankle_target")


def validate(data: dict) -> dict:
    if data.get("schema_version") != 2 or data.get("id") != "mkm:sword_slash_r02_authoring" or data.get("source_only") is not True:
        raise ValueError("Wrong source-only R02 schema")
    fps = data.get("fps")
    first = data.get("frame_start")
    last = data.get("frame_end")
    duration = data.get("action_ticks")
    contact = data.get("visual_contact_tick")
    contact_frame = data.get("contact_frame")
    if any(type(x) is not int for x in (fps, first, last, duration, contact, contact_frame)):
        raise ValueError("Frame/tick values must be integers")
    if (fps, first, last, duration, contact, contact_frame) != (60, 1, 43, 14, 5, 16):
        raise ValueError("R02 14-tick reference timing has been changed")
    if (last - first) * 20 != fps * duration or (contact_frame - first) * 20 != fps * contact:
        raise ValueError("Blender timeline differs from server reference")
    if data.get("mainhand") != "RIGHT" or data.get("weapon_family") != "one_handed_sword":
        raise ValueError("R02 is only a right-hand sword pilot")
    refs = data.get("source_refs")
    if not isinstance(refs, list) or len(refs) < 3:
        raise ValueError("External and project references required")
    keys = data.get("keys")
    if not isinstance(keys, list) or [x.get("phase") for x in keys] != list(REQUIRED_PHASES):
        raise ValueError("Incomplete or misordered choreography")
    frames = [x.get("frame") for x in keys]
    if any(type(x) is not int for x in frames) or frames != sorted(set(frames)):
        raise ValueError("Keys out of order or duplicated")
    if (frames[0], frames[-1]) != (first, last) or contact_frame not in frames:
        raise ValueError("Missing start/end/contact frames")
    for key in keys:
        if set(VECTORS) - set(key):
            raise ValueError("Missing a declared channel")
        for name in VECTORS:
            val = key[name]
            if not isinstance(val, list) or len(val) != 3:
                raise ValueError(f"Bad 3D vector for {name}")
            if any(type(v) not in (float, int) or not math.isfinite(v) for v in val):
                raise ValueError(f"Nonfinite {name}")
            if name.endswith("euler_deg") or name.endswith("_L_deg"):
                if any(abs(v) > 65 for v in val):
                    raise ValueError(f"Excessive body rotation for {name}")
            if name == "sword_tip_direction":
                mag = math.sqrt(sum(v*v for v in val))
                if not 0.80 < mag < 1.20:
                    raise ValueError("Sword tip direction is not normalized")
            if name in ("right_hand_target", "left_ankle_target") and any(abs(v) > 2.5 for v in val):
                raise ValueError("Out-of-bounds IK target")
    for name in VECTORS:
        if keys[0][name] != keys[-1][name]:
            raise ValueError(f"Animation does not return to its initial stance: {name}")
    sample = data.get("review_frames")
    if not isinstance(sample, list) or len(sample) < 9 or sample != sorted(set(sample)) or sample[0] != first or sample[-1] != last or contact_frame not in sample:
        raise ValueError("Need >=9 ordered contact-inclusive visual samples")
    if any(type(f) is not int or f < first or f > last for f in sample):
        raise ValueError("Review frame outside duration")
    return data


def load(path: Path = SPEC) -> dict:
    return validate(json.loads(path.read_text(encoding="utf-8")))


if __name__ == "__main__":
    x = load()
    print(f"R02 SPEC PASS: {x['id']}, 60FPS, 14 ticks, contact tick 5/frame 16, {len(x['keys'])} phases")
