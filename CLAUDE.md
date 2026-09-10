# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kotlin Multiplatform + Compose Multiplatform app that renders the live camera feed as a variable-width L-system curve: dark image regions become thick line segments, bright regions thin. Store name "L-system Camera", package/bundle id `se.kjellstrand.lsystemcamera`.

Almost everything — the L-system library, the renderer, the whole UI — lives in `shared/`. `app/` is the Android entry point (a ~20-line `MainActivity`, manifest, launcher icon, signing, Play publishing) and `iosApp/` is the Xcode host (SwiftUI wrapper, Info.plist, fastlane). Android needs a device or emulator with a camera (minSdk 28, targetSdk 37); the iOS simulator has no camera and falls back to a photo picker.

## Build and test

Gradle wrapper (Gradle 9.7.1, AGP 9.4.0, Kotlin 2.2.10, Compose Multiplatform 1.9.0, `material-icons-core` 1.7.3 — icons ship separately and stopped there, CameraX 1.6.2, compileSdk/targetSdk 37). Versions live in `gradle/libs.versions.toml`. Android, from the repo root in PowerShell:

```
.\gradlew.bat :app:assembleDebug             # build APK
.\gradlew.bat :app:installDebug              # install on connected device/emulator
.\gradlew.bat :shared:testDebugUnitTest      # JVM unit tests (shared/src/androidUnitTest)
.\gradlew.bat :shared:testDebugUnitTest --tests "se.kjellstrand.lsystem.LSystemGeneratorTest"
.\gradlew.bat :app:bundleRelease             # signed AAB, needs keystore.properties
```

The tests are Android unit tests, not `jvmTest`: there is no `jvm()` target, because the four `expect` seams would have needed dead JVM actuals just to keep it compiling. They still run on the host JVM.

Common code can be compile-checked on Windows without a Mac:

```
.\gradlew.bat :shared:compileKotlinIosSimulatorArm64
```

`:app:lintDebug` is clean apart from version-available notices; keep it that way. There are no instrumented tests and no product flavors.

iOS, on a Mac (first time, then per Xcode-version change):

```sh
cd iosApp
cp Configuration/Signing.xcconfig.template Configuration/Signing.xcconfig
xcodegen generate            # iosApp.xcodeproj is generated and gitignored
```

Headless build and the UI/screenshot test (both from `iosApp/`, see `iosApp/README.md`):

```sh
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO build

xcodebuild test -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO \
  -only-testing:iosAppUITests
```

On macOS export `JAVA_HOME=$(/usr/libexec/java_home)` before any `./gradlew` (the Xcode pre-build script runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so this bites through `xcodebuild` too). `gradlew` is committed with the execute bit set (mode 100755).

Gradle properties that matter (`gradle.properties`):

- `android.builtInKotlin=false` — bypasses AGP 9's built-in Kotlin so `com.android.library` and `kotlin-multiplatform` can coexist until Compose Multiplatform supports `com.android.kotlin.multiplatform.library`. Consequence: `app/` applies `org.jetbrains.kotlin.android` explicitly.
- `android.newDsl=false` — set alongside it for the same coexistence (the file gives no separate reason).
- `kotlin.native.ignoreDisabledTargets=true` — the iOS targets are declared but cannot be built on Windows.

## Release

### Android

Signing and publishing follow the same shape as FieldShootingTimer, so the `/release` skill works here:

