# iOS port of L-system Camera

Source of truth for the port. Orchestrator workflow: one task = one reviewed commit;
the Android gate must stay green after every task:

```
.\gradlew.bat :shared:testDebugUnitTest :app:assembleDebug --rerun-tasks
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

- [ ] **1. Restructure** to `shared/` (KMP library, androidTarget + jvm + iosArm64 +
  iosSimulatorArm64, static `Shared` framework) + `app/` (Android entry, signing,
  Play). Move both packages to `commonMain` where they compile, `androidMain`
  otherwise; strings + 19 icons to composeResources; tests to `jvmTest`
  (`CurveTuningTest`/`CurvePreviewTest` use `java.awt`, stay JVM). Android
  behaviour identical. `compileKotlinIosSimulatorArm64` may still fail (that is task 2).
- [ ] **2. Platform seams**: `LumaFrame`, Compose-graphics renderer, coroutine
  analyzer, `rememberCameraSource`/`rememberCameraPermission`/`shareImage`/
  `dynamicColorSchemeOrNull` expect + Android actuals. Gate:
  `:shared:compileKotlinIosSimulatorArm64` passes with stub iOS actuals (`TODO()`).
- [ ] **3. iOS actuals**: AVFoundation camera source, picker fallback, permission,
  share, dynamic colour, `MainViewController()`.
- [ ] **4. iosApp/**: XcodeGen spec, Swift wrapper, Info.plist with
  `NSCameraUsageDescription`, PrivacyInfo, icon, fastlane, CI job. Build and run in
  the simulator on the Mac mini; measure `analyze()` time with the picker source.
- [ ] **5. Docs**: CLAUDE.md module table + Mac steps, README, memory note.

## Follow-ups / queue

(none yet)

## Status

Task 1 in progress.
