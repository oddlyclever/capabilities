#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""Read published versions for a Gradle version catalog and plan an update, without changing anything.

Subcommands:
  plan [--catalog PATH] [--rules PATH]
      A proposed version for every [versions] key, as JSON, applying the dependency rules file: a follower at the
      version its leader's proposed release requires, a held key not at all, a set only when every member has the same
      newer release, anything else at its newest stable release. Keys with no published coordinate are marked
      "unknown" for the caller to check by hand. With no rules file every key is independent, and the output says so.
  catalog [--catalog PATH] [--all]
      For each key the catalog's libraries and plugins use, the current version and the newest stable and newest of
      any kind. No rules applied: a raw view, for when the plan needs explaining.
  requires COORDINATE --group GROUP
      What a published module (group:artifact:version) requires of every module in GROUP, read from its Gradle
      module metadata, else its POM: `requires org.jetbrains.compose.ui:ui-graphics:1.12.2 --group org.jetbrains.skiko`.
  npm PACKAGE
      The newest stable and newest of any kind of an npm package.

The rules file (default gradle/dependency-rules.toml) has four optional tables:
  [followers.<key>]  leader = "<key>", via = "group:artifact", group = "<group>", module = "<artifact>"
  [held.<key>]       reason = "...", npm = "<package>" (to report its newest anyway)
  [sets.<name>]      keys = [...], npm = { <key> = "<package>" }, vendored = "<path>", reason = "..."
  [verify]           commands = ["..."]

Output is JSON on stdout; diagnostics go to stderr.
Exit codes: 0 done, 1 bad usage or an unreadable catalog or rules file, 2 a repository could not be reached or had
no answer for something the command needed.

Repositories read: Maven Central, Google's Maven repository (androidx, com.android, com.google), the Gradle Plugin
Portal for plugin markers, and the npm registry. Only metadata is read; nothing is sent and nothing is written.
"""

import argparse
import json
import os
import re
import sys
import tomllib
import urllib.error
import urllib.request
import xml.etree.ElementTree as ElementTree

CENTRAL = "https://repo1.maven.org/maven2"
GOOGLE = "https://dl.google.com/dl/android/maven2"
PLUGIN_PORTAL = "https://plugins.gradle.org/m2"
NPM = "https://registry.npmjs.org"
GOOGLE_GROUPS = ("androidx.", "com.android.", "com.google.")

# A release whose version carries one of these is not stable, whatever its number.
PRERELEASE = re.compile(r"(?i)(alpha|beta|rc|cr|eap|dev|snapshot|preview|milestone|-m\d|\.m\d)")


class Unreachable(Exception):
    pass


def fetch(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise Unreachable(f"{url} answered {e.code}")
    except urllib.error.URLError as e:
        raise Unreachable(f"cannot reach {url}: {e.reason}")


def load_toml(path, what):
    try:
        with open(path, "rb") as f:
            return tomllib.load(f)
    except (OSError, tomllib.TOMLDecodeError) as e:
        print(f"versions: cannot read the {what} {path}: {e}", file=sys.stderr)
        sys.exit(1)


def repositories(group, plugin=False):
    if plugin:
        return [GOOGLE, PLUGIN_PORTAL] if group.startswith(GOOGLE_GROUPS) else [PLUGIN_PORTAL, CENTRAL]
    return [GOOGLE, CENTRAL] if group.startswith(GOOGLE_GROUPS) else [CENTRAL]


def published_versions(group, artifact, plugin=False):
    for repo in repositories(group, plugin):
        data = fetch(f"{repo}/{group.replace('.', '/')}/{artifact}/maven-metadata.xml")
        if data:
            return [v.text for v in ElementTree.fromstring(data).iter("version") if v.text]
    return []


def npm_versions(package):
    data = fetch(f"{NPM}/{package}")
    return list(json.loads(data).get("versions", {}).keys()) if data else []


def sort_key(version):
    """Orders versions by their dotted parts, numerically; a pre-release sorts before the release it leads to."""
    return [(0, int(p), "") if p.isdigit() else (-1, 0, p.lower()) for p in re.split(r"[.\-+]", version)]


def newer(a, b):
    return sort_key(a) > sort_key(b)


def newest(versions, stable_only):
    pool = [v for v in versions if not (stable_only and PRERELEASE.search(v))]
    return max(pool, key=sort_key) if pool else None


def coordinate_of(entry):
    if "module" in entry:
        return tuple(entry["module"].split(":")[:2])
    return entry.get("group"), entry.get("name")


def key_users(catalog):
    """Each [versions] key the catalog's libraries and plugins refer to, with the coordinates that use it."""
    users = {}
    for entry in catalog.get("libraries", {}).values():
        if isinstance(entry, dict) and isinstance(entry.get("version"), dict) and "ref" in entry["version"]:
            users.setdefault(entry["version"]["ref"], []).append((*coordinate_of(entry), False))
    for entry in catalog.get("plugins", {}).values():
        if isinstance(entry, dict) and isinstance(entry.get("version"), dict) and "ref" in entry["version"]:
            users.setdefault(entry["version"]["ref"], []).append((entry["id"], f"{entry['id']}.gradle.plugin", True))
    return users


