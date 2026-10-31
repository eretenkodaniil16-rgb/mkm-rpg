#!/usr/bin/env python3
"""Release packaging for MKM and the official Fabric Loader installation wrapper.

Run from the repository root after ./gradlew build.
"""
import hashlib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
mod_filename = 'mkm-rpg-0.0.3-fabric.4.jar'
jar = root / 'build' / 'libs' / mod_filename
source = root / 'distribution' / 'windows'
output = root / 'dist' / 'MKM_Fabric_26.3_Installer_Windows.zip'
if not jar.is_file():
    raise SystemExit(f'Build the release JAR first: {jar}')
output.parent.mkdir(parents=True, exist_ok=True)
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
items = {'Install_MKM.cmd': source / 'Install_MKM.cmd',
         'Install_MKM.ps1': source / 'Install_MKM.ps1',
         'README_RU.txt': source / 'README_RU.txt',
         mod_filename: jar}
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for name, path in items.items():
        archive.write(path, name)
    archive.writestr('SHA256SUMS.txt', f'{digest}  {mod_filename}\n')
with zipfile.ZipFile(output) as archive:
    assert all(name in archive.namelist() for name in list(items) + ['SHA256SUMS.txt'])
    assert hashlib.sha256(archive.read(mod_filename)).hexdigest() == digest
print(f'OK: {output} ({output.stat().st_size} bytes); MKM SHA-256 {digest}')
