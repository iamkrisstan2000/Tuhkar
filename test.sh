#!/usr/bin/env bash
# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$PWD/tools/sdk}}"
ANDROID="$SDK/platforms/android-35/android.jar"
[[ -d classes ]] || { echo 'Run bash build.sh debug (or unsigned/release) first.' >&2; exit 1; }
mkdir -p .test-classes
javac -encoding UTF-8 -cp "classes:$ANDROID" -d .test-classes tests/*Test.java
for test in tests/*Test.java; do
  java -cp ".test-classes:classes:$ANDROID" "com.luma.media.$(basename "$test" .java)"
done
for test in tests/*Test.py; do python3 "$test"; done
echo 'Host tests passed. Real Android permission/audio/layout/lifecycle testing is still required.'
