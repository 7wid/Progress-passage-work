import copy
from contextlib import nullcontext
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock

SPEC = importlib.util.spec_from_file_location("pull_agent", Path(__file__).resolve().parents[1] / "pull-agent.py")
agent = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(agent)


def record(sequence=63):
    return {"schema_version": 1, "repository": agent.REPOSITORY, "commit": "a" * 40,
            "sequence": sequence, "ci_run_id": 10, "publish_run_id": 11,
            "publish_run_attempt": 1, "created_at": "2026-08-31T00:00:00Z",
            "images": {c: agent.PREFIX + c + "@sha256:" + "b" * 64 for c in ("backend", "frontend")}}


def candidate(sequence=63):
    return agent.token(record(sequence), "sha256:" + "c" * 64)


def images():
    return {c: agent.PREFIX + c + ":sha-" + "d" * 40 for c in ("backend", "frontend")}


def runtime():
    return {c: {"container_id": digit * 64, "image_id": "sha256:" + digit * 64}
            for c, digit in (("backend", "1"), ("frontend", "2"), ("mysql", "3"))}


def state():
    return {"schema_version": 1, "status": "ready", "floor": candidate(), "last": None,
            "attempt": None, "expected_images": images(), "expected_runtime": runtime(),
            "blocked_commits": []}


class ValidationTests(unittest.TestCase):
    def test_valid_record(self):
        self.assertEqual(agent.validate_record(record()), record())

    def test_rejects_bad_record_fields(self):
        for name, value in (("schema_version", True), ("repository", "other/repo"), ("commit", "$(id)"),
                            ("sequence", 0), ("ci_run_id", True), ("publish_run_attempt", -1),
                            ("created_at", "2026-02-30T00:00:00Z"), ("images", {"backend": "latest"})):
            with self.subTest(name=name), self.assertRaises(agent.Stop):
                agent.validate_record(dict(record(), **{name: value}))
        with self.assertRaises(agent.Stop):
            agent.validate_record(dict(record(), extra="ignored?"))

    def test_rejects_json_duplicates_nonfinite_and_oversize(self):
        for value in ('{"a":1,"a":2}', '{"a":NaN}', "not json", " " * 2_000_001, None):
            with self.subTest(value=str(value)[:30]), self.assertRaises(agent.Stop):
                agent.parse_json(value)

    def test_digest_rejects_tags_wrong_repo_and_shell_input(self):
        for ref in ("ghcr.io/other/app@sha256:" + "a" * 64, "latest",
                    agent.PREFIX + "backend@sha256:" + "a" * 63,
                    agent.PREFIX + "backend@sha256:" + "a" * 64 + "\n"):
            with self.subTest(ref=ref), self.assertRaises(agent.Stop):
                agent.image_pair(dict(record()["images"], backend=ref))

    def test_uninitialized_and_new_release(self):
        self.assertEqual(agent.decision(candidate(), None), "uninitialized")
        self.assertEqual(agent.decision(candidate(), state()), "deploy")

    def test_idempotence_ignores_metadata_only_republication(self):
        s = dict(state(), last=candidate())
        republished = dict(candidate(), metadata_digest="sha256:" + "e" * 64)
        self.assertEqual(agent.decision(republished, s), "unchanged")

    def test_blocks_older_or_reused_sequence(self):
        for value in (candidate(62), dict(candidate(), commit="f" * 40),
                      dict(candidate(), images={c: agent.PREFIX + c + "@sha256:" + "f" * 64
                                                 for c in ("backend", "frontend")})):
            with self.subTest(value=value), self.assertRaises(agent.Stop):
                agent.decision(value, state())

    def test_blocks_failed_interrupted_and_same_commit_new_sequence(self):
        for s in (dict(state(), status="failed"), dict(state(), status="in_progress"),
                  dict(state(), blocked_commits=["a" * 40])):
            with self.assertRaises(agent.Stop):
                agent.decision(candidate(), s)
        with self.assertRaises(agent.Stop):
            agent.decision(candidate(64), dict(state(), last=candidate()))
        new = dict(candidate(64), commit="e" * 40)
        self.assertEqual(agent.decision(new, dict(state(), last=candidate())), "deploy")

    def test_verbose_manifest_old_and_new_shapes(self):
        old = {"Digest": "sha256:" + "a" * 64, "Platform": {"os": "linux", "architecture": "amd64"}}
        new = {"Descriptor": {"digest": "sha256:" + "a" * 64,
                             "platform": {"os": "linux", "architecture": "amd64"}}}
        for value in (old, new):
            self.assertEqual(agent.marker_digest(value), "sha256:" + "a" * 64)
        self.assertIn("linux/amd64", agent.manifest_platforms([old, {"Platform": {"os": "unknown", "architecture": "unknown"}}]))
        with self.assertRaises(agent.Stop):
            agent.marker_digest([old])
        with self.assertRaises(agent.Stop):
            agent.marker_digest(dict(old, Platform={"os": "linux", "architecture": "arm64"}))

    def test_corrupted_state_is_not_treated_as_first_install(self):
        for changed in (dict(state(), expected_runtime={}), dict(state(), status="in_progress"),
                        dict(state(), blocked_commits=["bad"]), dict(state(), extra=True),
                        dict(state(), last=candidate(62))):
            with self.assertRaises(agent.Stop):
                agent.validate_state(changed)

    def test_service_defaults_to_dry_run(self):
        service = (Path(__file__).resolve().parents[1] / "systemd/tech-request-cd.service").read_text()
        self.assertIn("User=Ted_Kasane", service)
        self.assertIn("--dry-run", service)
        self.assertNotIn("--apply", service)
        self.assertIn("KillMode=control-group", service)


class FlowTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "history").mkdir()
        self.agent = agent.Agent(self.root, self.root)
        self.current_runtime = runtime()
        self.write_images(images())
        # Windows cannot model Linux uid/mode/flock; Linux CI runs real checks.
        if os.name == "nt":
            patch = mock.patch.object(agent, "private_file", lambda p: p.stat())
            patch.start()
            self.addCleanup(patch.stop)
            patch = mock.patch.object(agent, "locked", lambda p: nullcontext())
            patch.start()
            self.addCleanup(patch.stop)
        for name, arguments in (
            ("runtime_snapshot", {"side_effect": lambda images: copy.deepcopy(self.current_runtime)}),
            ("fetch_record", {"return_value": candidate()}),
            ("check_images", {}),
            ("deploy", {"side_effect": self.successful_deploy}),
        ):
            patch = mock.patch.object(self.agent, name, **arguments)
            setattr(self, "mock_" + name, patch.start())
            self.addCleanup(patch.stop)

    def write_images(self, refs):
        path = self.root / "release.env"
        path.write_text("".join(c.upper() + "_IMAGE=" + refs[c] + "\n" for c in ("backend", "frontend")))
        path.chmod(0o600)

    def enable(self):
        path = self.root / "pull-agent.enabled"
        path.write_bytes(b"enabled\n")
        path.chmod(0o600)

    def initialize(self):
        self.agent.initialize("sha256:" + "c" * 64)

    def successful_deploy(self, candidate, checksum, log_path):
        self.assertEqual(self.agent.read_state()["status"], "in_progress")
        self.assertEqual(self.agent.environment_snapshot()[1], checksum)
        self.write_images(candidate["images"])
        for c in ("backend", "frontend"):
            self.current_runtime[c] = {"container_id": "4" * 64, "image_id": "sha256:" + "5" * 64}
        return 0

    def test_dry_run_does_not_initialize_or_deploy(self):
        before = (self.root / "release.env").read_bytes()
        self.agent.once()
        self.assertFalse(self.agent.state_path.exists())
        self.assertEqual((self.root / "release.env").read_bytes(), before)
        self.mock_deploy.assert_not_called()

    def test_initialize_once_without_deployment(self):
        self.initialize()
        self.assertEqual(self.agent.read_state()["floor"], candidate())
        before = self.agent.state_path.read_bytes()
        with self.assertRaises(agent.Stop):
            self.initialize()
        self.assertEqual(self.agent.state_path.read_bytes(), before)
        self.mock_deploy.assert_not_called()

    def test_apply_requires_initialization_and_enable_file(self):
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.initialize()
        with self.assertRaises(FileNotFoundError):
            self.agent.once(apply=True)
        self.mock_deploy.assert_not_called()

    def test_apply_success_then_no_redeployment(self):
        self.initialize()
        self.enable()
        self.agent.once(apply=True)
        self.assertEqual(self.agent.read_state()["last"], candidate())
        self.agent.once(apply=True)
        self.assertEqual(self.mock_deploy.call_count, 1)

    def test_dry_run_preserves_initialized_state(self):
        self.initialize()
        before = self.agent.state_path.read_bytes()
        self.agent.once()
        self.assertEqual(self.agent.state_path.read_bytes(), before)
        self.mock_deploy.assert_not_called()

    def test_manual_environment_or_runtime_drift_blocks(self):
        self.initialize()
        self.enable()
        self.current_runtime["backend"]["container_id"] = "f" * 64
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.current_runtime = runtime()
        self.write_images(record()["images"])
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.mock_deploy.assert_not_called()

    def test_deploy_failure_persists_and_next_run_stops(self):
        self.initialize()
        self.enable()
        self.mock_deploy.side_effect = lambda *args: 1
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.assertEqual(self.agent.read_state()["status"], "failed")
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.assertEqual(self.mock_deploy.call_count, 1)

    def test_interruption_marks_failed(self):
        self.initialize()
        self.enable()
        self.mock_deploy.side_effect = KeyboardInterrupt()
        with self.assertRaises(KeyboardInterrupt):
            self.agent.once(apply=True)
        self.assertEqual(self.agent.read_state()["status"], "failed")

    def test_busy_manual_lock_keeps_prior_state(self):
        self.initialize()
        self.enable()
        before = self.agent.read_state()
        self.mock_deploy.side_effect = lambda *args: 75
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.assertEqual(self.agent.read_state(), before)
        self.assertEqual(self.mock_deploy.call_count, 1)

    def test_registry_failure_keeps_current_state(self):
        self.initialize()
        self.enable()
        before = self.agent.state_path.read_bytes()
        self.mock_fetch_record.side_effect = agent.Stop("registry timeout")
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.assertEqual(self.agent.state_path.read_bytes(), before)
        self.mock_deploy.assert_not_called()

    def test_resume_accepts_recreated_rollback_containers_not_other_images(self):
        self.initialize()
        self.enable()
        self.mock_deploy.side_effect = lambda *args: 1
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.current_runtime["backend"]["container_id"] = "6" * 64
        self.current_runtime["backend"]["image_id"] = "sha256:" + "7" * 64
        with self.assertRaises(agent.Stop):
            self.agent.resume()
        self.current_runtime["backend"]["image_id"] = runtime()["backend"]["image_id"]
        self.agent.resume()
        resumed = self.agent.read_state()
        self.assertEqual(resumed["status"], "ready")
        self.assertIn(candidate()["commit"], resumed["blocked_commits"])
        self.assertEqual(resumed["expected_runtime"], self.current_runtime)
        with self.assertRaises(agent.Stop):
            self.agent.once(apply=True)
        self.assertEqual(len(list((self.root / "history").glob("pull-state-before-resume-*.json"))), 1)

    def test_release_env_is_parsed_not_sourced(self):
        (self.root / "release.env").write_text("BACKEND_IMAGE=$(touch /tmp/not-executed)\n")
        with self.assertRaises(agent.Stop):
            self.agent.environment_snapshot()


