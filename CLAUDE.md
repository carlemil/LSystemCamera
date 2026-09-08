# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Android app (Kotlin, Jetpack Compose, single `app` module) that renders the live camera feed as a variable-width L-system curve: dark image regions become thick line segments, bright regions thin. Package `se.kjellstrand.lsystemcamera`, store name "L-system Camera". Requires a device or emulator with a camera (minSdk 28, targetSdk 37).

## Build and test

Gradle wrapper (Gradle 9.7.1, AGP 9.4.0 with built-in Kotlin, Compose BOM 2026.08.00, CameraX 1.6.2, compileSdk/targetSdk 37, the highest platform that exists). Versions live in `gradle/libs.versions.toml`. Run from repo root in PowerShell:

```
.\gradlew.bat :app:assembleDebug        # build APK
.\gradlew.bat :app:installDebug         # install on connected device/emulator
.\gradlew.bat :app:testDebugUnitTest    # JVM unit tests (app/src/test)
.\gradlew.bat :app:testDebugUnitTest --tests "se.kjellstrand.lsystem.LSystemGeneratorTest"
.\gradlew.bat :app:bundleRelease        # signed AAB, needs keystore.properties
```

There are no instrumented tests and no product flavors. The `se.kjellstrand.lsystem` package is pure Kotlin with no Android imports, so logic there is unit-testable on the JVM; keep it that way.

Kotlin is compiled by AGP's built-in support: there is no `kotlin-android` plugin and no `kotlin {}` block. Only the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) is applied, and the `kotlin` version in the catalog exists for that plugin alone. `lintDebug` is clean apart from version-available notices; keep it that way.

## Release

Signing and publishing follow the same shape as FieldShootingTimer, so the `/release` skill works here:

- `keystore.properties` at repo root (gitignored) with `storeFile` (absolute path, forward slashes), `storePassword`, `keyAlias=key0`, `keyPassword`. The upload key is the one enrolled in Play App Signing.
- `play-account.json` at repo root (gitignored): Play Console service-account key with Release manager role.
- `app/build.gradle.kts` holds `appVersionCode` / `appVersionName`; the Gradle Play Publisher plugin uploads to the **internal** track (`:app:publishReleaseBundle`). Production promotion is manual in Play Console.
- Release notes: `app/src/main/play/release-notes/en-GB/default.txt` (en-GB is the listing's default locale in Play Console; the app is English-only).
- Store listing graphics (512px icon, phone screenshots) live in `app/src/main/play/listings/en-GB/graphics/` and are uploaded by `:app:publishReleaseListing`, which is separate from the bundle upload. Screenshots are 1080x1920 emulator captures (`adb shell wm size 1080x1920` on a Pixel 9 AVD); the icon is the launcher foreground cropped to its safe zone over white.
- Privacy policy: `PRIVACY.md` at repo root; its GitHub URL is the one entered in Play Console.

## Architecture

Two packages with a deliberate boundary:

- `se.kjellstrand.lsystem` is the pure L-system library, vendored into the app (intended to be broken back out to a jar some day). Keep it free of Android imports.
  - `model/LSystem.kt` holds the catalog of systems (`LSystem.systems`), each with axiom, rules, angle, iteration range, and per-system width tuning constants (`lineWidthExp`, `lineWidthBold`). Adding a curve means adding an entry here plus an icon mapping in `MainScreen.kt` (`iconFor`).
  - `LSystemGenerator` turns an `LSystem` + iteration count into a normalized polyline of `LSTriple(x, y, w)` in 0..1 space, then maps luminance to `w`, smooths widths, and insets by a side buffer.
  - `VariableWidthCurve.kt` (`buildHullFromPolygon`) converts that width-annotated polyline into a closed outline hull (left side + reversed right side). It keeps private module-level scratch buffers reused across frames; not thread-safe by design, only ever called from the single analyzer thread.

- `se.kjellstrand.lsystemcamera` is the Android layer.
  - `MainActivity` (`ComponentActivity`) reads the camera permission in `onResume`, binds a CameraX `ImageAnalysis` use case alone (no preview) at roughly 200x200 with keep-latest backpressure, and handles Share (PNG to cache dir, `ShareCompat` + `FileProvider`).
  - `MainScreen.kt` is the whole UI: Material 3 `Scaffold` with no app bar (title and Share sit in a header row), square output `Image` in a rounded `Surface`, a horizontal `LazyRow` of icon tiles for picking the system (camel-case names are split for display), and a `SingleChoiceSegmentedButtonRow` (Contrast / Brightness / Complexity) that swaps a single `Slider` with its value shown. The selected segment is `rememberSaveable` local state, not part of `UiState`. Permission rationale with "Allow camera" and "Open settings" buttons replaces the image until granted. `Theme.kt` applies dynamic color on API 31+ and the M3 baseline below that.
  - `LSystemViewModel` owns `UiState` (system, iterations, contrast, brightness) in a `MutableStateFlow`, the output `frame: StateFlow<Bitmap?>`, the single-thread analyzer executor, and the `ImageAnalyzer` instance. Selecting a system resets iterations to `maxIterations - 1`.
  - `ImageAnalyzer` does the per-frame work on the analyzer thread: reads the Y plane into a center-cropped, vertically flipped luminance grid, maps it to line widths, builds the hull, draws it as a quadratic-bezier `Path` into one of two 1024x1024 bitmaps (double buffered so the UI never reads the one being drawn). The polyline is regenerated only when system or iterations change; everything else is per-frame width mapping.

Frame flow: camera -> `ImageAnalysis` -> `ImageAnalyzer.analyze` on the analyzer thread -> `frame` StateFlow -> Compose `Image`.

## Things to know

- Iteration counts near each system's `maxIterations` get expensive; the per-system max values were tuned by hand.
- There is no orientation lock (Android 17 ignores them on large screens anyway). `ImageAnalyzer.readLuminance` rotates the crop per frame using `imageInfo.rotationDegrees`, and the layout is width-capped and centered so landscape and tablets work. The activity is recreated on rotation, which rebinds `ImageAnalysis` with the new target rotation; the ViewModel keeps the executor and renderer alive across that.
- Picker icons live in `res/drawable-nodpi` (they are drawn at a fixed dp size). Pentaplexity and QuadraticGosper have no icon and show `unknown_system` in the tile row. The adaptive launcher icon reuses the foreground PNG as its monochrome layer.
- `KochSnowFlake` exists in the catalog but is filtered out of the picker.
