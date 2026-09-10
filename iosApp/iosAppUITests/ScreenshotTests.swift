import XCTest

// Drives the app headlessly and saves raw PNGs to /tmp/shots/ (simulator
// processes write straight to the host filesystem), so the render path can be
// checked on a Mac with no attached GUI session:
//
//   xcodebuild test -project iosApp.xcodeproj -scheme iosApp \
//     -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' \
//     -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO \
//     -only-testing:iosAppUITests
//
// The simulator has no camera, so the app presents a PHPicker at launch and
// renders the picked still. Put a photo in the library first:
//   xcrun simctl addmedia booted <some.jpg>
//
// Compose sets no test tags, so everything is addressed by accessibility label
// (Text content / contentDescription) with coordinate fallbacks.
final class ScreenshotTests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
        try FileManager.default.createDirectory(
            atPath: "/tmp/shots", withIntermediateDirectories: true)
    }

    func testPickerRenderScreenshots() throws {
        let app = XCUIApplication()
        app.launch()

        pickFirstPhoto(app)
        sleep(3)
        save("01_curve")

        // Segmented buttons pick which parameter the single slider edits.
        tap(app, "Contrast")
        dragSlider(app, to: 0.75)
        sleep(2)
        save("02_contrast")

        tap(app, "Complexity")
        dragSlider(app, to: 1.0)
        sleep(3)
        save("03_complexity")

        // Second tile in the icon strip. The strip is sorted by name and
        // scrolled to the selected system (Moore), so the neighbours vary —
        // address the label instead of a position.
        tap(app, "Peano")
        sleep(3)
        save("04_curve2")

        tap(app, "Hold frame")
        sleep(1)
        save("05_frozen")

        tap(app, "Share image")
        sleep(2)
        save("06_share")
        dismissShareSheet(app)
    }

    // MARK: - picker

    private func pickFirstPhoto(_ app: XCUIApplication) {
        // PHPicker runs in a remote view service but its grid does come through:
        // each tile is an Image with the identifier below, newest first.
        let tile = app.images.matching(identifier: "PXGGridLayout-Info").firstMatch
        if tile.waitForExistence(timeout: 15) {
            // The tile is not "hittable" (remote view service: hit point comes
            // back as {-1,-1}), so tap its centre by coordinate.
            tile.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            return
        }
        print("PICKER FALLBACK, element tree follows:\n\(app.debugDescription)")
        // First tile of the grid on an iPhone 17 Pro Max.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.165, dy: 0.405)).tap()
    }

    // MARK: - helpers

    private func tap(_ app: XCUIApplication, _ label: String) {
        let element = app.descendants(matching: .any)[label].firstMatch
        if element.waitForExistence(timeout: 5) {
            element.tap()
        } else {
            print("NOT ADDRESSABLE: \(label)")
        }
    }

    /// Compose exposes its Slider as a plain "Other" carrying a percentage
    /// value — `app.sliders` is empty — so match the value and drag the track.
    private func dragSlider(_ app: XCUIApplication, to position: CGFloat) {
        let track = app.otherElements.matching(NSPredicate(format: "value ENDSWITH '%'")).firstMatch
        guard track.waitForExistence(timeout: 5) else {
            print("NOT ADDRESSABLE: slider")
            return
        }
        track.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            .press(forDuration: 0.1,
                   thenDragTo: track.coordinate(withNormalizedOffset: CGVector(dx: position, dy: 0.5)))
    }

    private func dismissShareSheet(_ app: XCUIApplication) {
        let close = app.buttons["Close"].firstMatch
        if close.exists { close.tap() } else { app.swipeDown() }
    }

    private func save(_ name: String) {
        let png = XCUIScreen.main.screenshot().pngRepresentation
        try? png.write(to: URL(fileURLWithPath: "/tmp/shots/\(name).png"))
    }
}
