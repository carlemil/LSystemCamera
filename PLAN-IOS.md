# iOS port of L-system Camera

Source of truth for the port. Orchestrator workflow: one task = one reviewed commit;
the Android gate must stay green after every task:

```
.\gradlew.bat :shared:testDebugUnitTest :shared:compileCommonMainKotlinMetadata :app:assembleDebug --rerun-tasks
```

## Context

The Android layer is ~560 lines; the "low level" camera code is the 25-line
`ImageAnalyzer.readLuminance` loop over the Y plane of a YUV frame. iOS gives the
identical layout (`AVCaptureVideoDataOutput`, 420 bi-planar, `CVPixelBuffer` plane 0
= Y with a row stride), so the loop stays as is behind a tiny `LumaFrame` interface.
`se.kjellstrand.lsystem` is pure Kotlin bar two JVM imports.

Decisions (user, 2026-09-10): simulator-only on the Mac mini (`ssh macmini`, no
iPhone; photo-picker frame source stands in for the camera there), repo restructured
to the FieldShootingTimer shape (`shared/` + `app/` + `iosApp/`), bundle id
`se.kjellstrand.lsystemcamera`. Templates: `D:\source\FieldShootingTimer` (module
layout, `iosApp/project.yml`, fastlane, CI) and `D:\source\Markera`
(`iosMain` AVFoundation camera, permission, share, dynamic colour actuals).
Versions pinned to what builds on the Mac mini today: Kotlin 2.2.10, Compose
Multiplatform 1.9.0 (both sibling repos); AGP stays 9.4.0 unless it refuses the
multiplatform plugin, then 9.2.1 (Markera).

## Architecture

| Android-only today | Replacement |
|---|---|
| `java.lang.Math.PI`, `java.util.*` in `LSystemGenerator` | `kotlin.math.PI`; drop the import |
| `ImageProxy` into `ImageAnalyzer.analyze` | `LumaFrame` interface: `width`, `height`, `rowStride`, `pixelStride`, `rotationDegrees`, `byteAt(offset)`; Android actual over `planes[0]`, iOS over `CVPixelBufferGetBaseAddressOfPlane(buf, 0)` |
| `android.graphics.Bitmap/Canvas/Path/Paint/Matrix` | `androidx.compose.ui.graphics.ImageBitmap/Canvas/Path/Paint/Matrix` (same Skia/Android canvas underneath; no iOS actual needed) |
| `Executors.newSingleThreadExecutor` + `ImageAnalysis.Analyzer` | `Dispatchers.Default.limitedParallelism(1)` + `Channel(CONFLATED)` (keep-latest) |
| CameraX bind in `MainActivity` | `expect @Composable fun rememberCameraSource(front, onFrame)`; iOS = `AVCaptureSession` + `AVCaptureVideoDataOutput`, `alwaysDiscardsLateVideoFrames`, preset 352x288, `videoRotationAngle` so frames arrive upright; `PHPicker` fallback when no capture device |
| Permission read + Settings intent | `expect rememberCameraPermission()` (Markera `CameraPermission.kt` / `.ios.kt`) |
| `ShareCompat` + `FileProvider` | `expect shareImage(ImageBitmap)`; iOS `UIActivityViewController` |
| `dynamicColorScheme` | Markera `expect dynamicColorSchemeOrNull` |
| `R.string`, `R.drawable` | composeResources (`Res.string`, `Res.drawable`); `android.R.drawable.*` icons → `Icons.Default.Share` / `PlayArrow` |

## Tasks

- [x] **1. Restructure** to `shared/` (KMP library, androidTarget + jvm + iosArm64 +
  iosSimulatorArm64, static `Shared` framework) + `app/` (Android entry, signing,
  Play). Move both packages to `commonMain` where they compile, `androidMain`
  otherwise; strings + 19 icons to composeResources; tests to `jvmTest`
  (`CurveTuningTest`/`CurvePreviewTest` use `java.awt`, stay JVM). Android
  behaviour identical. `compileKotlinIosSimulatorArm64` may still fail (that is task 2).
- [x] **2. Platform seams**: `LumaFrame`, Compose-graphics renderer, coroutine
  analyzer, `rememberCameraSource`/`rememberCameraPermission`/`shareImage`/
  `dynamicColorSchemeOrNull` expect + Android actuals. Gate:
  `:shared:compileKotlinIosSimulatorArm64` passes with stub iOS actuals (`TODO()`).
- [x] **3. iOS actuals**: AVFoundation camera source, picker fallback, permission,
  share, dynamic colour, `MainViewController()`.
