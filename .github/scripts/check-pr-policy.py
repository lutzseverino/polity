#!/usr/bin/env python3
"""Validate the repository-family pull-request contract from environment data."""

from __future__ import annotations

import os
import re
import sys


ALLOWED_TYPES = "feat|fix|docs|refactor|test|build|ci|chore|perf|revert"
BRANCH = re.compile(rf"^(?:{ALLOWED_TYPES})/[1-9][0-9]*-[a-z0-9]+(?:-[a-z0-9]+)*$")
TITLE = re.compile(
    rf"^(?:{ALLOWED_TYPES})(?:\([a-z0-9][a-z0-9._/-]*\))?!?: [a-z0-9].+$"
)
ISSUE = re.compile(r"(?im)^Closes #[1-9][0-9]*\s*$")
HEADINGS = ("Summary", "Motivation", "Impact", "Validation")
EXEMPT_AUTHORS = {"dependabot[bot]"}


def validate(branch: str, title: str, body: str, author: str) -> list[str]:
    if author in EXEMPT_AUTHORS:
        return []
    errors: list[str] = []
    if not BRANCH.fullmatch(branch):
        errors.append(
            "branch must match <type>/<issue-number>-<short-kebab-slug> with an allowed type"
        )
    if not TITLE.fullmatch(title):
        errors.append("pull-request title must be a Conventional Commit subject")
    if not ISSUE.search(body):
        errors.append("pull-request body must contain a line in the form: Closes #123")
    # Adoption records carry their own structured validation and must remain
    # byte-preserved instead of being rewritten into the ordinary PR template.
    adoption = body.startswith("# Repository Standards adoption record\n")
    headings = (
        "Ready to commit", "Selection", "Operations", "Changed paths",
        "Scope changes", "Identities",
    ) if adoption else HEADINGS
    if adoption and "Ready to commit: yes." not in body:
        errors.append("adoption record must be ready to commit")
    for heading in headings:
        if not re.search(rf"(?im)^## {re.escape(heading)}\s*$", body):
            errors.append(f"pull-request body is missing: ## {heading}")
    return errors


def main() -> int:
    errors = validate(
        os.environ.get("PR_BRANCH", ""),
        os.environ.get("PR_TITLE", ""),
        os.environ.get("PR_BODY", ""),
        os.environ.get("PR_AUTHOR", ""),
    )
    if errors:
        print("Pull-request policy failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Pull-request policy passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
