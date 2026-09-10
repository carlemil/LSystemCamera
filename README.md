# L-system Camera

Draws the live camera image as a single continuous fractal curve. Dark parts of the
picture become thick line segments, bright parts thin ones, so the whole scene is
rendered with one unbroken space-filling line. Pick from 18 L-systems (Hilbert,
Moore, Gosper, Peano, Dragon, Sierpinski and more), adjust contrast, brightness or
complexity, hold the frame with the shutter and share the result as a PNG. Nothing
is stored, nothing is uploaded, no accounts, ads or analytics.

- Google Play: https://play.google.com/store/apps/details?id=se.kjellstrand.lsystemcamera
- Demo: https://youtube.com/shorts/hphq7vjqkKQ

## Layout

Kotlin Multiplatform + Compose Multiplatform.

| | |
|---|---|
| `shared/` | The L-system library, the renderer and the entire UI — everything but the platform entry points |
| `app/` | Android host: `MainActivity`, manifest, launcher icon, signing, Play publishing |
| `iosApp/` | iOS host: XcodeGen spec, SwiftUI wrapper, Info.plist, fastlane |

## Build

Android (PowerShell, repo root):

```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :shared:testDebugUnitTest
```

iOS (on a Mac):

```sh
cd iosApp
cp Configuration/Signing.xcconfig.template Configuration/Signing.xcconfig
xcodegen generate
open iosApp.xcodeproj
```

See [CLAUDE.md](CLAUDE.md) for the full build, release and architecture notes, and
[iosApp/README.md](iosApp/README.md) for the iOS host, the simulator photo-picker
fallback and the fastlane lanes.

## Privacy

No data is collected or transmitted — see [PRIVACY.md](PRIVACY.md).
