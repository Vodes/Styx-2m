#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$ROOT_DIR/iosApp"
VERSION="${1:-dev}"
OUT_DIR="${OUTPUT_DIR:-$ROOT_DIR/build/ios-sideload/$VERSION}"
DERIVED_DATA_DIR="${DERIVED_DATA_DIR:-$ROOT_DIR/build/ios-derived-data/$VERSION}"
APP_NAME="Styx2m.app"
IPA_NAME="Styx2m.ipa"

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "error: xcodebuild is required." >&2
  exit 1
fi

if ! command -v pod >/dev/null 2>&1; then
  echo "error: CocoaPods is required. Install it with 'sudo gem install cocoapods'." >&2
  exit 1
fi

if [[ ! -f "$IOS_DIR/Pods/Manifest.lock" ]] || ! cmp -s "$IOS_DIR/Podfile.lock" "$IOS_DIR/Pods/Manifest.lock"; then
  echo "Installing CocoaPods dependencies..."
  (cd "$IOS_DIR" && pod install)
fi

echo "Building unsigned iOS app..."
rm -rf "$DERIVED_DATA_DIR"
mkdir -p "$OUT_DIR"

(
  cd "$IOS_DIR"
  xcodebuild \
    -workspace iosApp.xcworkspace \
    -scheme iosApp \
    -configuration Release \
    -destination generic/platform=iOS \
    -derivedDataPath "$DERIVED_DATA_DIR" \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGNING_REQUIRED=NO \
    build
)

APP_PATH="$DERIVED_DATA_DIR/Build/Products/Release-iphoneos/$APP_NAME"
if [[ ! -d "$APP_PATH" ]]; then
  echo "error: expected built app at '$APP_PATH'." >&2
  exit 1
fi

STAGING_DIR="$(mktemp -d "${TMPDIR:-/tmp}/styx2m-ios-ipa.XXXXXX")"
trap 'rm -rf "$STAGING_DIR"' EXIT

mkdir -p "$STAGING_DIR/Payload"
cp -R "$APP_PATH" "$STAGING_DIR/Payload/"

IPA_PATH="$OUT_DIR/$IPA_NAME"
rm -f "$IPA_PATH" "$OUT_DIR/checksums.sha384"
ditto -c -k --sequesterRsrc --keepParent "$STAGING_DIR/Payload" "$IPA_PATH"
shasum -a 384 "$IPA_PATH" | tee "$OUT_DIR/checksums.sha384" >/dev/null

echo "Created $IPA_PATH"
