# Git hooks

This repository is published, history included, so these hooks refuse a commit that would publish private data before it exists. The same check runs in CI ([`privacy.yml`](../.github/workflows/privacy.yml)) on every push and pull request, as the backstop for a commit made without them.

## What is checked

`privacy-check.py` looks at what a commit adds, never the whole tree: the added lines and file names of the diff, the commit message, and the author and committer email.

- **Email addresses**, except GitHub noreply addresses, `noreply@` and `git@` addresses, and reserved example domains (`example.com`, `*.test`, `*.invalid`).
- **Absolute home paths** such as `/Users/<name>/` or `/home/<name>/`, except placeholder names like `you` or `user`.
- **Private network addresses and hostnames**: RFC 1918 and CGNAT ranges, `.local`, `.lan` and `.home.arpa` names, and URLs to a host with no domain.
- **Secrets**: private keys, AWS, GitHub, Google and Slack tokens, and credential literals.
- **Tool attribution** in a commit message, such as a `Co-Authored-By` line naming an AI tool.
- **The author and committer email** must be a GitHub noreply address. Set yours with `git config user.email <id>+<handle>@users.noreply.github.com`, which GitHub shows under Settings → Emails.
- **Private terms** from a denylist you keep outside the repository (below).

## Setting up

Git never enables a repository's hooks on its own, so each clone opts in once:

```bash
git config core.hooksPath .githooks
```

Then point the check at your denylist: the names of private projects, clients, employers, hosts and tracker prefixes that must never appear here. It lives outside the repository, because committing it would publish exactly what it protects.

```bash
git config privacy.denylist ~/.config/git/private-terms
```

One term per line, matched case-insensitively as a whole word. A line starting with `re:` is a regular expression, which suits a tracker prefix: `re:\bPROJ-\d+`. Blank lines and `#` comments are ignored. A match is reported by its line in the denylist, never by its text.

In CI the denylist is the `PRIVACY_DENYLIST` repository secret, with the same format. A pull request from a fork receives no secrets and is checked for the generic patterns only.

## When a line is legitimately public

Add `privacy-check: allow` to the line. The marker stays visible in review, which is the point. Third-party code vendored whole, whose upstream author lines are already public, is listed by glob in [`privacy-exempt`](privacy-exempt) instead; first-party code never goes there.

To audit everything rather than one commit: `python3 .githooks/privacy-check.py --tree` checks every tracked file, and `--commits HEAD` checks the whole history.
