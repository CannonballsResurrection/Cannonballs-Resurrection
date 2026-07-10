import AppKit
import SceneKit

final class AppDelegate: NSObject, NSApplicationDelegate, NSMenuItemValidation {
    var window: NSWindow!
    var controller: GameViewController!

    func applicationDidFinishLaunching(_ notification: Notification) {
        buildMenuBar()
        // locked to the original 800x600 (the game's native resolution)
        let rect = NSRect(x: 0, y: 0, width: 800, height: 600)
        window = NSWindow(contentRect: rect,
                          styleMask: [.titled, .closable, .miniaturizable],
                          backing: .buffered, defer: false)
        window.title = "Cannonballs!"
        window.contentMinSize = NSSize(width: 800, height: 600)
        window.contentMaxSize = NSSize(width: 800, height: 600)
        window.center()
        controller = GameViewController()
        window.contentViewController = controller
        window.makeKeyAndOrderFront(nil)
        window.makeFirstResponder(controller.scnView)
        NSApp.activate(ignoringOtherApps: true)
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }

    // MARK: - Menu bar

    private func buildMenuBar() {
        let main = NSMenu()

        // App menu
        let appItem = NSMenuItem()
        main.addItem(appItem)
        let appMenu = NSMenu()
        appItem.submenu = appMenu
        appMenu.addItem(withTitle: "About Cannonballs!", action: #selector(showAbout), keyEquivalent: "")
        appMenu.addItem(.separator())
        appMenu.addItem(withTitle: "Quit Cannonballs!", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")

        // Game menu — clickable actions (key hints shown in titles; no key equivalents so
        // they never steal input from the 3D view during play)
        let gameItem = NSMenuItem(); main.addItem(gameItem)
        let gameMenu = NSMenu(title: "Game"); gameItem.submenu = gameMenu
        gameMenu.title = "Game"
        addAction(gameMenu, "Fire  (Space)", #selector(mFire))
        addAction(gameMenu, "Next Weapon  (+)", #selector(mNextWeapon))
        addAction(gameMenu, "Previous Weapon  (–)", #selector(mPrevWeapon))
        gameMenu.addItem(.separator())
        addAction(gameMenu, "Forfeit Round  (Q)", #selector(mForfeit))
        addAction(gameMenu, "Back to Menu  (Esc)", #selector(mBackToMenu))

        // Camera menu — clickable mode selection (matches the original game)
        let camItem = NSMenuItem(); main.addItem(camItem)
        let camMenu = NSMenu(title: "Camera"); camItem.submenu = camMenu
        camMenu.title = "Camera"
        for mode in CameraController.Mode.allCases {
            let it = NSMenuItem(title: "\(mode.label) Camera", action: #selector(mCamera(_:)), keyEquivalent: "")
            it.tag = mode.rawValue
            it.target = self
            camMenu.addItem(it)
        }
        camMenu.addItem(.separator())
        addAction(camMenu, "Next Camera  (V)", #selector(mNextCamera))

        // Help menu — the controls reference
        let helpItem = NSMenuItem(); main.addItem(helpItem)
        let helpMenu = NSMenu(title: "Help"); helpItem.submenu = helpMenu
        helpMenu.title = "Help"
        helpMenu.addItem(withTitle: "Controls…", action: #selector(showControls), keyEquivalent: "/").target = self
        NSApp.mainMenu = main
    }

    private func addAction(_ menu: NSMenu, _ title: String, _ sel: Selector) {
        let it = NSMenuItem(title: title, action: sel, keyEquivalent: "")
        it.target = self
        menu.addItem(it)
    }

    private var game: GameController? { controller?.game }

    // Enable game actions only while a game is running; check-mark the active camera.
    func validateMenuItem(_ item: NSMenuItem) -> Bool {
        switch item.action {
        case #selector(mCamera(_:)):
            item.state = (game?.camera.mode.rawValue == item.tag) ? .on : .off
            return game != nil
        case #selector(mFire), #selector(mNextWeapon), #selector(mPrevWeapon),
             #selector(mForfeit), #selector(mBackToMenu), #selector(mNextCamera):
            return game != nil
        default:
            return true
        }
    }

    // MARK: - Menu actions

    // Menu clicks arrive on the main thread; route game mutations to the render thread.
    @objc private func mFire() { controller?.runGameCommand { $0.menuFire() }; refocus() }
    @objc private func mNextWeapon() { controller?.runGameCommand { $0.menuNextWeapon() }; refocus() }
    @objc private func mPrevWeapon() { controller?.runGameCommand { $0.menuPrevWeapon() }; refocus() }
    @objc private func mForfeit() { controller?.runGameCommand { $0.menuForfeit() }; refocus() }
    @objc private func mBackToMenu() { controller?.runGameCommand { $0.menuBackToMenu() }; refocus() }
    @objc private func mNextCamera() { controller?.runGameCommand { $0.camera.cycle() }; refocus() }
    @objc private func mCamera(_ sender: NSMenuItem) {
        if let mode = CameraController.Mode(rawValue: sender.tag) {
            controller?.runGameCommand { $0.menuSetCamera(mode) }
        }
        refocus()
    }

    /// Return keyboard focus to the game view after a menu click.
    private func refocus() { window?.makeFirstResponder(controller?.scnView) }

    @objc private func showAbout() {
        let a = NSAlert()
        a.messageText = "Cannonballs"
        a.informativeText = "A native macOS clone of the 2002 WildTangent pirate artillery game, rebuilt from the original assets."
        a.addButton(withTitle: "OK")
        a.runModal()
    }

    @objc private func showControls() {
        let a = NSAlert()
        a.messageText = "Cannonballs — Controls"
        a.informativeText = """
        AIM
          ← →   Turn the cannon left / right
          ↑ ↓   Raise / lower the barrel

        FIRE
          Space   Charge the power bar, press again to fire

        WEAPONS
          +   Next weapon      –   Previous weapon
          (or click a weapon in the on-screen list)

        CAMERA
          V   Cycle camera  (Cannon · Shot · Medium · High · Barrel)
          (or pick one from the Camera menu)

        ROUND
          Q   Forfeit the round
          Esc   Back to the main menu
        """
        a.addButton(withTitle: "OK")
        a.runModal()
        refocus()
    }
}
