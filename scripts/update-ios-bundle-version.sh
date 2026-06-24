#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFO_PLIST="${1:-}"

if [[ -z "$INFO_PLIST" ]]; then
  if [[ -n "${TARGET_BUILD_DIR:-}" && -n "${INFOPLIST_PATH:-}" ]]; then
    INFO_PLIST="$TARGET_BUILD_DIR/$INFOPLIST_PATH"
  else
    echo "error: pass an Info.plist path or run from an Xcode build phase." >&2
    exit 1
  fi
fi

if [[ ! -f "$INFO_PLIST" ]]; then
  echo "error: Info.plist not found at '$INFO_PLIST'." >&2
  exit 1
fi

MARKETING_VERSION=""
CURRENT_PROJECT_VERSION=""

while IFS='=' read -r key value; do
  case "$key" in
    MARKETING_VERSION) MARKETING_VERSION="$value" ;;
    CURRENT_PROJECT_VERSION) CURRENT_PROJECT_VERSION="$value" ;;
  esac
done < <("$ROOT_DIR/gradlew" --no-configuration-cache -p "$ROOT_DIR" -q :styx2m:printIosBundleVersion)

if [[ -z "$MARKETING_VERSION" || -z "$CURRENT_PROJECT_VERSION" ]]; then
  echo "error: failed to resolve iOS bundle version from Gradle." >&2
  exit 1
fi

/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $MARKETING_VERSION" "$INFO_PLIST"
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $CURRENT_PROJECT_VERSION" "$INFO_PLIST"

echo "Set iOS bundle version to $MARKETING_VERSION ($CURRENT_PROJECT_VERSION)"