class RegistryTests(unittest.TestCase):
    def setUp(self):
        self.agent = agent.Agent(Path.cwd(), Path.cwd())
        self.ref = agent.MARKER + "@sha256:" + "c" * 64
        self.image = {
            "RepoDigests": [self.ref], "Os": "linux", "Architecture": "amd64",
            "Config": {"Labels": {agent.LABEL: json.dumps(record()),
                                 "org.opencontainers.image.source": "https://github.com/" + agent.REPOSITORY,
                                 "org.opencontainers.image.revision": "a" * 40}}}
        patch = mock.patch.object(agent.shutil, "disk_usage", return_value=mock.Mock(free=6 * 1024**3))
        patch.start()
        self.addCleanup(patch.stop)

    def docker(self, *args, **kwargs):
        if args[:2] == ("manifest", "inspect"):
            return json.dumps({"Descriptor": {"digest": "sha256:" + "c" * 64,
                                               "platform": {"os": "linux", "architecture": "amd64"}}})
        if args[0] == "pull":
            self.assertEqual(args[1], self.ref)
            return ""
        if args[:2] == ("image", "inspect"):
            self.assertEqual(args[2], self.ref)
            return json.dumps([self.image])
        self.fail("Unexpected Docker operation")

    def test_mutable_pointer_resolves_then_only_digest_is_pulled(self):
        with mock.patch.object(self.agent, "docker", side_effect=self.docker) as commands:
            self.assertEqual(self.agent.fetch_record(), candidate())
            self.assertEqual(commands.call_args_list[0].args[-1], agent.MARKER + ":production")
            self.assertEqual(commands.call_args_list[1].args, ("pull", self.ref))

    def test_marker_config_mismatch_blocks(self):
        for field, value in (("RepoDigests", []), ("Architecture", "arm64"),
                             ("Config", {"Cmd": ["sh"]}), ("Config", {"Labels": {}})):
            with self.subTest(field=field):
                original = copy.deepcopy(self.image)
                self.image[field] = value
                with mock.patch.object(self.agent, "docker", side_effect=self.docker), self.assertRaises(agent.Stop):
                    self.agent.fetch_record()
                self.image = original

    def test_low_disk_stops_before_marker_pull(self):
        with mock.patch.object(agent.shutil, "disk_usage", return_value=mock.Mock(free=1)):
            with mock.patch.object(self.agent, "docker") as commands, self.assertRaises(agent.Stop):
                self.agent.fetch_record("sha256:" + "c" * 64)
            commands.assert_not_called()

    def test_application_platform_gate(self):
        with mock.patch.object(self.agent, "docker", side_effect=[
            "linux/amd64\n", json.dumps([{"Descriptor": {"platform": {"os": "linux", "architecture": "arm64"}}}])
        ]), self.assertRaises(agent.Stop):
            self.agent.check_images(candidate())


@unittest.skipUnless(os.name == "posix", "Real Linux permission/flock checks run in CI")
class LinuxSafetyTests(unittest.TestCase):
    def test_lock_excludes_second_open(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "lock"
            with agent.locked(path), self.assertRaises(agent.Stop):
                with agent.locked(path):
                    pass

    def test_private_file_rejects_symlink_or_broad_mode(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "file"
            path.write_text("{}")
            path.chmod(0o644)
            with self.assertRaises(agent.Stop):
                agent.private_file(path)
            path.chmod(0o600)
            agent.private_file(path)
            link = Path(directory) / "link"
            link.symlink_to(path)
            with self.assertRaises(agent.Stop):
                agent.private_file(link)


if __name__ == "__main__":
    unittest.main()
