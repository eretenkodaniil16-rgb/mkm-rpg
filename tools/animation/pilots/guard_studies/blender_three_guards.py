#!/usr/bin/env python3
"""MKM three sword guards: one Blender model, one hand-bound sword, nine views.
blender -b --factory-startup --python tools/animation/pilots/guard_studies/blender_three_guards.py -- --render
"""
from __future__ import annotations
import argparse, json, math, sys
from pathlib import Path
import bpy
from mathutils import Vector

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parent))
import blender_sword_slash_r01 as base

DATA=HERE/"first_three_stances_v1.json"
FRAMES=(1,20,40)
GRIP=Vector((base.RIGHT_X,-.04,.64))
WRIST=Vector((base.RIGHT_X,-.04,.68))
HEAD=Vector(base.HEAD_CENTER_REST)
LENGTH=.62


def controls(rig):
    base.add_foot_targets(rig)
    wrist=bpy.data.objects.new("TARGET_RIGHT_HAND",None)
    bpy.context.scene.collection.objects.link(wrist)
    wrist.empty_display_type="SPHERE"
    wrist.hide_render=True
    c=rig.pose.bones["forearm.R"].constraints.new("IK")
    c.target=wrist
    c.chain_count=2
    c.use_stretch=False
    c.iterations=96
    feet={}
    for side in ("L","R"):
        feet[side]=bpy.data.objects["IK_ANKLE_TARGET_"+side]
        feet[side].hide_render=True
        knee=bpy.data.objects.new("POLE_KNEE_"+side,None)
        bpy.context.scene.collection.objects.link(knee)
        knee.location=(.13 if side=="R" else -.13,-.55,.37)
        knee.hide_render=True
        ik=next(x for x in rig.pose.bones["shin."+side].constraints if x.type=="IK")
        ik.pole_target=knee
        ik.pole_angle=0.
        ik.iterations=96
        ik.use_stretch=False
    return wrist,feet


def set_rot(pb,xyz):
    pb.rotation_mode="XYZ"
    pb.rotation_euler=[math.radians(v) for v in xyz]


def set_stance(rig,wrist,feet,p,rest_dir=None):
    t=p["pose_targets"]
    pelvis=rig.pose.bones["pelvis"]
    pelvis.location=(0.,0.,-t["pelvis_drop_scene"])
    set_rot(pelvis,(0.,t["pelvis_yaw_deg"],0.))
    set_rot(rig.pose.bones["spine"],(t["trunk_forward_incline_deg"],t["spine_relative_yaw_deg"],0.))
    set_rot(rig.pose.bones["neck"],(0.,-t["spine_relative_yaw_deg"]*.25,0.))
    set_rot(rig.pose.bones["head"],(0.,-t["spine_relative_yaw_deg"]*.3,0.))
    set_rot(rig.pose.bones["upper_arm.L"],(-10.,0.,0.))
    set_rot(rig.pose.bones["forearm.L"],(-15.,0.,0.))
    wrist.location=t["wrist_xyz"]
    feet["L"].location=t["left_ankle_xyz"]
    feet["R"].location=t["right_ankle_xyz"]
    hand=rig.pose.bones["hand.R"]
    hand.rotation_mode="QUATERNION"
    hand.rotation_quaternion=(1.,0.,0.,0.)
    bpy.context.view_layer.update()
    desire=Vector(t["blade_direction_world"]).normalized()
    delta=hand.matrix @ hand.bone.matrix_local.inverted()
    if rest_dir is None:
        rest_dir=(delta.to_3x3().inverted() @ (rig.matrix_world.to_3x3().inverted() @ desire)).normalized()
    else:
        # Rotate the ACTUAL wrist pose, not a world-space sword transform.
        current=(rig.matrix_world.to_3x3() @ (delta.to_3x3() @ rest_dir)).normalized()
        q_change=current.rotation_difference(desire)
        world_q=(rig.matrix_world @ hand.matrix).to_quaternion()
        hand.rotation_quaternion=world_q.inverted() @ q_change @ world_q
        bpy.context.view_layer.update()
    return rest_dir


