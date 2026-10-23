#!/usr/bin/env python3
"""Keep private data out of a repository that is, or will become, public.

Everything committed here is readable by anyone and stays in the history, so the
check runs on what is about to be added rather than on the tree: the added lines
of a diff, the commit message, and the author and committer email addresses.

The patterns in this file are generic on purpose. Naming a private project,
client or host here would publish the very thing the check protects, so those
come from a denylist kept outside the repository:

  - locally, the file named by `git config privacy.denylist`;
  - in CI, the PRIVACY_DENYLIST environment variable, set from a repository secret.

A denylist entry is one term per line, matched case-insensitively as a whole
word. An entry starting with `re:` is a regular expression instead. Blank lines
and lines starting with `#` are ignored. A match on an entry is reported by its
line number in the denylist, never by its text, so the report cannot leak it.

A line that trips a pattern legitimately can carry `privacy-check: allow` to
pass. That marker is visible in review, which is the point. Third-party code
vendored whole, whose upstream author lines are already public, is listed by
glob in `.githooks/privacy-exempt` instead, since it cannot carry the marker
without being modified.

Usage:
  privacy-check.py --staged             the index, plus the configured identity
  privacy-check.py --message FILE       a commit message, as the commit-msg hook gets it
  privacy-check.py --commits RANGE      each commit in a git rev-list range
  privacy-check.py --tree               every tracked file, for an audit
"""

import argparse
import fnmatch
import os
import re
import subprocess
import sys

ALLOW_MARKER = "privacy-check: allow"

# This file has to spell out what it looks for.
SELF = ".githooks/privacy-check.py"
EXEMPT_LIST = ".githooks/privacy-exempt"

# Kotlin's qualified this and labelled jumps (`this@Outer`, `return@forEach`) look like addresses.
KOTLIN_LABEL_KEYWORDS = {"this", "super", "return", "break", "continue"}

# Placeholder names that read as examples rather than as somebody's machine.
PLACEHOLDER_USERS = {"you", "user", "username", "me", "example", "name", "runner", "shared", "ubuntu", "build"}

EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\.)+[A-Za-z]{2,}")
ALLOWED_EMAIL = [
    re.compile(r"@users\.noreply\.github\.com$", re.I),
    re.compile(r"^(no-?reply|git)@", re.I),
    re.compile(r"@(.+\.)?(example\.(com|org|net)|[a-z0-9-]+\.(example|test|invalid|localhost))$", re.I),
]

HOME_PATH = re.compile(r"(?:/Users/|/home/|[A-Za-z]:\\\\?Users\\\\?)([A-Za-z0-9._-]+)")

PRIVATE_IP = re.compile(
    r"(?<![\d.])(?:10\.\d{1,3}|192\.168|172\.(?:1[6-9]|2\d|3[01])|100\.(?:6[4-9]|[7-9]\d|1[01]\d|12[0-7]))\.\d{1,3}\.\d{1,3}(?![\d.])"
)
LOCAL_HOST = re.compile(r"\b[a-z0-9][a-z0-9-]*\.(?:local|lan|home\.arpa)\b", re.I)
# A URL whose host has no dot names a machine on somebody's own network. The host is taken whole, after any
# credentials, so `https://user:${TOKEN}@github.com` is read as github.com; a templated host is not judged.
URL_HOST = re.compile(r"\b(?:https?|wss?|ssh|ftp)://(?:[^@/\s]*@)?([^/:\s\"'`)]+)")
BARE_HOST = re.compile(r"[A-Za-z0-9-]+")

