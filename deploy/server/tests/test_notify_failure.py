import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock


SOURCE = Path(__file__).resolve().parents[1] / "notify-failure.py"
SPEC = importlib.util.spec_from_file_location("notify_failure", SOURCE)
notify = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(notify)


class CredentialTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def credential(self, name, value):
        (self.root / name).write_text(value, encoding="utf-8")

    def test_reads_one_line_credentials_and_recipients(self):
        self.credential("smtp-username", "sender@example.com\n")
        self.credential("alert-recipients", "one@example.com, two@example.com\n")
        self.assertEqual(notify.read_credential(self.root, "smtp-username"), "sender@example.com")
        self.assertEqual(
            notify.parse_recipients(notify.read_credential(self.root, "alert-recipients")),
            ["one@example.com", "two@example.com"],
        )

    def test_rejects_empty_multiline_and_bad_recipient(self):
        for value in ("", "\n", "first\nsecond\n"):
            with self.subTest(value=repr(value)):
                self.credential("smtp-password", value)
                with self.assertRaises(notify.AlertError):
                    notify.read_credential(self.root, "smtp-password")

    def test_rejects_whitespace_around_authorization_code(self):
        for value in (" code\n", "code \n", "code\t\n"):
            with self.subTest(value=repr(value)):
                self.credential("smtp-password", value)
                with self.assertRaises(notify.AlertError):
                    notify.read_credential(self.root, "smtp-password")
        with self.assertRaises(notify.AlertError):
            notify.parse_recipients("not-an-email")


class AlertTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def test_state_exposes_only_release_identity(self):
        path = self.root / "state.json"
        path.write_text(json.dumps({
            "status": "failed",
            "attempt": {"sequence": 67, "commit": "a" * 40, "images": {"secret": "ignored"}},
            "unexpected": "ignored",
        }), encoding="utf-8")
        self.assertEqual(notify.safe_state(path), {
            "status": "failed", "sequence": 67, "commit": "a" * 40
        })

    def test_message_does_not_contain_password_or_state_payload(self):
        details = {
            "source": "deployment", "host": "prod.example", "time_utc": "2026-09-03T00:00:00+00:00",
            "status": "failed", "sequence": 67, "commit": "a" * 40,
            "unit_result": "exit-code", "exit_status": "1",
        }
        message = notify.build_message(
            "sender@example.com", ["ops@example.com"], "deployment", details, False
        )
        text = message.as_string()
        self.assertIn("sequence: 67", text)
        self.assertIn("commit: " + "a" * 40, text)
        self.assertNotIn("smtp-password", text)

    def test_rate_limit_matches_only_same_recent_failure(self):
        path = self.root / "rate.json"
        notify.save_rate_state(path, "a" * 64, 1000)
        self.assertTrue(notify.rate_limited(path, "a" * 64, 1001))
        self.assertFalse(notify.rate_limited(path, "b" * 64, 1001))
        self.assertFalse(notify.rate_limited(path, "a" * 64, 1000 + notify.RATE_LIMIT_SECONDS))
        if os.name == "posix":
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    @mock.patch.object(notify, "send_message")
    @mock.patch.object(notify, "alert_details")
    def test_main_sends_test_without_writing_rate_state(self, details, send):
        credentials = self.root / "credentials"
        credentials.mkdir()
        (credentials / "smtp-username").write_text("sender@example.com\n", encoding="utf-8")
        (credentials / "smtp-password").write_text("authorization-code\n", encoding="utf-8")
        (credentials / "alert-recipients").write_text("ops@example.com\n", encoding="utf-8")
        details.return_value = {
            "source": "test", "host": "prod", "time_utc": "now", "status": "ready",
            "sequence": 66, "commit": "a" * 40, "unit_result": "n/a", "exit_status": "n/a",
        }
        rate = self.root / "rate.json"
        with mock.patch("sys.argv", [
            str(SOURCE), "--source", "test", "--test",
            "--credential-directory", str(credentials), "--rate-state", str(rate),
        ]):
            self.assertEqual(notify.main(), 0)
        send.assert_called_once()
        self.assertFalse(rate.exists())


if __name__ == "__main__":
    unittest.main()
