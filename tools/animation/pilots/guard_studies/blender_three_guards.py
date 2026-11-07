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


def prebend_rest_legs(rig):
    """The inherited R01 leg joints are almost collinear (IK singularity).
    Define physically hinged knees with a small forward rest bend before skinning.
    """
    bpy.context.view_layer.objects.active=rig
    bpy.ops.object.mode_set(mode="EDIT")
    for side in ("L","R"):
        eb=rig.data.edit_bones
        knee=eb["thigh."+side].tail.copy()
        knee.y=-.115
        eb["thigh."+side].tail=knee
        eb["shin."+side].head=knee
        eb["shin."+side].tail=(knee.x,-.045,.07)
    bpy.ops.object.mode_set(mode="OBJECT")
    bpy.context.view_layer.update()


def realign_leg_meshes_to_rest_bones(rig):
    """Keep cuboid knees visibly hinged in the geometry source."""
    for side in ("L","R"):
        x=.13 if side=="R" else -.13
        for label,bone,old in (
            ("Thigh_"+side,"thigh."+side,Vector((x,-.02,.52))),
            ("Shin_"+side,"shin."+side,Vector((x,-.04,.22))),
        ):
            obj=bpy.data.objects[label]
            b=rig.data.bones[bone]
            mid=(b.head_local+b.tail_local)*.5
            rot=Vector((0,0,-1)).rotation_difference((b.tail_local-b.head_local).normalized())
            for v in obj.data.vertices:
                v.co=mid+rot @ (v.co-old)
    bpy.context.view_layer.update()


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
        # Disable the old nearly straight-leg IK. Each leg is analytically
        # solved into actual thigh/shin FK bone rotations at every pose frame.
        for c in list(rig.pose.bones["shin."+side].constraints):
            rig.pose.bones["shin."+side].constraints.remove(c)
    return wrist,feet


def set_rot(pb,xyz):
    pb.rotation_mode="XYZ"
    pb.rotation_euler=[math.radians(v) for v in xyz]


def turn_bone_towards(rig,pb,desired_world):
    bpy.context.view_layer.update()
    current=(rig.matrix_world.to_3x3() @ (pb.tail-pb.head)).normalized()
    target=Vector(desired_world).normalized()
    delta=current.rotation_difference(target)
    current_frame=(rig.matrix_world @ pb.matrix).to_quaternion()
    pb.rotation_quaternion = pb.rotation_quaternion @ (
        current_frame.inverted() @ delta @ current_frame
    )
    bpy.context.view_layer.update()


def solve_leg_two_bone_fk(rig,side,world_ankle):
    # Exact two-segment geometry: hip -> knee -> planted ankle.
    # The pole is the character's forward (-Y) direction, not an ambiguous
    # numerical IK solution from the nearly straight inherited R01 leg.
    thigh=rig.pose.bones["thigh."+side]
    shin=rig.pose.bones["shin."+side]
    foot=rig.pose.bones["foot."+side]
    for pb in (thigh,shin,foot):
        pb.rotation_mode="QUATERNION"
        pb.rotation_quaternion=(1.,0.,0.,0.)
    bpy.context.view_layer.update()
    hip=rig.matrix_world @ thigh.head
    ankle=Vector(world_ankle)
    a=thigh.bone.length
    b=shin.bone.length
    v=ankle-hip
    d=v.length
    if d > a+b-1e-5 or d < abs(a-b)+1e-5:
        raise ValueError(f"Unreachable ankle for {side}: distance {d:.4f}, leg lengths {a:.4f}+{b:.4f}")
    axis=v.normalized()
    h=(a*a-b*b+d*d)/(2*d)
    bend=math.sqrt(max(0.,a*a-h*h))
    front=Vector((0.,-1.,0.))
    pole=(front-axis*front.dot(axis)).normalized()
    knee_target=hip+h*axis+bend*pole
    turn_bone_towards(rig,thigh,knee_target-hip)
    actual_knee=rig.matrix_world @ shin.head
    turn_bone_towards(rig,shin,ankle-actual_knee)
    turn_bone_towards(rig,foot,Vector((0.,-1.,0.)))
    return (rig.matrix_world @ shin.tail-ankle).length


def set_stance(rig,wrist,feet,p,rest_dir=None):
    t=p["pose_targets"]
    pelvis=rig.pose.bones["pelvis"]
    # Pelvis bone local Y follows world +Z; lower hip on -local-Y, not -Z.
    pelvis.location=(0.,-t["pelvis_drop_scene"],0.)
    set_rot(pelvis,(0.,t["pelvis_yaw_deg"],0.))
    set_rot(rig.pose.bones["spine"],(t["trunk_forward_incline_deg"],t["spine_relative_yaw_deg"],0.))
    set_rot(rig.pose.bones["neck"],(0.,-t["spine_relative_yaw_deg"]*.25,0.))
    set_rot(rig.pose.bones["head"],(0.,-t["spine_relative_yaw_deg"]*.3,0.))
    set_rot(rig.pose.bones["upper_arm.L"],(-10.,0.,0.))
    set_rot(rig.pose.bones["forearm.L"],(-15.,0.,0.))
    wrist.location=t["wrist_xyz"]
    feet["L"].location=t["left_ankle_xyz"]
    feet["R"].location=t["right_ankle_xyz"]
    for side in ("L","R"):
        solve_leg_two_bone_fk(rig,side,feet[side].location)
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
    for side in ("L","R"):
        for part in ("thigh","shin","foot"):
            pb=rig.pose.bones[part+"."+side]
            pb.keyframe_insert(data_path="rotation_quaternion",frame=frame,group=pb.name)
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
    thigh=rig.data.bones["thigh."+side]
    shin=rig.data.bones["shin."+side]
    hip=vec(rig,"thigh."+side,thigh.head_local)
    k=vec(rig,"shin."+side,shin.head_local)
    ankle=vec(rig,"shin."+side,shin.tail_local)
    return round(180-math.degrees((hip-k).angle(ankle-k)),2)


