#!/bin/bash

# Check argument
if [ -z "$1" ]; then
  echo "Arg must be a css file."
  exit 1
fi

# Extract URL.
grep -oE 'url\([^)]+\)' "$1" | sed -E 's/url\(([^)]+)\)/\1/'