def maven_latest(coordinates):
    """The newest releases of the first coordinate that has published metadata: every user of a key moves together."""
    for group, artifact, plugin in coordinates:
        published = published_versions(group, artifact, plugin)
        if published:
            return newest(published, True), newest(published, False), f"{group}:{artifact}"
    return None


def requires(coordinate, group):
    """What the module published at `coordinate` requires of each module in `group`, or None if it is not published."""
    g, artifact, version = coordinate.split(":")
    for repo in repositories(g):
        path = f"{repo}/{g.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}"
        module = fetch(path + ".module")
        if module:
            found = {}
            for variant in json.loads(module).get("variants", []):
                for dep in variant.get("dependencies", []):
                    if dep.get("group") == group:
                        wanted = dep.get("version", {})
                        found.setdefault(dep["module"], set()).add(wanted.get("strictly") or wanted.get("requires"))
            return {m: sorted(v for v in vs if v) for m, vs in sorted(found.items())}, "module"
        pom = fetch(path + ".pom")
        if pom:
            ns = {"m": "http://maven.apache.org/POM/4.0.0"}
            found = {}
            for dep in ElementTree.fromstring(pom).iterfind(".//m:dependencies/m:dependency", ns):
                if dep.findtext("m:groupId", namespaces=ns) == group:
                    found.setdefault(dep.findtext("m:artifactId", namespaces=ns), set()).add(dep.findtext("m:version", namespaces=ns))
            return {m: sorted(v for v in vs if v) for m, vs in sorted(found.items())}, "pom"
    return None


def cmd_plan(args):
    catalog = load_toml(args.catalog, "catalog")
    has_rules = os.path.exists(args.rules)
    rules = load_toml(args.rules, "rules file") if has_rules else {}
    versions = catalog.get("versions", {})
    users = key_users(catalog)
    followers, held, sets = rules.get("followers", {}), rules.get("held", {}), rules.get("sets", {})
    set_of = {key: name for name, s in sets.items() for key in s.get("keys", [])}
    npm_of = {key: rule["npm"] for key, rule in held.items() if "npm" in rule}
    for s in sets.values():
        npm_of.update(s.get("npm", {}))

    for key in [*followers, *held, *set_of]:
        if key not in versions:
            print(f"versions: the rules name {key}, which the catalog's [versions] does not have", file=sys.stderr)
            return 1

    def latest(key):
        if key in npm_of:
            published = npm_versions(npm_of[key])
            return (newest(published, True), newest(published, False), f"npm:{npm_of[key]}") if published else None
        return maven_latest(users[key]) if key in users else None

    rows = {}

    def row(key, status, proposed=None, reason=None, found=None):
        r = {"key": key, "current": versions[key], "status": status, "proposed": proposed or versions[key]}
        if found:
            r["latest_stable"], r["latest_any"], r["checked"] = found
        if reason:
            r["reason"] = reason
        rows[key] = r

    for key, current in versions.items():
        if key in followers or key in set_of:
            continue
        if key in held:
            row(key, "held", reason=held[key].get("reason"), found=latest(key))
            continue
        found = latest(key)
        if found is None:
            row(key, "unknown", reason="no published coordinate in the catalog; check by hand")
        elif found[0] and newer(found[0], current):
            row(key, "update", proposed=found[0], found=found)
        else:
            row(key, "current", found=found)

    # Followers in an order that puts every leader first, so a follower may lead another.
    pending = dict(followers)
    while pending:
        ready = [k for k, rule in pending.items() if rule["leader"] in rows]
        if not ready:
            print(f"versions: the followers {sorted(pending)} wait on leaders that never resolve", file=sys.stderr)
            return 1
        for key in ready:
            rule = pending.pop(key)
            leader_version = rows[rule["leader"]]["proposed"]
            answer = requires(f"{rule['via']}:{leader_version}", rule["group"])
            wanted = (answer[0].get(rule.get("module"), []) if rule.get("module") else
                      sorted({v for vs in answer[0].values() for v in vs})) if answer else []
            found = latest(key)
            if len(wanted) != 1:
                row(key, "unknown", found=found, reason=(
                    f"{rule['via']}:{leader_version} asks for {wanted or 'nothing'} of {rule['group']}"
                    f"{':' + rule['module'] if rule.get('module') else ''}; check by hand"))
                continue
            reason = f"{rule['leader']} {leader_version} requires it, through {rule['via']}"
            row(key, "current" if wanted[0] == versions[key] else "follow", proposed=wanted[0], reason=reason, found=found)

    for name, s in sets.items():
        found = {key: latest(key) for key in s["keys"]}
        stables = {key: (f[0] if f else None) for key, f in found.items()}
        moved = [key for key, v in stables.items() if v and newer(v, versions[key])]
        common = len({*stables.values()}) == 1 and None not in stables.values()
        for key in s["keys"]:
            if not moved:
                row(key, "current", found=found[key], reason=f"set {name}")
            elif common:
                row(key, "update", proposed=stables[key], found=found[key], reason=f"set {name}: every member has {stables[key]}")
            else:
                newest_of = ", ".join(f"{k} {v}" for k, v in stables.items())
                row(key, "held", found=found[key], reason=f"set {name} is not complete ({newest_of}): {s.get('reason', '')}".rstrip(": "))
        if moved and common and s.get("vendored"):
            for key in s["keys"]:
                rows[key]["vendored"] = s["vendored"]

    plan = [rows[key] for key in versions]
    json.dump({
        "rules": args.rules if has_rules else None,
        "plan": plan,
        "summary": {status: sum(1 for r in plan if r["status"] == status)
                    for status in ("update", "follow", "held", "unknown", "current")},
        "verify": rules.get("verify", {}).get("commands", []),
    }, sys.stdout, indent=2)
    print()
    if not has_rules:
        print(f"versions: no rules file at {args.rules}; every key was treated as independent", file=sys.stderr)
    return 0