def bind_sword(rig,mats,rest_dir):
    for n in ("Sword_Handle","Sword_Guard","Sword_Blade","Sword_Point"):
        bpy.data.objects.remove(bpy.data.objects[n],do_unlink=True)
    rot=Vector((0,0,-1)).rotation_difference(rest_dir)
    spec=[
        ("MKM_SWORD_HANDLE",(0,0,.006),(.07,.07,.19),mats["boots"]),
        ("MKM_SWORD_POMMEL",(0,0,.11),(.10,.10,.055),mats["metal"]),
        ("MKM_SWORD_GUARD",(0,0,-.12),(.25,.07,.05),mats["metal"]),
        ("MKM_SWORD_BLADE",(0,0,-.35),(.105,.04,.43),mats["metal"]),
        ("MKM_SWORD_TIP",(0,0,-.58),(.06,.04,.08),mats["metal"]),
    ]
    out=[]
    for name,pos,size,mat in spec:
        obj=base.cuboid(name,(0,0,0),size,mat,rig,"hand.R")
        for v in obj.data.vertices:
            v.co=GRIP + rot @ (v.co+Vector(pos))
        obj["bind"]="bone_local:hand.R"
        out.append(obj)
    return out


def mark_key(rig,wrist,feet,frame,id):
    for name in ("pelvis","spine","neck","head","upper_arm.L","forearm.L"):
        pb=rig.pose.bones[name]
        pb.keyframe_insert(data_path="rotation_euler",frame=frame,group=name)
    rig.pose.bones["pelvis"].keyframe_insert(data_path="location",frame=frame,group="pelvis")
    rig.pose.bones["hand.R"].keyframe_insert(data_path="rotation_quaternion",frame=frame,group="hand.R")
    for obj in (wrist,feet["L"],feet["R"]):
        obj.keyframe_insert(data_path="location",frame=frame)
    bpy.context.scene.timeline_markers.new(id,frame=frame)


def constant_keys(obj):
    if not obj.animation_data or not obj.animation_data.action:
        return
    for fc in getattr(obj.animation_data.action,"fcurves",()):
        for point in fc.keyframe_points:
            point.interpolation="CONSTANT"


def vec(rig,bone,p):
    return base.point_on_bone(rig,bone,p)


def knee(rig,side):
    x=.13 if side=="R" else -.13
    hip=vec(rig,"thigh."+side,(x,0.,.68))
    k=vec(rig,"shin."+side,(x,-.035,.37))
    ankle=vec(rig,"shin."+side,(x,-.045,.07))
    return round(180-math.degrees((hip-k).angle(ankle-k)),2)


def audit(rig,wrist,feet,stance,rest_dir,objs):
    p=stance["pose_targets"]
    grip=vec(rig,"hand.R",GRIP)
    w=vec(rig,"hand.R",WRIST)
    hand=rig.pose.bones["hand.R"]
    deform=hand.matrix @ hand.bone.matrix_local.inverted()
    direction=(rig.matrix_world.to_3x3() @ (deform.to_3x3() @ rest_dir)).normalized()
    desired=Vector(p["blade_direction_world"]).normalized()
    tip=grip+LENGTH*direction
    head=vec(rig,"head",HEAD)
    clearance=base.distance_to_segment(head,grip,tip)-.398-.055
    bends={s:knee(rig,s) for s in ("L","R")}
    toe={}
    for side in ("L","R"):
        x=.13 if side=="R" else -.13
        toe[side]=[round(float(a),5) for a in vec(rig,"foot."+side,(x,-.16,.02))]
    result={
        "id":stance["id"],"frame":FRAMES[stance["index"]-1],
        "wrist_ik_error":round((w-wrist.location).length,5),
        "sword_axis_dot":round(direction.dot(desired),6),
        "sword_head_proxy_clearance":round(clearance,5),
        "sword_tip_xyz":[round(float(a),5) for a in tip],
        "knee_flexion_deg":bends,
        "toe_xyz":toe,
        "sword_single_hand_bone":all(len(o.vertex_groups)==1 and o.vertex_groups[0].name=="hand.R" for o in objs),
        "knee_target_ranges":{"L":p["left_knee_flexion_deg_range"],"R":p["right_knee_flexion_deg_range"]},
    }
    result["checks"]={
        "wrist_reached":result["wrist_ik_error"]<.05,
        "sword_follows_hand":result["sword_axis_dot"]>.995 and result["sword_single_hand_bone"],
        "head_proxy_clear":clearance>0.,
        "real_knee_bend":min(bends.values())>=15.,
        "knees_within_design_ranges":all(
          result["knee_target_ranges"][side][0]<=bends[side]<=result["knee_target_ranges"][side][1] for side in ("L","R")
        )
    }
    return result


