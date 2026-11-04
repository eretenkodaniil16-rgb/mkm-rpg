#!/usr/bin/env python3
"""Original MKM R02 Blender source animation.

Blender 4.x:
 blender -b --factory-startup --python tools/animation/pilots/r02/blender_sword_slash_r02.py -- --output-dir build/reports/sword-r02 --render

Produces editable .blend, 43 continuous frames, 9+ review frames, JSON manifest.
No MNAE/gameplay runtime clip; reference-only contact at tick 5 of 14.
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
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent))
from validate_r02 import SPEC, load
import blender_sword_slash_r01 as base

REST_WRIST = (base.RIGHT_X, -0.04, 0.68)
HEAD_CENTER = base.HEAD_CENTER_REST
SWORD_LENGTH = 0.62


def options():
    argv = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--output-dir", type=Path, default=Path("build/reports/sword-r02"))
    ap.add_argument("--spec", type=Path, default=SPEC)
    ap.add_argument("--render", action="store_true")
    return ap.parse_args(argv)


def lerp_keys(keys, frame, channel):
    if frame <= keys[0]["frame"]:
        return Vector(keys[0][channel])
    if frame >= keys[-1]["frame"]:
        return Vector(keys[-1][channel])
    for a, b in zip(keys, keys[1:]):
        if a["frame"] <= frame <= b["frame"]:
            t = (frame - a["frame"])/(b["frame"]-a["frame"])
            s = t*t*(3-2*t)
            return Vector(a[channel]).lerp(Vector(b[channel]), s)
    raise RuntimeError("Missing interpolating key")


def slerp_sword_direction(keys, frame):
    # World-space **direction**, not Euler and not componentwise lerp.
    # Linear normalization of nearly opposite vectors produced >0.7m
    # one-frame blade-tip teleports in R02 first pass. Spherical interpolation
    # preserves a continuous, shortest path of the blade direction.
    if frame <= keys[0]["frame"]:
        return Vector(keys[0]["sword_tip_direction"]).normalized()
    if frame >= keys[-1]["frame"]:
        return Vector(keys[-1]["sword_tip_direction"]).normalized()
    for a, b in zip(keys, keys[1:]):
        if a["frame"] <= frame <= b["frame"]:
            alpha = (frame-a["frame"])/(b["frame"]-a["frame"])
            t = alpha*alpha*(3-2*alpha)
            left=Vector(a["sword_tip_direction"]).normalized()
            right=Vector(b["sword_tip_direction"]).normalized()
            dot=max(-1.0,min(1.0,left.dot(right)))
            theta=math.acos(dot)
            if theta<1.0e-5:
                return left.lerp(right,t).normalized()
            denom=math.sin(theta)
            return (left*(math.sin((1-t)*theta)/denom)
                    + right*(math.sin(t*theta)/denom)).normalized()
    raise RuntimeError("Missing sword direction key interval")


def create_ik_targets(rig):
    right = bpy.data.objects.new("R02_RIGHT_WRIST_TARGET", None)
    bpy.context.scene.collection.objects.link(right)
    right.empty_display_type = "SPHERE"
    right.empty_display_size = 0.055
    right.hide_render = True
    ik = rig.pose.bones["forearm.R"].constraints.new("IK")
    ik.target = right
    ik.chain_count = 2
    ik.iterations = 96
    ik.use_stretch = False
    left_ankle = bpy.data.objects.get("IK_ANKLE_TARGET_L")
    if left_ankle is None:
        raise RuntimeError("Missing foot IK target; R01 foundation changed")
    return right, left_ankle


def make_sword_socket_and_mesh(mats):
    # Old R01 blade is weighted to hand.R, delete only it, not the character rig.
    for name in ("Sword_Handle", "Sword_Guard", "Sword_Blade", "Sword_Point"):
        obj = bpy.data.objects.get(name)
        if obj is None:
            raise RuntimeError("R01 source sword component missing: " + name)
        bpy.data.objects.remove(obj, do_unlink=True)
    socket = bpy.data.objects.new("R02_RIGHT_HAND_SWORD_GRIP_SOCKET", None)
    bpy.context.scene.collection.objects.link(socket)
    socket.rotation_mode = "QUATERNION"
    socket.empty_display_size = .055
    socket.empty_display_type = "ARROWS"
    socket.hide_render = True

    # Mesh local -Z is blade direction. All elements rigidly parent to ONE
    # world-oriented grip socket so no per-part tip/handle separation occurs.
    parts = [
        ("R02_Sword_Handle", (0,0,0.005), (.07,.07,.18), mats["boots"]),
        ("R02_Sword_Pommel", (0,0,.10), (.10,.10,.055), mats["metal"]),
        ("R02_Sword_Guard", (0,0,-.105), (.25,.065,.048), mats["metal"]),
        ("R02_Sword_Blade", (0,0,-.335), (.102,.045,.42), mats["metal"]),
        ("R02_Sword_Point", (0,0,-.555), (.065,.035,.09), mats["metal"]),
    ]
    for name, center, size, mat in parts:
        mesh = base.cuboid(name, center, size, mat)
        mesh.parent = socket
        mesh["rig_socket"] = socket.name
    return socket


def apply_keyframes(rig, spec, right, ankle):
    scene = bpy.context.scene
    scene.frame_start = spec["frame_start"]
    scene.frame_end = spec["frame_end"]
    scene.render.fps = spec["fps"]
    controls = {
        "pelvis":"pelvis_euler_deg",
        "spine":"spine_euler_deg",
        "neck":"neck_euler_deg",
        "head":"head_euler_deg",
        "upper_arm.L":"upper_arm_L_deg",
        "forearm.L":"forearm_L_deg",
    }
    for key in spec["keys"]:
        frame = key["frame"]
        scene.frame_set(frame)
        for bone, channel in controls.items():
            pb = rig.pose.bones[bone]
            pb.rotation_euler = [math.radians(v) for v in key[channel]]
            pb.keyframe_insert(data_path="rotation_euler", frame=frame, group=bone)
        right.location = key["right_hand_target"]
        right.keyframe_insert(data_path="location", frame=frame)
        ankle.location = key["left_ankle_target"]
        ankle.keyframe_insert(data_path="location", frame=frame)
    if rig.animation_data is None or rig.animation_data.action is None:
        raise RuntimeError("No source action was created")
    rig.animation_data.action.name = "MKM_SWORD_SLASH_R02_SOURCE"
    scene.timeline_markers.new("VISUAL_CONTACT_TICK_5_NOT_DAMAGE", frame=spec["contact_frame"])
    # AUTO_CLAMPED prevents overshoot between deliberately controlled key poses.
    actions = (rig.animation_data.action, right.animation_data.action, ankle.animation_data.action)
    for action in actions:
        if action is None:
            continue
        for curve in getattr(action, "fcurves", ()):
            for point in curve.keyframe_points:
                point.interpolation = "BEZIER"
                point.handle_left_type = "AUTO_CLAMPED"
                point.handle_right_type = "AUTO_CLAMPED"


def bake_grip_socket(rig, socket, spec):
    scene = bpy.context.scene
    frames = range(spec["frame_start"], spec["frame_end"]+1)
    for f in frames:
        scene.frame_set(f)
        bpy.context.view_layer.update()
        wrist = base.point_on_bone(rig, "hand.R", REST_WRIST)
        desired = slerp_sword_direction(spec["keys"], f)
        socket.location = wrist
        socket.rotation_quaternion = Vector((0,0,-1)).rotation_difference(desired)
        socket.keyframe_insert(data_path="location",frame=f)
        socket.keyframe_insert(data_path="rotation_quaternion",frame=f)
    for curve in getattr(socket.animation_data.action, "fcurves", ()):
        for key in curve.keyframe_points:
            key.interpolation = "LINEAR"
    scene.frame_set(spec["frame_start"])


def source_audit(rig, socket, right, ankle, spec):
    scene = bpy.context.scene
    frames=[]
    max_grip_error=0.
    max_target_error=0.
    min_clearance=float("inf")
    max_right_toe_xy_drift=0.
    max_tip_jump=0.
    prev_tip=None
    initial_right_toe=None
    for f in range(spec["frame_start"],spec["frame_end"]+1):
        scene.frame_set(f)
        bpy.context.view_layer.update()
        wrist=base.point_on_bone(rig,"hand.R",REST_WRIST)
        grip=socket.matrix_world.translation.copy()
        direction=(socket.matrix_world.to_quaternion() @ Vector((0,0,-1))).normalized()
        tip=grip+direction*SWORD_LENGTH
        head=base.point_on_bone(rig,"head",HEAD_CENTER)
        # Conservative head half-extents (0.2275m each) approximated by sphere.
        distance=base.distance_to_segment(head,grip,tip)
        clearance=distance - .398 - .055
        toe=base.point_on_bone(rig,"foot.R",(0.13,-0.16,0.02))
        if initial_right_toe is None:
            initial_right_toe=toe.copy()
        drift=math.hypot(toe.x-initial_right_toe.x,toe.y-initial_right_toe.y)
        max_right_toe_xy_drift=max(max_right_toe_xy_drift,drift)
        max_grip_error=max(max_grip_error,(grip-wrist).length)
        max_target_error=max(max_target_error,(wrist-Vector(right.location)).length)
        min_clearance=min(min_clearance,clearance)
        if prev_tip is not None:
            max_tip_jump=max(max_tip_jump,(tip-prev_tip).length)
        prev_tip=tip
        frames.append({
          "frame":f,
          "wrist_xyz":[round(float(v),5) for v in wrist],
          "sword_tip_xyz":[round(float(v),5) for v in tip],
          "grip_error_m":round((grip-wrist).length,6),
          "wrist_target_error_m":round((wrist-Vector(right.location)).length,5),
          "head_surface_proxy_clearance_m":round(clearance,5),
          "right_toe_xy_drift_m":round(drift,5),
          "right_toe_height_m":round(float(toe.z),5)
        })
    return {
      "max_socket_wrist_drift_m":round(max_grip_error,6),
      "max_wrist_target_ik_error_m":round(max_target_error,5),
      "min_head_surface_proxy_clearance_m":round(min_clearance,5),
      "max_right_planted_toe_drift_xy_m":round(max_right_toe_xy_drift,5),
      "max_sword_tip_step_m_at_60fps":round(max_tip_jump,5),
      "source_pass_candidates":{
        "socket_wrist_drift":max_grip_error<=.015,
        "head_clearance_proxy":min_clearance>=.04,
        "right_toe_plant":max_right_toe_xy_drift<=.08,
        "tip_speed_bound":max_tip_jump<=.30
      },
      "frames":frames,
      "limitations":"Sphere head approximation and fixed-frame samples; actual mesh and camera occlusion require visual audit. Visual contact is NOT a server hit event."
    }


def main():
    args=options()
    spec=load(args.spec)
    out=args.output_dir.resolve()
    out.mkdir(parents=True,exist_ok=True)
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    rig=base.create_armature()
    base.add_foot_targets(rig)
    mats=base.build_character(rig)
    right,ankle=create_ik_targets(rig)
    socket=make_sword_socket_and_mesh(mats)
    apply_keyframes(rig,spec,right,ankle)
    bake_grip_socket(rig,socket,spec)
    scene=bpy.context.scene
    base.create_camera(scene)
    # A 3/4 camera keeps hand and blade trajectory visible against the voxel body.
    scene.camera.location=(2.9,-5.5,2.5)
    target=Vector((0.,-.06,.94))
    scene.camera.rotation_euler=(target-scene.camera.location).to_track_quat("-Z","Y").to_euler()
    scene.camera.data.ortho_scale=2.8
    scene.render.resolution_x=640
    scene.render.resolution_y=640
    scene.render.resolution_percentage=100
    scene.view_settings.view_transform="Standard"
    audit=source_audit(rig,socket,right,ankle,spec)
    scene.frame_set(spec["frame_start"])
    blend=out/"mkm_sword_slash_r02.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend))
    manifest={
        "source_only":"NOT_GAMEPLAY_OR_MNAE_ACCEPTED",
        "pilot":spec["id"],
        "reference_document":"docs/workflow/MKM_SWORD_SLASH_R02_MOTION_PLAN.md",
        "blender":bpy.app.version_string,
        "frame_range":[spec["frame_start"],spec["frame_end"]],
        "fps":spec["fps"],
        "logical_ticks":spec["action_ticks"],
        "visual_contact_tick":spec["visual_contact_tick"],
        "visual_contact_frame":spec["contact_frame"],
        "armature_bones":sorted(rig.data.bones.keys()),
        "review_frames":spec["review_frames"],
        "audit":audit,
        "note":"Check full continuous motion. Automated geometric proxies are not real Minecraft visual proof."
    }
    (out/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf8")
    if args.render:
        for f in range(spec["frame_start"],spec["frame_end"]+1):
            scene.frame_set(f)
            scene.render.filepath=str(out/f"frame_{f:03}.png")
            bpy.ops.render.render(write_still=True)
    print("R02 scene generated:",blend)
    print("Grip / head / planted foot audit:",{k:v for k,v in audit.items() if k!="frames"})
    print("AUTHORING ONLY. Do not ship in MNAE until Issue 43 and real game F5 QA.")


if __name__=="__main__":
    main()
