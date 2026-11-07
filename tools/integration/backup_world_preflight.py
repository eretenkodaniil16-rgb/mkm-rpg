#!/usr/bin/env python3
"""Offline Minecraft world backup with SHA-256 verification; never edits world."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import tempfile
from zipfile import ZipFile, ZIP_DEFLATED

MANIFEST = "_mkm_backup_manifest.json"


def digest_stream(stream):
    h = hashlib.sha256()
    while chunk := stream.read(1024 * 1024):
        h.update(chunk)
    return h.hexdigest()


def backup_world(world, output, confirmed_stopped=False):
    world, output = Path(world), Path(output)
    if not confirmed_stopped:
        raise ValueError("Stop Minecraft or the server, then pass --server-stopped")
    if world.is_symlink() or not world.is_dir():
        raise ValueError("World path must be a real directory, not a symlink")
    root = world.resolve(strict=True)
    if not (root / "level.dat").is_file() or not (root / "playerdata").is_dir():
        raise ValueError("World needs level.dat and playerdata to migrate")
    dest = output.resolve(strict=False)
    if dest.suffix.lower() != ".zip" or dest.is_relative_to(root):
        raise ValueError("Output must be a .zip OUTSIDE the world folder")
    if dest.exists():
        raise ValueError("Refusing to overwrite an existing backup")
    dest.parent.mkdir(parents=True, exist_ok=True)
    members = []
    for f in root.rglob("*"):
        if f.is_symlink():
            raise ValueError("Backup refuses symlinks: " + str(f.relative_to(root)))
        if f.is_file():
            key = f.relative_to(root).as_posix()
            if key == MANIFEST:
                raise ValueError("Backup manifest name conflicts with world file")
            members.append((key, f))
        elif not f.is_dir():
            raise ValueError("Non-regular filesystem entry in world")
    members.sort(key=lambda e: e[0])
    if not members:
        raise ValueError("Empty world")
    staged = None
    try:
        with tempfile.NamedTemporaryFile(dir=dest.parent, prefix=".mkm-backup-",
                                         suffix=".tmp", delete=False) as tmp:
            staged = Path(tmp.name)
        details = []
        with ZipFile(staged, "w", compression=ZIP_DEFLATED,
                     compresslevel=6, allowZip64=True) as archive:
            for key, f in members:
                with f.open("rb") as stream:
                    sha = digest_stream(stream)
                size = f.stat().st_size
                archive.write(f, arcname=key)
                details.append({"path": key, "size": size, "sha256": sha})
            metadata = {"format": 1, "world_name": root.name,
                        "operator_confirmed_stopped": True, "files": details}
            archive.writestr(MANIFEST, json.dumps(metadata, sort_keys=True))
        with ZipFile(staged) as archive:
            if archive.testzip() is not None:
                raise ValueError("ZIP CRC failed")
            if len(archive.namelist()) != len(details) + 1:
                raise ValueError("Unexpected archive members")
            if json.loads(archive.read(MANIFEST)) != metadata:
                raise ValueError("Backup manifest changed")
            for item in details:
                info = archive.getinfo(item["path"])
                if info.file_size != item["size"]:
                    raise ValueError("File size changed during backup: " + item["path"])
                with archive.open(item["path"]) as stream:
                    if digest_stream(stream) != item["sha256"]:
                        raise ValueError("File changed during backup: " + item["path"])
        if dest.exists():
            raise ValueError("Another backup appeared during verification")
        os.replace(staged, dest)
        staged = None
        with dest.open("rb") as stream:
            archive_sha = digest_stream(stream)
        return {"file_count": len(details), "archive_sha256": archive_sha,
                "output": str(dest)}
    finally:
        if staged is not None:
            staged.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--server-stopped", action="store_true")
    args = parser.parse_args()
    try:
        result = backup_world(args.world, args.output, args.server_stopped)
    except (OSError, ValueError) as exc:
        parser.exit(2, "Backup aborted: " + str(exc) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
