#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
source_hook="$repo_root/scripts/hooks/pre-commit"
target_hook="$repo_root/.git/hooks/pre-commit"

if [ ! -d "$repo_root/.git" ]; then
  echo "Not a git repository: $repo_root" >&2
  exit 1
fi

if [ -e "$target_hook" ] && ! cmp -s "$source_hook" "$target_hook"; then
  echo "Refusing to overwrite existing hook: $target_hook" >&2
  echo "Remove it first, or run with FORCE=1 to overwrite." >&2
  if [ "${FORCE:-}" != "1" ]; then
    exit 1
  fi
fi

mkdir -p "$repo_root/.git/hooks"
cp "$source_hook" "$target_hook"
chmod +x "$target_hook"

echo "Installed pre-commit hook to $target_hook"
