#!/usr/bin/env python3
"""Export a Blender Action to MKM v1 JSON keyframe resources.

Example (from repository root; run inside Blender's bundled Python):
  blender -b rig.blend --python tools/animation/export_blender_to_mkm.py -- \\
    --armature MKM_RIG --action sword_light_1 \\
    --clip-id mkm:sword/light_1 --duration-ticks 11 --layer upper_body \\
    --mapping tools/animation/mkm_humanoid_mapping.json \\
    --impact-tick 4 --output src/main/resources/assets/mkm/animations/sword/light_1.json

The rig must use additive rotations around the Minecraft-compatible bind pose.
This exporter intentionally rejects root translations: server movement is authoritative.
"""
import argparse
import json
import math
import sys
from pathlib import Path

try:
    import bpy
except ImportError as exc:
    raise SystemExit("This exporter must run using Blender's bundled Python (bpy).") from exc

def options():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser(description="Export validated MKM animation clip")
    ap.add_argument("--armature", required=True)
    ap.add_argument("--action", required=True)
    ap.add_argument("--clip-id", required=True)
    ap.add_argument("--duration-ticks", type=int, required=True)
    ap.add_argument("--layer", choices=("upper_body", "full_body"), required=True)
    ap.add_argument("--mapping", required=True)
    ap.add_argument("--output", required=True)
    ap.add_argument("--samples", type=int, default=32,
                    help="Number of uniformly sampled time intervals (2-128)")
    ap.add_argument("--impact-tick", type=int,
                    help="Server-authoritative hit tick measured from clip start")
    return ap.parse_args(argv)

def main():
    args = options()
    if not 2 <= args.duration_ticks <= 200 or not 2 <= args.samples <= 128:
        raise SystemExit("Duration or sample count outside C3 validated limits")
    if not args.clip_id.startswith("mkm:"):
        raise SystemExit("Clip ID must use mkm: namespace")
    rig = bpy.data.objects.get(args.armature)
    if rig is None or rig.type != "ARMATURE":
        raise SystemExit(f"Armature {args.armature!r} not present")
    action = bpy.data.actions.get(args.action)
    if action is None:
        raise SystemExit(f"Action {args.action!r} not present")
    mapping = json.loads(Path(args.mapping).read_text(encoding="utf-8"))
    bone_map = mapping.get("bones", {})
    if not bone_map:
        raise SystemExit("Mapping has no bone definitions")
    allowed = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}
    tracks = {}
    if args.layer == "upper_body" and any(b in bone_map for b in ("left_leg", "right_leg")):
        # Mapping file may contain all bones; skip legs for upper-body actions.
        bone_map = {k: v for k, v in bone_map.items() if k not in ("left_leg", "right_leg")}
    if not set(bone_map).issubset(allowed):
        raise SystemExit("Unknown target model bones")
    for name, rule in bone_map.items():
        if not rig.pose.bones.get(rule["source"]):
            raise SystemExit(f"Missing source bone {rule['source']} for {name}")
        axes = rule.get("axis_order", [0, 1, 2])
        signs = rule.get("axis_sign", [1, 1, 1])
        if sorted(axes) != [0, 1, 2] or len(signs) != 3 or any(s not in (-1, 1) for s in signs):
            raise SystemExit(f"Invalid axis retarget transform for {name}")
        tracks[name] = []

    if not rig.animation_data:
        rig.animation_data_create()
    old_action = rig.animation_data.action
    old_frame = bpy.context.scene.frame_current
    first_frame, last_frame = action.frame_range
    try:
        rig.animation_data.action = action
        for k in range(args.samples + 1):
            t = k / args.samples
            frame = float(first_frame + t * (last_frame - first_frame))
            integer = math.floor(frame)
            bpy.context.scene.frame_set(integer, subframe=frame - integer)
            for name, rule in bone_map.items():
                bone = rig.pose.bones[rule["source"]]
                # matrix_basis is the pose transform relative to the rest pose;
                # it is safer than exporting global/world-space Euler angles.
                angles = bone.matrix_basis.to_euler("XYZ")
                order = rule.get("axis_order", [0, 1, 2])
                sign = rule.get("axis_sign", [1, 1, 1])
                values = [round(float(angles[order[j]] * sign[j]), 6) for j in range(3)]
                if any(not math.isfinite(v) or abs(v) > math.tau for v in values):
                    raise SystemExit(f"Invalid rotation on bone {name} at frame {frame}")
                tracks[name].append({"t": round(t, 6), "r": values})
    finally:
        rig.animation_data.action = old_action
        bpy.context.scene.frame_set(old_frame)

    markers = []
    if args.impact_tick is not None:
        if not 0 <= args.impact_tick < args.duration_ticks:
            raise SystemExit("Impact tick must lie inside the action")
        markers.append({"name": "impact",
                        "t": round(args.impact_tick / args.duration_ticks, 6)})
    data = {"format": 1, "id": args.clip_id, "duration_ticks": args.duration_ticks,
            "layer": args.layer, "tracks": tracks, "markers": markers}
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"MKM animation exported: {path} ({len(tracks)} bones / {args.samples + 1} frames)")
    print("Review in-game. Rest-pose and axis calibration MUST be validated for each rig.")

if __name__ == "__main__":
    main()
