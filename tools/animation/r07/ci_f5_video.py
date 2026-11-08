#!/usr/bin/env python3
"""Best-effort *actual Minecraft client* F5 reproducibility attempt.

Runs on a disposable CI world with Xvfb. No game changes, no injected cheats
in the distributed mod. This QA run must not claim gameplay/video acceptance
unless the world login, real rendered frames and a visible sword are inspected.
"""
from __future__ import annotations
import json
import os
from pathlib import Path
import signal
import shutil
import subprocess
import sys
import time
import traceback

ROOT=Path.cwd()
QA=ROOT/"build"/"reports"/"mnae-r07-v4-f5"
QA.mkdir(parents=True,exist_ok=True)
LOG=QA/"capture_script.log"
WORLD_NAME="MKM_R07_V4_QA_WORLD"
WORLD=ROOT/"run"/"saves"/WORLD_NAME
LOGFILE=QA/"client.log"
MILESTONES=[]
PROC=[]

def run(args,**kwargs):
    print("RUN",args,flush=True)
    return subprocess.run(args,check=True,**kwargs)

def spawn(args,logfile,env=None):
    fh=open(logfile,"w",encoding="utf-8")
    p=subprocess.Popen(args,stdout=fh,stderr=subprocess.STDOUT,
                       env=env,start_new_session=True)
    PROC.append((p,fh))
    print("SPAWN",p.pid,args,flush=True)
    return p

def kill(p):
    if p.poll() is None:
        try:os.killpg(p.pid,signal.SIGTERM)
        except ProcessLookupError:pass
        try:p.wait(timeout=9)
        except subprocess.TimeoutExpired:
            try:os.killpg(p.pid,signal.SIGKILL)
            except ProcessLookupError:pass
            p.wait()

def wait_ready(p,logfile,needles,timeout):
    for sec in range(timeout):
        time.sleep(1)
        data=logfile.read_text(errors="replace") if logfile.exists() else ""
        if any(v in data for v in ["MixinApplyError","Mixin apply failed","Game crashed!"]):
            raise RuntimeError("Minecraft fatal log during startup")
        if all(v in data for v in needles):
            return sec
        if p.poll() is not None:
            raise RuntimeError(f"Process exited before ready: {p.returncode}. Log: {data[-3000:]}")
    raise TimeoutError("Timeout waiting for logs "+str(needles))

def snapshot(label):
    filename=QA/(label+".png")
    subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y",
       "-f","x11grab","-framerate","1","-video_size","1280x720",
       "-i",":98.0","-frames:v","1",str(filename)],timeout=20,check=False)
    return filename.is_file()

def key(*names):
    run(["xdotool","key","--clearmodifiers",*names])
    time.sleep(.20)

def chat(msg):
    key("t")
    run(["xdotool","type","--clearmodifiers","--delay","45",msg])
    key("Return")
    time.sleep(1.1)

def stage(label,mode,seconds=3.2):
    key("r")
    time.sleep(seconds)
    screenshot=snapshot(label)
    MILESTONES.append({"label":label,"expected_renderer":mode,
       "screenshot":screenshot,
       "timestamp":time.time()})
    print("CAPTURED",label,screenshot,flush=True)

