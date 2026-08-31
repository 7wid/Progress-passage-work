import copy
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_record import (  # noqa: E402
    IMAGE_PREFIX, MEDIA_TYPES, RELEASE_LABEL, build_record, manifest_digest, write_context,
)

SCRIPT = Path(__file__).resolve().parents[1] / "release_record.py"


def manifest(character="a"):
    return {
        "schemaVersion": 2,
        "mediaType": "application/vnd.oci.image.index.v1+json",
        "digest": "sha256:" + character * 64,
        "manifests": [{"digest": "sha256:" + "c" * 64}],
    }


def inputs():
    return {
        "commit": "d" * 40,
        "sequence": 60,
        "ci_run_id": 1001,
        "publish_run_id": 1002,
        "publish_run_attempt": 1,
        "created_at": "2026-08-31T06:00:00Z",
        "backend_manifest": manifest(),
        "frontend_manifest": manifest("b"),
    }


class ReleaseRecordTests(unittest.TestCase):
    def test_pins_both_images_to_root_digests(self):
        record = build_record(**inputs())
        self.assertEqual(record["images"], {
            "backend": IMAGE_PREFIX + "backend@sha256:" + "a" * 64,
            "frontend": IMAGE_PREFIX + "frontend@sha256:" + "b" * 64,
        })
        self.assertEqual(record["schema_version"], 1)
        self.assertEqual(record["sequence"], 60)

    def test_all_supported_manifest_media_types(self):
        for media_type in MEDIA_TYPES:
            with self.subTest(media_type=media_type):
                payload = manifest()
                payload["mediaType"] = media_type
                self.assertEqual(manifest_digest(payload), "sha256:" + "a" * 64)

    def test_rejects_invalid_commits(self):
        for commit in ("", "main", "d" * 39, "D" * 40, "d" * 40 + "\n",
                       "$(id)", "d" * 40 + ";id", None):
            with self.subTest(commit=commit), self.assertRaises(ValueError):
                build_record(**dict(inputs(), commit=commit))

    def test_rejects_invalid_identifiers(self):
        for name in ("sequence", "ci_run_id", "publish_run_id", "publish_run_attempt"):
            for value in (0, -1, True, "1", 1.5, None):
                with self.subTest(name=name, value=value), self.assertRaises(ValueError):
                    build_record(**dict(inputs(), **{name: value}))

    def test_rejects_missing_or_malformed_top_level_digest(self):
        for digest in (None, "", "latest", "sha256:" + "a" * 63,
                       "sha256:" + "A" * 64, "sha256:" + "a" * 64 + "\n"):
            with self.subTest(digest=digest), self.assertRaises(ValueError):
                manifest_digest(dict(manifest(), digest=digest))
        payload = manifest()
        del payload["digest"]
        with self.assertRaises(ValueError):
            manifest_digest(payload)  # A valid child digest must not be used instead.

    def test_rejects_wrong_manifest_structure(self):
        for payload in (None, [], {}, dict(manifest(), schemaVersion=1),
                        dict(manifest(), mediaType="application/json")):
            with self.subTest(payload=payload), self.assertRaises(ValueError):
                manifest_digest(payload)

    def test_requires_both_components(self):
        for component in ("backend_manifest", "frontend_manifest"):
            with self.subTest(component=component), self.assertRaises(ValueError):
                build_record(**dict(inputs(), **{component: None}))

    def test_rejects_invalid_dates(self):
        for value in ("yesterday", "2026-02-30T00:00:00Z", "2026-08-31T06:00:00+08:00",
                      '2026-08-31T06:00:00Z"\nRUN id'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                build_record(**dict(inputs(), created_at=value))

    def test_json_file_and_docker_label_match(self):
        record = build_record(**inputs())
        original = copy.deepcopy(record)
        with tempfile.TemporaryDirectory() as temporary:
            context = Path(temporary) / "context"
            write_context(context, record)
            self.assertEqual(json.loads((context / "release.json").read_text()), record)
            lines = (context / "Dockerfile").read_text().splitlines()
            label = next(line for line in lines if line.startswith(f"LABEL {RELEASE_LABEL}="))
            self.assertEqual(json.loads(json.loads(label.split("=", 1)[1])), record)
            self.assertEqual([line.split()[0] for line in lines],
                             ["FROM", "COPY", "LABEL", "LABEL", "LABEL"])
            with self.assertRaises(FileExistsError):
                write_context(context, record)
        self.assertEqual(record, original)

    def test_cli_success_and_invalid_input_no_output(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            for component, character in (("backend", "a"), ("frontend", "b")):
                (folder / f"{component}.json").write_text(json.dumps(manifest(character)))
            command = [
                sys.executable, str(SCRIPT), "--sequence", "60", "--ci-run-id", "1001",
                "--publish-run-id", "1002", "--publish-run-attempt", "1",
                "--backend-manifest", str(folder / "backend.json"),
                "--frontend-manifest", str(folder / "frontend.json"),
            ]
            good = subprocess.run(
                command + ["--commit", "d" * 40, "--output-dir", str(folder / "good")],
                capture_output=True, text=True, timeout=15,
            )
            self.assertEqual(good.returncode, 0, good.stderr)
            bad = subprocess.run(
                command + ["--commit", "main", "--output-dir", str(folder / "bad")],
                capture_output=True, text=True, timeout=15,
            )
            self.assertNotEqual(bad.returncode, 0)
            self.assertFalse((folder / "bad").exists())

    @unittest.skipUnless(os.environ.get("TEST_RELEASE_DOCKER") == "1",
                         "Docker integration runs in CI; enable TEST_RELEASE_DOCKER=1 locally")
    def test_docker_build_and_inspect_round_trip_without_running_container(self):
        self.assertIsNotNone(shutil.which("docker"), "Docker integration requested but unavailable")
        record = build_record(**inputs())
        tag = "release-record-test:" + uuid.uuid4().hex
        with tempfile.TemporaryDirectory() as temporary:
            context = Path(temporary) / "context"
            write_context(context, record)
            subprocess.run(
                ["docker", "build", "--network=none", "--tag", tag, str(context)],
                check=True, timeout=120, capture_output=True, text=True,
            )
            result = subprocess.run(
                ["docker", "image", "inspect", tag], check=True, timeout=20,
                capture_output=True, text=True,
            )
            config = json.loads(result.stdout)[0]["Config"]
            self.assertEqual(json.loads(config["Labels"][RELEASE_LABEL]), record)
            self.assertFalse(config.get("Cmd"))
            self.assertFalse(config.get("Entrypoint"))
            # The isolated GitHub runner is discarded after the job; no docker run/prune.


if __name__ == "__main__":
    unittest.main()
