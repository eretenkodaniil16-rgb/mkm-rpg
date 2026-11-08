#!/usr/bin/env python3
"""CI-only real Minecraft Fabric runtime: operator setup, saved tower, restart.

No real player worlds are ever touched. Uses the existing Environment RCON client
and a DIFFERENT disposable superflat save from its structural-placement smoke.
"""
from __future__ import annotations

import os
from pathlib import Path
import signal
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "environment"))
import smoke_watchtower_server as env  # noqa: E402

RUN = ROOT / "run"
WORLD = RUN / "showcase_world"
LOG = ROOT / "showcase-site-server-smoke.log"
env.LOG = LOG


def launch():
    stream = LOG.open("w", encoding="utf-8")
    proc = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon"], cwd=ROOT,
        stdin=subprocess.DEVNULL, stdout=stream, stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    try:
        env.wait_for_ready(proc)
        return proc, stream
    except BaseException:
        terminate(proc)
        stream.close()
        raise


def terminate(proc):
    if proc.poll() is not None:
        return
    os.killpg(proc.pid, signal.SIGTERM)
    try:
        proc.wait(timeout=15)
    except subprocess.TimeoutExpired:
        os.killpg(proc.pid, signal.SIGKILL)
        proc.wait(timeout=10)


def test_once() -> None:
    if os.environ.get("CI") != "true":
        raise RuntimeError("Only a disposable GitHub Actions runner may execute this script")
    if WORLD.exists():
        raise RuntimeError("Refusing to overwrite an existing run/showcase_world save")
    RUN.mkdir(parents=True, exist_ok=True)
    (RUN / "eula.txt").write_text("eula=true\n", encoding="ascii")
    # Same known-good PR60 superflat generator; place tower with first AIR at Y=-60.
    (RUN / "server.properties").write_text(
        "motd=MKM Showcase World-only ephemeral persistence test\n"
        "server-port=25588\n"
        "online-mode=false\n"
        "white-list=false\n"
        "enable-rcon=true\n"
        f"rcon.port={env.RCON_PORT}\n"
        f"rcon.password={env.PASSWORD}\n"
        "max-tick-time=180000\n"
        "level-name=showcase_world\n"
        "level-type=minecraft:flat\n"
        "generate-structures=false\n"
        "spawn-protection=0\n"
        'generator-settings={"biome":"minecraft:plains","features":false,"lakes":false,"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\n',
        encoding="utf-8",
    )

    first = None
    try:
        proc, stream = launch()
        first = (proc, stream)
        rcon = env.Rcon()
        try:
            rcon.command("forceload add 0 0 127 31")
            time.sleep(3)
            before = rcon.command("mkmshowcase status")
            assert "configured=false" in before, "Uninitialized site must not be invented"
            made = rcon.command("mkmshowcase setup 1 0 -60 0")
            assert "watchtower 1 placed" in made.lower() or "mkm tower bound" in made.lower(), (
                "The GM console could not install the real authored scene: " + made
            )
            after = rcon.command("mkmshowcase status")
            assert "configured=true" in after and "0, -60, 0" in after, (
                "World site registration failed: " + after
            )
            again = rcon.command("mkmshowcase bind 0 -60 0")
            assert "denied" in again.lower(), "Repeated site binding must be rejected"
            rcon.command("execute if block 7 -55 6 mkm:rune_slate run say MKM_SHOWCASE_RUNE_ACTUAL")
            env.wait_for_marker("MKM_SHOWCASE_RUNE_ACTUAL")
            saved = rcon.command("save-all flush")
            print("Saving real Showcase world and world-level Fabric attachments: " + saved[:250],flush=True)
            try:
                rcon.command("stop")
            except (OSError, RuntimeError):
                pass
        finally:
            rcon.close()
        proc.wait(timeout=50)
        stream.close()
        first = None

        # No world directory deletion, no new generated content or relinking on restart.
        proc, stream = launch()
        first = (proc, stream)
        rcon = env.Rcon()
        try:
            again = rcon.command("mkmshowcase status")
            assert "configured=true" in again and "0, -60, 0" in again, (
                "Saved tower site did not survive a REAL server restart: " + again
            )
            blocked = rcon.command("mkmshowcase setup 2 0 -60 0")
            assert "aborted" in blocked.lower(), "Existing structure must reject overwrite"
            repeated = rcon.command("mkmshowcase bind 0 -60 0")
            assert "denied" in repeated.lower(), "Previously saved world anchor was rebound"
            rcon.command("execute if block 7 -55 6 mkm:rune_slate run say MKM_SHOWCASE_RUNE_RESTART")
            env.wait_for_marker("MKM_SHOWCASE_RUNE_RESTART")
            print("PASS: real Fabric Minecraft server installed tower, preserved its site across "
                  "shutdown/restart, retained rune and denied duplicate placement/bind", flush=True)
            try:
                rcon.command("stop")
            except (OSError, RuntimeError):
                pass
        finally:
            rcon.close()
        proc.wait(timeout=50)
        stream.close()
        first = None
    except BaseException:
        if LOG.exists():
            print(LOG.read_text(encoding="utf-8",errors="replace")[-16000:],flush=True)
        raise
    finally:
        if first is not None:
            terminate(first[0])
            first[1].close()


if __name__ == "__main__":
    test_once()
