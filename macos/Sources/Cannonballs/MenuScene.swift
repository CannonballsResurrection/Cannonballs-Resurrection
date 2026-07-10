import SpriteKit
import AppKit

/// Main menu, rebuilt as a faithful clone of the original screens.
/// Everything is laid out in the original's 800x600 screen space (coordinates
/// extracted from the game's UI layout) and the scene scale-fills the window,
/// so the art stays chunky and proportional exactly like 2002.
///
/// Screens:
///  - Main: MAIN backdrop (logo art) + centered stack of purple text buttons.
///  - New Game Settings: centered 512x256 popup with map thumb + option rows.
final class MenuScene: SKScene {
    var onStart: ((GameOptions) -> Void)?

    // Source-space helpers: original coords are top-left origin, SpriteKit is bottom-left.
    private func P(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x, y: 600 - y) }

    /// Tells the host view to show/tint (Int = color index) or remove (nil) the
    /// slow-spinning 3D cannon preview behind the lobby screen.
    var onCannonPreview: ((Int?) -> Void)?

    // MARK: - State
    private enum Screen { case main, nameEntry, settings, lobby }
    private var screen: Screen = .main
    private var playerName = UserDefaults.standard.string(forKey: "playerName") ?? ""
    private var nameField = SKSpriteNode()
    private var selectedMap = 0
    private var playerCount = 4         // "Maximum # of Players" (You + bots)
    private var lives = 2
    private var goldIdx = 4
    private var hotseatIdx = 0
    private var treasure = true

    // lobby state
    private var colorIndex = UserDefaults.standard.integer(forKey: "cannonColor")
    private var colorOpen = false
    private static let colorNames = ["Blue", "Purple", "Red", "Green"]

    /// The original color square (Button_3DDropColor.createColor): a black 32x32
    /// bitmap with the color filled at (2,2)-(20,14), shown cropped to 23x17 —
    /// i.e. the color sits inside a small black border.
    static func colorSwatch(_ colorIndex: Int) -> NSImage {
        let img = NSImage(size: NSSize(width: 23, height: 17))
        img.lockFocus()
        NSColor.black.setFill()
        NSRect(x: 0, y: 0, width: 23, height: 17).fill()
        NSColor(rgb: G.colorRGB[colorIndex % 4]).setFill()
        NSRect(x: 2, y: 17 - 14, width: 18, height: 12).fill()
        img.unlockFocus()
        return img
    }
    // One AI type per slot (slot 0 = You). 0=none, 1=Dummy, 2=Aggressive, 3=Thinker, 4=Crazy —
    // exactly the original's per-slot "Add AI Player" dropdown options.
    private static let aiOptions = ["none", "Dummy", "Aggressive", "Thinker", "Crazy"]
    private var slotTypes: [Int] = []
    private var slotNames: [Int: String] = [:]   // roster name rolled at assignment (Network.java:204 random pick)
    private var openSlot: Int? = nil

    private let root = SKNode()
    private let modal = SKNode()

    // Hoverable buttons: name -> (sprite, normalImg, hoverImg)
    private var hoverButtons: [String: (SKSpriteNode, NSImage, NSImage)] = [:]
    private var hoverArrows: [String: SKSpriteNode] = [:]

    private var mapThumb = SKSpriteNode()
    private var valueSprites: [String: SKSpriteNode] = [:]

    override init(size: CGSize) {
        super.init(size: CGSize(width: 800, height: 600))
        scaleMode = .fill
        backgroundColor = .clear   // transparent so cutouts reveal the SCNView behind
        addChild(root)
        buildMain()
        modal.isHidden = true
        modal.zPosition = 50
        addChild(modal)
    }
    required init?(coder: NSCoder) { fatalError() }

    /// The window scales the fixed 800x600 scene; nothing to relayout.
    func relayout(size: CGSize) {}

    // MARK: - helpers

    private func sprite(_ img: NSImage, _ pos: CGPoint, z: CGFloat = 1) -> SKSpriteNode {
        let n = SKSpriteNode(texture: SKTexture(image: img))
        n.texture?.filteringMode = .nearest
        n.size = img.size
        n.position = pos
        n.zPosition = z
        return n
    }

    private func text(_ s: String, cap: CGFloat, tint: NSColor? = nil) -> SKSpriteNode {
        let img = HUDArt.text(s, capHeight: cap, tint: tint)
        let n = SKSpriteNode(texture: SKTexture(image: img))
        n.size = img.size
        return n
    }

    /// Original-style 252x28 text button with hover state.
    @discardableResult
    private func addTextButton(_ label: String, at src: CGPoint, name: String,
                               parent: SKNode, dimmed: Bool = false, z: CGFloat = 2) -> SKSpriteNode {
        let normal = HUDArt.menuButton(hover: false)
        let hover = HUDArt.menuButton(hover: true)
        let b = sprite(normal, P(src.x, src.y), z: z)
        b.size = CGSize(width: 256, height: 32)   // Button_3D plane 0.700416x0.087552 NDC
        b.name = name
        if dimmed { b.alpha = 0.55 }
        let l = text(label, cap: 24)              // Button_3D label = Message_3D f=1.0
        l.name = name
        l.zPosition = 0.1
        // Button_3D.show: centered label at (TopLeftX+128, TopLeftY+16) = center +2,+2
        l.position = CGPoint(x: 2, y: -2)
        b.addChild(l)
        parent.addChild(b)
        hoverButtons[name] = (b, normal, hover)
        return b
    }

    /// 26x27 controls-sheet arrow button with hover state.
    private func addArrow(_ s: HUDArt.ControlSprite, at src: CGPoint, name: String, parent: SKNode) {
        let b = sprite(HUDArt.control(s), CGPoint(x: P(src.x, src.y).x + 13, y: P(src.x, src.y).y - 13), z: 3)
        b.name = name
        parent.addChild(b)
        hoverArrows[name] = b
    }

    // MARK: - Main screen

    private func buildMain() {
        // Backdrop: the decoded MAIN .wjp art is natively 800x600 — 1:1, pixel exact.
        if let bg = Assets.image("MENUS/MAIN.png") {
            let n = sprite(bg, CGPoint(x: 400, y: 300), z: -2)
            n.size = CGSize(width: 800, height: 600)
            root.addChild(n)
        }

        // Top magenta menu bar is baked into the MAIN.png backdrop art (~26px tall);
        // drawing topbar.png over it left the baked bar peeking out below as a
        // "duplicate". Only the menu text is ours, on the art's own bar.
        let quit = text("Quit", cap: 24); quit.name = "quit"
        quit.position = CGPoint(x: 10 + quit.size.width / 2, y: 600 - 14); quit.zPosition = 6
        root.addChild(quit)
        let title = text("Cannonballs! v1.869", cap: 18)   // ref frame_001   // Main.VersionNumber; f=0.75
        title.position = CGPoint(x: 400, y: 600 - 14); title.zPosition = 6
        root.addChild(title)

        // Original button stack (centered x=400). Online-only entries are shown for
        // authenticity but dimmed — the 2002 services are gone.
        addTextButton("Guest Login", at: CGPoint(x: 400, y: 380), name: "dead-guest", parent: root, dimmed: true)
        addTextButton("Single Player", at: CGPoint(x: 400, y: 420), name: "single", parent: root)
        addTextButton("View Leaderboards", at: CGPoint(x: 400, y: 460), name: "dead-stats", parent: root, dimmed: true)
        addTextButton("Register Now!", at: CGPoint(x: 400, y: 520), name: "dead-register", parent: root, dimmed: true)
    }

    // MARK: - "Your Name" popup (shown before New Game Settings)

    private func buildNameEntry() {
        modal.removeAllChildren()
        hoverButtons = hoverButtons.filter { !$0.key.hasPrefix("m-") }

        let veil = SKSpriteNode(color: NSColor(calibratedWhite: 0, alpha: 0.55), size: CGSize(width: 820, height: 620))
        veil.position = CGPoint(x: 400, y: 300)
        veil.zPosition = -1
        modal.addChild(veil)

        let panel = sprite(HUDArt.popupPanel(), CGPoint(x: 400, y: 300), z: 0)
        modal.addChild(panel)
        let title = text("Your Name", cap: 24)   // PopUp title f=1.0 at (154,188)
        title.position = CGPoint(x: 154 + title.size.width / 2, y: 600 - 188)
        title.zPosition = 1
        modal.addChild(title)

        let label = text("Name:", cap: 24)
        label.position = CGPoint(x: 232, y: 600 - 290)
        label.zPosition = 1
        modal.addChild(label)
        let field = sprite(HUDArt.textBar(width: 320, height: 26), CGPoint(x: 440, y: 600 - 290), z: 1)
        modal.addChild(field)
        nameField = SKSpriteNode()
        nameField.anchorPoint = CGPoint(x: 0, y: 0.5)
        nameField.position = CGPoint(x: 290, y: 600 - 290)
        nameField.zPosition = 2
        modal.addChild(nameField)
        refreshNameField()

        addTextButton("Cancel", at: CGPoint(x: 273, y: 412), name: "m-name-cancel", parent: modal, z: 2)
        addTextButton("Enter", at: CGPoint(x: 527, y: 412), name: "m-name-enter", parent: modal, z: 2)
    }

    private func refreshNameField() {
        let shown = playerName.isEmpty ? "_" : playerName + "_"
        let img = HUDArt.text(shown, capHeight: 24)
        nameField.texture = SKTexture(image: img)
        nameField.size = img.size
    }

    private func confirmName() {
        let trimmed = playerName.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        playerName = trimmed
        UserDefaults.standard.set(playerName, forKey: "playerName")
        // Original flow: dissolve out -> loading hourglass -> dissolve in
        // (Menu_Manager cases 102/100/101 + the skull hourglass @0.4s/frame).
        irisTransition {
            self.screen = .settings
            self.buildSettings()
        }
    }

    /// The original telescoping screen transition (Menu_Manager.java:246-292):
    /// the iris CLOSES over the old screen (TRANSITION sheet scale 1.0 -> 0.001
    /// over 1s), the hourglass spins while the screen switches under black,
    /// then the iris OPENS onto the new screen. The TRANSITION actor is a huge
    /// black sheet with a soft-rimmed circular hole (see
    /// IrisTransition.sheetImage()); four black arm sprites stand in for the
    /// mesh's giant quad around the punched 512px center.
    private func irisTransition(_ swap: @escaping () -> Void) {
        guard let sheetImg = IrisTransition.sheetImage() else { swap(); return }
        let iris = SKNode()
        iris.position = CGPoint(x: 400, y: 300)
        iris.zPosition = 500
        let hole = SKSpriteNode(texture: SKTexture(image: HUDArt.compensated(sheetImg)))
        hole.size = CGSize(width: 512, height: 512)
        iris.addChild(hole)
        for r in IrisTransition.armRects() {
            let arm = SKSpriteNode(color: .black, size: CGSize(width: r.width, height: r.height))
            arm.position = CGPoint(x: r.midX, y: r.midY)
            iris.addChild(arm)
        }
        let open = IrisTransition.openScale(for: size)
        iris.setScale(open)   // scale 1.0 = iris fully open (hole past the corners)
        addChild(iris)
        let hourglassFrames: [SKTexture] = (0..<6).map { i in
            SKTexture(image: HUDArt.crop("hourglass.png", NSRect(x: (i % 4) * 64, y: (i / 4) * 128, width: 64, height: 128)))
        }
        iris.run(.sequence([
            .scale(to: 0.002, duration: 1.0),   // iris closes (case 102)
            .run { [weak self] in
                guard let self else { return }
                swap()
                let hg = SKSpriteNode(texture: hourglassFrames[0])
                hg.position = CGPoint(x: 400, y: 300)
                hg.zPosition = 600
                hg.size = CGSize(width: 64, height: 128)
                self.addChild(hg)
                var f = 0
                hg.run(.sequence([
                    .repeat(.sequence([.run { f = (f + 1) % 6; hg.texture = hourglassFrames[f] },
                                       .wait(forDuration: 0.4)]), count: 3),
                    .removeFromParent()
                ]))
            },
            .wait(forDuration: 1.2),
            .scale(to: open, duration: 1.0),    // iris opens (case 101)
            .removeFromParent()
        ]))
    }

    // MARK: - Lobby screen (Joining Players + color picker + spinning cannon)

    private func openLobby() {
        screen = .lobby
        root.isHidden = true      // full-screen lobby replaces the main screen entirely
        colorOpen = false
        openSlot = nil
        // Slots: index 0 = You; 1..playerCount-1 = AI slots. Pre-fill with a mix of
        // difficulties (dropdowns let you change or set to "none").
        let defaults = [3, 2, 4, 1, 3, 2, 4]   // Thinker/Aggressive/Crazy/Dummy...
        slotTypes = (0..<max(1, playerCount)).map { $0 == 0 ? -1 : 0 }   // empty until AI assigned
        slotNames = [:]
        buildLobby()
        modal.isHidden = false
        onCannonPreview?(colorIndex)
    }

    private func buildLobby() {
        modal.removeAllChildren()
        hoverButtons = hoverButtons.filter { !$0.key.hasPrefix("l-") && !$0.key.hasPrefix("m-") }

        // Full-screen JOIN backdrop with the right-hand scenery window punched
        // transparent so the 3D spinning cannon preview (in the SCNView behind,
        // whose scene background is this same art) shows through. The original
        // renders the cannon camera-space over the whole screen, so the punch
        // must cover the entire window frame or the cannon clips: art frame is
        // x 406..800, y 29..369 (source top-left coords).
        if let bg = Assets.image("MENUS/JOIN.png") {
            let img = NSImage(size: NSSize(width: 800, height: 600))
            img.lockFocus()
            bg.draw(in: NSRect(x: 0, y: 0, width: 800, height: 600))
            if let cg = NSGraphicsContext.current?.cgContext {
                cg.setBlendMode(.destinationOut)
                cg.setFillColor(CGColor(gray: 0, alpha: 1))
                cg.fill(CGRect(x: 406, y: 600 - 369, width: 394, height: 340))
                cg.setBlendMode(.normal)
            }
            img.unlockFocus()
            let n = sprite(img, CGPoint(x: 400, y: 300), z: -1)
            n.size = CGSize(width: 800, height: 600)
            modal.addChild(n)
        }

        // Top bar: the lobby re-shows the version centered (showCreateJoinMenu:
        // Message_3D(VersionNumber, 1, 1.0).show(400, 16)).
        let ver = text("Cannonballs! v1.869", cap: 24)
        ver.position = CGPoint(x: 400, y: 600 - 16)
        ver.zPosition = 1
        modal.addChild(ver)

        // ---- Joining Players panel (left) ----
        // Source layout (Menu_Lobby_Screen / Button_3D semantics — Message_3D.show(x,y)
        // is LEFT edge + vertical CENTERLINE): rows share the centerline y=112+i*32;
        // names (f=0.75) at x=10; IconCheck 24px top-left (330,100+i*32); Kick is the
        // Controls-sheet LINE button, 45x26 top-left (355,100+i*32); empty slots carry
        // a 256x32 Button_3DDrop centered at x=200 — rows TOUCH (pitch = art height).
        // The JOIN background art already CONTAINS the lobby windows (frame_002);
        // drawing our own panel doubled it. Only the title text is ours.
        let title = text("Joining Players", cap: 24)   // TitleRoom f=1.0 at (14,82)
        title.position = CGPoint(x: 14 + title.size.width / 2, y: 600 - 82)
        title.zPosition = 1
        modal.addChild(title)

        var botNameIdx = 0
        for slot in 0..<slotTypes.count {
            let cy: CGFloat = 112 + CGFloat(slot) * 32         // row centerline (source y)
            func check() {
                let ck = sprite(HUDArt.image("iconcheck.png"), P(342, cy), z: 2)
                ck.size = CGSize(width: 24, height: 24)
                modal.addChild(ck)
            }
            if slot == 0 {
                let l = text(playerName.isEmpty ? "You" : playerName, cap: 18)
                l.position = CGPoint(x: 10 + l.size.width / 2, y: 600 - cy); l.zPosition = 1
                modal.addChild(l)
                check()                                        // host is checked in
            } else if slotTypes[slot] > 0 {
                // assigned bot: blue roster name + check + Kick (the LINE button)
                let bn = slotNames[slot] ?? G.botNames[botNameIdx % G.botNames.count]
                botNameIdx += 1
                let l = text(bn, cap: 18, tint: nil)
                let img = HUDArt.text(bn, capHeight: 18, color: .blue)
                l.texture = SKTexture(image: img); l.size = img.size
                l.position = CGPoint(x: 10 + l.size.width / 2, y: 600 - cy); l.zPosition = 1
                modal.addChild(l)
                check()
                // original Kick art: controls sheet (57,154) normal / (57,128) hover
                let kickNormal = HUDArt.crop("controls.png", NSRect(x: 57, y: 154, width: 45, height: 26))
                let kickHover = HUDArt.crop("controls.png", NSRect(x: 57, y: 128, width: 45, height: 26))
                let kb = sprite(kickNormal, P(377.5, cy + 1), z: 2)
                kb.size = CGSize(width: 45, height: 26)
                kb.name = "l-kick-\(slot)"
                modal.addChild(kb)
                hoverButtons[kb.name!] = (kb, kickNormal, kickHover)
            } else {
                // empty slot: 'Add AI Player' dropdown (Button_3DDrop at x=200,
                // label f=1.0 left-aligned at TopLeftX+10 = center-116, +2 down).
                let open = openSlot == slot
                let normal = HUDArt.dropRow(hover: false)
                let hovered = HUDArt.dropRow(hover: true)
                let btn = sprite(open ? hovered : normal, P(200, cy), z: 2)
                btn.size = CGSize(width: 256, height: 32)
                btn.name = "l-slot-\(slot)"
                modal.addChild(btn)
                // an open dropdown stays red until it rerolls (original setState never resets)
                hoverButtons[btn.name!] = open ? (btn, hovered, hovered) : (btn, normal, hovered)
                let lbl = text("Add AI Player", cap: 24)
                lbl.position = CGPoint(x: -116 + lbl.size.width / 2, y: -2); lbl.zPosition = 0.1
                lbl.name = btn.name
                btn.addChild(lbl)
            }
        }
        // Expanded AI-type menu for the open slot (drawn last so it overlays).
        // Original rollout: rows on the arrow-less rollout art, pitch 32, first row
        // at button center +31; item text f=1.0 at TopLeftX+10, centerline +34+32n;
        // the hovered row shows the red button-art overlay (Buttons bottom half).
        if let s = openSlot, s < slotTypes.count {
            let cy: CGFloat = 112 + CGFloat(s) * 32
            let rowNormal = HUDArt.rolloutRow()
            let rowHover = HUDArt.crop("buttons_button.png", NSRect(x: 0, y: 32, width: 256, height: 30))
            for (opt, name) in MenuScene.aiOptions.enumerated() {
                let row = sprite(rowNormal, P(200, cy + 31 + CGFloat(opt) * 32), z: 6)
                row.size = CGSize(width: 256, height: 32)
                row.name = "l-settype-\(s)-\(opt)"
                modal.addChild(row)
                hoverButtons[row.name!] = (row, rowNormal, rowHover)
                let l = text(name, cap: 24)
                l.position = CGPoint(x: -116 + l.size.width / 2, y: -3); l.zPosition = 0.1
                l.name = row.name
                row.addChild(l)
            }
        }

        // ---- right column ----
        addTextButton("Abandon This Game", at: CGPoint(x: 610, y: 70), name: "l-abandon", parent: modal, z: 2)

        // Color dropdown (Button_3DDropColor at 610,320): full 256x32 dropdown art,
        // black-bordered color swatch at center-108, "Color" label left-aligned at
        // TopLeftX+50 = center-76. The rollout rows carry ONLY swatches (no labels).
        let dropNormal = HUDArt.dropRow(hover: false)
        let dropHover = HUDArt.dropRow(hover: true)
        let colBtn = sprite(colorOpen ? dropHover : dropNormal, P(610, 320), z: 2)
        colBtn.size = CGSize(width: 256, height: 32)
        colBtn.name = "l-color"
        modal.addChild(colBtn)
        hoverButtons["l-color"] = colorOpen ? (colBtn, dropHover, dropHover) : (colBtn, dropNormal, dropHover)
        let swatch = sprite(MenuScene.colorSwatch(colorIndex % 4), CGPoint(x: -108, y: 0), z: 0.1)
        swatch.name = "l-color"
        colBtn.addChild(swatch)
        let colLabel = text("Color", cap: 24)
        colLabel.position = CGPoint(x: -76 + colLabel.size.width / 2, y: -2)
        colLabel.zPosition = 0.1
        colLabel.name = "l-color"
        colBtn.addChild(colLabel)

        if colorOpen {
            let rowNormal = HUDArt.rolloutRow()
            let rowHover = HUDArt.crop("buttons_button.png", NSRect(x: 0, y: 32, width: 256, height: 30))
            for i in 0..<4 {
                let row = sprite(rowNormal, P(610, 320 + 31 + CGFloat(i) * 32), z: 5)
                row.size = CGSize(width: 256, height: 32)
                row.name = "l-pick-\(i)"
                modal.addChild(row)
                hoverButtons[row.name!] = (row, rowNormal, rowHover)
                let sw = sprite(MenuScene.colorSwatch(i), CGPoint(x: -108, y: -4), z: 0.1)
                sw.name = row.name
                row.addChild(sw)
            }
        }

        if !colorOpen {
            addTextButton("Begin The Game!", at: CGPoint(x: 610, y: 352), name: "l-begin", parent: modal, z: 2)
        }
    }

    private func handleLobbyClick(_ name: String) {
        switch name {
        case "l-abandon":
            onCannonPreview?(nil)
            closeModal()
        case "l-begin":
            onCannonPreview?(nil)
            startGame()
        case "l-color":
            colorOpen.toggle(); openSlot = nil
            buildLobby()
        case let n where n.hasPrefix("l-pick-"):
            if let i = Int(n.dropFirst(7)) {
                colorIndex = i
                UserDefaults.standard.set(i, forKey: "cannonColor")
                onCannonPreview?(i)
            }
            colorOpen = false
            buildLobby()
        case let n where n.hasPrefix("l-slot-"):
            if let s = Int(n.dropFirst(7)) {
                openSlot = (openSlot == s) ? nil : s   // toggle this slot's AI dropdown
                colorOpen = false
            }
            buildLobby()
        case let n where n.hasPrefix("l-settype-"):
            // roll a random unused roster name for the new bot (Network.java:204)
            if let parts = Optional(n.split(separator: "-")), parts.count >= 4,
               let slot = Int(parts[2]), let opt = Int(parts[3]), opt > 0, slotNames[slot] == nil {
                let used = Set(slotNames.values)
                let free = G.botNames.filter { !used.contains($0) }
                slotNames[slot] = free.randomElement() ?? G.botNames.randomElement()!
            }
            let parts = n.dropFirst(10).split(separator: "-")
            if parts.count == 2, let s = Int(parts[0]), let t = Int(parts[1]), slotTypes.indices.contains(s) {
                slotTypes[s] = t          // 0=none removes the AI; 1..4 set the difficulty
            }
            openSlot = nil
            buildLobby()
        case let n where n.hasPrefix("l-kick-"):
            if let slot = Int(n.split(separator: "-").last ?? "") { slotNames[slot] = nil }
            if let s = Int(n.dropFirst(7)), slotTypes.indices.contains(s) { slotTypes[s] = 0 }
            openSlot = nil
            buildLobby()
        default: break
        }
    }

    // MARK: - New Game Settings modal (popup centered at 400,300 in source space)

    private func buildSettings() {
        modal.removeAllChildren()
        hoverButtons = hoverButtons.filter { !$0.key.hasPrefix("m-") }

        // dim veil over the main screen (like the original's modal state)
        let veil = SKSpriteNode(color: NSColor(calibratedWhite: 0, alpha: 0.55), size: CGSize(width: 820, height: 620))
        veil.position = CGPoint(x: 400, y: 300)
        veil.zPosition = -1
        modal.addChild(veil)

        // 512x256 panel from the original popup halves, centered (400,300).
        let panel = sprite(HUDArt.popupPanel(), CGPoint(x: 400, y: 300), z: 0)
        modal.addChild(panel)

        // Title in the magenta header (left-aligned like the original).
        let title = text("New Game Settings", cap: 24)
        title.position = CGPoint(x: 152 + title.size.width / 2, y: 600 - 185)
        title.zPosition = 1
        modal.addChild(title)

        // Map thumbnail between its cycle arrows (arrows at src 148,287 / 278,287).
        addArrow(.arrowLeft, at: CGPoint(x: 148, y: 287), name: "m-mapdown", parent: modal)
        addArrow(.arrowRight, at: CGPoint(x: 278, y: 287), name: "m-mapup", parent: modal)
        mapThumb = SKSpriteNode()
        mapThumb.size = CGSize(width: 96, height: 88)
        mapThumb.position = CGPoint(x: 226, y: 600 - 290)
        mapThumb.zPosition = 1
        modal.addChild(mapThumb)

        // Option rows: label right-aligned to x=530, arrows at 540/620, value bar at 568..638.
        let rows: [(String, String)] = [
            ("players", "Maximum # of Players"),
            ("lives", "Starting # of Lives"),
            ("gold", "Starting Gold"),
            ("seat", "HotSeat Mode"),
            ("treasure", "Treasures Respawn"),
            ("team", "Team Play")
        ]
        for (i, row) in rows.enumerated() {
            let ySrc = CGFloat(203 + i * 30)
            let label = text(row.1, cap: 18)
            label.position = CGPoint(x: 530 - label.size.width / 2, y: 600 - (ySrc + 13.5))
            label.zPosition = 1
            modal.addChild(label)
            addArrow(.arrowLeft, at: CGPoint(x: 540, y: ySrc), name: "m-\(row.0)-down", parent: modal)
            addArrow(.arrowRight, at: CGPoint(x: 620, y: ySrc), name: "m-\(row.0)-up", parent: modal)
            let bar = sprite(HUDArt.textBar(width: 56, height: 24), CGPoint(x: 593, y: 600 - (ySrc + 13.5)), z: 1)
            modal.addChild(bar)
            let v = SKSpriteNode()
            v.position = bar.position
            v.zPosition = 2
            valueSprites[row.0] = v
            modal.addChild(v)
        }

        addTextButton("Cancel", at: CGPoint(x: 273, y: 412), name: "m-cancel", parent: modal, z: 2)
        addTextButton("Create", at: CGPoint(x: 527, y: 412), name: "m-create", parent: modal, z: 2)
        refreshSettings()
    }

    private func refreshSettings() {
        let map = MapCatalog.maps[selectedMap]
        // Original thumbnails carry their stylized map name baked in — no extra label.
        if let img = Assets.image(map.thumbPath) {
            mapThumb.texture = SKTexture(image: img)
            mapThumb.texture?.filteringMode = .nearest
        }

        func setValue(_ key: String, _ s: String, dim: Bool = false) {
            guard let n = valueSprites[key] else { return }
            let img = HUDArt.text(s, capHeight: 18, tracking: 1, color: dim ? .gray : .white)
            n.texture = SKTexture(image: img)
            n.size = img.size
        }
        setValue("players", "\(playerCount)")
        setValue("lives", "\(lives)")
        setValue("gold", "\(G.startingCashTable[goldIdx])")
        setValue("seat", hotseatIdx == 0 ? "NA" : "\(G.hotSeatTimes[hotseatIdx])")
        setValue("treasure", treasure ? "yes" : "no")
        setValue("team", "NA", dim: true)
    }

    // MARK: - Interaction

    func keyDown(_ e: NSEvent) -> Bool {
        switch screen {
        case .nameEntry:
            if e.keyCode == 36 { confirmName(); return true }          // return
            if e.keyCode == 53 { closeModal(); return true }           // esc
            if e.keyCode == 51 {                                       // backspace
                if !playerName.isEmpty { playerName.removeLast() }
                refreshNameField()
                return true
            }
            if let chars = e.characters, playerName.count < 12 {
                let ok = chars.filter { $0.isLetter || $0.isNumber }
                if !ok.isEmpty {
                    playerName += ok
                    refreshNameField()
                    return true
                }
            }
            return false
        case .settings:
            if e.keyCode == 36 { openLobby(); return true }
            if e.keyCode == 53 { closeModal(); return true }
            return false
        case .lobby:
            if e.keyCode == 36 { onCannonPreview?(nil); startGame(); return true }
            if e.keyCode == 53 { onCannonPreview?(nil); closeModal(); return true }
            return false
        case .main:
            if e.keyCode == 36 { openNameEntry(); return true }
            return false
        }
    }

    private var lastHovered: String? = nil

    func hover(at p: CGPoint) {
        let activePrefix: String?
        switch screen {
        case .main: activePrefix = nil
        case .lobby: activePrefix = "l-"
        case .nameEntry, .settings: activePrefix = "m-"
        }
        // Sound_Over: play once when the pointer enters a button.
        var nowHovered: String? = nil
        for (name, entry) in hoverButtons {
            let inLayer = activePrefix == nil
                ? (!name.hasPrefix("m-") && !name.hasPrefix("l-"))
                : name.hasPrefix(activePrefix!)
            if inLayer && entry.0.frame.contains(p) { nowHovered = name; break }
        }
        if let h = nowHovered, h != lastHovered { Audio.shared.play("hover", volume: 0.5) }
        lastHovered = nowHovered

        for (name, entry) in hoverButtons {
            let inLayer = activePrefix == nil
                ? (!name.hasPrefix("m-") && !name.hasPrefix("l-"))
                : name.hasPrefix(activePrefix!)
            // frame is in the button's parent space; both root and modal sit at scene origin
            let over = inLayer && entry.0.frame.contains(p)
            let img = over ? entry.2 : entry.1
            entry.0.texture = SKTexture(image: img)
            entry.0.texture?.filteringMode = .nearest
        }
        for (name, b) in hoverArrows {
            let over = b.frame.contains(p)
            let base: HUDArt.ControlSprite = name.hasSuffix("down") ? .arrowLeft : .arrowRight
            b.texture = SKTexture(image: HUDArt.control(base, hover: over))
        }
    }

    func click(at p: CGPoint) -> Bool {
        let hit = nodes(at: p).compactMap(\.name)
        guard let name = hit.first else { return false }
        Audio.shared.play("click")
        switch screen {
        case .nameEntry:
            switch name {
            case "m-name-enter": confirmName()
            case "m-name-cancel": closeModal()
            default: break
            }
            return true
        case .settings:
            handleSettingsClick(name)
            return true
        case .lobby:
            handleLobbyClick(name)
            return true
        case .main:
            switch name {
            case "single": openNameEntry()
            case "quit": NSApp.terminate(nil)
            case let n where n.hasPrefix("dead-"):
                flash("The 2002 online services are gone - Single Player works!")
            default: return false
            }
            return true
        }
    }

    private func handleSettingsClick(_ name: String) {
        switch name {
        case "m-cancel": closeModal()
        case "m-create": openLobby()
        case "m-mapdown": selectedMap = (selectedMap + MapCatalog.maps.count - 1) % MapCatalog.maps.count
        case "m-mapup": selectedMap = (selectedMap + 1) % MapCatalog.maps.count
        case "m-players-down": playerCount = max(2, playerCount - 1)
        case "m-players-up": playerCount = min(8, playerCount + 1)
        case "m-lives-down": lives = max(0, lives - 1)
        case "m-lives-up": lives = min(5, lives + 1)
        case "m-gold-down": goldIdx = max(0, goldIdx - 1)
        case "m-gold-up": goldIdx = min(G.startingCashTable.count - 1, goldIdx + 1)
        case "m-seat-down": hotseatIdx = max(0, hotseatIdx - 1)
        case "m-seat-up": hotseatIdx = min(G.hotSeatTimes.count - 1, hotseatIdx + 1)
        case "m-treasure-down", "m-treasure-up": treasure.toggle()
        default: break
        }
        refreshSettings()
    }

    private func openNameEntry() {
        screen = .nameEntry
        buildNameEntry()
        modal.isHidden = false
    }

    private func closeModal() {
        screen = .main
        root.isHidden = false
        modal.isHidden = true
    }

    private func flash(_ s: String) {
        let l = text(s, cap: 18)
        l.position = CGPoint(x: 400, y: 40)
        l.zPosition = 80
        addChild(l)
        l.run(.sequence([.wait(forDuration: 2.2), .fadeOut(withDuration: 0.6), .removeFromParent()]))
    }

    private func startGame() {
        var opts = GameOptions()
        opts.mapIndex = selectedMap
        opts.startingCashIndex = goldIdx
        opts.maxRespawns = lives
        opts.hotSeatIndex = hotseatIdx
        opts.treasureRespawn = treasure
        // You (chosen color) + one AI per non-"none" slot. Colors repeat past 4;
        // names drawn from the original bot roster.
        let you = playerName.isEmpty ? "You" : playerName
        var players = [PlayerConfig(name: you, colorIndex: colorIndex % 4, botType: 0)]
        for slot in 1..<slotTypes.count where slotTypes[slot] > 0 {
            players.append(PlayerConfig(name: slotNames[slot] ?? G.botNames.randomElement()!,
                                        colorIndex: (colorIndex + slot) % 4,
                                        botType: slotTypes[slot]))
        }
        if players.count < 2 {   // all slots set to "none" — bring one crewmate
            players.append(PlayerConfig(name: G.botNames[0], colorIndex: (colorIndex + 1) % 4, botType: 3))
        }
        opts.players = players
        onStart?(opts)
    }
}
