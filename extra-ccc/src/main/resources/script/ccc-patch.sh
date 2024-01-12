#!/bin/bash

set -euo pipefail

if [[ $# -eq 0 ]] ; then
  echo "You need to specify ISO path as first argument. For example ./patch.sh ccc.iso"
  exit 1
fi

FINE_PATCH="./ccc.patchfs"
FINE_DST="./ccc-patched.iso"
FINE_FLAGS=""

read -p "Change Nero's Praetor to Maestro? [yN] " yn
case $yn in
    [Yy]* ) FINE_FLAGS="$FINE_FLAGS --useMaestro" ;;
esac
read -p "Change Meltlilith to Meltryllis? [yN] " yn
case $yn in
    [Yy]* ) FINE_FLAGS="$FINE_FLAGS --useMeltryllis" ;;
esac
read -p "Use low-res ruby font? (use when playing on a PSP) [yN] " yn
case $yn in
    [Yy]* ) FINE_FLAGS="$FINE_FLAGS --useLowResRuby" ;;
esac

if [[ "$OSTYPE" == "darwin"* ]]; then
  chmod +x ./fine-macos
  ./fine-macos $FINE_PATCH "$1" $FINE_DST $FINE_FLAGS
else
  chmod +x ./fine-linux
  ./fine-linux $FINE_PATCH "$1" $FINE_DST $FINE_FLAGS
fi