SECRETS = [
    ("private key", re.compile(r"-----BEGIN (?:[A-Z]+ )?PRIVATE KEY-----")),
    ("AWS access key", re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("GitHub token", re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{40,})\b")),
    ("Google API key", re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b")),
    ("Slack token", re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}")),
    # A value with `$` in it is a variable reference, not a credential.
    ("credential literal", re.compile(r"(?i)\b(?:api[_-]?key|secret|token|password|passwd)\b\s*[:=]\s*[\"'][^\"'\s$]{12,}[\"']")),
]

# Commit messages only: tool attribution does not belong in the history.
ATTRIBUTION = [
    re.compile(r"(?i)^co-authored-by:.*\b(claude|anthropic|copilot|openai|chatgpt|gemini)\b"),
    re.compile(r"(?i)\bgenerated (?:with|by)\b.*\b(claude|copilot|chatgpt|gemini|ai)\b"),
    re.compile(r"(?i)noreply@anthropic\.com"),
]

# The one address shape an author or committer may use.
IDENTITY_EMAIL = re.compile(r"^(?:\d+\+)?[A-Za-z0-9-]+(?:\[bot\])?@users\.noreply\.github\.com$|^noreply@github\.com$", re.I)

IN_CI = os.environ.get("GITHUB_ACTIONS") == "true"


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, errors="replace").stdout


def load_denylist():
    text = os.environ.get("PRIVACY_DENYLIST")
    if text is None:
        try:
            path = git("config", "--get", "privacy.denylist").strip()
        except subprocess.CalledProcessError:
            path = ""
        if path:
            path = os.path.expanduser(path)
            try:
                with open(path, encoding="utf-8") as f:
                    text = f.read()
            except OSError as e:
                sys.exit(f"privacy-check: cannot read the denylist named by privacy.denylist: {e}")
    entries = []
    for number, raw in enumerate((text or "").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("re:"):
            pattern = re.compile(line[3:], re.I)
        else:
            pattern = re.compile(r"(?<![A-Za-z0-9])" + re.escape(line) + r"(?![A-Za-z0-9])", re.I)
        entries.append((number, pattern))
    return entries


class Report:
    def __init__(self):
        self.findings = []

    def add(self, where, line, rule, match=None):
        self.findings.append((where, line, rule, match))

    def print(self):
        for where, line, rule, match in self.findings:
            if IN_CI:
                # A public log is itself a leak, so CI names the rule and the place only.
                loc = f"file={where},line={line}" if line else f"title={where}"
                print(f"::error {loc}::privacy-check: {rule}")
            else:
                at = f"{where}:{line}" if line else where
                shown = f": {match}" if match else ""
                print(f"  {at}: {rule}{shown}", file=sys.stderr)


def scan_line(text, where, line, report, denylist, message=False):
    if ALLOW_MARKER in text:
        return
    for m in EMAIL.finditer(text):
        if m.group(0).split("@")[0] in KOTLIN_LABEL_KEYWORDS:
            continue
        if not any(p.search(m.group(0)) for p in ALLOWED_EMAIL):
            report.add(where, line, "email address", m.group(0))
    for m in HOME_PATH.finditer(text):
        if m.group(1).lower() not in PLACEHOLDER_USERS and not m.group(1).startswith("<"):
            report.add(where, line, "absolute home path", m.group(0))
    for m in PRIVATE_IP.finditer(text):
        report.add(where, line, "private network address", m.group(0))
    for m in LOCAL_HOST.finditer(text):
        report.add(where, line, "local network hostname", m.group(0))
    for m in URL_HOST.finditer(text):
        if BARE_HOST.fullmatch(m.group(1)) and m.group(1).lower() != "localhost":
            report.add(where, line, "URL to a bare hostname", m.group(0))
    for name, pattern in SECRETS:
        if pattern.search(text):
            report.add(where, line, name)
    if message and any(pattern.search(text) for pattern in ATTRIBUTION):
        report.add(where, line, "tool attribution in a commit message")
    for number, pattern in denylist:
        if pattern.search(text):
            report.add(where, line, f"private term (denylist entry on line {number})")


def load_exempt(read):
    """Globs of vendored paths, read through `read` so the staged check sees the staged copy of the list."""
    try:
        text = read(EXEMPT_LIST)
    except (OSError, subprocess.CalledProcessError):
        text = ""
    globs = [l.strip() for l in text.splitlines() if l.strip() and not l.strip().startswith("#")]
    return lambda path: path == SELF or any(fnmatch.fnmatchcase(path, g) for g in globs)


def read_worktree(path):
    with open(os.path.join(git("rev-parse", "--show-toplevel").strip(), path), encoding="utf-8") as f:
        return f.read()


def scan_diff(diff, report, denylist, exempt, prefix=""):
    """Scans the added lines of a unified diff produced with -U0."""
    path, new_line = None, 0
    for raw in diff.splitlines():
        if raw.startswith("+++ "):
            target = raw[4:]
            path = None if target == "/dev/null" else target[2:] if target.startswith("b/") else target
            if path and not exempt(path):
                # A file's name is published too.
                scan_line(path, prefix + path, 0, report, denylist)
            continue
        if raw.startswith("@@"):
            m = re.match(r"@@ -\S+ \+(\d+)", raw)
            new_line = int(m.group(1)) if m else 0
            continue
        if path is None or exempt(path):
            continue
        if raw.startswith("+"):
            scan_line(raw[1:], prefix + path, new_line, report, denylist)
            new_line += 1


def scan_message(text, where, report, denylist):
    for number, raw in enumerate(text.splitlines(), start=1):
        scan_line(raw, where, number, report, denylist, message=True)


def check_identity(email, role, where, report):
    if not IDENTITY_EMAIL.search(email):
        report.add(where, 0, f"{role} email is not a GitHub noreply address", email)


DIFF_ARGS = ["--no-color", "--no-ext-diff", "-U0", "--no-renames", "--diff-filter=AM"]


def run_staged(report, denylist):
    exempt = load_exempt(lambda p: git("show", f":{p}"))
    scan_diff(git("-c", "core.quotepath=off", "diff", "--cached", *DIFF_ARGS), report, denylist, exempt)
    for role, var in (("author", "GIT_AUTHOR_IDENT"), ("committer", "GIT_COMMITTER_IDENT")):
        ident = git("var", var)
        m = re.search(r"<([^>]*)>", ident)
        check_identity(m.group(1) if m else "", role, "git identity", report)


def run_message(path, report, denylist):
    with open(path, encoding="utf-8", errors="replace") as f:
        text = f.read()
    # What git strips before recording the message is not part of it.
    text = text.split("# ------------------------ >8 ------------------------")[0]
    text = "\n".join(l for l in text.splitlines() if not l.startswith("#"))
    scan_message(text, "commit message", report, denylist)


def run_commits(rev_range, report, denylist):
    # The checked-out tip's list governs the whole range, so vendored code committed before the list existed passes.
    exempt = load_exempt(read_worktree)
    for sha in git("rev-list", *rev_range.split()).split():
        short = sha[:9]
        parents, author, committer = git("show", "-s", "--format=%P%n%ae%n%ce", sha).split("\n")[:3]
        check_identity(author, "author", f"commit {short}", report)
        check_identity(committer, "committer", f"commit {short}", report)
        scan_message(git("show", "-s", "--format=%B", sha), f"commit {short} message", report, denylist)
        if len(parents.split()) <= 1:
            # A merge brings in commits that are scanned on their own.
            diff = git("-c", "core.quotepath=off", "show", "--format=", *DIFF_ARGS, sha)
            scan_diff(diff, report, denylist, exempt, prefix=f"{short}:")


def run_tree(report, denylist):
    exempt = load_exempt(read_worktree)
    for path in git("-c", "core.quotepath=off", "ls-files").splitlines():
        if exempt(path) or not os.path.isfile(path):
            continue
        scan_line(path, path, 0, report, denylist)
        try:
            with open(path, "rb") as f:
                data = f.read()
        except OSError:
            continue
        if b"\0" in data[:8000]:
            continue
        for number, line in enumerate(data.decode("utf-8", errors="replace").splitlines(), start=1):
            scan_line(line, path, number, report, denylist)


def main():
    parser = argparse.ArgumentParser(description="Keep private data out of a public repository.")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--staged", action="store_true")
    mode.add_argument("--message", metavar="FILE")
    mode.add_argument("--commits", metavar="RANGE")
    mode.add_argument("--tree", action="store_true")
    args = parser.parse_args()

    denylist = load_denylist()
    report = Report()
    if args.staged:
        run_staged(report, denylist)
    elif args.message:
        run_message(args.message, report, denylist)
    elif args.commits:
        run_commits(args.commits, report, denylist)
    else:
        run_tree(report, denylist)

    if not report.findings:
        return 0
    if not IN_CI:
        print("privacy-check: this would publish private data:", file=sys.stderr)
    report.print()
    if not IN_CI:
        print(
            "Remove it, or replace it with a placeholder (example.com, acme, owner/repo, PROJ-123).\n"
            f"A line that is legitimately public can carry `{ALLOW_MARKER}`.",
            file=sys.stderr,
        )
    return 1


if __name__ == "__main__":
    sys.exit(main())
