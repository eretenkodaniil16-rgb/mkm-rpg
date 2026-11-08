#!/usr/bin/env python3
"""CI-only, disposable real-Minecraft F2 screenshot attempt.

Uses the exact PR's Fabric runServer world, then Quick Play singleplayer under
software Xvfb. An image is accepted only if Minecraft itself wrote it via F2.
No offline renderer, image generation, or fake in-game screenshot.
"""
from __future__ import annotations

import os
from pathlib import Path
import shutil
import signal
import struct
import subprocess
import time

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/"run"
REPORT=ROOT/"build/reports/environment-showcase"
CLIENT_LOG=ROOT/"environment-client-capture.log"


def log_tail(path: Path, length: int=13000) -> str:
    return path.read_text(encoding="utf-8",errors="replace")[-length:] if path.exists() else "<not found>"


def main():
    if os.getenv("CI") != "true":
        raise RuntimeError("Safety: screenshot creation is only enabled on ephemeral CI runners")
    if (RUN/"world").exists() or (RUN/"saves/mkm_visual_ci").exists():
        raise RuntimeError("Refusing preexisting worlds; use an empty CI checkout")
    # Import the proven QA RCON server harness instead of recreating or inventing world data.
    from smoke_watchtower_server import main as prepare_server
    os.environ["MKM_SHOWCASE_CAPTURE"]="1"
    prepare_server()
    world=RUN/"world"
    assert (world/"level.dat").is_file(), "Dedicated server did not persist its world"
    folder=RUN/"saves"
    folder.mkdir(parents=True,exist_ok=True)
    shutil.move(str(world),str(folder/"mkm_visual_ci"))
    REPORT.mkdir(parents=True,exist_ok=True)
    if not os.environ.get("DISPLAY"):
        raise RuntimeError("Launch capture under xvfb-run; DISPLAY missing")
    env=os.environ.copy()
    env.update(LIBGL_ALWAYS_SOFTWARE="1",MKM_SHOWCASE_CAPTURE="1")
    client=None
    try:
        time.sleep(2)
        with CLIENT_LOG.open("w",encoding="utf-8") as output:
            client=subprocess.Popen(
                ["./gradlew","runClient","--no-daemon","-PmkmShowcaseQuickPlay=true"],
                cwd=ROOT,env=env,stdin=subprocess.DEVNULL,
                stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
            started=time.monotonic()
            window=None
            while time.monotonic()-started<160:
                if client.poll() is not None:
                    raise RuntimeError("Client exited before screenshot; see log")
                found=subprocess.run(["xdotool","search","--onlyvisible","--name","Minecraft"],
                                     env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True)
                if found.returncode==0 and found.stdout.strip():
                    window=found.stdout.strip().splitlines()[-1]
                    break
                time.sleep(2)
            if not window:
                raise RuntimeError("No visible Minecraft window before timeout")
            # Allow the Quick Play world/chunks and textures to become visible.
            time.sleep(55)
            if client.poll() is not None:
                raise RuntimeError("Client died during world load")
            subprocess.run(["xdotool","windowactivate","--sync",window],env=env,check=True)
            subprocess.run(["xdotool","key","--clearmodifiers","F1"],env=env,check=True)
            time.sleep(2)
            subprocess.run(["xdotool","key","--clearmodifiers","F2"],env=env,check=True)
            time.sleep(6)
            screenshots=sorted((RUN/"screenshots").glob("*.png"))
            if not screenshots:
                raise RuntimeError("No native Minecraft F2 screenshot captured; UI may be at title screen")
            for i,path in enumerate(screenshots):
                data=path.read_bytes()
                assert data[:8] == bytes((137, 80, 78, 71, 13, 10, 26, 10))
                width,height=struct.unpack(">II",data[16:24])
                assert width>=640 and height>=360,(width,height)
                shutil.copy2(path,REPORT/f"mkm-watchtower-f2-{i+1}.png")
            print(f"PASS: native Minecraft F2 captured {len(screenshots)} screenshot(s) {[(p.name,p.stat().st_size) for p in screenshots]}",flush=True)
    except BaseException:
        print("CLIENT LOG:\\n"+log_tail(CLIENT_LOG),flush=True)
        print("MINECRAFT LATEST:\\n"+log_tail(RUN/"logs/latest.log"),flush=True)
        raise
    finally:
        if client is not None and client.poll() is None:
            os.killpg(client.pid,signal.SIGTERM)
            try: client.wait(timeout=12)
            except subprocess.TimeoutExpired:
                os.killpg(client.pid,signal.SIGKILL);client.wait()


if __name__=="__main__":
    main()
