#!/usr/bin/env bash
# Generate the full iOS app icon set from a single 1024x1024 source PNG.
#
# Source: iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
# Output: per-size PNGs in the same .appiconset plus a regenerated Contents.json
# Uses macOS-builtin `sips` only — no extra tooling required.
# Idempotent — re-run any time the source 1024 changes.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APPICON_DIR="$SCRIPT_DIR/../iosApp/Assets.xcassets/AppIcon.appiconset"
SOURCE="$APPICON_DIR/AppIcon-1024.png"
LAUNCH_LOGO_DIR="$SCRIPT_DIR/../iosApp/Assets.xcassets/LaunchLogo.imageset"

if [[ ! -f "$SOURCE" ]]; then
  echo "error: source icon not found at $SOURCE" >&2
  exit 1
fi

# Apple rejects icons with alpha. The source must already be opaque PNG;
# we sanity-check rather than trying to flatten (sips' alpha handling is flaky).
if [[ "$(sips -g hasAlpha "$SOURCE" | awk '/hasAlpha/{print $2}')" == "yes" ]]; then
  echo "error: $SOURCE has an alpha channel; re-export as opaque PNG before running" >&2
  exit 1
fi

emit() {
  local size="$1" filename="$2"
  sips -z "$size" "$size" "$SOURCE" --out "$APPICON_DIR/$filename" >/dev/null
}

# iPhone
emit 40  AppIcon-20@2x.png    # 20pt @2x notification
emit 60  AppIcon-20@3x.png    # 20pt @3x notification
emit 58  AppIcon-29@2x.png    # 29pt @2x settings
emit 87  AppIcon-29@3x.png    # 29pt @3x settings
emit 80  AppIcon-40@2x.png    # 40pt @2x spotlight
emit 120 AppIcon-40@3x.png    # 40pt @3x spotlight
emit 120 AppIcon-60@2x.png    # 60pt @2x app
emit 180 AppIcon-60@3x.png    # 60pt @3x app

# iPad
emit 20  AppIcon-20.png       # 20pt @1x notification
# 20@2x ipad reuses iPhone 40
emit 29  AppIcon-29.png       # 29pt @1x settings
# 29@2x ipad reuses iPhone 58
emit 40  AppIcon-40.png       # 40pt @1x spotlight
# 40@2x ipad reuses iPhone 80
emit 76  AppIcon-76.png       # 76pt @1x app
emit 152 AppIcon-76@2x.png    # 76pt @2x app
emit 167 AppIcon-83.5@2x.png  # 83.5pt @2x app (iPad Pro)

# App Store marketing — source PNG already lives at this filename, leave as-is.

cat >"$APPICON_DIR/Contents.json" <<'JSON'
{
  "images" : [
    { "filename" : "AppIcon-20@2x.png",   "idiom" : "iphone", "scale" : "2x", "size" : "20x20" },
    { "filename" : "AppIcon-20@3x.png",   "idiom" : "iphone", "scale" : "3x", "size" : "20x20" },
    { "filename" : "AppIcon-29@2x.png",   "idiom" : "iphone", "scale" : "2x", "size" : "29x29" },
    { "filename" : "AppIcon-29@3x.png",   "idiom" : "iphone", "scale" : "3x", "size" : "29x29" },
    { "filename" : "AppIcon-40@2x.png",   "idiom" : "iphone", "scale" : "2x", "size" : "40x40" },
    { "filename" : "AppIcon-40@3x.png",   "idiom" : "iphone", "scale" : "3x", "size" : "40x40" },
    { "filename" : "AppIcon-60@2x.png",   "idiom" : "iphone", "scale" : "2x", "size" : "60x60" },
    { "filename" : "AppIcon-60@3x.png",   "idiom" : "iphone", "scale" : "3x", "size" : "60x60" },
    { "filename" : "AppIcon-20.png",      "idiom" : "ipad",   "scale" : "1x", "size" : "20x20" },
    { "filename" : "AppIcon-20@2x.png",   "idiom" : "ipad",   "scale" : "2x", "size" : "20x20" },
    { "filename" : "AppIcon-29.png",      "idiom" : "ipad",   "scale" : "1x", "size" : "29x29" },
    { "filename" : "AppIcon-29@2x.png",   "idiom" : "ipad",   "scale" : "2x", "size" : "29x29" },
    { "filename" : "AppIcon-40.png",      "idiom" : "ipad",   "scale" : "1x", "size" : "40x40" },
    { "filename" : "AppIcon-40@2x.png",   "idiom" : "ipad",   "scale" : "2x", "size" : "40x40" },
    { "filename" : "AppIcon-76.png",      "idiom" : "ipad",   "scale" : "1x", "size" : "76x76" },
    { "filename" : "AppIcon-76@2x.png",   "idiom" : "ipad",   "scale" : "2x", "size" : "76x76" },
    { "filename" : "AppIcon-83.5@2x.png", "idiom" : "ipad",   "scale" : "2x", "size" : "83.5x83.5" },
    { "filename" : "AppIcon-1024.png",    "idiom" : "ios-marketing", "scale" : "1x", "size" : "1024x1024" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
JSON

# LaunchLogo — reuse the icon, no alpha required but we keep it visually
# consistent with the home-screen icon. Three scales for typical Retina range.
mkdir -p "$LAUNCH_LOGO_DIR"
sips -z 200 200 "$SOURCE" --out "$LAUNCH_LOGO_DIR/LaunchLogo.png"    >/dev/null
sips -z 400 400 "$SOURCE" --out "$LAUNCH_LOGO_DIR/LaunchLogo@2x.png" >/dev/null
sips -z 600 600 "$SOURCE" --out "$LAUNCH_LOGO_DIR/LaunchLogo@3x.png" >/dev/null

cat >"$LAUNCH_LOGO_DIR/Contents.json" <<'JSON'
{
  "images" : [
    { "filename" : "LaunchLogo.png",    "idiom" : "universal", "scale" : "1x" },
    { "filename" : "LaunchLogo@2x.png", "idiom" : "universal", "scale" : "2x" },
    { "filename" : "LaunchLogo@3x.png", "idiom" : "universal", "scale" : "3x" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
JSON

echo "ok: regenerated AppIcon + LaunchLogo from $SOURCE"
