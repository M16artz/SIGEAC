#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(dirname "$SCRIPT_DIR")
TARGETS=""
for candidate in "$ROOT_DIR/tests/fixtures" "$ROOT_DIR/tests/acceptance/results"; do
  [ ! -d "$candidate" ] || TARGETS="$TARGETS $candidate"
done
[ -n "$TARGETS" ] || { echo 'No synthetic-data directories found.'; exit 0; }
if grep -RInE --exclude='*.png' --exclude='*.pdf' 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|@(unl\.edu\.ec|caces\.gob\.ec)' $TARGETS; then
  echo 'Prohibited real-data or secret pattern found.' >&2
  exit 1
fi
echo 'Synthetic-data verification passed.'
