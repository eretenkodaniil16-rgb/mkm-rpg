import json
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

from backup_world_preflight import MANIFEST, backup_world


class BackupWorldTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.world = self.base / "world"
        (self.world / "playerdata").mkdir(parents=True)
        (self.world / "data").mkdir()
        (self.world / "region").mkdir()
        (self.world / "level.dat").write_bytes(b"level")
        (self.world / "playerdata" / "owner.dat").write_bytes(b"schema-v3")
        (self.world / "data" / "attachments.dat").write_bytes(b"world-quest")
        (self.world / "region" / "r.0.0.mca").write_bytes(b"region")
        self.zip = self.base / "before-v4.zip"

    def test_whole_world_is_archived_and_byte_verified(self):
        result = backup_world(self.world, self.zip, confirmed_stopped=True)
        self.assertEqual(4, result["file_count"])
        self.assertEqual(64, len(result["archive_sha256"]))
        with ZipFile(self.zip) as archive:
            self.assertIsNone(archive.testzip())
            self.assertIn("playerdata/owner.dat", archive.namelist())
            self.assertIn("data/attachments.dat", archive.namelist())
            manifest = json.loads(archive.read(MANIFEST))
            self.assertTrue(manifest["operator_confirmed_stopped"])
            self.assertEqual(4, len(manifest["files"]))
        self.assertEqual(b"schema-v3", (self.world / "playerdata" / "owner.dat").read_bytes())

    def test_unconfirmed_live_server_and_overwrite_fail_closed(self):
        with self.assertRaises(ValueError):
            backup_world(self.world, self.zip)
        self.assertFalse(self.zip.exists())
        backup_world(self.world, self.zip, confirmed_stopped=True)
        with self.assertRaises(ValueError):
            backup_world(self.world, self.zip, confirmed_stopped=True)

    def test_output_inside_world_rejected(self):
        with self.assertRaises(ValueError):
            backup_world(self.world, self.world / "backup.zip", confirmed_stopped=True)

    def test_missing_world_level_rejected(self):
        (self.world / "level.dat").unlink()
        with self.assertRaises(ValueError):
            backup_world(self.world, self.zip, confirmed_stopped=True)

    def test_symlink_inside_world_rejected(self):
        link = self.world / "data" / "outside"
        try:
            link.symlink_to(self.world / "level.dat")
        except (OSError, NotImplementedError):
            self.skipTest("Symlinks unavailable on this runner")
        with self.assertRaises(ValueError):
            backup_world(self.world, self.zip, confirmed_stopped=True)


if __name__ == "__main__":
    unittest.main()
