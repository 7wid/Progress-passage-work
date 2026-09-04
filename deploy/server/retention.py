#!/usr/bin/env python3
"""Safely enforce local production backup and deployment-audit retention."""

from __future__ import annotations

import argparse
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import gzip
import os
from pathlib import Path
import re
import stat
import sys

if sys.platform == "linux":
    import fcntl
    import pwd


DEPLOY_DIR = Path("/home/Ted_Kasane/tech-request-prod-deploy")
BACKUP_DIR = Path("/data/volumes/tech-request-prod/backups")
HISTORY_DIR = DEPLOY_DIR / "history"
LOCK_FILE = DEPLOY_DIR / "deploy.lock"
ENABLE_FILE = DEPLOY_DIR / "retention.enabled"
DEPLOY_USER = "Ted_Kasane"
DATABASE_DAYS = 30
DATABASE_MINIMUM = 14
AUDIT_DAYS = 180

DATABASE_NAME = re.compile(
    r"mysql-(?:pre-cd-)?\d{8}T\d{6}(?:\.\d+)?Z(?:\.[A-Za-z0-9]+)?\.sql\.gz\Z"
)
AUDIT_NAME = re.compile(
    r"(?:pull-deploy-\d{8}T\d{6}(?:\.\d+)?Z\.log"
    r"|release-\d{8}T\d{6}(?:\.\d+)?Z\.env"
    r"|pull-state-before-resume-\d{8}T\d{6}(?:\.\d+)?Z\.json)\Z"
)


