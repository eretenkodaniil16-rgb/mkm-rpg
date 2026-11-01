#!/usr/bin/env python3
"""Render controlled Blender proxy frames from an MKM animation JSON clip.

Example:
 blender -b --python tools/animation/blender/render_clip_review.py -- \
   --clip sword/light_1 --output-dir build/reports/blender-review --frames 9

A six-part proxy only. Not game footage, not a source of gameplay authority,
not actual MC armor/skin/hand grip, and not IK/contact-aware animation.
"""
import argparse
import math
import sys
from pathlib import Path

try:
    import bpy
    from mathutils import Vector
except ImportError as exc:
    raise SystemExit("Run through Blender with bpy installed") from exc

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools/animation/qa"))
from biomechanics_audit import read_clip, pose, validate  # noqa: E402


def args():
    argv = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument("--clip", default="sword/light_1")
    p.add_argument("--output-dir", default="build/reports/blender-review")
    p.add_argument("--frames", type=int, default=9)
    p.add_argument("--size", type=int, default=480)
    return p.parse_args(argv)


def cube(name, location, dimensions, material, parent=None):
    bpy.ops.mesh.primitive_cube_add(size=1)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.location = location
    if parent:
        obj.parent = parent
    obj.data.materials.append(material)
    return obj


def pivot(name, location, parent=None):
    obj = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    if parent:
        obj.parent = parent
    return obj


def material(name, rgb):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*rgb, 1)
    return m


def aim(obj, target):
    direction = Vector(target) - obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def rig():
    skin = material("mat_cloth", (.32, .47, .65))
    left = material("mat_left_limb", (.13, .62, .69))
    right = material("mat_right_limb", (.92, .56, .27))
    ground = material("mat_floor", (.3, .32, .33))
    root = pivot("model_origin", (0, 0, 0))
    torso = pivot("body", (0, 0, 1.36), root)
    cube("chest", (0, 0, -.02), (.49, .28, .70), skin, torso)
    head = pivot("head", (0, 0, .43), torso)
    cube("head_cube", (0, 0, .14), (.49, .47, .49), skin, head)
    arms = {}
    for name, side, mat in (("left_arm", -1, left), ("right_arm", 1, right)):
        arm = pivot(name, (side*.36, 0, .28), torso)
        cube(name+"_cube", (0, 0, -.33), (.23, .23, .65), mat, arm)
        arms[name] = arm
    legs = {}
    for name, side, mat in (("left_leg", -1, left), ("right_leg", 1, right)):
        leg = pivot(name, (side*.13, 0, .87), root)
        cube(name+"_cube", (0, 0, -.43), (.23, .28, .85), mat, leg)
        legs[name] = leg
    cube("floor", (0, 0, -.06), (3.0, 3.0, .10), ground)
    return {"head": head, "body": torso, **arms, **legs}


def main():
    opt = args()
    if not 3 <= opt.frames <= 120 or not 160 <= opt.size <= 2048:
        raise SystemExit("Frame or render dimension out of range")
    clip = read_clip(opt.clip)
    validate(clip, opt.clip)
    output = Path(opt.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    parts = rig()
    bpy.ops.object.camera_add(location=(2.85, -4.8, 2.5))
    camera = bpy.context.object
    aim(camera, (0, 0, 1.0))
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 3.3
    bpy.context.scene.camera = camera
    bpy.ops.object.light_add(type="AREA", location=(1.0, -3.0, 4.0))
    bpy.context.object.data.energy = 900
    bpy.context.object.data.shape = "DISK"
    bpy.context.object.data.size = 5
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 20
    scene.render.resolution_x = opt.size
    scene.render.resolution_y = opt.size
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.world.color = (.08, .09, .11)
    # Vanilla/Blender coordinate conventions differ. This preview provides
    # repeatable BODY-shape comparisons, not production retarget validation.
    for i in range(opt.frames):
        t = i/(opt.frames-1)
        values = pose(clip, t)
        for name, obj in parts.items():
            e = values.get(name, (0.0, 0.0, 0.0))
            obj.rotation_euler = (e[0], e[2], e[1])
        scene.render.filepath = str((output / f'{opt.clip.replace("/", "_")}_{i:03d}.png').resolve())
        bpy.ops.render.render(write_still=True)
    print("MKM Blender proxy rendered:", opt.clip, opt.frames, "frames in", output)


if __name__ == "__main__":
    main()
