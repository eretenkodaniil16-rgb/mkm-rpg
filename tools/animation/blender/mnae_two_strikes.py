#!/usr/bin/env python3
"""MNAE-1 reproducible Blender authoring source for exactly two attack clips.

python3 tools/animation/blender/mnae_two_strikes.py --verify
blender -b --python tools/animation/blender/mnae_two_strikes.py -- --blender --render

The JSON fixtures are reproducible from these authored poses; Blender produces
an inspectable .blend file and optional pose review images when installed.
Rigs/frames are PROXY AUTHORING ASSETS, not actual Minecraft gameplay footage.
"""
import argparse
import json
import sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
FRAMES = [0,0.12,0.25,0.357143,0.52,0.78,1]
POSES = {"sword":{"body":{"r":[[0,0,0],[0.03,-0.3,-0.08],[0.12,-0.52,-0.12],[-0.06,0.38,0.09],[-0.05,0.18,0.045],[0.015,-0.08,-0.02],[0,0,0]],"p":[[0,0,0],[0,-0.12,-0.12],[0,-0.24,-0.28],[0,-0.38,-0.56],[0,-0.12,-0.34],[0,0,-0.08],[0,0,0]]},"head":{"r":[[0,0,0],[-0.02,0.08,0],[-0.04,0.18,0.02],[0.015,-0.13,-0.03],[0.01,-0.04,-0.01],[0,0,0],[0,0,0]]},"right_arm":{"r":[[0,0,0],[-0.4,0.3,0.35],[-1.14,0.56,0.65],[-1.45,-0.38,-0.47],[-1.22,-0.53,-0.55],[-0.34,-0.1,-0.16],[0,0,0]],"p":[[0,0,0],[0,0,0],[0.12,-0.1,0.1],[0.18,-0.1,-0.18],[0.07,0,-0.1],[0,0,0],[0,0,0]]},"left_arm":{"r":[[0,0,0],[-0.35,-0.2,-0.18],[-0.67,-0.28,-0.32],[-0.9,0.12,0.15],[-0.6,0.08,0.1],[-0.2,0,0],[0,0,0]]},"left_leg":{"r":[[0,0,0],[0.06,0,-0.09],[0.19,0,-0.16],[0.24,0,-0.18],[0.12,0,-0.1],[0.025,0,-0.04],[0,0,0]]},"right_leg":{"r":[[0,0,0],[-0.07,0,0.09],[-0.16,0,0.13],[-0.23,0,0.17],[-0.08,0,0.09],[-0.015,0,0.03],[0,0,0]]}},"punch":{"body":{"r":[[0,0,0],[0.045,-0.21,-0.045],[0.09,-0.38,-0.07],[-0.055,0.29,0.07],[-0.035,0.2,0.04],[0.01,-0.05,-0.015],[0,0,0]],"p":[[0,0,0],[0,-0.1,-0.06],[0,-0.18,-0.16],[0,-0.3,-0.38],[0,-0.1,-0.18],[0,0,-0.06],[0,0,0]]},"head":{"r":[[0,0,0],[-0.015,0.07,0],[-0.04,0.11,0],[0.025,-0.09,0],[0,-0.045,0],[0,0,0],[0,0,0]]},"right_arm":{"r":[[0,0,0],[-0.52,0.27,0.17],[-0.95,0.2,0.1],[-1.4,-0.04,-0.055],[-1.25,-0.075,-0.065],[-0.45,0.06,0],[0,0,0]],"p":[[0,0,0],[0,0,0],[0.12,-0.06,-0.09],[0.3,-0.06,-0.42],[0.16,-0.03,-0.24],[0,0,0],[0,0,0]]},"left_arm":{"r":[[0,0,0],[-0.45,-0.08,-0.12],[-0.95,-0.15,-0.18],[-0.98,-0.14,-0.2],[-0.75,-0.1,-0.16],[-0.31,0,-0.07],[0,0,0]]},"left_leg":{"r":[[0,0,0],[0.055,0,-0.065],[0.15,0,-0.11],[0.22,0,-0.13],[0.12,0,-0.08],[0.035,0,-0.02],[0,0,0]]},"right_leg":{"r":[[0,0,0],[-0.06,0,0.07],[-0.16,0,0.13],[-0.22,0,0.15],[-0.1,0,0.09],[-0.025,0,0.02],[0,0,0]]}}}
ZERO = [0, 0, 0]

