#!/usr/bin/env bash
#
# Renders PlantUML sequence diagrams (*.puml) in this folder to SVGs of the
# same base name, next to the source files.
#
# Usage:
#   ./render-svgs.sh          # render ALL diagrams (default)
#   ./render-svgs.sh 9        # render only diagram number 09
#   ./render-svgs.sh 10       # render only diagram number 10
#
# Portable: works with the stock macOS /bin/bash 3.2 (no mapfile/readarray).
set -euo pipefail

# Always operate on the folder this script lives in, regardless of where it is called from.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Empty (non-matching) globs expand to nothing instead of the literal pattern.
shopt -s nullglob

if ! command -v plantuml >/dev/null 2>&1; then
    echo "error: 'plantuml' is not on your PATH." >&2
    echo "       install it with: brew install plantuml" >&2
    exit 1
fi

if [[ $# -gt 1 ]]; then
    echo "usage: $(basename "$0") [diagram-number]" >&2
    exit 1
fi

if [[ $# -eq 1 ]]; then
    # A single diagram requested by number: validate, zero-pad, match the "NN-" prefix.
    if [[ ! "$1" =~ ^[0-9]+$ ]]; then
        echo "error: '$1' is not a diagram number." >&2
        exit 1
    fi
    num="$(printf '%02d' "$1")"
    files=( ./"$num"-*.puml )
    if [[ ${#files[@]} -eq 0 ]]; then
        echo "error: no diagram numbered '$num' found in $SCRIPT_DIR" >&2
        exit 1
    fi
else
    # No argument: render every diagram in the folder.
    files=( ./*.puml )
    if [[ ${#files[@]} -eq 0 ]]; then
        echo "error: no .puml files found in $SCRIPT_DIR" >&2
        exit 1
    fi
fi

echo "Rendering ${#files[@]} diagram(s) to SVG in $SCRIPT_DIR"

# -tsvg      : SVG output
# -failfast2 : abort on the first syntax error
plantuml -tsvg -failfast2 "${files[@]}"

echo "Done. Generated:"
for f in "${files[@]}"; do
    echo "  ${f%.puml}.svg"
done