def cmd_catalog(args):
    catalog = load_toml(args.catalog, "catalog")
    report, unresolved = [], []
    users = key_users(catalog)
    for key, current in catalog.get("versions", {}).items():
        if key not in users:
            unresolved.append({"key": key, "current": current})
            continue
        found = maven_latest(users[key])
        if not found:
            report.append({"key": key, "current": current, "error": "no metadata found"})
            continue
        stable, any_kind, checked = found
        if args.all or (stable and newer(stable, current)):
            report.append({"key": key, "current": current, "latest_stable": stable, "latest_any": any_kind,
                           "checked": checked, "coordinates": [f"{g}:{a}" for g, a, _ in users[key]]})
    json.dump({"versions" if args.all else "updates": report, "unresolved": unresolved}, sys.stdout, indent=2)
    print()
    return 0


def cmd_requires(args):
    if args.coordinate.count(":") != 2:
        print("versions: a coordinate is group:artifact:version", file=sys.stderr)
        return 1
    answer = requires(args.coordinate, args.group)
    if answer is None:
        print(f"versions: {args.coordinate} has no .module or .pom where looked", file=sys.stderr)
        return 2
    json.dump({"coordinate": args.coordinate, "group": args.group, "source": answer[1], "requires": answer[0]},
              sys.stdout, indent=2)
    print()
    return 0


def cmd_npm(args):
    published = npm_versions(args.package)
    if not published:
        print(f"versions: no npm package named {args.package}", file=sys.stderr)
        return 2
    json.dump({"package": args.package, "latest_stable": newest(published, True), "latest_any": newest(published, False)},
              sys.stdout, indent=2)
    print()
    return 0


def main():
    parser = argparse.ArgumentParser(description="Read published versions for a Gradle version catalog and plan an update.",
                                     formatter_class=argparse.RawDescriptionHelpFormatter, epilog=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    plan = sub.add_parser("plan", help="a proposed version for every key, applying the dependency rules")
    plan.add_argument("--catalog", default="gradle/libs.versions.toml")
    plan.add_argument("--rules", default="gradle/dependency-rules.toml")
    catalog = sub.add_parser("catalog", help="the catalog's versions against what is published, no rules applied")
    catalog.add_argument("--catalog", default="gradle/libs.versions.toml")
    catalog.add_argument("--all", action="store_true", help="list every resolved key, not only the ones behind")
    req = sub.add_parser("requires", help="what a published module requires of a group")
    req.add_argument("coordinate")
    req.add_argument("--group", required=True)
    npm = sub.add_parser("npm", help="the newest releases of an npm package")
    npm.add_argument("package")
    args = parser.parse_args()
    try:
        return {"plan": cmd_plan, "catalog": cmd_catalog, "requires": cmd_requires, "npm": cmd_npm}[args.command](args)
    except Unreachable as e:
        print(f"versions: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
