#!/usr/bin/env python3
"""R07: immutable R06 Mid Guard -> ONE 17-bone sword cut.

Launch through an installed Blender 4.0+:
 blender -b --factory-startup --python tools/animation/r07/export_r07_blender.py -- \
  --source-blend /path/to/APPROVED_R06.blend --out build/r07

Requires the R06 source from annotated tag mkm-guards-r06-approved.
Never writes any file inside assets/animation/approved/.
Authoring coordinate: Blender right X, forward -Y, up Z.
Output coordinate: Minecraft model pixels, right X, DOWN Y, back Z.
The 43 sampled frames cover 14 server ticks at 60 fps: visual impact
frame 16 = 5/14 of the duration, not an independently generated hit.
"""
import argparse
import hashlib
import json
import math
import sys
from pathlib import Path
import bpy
from mathutils import Vector, Quaternion, Matrix

FPS=60
SAMPLES=43
IMPACT=16
GUARD_FRAME=20
RIG_NAME="MKM_PILOT_RIG"
CONVERSION=24.0 / 1.8
B=Matrix(((1,0,0),(0,0,-1),(0,1,0)))
B_INV=B.inverted()
REST_HEIGHT_PX=24.0
KEYS={
    # (frame, right-wrist IK target in Blender's scene coordinates,
    #  right-hand wrist relative twist degrees, torso relative yaw degrees)
    1:((.520,-.350,.900),0,0),
    5:((.560,-.420,1.010),-7,-4),
    9:((.590,-.500,1.170),-13,-9),
    12:((.480,-.570,1.190),1,-7),
    16:((.270,-.600,1.100),29,10),
    22:((.180,-.560,.950),34,13),
    29:((.300,-.460,.900),12,6),
    36:((.420,-.390,.900),3,2),
    43:((.520,-.350,.900),0,0)
}
UVS={
    "head":(0,0),"pelvis":(16,16),"spine":(16,16),"neck":(16,16),
    "upper_arm.R":(40,16),"forearm.R":(40,16),"hand.R":(40,16),
    "upper_arm.L":(32,48),"forearm.L":(32,48),"hand.L":(32,48),
    "thigh.R":(0,16),"shin.R":(0,16),"foot.R":(0,16),
    "thigh.L":(16,48),"shin.L":(16,48),"foot.L":(16,48),
    "root":(16,16)
}

def args():
    argv=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    p=argparse.ArgumentParser()
    p.add_argument("--source-blend",type=Path,required=True)
    p.add_argument("--out",type=Path,required=True)
    p.add_argument("--render",action="store_true")
    return p.parse_args(argv)

def xyz(v):
    return [round(float(x),7) for x in v]

def game_point(vec):
    return xyz(B @ Vector(vec) * CONVERSION + Vector((0,REST_HEIGHT_PX,0)))

def game_vector(vec):
    return xyz(B @ Vector(vec) * CONVERSION)

def game_quat(q):
    mat=B @ q.to_matrix() @ B_INV
    r=mat.to_quaternion().normalized()
    return xyz((r.x,r.y,r.z,r.w))

def key_bone(rig,frame):
    for pb in rig.pose.bones:
        if pb.rotation_mode != "QUATERNION":
            pb.rotation_mode="QUATERNION"
        pb.keyframe_insert(data_path="rotation_quaternion",frame=frame,group=pb.name)
        pb.keyframe_insert(data_path="location",frame=frame,group=pb.name)

