#!/usr/bin/env bash
# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
set -euo pipefail
cd "$(dirname "$0")"
MODE="${1:-debug}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$PWD/tools/sdk}}"
BT="$SDK/build-tools/35.0.0"
ANDROID="$SDK/platforms/android-35/android.jar"
VERSION=$(sed -n 's/^app.version=//p' app.properties)
[[ "$MODE" == debug || "$MODE" == unsigned || "$MODE" == release ]] || { echo 'Usage: bash build.sh [debug|unsigned|release]' >&2; exit 2; }
for file in "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$BT/apksigner" "$ANDROID"; do
  [[ -f "$file" ]] || { echo "Missing SDK component: $file. See BUILDING.md." >&2; exit 1; }
done
if [[ "$MODE" == release ]]; then
  : "${RELEASE_KEYSTORE:?Set RELEASE_KEYSTORE to your private KnightWin .p12 file}"
  : "${RELEASE_PASSWORD_FILE:?Set RELEASE_PASSWORD_FILE to a private password file}"
  : "${LEGACY_KEYSTORE:?Set LEGACY_KEYSTORE for the Android 8 fallback signer}"
  : "${LEGACY_PASSWORD_FILE:?Set LEGACY_PASSWORD_FILE to its private password file}"
  for file in "$RELEASE_KEYSTORE" "$RELEASE_PASSWORD_FILE" "$LEGACY_KEYSTORE" "$LEGACY_PASSWORD_FILE"; do
    [[ -f "$file" ]] || { echo "Missing private signing input: $file" >&2; exit 1; }
  done
fi
rm -rf generated classes dex
mkdir -p generated classes dex output
rm -f output/classes.jar output/unsigned.apk
"$BT/aapt2" compile --dir app/res -o output/resources.zip
"$BT/aapt2" link -o output/unsigned.apk -I "$ANDROID" --manifest app/AndroidManifest.xml --java generated -A app/assets output/resources.zip
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID:$BT/core-lambda-stubs.jar" -d classes $(find app/src generated -name '*.java')
(cd classes && zip -q -r ../output/classes.jar .)
"$BT/d8" --lib "$ANDROID" --min-api 26 --output dex output/classes.jar
(cd dex && zip -q -u ../output/unsigned.apk classes*.dex)
"$BT/zipalign" -f -p 4 output/unsigned.apk output/aligned.apk
if [[ "$MODE" == unsigned ]]; then
  cp output/aligned.apk "output/Tuhkar-v$VERSION-unsigned.apk"
  echo "Unsigned (not installable until signed): output/Tuhkar-v$VERSION-unsigned.apk"
  exit 0
fi
if [[ "$MODE" == debug ]]; then
  mkdir -p .signing
  if [[ ! -f .signing/debug.p12 ]]; then
    umask 077
    openssl req -x509 -newkey rsa:2048 -nodes -keyout .signing/debug.key -out .signing/debug.crt -days 3650 -subj '/CN=Tuhkar Local Development/' >/dev/null 2>&1
    openssl pkcs12 -export -legacy -out .signing/debug.p12 -inkey .signing/debug.key -in .signing/debug.crt -name debug -passout pass:android
    rm .signing/debug.key
  fi
  DEST="output/Tuhkar-v$VERSION-debug.apk"
  "$BT/apksigner" sign --ks .signing/debug.p12 --ks-key-alias debug --ks-pass pass:android --v4-signing-enabled false --out "$DEST" output/aligned.apk
else
  DEST="output/Tuhkar-v$VERSION.apk"
  "$BT/apksigner" sign --ks "$LEGACY_KEYSTORE" --ks-key-alias legacy --ks-pass "file:$LEGACY_PASSWORD_FILE" \
    --next-signer --ks "$RELEASE_KEYSTORE" --ks-key-alias tuhkar --ks-pass "file:$RELEASE_PASSWORD_FILE" \
    --lineage signing/upgrade-lineage.bin --rotation-min-sdk-version 28 --v4-signing-enabled false \
    --debuggable-apk-permitted false --out "$DEST" output/aligned.apk
fi
"$BT/apksigner" verify --verbose --print-certs "$DEST"
sha256sum "$DEST"
ls -lh "$DEST"
