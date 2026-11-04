#!/usr/bin/env python3
"""Render R03 *static* compact guard on the same voxel source armature as R02.

blender -b --factory-startup --python tools/animation/pilots/r03/blender_r03_guard_study.py -- --render --output-dir build/reports/r03-guard

This produces ONE pose, three fixed camera views and an editable .blend. It does
not build R03 animation clips or introduce game data/Java changes.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[0]))
sys.path.insert(0, str(HERE.parents[1]))
import blender_sword_slash_r01 as base

SPEC_PATH = HERE / "compact_guard_spec.json"
REST_GRIP = Vector((base.RIGHT_X, -0.04, 0.64))
REST_WRIST = Vector((base.RIGHT_X, -0.04, 0.68))
REST_HEAD = Vector(base.HEAD_CENTER_REST)
REST_SHOULDER = Vector((base.RIGHT_X, 0, 1.27))


def cli():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--output-dir", type=Path, default=Path("build/reports/r03-guard"))
    ap.add_argument("--render", action="store_true")
    return ap.parse_args(args)


def angle(pose_bone, values):
    pose_bone.rotation_mode = "XYZ"
    pose_bone.rotation_euler = tuple(math.radians(x) for x in values)


def setup_pose(rig, spec):
    p = spec["pose"]
    angle(rig.pose.bones["pelvis"], p["pelvis_rotation_deg"])
    angle(rig.pose.bones["spine"], p["spine_rotation_deg"])
    angle(rig.pose.bones["head"], p["head_rotation_deg"])
    angle(rig.pose.bones["upper_arm.L"], p["left_arm_rotation_deg"])

    right = bpy.data.objects.new("R03_RIGHT_FOREARM_IK", None)
    bpy.context.scene.collection.objects.link(right)
    right.location = p["right_wrist_ik_target"]
    right.empty_display_type = "SPHERE"
    right.empty_display_size = .055
    right.hide_render = True

    constraint = rig.pose.bones["forearm.R"].constraints.new("IK")
    constraint.target = right
    constraint.chain_count = 2
    constraint.iterations = 96
    constraint.use_stretch = False

    for hand, target in (("R", "right_ankle_target"), ("L", "left_ankle_target")):
        obj = bpy.data.objects.get("IK_ANKLE_TARGET_" + hand)
        if obj is None:
            raise RuntimeError("Missing foot IK target from base rig")
        obj.location = p[target]
        obj.hide_render = True

    bpy.context.scene.frame_set(1)
    bpy.context.view_layer.update()
    return right


def replace_sword_with_rigid_hand_bind(rig, mats, spec):
    for name in ("Sword_Handle", "Sword_Guard", "Sword_Blade", "Sword_Point"):
        obj = bpy.data.objects.get(name)
        if obj is None:
            raise RuntimeError("Missing R01 sword: " + name)
        bpy.data.objects.remove(obj, do_unlink=True)

    desired = Vector(spec["pose"]["sword_tip_direction_world"]).normalized()
    hand = rig.pose.bones["hand.R"]
    # Rigid 100% weight hand-binding. Compute the rest-space blade axis ONE
    # time so the evaluated target pose matches the desired world direction.
    # No independently animated world-space sword rotation channel.
    deformation = hand.matrix @ hand.bone.matrix_local.inverted()
    armature_dir = rig.matrix_world.to_3x3().inverted() @ desired
    rest_dir = (deformation.to_3x3().inverted() @ armature_dir).normalized()
    rest_quat = Vector((0.0, 0.0, -1.0)).rotation_difference(rest_dir)

    pieces = (
        ("R03_Handle", (0, 0, .005), (.07, .07, .19), mats["boots"]),
        ("R03_Pommel", (0, 0, .115), (.10, .10, .06), mats["metal"]),
        ("R03_Guard", (0, 0, -.118), (.245, .07, .05), mats["metal"]),
        ("R03_Blade", (0, 0, -.363), (.102, .046, .445), mats["metal"]),
        ("R03_Point", (0, 0, -.592), (.065, .039, .065), mats["metal"]),
    )
    sword_parts = []
    for name, offset, size, material in pieces:
        obj = base.cuboid(name, (0, 0, 0), size, material, rig, "hand.R")
        relative = Vector(offset)
        for vertex in obj.data.vertices:
            vertex.co = REST_GRIP + rest_quat @ (vertex.co + relative)
        obj["MKM_BIND_MODE"] = "single_hand_bone_100_percent"
        sword_parts.append(name)

    bpy.context.view_layer.update()
    return desired, rest_dir, sword_parts


def pose_audit(rig, desired, rest_dir, spec):
    target = Vector(spec["pose"]["right_wrist_ik_target"])
    p = spec["pose"]
    wrist = base.point_on_bone(rig, "hand.R", REST_WRIST)
    grip = base.point_on_bone(rig, "hand.R", REST_GRIP)
    hand = rig.pose.bones["hand.R"]
    delta = hand.matrix @ hand.bone.matrix_local.inverted()
    actual = (rig.matrix_world.to_3x3() @ (delta.to_3x3() @ rest_dir)).normalized()
    tip = grip + actual * p["blade_length"]
    head = base.point_on_bone(rig, "head", REST_HEAD)
    clearance = base.distance_to_segment(head, grip, tip) - .398 - .055

    # Geometric reach of shoulder to wrist; a necessary but insufficient
    # source check of this 2-bone IK (upper arm length + forearm length).
    armature = rig.data.bones
    available = armature["upper_arm.R"].length + armature["forearm.R"].length
    shoulder = rig.matrix_world @ REST_SHOULDER
    target_dist = (target - shoulder).length
    reached = (wrist - target).length
    return {
        "visual_source_only": True,
        "frame": 1,
        "blend_rig_bones": len(rig.data.bones),
        "right_hand_actual_xyz": list(round(float(a), 6) for a in grip),
        "blade_tip_actual_xyz": list(round(float(a), 6) for a in tip),
        "blade_bind_direction_alignment_dot": round(actual.dot(desired), 7),
        "head_proxy_clearance_m": round(clearance, 6),
        "right_wrist_ik_error_m": round(reached, 6),
        "estimated_upper_and_lower_arm_length": round(available, 6),
        "shoulder_to_target_distance": round(target_dist, 6),
        "bone_local_sword_bind_no_world_animation": True,
        "source_geometry_preflight": {
            "guard_is_within_arm_reach": target_dist <= available,
            "blade_direction_matches_fixed_bone_bind": actual.dot(desired) >= .999,
            "sword_proxy_outside_head": clearance >= .05,
            "wrist_ik_target_reached": reached <= .08,
        },
        "review_warning": "A source geometry preflight is not a visual pose acceptance, anatomical wrist validation or Minecraft F5 pass."
    }


def create_camera(scene, name, position, target=(0, -.02, .99)):
    data = bpy.data.cameras.new(name)
    obj = bpy.data.objects.new(name, data)
    scene.collection.objects.link(obj)
    obj.location = position
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()
    data.type = "ORTHO"
    data.ortho_scale = 2.65
    return obj


def main():
    args = cli()
    spec = json.loads(SPEC_PATH.read_text(encoding="utf-8"))
    if spec.get("status") != "SOURCE_POSE_REVIEW_ONLY":
        raise ValueError("Wrong R03 static-pose status")
    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=True)

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    rig = base.create_armature()
    base.add_foot_targets(rig)
    mats = base.build_character(rig)
    setup_pose(rig, spec)
    desired, rest_dir, parts = replace_sword_with_rigid_hand_bind(rig, mats, spec)

    scene = bpy.context.scene
    base.create_camera(scene)
    scene.render.resolution_x = 720
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.frame_set(1)
    bpy.context.view_layer.update()
    report = {
        "spec_id": spec["id"],
        "authoring_only": True,
        "blender_version": bpy.app.version_string,
        "sword_parts": parts,
        "pose_audit": pose_audit(rig, desired, rest_dir, spec),
        "views": ["front", "right_side", "three_quarter"],
        "no_full_animation": True,
        "warning": "Static 3D visual signoff required before R03 animation."
    }
    (output / "manifest.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    if args.render:
        views = (
            ("front", (0, -4.8, 2.0)),
            ("right_side", (4.8, -.1, 1.8)),
            ("three_quarter", (3.4, -4.7, 2.6)),
        )
        for name, pos in views:
            scene.camera = create_camera(scene, "R03_" + name, pos)
            scene.render.filepath = str(output / ("r03_guard_" + name + ".png"))
            bpy.ops.render.render(write_still=True)
    scene.frame_set(1)
    scene.camera = bpy.data.objects.get("R03_three_quarter") or scene.camera
    bpy.ops.wm.save_as_mainfile(filepath=str(output / "mkm_r03_compact_guard.blend"))
    print("R03 guard:", json.dumps(report["pose_audit"]))
    print("Source-only 3-view guard study; no full animation or MNAE clip.")


if __name__ == "__main__":
    main()