def mesh_parts(rig):
    parts=[]
    for obj in bpy.data.objects:
        bone=obj.get("mkm_source_bone")
        if not isinstance(bone,str) or bone not in rig.data.bones or obj.type!="MESH" or "sword" in obj.name.lower():
            continue
        vertices=[obj.matrix_world @ v.co for v in obj.data.vertices]
        if not vertices:continue
        # R06's rigid vertex groups are all 100% on their named bone.
        pivot=rig.data.bones[bone].head_local
        rel=[B @ (v-pivot) * CONVERSION for v in vertices]
        xyz_min=[min(v[i] for v in rel) for i in range(3)]
        xyz_max=[max(v[i] for v in rel) for i in range(3)]
        size=[max(0.10,xyz_max[i]-xyz_min[i]) for i in range(3)]
        # Use a skin-atlas hint, preserving existing Steve/Alex textures.
        parts.append({"id":obj.name,"bone":bone,
          "box":[round(x,5) for x in (*xyz_min,*size)],
          "uv":UVS[bone]})
    assert len(parts)>=15,("Incomplete visible 17-bone mesh",len(parts))
    return sorted(parts,key=lambda p:p["id"])

def snapshot_bone(rig,pb):
    arm_rest=pb.bone.matrix_local
    arm_pose=pb.matrix
    # World-space rest->evaluated (including every IK constraint).
    delta=arm_pose @ arm_rest.inverted()
    new_head=delta @ pb.bone.head_local
    parent=pb.parent
    rest_local=(parent.bone.matrix_local.inverted() @ arm_rest
        if parent else arm_rest)
    pose_local=(parent.matrix.inverted() @ arm_pose if parent else arm_pose)
    return {
        "pivot":game_point(new_head),
        "quaternion":game_quat(delta.to_quaternion()),
        "scale":[1,1,1],
        "local_translation":game_vector(pose_local.translation-rest_local.translation),
        "local_quaternion":game_quat((rest_local.inverted() @ pose_local).to_quaternion())
    }

def segment_dist(p,a,b):
    line=b-a
    if line.length_squared < 1e-10:return (p-a).length
    t=max(0,min(1,(p-a).dot(line)/line.length_squared))
    return (p-(a+t*line)).length

def audit_frame(rig):
    head=rig.pose.bones["head"]
    hand=rig.pose.bones["hand.R"]
    rest_head_center=Vector((0,-.01,1.575))
    head_center=head.matrix @ head.bone.matrix_local.inverted() @ rest_head_center
    # Extract original R06 sword centers from its actual rigid hand-bound
    # geometry. Never use the obsolete R01 hardcoded sword axis.
    def center(obj):
        return sum((obj.matrix_world @ v.co for v in obj.data.vertices),
                   Vector((0,0,0))) / len(obj.data.vertices)
    handle=bpy.data.objects.get("MKM_SWORD_HANDLE")
    blade_tip=bpy.data.objects.get("MKM_SWORD_TIP")
    if handle is None or blade_tip is None:
        raise RuntimeError("Frozen R06 sword mesh missing")
    grip_rest=center(handle)
    tip_rest=center(blade_tip)
    grip=hand.matrix @ hand.bone.matrix_local.inverted() @ grip_rest
    tip=hand.matrix @ hand.bone.matrix_local.inverted() @ tip_rest
    clearance=segment_dist(head_center,grip,tip)-.235-.055
    feet={}
    for side in ("R","L"):
        p=rig.pose.bones["foot."+side]
        toe=p.matrix @ p.bone.matrix_local.inverted() @ Vector((
            (.13 if side=="R" else -.13),-.16,.02))
        feet[side]=xyz(toe)
    return xyz(grip),xyz(tip),round(clearance,5),feet

def capture_original_pose(rig,controls):
    snapshot={}
    for pb in rig.pose.bones:
        snapshot[pb.name]={"rot":pb.matrix_basis.to_quaternion().copy(),
                           "loc":pb.location.copy()}
    targets={o.name:o.location.copy() for o in controls}
    return snapshot,targets