def evaluated_head_geometry(rig, grip, tip):
    """Conservative deformed HEAD+HAIR mesh AABB/segment clearance.

    The legacy .398-radius head sphere falsely rejects an above-crown cut
    even when the actual rectangular voxel hair and head are untouched.
    These mesh boxes are recomputed after pose evaluation on EACH frame.
    """
    depsgraph=bpy.context.evaluated_depsgraph_get()
    distance=float("inf")
    hair_top=-float("inf")
    hair_right=-float("inf")
    head_front_y=float("inf")
    for name in ("Head","HairCap"):
        obj=bpy.data.objects[name]
        evaluated=obj.evaluated_get(depsgraph)
        mesh=evaluated.to_mesh()
        try:
            vertices=[evaluated.matrix_world @ v.co for v in mesh.vertices]
        finally:
            evaluated.to_mesh_clear()
        low=Vector(tuple(min(v[k] for v in vertices) for k in range(3)))
        high=Vector(tuple(max(v[k] for v in vertices) for k in range(3)))
        if name=="HairCap":
            hair_top=high.z
            hair_right=high.x
        elif name=="Head":
            head_front_y=low.y
        for i in range(201):
            t=i/200.
            p=grip.lerp(tip,t)
            dx=max(low.x-p.x, 0., p.x-high.x)
            dy=max(low.y-p.y, 0., p.y-high.y)
            dz=max(low.z-p.z, 0., p.z-high.z)
            distance=min(distance, math.sqrt(dx*dx+dy*dy+dz*dz))
    # R01 blade is a 0.105-wide cuboid. Account for half-width.
    return distance-.053, hair_top, hair_right, head_front_y


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
    mesh_clearance,hair_top,hair_right,head_front_y=evaluated_head_geometry(rig,grip,tip)
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
        "head_mesh_aabb_clearance_after_blade_halfwidth":round(mesh_clearance,5),
        "hair_top_world_z":round(hair_top,5),
        "head_front_mesh_y":round(head_front_y,5),
        "sword_grip_xyz":[round(float(a),5) for a in grip],
        "sword_tip_xyz":[round(float(a),5) for a in tip],
        "knee_flexion_deg":bends,
        "toe_xyz":toe,
        "sword_single_hand_bone":all(len(o.vertex_groups)==1 and o.vertex_groups[0].name=="hand.R" for o in objs),
        "knee_target_ranges":{"L":p["left_knee_flexion_deg_range"],"R":p["right_knee_flexion_deg_range"]},
    }
    result["checks"]={
        "wrist_reached":result["wrist_ik_error"]<.05,
        "sword_follows_hand":result["sword_axis_dot"]>.995 and result["sword_single_hand_bone"],
        "head_proxy_clear":(mesh_clearance>.012 if stance["index"]==1 else clearance>0.),
        "real_knee_bend":min(bends.values())>=15.,
        "knees_within_design_ranges":all(
          result["knee_target_ranges"][side][0]<=bends[side]<=result["knee_target_ranges"][side][1] for side in ("L","R")
        )
    }
    if stance["index"]==1:
        # User explicitly requested the sword hand in FRONT OF the head, not
        # held overhead. Validate actual deformed head mesh in world-space Y.
        # The whole blade should be in the air *before* the front head surface.
        result["checks"]["hand_in_front_of_face"]=grip.y<head_front_y-.07
        result["checks"]["blade_entirely_in_front_of_face"]=max(grip.y,tip.y)<head_front_y-.06
        result["checks"]["grip_near_forehead_level"]=abs(grip.z-head.z)<.30
        result["checks"]["blade_inward_from_right"]=tip.x<.03 and grip.x>.14
        result["face_forward_clearance_scene_units"]=round(head_front_y-max(grip.y,tip.y),5)
    if stance["index"]==3:
        # R06: 03 must point inward and DOWN (not the prior R05 upward form).
        # Keep the tip well above the floor and clear of the head and legs.
        result["checks"]["tip_down_and_in"]=tip.x<grip.x-.30 and tip.z<grip.z-.25
        result["checks"]["tip_safely_above_floor"]=tip.z>.12
        result["checks"]["tip_forward_of_chest"]=tip.y<-.25
        result["checks"]["tip_on_center_half"]=tip.x<.25
        result["checks"]["face_mesh_clear"]=mesh_clearance>.025
    if stance["index"]==2:
        # The mid-guard diagonal must finish below the chin in front view.
        # This is a geometric screen-silhouette proxy; actual images still need review.
        result["checks"]["mid_guard_tip_below_face"]=tip.z < head.z-.23
        result["mid_guard_tip_clearance_below_face_z"]=round(head.z-.23-tip.z,5)
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


