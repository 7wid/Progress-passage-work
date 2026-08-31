"""Linux subprocess tests using fake Docker; never connect to a daemon."""
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
if sys.platform == "linux":
    import fcntl

SOURCE = Path(__file__).resolve().parents[1] / "deploy.sh"
FAKE_DOCKER = r"""#!/usr/bin/env python3
import json, os, pathlib, sys
args = sys.argv[1:]
log = pathlib.Path(os.environ["FAKE_LOG"])
events = [json.loads(s) for s in log.read_text().splitlines()] if log.exists() else []
with log.open("a") as out:
    out.write(json.dumps(args) + "\n")
if args[0] == "inspect":
    fmt = args[args.index("--format") + 1]
    if fmt == "{{.Id}}":
        previous = sum(1 for e in events if e[0] == "inspect" and "{{.Id}}" in e)
        print(("e" if os.environ.get("CHANGE_MYSQL") and previous else "f") * 64)
    elif fmt == "{{.Config.Image}}":
        values = dict(line.split("=", 1) for line in pathlib.Path(os.environ["FAKE_RELEASE"]).read_text().splitlines())
        key = "BACKEND_IMAGE" if "backend" in args[-1] else "FRONTEND_IMAGE"
        print(values[key])
    else:
        sys.exit(90)
elif args[0] == "compose":
    if "pull" in args and os.environ.get("FAIL_PULL"):
        sys.exit(21)
    if "exec" in args:
        if os.environ.get("FAIL_BACKUP"):
            sys.exit(22)
        print("-- test-only database dump")
    if "up" in args:
        previous = sum(1 for e in events if e[0] == "compose" and "up" in e)
        if os.environ.get("FAIL_UP") and previous == 0:
            sys.exit(23)
        if os.environ.get("FAIL_ROLLBACK") and previous > 0:
            sys.exit(24)
else:
    sys.exit(91)
"""


@unittest.skipUnless(sys.platform == "linux", "Real Bash/flock/backup integration runs in Linux CI")
class DeployScriptTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.deploy = self.root / "deployment"
        self.backups = self.root / "backups"
        self.store = self.root / "image-store"
        self.bin = self.root / "bin"
        for path in (self.deploy, self.backups, self.store, self.bin, self.deploy / "history"):
            path.mkdir()
        (self.deploy / "docker-compose.yml").write_text("services: {}\n")
        (self.deploy / ".env.prod").write_text("TEST_ONLY=not-a-production-secret\n")
        (self.deploy / ".env.prod").chmod(0o600)
        self.release = self.deploy / "release.env"
        self.before = ("BACKEND_IMAGE=ghcr.io/7wid/progress-passage-work-backend:sha-" + "a" * 40 +
                       "\nFRONTEND_IMAGE=ghcr.io/7wid/progress-passage-work-frontend:sha-" + "a" * 40 + "\n")
        self.release.write_text(self.before)
        self.release.chmod(0o600)
        self.log = self.root / "docker-calls.jsonl"
        fake = self.bin / "docker"
        fake.write_text(FAKE_DOCKER)
        fake.chmod(0o755)
        text = SOURCE.read_text()
        text = text.replace('readonly DEPLOY_DIR="/home/Ted_Kasane/tech-request-prod-deploy"',
                            'readonly DEPLOY_DIR="' + str(self.deploy) + '"')
        text = text.replace('readonly BACKUP_DIR="/data/volumes/tech-request-prod/backups"',
                            'readonly BACKUP_DIR="' + str(self.backups) + '"')
        text = text.replace('readonly IMAGE_STORE_PATH="/var/lib/containerd"',
                            'readonly IMAGE_STORE_PATH="' + str(self.store) + '"')
        text = text.replace('="5368709120"', '="1"')
        self.script = self.root / "deploy-test.sh"
        self.script.write_text(text)
        self.env = dict(os.environ, PATH=str(self.bin) + os.pathsep + os.environ["PATH"],
                        FAKE_LOG=str(self.log), FAKE_RELEASE=str(self.release))
        self.refs = ["ghcr.io/7wid/progress-passage-work-" + c + "@sha256:" + "b" * 64
                     for c in ("backend", "frontend")]

    def invoke(self, args=None, **environment):
        if args is None:
            args = ["--digests", "c" * 40, *self.refs, hashlib.sha256(self.before.encode()).hexdigest()]
        return subprocess.run(["bash", str(self.script), *args], env=dict(self.env, **environment),
                              capture_output=True, text=True, timeout=20)

    def events(self):
        return [json.loads(s) for s in self.log.read_text().splitlines()] if self.log.exists() else []

    def test_digest_success_backs_up_and_only_updates_apps(self):
        result = self.invoke()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(self.refs[0], self.release.read_text())
        backups = list(self.backups.glob("*.sql.gz"))
        self.assertEqual(len(backups), 1)
        self.assertIn("test-only", gzip.decompress(backups[0].read_bytes()).decode())
        self.assertEqual(backups[0].stat().st_mode & 0o777, 0o600)
        commands = self.events()
        up = [e for e in commands if "up" in e]
        self.assertEqual(len(up), 1)
        self.assertEqual(up[0][-2:], ["backend", "frontend"])
        self.assertIn("--no-deps", up[0])
        self.assertIn("--wait", up[0])
        self.assertFalse(any("down" in e or "run" in e for e in commands))
        self.assertFalse(list(self.deploy.glob(".release.candidate.*")))

    def test_legacy_sha_interface_remains_supported(self):
        result = self.invoke(["c" * 40])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(":sha-" + "c" * 40, self.release.read_text())

    def test_wrong_current_hash_stops_before_docker(self):
        result = self.invoke(["--digests", "c" * 40, *self.refs, "0" * 64])
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.events(), [])
        self.assertEqual(self.release.read_text(), self.before)

    def test_invalid_refs_stop_before_docker(self):
        for args in (["--digests", "c" * 40, "latest", self.refs[1], "a" * 64],
                     ["$(id)"], ["--digests", "c" * 40, *self.refs]):
            result = self.invoke(args)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(self.events(), [])

    def test_pull_or_backup_failure_never_updates_containers(self):
        for name in ("FAIL_PULL", "FAIL_BACKUP"):
            with self.subTest(name=name):
                result = self.invoke(**{name: "1"})
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(self.release.read_text(), self.before)
                self.assertFalse(any("up" in e for e in self.events()))
                self.assertFalse(list(self.backups.glob("*.tmp")))

    def test_failed_health_rolls_back_but_returns_failure(self):
        result = self.invoke(FAIL_UP="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.release.read_text(), self.before)
        self.assertEqual(len([e for e in self.events() if "up" in e]), 2)
        self.assertEqual(len(list(self.backups.glob("*.sql.gz"))), 1)

    def test_failed_rollback_still_stops_without_down(self):
        result = self.invoke(FAIL_UP="1", FAIL_ROLLBACK="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any("down" in e for e in self.events()))

    def test_manual_lock_returns_retryable_exit_without_docker(self):
        with (self.deploy / "deploy.lock").open("w") as stream:
            fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
            result = self.invoke()
        self.assertEqual(result.returncode, 75)
        self.assertEqual(self.events(), [])

    def test_mysql_recreation_is_detected(self):
        result = self.invoke(CHANGE_MYSQL="1")
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