def main():
    opt=args()
    opt.out.mkdir(parents=True,exist_ok=True)
    asset=(opt.source_blend).resolve()
    if not asset.is_file():raise ValueError("Frozen R06 .blend not found")
    approved_hash=hashlib.sha256(asset.read_bytes()).hexdigest()
    bpy.ops.wm.open_mainfile(filepath=str(asset))
    rig=bpy.data.objects.get(RIG_NAME)
    if not rig or rig.type!="ARMATURE" or len(rig.data.bones)!=17:
        raise RuntimeError("R06 17-bone archive mismatch")
    wrist=bpy.data.objects.get("TARGET_RIGHT_HAND")
    feet=[bpy.data.objects.get("IK_ANKLE_TARGET_"+side) for side in ("R","L")]
    if not wrist or any(o is None for o in feet):
        raise RuntimeError("R06 wrist/foot constraints absent")
    scene=bpy.context.scene
    scene.frame_set(GUARD_FRAME)
    bpy.context.view_layer.update()
    originals,targets=capture_original_pose(rig,[wrist,*feet])
    # New actions only. The loaded R06 archive is not saved back to its path.
    for ob in [rig,wrist,*feet]:
        ob.animation_data_create()
        ob.animation_data.action=bpy.data.actions.new("MKM_R07_"+ob.name)
    scene.frame_start,scene.frame_end=1,SAMPLES
    scene.render.fps=FPS
    action=rig.animation_data.action
    baseline_wrist=targets["TARGET_RIGHT_HAND"]
    baseline_spine=originals["spine"]["rot"].copy()
    baseline_hand=originals["hand.R"]["rot"].copy()
    for frame,(wrist_xyz,twist,yaw) in KEYS.items():
        scene.frame_set(frame)
        for name,rest in originals.items():
            pb=rig.pose.bones[name]
            pb.rotation_mode="QUATERNION"
            pb.rotation_quaternion=rest["rot"]
            pb.location=rest["loc"]
        rig.pose.bones["spine"].rotation_quaternion=(
            baseline_spine @ Quaternion((0,0,1),math.radians(yaw)))
        rig.pose.bones["hand.R"].rotation_quaternion=(
            baseline_hand @ Quaternion((0,1,0),math.radians(twist)))
        wrist.location=Vector(wrist_xyz)
        for foot in feet:foot.location=targets[foot.name]
        key_bone(rig,frame)
        wrist.keyframe_insert(data_path="location",frame=frame)
        for foot in feet:foot.keyframe_insert(data_path="location",frame=frame)
    for ob in (rig,wrist,*feet):
        if not ob.animation_data or not ob.animation_data.action:continue
        for curve in getattr(ob.animation_data.action,"fcurves",()):
            for k in curve.keyframe_points:
                k.interpolation="BEZIER"
                k.handle_left_type="AUTO_CLAMPED"
                k.handle_right_type="AUTO_CLAMPED"
    for marker in list(scene.timeline_markers):
        scene.timeline_markers.remove(marker)
    for f,label in ((1,"MID_GUARD"),(5,"ANTICIPATION"),(9,"WINDUP"),
                    (16,"VISUAL_IMPACT_ONLY"),(22,"FOLLOW_THROUGH"),
                    (29,"RECOVERY"),(43,"MID_GUARD_RETURN")):
        scene.timeline_markers.new(label,frame=f)
    scene.frame_set(1)
    bpy.context.view_layer.update()
    export={
      "format":3,"id":"mkm:r07/mid_guard_sword_cut",
      "source":{"approved_tag":"mkm-guards-r06-approved",
                "approved_sha":"5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8",
                "approved_blend_sha256":approved_hash,
                "selected_guard":"MKM_GUARD_MID_02","source_frame":20},
      "coordinate_space":{"origin":"model_root","units":"model_pixels",
        "up_axis":"negative_y","forward_axis":"negative_z",
        "blender_to_model":"x,24-z*13.333333,y*13.333333",
        "quaternion_order":"xyzw"},
      "duration_ticks":14,"fps":60,
      "bones":[{"id":b.name,
        "parent":b.parent.name if b.parent else None,
        "rest_pivot":game_point(b.head_local),
        "rest_quaternion":game_quat(b.matrix_local.to_quaternion()),
        "scale":[1,1,1]} for b in rig.data.bones],
      "mesh_bindings":mesh_parts(rig),
      "weapon_socket":{"hand_bone":"hand.R",
        "local_handle":game_vector(
            sum((bpy.data.objects["MKM_SWORD_HANDLE"].matrix_world @ v.co
                 for v in bpy.data.objects["MKM_SWORD_HANDLE"].data.vertices),
                Vector((0,0,0)))/
            len(bpy.data.objects["MKM_SWORD_HANDLE"].data.vertices)
            -rig.data.bones["hand.R"].head_local),
        "local_tip":game_vector(
            sum((bpy.data.objects["MKM_SWORD_TIP"].matrix_world @ v.co
                 for v in bpy.data.objects["MKM_SWORD_TIP"].data.vertices),
                Vector((0,0,0)))/
            len(bpy.data.objects["MKM_SWORD_TIP"].data.vertices)
            -rig.data.bones["hand.R"].head_local),
        "vanilla_item_layer_required":True},
      "markers":[{"name":name,"t":round((f-1)/(SAMPLES-1),8),"frame":f}
          for f,name in ((1,"guard"),(5,"anticipation"),(9,"windup"),
                         (16,"impact"),(22,"follow_through"),(29,"recovery"),(43,"guard_return"))],
      "frames":[]}
    clearance_min=1e9
    toes_first=None
    max_toe_drift=0
    for frame in range(1,SAMPLES+1):
        scene.frame_set(frame)
        bpy.context.view_layer.update()
        points={pb.name:snapshot_bone(rig,pb) for pb in rig.pose.bones}
        grip,tip,clearance,feet_pos=audit_frame(rig)
        clearance_min=min(clearance_min,clearance)
        if toes_first is None:toes_first=feet_pos
        for side in ("L","R"):
            a=feet_pos[side];b=toes_first[side]
            drift=math.hypot(a[0]-b[0],a[1]-b[1])
            max_toe_drift=max(max_toe_drift,drift)
        export["frames"].append({"frame":frame,"t":round((frame-1)/42,8),
           "bones":points,"grip_blender":grip,"tip_blender":tip,
           "head_proxy_clearance_blender":clearance})
    # Prohibit inventing a passing QA if visible geometry penetrates head.
    export["qa"]={"min_head_proxy_clearance":round(clearance_min,5),
         "max_toe_xy_drift_blender":round(max_toe_drift,5),
         "no_real_game_clearance_claim":True,
         "frame_count":SAMPLES,"joint_count":len(rig.pose.bones)}
    if clearance_min<0:
        raise RuntimeError(f"R07 blade intersects Blender head proxy {clearance_min:.3f}m; no runtime export")
    if max_toe_drift>.15:
        raise RuntimeError(f"R07 planted foot drift {max_toe_drift:.3f}m; no runtime export")
    destination=opt.out/"r07_mid_guard_cut_v3.json"
    destination.write_text(json.dumps(export,indent=2)+"\n",encoding="utf-8")
    scene.frame_set(1)
    bpy.ops.wm.save_as_mainfile(filepath=str((opt.out/"MKM_R07_Mid_Guard_Cut_Derived.blend").resolve()))
    if opt.render:
        scene.render.engine="BLENDER_WORKBENCH"
        scene.render.resolution_x=640
        scene.render.resolution_y=640
        scene.render.resolution_percentage=100
        scene.render.image_settings.file_format="PNG"
        for f in (1,5,9,13,16,22,29,36,43):
            scene.frame_set(f)
            scene.render.filepath=str((opt.out/f"frame_{f:03}.png").resolve())
            bpy.ops.render.render(write_still=True)
    (opt.out/"r07_metrics.json").write_text(json.dumps(export["qa"],indent=2)+"\n")
    print("R07_EXPORTED",destination,"frames",SAMPLES,
          "head_proxy_clearance",clearance_min,"toe_drift",max_toe_drift)

if __name__=="__main__":
    main()