def screen_space_audit(cameras, poses):
    """Evaluate actual rendered camera-space blade angle, not only 3D axis dot.

    Blender camera local X is image right, local Y is image up. This is
    independent of the chosen world-space XYZ component sign assumptions.
    """
    allowed_front = {
        "MKM_GUARD_HIGH_OUTER_01": (155., 177.), # almost horizontal LEFT, in FRONT of head
        "MKM_GUARD_MID_02": (110., 157.),        # diagonal left and upward
        "MKM_GUARD_LOW_MOBILE_03": (-150., -140.), # 35deg DOWN and IN, +/-5deg front view
    }
    # Newly created camera objects have deferred world transforms in Blender.
    # Force dependency graph evaluation before using matrix_world; otherwise
    # every camera can appear as an identity transform in headless runs.
    bpy.context.view_layer.update()
    for pose in poses:
        grip=Vector(pose["sword_grip_xyz"])
        tip=Vector(pose["sword_tip_xyz"])
        angles={}
        xy_deltas={}
        for view,cam in cameras.items():
            local=cam.matrix_world.inverted().to_3x3() @ (tip-grip)
            if math.hypot(local.x,local.y)<.05:
                raise ValueError(f"Blade projects as a point in {view} for {pose['id']}")
            angles[view]=round(math.degrees(math.atan2(local.y,local.x)),2)
            xy_deltas[view]=[round(local.x,5),round(local.y,5)]
        a,b=allowed_front[pose["id"]]
        pose["screen_angle_deg"]=angles
        pose["screen_blade_delta"]=xy_deltas
        pose["checks"]["front_camera_sword_angle_matches_design"]=a<=angles["front"]<=b
        if pose["id"]=="MKM_GUARD_LOW_MOBILE_03":
            pose["checks"]["about_35deg_down_from_horizontal"]=30.<=180.+angles["front"]<=40.
        pose["angle_expected_front_range_deg"]=[a,b]
        print("GUARD-PROJECTION:",pose["id"],"front/back/side",angles,
              "front_target", [a,b], "match",pose["checks"]["front_camera_sword_angle_matches_design"])


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
    prebend_rest_legs(rig)
    mats=base.build_character(rig)
    realign_leg_meshes_to_rest_bones(rig)
    wrist,feet=controls(rig)
    first=data["stances"][0]
    bpy.context.scene.frame_set(FRAMES[0])
    # Freeze the EXACT prior weapon geometry/palm bind (R2 high guard
    # before this correction). Otherwise changing guard 01 would silently
    # change the bone-local sword mesh and the already approved guard 02.
    baseline=data["weapon_binding"]["immutable_blade_bind_calibration"]
    immutable_mid=data["weapon_angle_revision"]["baseline_mid_guard_snapshot"]
    assert data["stances"][1]==immutable_mid, "Approved guard 02 was altered"
    calibration={"pose_targets":dict(first["pose_targets"])}
    calibration["pose_targets"]["wrist_xyz"]=baseline["guard01_baseline_wrist_xyz"]
    calibration["pose_targets"]["blade_direction_world"]=baseline["guard01_baseline_blade_direction_world"]
    rest_dir=set_stance(rig,wrist,feet,calibration)
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
    scene.display.shading.background_type="VIEWPORT"
    scene.display.shading.background_color=(.94,.95,.96)
    mats["floor"].diffuse_color=(.79,.82,.85,1.0)
    scene.display.shading.show_shadows=True
    cams={
        "front":new_camera("CAM_FRONT",(0.,-4.5,1.9)),
        "back":new_camera("CAM_BACK",(0.,4.5,1.9)),
        "side":new_camera("CAM_SIDE",(4.5,0.,1.9)),
    }
    scene.frame_start=1
    scene.frame_end=40
    screen_space_audit(cams, report)

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
        "user_visual_approval":("APPROVED_R06_SOURCE_BASELINE" if data.get("status")=="USER_APPROVED_BLENDER_AUTHORING_BASELINE_R06" else "PENDING"),
        "source_approval":data.get("source_approval",{}),
        "game_mnae_status":"NOT_TESTED",
        "warning":"Do not accept as Minecraft render or a complete animation. Knee ranges may be incompatible with this authoring rig."
    }
    (out/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    for x in report:
        print("GUARD-QA:",json.dumps({"id":x["id"],"knee":x["knee_flexion_deg"],"wrist":x["wrist_ik_error"],"sword":x["sword_axis_dot"],"checks":x["checks"]}))
    print("BLENDER:",out/"MKM_Three_Sword_Guards_v1.blend")


if __name__=="__main__":
    run()