- `keystore.properties` at repo root (gitignored) with `storeFile` (absolute path, forward slashes), `storePassword`, `keyAlias=key0`, `keyPassword`. The upload key is the one enrolled in Play App Signing.
- `play-account.json` at repo root (gitignored): Play Console service-account key with Release manager role.
- `app/build.gradle.kts` holds `appVersionCode` / `appVersionName`; the Gradle Play Publisher plugin uploads to the **internal** track (`:app:publishReleaseBundle`). The release build is minified with R8 (`proguard-android-optimize.txt`, no custom keep rules needed so far) and the plugin uploads `app/build/outputs/mapping/release/mapping.txt` with the bundle. `installRelease` fails on a device that has the debug build: uninstall it first, the signatures differ. Production promotion is manual in Play Console.
- Release notes: `app/src/main/play/release-notes/en-GB/default.txt` (en-GB is the listing's default locale in Play Console; the app is English-only).
- Store listing graphics (512px icon, phone screenshots) live in `app/src/main/play/listings/en-GB/graphics/` and are uploaded by `:app:publishReleaseListing`, which is separate from the bundle upload. Screenshots are 1080x1920 emulator captures (`adb shell wm size 1080x1920` on a Pixel 9 AVD); the icon is the launcher foreground cropped to its safe zone over white.
- Privacy policy: `PRIVACY.md` at repo root; its GitHub URL is the one entered in Play Console.

### iOS

Runs locally from a Mac, never from CI. `iosApp/fastlane/Fastfile` has three lanes: `beta` (archive + TestFlight), `release` (archive + App Store, not submitted) and `metadata` (ASC text and screenshots, no binary). Credentials come from `iosApp/fastlane/.env`, copied from `.env.template` (gitignored: ASC API key id/issuer/`.p8` path, Apple ID, team id). Store text lives under `iosApp/fastlane/metadata/en-GB` — it is generated wording, read it before pushing. `CFBundleShortVersionString` in `iosApp/iosApp/Info.plist` is kept equal to `appVersionName` in `app/build.gradle.kts` (2.2.0 today); `CFBundleVersion` is `$(CURRENT_PROJECT_VERSION)` and fastlane stamps it with `git rev-list --count HEAD` (override with `BUILD_NUMBER=<n>`). The icon set is regenerated from `AppIcon-1024.png` by `bash iosApp/scripts/generate-app-icons.sh` (macOS `sips` only) — the 1024 is currently the 512 px Play icon upscaled, so re-render a true 1024 if it ever matters. Full detail, including the one-off keychain step certificates need, is in `iosApp/README.md`.

## Architecture

| Module / source set | Holds |
|---|---|
| `shared/src/commonMain` | `se.kjellstrand.lsystem` (the L-system library) and `se.kjellstrand.lsystemcamera` (UI, view model, renderer, the `expect` seams), plus `composeResources` (strings, 19 picker icons) |
| `shared/src/androidMain` | the four `.android.kt` actuals only |
| `shared/src/iosMain` | the four `.ios.kt` actuals plus `MainViewController.kt` |
| `shared/src/androidUnitTest` | all tests; they need `java.awt`/`javax.imageio` to render PNGs, so they run on the host JVM |
| `app/` | `MainActivity`, manifest, launcher icon, signing config, Play listing/notes |
| `iosApp/` | XcodeGen spec, SwiftUI wrapper, Info.plist, PrivacyInfo, asset catalog, fastlane, `iosAppUITests` |

`shared` is a KMP library with `androidTarget`, `iosArm64` and `iosSimulatorArm64`, the iOS targets producing a **static** `Shared.framework`.

Two packages with a deliberate boundary:

- `se.kjellstrand.lsystem` is the pure L-system library, vendored into the app (intended to be broken back out to a jar some day). It has no platform imports at all — only `kotlin.math` and its own types — and compiles for Kotlin/Native. Keep it that way.
  - `model/LSystem.kt` holds the catalog of systems (`LSystem.systems`), each with axiom, rules, angle, iteration range, `defaultIterations` (what the picker selects; defaults to `maxIterations - 1`) and per-system width tuning constants (`lineWidthExp`, `lineWidthBold`). Adding a curve means adding an entry here plus an icon mapping in `MainScreen.kt` (`iconFor`).
  - `LSystemGenerator` turns an `LSystem` + iteration count into a normalized polyline of `LSTriple(x, y, w)` in 0..1 space, then maps luminance to `w`, smooths widths, and insets by a side buffer.
  - `VariableWidthCurve.kt` (`buildHullFromPolygon`) converts that width-annotated polyline into a closed outline hull (left side + reversed right side). It keeps private module-level scratch buffers reused across frames; not thread-safe by design, only ever called from the single frame-delivery thread.

- `se.kjellstrand.lsystemcamera` in `commonMain` is the whole app.
  - `MainScreen.kt` is the whole UI: Material 3 `Scaffold` with no app bar, a plain title, square output `Image` in a rounded `Surface`, a button row (switch camera, shutter, share) where the shutter sets `LSystemViewModel.frozen` and is replaced by a resume button while the frame is held, a horizontal `LazyRow` of icon tiles for picking the system (`displayName` splits camel case with a lookbehind regex — that also works on Kotlin/Native), and a `SingleChoiceSegmentedButtonRow` (Contrast / Brightness / Complexity) that swaps a single `Slider` with its value shown. The selected segment is `rememberSaveable` local state, not part of `UiState`. Permission rationale with "Allow camera" and "Open settings" buttons replaces the image until granted.
  - `Theme.kt` (`LSystemTheme`) takes `dynamicColorSchemeOrNull` and falls back to the M3 baseline light/dark scheme.
  - `LSystemViewModel` owns `UiState` (system, iterations, contrast, brightness) in a `MutableStateFlow`, plus `frame: MutableStateFlow<ImageBitmap?>`, `frontCamera` and `frozen`, and the single `ImageAnalyzer` instance. `onFrame` is called from the camera's delivery thread, one frame at a time — that serialisation *is* the thread model, there is no executor of its own any more. `select(system)` resets iterations to the new system's `defaultIterations`.
  - `ImageAnalyzer.analyze(LumaFrame, UiState)` does the per-frame work: `readLuminance` reads the Y plane into a center-cropped luminance grid rotated by `LumaFrame.rotationDegrees`, maps it to line widths, builds the hull, draws it as a quadratic-bezier `Path` into one of two 1024x1024 `ImageBitmap`s (double buffered so the UI never reads the one being drawn) with Compose's own `Canvas`/`Paint`/`Matrix` — no Android graphics types. The polyline is regenerated only when system or iterations change; everything else is per-frame width mapping.
  - `LumaFrame` is the one-interface camera abstraction: `width`, `height`, `rowStride`, `pixelStride`, `rotationDegrees`, `byteAt(offset)`. Android's `ImageProxy.planes[0]` and iOS' `CVPixelBuffer` plane 0 are both strided Y planes, so the same loop reads both.
  - Four `expect` seams, one file each: `rememberCameraSource(front, onFrame)`, `rememberCameraPermission(): CameraPermissionState`, `rememberShareImage(): (ImageBitmap) -> Unit`, `dynamicColorSchemeOrNull(darkTheme): ColorScheme?`.
  - `Format.kt` (`format2`, `formatSigned1`) formats the slider labels: `String.format` is JVM-only, and it followed the device locale, so the decimal separator is now always `.`.

- `androidMain` actuals: `CameraSource.android.kt` binds a CameraX `ImageAnalysis` use case alone (no preview) at roughly 200x200 with `STRATEGY_KEEP_ONLY_LATEST` on a `Executors.newSingleThreadExecutor`, rebinding front/back when `front` flips and falling back to back if there is no front camera; `CameraPermission.android.kt` re-checks the grant on every `ON_RESUME`; `Share.android.kt` writes a PNG to the cache dir and hands it over with `ShareCompat` + `FileProvider`; `DynamicColor.android.kt` returns the wallpaper scheme on API 31+.

- `iosMain` actuals: `CameraSource.ios.kt` runs an `AVCaptureSession` + `AVCaptureVideoDataOutput` in 420v (`kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange`) with `alwaysDiscardsLateVideoFrames` and preset `AVCaptureSessionPreset352x288`, rotating in the pipeline via `videoRotationAngle` where supported (iOS 17+) and otherwise letting `readLuminance` turn the frame; when there is no capture device at all (the simulator) it presents a `PHPickerViewController` instead and re-emits the picked still on a 100 ms ticker so slider changes redraw. `CameraPermission.ios.kt` uses `AVCaptureDevice.authorizationStatusForMediaType` and reports "granted" when there is no device to authorise. `Share.ios.kt` encodes the `ImageBitmap` to PNG through skiko (`Image.makeFromBitmap(...).encodeToData`) and presents a `UIActivityViewController`. `DynamicColor.ios.kt` returns null. `MainViewController.kt` is the Xcode entry point: `ComposeUIViewController { LSystemTheme { MainScreen(viewModel { LSystemViewModel() }) } }`.

Frame flow: camera (or picker) -> platform `rememberCameraSource` -> `LumaFrame` -> `LSystemViewModel.onFrame` -> `ImageAnalyzer.analyze` on the delivery thread -> `frame` StateFlow -> Compose `Image`.

## Things to know

- Iteration counts near each system's `maxIterations` get expensive. Width constants and defaults are set by numbers, not by eye: `CurveTuningTest` (`shared/src/androidUnitTest`) prints, per curve and iteration, the spacing between neighbouring strands (`gap`), the shrink ratio between iterations (this is what `lineWidthExp` should be) and how much of the gap the max width fills (`fill`; target about 0.8, so a black region never merges strands), and renders every iteration at max width to `shared/build/curve-tuning/<Name>.png` with the default framed. Defaults sit where `gap` is roughly 0.011-0.016 (a 64-81 step grid). Curves that touch themselves at corners (Dragon, TwinDragon, Terdragon, Fudgeflake, Cross) need fill around 0.35-0.45 or they turn into blobs. TwinDragon is capped at 12 iterations because 13 rendered nothing.
- Picker icons live in `shared/src/commonMain/composeResources/drawable/<snake_case>.png` and are referenced as `Res.drawable.<name>` from `iconFor` in `MainScreen.kt`. They are generated: `CurvePreviewTest` (`shared/src/androidUnitTest`) renders every catalog curve to `shared/build/curve-previews/<Name>-icon.png` (and `-max.png` for tuning) on each unit-test run; copy the icon PNG into `composeResources/drawable` under a snake_case name and add it to `iconFor`. Rules that need a fixed turn angle, unit steps and one continuous line (no brackets) are the only ones the renderer draws well; space-filling curves with even density show the camera image best.
- **Vector XML in `composeResources` must use literal colours like `#FFFFFFFF`, never `@android:color/...`.** The Compose Multiplatform resource parser throws `Invalid color value` at runtime and nothing at build time catches it (`ic_switch_camera.xml` is the only one).
- UI strings live in `shared/src/commonMain/composeResources/values/strings.xml` and are read as `Res.string.*`. The Android manifest label still comes from `app/src/main/res/values/strings.xml`, which is why `app_name` exists in both.
- The iOS simulator has no capture device, so the app presents the photo picker instead — add a photo first with `xcrun simctl addmedia booted <some.jpg>`, and tapping switch-camera re-presents the picker. The real AVFoundation capture path (420v plane, `videoRotationAngle`) has never run on a device and is unverified.
- Performance measured in the debug simulator build (PLAN-IOS.md task 4b): `analyze()` at Moore/default 10 ms median, Moore at max complexity 124 ms (335 ms for the first frame, which regenerates the polyline), Peano/default 35 ms.
- There is no orientation lock (Android 17 ignores them on large screens anyway). `ImageAnalyzer.readLuminance` rotates the crop per frame using `LumaFrame.rotationDegrees`, and the layout is width-capped and centered so landscape and tablets work. The Android activity is recreated on rotation, which rebinds `ImageAnalysis` with the new target rotation; the ViewModel keeps the renderer alive across that.
- `KochSnowFlake` exists in the catalog but is filtered out of the picker.
- Anything needing a Mac (Kotlin/Native link, `xcodebuild`, the UI test, fastlane) runs on the host `macmini` over ssh, which has a clone at `~/source/LSystemCamera`. Run one Gradle or `xcodebuild` at a time there.
- This repo is checked out on Windows with `core.autocrlf=true`, so shell scripts would otherwise land as CRLF and fail on the Mac. `.gitattributes` pins `gradlew` and `*.sh` to LF; verify with `git ls-files --eol gradlew iosApp/scripts/` (want `i/lf w/lf`).
- CI (`.github/workflows/ci.yml`) runs two jobs: `android` (`:shared:testDebugUnitTest`, `:app:assembleDebug`) and `ios` on macOS (`:shared:iosSimulatorArm64Test` — a NO-SOURCE no-op until an `iosTest` source set exists, then xcodegen, a simulator build and the UI test, uploading `/tmp/shots/*.png`).
