#!/usr/bin/env python3
"""Generate a voxel-style articulated Minecraft warrior and one sword strike.

Blender 4.x/5.x:
    blender -b --factory-startup --python tools/animation/pilots/blender_sword_slash_r01.py -- --output-dir build/reports/sword-pilot --render

Writes an editable .blend, JSON motion/contact manifest, and optionally nine PNGs.
AUTHORING ONLY: no shippable MKM/MNAE resource, no server-side gameplay changes.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

from validate_sword_pilot import SPEC_PATH, load_spec

ARMATURE_NAME = "MKM_PILOT_RIG"
ACTION_NAME = "MKM_SWORD_SLASH_R01"
RIGHT_X = 0.36
SWORD_GRIP_REST = (RIGHT_X, -0.09, 0.64)
SWORD_TIP_REST = (RIGHT_X, -0.09, 0.05)
HEAD_CENTER_REST = (0.0, -0.01, 1.575)


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec", type=Path, default=SPEC_PATH)
    parser.add_argument("--output-dir", type=Path, default=Path("build/reports/sword-pilot"))
    parser.add_argument("--render", action="store_true", help="Render nine Blender Workbench review frames")
    return parser.parse_args(argv)


def create_material(name, rgb):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*rgb, 1.0)
    return mat


def cuboid(name, center, size, material, rig=None, bone=None):
    cx, cy, cz = center
    sx, sy, sz = (v / 2 for v in size)
    vertices = [
        (cx - sx, cy - sy, cz - sz), (cx + sx, cy - sy, cz - sz),
        (cx + sx, cy + sy, cz - sz), (cx - sx, cy + sy, cz - sz),
        (cx - sx, cy - sy, cz + sz), (cx + sx, cy - sy, cz + sz),
        (cx + sx, cy + sy, cz + sz), (cx - sx, cy + sy, cz + sz),
    ]
    faces = [
        (3, 2, 1, 0), (4, 5, 6, 7),
        (0, 1, 5, 4), (1, 2, 6, 5),
        (2, 3, 7, 6), (3, 0, 4, 7),
    ]
    mesh = bpy.data.meshes.new(name + "_mesh")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.data.materials.append(material)
    if rig is not None:
        if not bone:
            raise ValueError("Skinned cuboid requires a bone")
        weights = obj.vertex_groups.new(name=bone)
        weights.add(list(range(len(vertices))), 1.0, "REPLACE")
        mod = obj.modifiers.new("MKM_Armature", "ARMATURE")
        mod.object = rig
        obj["mkm_source_bone"] = bone
    return obj


def create_armature():
    data = bpy.data.armatures.new(ARMATURE_NAME)
    rig = bpy.data.objects.new(ARMATURE_NAME, data)
    bpy.context.scene.collection.objects.link(rig)
    bpy.context.view_layer.objects.active = rig
    rig.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")

    def add(name, a, b, parent=None):
        eb = data.edit_bones.new(name)
        eb.head = a
        eb.tail = b
        if parent:
            eb.parent = data.edit_bones[parent]
            eb.use_connect = False
        return eb

    add("root", (0, 0, 0), (0, 0, 0.68))
    add("pelvis", (0, 0, 0.68), (0, 0, 0.78), "root")
    add("spine", (0, 0, 0.78), (0, 0, 1.35), "pelvis")
    add("neck", (0, 0, 1.35), (0, 0, 1.42), "spine")
    add("head", (0, 0, 1.42), (0, 0, 1.80), "neck")
    for label, side in (("R", 1), ("L", -1)):
        x = side * RIGHT_X
        leg_x = side * 0.13
        add("upper_arm." + label, (x, 0, 1.27), (x, -0.01, 0.96), "spine")
        add("forearm." + label, (x, -0.01, 0.96), (x, -0.04, 0.68), "upper_arm." + label)
        add("hand." + label, (x, -0.04, 0.68), (x, -0.04, 0.58), "forearm." + label)
        add("thigh." + label, (leg_x, 0, 0.68), (leg_x, -0.035, 0.37), "pelvis")
        add("shin." + label, (leg_x, -0.035, 0.37), (leg_x, -0.045, 0.07), "thigh." + label)
        add("foot." + label, (leg_x, -0.045, 0.07), (leg_x, -0.20, 0.07), "shin." + label)

    bpy.ops.object.mode_set(mode="OBJECT")
    data.display_type = "STICK"
    rig.show_in_front = True
    for pb in rig.pose.bones:
        pb.rotation_mode = "XYZ"
    return rig


def add_foot_targets(rig):
    # Two-bone IK keeps each ankle at its target; actual toe contact is audited below.
    for label, side in (("R", 1), ("L", -1)):
        target = bpy.data.objects.new("IK_ANKLE_TARGET_" + label, None)
        bpy.context.scene.collection.objects.link(target)
        target.location = (side * 0.13, -0.045, 0.07)
        target.empty_display_type = "SPHERE"
        target.empty_display_size = 0.07
        ik = rig.pose.bones["shin." + label].constraints.new("IK")
        ik.target = target
        ik.chain_count = 2
        ik.use_stretch = False
        ik.iterations = 48


def build_character(rig):
    mats = {
        "skin": create_material("Skin", (0.68, 0.49, 0.42)),
        "hair": create_material("Hair", (0.10, 0.075, 0.085)),
        "armor": create_material("Dark leather", (0.13, 0.12, 0.16)),
        "metal": create_material("Steel", (0.54, 0.58, 0.66)),
        "accent": create_material("Burgundy scarf", (0.34, 0.075, 0.13)),
        "boots": create_material("Boots", (0.10, 0.075, 0.055)),
        "eyes": create_material("Eyes", (0.025, 0.025, 0.025)),
        "floor": create_material("Reference floor", (0.31, 0.35, 0.37)),
    }

    def body(name, center, size, mat, bone):
        return cuboid(name, center, size, mats[mat], rig, bone)

    body("Pelvis", (0, 0, 0.73), (0.47, 0.26, 0.16), "armor", "pelvis")
    body("Chest", (0, 0, 1.07), (0.49, 0.28, 0.58), "armor", "spine")
    body("Scarf", (0, 0, 1.34), (0.53, 0.31, 0.085), "accent", "spine")
    body("Head", (0, 0, 1.575), (0.455, 0.455, 0.45), "skin", "head")
    body("HairCap", (0, 0, 1.772), (0.468, 0.47, 0.095), "hair", "head")
    for side in (-1, 1):
        label = "R" if side == 1 else "L"
        x = side * RIGHT_X
        lx = side * 0.13
        body("UpperArm_" + label, (x, 0, 1.11), (0.205, 0.235, 0.29), "armor", "upper_arm." + label)
        body("ShoulderPlate_" + label, (x, 0, 1.267), (0.245, 0.28, 0.11), "metal", "upper_arm." + label)
        body("Forearm_" + label, (x, -0.02, 0.82), (0.192, 0.225, 0.275), "armor", "forearm." + label)
        body("Hand_" + label, (x, -0.04, 0.64), (0.18, 0.20, 0.125), "skin", "hand." + label)
        body("Thigh_" + label, (lx, -0.02, 0.52), (0.205, 0.245, 0.32), "armor", "thigh." + label)
        body("Shin_" + label, (lx, -0.04, 0.22), (0.202, 0.23, 0.30), "armor", "shin." + label)
        body("Boot_" + label, (lx, -0.12, 0.072), (0.213, 0.30, 0.136), "boots", "foot." + label)

    body("Eye_R", (0.105, -0.233, 1.595), (0.060, 0.018, 0.049), "eyes", "head")
    body("Eye_L", (-0.105, -0.233, 1.595), (0.060, 0.018, 0.049), "eyes", "head")
    # Each sword mesh is rigidly weighted to hand.R; no independent transform drift.
    body("Sword_Handle", (RIGHT_X, -0.09, 0.635), (0.066, 0.065, 0.195), "boots", "hand.R")
    body("Sword_Guard", (RIGHT_X, -0.09, 0.535), (0.23, 0.065, 0.044), "metal", "hand.R")
    body("Sword_Blade", (RIGHT_X, -0.09, 0.293), (0.105, 0.038, 0.437), "metal", "hand.R")
    body("Sword_Point", (RIGHT_X, -0.09, 0.065), (0.065, 0.036, 0.035), "metal", "hand.R")
    cuboid("Ground", (0, 0, -0.05), (4, 4, 0.10), mats["floor"])
    return mats


def apply_animation(rig, spec):
    scene = bpy.context.scene
    scene.frame_start = spec["frame_start"]
    scene.frame_end = spec["frame_end"]
    scene.render.fps = spec["fps"]
    rig.animation_data_create()
    for item in spec["poses"]:
        scene.frame_set(item["frame"])
        for name in spec["animated_bones"]:
            pb = rig.pose.bones[name]
            pb.rotation_euler = [math.radians(v) for v in item["degrees"].get(name, (0, 0, 0))]
            pb.keyframe_insert(data_path="rotation_euler", frame=item["frame"], group=name)
    if not rig.animation_data.action:
        raise RuntimeError("Blender did not create an animation Action")
    rig.animation_data.action.name = ACTION_NAME
    scene.timeline_markers.new("VISUAL_CONTACT_REFERENCE_ONLY", frame=spec["impact_frame"])
    scene.frame_set(spec["frame_start"])


def create_camera(scene):
    cam_data = bpy.data.cameras.new("MKM_Review_Ortho")
    camera = bpy.data.objects.new("MKM_Review_Ortho", cam_data)
    scene.collection.objects.link(camera)
    camera.location = (3.1, -4.7, 2.7)
    target = Vector((0, 0, 0.91))
    camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = 2.65
    scene.camera = camera
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.render.resolution_x = 720
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.show_shadows = True
    scene.render.film_transparent = False


def point_on_bone(rig, bone, rest_point):
    pb = rig.pose.bones[bone]
    delta = pb.matrix @ pb.bone.matrix_local.inverted()
    return rig.matrix_world @ (delta @ Vector(rest_point))


def distance_to_segment(point, a, b):
    ab = b - a
    if ab.length_squared < 1e-10:
        return (point - a).length
    t = max(0.0, min(1.0, (point - a).dot(ab) / ab.length_squared))
    return (point - (a + t * ab)).length


def probe_frames(rig, spec):
    scene = bpy.context.scene
    snapshots = []
    for frame in spec["render_frames"]:
        scene.frame_set(frame)
        bpy.context.view_layer.update()
        hand = point_on_bone(rig, "hand.R", SWORD_GRIP_REST)
        tip = point_on_bone(rig, "hand.R", SWORD_TIP_REST)
        head = point_on_bone(rig, "head", HEAD_CENTER_REST)
        feet = {
            label: point_on_bone(rig, "foot." + label, (side * 0.13, -0.16, 0.02))
            for label, side in (("R", 1), ("L", -1))
        }
        snapshots.append({
            "frame": frame,
            "grip_xyz": [round(float(x), 5) for x in hand],
            "sword_tip_xyz": [round(float(x), 5) for x in tip],
            "head_proximity_m": round(distance_to_segment(head, hand, tip), 5),
            "feet_xyz": {label: [round(float(x), 5) for x in v] for label, v in feet.items()},
        })

    original = snapshots[0]["feet_xyz"]
    max_foot_drift = max(
        math.dist(snap["feet_xyz"][label][:2], original[label][:2])
        for snap in snapshots for label in ("R", "L")
    )
    return {
        "frames": snapshots,
        "max_sampled_foot_xy_drift_m": round(max_foot_drift, 5),
        "min_sampled_sword_to_head_center_m": min(snap["head_proximity_m"] for snap in snapshots),
        "warning": "Approximate contact markers, not verified surface collision or visual game-frame QA.",
    }


def main():
    args = parse_args()
    spec = load_spec(args.spec)
    out = args.output_dir.resolve()
    out.mkdir(parents=True, exist_ok=True)

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    rig = create_armature()
    add_foot_targets(rig)
    build_character(rig)
    apply_animation(rig, spec)
    create_camera(bpy.context.scene)

    qa = probe_frames(rig, spec)
    bpy.context.scene.frame_set(spec["frame_start"])
    blend_path = out / "mkm_sword_slash_r01.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_path))
    manifest = {
        "format": "MKM_AUTHORING_PILOT_1",
        "spec_name": spec["name"],
        "armature_name": ARMATURE_NAME,
        "action_name": ACTION_NAME,
        "blender_version": bpy.app.version_string,
        "fps": spec["fps"],
        "duration_frames": spec["frame_end"] - spec["frame_start"],
        "minecraft_action_ticks_reference": spec["minecraft_action_ticks"],
        "minecraft_impact_tick_reference": spec["minecraft_impact_tick"],
        "impact_frame": spec["impact_frame"],
        "rig_bones": sorted(rig.data.bones.keys()),
        "rendered_review_frames": spec["render_frames"] if args.render else [],
        "qa": qa,
        "shipping_status": "AUTHORING_ONLY_NOT_MNAE_COMPATIBLE",
        "notes": [
            "The 17-bone articulated source cannot be exported losslessly via the six-part legacy exporter.",
            "Foot-lock is an IK authoring aid, not a statement that gameplay collision is correct.",
            "Weapon handle and blade are both weighted to hand.R; this does not prove in-game item grip.",
            "This script never changes server damage, animation clip IDs, hitboxes or saved RPG data.",
        ],
    }
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

    if args.render:
        for frame in spec["render_frames"]:
            bpy.context.scene.frame_set(frame)
            bpy.context.scene.render.filepath = str(out / f"review_{frame:03}.png")
            bpy.ops.render.render(write_still=True)

    print("Generated Blender authoring pilot:", blend_path)
    print("Manifest:", out / "manifest.json")
    print("Source not accepted for MNAE/Minecraft gameplay until independent in-game QA.")


if __name__ == "__main__":
    main()