def make_asset(name, motion):
    clip_id = "mkm:mnae/" + ("sword_cut" if name == "sword" else "straight_punch")
    tracks = {}
    for bone, data in motion.items():
        tracks[bone] = [
            {"t":t, "p":data.get("p",[ZERO]*len(FRAMES))[i],
             "r":data["r"][i], "s":[1,1,1]} for i,t in enumerate(FRAMES)]
    return {"format":2, "id":clip_id, "duration_ticks":14,
            "units":"model_pixels", "rotation":"xyz_radians",
            "tracks":tracks, "markers":[{"name":"impact","t":.357143},
                                      {"name":"follow_through","t":.52}]}

def output_paths():
    return {name:ROOT/"src/main/resources/assets/mkm/mnae"/
            ("sword_cut.json" if name == "sword" else "straight_punch.json")
            for name in POSES}

def check_or_write(write):
    for name, path in output_paths().items():
        raw = json.dumps(make_asset(name,POSES[name]),indent=2)+"\n"
        if write:
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(raw,encoding="utf-8")
        elif path.read_text(encoding="utf-8") != raw:
            raise SystemExit("MNAE JSON differs from Blender source: "+str(path))
    print("MNAE-1 two authored clips: OK")

def create_blender_source(render):
    import bpy
    from mathutils import Vector
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    bpy.ops.object.armature_add(enter_editmode=True)
    rig=bpy.context.object
    rig.name="MNAE_six_part_proxy"
    arm=rig.data
    for bone in list(arm.edit_bones):
        arm.edit_bones.remove(bone)
    # Hierarchical Blender source rig; model-part final bridge is still six bones.
    placements={
        "body":((0,0,1.25),(0,0,1.7),None),
        "head":((0,0,1.7),(0,0,2.1),"body"),
        "right_arm":((.30,0,1.55),(.30,0,.98),"body"),
        "left_arm":((-.30,0,1.55),(-.30,0,.98),"body"),
        "right_leg":((.14,0,.91),(.14,0,.13),None),
        "left_leg":((-.14,0,.91),(-.14,0,.13),None)}
    for bone,(head,tail,parent) in placements.items():
        b=arm.edit_bones.new(bone);b.head=head;b.tail=tail
    for bone,(_,_,parent) in placements.items():
        if parent: arm.edit_bones[bone].parent=arm.edit_bones[parent]
    bpy.ops.object.mode_set(mode='OBJECT')

    # Visible six-part Minecraft-like mesh: bone constraints keep each cube
    # attached to the editable armature so PNG renders are not empty.
    palette={
        "body":(.23,.34,.46,1),"head":(.76,.70,.61,1),
        "left_arm":(.17,.60,.62,1),"right_arm":(.79,.40,.25,1),
        "left_leg":(.17,.60,.62,1),"right_leg":(.79,.40,.25,1)}
    for bone,(head,tail,_) in placements.items():
        length=(Vector(tail)-Vector(head)).length
        bpy.ops.mesh.primitive_cube_add(size=1)
        obj=bpy.context.object
        obj.name="VISIBLE_"+bone
        obj.dimensions=(.46 if bone in ("body","head") else .23,
                        length, .34 if bone in ("body","head") else .23)
        bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
        # Bone local Y runs head-to-tail; center the cube along that segment.
        for vertex in obj.data.vertices:
            vertex.co.y += length/2
        material=bpy.data.materials.new(name="MNAE_MAT_"+bone)
        material.diffuse_color=palette[bone]
        obj.data.materials.append(material)
        follow=obj.constraints.new(type='COPY_TRANSFORMS')
        follow.target=rig
        follow.subtarget=bone

    # Ground, camera and lighting are saved INSIDE the editable .blend.
    bpy.ops.mesh.primitive_plane_add(size=4,location=(0,0,.08))
    floor=bpy.context.object
    floor.name="GROUND_REFERENCE_NOT_COLLISION"
    mat=bpy.data.materials.new(name="MNAE_FLOOR")
    mat.diffuse_color=(.12,.14,.18,1)
    floor.data.materials.append(mat)
    bpy.ops.object.camera_add(location=(3,-4,2.2))
    cam=bpy.context.object
    direction=Vector((0,0,1.0))-cam.location
    cam.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
    cam.data.type="ORTHO"
    cam.data.ortho_scale=3.5
    bpy.context.scene.camera=cam
    bpy.ops.object.light_add(type="AREA",location=(2,-3,4))
    bpy.context.object.data.energy=900
    bpy.context.object.data.size=5
    for name,motion in POSES.items():
        act=bpy.data.actions.new("MNAE_"+name)
        rig.animation_data_create()
        # Legacy Blender API authoring keys; sample at 60fps review frames.
        rig.animation_data.action=act
        # Blender 4.4+ uses slotted actions. Assign the slot explicitly so a
        # saved/loaded .blend animates even if slot auto-detection would fail.
        if bpy.app.version >= (4,4,0):
            slot=act.slots.new(id_type='OBJECT',name=rig.name)
            rig.animation_data.action_slot=slot
        for i,t in enumerate(FRAMES):
            bpy.context.scene.frame_set(1+int(round(t*60)))
            for bone,channels in motion.items():
                pb=rig.pose.bones[bone]
                pb.rotation_mode="XYZ"
                # The test model is an armature; coordinate calibration for
                # Minecraft model parts is explicitly a separate acceptance gate.
                pb.rotation_euler=channels["r"][i]
                pb.keyframe_insert(data_path="rotation_euler",group=bone)
                if "p" in channels:
                    p=channels["p"][i]
                    pb.location=Vector((p[0],-p[2],-p[1]))/16
                    pb.keyframe_insert(data_path="location",group=bone)
        if bpy.app.version >= (5,0,0):
            curves=act.layers[0].strips[0].channelbag(act.slots[0]).fcurves
        else:
            curves=act.fcurves
        for fc in curves:
            for key in fc.keyframe_points:
                key.interpolation="BEZIER"
    saved=ROOT/"tools/animation/blender/mnae_m1_source.blend"
    saved.parent.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(saved))
    if render:
        # Real Blender frames of our editable six-cube proxy, not Minecraft.
        scene=bpy.context.scene
        scene.render.engine="CYCLES";scene.cycles.samples=12
        scene.render.resolution_x=384;scene.render.resolution_y=384
        scene.render.resolution_percentage=100
        for name in POSES:
            action=bpy.data.actions["MNAE_"+name]
            rig.animation_data.action=action
            if bpy.app.version >= (4,4,0):
                rig.animation_data.action_slot=action.slots[0]
            for k,t in enumerate([0,.25,.357143,.52,1]):
                scene.frame_set(1+int(t*60))
                scene.render.filepath=str((ROOT/"build/reports/mnae-blender"/f"{name}_{k}.png").resolve())
                Path(scene.render.filepath).parent.mkdir(parents=True,exist_ok=True)
                bpy.ops.render.render(write_still=True)
    print("Blender source written",saved)

def main():
    argv=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else sys.argv[1:]
    ap=argparse.ArgumentParser()
    ap.add_argument("--verify",action="store_true")
    ap.add_argument("--write",action="store_true")
    ap.add_argument("--blender",action="store_true")
    ap.add_argument("--render",action="store_true")
    args=ap.parse_args(argv)
    check_or_write(args.write)
    if args.blender:
        create_blender_source(args.render)

if __name__ == "__main__":
    main()
