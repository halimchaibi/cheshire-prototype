#!/usr/bin/env bash

MODE="NONE" # Options: SUMMARY, NO_COMMENTS, FULL
DIR=""

# 1. Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        -c)   MODE="SUMMARY"; shift ;;
        -cc)  MODE="NO_COMMENTS"; shift ;;
        -cx|-ccx) MODE="FULL"; shift ;;
        -d)   DIR="$2"; shift 2 ;;
        *)
           if [[ -z "$DIR" ]]; then
               DIR="$1"; shift
           else
               echo "Unknown argument: $1"; exit 1
           fi
           ;;
    esac
done

# 2. Validate directory
if [[ -z "$DIR" ]]; then
    echo "Usage: $0 <directory> [-c | -cc | -cx]"
    exit 1
fi

# 3. Path Normalization (WSL & Existence)
if [[ "$DIR" == *'wsl.localhost'* ]]; then
    DIR="${DIR#*Ubuntu-22.04}"
    DIR="${DIR//\\//}"
    DIR="/${DIR#/}"
fi

if [[ ! -d "$DIR" ]]; then
    echo "Directory does not exist: $DIR"; exit 1
fi

echo "========================================"
echo " Directory inspection: $DIR"
echo "========================================"
echo

# 4. Directory tree
echo "=== Directory Structure ==="
find "$DIR" -maxdepth 10 -not -path '*/.*' | sed 's|[^/]*/|  |g'
echo

# 5. File contents logic
if [[ "$MODE" != "NONE" ]]; then
  echo "=== File Contents ($MODE) ==="
  find "$DIR" -type f -not -path '*/.*' | while read -r file; do
    echo -e "\n----------------------------------------\nFILE: $file\n----------------------------------------"

    # Skip binary files
    if ! grep -Iq . "$file"; then
      echo "  [binary file]"; continue
    fi

    case "$MODE" in
        "SUMMARY")
            # Grab imports and method signatures (lines ending in { or ; containing access modifiers)
            grep -E "^import |(public|protected|private|static).+\(.*\).*(;|\{)" "$file"
            ;;

        "NO_COMMENTS")
            # Strip block comments (/* */) and line comments (//)
            # This uses a regex to match comments while ignoring strings roughly
            perl -0777 -pe 's/\/\*.*?\*\/| \/\/.*//sg' "$file" | sed '/^\s*$/d'
            ;;

        "FULL")
            cat "$file"
            ;;
    esac
  done
else
  echo "=== File Contents (Skipped: use -c, -cc, or -cx) ==="
fi

echo -e "\n=== Done ==="