#!/usr/bin/env python3
"""Validate the MKM sword animation authoring specification without Blender."""
from __future__ import annotations

import json
import math
from pathlib import Path

SPEC_PATH = Path(__file__).with_name("sword_slash_r01.json")


def validate_spec(data: dict) -> dict:
    if data.get("schema_version") != 1:
        raise ValueError("Unsupported pilot spec schema")
    if data.get("name") != "mkm:sword_slash_pilot_r01":
        raise ValueError("Unexpected pilot identifier")
    fps = data.get("fps")
    first, last = data.get("frame_start"), data.get("frame_end")
    duration, impact_tick = data.get("minecraft_action_ticks"), data.get("minecraft_impact_tick")
    impact_frame = data.get("impact_frame")
    for name, value in {
        "fps": fps, "frame_start": first, "frame_end": last,
        "minecraft_action_ticks": duration, "minecraft_impact_tick": impact_tick,
        "impact_frame": impact_frame,
    }.items():
        if type(value) is not int:
            raise ValueError(f"{name} must be an integer")
    if fps < 1 or fps > 120 or first < 1 or last <= first:
        raise ValueError("Invalid FPS or frame range")
    if duration < 2 or duration > 200 or impact_tick < 0 or impact_tick >= duration:
        raise ValueError("Invalid server action/impact ticks")
    if (last - first) * 20 != duration * fps:
        raise ValueError("Duration must exactly match the 20 TPS authoring reference")
    if (impact_frame - first) * 20 != impact_tick * fps:
        raise ValueError("Impact frame must exactly align with the declared tick")
    bones = data.get("animated_bones")
    if not isinstance(bones, list) or not bones or len(bones) != len(set(bones)):
        raise ValueError("Missing or duplicate animated bones")
    if any(not isinstance(b, str) or not b for b in bones):
        raise ValueError("Invalid bone name")
    poses = data.get("poses")
    if not isinstance(poses, list) or len(poses) < 6:
        raise ValueError("Too few pose phases")
    frames = [pose.get("frame") for pose in poses]
    if any(type(f) is not int for f in frames) or frames != sorted(set(frames)):
        raise ValueError("Pose frames must be unique and strictly increasing")
    if frames[0] != first or frames[-1] != last or impact_frame not in frames:
        raise ValueError("Idle or impact key pose is missing")
    if poses[0].get("degrees") or poses[-1].get("degrees"):
        raise ValueError("First and last poses must be neutral")
    phases = [pose.get("phase") for pose in poses]
    if len(phases) != len(set(phases)) or any(not phase for phase in phases):
        raise ValueError("Phase names must be unique and nonempty")
    for pose in poses:
        values = pose.get("degrees")
        if not isinstance(values, dict) or not set(values).issubset(bones):
            raise ValueError("Unknown or invalid animated bone mapping")
        for bone, angles in values.items():
            if not isinstance(angles, list) or len(angles) != 3:
                raise ValueError(f"{bone} must have three Euler angles")
            if any(type(a) not in (float, int) or not math.isfinite(a) or abs(a) > 120 for a in angles):
                raise ValueError(f"Unbounded or non-finite angle for {bone}")
    sample_frames = data.get("render_frames")
    if (not isinstance(sample_frames, list) or len(sample_frames) < 9
            or any(type(f) is not int for f in sample_frames)
            or sample_frames != sorted(set(sample_frames))
            or sample_frames[0] != first or sample_frames[-1] != last
            or impact_frame not in sample_frames):
        raise ValueError("Need nine or more ordered frames including boundaries and impact")
    if any(frame < first or frame > last for frame in sample_frames):
        raise ValueError("Render frame outside animation")
    return data


def load_spec(path: Path = SPEC_PATH) -> dict:
    return validate_spec(json.loads(path.read_text(encoding="utf-8")))


if __name__ == "__main__":
    result = load_spec()
    print(f"VALID: {result['name']} ({result['frame_start']}..{result['frame_end']} at "
          f"{result['fps']} FPS, contact frame {result['impact_frame']})")
