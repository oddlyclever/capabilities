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

## The private-term denylist

The generic checks can't know the names that are private to you: your other projects, clients, employer, machines and tracker prefixes. You list those yourself, in a plain text file that lives outside the repository, because committing it would publish exactly what it protects. The check reads it in two places: on your machine from that file, and in CI from a repository secret holding a copy of it.

### Create it once

Make the file and tell git where it is. `--global` points every clone on this machine at the same list:

```bash
mkdir -p ~/.config/git && touch ~/.config/git/private-terms
git config --global privacy.denylist ~/.config/git/private-terms
```

`git config --get privacy.denylist` prints the path the check will read, and is the quickest way to find the file again. `~/.config` is a hidden folder, so Finder doesn't show it unless you press Cmd-Shift-. in a Finder window.

### Add a term

Open the file in a text editor and add one entry per line:

```bash
open -e ~/.config/git/private-terms
```

```
# Private project and client names: matched as whole words, ignoring case.
acme
widgetco

# A tracker prefix: a line starting with re: is a regular expression.
re:\bPROJ-\d+
```

- A plain line is a whole word, matched case-insensitively: `acme` matches `Acme` and `ACME`, not `acmeville`.
- A line starting with `re:` is a regular expression, also case-insensitive. Use it for a pattern, such as a tracker key: `re:\bPROJ-\d+` matches `PROJ-123`.
- Blank lines and lines starting with `#` are ignored, so the file can carry its own notes.

To add a line without opening an editor, use single quotes, so the shell leaves a backslash alone:

```bash
printf '%s\n' 're:\bPROJ-\d+' >> ~/.config/git/private-terms
```

### Check that it works

Put the term you added in a scratch file, here `PROJ-123` for the pattern above, and run the check on it. It should fail and name the denylist line that matched:

```bash
printf 'a note about PROJ-123\n' > /tmp/denylist-test.txt
python3 .githooks/privacy-check.py --message /tmp/denylist-test.txt
```

A match is reported by its line number in the denylist, never by its text, so the output is safe to share.

### Copy it to CI

CI can't read your file, so it reads the `PRIVACY_DENYLIST` repository secret instead, which holds the same text. After every change to the file, replace the secret with the file's new contents:

```bash
gh secret set PRIVACY_DENYLIST < ~/.config/git/private-terms
```

Without `gh`, open the repository on GitHub, go to Settings → Secrets and variables → Actions, edit `PRIVACY_DENYLIST`, and paste in the whole file. A secret can be replaced but never read back, so the file on your machine is the master copy. A pull request from a fork receives no secrets and is checked for the generic patterns only.

## When a line is legitimately public

Add `privacy-check: allow` to the line. The marker stays visible in review, which is the point. Third-party code vendored whole, whose upstream author lines are already public, is listed by glob in [`privacy-exempt`](privacy-exempt) instead; first-party code never goes there.

To audit everything rather than one commit: `python3 .githooks/privacy-check.py --tree` checks every tracked file, and `--commits HEAD` checks the whole history.