- [x] **4. iosApp/**: XcodeGen spec, Swift wrapper, Info.plist with
  `NSCameraUsageDescription`, PrivacyInfo, icon, fastlane, CI job. Build and run in
  the simulator on the Mac mini; measure `analyze()` time with the picker source.
- [x] **4b. Simulator UI test**: an `iosAppUITests` target (FieldShootingTimer's
  `ScreenshotTests.swift` shape) that picks the first library photo, waits for the
  curve, screenshots, moves each slider, taps shutter/share, and reads the
  `analyze()` timing from the log. This is the only way to drive the picker on the
  headless Mac mini (no GUI session for `osascript`, `simctl` has no tap) and it
  doubles as the App Store screenshot lane.
- [x] **5. Docs**: CLAUDE.md module table + Mac steps, README, memory note.

## Follow-ups / queue

- Task 2 outcome: the `jvm()` target was dropped (four expects would have needed dead JVM stubs); tests live in `shared/src/androidUnitTest` and run with `:shared:testDebugUnitTest` on the host JVM. `androidMain` holds only the four actuals (`CameraSource`, `CameraPermission`, `Share`, `DynamicColor` `.android.kt`). Slider labels now use `Format.kt` (always `.` decimal; the old `String.format` followed the device locale).
- Check in the simulator (task 4): `displayName()` in `MainScreen.kt` uses a lookbehind regex; Kotlin/Native's regex engine must split "SierpinskiTriangle" the same way.
- Task 4 outcome: `iosApp/` builds with `xcodegen generate` + `xcodebuild` (clean build on the Mac, EXIT 0), installs and launches in the iPhone 17 Pro Max simulator, and presents the PHPicker over the app at first composition (screenshot: picker sheet naming "L-system Camera", app title visible behind it). App icon is the 512 px Play icon upscaled to 1024 with `sips` (follow-up: render a true 1024 from `CurvePreviewTest`). App Store text under `iosApp/fastlane/metadata/en-GB` is generated wording, read it before `fastlane metadata`. `:shared:iosSimulatorArm64Test` is a NO-SOURCE no-op in CI until an `iosTest` source set exists.
- Task 4b outcome: `iosAppUITests/ScreenshotTests.swift` (run: `xcodebuild test … -only-testing:iosAppUITests`, ~32 s, PNGs in `/tmp/shots`) verified on the iPhone 17 Pro Max simulator: curve renders from the picked photo with correct orientation (a photo with a black left half and a black top bar renders black-left, black-top: no mirror, no flip), contrast and complexity sliders redraw, curve switch resets complexity to the new default, shutter freezes, share sheet shows the PNG, `displayName()` labels split correctly on Kotlin/Native ("Quadratic Gosper", "Sierpinski Curve"). `analyze()` timing in the debug simulator build: Moore default 10 ms median, Moore max complexity 124 ms (335 ms first frame, polyline regen), Peano default 35 ms. Compose's `Slider` is not in `app.sliders` (matched by a value ending in `%`), PHPicker tiles are not hittable (coordinate tap on the `PXGGridLayout-Info` image). CI runs the UI test on the newest available iPhone simulator (green on GitHub since 04de163).
- Was unverified before 4b: grey rows from `CGBitmapContext` are top-down; `Info.plist` needs `NSCameraUsageDescription` and `NSPhotoLibraryUsageDescription` (the picker path uses PhotosUI); picked-photo EXIF orientation is ignored. The real-camera path (420v plane, `videoRotationAngle`) needs a device and stays unverified.
- Pre-existing: `hasCamera(DEFAULT_FRONT_CAMERA)` can throw `CameraInfoUnavailableException`, now inside a `DisposableEffect`.
- Task 5 docs: composeResources vector XML must use literal colours (`#FFFFFFFF`), not `@android:color/...`; the Compose Multiplatform parser throws `Invalid color value` at runtime and no build step catches it (found 2026-09-10 on the emulator). Tests now run with `:shared:jvmTest` and write PNGs under `shared/build/`; icons live in `shared/src/commonMain/composeResources/drawable`; `android.builtInKotlin=false` so `kotlin-android` is applied explicitly in `app/`.

## Status

Tasks 1-3 done (2026-09-10). Tasks 1-2 verified on the Pixel 9 emulator (live, freeze, front camera, share, rationale, re-grant); task 3 verified by compile + link of the iosSimulatorArm64 framework on the Mac mini (clone at `~/source/LSystemCamera`, sync uncommitted work with `tar -cf - shared ... | ssh macmini "cd ~/source/LSystemCamera && tar -xf -"`, then `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` with `JAVA_HOME=$(/usr/libexec/java_home)`). All tasks done (2026-09-10). `iosApp/` builds, launches and renders in the simulator, driven by the UI test; CLAUDE.md, README.md and iosApp/README.md describe the KMP layout. `NSPhotoLibraryUsageDescription` was dropped from Info.plist (PHPicker needs no permission) so PRIVACY.md stays true. Remaining follow-ups are listed above; the real-camera path needs an iPhone.

TestFlight prep (2026-09-11): `:shared:linkReleaseFrameworkIosArm64` and an unsigned
`xcodebuild -configuration Release -sdk iphoneos` build both pass on the Mac mini. The Mac
has `iosApp/fastlane/.env` and `Configuration/Signing.xcconfig` copied from
FieldShootingTimer (same team, same ASC key) and two valid Apple Distribution identities.
App Store Connect has no app record and no bundle id for `se.kjellstrand.lsystemcamera`
yet, so `fastlane beta` stops at `get_provisioning_profile` until one is created
(`fastlane produce` or the ASC web UI). Over non-login ssh, fastlane needs
`PATH=/opt/homebrew/bin:$PATH`.