def main():
    (ROOT/"run").mkdir(exist_ok=True)
    # Create a real 26.3 dedicated server world, then copy the NBT save into
    # this disposable singleplayer client's own saves directory.
    prop=ROOT/"run"/"server.properties"
    prop.write_text("level-name=R07_CI_SERVER\ngamemode=creative\n"
                    "difficulty=peaceful\nonline-mode=false\n"
                    "spawn-protection=0\n",encoding="utf-8")
    (ROOT/"run"/"eula.txt").write_text("eula=true\n",encoding="utf-8")
    server=spawn(["./gradlew","runServer","--no-daemon"],QA/"server.log")
    try:wait_ready(server,QA/"server.log",["MKM Fabric 26.3 initialized","Done ("],100)
    finally:kill(server)
    old=ROOT/"run"/"R07_CI_SERVER"
    if not (old/"level.dat").is_file():
        raise RuntimeError("Actual server world level.dat missing")
    WORLD.parent.mkdir(parents=True,exist_ok=True)
    if WORLD.exists():shutil.rmtree(WORLD)
    shutil.copytree(old,WORLD)
    (WORLD/"session.lock").unlink(missing_ok=True)
    # Cheats are enabled exclusively in the disposable CI copy, not in a user's
    # real world. nbtlib is a QA-only dependency installed in isolated venv.
    import nbtlib
    save=nbtlib.load(str(WORLD/"level.dat"))
    save["Data"]["GameType"]=nbtlib.Int(1)
    save["Data"]["allowCommands"]=nbtlib.Byte(1)
    save["Data"]["LevelName"]=nbtlib.String(WORLD_NAME)
    save.save()
    env=os.environ.copy()
    env.update({"DISPLAY":":98","LIBGL_ALWAYS_SOFTWARE":"1","GDK_BACKEND":"x11",
                "SDL_VIDEODRIVER":"x11"})
    xvfb=spawn(["Xvfb",":98","-screen","0","1280x720x24","-ac",
                 "+extension","GLX"],QA/"xvfb.log",env=env)
    time.sleep(2)
    if xvfb.poll() is not None:raise RuntimeError("Xvfb exited")
    subprocess.run(["xdpyinfo","-display",":98"],env=env,check=True,
                  stdout=subprocess.DEVNULL)
    client=spawn(["./gradlew","runClient","--no-daemon",
              "--args=--width 1280 --height 720 --quickPlaySingleplayer "
              +WORLD_NAME+" --quickPlayPath mkm-r07-quickplay.json"],
              LOGFILE,env)
    try:
        wait_ready(client,LOGFILE,["MKM Fabric 26.3 initialized"],150)
        joined=False
        quick=ROOT/"run"/"mkm-r07-quickplay.json"
        for _ in range(145):
            time.sleep(1)
            if quick.exists() and WORLD_NAME in quick.read_text(errors="replace"):
                joined=True
                break
            if client.poll() is not None:break
        if not joined:
            snapshot("quickplay_not_confirmed")
            raise RuntimeError("Real singleplayer join unconfirmed; no F5 video acceptance")
        print("REAL_MINECRAFT_SINGLEPLAYER_JOIN_CONFIRMED",flush=True)
        time.sleep(6)
        windows=subprocess.run(["xdotool","search","--name","Minecraft"],
                 capture_output=True,text=True,env=env).stdout.split()
        if windows:
            subprocess.run(["xdotool","windowactivate","--sync",windows[-1]],env=env)
        key("Escape") # release any overlays
        time.sleep(.4)
        key("Escape") # if pause appeared, resume; no menu automation required
        time.sleep(.4)
        chat("/give @s minecraft:iron_sword")
        chat("/gamemode survival")
        key("F5")
        time.sleep(2)
        snapshot("in_world_f5")
        # Video captures actual GPU/Xvfb screen. 60 encoded FPS may duplicate
        # frames; report effective visual FPS separately from codec setting.
        video=QA/"r07_f5_actual_client_60fps_target.mp4"
        ff=spawn(["ffmpeg","-hide_banner","-loglevel","warning","-y",
            "-f","x11grab","-framerate","60","-video_size","1280x720",
            "-i",":98.0","-t","23","-an","-c:v","libx264",
            "-preset","ultrafast","-pix_fmt","yuv420p",str(video)],
            QA/"ffmpeg.log",env)
        stage("mode_c3_impact","C3.7",3.2)
        key("F8")
        stage("mode_mnae6_impact","MNAE6_PROXY",3.2)
        key("F9")
        stage("mode_r07_impact","MNAE17",3.2)
        # Visual fallback probes: shield offhand, left main hand, axe and
        # armor remain separate manual acceptance gates.
        time.sleep(4)
        ff.wait(timeout=45)
        if ff.returncode!=0 or not video.exists() or video.stat().st_size<50000:
            raise RuntimeError("No meaningful Xvfb client MP4 produced")
        print("Actual Minecraft F5 file recorded; *NOT* automatically visually accepted",flush=True)
    finally:
        kill(client)
        kill(xvfb)

if __name__=="__main__":
    success=False
    try:
        main();success=True
    except Exception:
        traceback.print_exc()
    finally:
        for p,fh in reversed(PROC):
            kill(p)
            try:fh.close()
            except Exception:pass
        report={"real_singleplayer_qa_attempt_pass":success,
                "milestones":MILESTONES,
                "camera_f5_intended":True,"fps_capture_target":60,
                "effective_fps_unverified":True,
                "manual_frame_review_required":True,
                "not_tested":["left_main_arm","second_player",
                              "armor_and_offhand","held_item_collision"]}
        (QA/"report.json").write_text(json.dumps(report,indent=2)+"\n")
    sys.exit(0 if success else 1)
