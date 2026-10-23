#!/usr/bin/env python3
"""Audit and prune orphaned Antigravity (agy) and Gemini project entries.

Scans ~/.gemini/config/projects/*.json and ~/.gemini/projects.json for project
registrations whose underlying filesystem paths no longer exist on disk (such
as deleted mktemp directories under /tmp or /var/folders created during test
runs). System sentinel files without folder URIs (e.g., default-cli-project.json
and outside-of-project.json) are always preserved.
"""

import argparse
import glob
import json
import os
import sys
from urllib.parse import unquote, urlparse

SYSTEM_PROJECT_FILES = {"default-cli-project.json", "outside-of-project.json"}


def extract_uris(obj):
    uris = []
    if isinstance(obj, dict):
        for v in obj.values():
            if isinstance(v, str) and v.startswith("file://"):
                uris.append(v)
            else:
                uris.extend(extract_uris(v))
    elif isinstance(obj, list):
        for item in obj:
            uris.extend(extract_uris(item))
    return uris


def scan_agy_projects(projects_dir):
    orphaned = []
    kept = []
    if not os.path.isdir(projects_dir):
        return orphaned, kept

    for pf in sorted(glob.glob(os.path.join(projects_dir, "*.json"))):
        fname = os.path.basename(pf)
        if fname in SYSTEM_PROJECT_FILES:
            kept.append({"file": pf, "name": fname, "reason": "system-sentinel"})
            continue
        try:
            with open(pf, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as exc:
            print(f"warning: failed to parse {pf}: {exc}", file=sys.stderr)
            continue

        name = data.get("name", "")
        uris = extract_uris(data)
        paths = [unquote(urlparse(u).path) for u in uris]

        if not paths:
            kept.append({"file": pf, "name": name, "reason": "no-folder-uri"})
            continue

        if any(os.path.exists(p) for p in paths):
            kept.append({"file": pf, "name": name, "paths": paths})
        else:
            orphaned.append({
                "file": pf,
                "id": data.get("id", ""),
                "name": name,
                "paths": paths,
            })
    return orphaned, kept


def scan_gemini_projects_map(projects_json_path):
    orphaned_keys = []
    kept_keys = []
    if not os.path.isfile(projects_json_path):
        return orphaned_keys, kept_keys, None

    try:
        with open(projects_json_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as exc:
        print(f"warning: failed to parse {projects_json_path}: {exc}", file=sys.stderr)
        return orphaned_keys, kept_keys, None

    projects_map = data.get("projects")
    if not isinstance(projects_map, dict):
        return orphaned_keys, kept_keys, data

    for path_key, alias in list(projects_map.items()):
        if os.path.exists(path_key):
            kept_keys.append({"path": path_key, "alias": alias})
        else:
            orphaned_keys.append({"path": path_key, "alias": alias})

    return orphaned_keys, kept_keys, data


def main():
    parser = argparse.ArgumentParser(
        description="Audit and prune orphaned Antigravity (agy) and Gemini project entries whose paths no longer exist on disk."
    )
    parser.add_argument(
        "--gemini-dir",
        default=os.path.expanduser("~/.gemini"),
        help="Path to the .gemini directory (default: ~/.gemini).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        default=True,
        help="Report orphaned project entries without deleting anything (default).",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Delete orphaned project JSON files and prune missing paths from projects.json.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=50,
        help="Maximum number of orphaned items to list in stdout JSON array (default: 50, 0 for all).",
    )
    args = parser.parse_args()

    dry_run = not args.apply
    projects_dir = os.path.join(args.gemini_dir, "config", "projects")
    projects_json_path = os.path.join(args.gemini_dir, "projects.json")

    orphaned_agy, kept_agy = scan_agy_projects(projects_dir)
    orphaned_map, kept_map, map_data = scan_gemini_projects_map(projects_json_path)

    removed_files = 0
    removed_map_keys = 0

    if args.apply:
        for item in orphaned_agy:
            try:
                os.unlink(item["file"])
                removed_files += 1
            except OSError as exc:
                print(f"error: failed to remove {item['file']}: {exc}", file=sys.stderr)
                sys.exit(2)

        if orphaned_map and isinstance(map_data, dict) and isinstance(map_data.get("projects"), dict):
            for item in orphaned_map:
                map_data["projects"].pop(item["path"], None)
                removed_map_keys += 1
            try:
                with open(projects_json_path, "w", encoding="utf-8") as f:
                    json.dump(map_data, f, indent=2)
                    f.write("\n")
            except OSError as exc:
                print(f"error: failed to update {projects_json_path}: {exc}", file=sys.stderr)
                sys.exit(2)

    listed_agy = orphaned_agy if args.limit == 0 else orphaned_agy[: args.limit]
    result = {
        "mode": "dry-run" if dry_run else "apply",
        "geminiDir": args.gemini_dir,
        "agyProjects": {
            "keptCount": len(kept_agy),
            "orphanedCount": len(orphaned_agy),
            "removedCount": removed_files,
            "orphanedSample": listed_agy,
            "truncated": len(orphaned_agy) > len(listed_agy),
        },
        "geminiProjectsJson": {
            "keptCount": len(kept_map),
            "orphanedCount": len(orphaned_map),
            "removedCount": removed_map_keys,
            "orphaned": orphaned_map,
        },
    }
    json.dump(result, sys.stdout, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
