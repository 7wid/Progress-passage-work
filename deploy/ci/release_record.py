"""Generate a data-only GHCR release image context; never deploy or contact Docker."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re

REPOSITORY = "7wid/Progress-passage-work"
IMAGE_PREFIX = "ghcr.io/7wid/progress-passage-work-"
RELEASE_LABEL = "xyz.gxutech.progress-passage-work.release.v1"
MEDIA_TYPES = {
    "application/vnd.oci.image.index.v1+json",
    "application/vnd.oci.image.manifest.v1+json",
    "application/vnd.docker.distribution.manifest.list.v2+json",
    "application/vnd.docker.distribution.manifest.v2+json",
}


def positive_integer(value, name):
    if type(value) is not int or value < 1:
        raise ValueError(f"{name} must be a positive integer")
    return value


def manifest_digest(manifest):
    """Read the TOP-LEVEL digest from Buildx '{{json .Manifest}}', not --raw."""
    if not isinstance(manifest, dict):
        raise ValueError("manifest must be an object")
    if manifest.get("schemaVersion") != 2:
        raise ValueError("unsupported manifest schema")
    if manifest.get("mediaType") not in MEDIA_TYPES:
        raise ValueError("unsupported manifest media type")
    digest = manifest.get("digest")
    if not isinstance(digest, str) or not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
        raise ValueError("missing or invalid top-level sha256 digest")
    return digest


def build_record(*, commit, sequence, ci_run_id, publish_run_id,
                 publish_run_attempt, backend_manifest, frontend_manifest,
                 created_at=None):
    if not isinstance(commit, str) or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("commit must be exactly 40 lowercase hexadecimal characters")
    timestamp = created_at or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    if not isinstance(timestamp, str) or not re.fullmatch(
        r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", timestamp
    ):
        raise ValueError("created_at must be a UTC timestamp")
    datetime.strptime(timestamp, "%Y-%m-%dT%H:%M:%SZ")
    return {
        "schema_version": 1,
        "repository": REPOSITORY,
        "commit": commit,
        "sequence": positive_integer(sequence, "sequence"),
        "ci_run_id": positive_integer(ci_run_id, "ci_run_id"),
        "publish_run_id": positive_integer(publish_run_id, "publish_run_id"),
        "publish_run_attempt": positive_integer(publish_run_attempt, "publish_run_attempt"),
        "created_at": timestamp,
        "images": {
            "backend": IMAGE_PREFIX + "backend@" + manifest_digest(backend_manifest),
            "frontend": IMAGE_PREFIX + "frontend@" + manifest_digest(frontend_manifest),
        },
    }


def write_context(output_dir, record):
    """Create a NEW build context. Caller must supply build_record() output."""
    compact = json.dumps(record, ensure_ascii=True, separators=(",", ":"), sort_keys=True)
    # The inner JSON is data; quoting it once more escapes Dockerfile quotes.
    # No RUN/CMD/ENTRYPOINT, shell expansion, credentials, or remote base image.
    dockerfile = (
        "FROM scratch\n"
        "COPY release.json /release.json\n"
        f'LABEL org.opencontainers.image.source="https://github.com/{REPOSITORY}"\n'
        f'LABEL org.opencontainers.image.revision={json.dumps(record["commit"])}\n'
        f"LABEL {RELEASE_LABEL}={json.dumps(compact)}\n"
    )
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=False)
    (output_dir / "release.json").write_text(compact + "\n", encoding="utf-8", newline="\n")
    (output_dir / "Dockerfile").write_text(dockerfile, encoding="utf-8", newline="\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--commit", required=True)
    for name in ("sequence", "ci-run-id", "publish-run-id", "publish-run-attempt"):
        parser.add_argument("--" + name, required=True, type=int)
    for name in ("backend-manifest", "frontend-manifest", "output-dir"):
        parser.add_argument("--" + name, required=True, type=Path)
    args = parser.parse_args()
    try:
        record = build_record(
            commit=args.commit, sequence=args.sequence, ci_run_id=args.ci_run_id,
            publish_run_id=args.publish_run_id, publish_run_attempt=args.publish_run_attempt,
            backend_manifest=json.loads(args.backend_manifest.read_text(encoding="utf-8")),
            frontend_manifest=json.loads(args.frontend_manifest.read_text(encoding="utf-8")),
        )
        write_context(args.output_dir, record)
    except (ValueError, OSError) as error:
        parser.exit(1, f"Release record rejected: {error}\n")
    print(f"Release record prepared for {record['commit']} (sequence {record['sequence']})")


if __name__ == "__main__":
    main()