class RetentionError(RuntimeError):
    """A retention safety check failed."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RetentionError(message)


@dataclass(frozen=True)
class FileRecord:
    path: Path
    modified: datetime
    size: int
    device: int
    inode: int
    modified_ns: int


@dataclass(frozen=True)
class RetentionPlan:
    database_delete: tuple[FileRecord, ...]
    audit_delete: tuple[FileRecord, ...]
    database_protected: tuple[FileRecord, ...]

    @property
    def delete(self) -> tuple[FileRecord, ...]:
        return self.database_delete + self.audit_delete


def inspect_directory(path: Path, expected_uid: int | None = None) -> None:
    metadata = path.lstat()
    require(stat.S_ISDIR(metadata.st_mode), f"Not a directory: {path}")
    require(not path.is_symlink(), f"Directory must not be a symlink: {path}")
    require(metadata.st_mode & 0o022 == 0, f"Directory is group/world writable: {path}")
    if expected_uid is not None:
        require(metadata.st_uid == expected_uid, f"Unexpected directory owner: {path}")


@contextmanager
def locked(path: Path, expected_uid: int):
    flags = os.O_CREAT | os.O_RDWR
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    descriptor = os.open(path, flags, 0o600)
    try:
        metadata = os.fstat(descriptor)
        require(stat.S_ISREG(metadata.st_mode), "Deployment lock must be a regular file")
        require(metadata.st_uid == expected_uid, "Deployment lock has the wrong owner")
        require(metadata.st_mode & 0o777 == 0o600, "Deployment lock must have mode 600")
        try:
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            yield False
            return
        yield True
    finally:
        os.close(descriptor)


def scan(path: Path, pattern: re.Pattern[str]) -> list[FileRecord]:
    records: list[FileRecord] = []
    for entry in path.iterdir():
        if pattern.fullmatch(entry.name) is None:
            continue
        metadata = entry.lstat()
        require(stat.S_ISREG(metadata.st_mode), f"Retention target is not a regular file: {entry}")
        require(metadata.st_nlink == 1, f"Retention target has multiple hard links: {entry}")
        records.append(
            FileRecord(
                path=entry,
                modified=datetime.fromtimestamp(metadata.st_mtime, timezone.utc),
                size=metadata.st_size,
                device=metadata.st_dev,
                inode=metadata.st_ino,
                modified_ns=metadata.st_mtime_ns,
            )
        )
    return records


def build_plan(backup_dir: Path, history_dir: Path, now: datetime) -> RetentionPlan:
    require(now.tzinfo is not None, "Current time must be timezone-aware")
    database = sorted(scan(backup_dir, DATABASE_NAME), key=lambda value: value.modified, reverse=True)
    audit = sorted(scan(history_dir, AUDIT_NAME), key=lambda value: value.modified, reverse=True)
    protected = tuple(database[:DATABASE_MINIMUM])
    database_cutoff = now - timedelta(days=DATABASE_DAYS)
    audit_cutoff = now - timedelta(days=AUDIT_DAYS)
    database_delete = tuple(
        value for value in database[DATABASE_MINIMUM:] if value.modified < database_cutoff
    )
    audit_delete = tuple(value for value in audit if value.modified < audit_cutoff)
    return RetentionPlan(database_delete, audit_delete, protected)


def validate_file(record: FileRecord, expected_uid: int) -> None:
    metadata = record.path.lstat()
    require(stat.S_ISREG(metadata.st_mode), f"Target changed type: {record.path}")
    require(metadata.st_nlink == 1, f"Target gained a hard link: {record.path}")
    require(metadata.st_uid == expected_uid, f"Unexpected target owner: {record.path}")
    require(metadata.st_mode & 0o077 == 0, f"Target permissions are not private: {record.path}")
    require(
        (metadata.st_dev, metadata.st_ino, metadata.st_size, metadata.st_mtime_ns)
        == (record.device, record.inode, record.size, record.modified_ns),
        f"Target changed after planning: {record.path}",
    )


def validate_gzip(record: FileRecord) -> None:
    with gzip.open(record.path, "rb") as stream:
        while stream.read(1024 * 1024):
            pass


def enabled(path: Path, expected_uid: int) -> None:
    metadata = path.lstat()
    require(stat.S_ISREG(metadata.st_mode), "Retention enable marker must be a regular file")
    require(metadata.st_uid == expected_uid, "Retention enable marker has the wrong owner")
    require(metadata.st_mode & 0o777 == 0o600, "Retention enable marker must have mode 600")
    require(path.read_bytes() == b"enabled\n", "Retention enable marker has invalid content")


def delete_record(record: FileRecord, expected_uid: int) -> None:
    validate_file(record, expected_uid)
    record.path.unlink()
    print(f"[DELETE] {record.path.name} ({record.size} bytes)")


def describe(plan: RetentionPlan) -> None:
    print(
        f"[PLAN] database_delete={len(plan.database_delete)} "
        f"audit_delete={len(plan.audit_delete)} "
        f"database_protected={len(plan.database_protected)}"
    )
    for record in plan.database_delete:
        print(f"[CANDIDATE database] {record.modified.isoformat()} {record.size} {record.path.name}")
    for record in plan.audit_delete:
        print(f"[CANDIDATE audit] {record.modified.isoformat()} {record.size} {record.path.name}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    try:
        require(sys.version_info >= (3, 9), "Python 3.9 or newer is required")
        require(sys.platform == "linux", "Run retention only on the approved Linux server")
        deploy_uid = pwd.getpwnam(DEPLOY_USER).pw_uid
        require(os.geteuid() == deploy_uid, f"Run as {DEPLOY_USER}, not root")
        os.umask(0o077)
        inspect_directory(DEPLOY_DIR, deploy_uid)
        inspect_directory(HISTORY_DIR, deploy_uid)
        inspect_directory(BACKUP_DIR, deploy_uid)
        with locked(LOCK_FILE, deploy_uid) as acquired:
            if not acquired:
                print("[SKIP] A deployment currently holds deploy.lock")
                return 0
            plan = build_plan(BACKUP_DIR, HISTORY_DIR, datetime.now(timezone.utc))
            describe(plan)
            if not args.apply:
                print("[DRY-RUN] No files were removed")
                return 0
            enabled(ENABLE_FILE, deploy_uid)
            for record in plan.delete:
                validate_file(record, deploy_uid)
            if plan.database_delete:
                require(
                    len(plan.database_protected) == DATABASE_MINIMUM,
                    "Refusing database cleanup without the minimum protected backups",
                )
                for record in plan.database_protected:
                    validate_file(record, deploy_uid)
                    validate_gzip(record)
            for record in plan.database_delete:
                delete_record(record, deploy_uid)
            for record in plan.audit_delete:
                delete_record(record, deploy_uid)
            print(f"[OK] Removed {len(plan.delete)} expired files")
            return 0
    except (OSError, RetentionError, ValueError) as error:
        print("[STOP] Retention failed: " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
