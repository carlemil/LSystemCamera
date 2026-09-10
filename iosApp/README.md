# iosApp — L-system Camera iOS host

A minimal SwiftUI app that embeds the Compose Multiplatform UI exposed by
`:shared` as a static `Shared.framework`.

## What's committed

- `iosApp/iosApp/iosAppApp.swift` — `@main` SwiftUI app, nothing but the
  `WindowGroup`.
- `iosApp/iosApp/ContentView.swift` — wraps `MainViewControllerKt.MainViewController()`
  from the shared framework in a `UIViewControllerRepresentable`.
- `iosApp/iosApp/Info.plist` — portrait + both landscape orientations,
  `NSCameraUsageDescription` (the live camera; the PHPicker fallback needs
  no photo-library permission, it runs out of process). `CFBundleVersion` is
  `$(CURRENT_PROJECT_VERSION)` so fastlane can stamp a unique build number
  on every archive. `CFBundleShortVersionString` is kept aligned with the
  Android `appVersionName` in `app/build.gradle.kts`.
- `iosApp/iosApp/Assets.xcassets/` — AppIcon (regenerated from
  `AppIcon-1024.png` by `scripts/generate-app-icons.sh`), `LaunchLogo`
  image set, and `LaunchBackground` color set referenced by
  `UILaunchScreen` in Info.plist.
- `iosApp/iosApp/PrivacyInfo.xcprivacy` — no tracking, no collected data and
  no required-reason API declarations: the app has no persistence at all
  (no datastore, no UserDefaults, no file timestamps).
- `iosApp/iosAppUITests/ScreenshotTests.swift` — one XCUITest that drives the
  app and saves PNGs of each state (see "Screenshot / smoke test" below).
- `iosApp/Configuration/Signing.xcconfig.template` — copy to
  `Signing.xcconfig` (gitignored) with your real `DEVELOPMENT_TEAM`.
- `iosApp/fastlane/` — `Fastfile` lanes for TestFlight (`beta`),
  App Store (`release`), and metadata-only (`metadata`) uploads.
  `metadata/en-GB/` holds the ASC text under git (en-GB matches the Play
  listing's default locale; the app is English-only).
- `iosApp/project.yml` — the [XcodeGen](https://github.com/yonaskolb/XcodeGen)
  spec. Running `xcodegen generate` produces `iosApp.xcodeproj` deterministically.

## What's NOT committed

- The Xcode project file (`iosApp.xcodeproj`). Its `project.pbxproj` is fragile
  to hand-author and noisy across Xcode versions, so it is **generated** from
  `project.yml` and gitignored. Regenerate it with `xcodegen generate`.

## First-time setup on a Mac

```sh
brew install xcodegen           # one-time
cd iosApp
cp Configuration/Signing.xcconfig.template Configuration/Signing.xcconfig
xcodegen generate               # produces iosApp.xcodeproj (gitignored)
open iosApp.xcodeproj           # then build for an iOS 16+ simulator
```

Building the app automatically (re)builds the shared framework — `project.yml`
adds a pre-build Run Script phase that calls
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

Headless simulator build:

```sh
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO build
```

## How the framework gets linked

- a **pre-build Run Script** phase runs
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (it builds only the arch
  Xcode is currently targeting and writes it under
  `iosApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`);
- **`FRAMEWORK_SEARCH_PATHS`** points at that directory;
- **`OTHER_LDFLAGS`** adds `-framework Shared`. The framework is *static*, so it
  is linked, not embedded (no Copy Frameworks phase).

## Simulator caveat

The simulator has no capture device. `rememberCameraSource` detects that and
presents a `PHPickerViewController` instead, so the app asks for a photo at
launch and renders that still. Add one first with
`xcrun simctl addmedia booted <some.jpg>`. Tapping the switch-camera button
re-presents the picker. The real AVFoundation capture path needs a device.

## Screenshot / smoke test

`iosAppUITests/ScreenshotTests.swift` drives the whole screen headlessly — it
picks the newest photo, moves the contrast and complexity sliders, switches
curve, freezes the frame and opens the share sheet, saving a PNG of each state
to `/tmp/shots/`. That is the only way to see the render on a Mac with no
attached GUI session (`Simulator.app` has no window there).

```sh
sips -s format jpeg ../app/src/main/play/listings/en-GB/graphics/phone-screenshots/1-moore.png --out /tmp/photo.jpg
xcrun simctl addmedia booted /tmp/photo.jpg
xcodebuild test -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO \
  -only-testing:iosAppUITests
```

Compose sets no test tags, so the test addresses elements by accessibility
label (`Hold frame`, `Share image`, `Contrast`, curve names). Two exceptions,
both worked around in the test: Compose's `Slider` surfaces as a plain "Other"
with a percentage value rather than as `app.sliders`, and the PHPicker's grid
tiles report as not hittable (they are in a remote view service), so both are
driven by coordinate.

## Manual acceptance criteria

- App launches; the curve renders from the camera (or the picked photo).
- The icon strip switches curves; the three sliders change the image.
- Shutter freezes the frame, resume goes live again, share opens the
  system share sheet.
- Denying camera access shows the rationale with a working "Open settings".

## Releasing

The iOS release pipeline runs **locally from a Mac** (not from CI). One-time
setup on a Mac with Xcode 16+:

```sh
brew install fastlane
cd iosApp
cp fastlane/.env.template fastlane/.env
$EDITOR fastlane/.env            # ASC API key + Apple ID + team ID
```

The ASC API key (`ASC_KEY_ID`, `ASC_ISSUER_ID`, `.p8` path in `ASC_KEY_PATH`)
is generated at [appstoreconnect.apple.com/access/api](https://appstoreconnect.apple.com/access/api).
Apple only lets you download the `.p8` once.

The `beta`/`release` lanes call `get_certificates` and
`get_provisioning_profile`, which create the Apple Distribution cert and the
App Store profile via the API key on first run. macOS then needs one
keychain step before `codesign` will use the freshly imported private key:

```sh
security set-key-partition-list -S apple-tool:,apple:,codesign:,productbuild: \
    -s -k '<your-mac-password>' ~/Library/Keychains/login.keychain-db
```

Then:

```sh
fastlane beta        # archive + upload to TestFlight
fastlane release     # archive + upload to App Store (not submitted for review)
fastlane metadata    # push en-GB ASC text + screenshots without a binary
```

`fastlane beta` stamps `CFBundleVersion` with `git rev-list --count HEAD`.

### Regenerating app icons

If you change `AppIcon-1024.png`, regenerate the full size set:

```sh
bash iosApp/scripts/generate-app-icons.sh
```

The script only requires the macOS-builtin `sips`. It also regenerates the
`LaunchLogo` image set used by the launch screen.

## Out of scope (intentional)

- `fastlane match` for centralized cert/profile storage.
- Store-screenshot automation: the UI test saves raw PNGs for inspection, it
  does not stage App Store shots (no device matrix, no `snapshot`).
- CI-driven TestFlight upload. CI builds the simulator app and runs the UI
  test — see `.github/workflows/ci.yml` (the `ios` job).
