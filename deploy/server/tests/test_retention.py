from datetime import datetime, timedelta, timezone
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock


SOURCE = Path(__file__).resolve().parents[1] / "retention.py"
SPEC = importlib.util.spec_from_file_location("retention", SOURCE)
retention = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = retention
SPEC.loader.exec_module(retention)


class RetentionPlanTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.backups = self.root / "backups"
        self.history = self.root / "history"
        self.backups.mkdir()
        self.history.mkdir()
        self.now = datetime(2026, 9, 3, tzinfo=timezone.utc)

    def create(self, directory, name, age_days):
        path = directory / name
        path.write_bytes(b"test")
        path.chmod(0o600)
        modified = (self.now - timedelta(days=age_days)).timestamp()
        os.utime(path, (modified, modified))
        return path

    def backup_name(self, index):
        value = self.now - timedelta(days=index)
        return "mysql-" + value.strftime("%Y%m%dT%H%M%SZ") + ".sql.gz"

    def test_preserves_minimum_fourteen_even_when_all_are_old(self):
        paths = [self.create(self.backups, self.backup_name(i + 40), i + 40) for i in range(16)]
        plan = retention.build_plan(self.backups, self.history, self.now)
        self.assertEqual(len(plan.database_protected), 14)
        self.assertEqual({value.path for value in plan.database_delete}, set(paths[-2:]))

    def test_preserves_recent_backups_beyond_minimum(self):
        for index in range(20):
            self.create(self.backups, self.backup_name(index), index)
        plan = retention.build_plan(self.backups, self.history, self.now)
        self.assertEqual(plan.database_delete, ())

    def test_handles_pre_cd_backup_name_and_ignores_unknown_files(self):
        pre_cd = self.create(
            self.backups, "mysql-pre-cd-20260701T010203Z.abc123.sql.gz", 64
        )
        for index in range(14):
            self.create(self.backups, self.backup_name(index), index)
        unknown = self.create(self.backups, "other-backup.sql.gz", 365)
        plan = retention.build_plan(self.backups, self.history, self.now)
        self.assertEqual([value.path for value in plan.database_delete], [pre_cd])
        self.assertNotIn(unknown, [value.path for value in plan.delete])

    def test_expires_only_recognized_audit_files(self):
        expired = self.create(self.history, "pull-deploy-20260101T000000.000000Z.log", 245)
        recent = self.create(self.history, "release-20260801T000000Z.env", 33)
        unknown = self.create(self.history, "notes.txt", 365)
        plan = retention.build_plan(self.backups, self.history, self.now)
        self.assertEqual([value.path for value in plan.audit_delete], [expired])
        self.assertNotIn(recent, [value.path for value in plan.delete])
        self.assertNotIn(unknown, [value.path for value in plan.delete])

    @unittest.skipUnless(os.name == "posix", "Linux permission semantics are verified in CI")
    def test_enable_marker_requires_exact_content_and_mode(self):
        marker = self.root / "retention.enabled"
        marker.write_bytes(b"enabled\n")
        marker.chmod(0o600)
        retention.enabled(marker, marker.stat().st_uid)
        marker.write_bytes(b"enabled")
        with self.assertRaises(retention.RetentionError):
            retention.enabled(marker, marker.stat().st_uid)

    @unittest.skipUnless(os.name == "posix", "Linux no-follow lock semantics are verified in CI")
    def test_lock_rejects_symlink(self):
        target = self.root / "target"
        target.write_bytes(b"unchanged")
        link = self.root / "deploy.lock"
        link.symlink_to(target)
        with self.assertRaises(OSError):
            with retention.locked(link, os.geteuid()):
                pass
        self.assertEqual(target.read_bytes(), b"unchanged")

    @unittest.skipUnless(os.name == "posix", "Linux flock semantics are verified in CI")
    def test_lock_reports_busy_without_mutating_files(self):
        lock = self.root / "deploy.lock"
        lock.write_bytes(b"")
        lock.chmod(0o600)
        with mock.patch.object(retention.fcntl, "flock", side_effect=BlockingIOError):
            with retention.locked(lock, os.geteuid()) as acquired:
                self.assertFalse(acquired)


if __name__ == "__main__":
    unittest.main()
