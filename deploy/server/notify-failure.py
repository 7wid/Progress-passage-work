#!/usr/bin/env python3
"""Send rate-limited production CD alerts using systemd credentials."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
from email.message import EmailMessage
from email.utils import getaddresses
import hashlib
import json
import os
from pathlib import Path
import re
import smtplib
import socket
import ssl
import subprocess
import sys
import tempfile
import time


DEPLOY_DIR = Path("/home/Ted_Kasane/tech-request-prod-deploy")
STATE_FILE = DEPLOY_DIR / "pull-agent-state.json"
RATE_LIMIT_SECONDS = 3600
SMTP_HOST = "smtp.qq.com"
SMTP_PORT = 465
SOURCE_PATTERN = re.compile(r"[a-z0-9][a-z0-9-]{0,31}\Z")


class AlertError(RuntimeError):
    """A safe, user-facing alert configuration or delivery error."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AlertError(message)


def read_credential(directory: Path, name: str) -> str:
    path = directory / name
    require(path.is_file() and not path.is_symlink(), f"Missing credential: {name}")
    raw = path.read_bytes()
    require(0 < len(raw) <= 4096, f"Invalid credential size: {name}")
    try:
        value = raw.decode("utf-8")
    except UnicodeDecodeError as error:
        raise AlertError(f"Credential is not UTF-8: {name}") from error
    if value.endswith("\r\n"):
        value = value[:-2]
    elif value.endswith("\n"):
        value = value[:-1]
    require("\n" not in value and "\r" not in value, f"Credential must be one line: {name}")
    require(bool(value), f"Credential is empty: {name}")
    require(value.strip() == value, f"Credential has surrounding whitespace: {name}")
    return value


def parse_recipients(value: str) -> list[str]:
    parsed = [address for _, address in getaddresses([value])]
    require(parsed, "No alert recipients configured")
    for address in parsed:
        require(
            re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", address) is not None,
            "Invalid alert recipient",
        )
    return parsed


def safe_state(path: Path = STATE_FILE) -> dict[str, object]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        require(isinstance(value, dict), "Invalid pull-agent state")
    except (OSError, ValueError, AlertError):
        return {"status": "unavailable", "sequence": None, "commit": None}
    token = value.get("attempt") or value.get("last") or value.get("floor") or {}
    if not isinstance(token, dict):
        token = {}
    sequence = token.get("sequence")
    commit = token.get("commit")
    return {
        "status": value.get("status", "unknown"),
        "sequence": sequence if isinstance(sequence, int) and not isinstance(sequence, bool) else None,
        "commit": commit if isinstance(commit, str) and re.fullmatch(r"[0-9a-f]{40}", commit) else None,
    }


def systemd_property(name: str) -> str:
    result = subprocess.run(
        ["systemctl", "show", "tech-request-cd.service", f"--property={name}", "--value"],
        check=False,
        capture_output=True,
        text=True,
        timeout=10,
    )
    if result.returncode != 0:
        return "unavailable"
    value = result.stdout.strip()
    return value if value and "\n" not in value else "unavailable"


def alert_details(source: str, state_path: Path = STATE_FILE) -> dict[str, object]:
    details = safe_state(state_path)
    details.update(
        {
            "source": source,
            "host": socket.getfqdn(),
            "time_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "unit_result": systemd_property("Result") if source == "deployment" else "n/a",
            "exit_status": systemd_property("ExecMainStatus") if source == "deployment" else "n/a",
        }
    )
    return details


def fingerprint(details: dict[str, object]) -> str:
    stable = {key: details.get(key) for key in (
        "source", "host", "status", "sequence", "commit", "unit_result", "exit_status"
    )}
    return hashlib.sha256(
        json.dumps(stable, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()


def rate_limited(path: Path, current_fingerprint: str, now: float) -> bool:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        return (
            value.get("fingerprint") == current_fingerprint
            and isinstance(value.get("sent_at"), (int, float))
            and now - float(value["sent_at"]) < RATE_LIMIT_SECONDS
        )
    except (OSError, ValueError, TypeError):
        return False


def save_rate_state(path: Path, current_fingerprint: str, now: float) -> None:
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=".alert-state.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump({"fingerprint": current_fingerprint, "sent_at": now}, stream)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.chmod(temporary, 0o600)
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def build_message(username: str, recipients: list[str], source: str,
                  details: dict[str, object], test: bool) -> EmailMessage:
    label = "TEST" if test else "FAILURE"
    subject = f"[{label}] Tech Request production CD {source} on {details['host']}"
    message = EmailMessage()
    message["From"] = username
    message["To"] = ", ".join(recipients)
    message["Subject"] = subject
    message.set_content(
        "\n".join(
            [
                "Tech Request production CD alert",
                f"mode: {label}",
                f"source: {source}",
                f"host: {details['host']}",
                f"time_utc: {details['time_utc']}",
                f"state: {details['status']}",
                f"sequence: {details['sequence']}",
                f"commit: {details['commit']}",
                f"unit_result: {details['unit_result']}",
                f"exit_status: {details['exit_status']}",
                "",
                "Inspect on the production server:",
                "sudo systemctl disable --now tech-request-cd.timer",
                "sudo systemctl status tech-request-cd.service --no-pager",
                "sudo journalctl -u tech-request-cd.service -n 200 --no-pager",
                "",
                "Do not delete state/lock files or restore a database before review.",
            ]
        )
        + "\n"
    )
    return message


def send_message(username: str, password: str, message: EmailMessage) -> None:
    context = ssl.create_default_context()
    with smtplib.SMTP_SSL(SMTP_HOST, SMTP_PORT, timeout=15, context=context) as client:
        client.login(username, password)
        client.send_message(message)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--test", action="store_true")
    parser.add_argument("--credential-directory", type=Path)
    parser.add_argument("--state-file", type=Path, default=STATE_FILE)
    parser.add_argument("--rate-state", type=Path)
    args = parser.parse_args()
    try:
        require(SOURCE_PATTERN.fullmatch(args.source) is not None, "Invalid alert source")
        directory = args.credential_directory
        if directory is None:
            credential_directory = os.environ.get("CREDENTIALS_DIRECTORY")
            require(bool(credential_directory), "CREDENTIALS_DIRECTORY is not set")
            directory = Path(credential_directory)
        username = read_credential(directory, "smtp-username")
        password = read_credential(directory, "smtp-password")
        recipients = parse_recipients(read_credential(directory, "alert-recipients"))
        require(
            re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", username) is not None,
            "Invalid SMTP username",
        )
        details = alert_details(args.source, args.state_file)
        current_fingerprint = fingerprint(details)
        rate_state = args.rate_state or DEPLOY_DIR / f"alert-state-{args.source}.json"
        now = time.time()
        if not args.test and rate_limited(rate_state, current_fingerprint, now):
            print("[SKIP] Duplicate alert is within the one-hour rate limit")
            return 0
        message = build_message(username, recipients, args.source, details, args.test)
        send_message(username, password, message)
        if not args.test:
            save_rate_state(rate_state, current_fingerprint, now)
        print("[OK] Alert email sent")
        return 0
    except (AlertError, OSError, smtplib.SMTPException, subprocess.SubprocessError) as error:
        print("[STOP] Alert delivery failed: " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
