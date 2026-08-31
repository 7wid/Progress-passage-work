#!/usr/bin/env python3
"""One-shot GHCR pull CD. Defaults to dry-run; no SSH, Git, build or shell eval."""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tempfile

DEPLOY_DIR = Path("/home/Ted_Kasane/tech-request-prod-deploy")
IMAGE_STORE = Path("/var/lib/containerd")
REPOSITORY = "7wid/Progress-passage-work"
PREFIX = "ghcr.io/7wid/progress-passage-work-"
MARKER = PREFIX + "release"
LABEL = "xyz.gxutech.progress-passage-work.release.v1"
MIN_FREE = 5 * 1024**3
DIGEST = r"sha256:[0-9a-f]{64}"
SHA = r"[0-9a-f]{40}"
DOCKER = ["docker", "--host", "unix:///var/run/docker.sock"]


class Stop(RuntimeError):
    pass


def require(condition, message):
    if not condition:
        raise Stop(message)


def matches(pattern, value):
    return isinstance(value, str) and re.fullmatch(pattern, value) is not None


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "Duplicate JSON key")
        result[key] = value
    return result


def parse_json(text):
    require(isinstance(text, str), "JSON must be text")
    require(len(text) <= 2_000_000, "JSON exceeds size limit")
    try:
        return json.loads(text, object_pairs_hook=unique_object,
                          parse_constant=lambda value: (_ for _ in ()).throw(Stop("Non-finite JSON")))
    except (ValueError, TypeError) as error:
        raise Stop("Invalid JSON") from error


def image_pair(images, legacy=False):
    require(isinstance(images, dict) and set(images) == {"backend", "frontend"}, "Invalid image pair")
    for component, ref in images.items():
        suffix = "(?:@" + DIGEST + "|:sha-" + SHA + ")" if legacy else "@" + DIGEST
        require(matches(re.escape(PREFIX + component) + suffix, ref), "Invalid " + component + " image")
    return images