def new_camera(name,pos):
    data=bpy.data.cameras.new(name)
    obj=bpy.data.objects.new(name,data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location=pos
    obj.rotation_euler=(Vector((0,0,.91))-obj.location).to_track_quat("-Z","Y").to_euler()
    data.type="ORTHO"
    data.ortho_scale=2.65
    return obj


def run():
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",type=Path,default=Path("build/reports/three-guards"))
    ap.add_argument("--render",action="store_true")
    opt=ap.parse_args(args)
    out=opt.out.resolve()
    out.mkdir(parents=True,exist_ok=True)
    data=json.loads(DATA.read_text(encoding="utf-8"))
    assert len(data["stances"])==3 and data["weapon_binding"]["binding"]=="fixed_hand_bone_local_transform"
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    rig=base.create_armature()
    mats=base.build_character(rig)
    wrist,feet=controls(rig)
    first=data["stances"][0]
    bpy.context.scene.frame_set(FRAMES[0])
    rest_dir=set_stance(rig,wrist,feet,first)
    objs=bind_sword(rig,mats,rest_dir)
    report=[]

    for stance in data["stances"]:
        f=FRAMES[stance["index"]-1]
        bpy.context.scene.frame_set(f)
        set_stance(rig,wrist,feet,stance,rest_dir)
        mark_key(rig,wrist,feet,f,stance["id"])
        bpy.context.view_layer.update()
        report.append(audit(rig,wrist,feet,stance,rest_dir,objs))

    for ob in (rig,wrist,feet["L"],feet["R"]):
        constant_keys(ob)
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.render.resolution_x=640
    scene.render.resolution_y=640
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_shadows=True
    cams={
        "front":new_camera("CAM_FRONT",(0.,-4.5,1.9)),
        "back":new_camera("CAM_BACK",(0.,4.5,1.9)),
        "side":new_camera("CAM_SIDE",(4.5,0.,1.9)),
    }
    scene.frame_start=1
    scene.frame_end=40

    if opt.render:
        for stance in data["stances"]:
            scene.frame_set(FRAMES[stance["index"]-1])
            for view,cam in cams.items():
                scene.camera=cam
                scene.render.filepath=str(out/f"stance_{stance['index']:02d}_{view}.png")
                bpy.ops.render.render(write_still=True)
    scene.frame_set(1)
    scene.camera=cams["front"]
    rig["MKM_STUDY"]="01@frame1 02@frame20 03@frame40"
    bpy.ops.wm.save_as_mainfile(filepath=str(out/"MKM_Three_Sword_Guards_v1.blend"))
    manifest={
        "authoring_only":True,
        "blender":bpy.app.version_string,
        "rig_bone_count":len(rig.data.bones),
        "rig_object":rig.name,
        "sword_parts":[o.name for o in objs],
        "pose_frames":{"01":1,"02":20,"03":40},
        "view_cameras":list(cams),
        "rendered_images":9 if opt.render else 0,
        "poses":report,
        "all_geometry_checks":all(all(x["checks"].values()) for x in report),
        "user_visual_approval":"PENDING",
        "game_mnae_status":"NOT_TESTED",
        "warning":"Do not accept as Minecraft render or a complete animation. Knee ranges may be incompatible with this authoring rig."
    }
    (out/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    for x in report:
        print("GUARD-QA:",json.dumps({"id":x["id"],"knee":x["knee_flexion_deg"],"wrist":x["wrist_ik_error"],"sword":x["sword_axis_dot"],"checks":x["checks"]}))
    print("BLENDER:",out/"MKM_Three_Sword_Guards_v1.blend")


if __name__=="__main__":
    run()
