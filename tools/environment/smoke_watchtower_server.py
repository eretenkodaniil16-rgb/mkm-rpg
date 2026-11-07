#!/usr/bin/env python3
"""Disposable dedicated-server runtime QA of MKM watchtower placement.

CI only: NEVER use this against a real player world. Creates a fresh superflat
world in Gradle's 'run/' directory and tests guarded GM placement over RCON.
Standard-library only; exits nonzero on timeout, failed placement, or lost loot.
"""
from __future__ import annotations

import os
from pathlib import Path
import shutil
import signal
import socket
import struct
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]
RUN = ROOT / "run"
LOG = ROOT / "environment-server-smoke.log"
PASSWORD = "mkm-watchtower-ci-only"
RCON_PORT = 25589


def receive_exact(conn: socket.socket, n: int) -> bytes:
    result = bytearray()
    while len(result) < n:
        chunk = conn.recv(n - len(result))
        if not chunk:
            raise RuntimeError("RCON socket closed unexpectedly")
        result.extend(chunk)
    return bytes(result)


class Rcon:
    def __init__(self):
        self.conn = socket.create_connection(("127.0.0.1", RCON_PORT), timeout=8)
        self.conn.settimeout(10)
        self.serial = 2
        self.send(1, 3, PASSWORD)
        for _ in range(5):
            ident, typ, _body = self.receive()
            if typ == 2:
                if ident != 1:
                    raise RuntimeError("RCON auth rejected")
                return
        raise RuntimeError("RCON auth response missing")

    def send(self, ident: int, typ: int, message: str):
        body = struct.pack("<ii", ident, typ) + message.encode("utf-8") + b"\0\0"
        self.conn.sendall(struct.pack("<i", len(body)) + body)

    def receive(self):
        size = struct.unpack("<i", receive_exact(self.conn, 4))[0]
        if not 10 <= size <= 1_048_576:
            raise RuntimeError(f"Bad RCON packet size {size}")
        packet = receive_exact(self.conn, size)
        ident, typ = struct.unpack("<ii", packet[:8])
        return ident, typ, packet[8:-2].decode("utf-8", errors="replace")

    def command(self, command: str) -> str:
        ident = self.serial
        self.serial += 1
        self.send(ident, 2, command)
        for _ in range(6):
            num, _typ, body = self.receive()
            if num == ident:
                print(f"RCON> {command}: {body[:500]}", flush=True)
                return body
        raise RuntimeError("Missing RCON command response: " + command)

    def close(self):
        self.conn.close()


def wait_for_ready(proc: subprocess.Popen):
    start = time.monotonic()
    while time.monotonic() - start < 130:
        if proc.poll() is not None:
            raise RuntimeError("Server exited early; see environment-server-smoke.log")
        if LOG.exists():
            text = LOG.read_text(encoding="utf-8", errors="replace")
            if "Done (" in text and 'For help, type "help"' in text:
                return
        time.sleep(1)
    raise RuntimeError("Dedicated Minecraft server startup timeout")


def wait_for_marker(marker: str):
    start = time.monotonic()
    while time.monotonic() - start < 12:
        if marker in LOG.read_text(encoding="utf-8", errors="replace"):
            return
        time.sleep(.25)
    raise AssertionError("Server command marker not found: " + marker)


def main():
    if os.environ.get("CI") != "true":
        raise RuntimeError("This script is destructive. Run only in an ephemeral GitHub Actions runner (CI=true).")
    if (RUN / "world").exists():
        # CI checkout is always fresh, but do not allow deleting any preexisting world.
        raise RuntimeError("Refusing to run: existing run/world directory")
    RUN.mkdir(parents=True, exist_ok=True)
    (RUN / "eula.txt").write_text("eula=true\n", encoding="ascii")
    (RUN / "server.properties").write_text(
        "motd=MKM Environment CI ephemeral test\n"
        "server-port=25588\n"
        "online-mode=false\n"
        "white-list=false\n"
        "enable-rcon=true\n"
        f"rcon.port={RCON_PORT}\n"
        f"rcon.password={PASSWORD}\n"
        "enable-command-block=true\n"
        "max-tick-time=180000\n"
        "level-name=world\n"
        "level-type=minecraft:flat\n"
        "generate-structures=false\n"
        "spawn-protection=0\n"
        'generator-settings={"biome":"minecraft:plains","features":false,"lakes":false,"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\n',
        encoding="utf-8",
    )
    with LOG.open("w", encoding="utf-8") as output:
        proc = subprocess.Popen(
            ["./gradlew", "runServer", "--no-daemon"], cwd=ROOT, stdin=subprocess.DEVNULL,
            stdout=output, stderr=subprocess.STDOUT, start_new_session=True,
        )
        try:
            wait_for_ready(proc)
            with_socket = Rcon()
            try:
                with_socket.command("forceload add 0 0 31 31")
                time.sleep(4)
                # Superflat ground at -61, so the first air block is y=-60.
                with_socket.command("setblock 4 -55 4 minecraft:gold_block")
                blocked = with_socket.command("mkmenv watchtower 1 0 -60 0")
                assert "aborted" in blocked.lower(), "Preflight did not reject existing block"
                with_socket.command("execute if block 4 -55 4 minecraft:gold_block run say MKM_ENV_NO_OVERWRITE_PASS")
                wait_for_marker("MKM_ENV_NO_OVERWRITE_PASS")
                with_socket.command("setblock 4 -55 4 minecraft:air")
                placed = with_socket.command("mkmenv watchtower 1 0 -60 0")
                assert "watchtower 1 placed" in placed.lower(), "Native NBT placement failed"
                with_socket.command("execute if block 7 -55 6 mkm:rune_slate run say MKM_ENV_RUNE_PASS")
                wait_for_marker("MKM_ENV_RUNE_PASS")
                with_socket.command("execute if block 18 -55 14 minecraft:campfire run say MKM_ENV_CAMP_PASS")
                wait_for_marker("MKM_ENV_CAMP_PASS")
                loot = with_socket.command("data get block 11 -55 5 LootTable")
                assert "mkm:chests/watchtower" in loot, "Barrel loot table missing at runtime"
                twice = with_socket.command("mkmenv watchtower 2 0 -60 0")
                assert "aborted" in twice.lower(), "A second template overwrote the first"
                print("PASS: real dedicated server loaded NBT, placed structure, preserved occupied block, rune+camp+loot and rejected duplicate placement", flush=True)
            finally:
                with_socket.close()
        except BaseException:
            print(LOG.read_text(encoding="utf-8", errors="replace")[-14000:], flush=True)
            raise
        finally:
            if proc.poll() is None:
                os.killpg(proc.pid, signal.SIGTERM)
                try:
                    proc.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(proc.pid, signal.SIGKILL)
                    proc.wait()


if __name__ == "__main__":
    main()