def validate_record(record):
    keys = {"schema_version", "repository", "commit", "sequence", "ci_run_id",
            "publish_run_id", "publish_run_attempt", "created_at", "images"}
    require(isinstance(record, dict) and set(record) == keys, "Unsupported record fields")
    require(type(record["schema_version"]) is int and record["schema_version"] == 1, "Unsupported schema")
    require(record["repository"] == REPOSITORY, "Repository mismatch")
    require(matches(SHA, record["commit"]), "Invalid commit")
    for field in ("sequence", "ci_run_id", "publish_run_id", "publish_run_attempt"):
        require(type(record[field]) is int and record[field] > 0, "Invalid " + field)
    require(matches(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", record["created_at"]), "Invalid UTC time")
    try:
        datetime.strptime(record["created_at"], "%Y-%m-%dT%H:%M:%SZ")
    except ValueError as error:
        raise Stop("Invalid UTC date") from error
    image_pair(record["images"])
    return record


def token(record, digest):
    require(matches(DIGEST, digest), "Invalid metadata digest")
    return {key: record[key] for key in ("sequence", "commit", "images")} | {"metadata_digest": digest}


def validate_token(value):
    require(isinstance(value, dict) and set(value) == {"sequence", "commit", "images", "metadata_digest"},
            "Invalid stored release token")
    require(type(value["sequence"]) is int and value["sequence"] > 0, "Invalid stored sequence")
    require(matches(SHA, value["commit"]) and matches(DIGEST, value["metadata_digest"]), "Invalid stored identity")
    image_pair(value["images"])
    return value


def identity(value):
    return value["sequence"], value["commit"], value["images"]


def decision(candidate, state):
    """Fail closed on interrupted attempts, manual drift is checked separately."""
    if state is None:
        return "uninitialized"
    require(state["status"] == "ready", "Previous attempt failed/interrupted; inspect logs before --resume")
    require(candidate["commit"] not in state["blocked_commits"], "This failed commit is blocked")
    floor = state["floor"]
    require(candidate["sequence"] >= floor["sequence"], "Candidate predates the approved baseline")
    if candidate["sequence"] == floor["sequence"]:
        require(identity(candidate) == identity(floor), "Baseline sequence reused with different content")
    last = state["last"]
    if last:
        require(candidate["sequence"] >= last["sequence"], "Refusing automatic downgrade")
        if candidate["sequence"] == last["sequence"]:
            require(identity(candidate) == identity(last), "Sequence reused with different content")
            return "unchanged"
        require(candidate["commit"] != last["commit"], "Same commit published under a different sequence")
    return "deploy"


def private_file(path):
    info = path.lstat()
    require(stat.S_ISREG(info.st_mode), "Not a regular file: " + path.name)
    require(stat.S_IMODE(info.st_mode) == 0o600, "Expected mode 600: " + path.name)
    if hasattr(os, "geteuid"):
        require(info.st_uid == os.geteuid(), "Unexpected file owner: " + path.name)


def atomic_json(path, value):
    fd, name = tempfile.mkstemp(prefix="." + path.name + ".", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as stream:
            json.dump(value, stream, sort_keys=True)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(name, path)
        if os.name == "posix":
            directory = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
    finally:
        if os.path.exists(name):
            os.unlink(name)  # Only this invocation's exact mkstemp file.


@contextmanager
def locked(path):
    import fcntl  # Linux only; keep pure validation tests importable on Windows.
    descriptor = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    try:
        private_file(path)
        try:
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise Stop("Another pull-agent invocation is running") from error
        yield
    finally:
        os.close(descriptor)


def validate_state(state):
    keys = {"schema_version", "status", "floor", "last", "expected_images",
            "expected_runtime", "attempt", "blocked_commits"}
    require(isinstance(state, dict) and set(state) == keys, "Invalid state; do not delete it to bypass checks")
    require(type(state["schema_version"]) is int and state["schema_version"] == 1, "Invalid state schema")
    require(state["status"] in ("ready", "in_progress", "failed"), "Invalid state status")
    validate_token(state["floor"])
    if state["last"] is not None:
        validate_token(state["last"])
        require(state["last"]["sequence"] >= state["floor"]["sequence"], "Invalid state ordering")
        require(state["expected_images"] == state["last"]["images"], "Stored deployed images mismatch")
    if state["attempt"] is not None:
        validate_token(state["attempt"])
    require((state["attempt"] is None) == (state["status"] == "ready"), "Invalid attempt state")
    image_pair(state["expected_images"], legacy=True)
    runtime = state["expected_runtime"]
    require(isinstance(runtime, dict) and set(runtime) == {"backend", "frontend", "mysql"}, "Invalid runtime state")
    for item in runtime.values():
        require(isinstance(item, dict) and set(item) == {"container_id", "image_id"}, "Invalid runtime entry")
        require(matches(r"[0-9a-f]{64}", item["container_id"]) and matches(DIGEST, item["image_id"]),
                "Invalid runtime identity")
    require(isinstance(state["blocked_commits"], list) and
            all(matches(SHA, item) for item in state["blocked_commits"]), "Invalid blocked commits")
    return state


def manifest_platforms(payload):
    entries = payload if isinstance(payload, list) else [payload]
    result = set()
    for entry in entries:
        require(isinstance(entry, dict), "Invalid verbose manifest")
        descriptor = entry.get("Descriptor", {})
        require(isinstance(descriptor, dict), "Invalid descriptor")
        platform = descriptor.get("platform") or entry.get("Platform") or {}
        require(isinstance(platform, dict), "Invalid platform")
        if isinstance(platform.get("os"), str) and isinstance(platform.get("architecture"), str):
            result.add(platform["os"] + "/" + platform["architecture"])
    return result


def marker_digest(payload):
    require(isinstance(payload, dict), "Release marker must be a single-platform image")
    descriptor = payload.get("Descriptor", {})
    require(isinstance(descriptor, dict), "Invalid marker descriptor")
    digest = descriptor.get("digest") or payload.get("Digest")
    require(matches(DIGEST, digest), "Missing marker digest")
    require("linux/amd64" in manifest_platforms(payload), "Marker platform is not linux/amd64")
    return digest


class Agent:
    def __init__(self, root=DEPLOY_DIR, image_store=IMAGE_STORE):
        self.root = Path(root)
        self.image_store = Path(image_store)
        self.state_path = self.root / "pull-agent-state.json"

    def command(self, args, seconds=45):
        try:
            result = subprocess.run(args, cwd=self.root, check=True, capture_output=True,
                                    text=True, timeout=seconds)
            return result.stdout
        except subprocess.CalledProcessError as error:
            # Commands never include production environment contents or credentials.
            raise Stop(f"Command failed ({error.returncode}): {' '.join(args[:4])}; "
                       + error.stderr[-1500:]) from error
        except subprocess.TimeoutExpired as error:
            raise Stop(f"Command timed out after {seconds}s: {' '.join(args[:4])}") from error

    def docker(self, *args, seconds=45):
        return self.command(DOCKER + list(args), seconds)

    def environment_snapshot(self):
        path = self.root / "release.env"
        private_file(path)
        raw = path.read_bytes()
        require(len(raw) <= 8192, "release.env is unexpectedly large")
        images = {}
        for line in raw.decode("utf-8").splitlines():
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            key, separator, value = line.partition("=")
            require(separator and key in ("BACKEND_IMAGE", "FRONTEND_IMAGE"), "Unexpected release.env entry")
            component = key.removesuffix("_IMAGE").lower()
            require(component not in images, "Duplicate release.env entry")
            images[component] = value
        return image_pair(images, legacy=True), hashlib.sha256(raw).hexdigest()

    def runtime_snapshot(self, images):
        template = "{{.Id}} {{.Image}} {{.Config.Image}} {{.State.Running}} {{if .State.Health}}{{.State.Health.Status}}{{end}}"
        names = ["tech-request-prod-" + c + "-1" for c in ("backend", "frontend", "mysql")]
        lines = self.docker("inspect", "--format", template, *names).splitlines()
        require(len(lines) == 3, "Missing production containers")
        snapshot = {}
        for component, line in zip(("backend", "frontend", "mysql"), lines):
            fields = line.split()
            require(len(fields) == 5, "Missing runtime health information")
            container_id, image_id, ref, running, health = fields
            require(matches(r"[0-9a-f]{64}", container_id) and matches(DIGEST, image_id), "Invalid Docker identity")
            require(running == "true" and health == "healthy", component + " is not healthy")
            if component != "mysql":
                require(ref == images[component], component + " runtime differs from release.env")
            snapshot[component] = {"container_id": container_id, "image_id": image_id}
        return snapshot

    def fetch_record(self, digest=None):
        if digest is None:
            payload = parse_json(self.docker("manifest", "inspect", "--verbose", MARKER + ":production"))
            digest = marker_digest(payload)
        require(matches(DIGEST, digest), "Invalid record digest")
        require(shutil.disk_usage(self.image_store).free >= MIN_FREE, "Image store has less than 5 GiB free")
        ref = MARKER + "@" + digest
        # Only the tiny, digest-pinned marker is downloaded in dry-run.
        self.docker("pull", ref, seconds=90)
        records = parse_json(self.docker("image", "inspect", ref))
        require(isinstance(records, list) and len(records) == 1 and isinstance(records[0], dict), "Invalid marker image")
        image = records[0]
        require(isinstance(image.get("RepoDigests"), list), "Missing local marker digests")
        require(ref in image.get("RepoDigests", []), "Local marker digest mismatch")
        require(image.get("Os") == "linux" and image.get("Architecture") == "amd64", "Marker platform mismatch")
        config = image.get("Config") or {}
        require(isinstance(config, dict), "Invalid marker config")
        require(not config.get("Cmd") and not config.get("Entrypoint"), "Marker unexpectedly executable")
        labels = config.get("Labels") or {}
        require(isinstance(labels, dict), "Invalid marker labels")
        raw = labels.get(LABEL)
        require(isinstance(raw, str) and len(raw) <= 16384, "Missing/oversized release record label")
        record = validate_record(parse_json(raw))
        require(labels.get("org.opencontainers.image.source") == "https://github.com/" + REPOSITORY, "Marker source mismatch")
        require(labels.get("org.opencontainers.image.revision") == record["commit"], "Marker revision mismatch")
        return token(record, digest)

    def check_images(self, candidate):
        platform = self.docker("version", "--format", "{{.Server.Os}}/{{.Server.Arch}}").strip()
        require(platform == "linux/amd64", "This deployment is approved only for linux/amd64")
        for component, ref in candidate["images"].items():
            payload = parse_json(self.docker("manifest", "inspect", "--verbose", ref))
            require(platform in manifest_platforms(payload), component + " has no compatible platform")
            print("[OK] manifest " + ref, flush=True)

    def read_state(self):
        if not self.state_path.exists() and not self.state_path.is_symlink():
            return None
        private_file(self.state_path)
        return validate_state(parse_json(self.state_path.read_text(encoding="utf-8")))

    def save_state(self, state):
        validate_state(state)
        atomic_json(self.state_path, state)

    def verify_baseline(self, state):
        images, checksum = self.environment_snapshot()
        runtime = self.runtime_snapshot(images)
        if state is not None:
            require(images == state["expected_images"] and runtime == state["expected_runtime"],
                    "Manual/runtime change detected; pause automation and reconcile state")
        return images, checksum, runtime

    def initialize(self, digest):
        require(self.read_state() is None, "State already exists; initialization never overwrites it")
        candidate = self.fetch_record(digest)
        self.check_images(candidate)
        # Share the manual deployment lock when capturing a baseline.
        with locked(self.root / "deploy.lock"):
            images, _, runtime = self.verify_baseline(None)
            state = {"schema_version": 1, "status": "ready", "floor": candidate,
                     "last": None, "attempt": None, "expected_images": images,
                     "expected_runtime": runtime, "blocked_commits": []}
            self.save_state(state)
        print("[OK] Baseline initialized; no deployment executed")

    def resume(self):
        state = self.read_state()
        require(state is not None and state["status"] in ("failed", "in_progress"), "No interrupted/failed attempt")
        with locked(self.root / "deploy.lock"):
            images, _ = self.environment_snapshot()
            runtime = self.runtime_snapshot(images)
            require(images == state["expected_images"], "Restore prior release.env before resuming")
            require(all(runtime[c]["image_id"] == state["expected_runtime"][c]["image_id"]
                        for c in ("backend", "frontend", "mysql")), "Prior image contents are not restored")
            require(runtime["mysql"] == state["expected_runtime"]["mysql"], "MySQL changed; manual review required")
            archived = self.root / "history" / ("pull-state-before-resume-" + timestamp() + ".json")
            atomic_json(archived, state)
            state["blocked_commits"] = sorted(set(state["blocked_commits"] + [state["attempt"]["commit"]]))
            state["attempt"] = None
            state["status"] = "ready"
            state["expected_runtime"] = runtime
            self.save_state(state)
        print("[OK] Resumed at prior baseline; failed commit remains blocked")

    def deploy(self, candidate, checksum, log_path):
        args = ["/bin/bash", str(self.root / "deploy.sh"), "--digests", candidate["commit"],
                candidate["images"]["backend"], candidate["images"]["frontend"], checksum]
        environment = os.environ.copy()
        environment.pop("DOCKER_CONTEXT", None)
        environment["DOCKER_HOST"] = "unix:///var/run/docker.sock"
        # deploy.sh bounds each Docker phase. systemd stops the entire control group on timeout.
        with log_path.open("x", encoding="utf-8") as stream:
            result = subprocess.run(args, cwd=self.root, env=environment, stdout=stream,
                                    stderr=subprocess.STDOUT, check=False)
        return result.returncode

    def once(self, apply=False):
        state = self.read_state()
        if apply:
            require(state is not None, "Initialize an approved baseline first")
            private_file(self.root / "pull-agent.enabled")
            require((self.root / "pull-agent.enabled").read_bytes() == b"enabled\n",
                    "Explicit enable file must contain enabled followed by a newline")
        if state is not None:
            require(state["status"] == "ready", "Previous attempt failed/interrupted; inspect logs before --resume")
        images, checksum, runtime = self.verify_baseline(state)
        candidate = self.fetch_record()
        action = decision(candidate, state)
        if action == "unchanged":
            print("[SKIP] Already deployed sequence " + str(candidate["sequence"]))
            return
        self.check_images(candidate)
        print(f"[CANDIDATE] sequence={candidate['sequence']} commit={candidate['commit']} "
              f"metadata={candidate['metadata_digest']}", flush=True)
        if not apply:
            print("[DRY-RUN] " + action + "; no app pulls, backup, release.env change or deployment")
            return
        previous = dict(state)
        state = dict(state, status="in_progress", attempt=candidate)
        self.save_state(state)  # Persist BEFORE any potentially mutating deployment.
        log_path = self.root / "history" / ("pull-deploy-" + timestamp() + ".log")
        print("[AUDIT] " + str(log_path), flush=True)
        try:
            result = self.deploy(candidate, checksum, log_path)
            if result == 75:  # Existing manual deployment holds the lock; no mutation occurred.
                self.save_state(previous)
                raise Stop("Deployment lock busy; left baseline unchanged")
            require(result == 0, "Deployment failed; inspect " + str(log_path))
            current_images, _ = self.environment_snapshot()
            require(current_images == candidate["images"], "release.env changed during/after deployment")
            current_runtime = self.runtime_snapshot(current_images)
            require(current_runtime["mysql"] == runtime["mysql"], "MySQL identity changed")
            self.save_state(dict(state, status="ready", attempt=None, last=candidate,
                                 expected_images=current_images, expected_runtime=current_runtime))
        except BaseException:
            # An abrupt kill leaves in_progress on disk and also stops subsequent runs.
            if self.read_state()["status"] == "in_progress":
                self.save_state(dict(state, status="failed"))
            raise
        print("[OK] Applied sequence " + str(candidate["sequence"]), flush=True)


def timestamp():
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--dry-run", action="store_true")
    modes.add_argument("--apply", action="store_true")
    modes.add_argument("--initialize", metavar="APPROVED_METADATA_SHA256")
    modes.add_argument("--status", action="store_true")
    modes.add_argument("--resume", action="store_true")
    args = parser.parse_args()
    try:
        require(sys.version_info >= (3, 9), "Python 3.9 or newer is required")
        require(sys.platform == "linux", "Run the agent only on the approved Linux server")
        import pwd
        require(os.geteuid() == pwd.getpwnam("Ted_Kasane").pw_uid, "Run as Ted_Kasane, not root")
        os.umask(0o077)
        require(DEPLOY_DIR.is_dir() and not DEPLOY_DIR.is_symlink(), "Invalid deployment directory")
        require((DEPLOY_DIR / "history").is_dir(), "Missing audit directory")
        agent = Agent()
        with locked(DEPLOY_DIR / "pull-agent.lock"):
            if args.status:
                print(json.dumps(agent.read_state(), indent=2))
            elif args.initialize:
                agent.initialize(args.initialize)
            elif args.resume:
                agent.resume()
            else:
                agent.once(apply=args.apply)
        return 0
    except (Stop, OSError, ValueError) as error:
        print("[STOP] " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
