#!/usr/bin/env bash

# Steps to add new git hooks
# 1. Save your git hooks in "hooks/git/<hook name>"
# 2. Execute this script with bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "${DIR}/.."

# Symlink git hooks
for filename in ${DIR}/git/*; do
	filename=$(basename ${filename})
	ln -s "../../hooks/git/${filename}" ".git/hooks/${filename}"
done