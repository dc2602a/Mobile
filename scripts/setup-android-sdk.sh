#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-$ROOT_DIR/.android-sdk}"
CMDLINE_TOOLS_REV="${CMDLINE_TOOLS_REV:-14742923}"
COMPILE_SDK="${COMPILE_SDK:-37.0}"
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"
ZIP="commandlinetools-linux-${CMDLINE_TOOLS_REV}_latest.zip"
URL="https://dl.google.com/android/repository/${ZIP}"

mkdir -p "$SDK_DIR/cmdline-tools"

if [[ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]]; then
    TMPDIR="$(mktemp -d)"
    trap 'rm -rf "$TMPDIR"' EXIT
    echo "Downloading Android command line tools..."
    curl -fL "$URL" -o "$TMPDIR/$ZIP"
    unzip -q "$TMPDIR/$ZIP" -d "$TMPDIR"
    rm -rf "$SDK_DIR/cmdline-tools/latest"
    mv "$TMPDIR/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
fi

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

set +o pipefail
yes | sdkmanager --licenses >/dev/null
set -o pipefail
sdkmanager \
    "platform-tools" \
    "platforms;android-${COMPILE_SDK}" \
    "build-tools;${BUILD_TOOLS}"

printf 'sdk.dir=%s\n' "$SDK_DIR" > "$ROOT_DIR/local.properties"

echo "Android SDK is ready at $SDK_DIR"
