#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOL="${RECEIPT_TOOLCHAIN:-$ROOT/toolchain}"
export JAVA_HOME="$TOOL/jdk"
BT="$TOOL/android-sdk/build-tools/34.0.0"
PLATFORM="$TOOL/android-sdk/platforms/android-34"
AJAR="$PLATFORM/android.jar"
PATH="$JAVA_HOME/bin:$BT:$PATH"

OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/aidl-src" "$OUT/classes" "$OUT/dex" "$OUT/gen" "$OUT/res"

echo "== 1. aidl -> java"
find "$ROOT/aidl" -name '*.aidl' -print0 | while IFS= read -r -d '' f; do
  aidl -p "$PLATFORM/framework.aidl" -I "$ROOT/aidl" -o "$OUT/aidl-src" "$f"
done
find "$OUT/aidl-src" -name '*.java'

echo "== 2. javac"
find "$ROOT/src" "$OUT/aidl-src" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -nowarn -Xlint:-options \
  -classpath "$AJAR" -d "$OUT/classes" @"$OUT/sources.txt"

echo "== 3. d8"
find "$OUT/classes" -name '*.class' > "$OUT/classlist.txt"
d8 --lib "$AJAR" --min-api 29 --output "$OUT/dex" $(cat "$OUT/classlist.txt")

echo "== 4. aapt2 compile resources"
aapt2 compile --dir "$ROOT/res" -o "$OUT/res.zip"

echo "== 5. aapt2 link"
aapt2 link -o "$OUT/base.apk" -I "$AJAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -R "$OUT/res.zip" --auto-add-overlay --java "$OUT/gen" --min-sdk-version 29 --target-sdk-version 31

echo "== 6. add classes.dex"
cd "$OUT"
cp base.apk unsigned.apk
zip -q -j unsigned.apk dex/classes.dex

echo "== 7. zipalign"
zipalign -f -p 4 unsigned.apk aligned.apk

echo "== 8. keystore + sign"
KS="$ROOT/debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Receipt Print, OU=SovTech, O=SovTech, L=, ST=, C=SV" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$ROOT/receiptprint.apk" aligned.apk

echo "== done: $ROOT/receiptprint.apk"
ls -la "$ROOT/receiptprint.apk"
