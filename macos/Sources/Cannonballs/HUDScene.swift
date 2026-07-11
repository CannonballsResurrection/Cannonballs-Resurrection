import SpriteKit
import SceneKit
import AppKit

/// In-game HUD, rebuilt as a faithful clone of the original.
/// All coordinates live in the original's 800x600 screen space (positions and
/// sizes extracted from the game's UI layout); the scene scale-fills the window
/// so sprites render chunky and proportional exactly like the 2002 build.
///
/// Layout map (source pixels, top-left origin):
///  - Top magenta bar (h30, Button_Bar 800x30): Quit / Options / Camera / Help! menus.
///  - Power bar: three native ui.png slices spanning x 5..552, y 32..93.
///    Fill channel x 90..540; bone marker shows the last shot's power.
///  - Pitch bar: vertical 53x212 at x 12..66, y 94..306, bone = current pitch.
///  - Turn order list at (115,160), 24px rows.
///  - Weapon dropdown (gilt) centered at (677,69), 256x32.
///  - Right column: coin (684,135) 64x64, cash bar (655,210), Lives (655,260),
///    minimap 128x128 at center (720,350), fish + wind bar (655,540).
///  - Chat popup 295x88 at (5,486); "Press 'C' to Chat" hint at bottom.
///  - YOUR TURN / DEFENSE MODE banners centered at (400,480).
///  - Floating player name bars project from cannon positions (targetbar art).
final class HUDScene: SKScene {
    weak var game: GameController?
    weak var scnView: SCNView?          // for projecting cannon positions
    var resultsShown = false

    static let purple = NSColor(calibratedRed: 0.62, green: 0.08, blue: 0.78, alpha: 1)
    static let darkPurple = NSColor(calibratedRed: 0.22, green: 0.03, blue: 0.32, alpha: 1)
    static let gold = NSColor(calibratedRed: 0.98, green: 0.80, blue: 0.20, alpha: 1)
    static let orange = NSColor(calibratedRed: 1.0, green: 0.55, blue: 0.1, alpha: 1)
    static let chatBlue = NSColor(calibratedRed: 0.45, green: 0.72, blue: 1.0, alpha: 1)
    static let boldFont = "AvenirNext-Bold"
    static let font = "AvenirNext-Medium"

    // Source-space coordinate helper (top-left origin -> SpriteKit bottom-left).
    private func P(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x, y: 600 - y) }

    // HUD.java lays these pieces out in WT screen units: 0.002736 per pixel,
    // origin at screen center, +y up. wt() converts a verbatim WT coordinate
    // pair from the source into this scene's coords (positions are quad centers,
    // matching WTGroup.setPosition semantics).
    private static let wtS: CGFloat = 0.002736
    private func wt(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
        CGPoint(x: 400 + x / HUDScene.wtS, y: 300 + y / HUDScene.wtS)
    }

    private let root = SKNode()

    // power / pitch (PowerBarModel patch + PowerMarker1 / PitchMarker1 / PitchMarker2)
    private var powerFillBody = SKSpriteNode()
    private var powerFillCap = SKSpriteNode()
    private var powerBone = SKSpriteNode()
    private var pitchLive = SKSpriteNode()      // PitchMarker1: small marker, live tilt
    private var pitchLastBone = SKSpriteNode()  // PitchMarker2: bone, last shot's tilt

    // weapon — Button_3DWeapon(677,69): 256x32 gilt plane, name LEFT at 561,
    // cost RIGHT-aligned at 771 (Button_3DWeapon.java:66-99, :272-273; the
    // source has no ball icon)
    private var weaponButton = SKSpriteNode()
    private var weaponTitle = SKSpriteNode()
    private var weaponCost = SKSpriteNode()
    private var dropdown = SKNode()
    private var dropdownOpen = false

    // right column
    private var goldText = SKSpriteNode()
    private var livesText = SKSpriteNode()
    private var minimapSprite = SKSpriteNode()
    private var minimapNoise: [Float] = []      // per-cell color mottle, baked per island
    private var minimapAlphaNoise: [Float] = []
    private var minimapDirty = true
    private var windFish = SKSpriteNode()
    private var windText = SKSpriteNode()

    // top menus
    private var topMenu = SKNode()
    private var menuDropdown = SKNode()
    private var openMenu: String? = nil

    // turn list + floating name tags
    private var queueContainer = SKNode()
    private var tagNodes: [SKNode] = []
    private var tagBars: [SKSpriteNode] = []
    private var tagArrows: [SKSpriteNode] = []

    // chat — each visual line is a list of colored segments (bot lines are
    // blue name + white tail, exactly the original's backtick color toggling)
    private var chatContainer = SKNode()
    private var chatLines: [[(String, HUDArt.FontColor)]] = []
    private(set) var chatting = false
    private var chatMessage = ""
    private var chatBlink: Float = 0
    private var chatCursorOn = true
    private var chatEntry = SKSpriteNode()
    private var chatHint = SKSpriteNode()

    // fade-up game messages (HUD.java: addMessage/checkMessageQueue/updateGameMessage)
    private var messageQueue: [String] = []
    private var messagesInFlight: [(node: SKSpriteNode, pos: Float)] = []

    // banners / messages
    private var yourTurn = SKSpriteNode()
    private var defenseBanner = SKSpriteNode()
    private var timerText = SKSpriteNode()
    private var resultsNode = SKNode()
    private var reticle = SKSpriteNode()

    // Main.updateSinTable (Main.java:310-311): SinPosition[1] -= 2.75/s —
    // drives the banner pulse and the name-tag arrow bob
    private var sinPos1: Float = 0
    private var sin1: Float = 0

    // game-over / spectator (HUD.SuccessMessage, Camera spectator labels)
    private var barVisible = true                     // HUD.BarVisible (hideBar)
    private var successMessage: SKSpriteNode?
    private var spectatorLabel: SKSpriteNode?
    private var spectatorName: SKSpriteNode?
    private var hudBits: [SKSpriteNode] = []          // HUDBits[0..3], pulled by hideBar()
    private var barNodes: [SKNode] = []               // the rest hideBar removes (HUD.java:94-131)
    private var topMenuItems: [String: SKSpriteNode] = [:]

    override init(size: CGSize) {
        super.init(size: CGSize(width: 800, height: 600))
        scaleMode = .fill
        backgroundColor = .clear
        addChild(root)
        buildNodes()
    }
    required init?(coder: NSCoder) { fatalError() }

    func relayout(size: CGSize) {}   // fixed 800x600, window scaling handles the rest

    // MARK: - small helpers

    private func sprite(_ img: NSImage, _ pos: CGPoint, z: CGFloat = 10) -> SKSpriteNode {
        let n = SKSpriteNode(texture: SKTexture(image: img))
        n.texture?.filteringMode = .nearest
        n.size = img.size
        n.position = pos
        n.zPosition = z
        return n
    }
    /// A WT surface-shader quad, built verbatim from the source's numbers:
    /// setBitmapTextureRect fractions (u1,v1,u2,v2; v measured from the top)
    /// into a sheet, at the attachSurfaceShader WT dimensions.
    private func wtSlice(_ sheet: String, _ u1: CGFloat, _ v1: CGFloat, _ u2: CGFloat, _ v2: CGFloat,
                         w: CGFloat, h: CGFloat, at pos: CGPoint, z: CGFloat) -> SKSpriteNode {
        let tex = SKTexture(rect: CGRect(x: u1, y: 1 - v2, width: u2 - u1, height: v2 - v1),
                            in: HUDArt.texture(sheet))
        tex.filteringMode = .nearest
        let n = SKSpriteNode(texture: tex)
        n.size = CGSize(width: w / HUDScene.wtS, height: h / HUDScene.wtS)
        n.position = pos
        n.zPosition = z
        return n
    }
    private func textSprite(_ s: String, cap: CGFloat, tint: NSColor? = nil, left: Bool = false,
                            color: HUDArt.FontColor = .white) -> SKSpriteNode {
        let img = HUDArt.text(s, capHeight: cap, tint: tint, color: color)
        let n = SKSpriteNode(texture: SKTexture(image: img))
        n.size = img.size
        if left { n.anchorPoint = CGPoint(x: 0, y: 0.5) }
        return n
    }
    private func setText(_ node: SKSpriteNode, _ s: String, cap: CGFloat, tint: NSColor? = nil,
                         color: HUDArt.FontColor = .white) {
        let img = HUDArt.text(s, capHeight: cap, tint: tint, color: color)
        node.texture = SKTexture(image: img)
        node.size = img.size
    }
    /// Button_Bar media 9-slice (Button_Bar.java media constructor): 10px end
    /// caps + the stretched px 20..40 center band of the sheet, `h` rows tall
    /// (rows counted from the top of the sheet).
    private func barFromSheet(_ sheet: String, w: CGFloat, h: CGFloat) -> NSImage {
        let src = HUDArt.image(sheet)
        let sw = src.size.width, sh = src.size.height
        let img = NSImage(size: NSSize(width: w, height: h))
        img.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        // NSImage y-origin is bottom-left; sheet rows count from the top
        src.draw(in: NSRect(x: 10, y: 0, width: w - 20, height: h),
                 from: NSRect(x: 20, y: sh - h, width: 20, height: h),
                 operation: .copy, fraction: 1)
        src.draw(in: NSRect(x: 0, y: 0, width: 10, height: h),
                 from: NSRect(x: 0, y: sh - h, width: 10, height: h),
                 operation: .copy, fraction: 1)
        src.draw(in: NSRect(x: w - 10, y: 0, width: 10, height: h),
                 from: NSRect(x: sw - 10, y: sh - h, width: 10, height: h),
                 operation: .copy, fraction: 1)
        img.unlockFocus()
        return img
    }

    // MARK: - build

    private func buildNodes() {
        // ---- top magenta bar + menu items ----
        // TitleBar = Button_Bar(TopBar, 800, 30, 0, 15, 50) (HUD.java:1054):
        // 10px end caps + the stretched px 20..40 center band of the sheet,
        // 30 rows tall, left edge x=0, centerline y=15.
        let bar = sprite(barFromSheet("topbar.png", w: 800, h: 30), P(400, 15), z: 20)
        root.addChild(bar)
        var x: CGFloat = 10
        for item in ["Quit", "Options", "Camera", "Help!"] {
            let t = textSprite(item, cap: 24, left: true)
            // Button_3DMenu title at (TopLeftX+10, TopLeftY+16) = (x+12, 16)
            t.position = P(x, 16)
            t.zPosition = 21
            t.name = "menu-\(item)"
            root.addChild(t)
            topMenu.addChild(SKNode())
            topMenuItems[item] = t
            x += t.size.width + 28
        }
        menuDropdown.zPosition = 60
        menuDropdown.isHidden = true
        root.addChild(menuDropdown)

        // ---- power + pitch bar frames: HUDBits[0..3] (HUD.java:342-354, positions :546-549).
        // All numbers are the source's own: attachSurfaceShader dims, setBitmapTextureRect
        // fractions, setPosition WT coords. Draw order options: frame 71 < fill 72 <
        // PowerMarker1/PitchMarker1 73 < PitchMarker2 74 -> z 12 / 12.5 / 13 / 13.5.
        // Kept referenced so hideBar can pull them (HUD.java:107-110 removes ALL FOUR).
        hudBits = [
            wtSlice("ui.png", 0.0, 0.0, 0.99609375, 0.23828125,
                    w: 0.700416, h: 0.169632, at: wt(-0.730512, 0.648432), z: 12),
            wtSlice("ui.png", 0.0, 0.2421875, 0.99609375, 0.48046875,
                    w: 0.700416, h: 0.169632, at: wt(-0.030096, 0.648432), z: 12),
            wtSlice("ui.png", 0.859375, 0.484375, 0.99609375, 0.72265625,
                    w: 0.098496, h: 0.169632, at: wt(0.36936, 0.648432), z: 12),
            wtSlice("ui.png", 0.0, 0.515625, 0.83203125, 0.72265625,
                    w: 0.580032, h: 0.147744, at: wt(-0.987696, 0.2736), z: 12),
        ]
        // WT setBitmapOrientation(t) renders as SK zRotation -t: pinned by the minimap
        // arrow, which passes SpinAngle and draws at -spinAngle (HUDScene:517).
        hudBits[3].zRotation = .pi / 2    // HUDBits[3].setBitmapOrientation(-90): 212 wide -> 212 tall
        for f in hudBits { root.addChild(f) }

        // PowerBar fill: a 3x2 patch, 440x24 px (1.20384x0.065664 WT) hung from its
        // top-left corner at (-0.84816, 0.667584) (HUD.java:1010-1014, :1123). Tile 0
        // stretches POWERBAR texture u 0.1..0.5 over the body; tile 1 holds u 0.5..1.0
        // in the leading 12 px cap (Cannon.java updatePowerBar patch-point math).
        // powerbar.png is IMAGES/POWERBAR image.png+alpha.png merged, pixel-identical.
        // (Tile 0's bottom-left UV corner is u=0.0, a slight skew SpriteKit can't
        // express; the top edge's 0.1..0.5 mapping is used for the whole quad.)
        let fillSheet = HUDArt.texture("powerbar.png")
        powerFillBody = SKSpriteNode(texture: SKTexture(rect: CGRect(x: 0.1, y: 0, width: 0.4, height: 1), in: fillSheet))
        powerFillCap = SKSpriteNode(texture: SKTexture(rect: CGRect(x: 0.5, y: 0, width: 0.5, height: 1), in: fillSheet))
        for n in [powerFillBody, powerFillCap] {
            n.texture?.filteringMode = .nearest
            n.anchorPoint = CGPoint(x: 0, y: 1)   // patch origin corner; rows extend downward (z 0 -> -0.065664)
            n.position = wt(-0.84816, 0.667584)
            n.size = CGSize(width: 0, height: 0.065664 / HUDScene.wtS)
            n.zPosition = 12.5
            n.isHidden = true                     // shown only while charging (showPowerBar/hidePowerBar)
            root.addChild(n)
        }
        powerBone = wtSlice("ui.png", 0.0, 0.890625, 0.30859375, 0.99609375,
                            w: 0.21888, h: 0.079344, at: wt(-0.84816, 0.634752), z: 13)
        powerBone.zRotation = .pi / 2       // PowerMarker1.setBitmapOrientation(-90): stands upright across the bar
        powerBone.isHidden = true           // hidden until a first shot (LastPowerLevel -1000 sentinel)
        root.addChild(powerBone)

        // Pitch markers (no bitmapOrientation in source: both lie horizontal).
        // PitchMarker1 = small marker, live tilt (z 73); PitchMarker2 = bone, last shot (z 74).
        pitchLive = wtSlice("ui.png", 0.0, 0.8125, 0.19921875, 0.8828125,
                            w: 0.142272, h: 0.051984, at: wt(-0.990432, 0.05472), z: 13)
        root.addChild(pitchLive)
        pitchLastBone = wtSlice("ui.png", 0.0, 0.890625, 0.30859375, 0.99609375,
                                w: 0.21888, h: 0.079344, at: wt(-0.990432, 0.05472), z: 13.5)
        pitchLastBone.isHidden = true       // hidden until a first shot (LastTiltMarker -1000 sentinel)
        root.addChild(pitchLastBone)

        // ---- weapon dropdown (Button_3DWeapon(677,69), gilt) ----
        // 256x32 GiltButtonsDrop plane; name LEFT at TopLeftX+10=561, cost
        // RIGHT-aligned at TopLeftX+220=771, both f=1.0 (Button_3DWeapon.java:
        // 66-99, :272-273; the source draws no ball icon).
        weaponButton = sprite(HUDArt.giltButton(hover: false), P(677, 69), z: 15)
        weaponButton.size = CGSize(width: 256, height: 32)
        weaponButton.name = "weaponButton"
        root.addChild(weaponButton)
        weaponTitle = textSprite("", cap: 24, left: true)
        weaponTitle.position = P(561, 71)
        weaponTitle.zPosition = 15.1
        weaponTitle.name = "weaponButton"
        root.addChild(weaponTitle)
        weaponCost = textSprite("", cap: 24)
        weaponCost.position = P(771, 71)
        weaponCost.zPosition = 15.1
        weaponCost.name = "weaponButton"
        root.addChild(weaponCost)
        dropdown.zPosition = 60
        dropdown.isHidden = true
        root.addChild(dropdown)

        // ---- right column ----
        // HUD.java:289 — the gold coin spins: 4x4 sheet, 16 frames, 20 fps,
        // column-major (same traversal as the coin particle in FXSprites).
        let coinFrames: [SKTexture] = (0..<16).map { f in
            let t = HUDArt.cropTexture("coin.png", NSRect(x: CGFloat(f / 4) * 64, y: CGFloat(f % 4) * 64,
                                                          width: 64, height: 64))
            t.filteringMode = .nearest
            return t
        }
        let coin = sprite(HUDArt.crop("coin.png", NSRect(x: 0, y: 0, width: 64, height: 64)), P(716, 167), z: 12)
        coin.run(.repeatForever(.animate(with: coinFrames, timePerFrame: 0.05)))
        root.addChild(coin)
        // CashBar = Button_Bar(120, 655, 210) — left edge 655, centerline 210
        // (HUD.java:552); the value text centers at x=721 (HUD.java:620).
        let cashBar = sprite(HUDArt.textBar(width: 120), P(715, 210), z: 12)
        root.addChild(cashBar)
        goldText = textSprite("0", cap: 24)
        goldText.position = P(721, 210); goldText.zPosition = 13
        root.addChild(goldText)
        let livesLabel = textSprite("Lives", cap: 24)   // RespawnsMessage.show(721,236) (HUD.java:553-554)
        livesLabel.position = P(721, 236); livesLabel.zPosition = 12
        root.addChild(livesLabel)
        let livesBar = sprite(HUDArt.textBar(width: 120), P(715, 260), z: 12)
        root.addChild(livesBar)
        livesText = textSprite("2", cap: 24)            // RespawnMessage.show(721,260) (HUD.java:633-634)
        livesText.position = P(721, 260); livesText.zPosition = 13
        root.addChild(livesText)
        minimapSprite = SKSpriteNode()
        minimapSprite.size = CGSize(width: 128, height: 128)
        minimapSprite.position = P(720, 350)
        minimapSprite.zPosition = 12
        root.addChild(minimapSprite)
        // (Wind direction is shown by the 3D golden arrow attached to the camera;
        //  the HUD keeps only the "N mph" speed bar.)
        // WindBar = Button_Bar(120, 655, 540) (HUD.java:1056); the wind text
        // centers at (719,540) f=1.0 (HUD.java:317-322 showWindSpeed).
        let windBar = sprite(HUDArt.textBar(width: 120), P(715, 540), z: 12)
        root.addChild(windBar)
        windText = textSprite("0 mph", cap: 24)
        windText.position = P(719, 540); windText.zPosition = 13
        root.addChild(windText)
        // hideBar (HUD.java:94-131) pulls the whole right column + weapon button
        barNodes = [coin, cashBar, goldText, livesLabel, livesBar, livesText,
                    weaponButton, weaponTitle, weaponCost]

        // ---- turn list ----
        queueContainer.zPosition = 14
        root.addChild(queueContainer)

        // ---- chat popup ----
        // Original expanded chat (HUD.java:835): PopUp(5, 320, 295, "Chat") — the
        // full-height 256px popup art cropped to 295 wide, top-left (5, 320),
        // title at (+10, +16), 14 lines (Chat.java n2=14) from y=360.
        let chat = sprite(HUDArt.popupPanel(width: 295), CGPoint(x: 5 + 147.5, y: 600 - 320 - 128), z: 14)
        root.addChild(chat)
        let chatTitle = textSprite("Chat", cap: 24, left: true)
        chatTitle.position = P(15, 336); chatTitle.zPosition = 15
        root.addChild(chatTitle)
        chatContainer.zPosition = 15
        root.addChild(chatContainer)
        // entry line / hint at (14,587), Message_3D scale 0.75 → cap 18 (Chat.java:279)
        chatHint = textSprite("Press 'C' to Chat", cap: 18, left: true)
        chatHint.position = P(14, 587); chatHint.zPosition = 15
        root.addChild(chatHint)
        chatEntry = textSprite("", cap: 18, left: true)
        chatEntry.position = P(14, 587); chatEntry.zPosition = 15
        chatEntry.isHidden = true
        root.addChild(chatEntry)

        // ---- banners ----
        // Barrel-camera crossbones reticle (ReticleTex, 64px, centered). Shown only in barrel cam.
        reticle = sprite(HUDArt.image("reticle.png"), P(400, 300), z: 35)
        reticle.size = CGSize(width: 64, height: 64)
        reticle.isHidden = true
        root.addChild(reticle)

        // YOUR TURN / DEFENSE MODE: persistent 128x128 quads (0.350208 WT) at
        // (400,480) (setPosition(0,-0.49248), HUD.java:562/569), pulsing
        // 1-|sin1*0.1| (HUD.java:277-283); shown/hidden by refreshDynamic.
        yourTurn = sprite(HUDArt.image("yourturn.png"), P(400, 480), z: 40)
        yourTurn.size = CGSize(width: 128, height: 128)
        yourTurn.isHidden = true
        root.addChild(yourTurn)
        defenseBanner = sprite(HUDArt.image("defense.png"), P(400, 480), z: 40)
        defenseBanner.size = CGSize(width: 128, height: 128)
        defenseBanner.isHidden = true
        root.addChild(defenseBanner)
        // hot-seat timer digits centered at (495,525) f=1.0 (Timer.java:66-67)
        timerText = textSprite("", cap: 24, tint: nil)
        timerText.position = P(495, 525); timerText.zPosition = 40
        timerText.isHidden = true
        root.addChild(timerText)

        resultsNode.isHidden = true
        resultsNode.zPosition = 100
        addChild(resultsNode)

        // Full-screen software-rasterized 3D, drawn behind the whole HUD (below `root`).
        rasterBG.position = P(400, 300)
        rasterBG.size = CGSize(width: 800, height: 600)
        rasterBG.zPosition = -1000
        rasterBG.isHidden = true
        addChild(rasterBG)
    }

    private var rasterBG = SKSpriteNode()

    /// Show the software-rasterized frame as the 3D layer (or hide to reveal SceneKit).
    func setRasterImage(_ img: NSImage?) {
        guard let img else { rasterBG.isHidden = true; return }
        let t = SKTexture(image: img); t.filteringMode = .nearest
        rasterBG.texture = t
        rasterBG.size = CGSize(width: 800, height: 600)
        rasterBG.isHidden = false
    }

    // MARK: - fish frames

    private func fishFrame(_ i: Int) -> NSImage {
        let col = i % 2, row = (i / 2) % 4
        return HUDArt.crop("fish.png", NSRect(x: col * 64, y: row * 64, width: 64, height: 64))
    }

    // MARK: - static + dynamic refresh

    func rebuildStatic() {
        guard let game else { return }
        // stain mottle is randomized once per island (Island.java:294), not per repaint
        let g = Terrain.grid
        minimapNoise = (0..<(g * g)).map { _ in Float.random(in: -20...20) }
        minimapAlphaNoise = (0..<(g * g)).map { _ in Float.random(in: 0..<0.5) }
        setText(windText, "\(Int(game.windVelocity)) mph", cap: 24)
        chatLines = []
        appendChat("Welcome to \(game.world.map.name)!", color: .blue)
        rebuildNameTags()
        refreshDynamic()
        updateMinimap()
    }

    /// Enabled state per weapon (HUD.java:597-609 createWeaponButton):
    /// 0 = disabled (gray), 1 = offensive (white), 2 = defensive (blue).
    private func weaponEnabled(_ c: Cannon, _ w: WeaponType) -> Int {
        var e = w.offensive ? 1 : 2
        if w.cost > c.cash { e = 0 }
        if let game, game.currentPlayerIndex != c.index, w.offensive { e = 0 }
        return e
    }
    private func enabledColor(_ e: Int) -> HUDArt.FontColor {
        e == 1 ? .white : (e == 2 ? .blue : .gray)
    }

    func refreshDynamic() {
        guard let game else { return }
        let c = game.localHuman ?? game.controlledCannon
        setText(goldText, "\(c?.cash ?? 0)", cap: 24)
        let lives = max(0, game.options.maxRespawns - (c?.respawnsUsed ?? 0))
        setText(livesText, "\(lives)", cap: 24)
        if let c, barVisible {
            let w = c.weapon
            let e = weaponEnabled(c, w)
            setText(weaponTitle, w.displayName, cap: 24, color: enabledColor(e))
            setText(weaponCost, "\(w.cost)", cap: 24, color: enabledColor(e))
            // cost RIGHT-aligned at 771 (Message_3D type 2, Button_3DWeapon.java:273)
            weaponCost.position.x = 771 - textAdvance("\(w.cost)", cap: 24) / 2
        }
        // showYourTurn / showDefense: persistent, mutually exclusive banners
        // (HUD.java:698-707, :559-570); both fall with the bar (hideBar).
        if let c, barVisible, !game.gameOver {
            let myTurn = game.currentPlayerIndex == c.index
            yourTurn.isHidden = !myTurn
            defenseBanner.isHidden = myTurn
        } else {
            yourTurn.isHidden = true
            defenseBanner.isHidden = true
        }
        rebuildQueue(game: game)
        rebuildNameTags()
        if dropdownOpen { layoutDropdown() }
    }

    // turn order list: current player full size, rest smaller + indented
    private func rebuildQueue(game: GameController) {
        queueContainer.removeAllChildren()
        var order: [Cannon] = []
        var idx = game.currentPlayerIndex
        for _ in 0..<game.players.count {
            if game.players[idx].active { order.append(game.players[idx]) }
            idx = (idx + 1) % game.players.count
        }
        // Original: current player white f=1.0 at (115,160); the rest blue f=0.75,
        // indented +10, stepping 24px (updateNextUp, HUD.java:359-433). The whole
        // list relocates to (55,100) while the bar is hidden (spectator).
        let baseX: CGFloat = barVisible ? 115 : 55
        let baseY: CGFloat = barVisible ? 160 : 100
        for (i, p) in order.prefix(6).enumerated() {
            let l = textSprite(p.name, cap: i == 0 ? 24 : 18, left: true,
                               color: i == 0 ? .white : .blue)
            l.position = P(i == 0 ? baseX : baseX + 10, baseY + CGFloat(i) * 24)
            queueContainer.addChild(l)
        }
    }

    // MARK: - floating name tags (projected from cannon positions)

    private func rebuildNameTags() {
        guard let game else { return }
        for n in tagNodes { n.removeFromParent() }
        tagNodes = []; tagBars = []; tagArrows = []
        for p in game.players {
            let holder = SKNode()
            holder.zPosition = 30
            let nameImg = HUDArt.text(p.name, capHeight: 24)
            // bar width = PixelWidth + 20, 28 tall (HUD.java:445)
            let barW = nameImg.size.width + 20
            let active = game.currentPlayerIndex == p.index
            let bar = sprite(HUDArt.targetBar(width: barW, active: active), .zero, z: 30)
            holder.addChild(bar)
            let label = SKSpriteNode(texture: SKTexture(image: nameImg))
            label.size = nameImg.size
            // TargetNames show at bar x + 0.01368 WT = +5px (HUD.java:258)
            label.position = CGPoint(x: 5, y: 0)
            label.zPosition = 31
            holder.addChild(label)
            // TargetArrow: GlobalMedia.Arrow 32x32 hanging 23px (0.062928 WT)
            // below; bobs on the current player (HUD.java:260-264)
            let arrow = sprite(HUDArt.image("menuarrow.png"), CGPoint(x: 0, y: -23), z: 30)
            arrow.size = CGSize(width: 32, height: 32)
            holder.addChild(arrow)
            holder.isHidden = true
            root.addChild(holder)
            tagNodes.append(holder); tagBars.append(bar); tagArrows.append(arrow)
        }
    }

    private func updateNameTags() {
        guard let game, let view = scnView else { return }
        let vw = view.bounds.width, vh = view.bounds.height
        guard vw > 0, vh > 0 else { return }
        // Tags ride a FIXED screen row, following only the projected x
        // (HUD.java:256-258: TempVector.Y is overwritten with 0.53352 /
        // 0.69768 WT = source y 105 / 45).
        let rowY: CGFloat = barVisible ? 600 - 105 : 600 - 45
        let spectating = game.camera.mode == .spectator
        for (i, p) in game.players.enumerated() {
            guard i < tagNodes.count else { break }
            let node = tagNodes[i]
            guard p.active, !p.dying else { node.isHidden = true; continue }
            // HUD.java:256 — your own cannon never shows a name tag; the
            // spectator camera's followed player hides his too
            guard p !== game.localHuman,
                  !(spectating && game.currentPlayerIndex == p.index)
            else { node.isHidden = true; continue }
            let world = SCNVector3(CGFloat(p.position.x), CGFloat(p.position.y), CGFloat(p.position.z))
            let proj = view.projectPoint(world)
            guard proj.z < 1 else { node.isHidden = true; continue }
            let sx = CGFloat(proj.x) / vw * 800
            node.position = CGPoint(x: sx, y: rowY)
            node.isHidden = false
            // arrow bob on the current player (HUD.java:260-263)
            tagArrows[i].position.y = game.currentPlayerIndex == p.index
                ? -(23 + CGFloat(sin1) * 5 + 5) : -23
        }
    }

    // MARK: - minimap

    func markMinimapDirty() { minimapDirty = true }

    func updateMinimap() {
        guard minimapDirty, let game else { return }
        minimapDirty = false
        let S: CGFloat = 128
        let img = NSImage(size: NSSize(width: S, height: S))
        img.lockFocus()
        NSGraphicsContext.current?.imageInterpolation = .none
        HUDArt.image("map.png").draw(in: NSRect(x: 0, y: 0, width: S, height: S))
        drawIslandStain(game)
        // World → map pixel per Island.java:1008: cell (x,z)/vertexScale lands at
        // texture (16+i, 16+j); the texture's V axis flips on screen, so in this
        // bottom-up drawing context that is simply y = 16 + z/vs (z=0 at the bottom).
        let vs = CGFloat(Terrain.vertexScale)
        func pt(_ p: SIMD3<Float>) -> CGPoint {
            CGPoint(x: 16 + CGFloat(p.x) / vs, y: 16 + CGFloat(p.z) / vs)
        }
        // MAPBITS icons, 15px (0.04104/0.002736): gold X = chests (Chest.java:145),
        // red X = other cannons (Cannon.java:1267), arrow 15x36 = you.
        func icon(_ crop: NSRect, at p: CGPoint) {
            HUDArt.crop("mapbits.png", crop)
                .draw(in: NSRect(x: p.x - 7.5, y: p.y - 7.5, width: 15, height: 15),
                      from: .zero, operation: .sourceOver, fraction: 1)
        }
        for chest in game.chests where chest.alive {
            icon(NSRect(x: 17, y: 2, width: 14, height: 14), at: pt(chest.position))
        }
        let you = game.localHuman
        for c in game.players where c.active && !c.dying && c !== you {
            icon(NSRect(x: 2, y: 2, width: 14, height: 14), at: pt(c.position))
        }
        if let you, you.active, !you.dying {
            let p = pt(you.position)
            // Native player marker: the MapBits arrow sprite, rotated to face heading.
            // (MiniArrowGroup UV 0.03125,0.28125..0.25,0.828125 of the 64px sheet.)
            let icon = HUDArt.crop("mapbits.png", NSRect(x: 2, y: 18, width: 14, height: 35))
            let iw: CGFloat = 15, ih: CGFloat = 36
            let ctx = NSGraphicsContext.current
            ctx?.saveGraphicsState()
            let xf = NSAffineTransform()
            xf.translateX(by: p.x, yBy: p.y)
            xf.rotate(byDegrees: -CGFloat(you.spinAngle))   // art points up; heading 0 = +z = up-screen
            xf.concat()
            icon.draw(in: NSRect(x: -iw / 2, y: -ih / 2, width: iw, height: ih),
                      from: .zero, operation: .sourceOver, fraction: 1)
            ctx?.restoreGraphicsState()
        }
        img.unlockFocus()
        minimapSprite.texture = SKTexture(image: img)
        minimapSprite.texture?.filteringMode = .nearest
    }

    /// The island as a rust-brown mottled stain on the parchment, verbatim from
    /// Island.java:262 (createMiniMap): cells with 0 < h/mapScale < 0.4 blend
    /// (139,55,24)±noise at opacity fading with elevation, with a dark (20,20,20)
    /// shoreline ring. Recomputing from live heights also reproduces the original's
    /// water-crater erase (Island.java:547 restores parchment where terrain sinks).
    private func drawIslandStain(_ game: GameController) {
        let g = Terrain.grid
        guard minimapNoise.count == g * g,
              let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 128, pixelsHigh: 128,
                                         bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                                         isPlanar: false, colorSpaceName: .deviceRGB,
                                         bytesPerRow: 128 * 4, bitsPerPixel: 32),
              let px = rep.bitmapData else { return }
        memset(px, 0, 128 * 128 * 4)
        let terrain = game.world.terrain
        let scale = game.world.map.mapScale
        for j in 0..<g {
            for i in 0..<g {
                var f = terrain.current[j * g + i] / scale
                guard f < 0.4 && f > 0 else { continue }
                f -= 0.04
                if f < 0 { f *= -6 }
                let r: Float, gc: Float, b: Float
                if f < 0.04 { r = 20; gc = 20; b = 20 }
                else { r = 139; gc = 55; b = 24 }
                let noise = minimapNoise[j * g + i]
                var a = (1 - f * 2) - minimapAlphaNoise[j * g + i]
                if a < 0 { a = 0 }
                if f < 0.025 { a = 0.9 }
                // texture row 16+j renders bottom-up on screen → bitmap row (top-down)
                let o = ((112 - j) * 128 + 16 + i) * 4
                px[o] = UInt8(max(0, min(255, (r - noise) * a)))       // premultiplied
                px[o + 1] = UInt8(max(0, min(255, (gc - noise) * a)))
                px[o + 2] = UInt8(max(0, min(255, (b - noise) * a)))
                px[o + 3] = UInt8(max(0, min(255, a * 255)))
            }
        }
        // sourceAtop = blend color but keep the parchment's alpha (blendPixel semantics:
        // the stain never paints outside the torn parchment edge)
        let stain = NSImage(size: NSSize(width: 128, height: 128))
        stain.addRepresentation(rep)
        stain.draw(in: NSRect(x: 0, y: 0, width: 128, height: 128),
                   from: .zero, operation: .sourceAtop, fraction: 1)
    }

    // MARK: - per-frame

    private var lastArrowAngle: Float = .nan
    func update(dt: Float) {
        guard let game else { return }
        // Main.updateSinTable (Main.java:310-311): SinPosition[1] -= 2.75/s
        sinPos1 -= 2.75 * dt
        sin1 = sin(sinPos1)
        // banner pulse: setBitmapSize(1 - |SinTable[1] * 0.1|) (HUD.java:277-283)
        let pulse = CGFloat(1 - abs(sin1 * 0.1))
        if !yourTurn.isHidden { yourTurn.size = CGSize(width: 128 * pulse, height: 128 * pulse) }
        if !defenseBanner.isHidden { defenseBanner.size = CGSize(width: 128 * pulse, height: 128 * pulse) }
        // the original MiniArrow follows SpinAngle live (Cannon.java:409)
        if let you = game.localHuman, abs(you.spinAngle - lastArrowAngle) > 0.5 || lastArrowAngle.isNaN {
            lastArrowAngle = you.spinAngle
            markMinimapDirty()
        }
        reticle.isHidden = game.camera.mode != .barrel   // crossbones only in barrel cam
        let c = game.localHuman ?? game.controlledCannon
        if let c, barVisible {
            // PowerBar patch columns (Cannon.java:579-589): right edge at level*440 px,
            // middle column 12 px behind it, so the u 0.5..1.0 cap tile leads the fill.
            let fillPx = CGFloat(c.powerLevel) * 440
            let bodyW = max(fillPx - 12, 0)
            powerFillBody.isHidden = !c.powerBarActive
            powerFillCap.isHidden = !c.powerBarActive
            powerFillBody.size.width = bodyW
            powerFillCap.size.width = fillPx - bodyW
            powerFillCap.position.x = wt(-0.84816, 0).x + bodyW
            // updateLastPowerMarker (Cannon.java:1072-1078)
            powerBone.isHidden = c.lastPowerLevel < -999
            powerBone.position = wt(-0.84816 + CGFloat(c.lastPowerLevel) * 440 * HUDScene.wtS, 0.634752)
            // updateTiltMarker (Cannon.java:1555-1565):
            // f = (-TiltAngle + MinTilt) / (MinTilt + MaxTilt) * 165 px above y base 0.05472
            func tiltMarkerPos(_ tilt: Float) -> CGPoint {
                let f = CGFloat((-tilt + G.minTiltAngle) / (G.minTiltAngle + G.maxTiltAngle)) * 165
                return wt(-0.990432, 0.05472 + f * HUDScene.wtS)
            }
            pitchLive.position = tiltMarkerPos(c.tiltAngle)
            pitchLastBone.isHidden = c.lastTiltMarker < -999
            pitchLastBone.position = tiltMarkerPos(c.lastTiltMarker)
        }
        updateNameTags()

        if game.hotSeatRemaining > 0 && Float(G.hotSeatTimes[game.options.hotSeatIndex]) > 0 && !game.gameOver {
            timerText.isHidden = false
            setText(timerText, "\(Int(ceil(game.hotSeatRemaining)))", cap: 24)
        } else {
            timerText.isHidden = true
        }

        updateGameMessages(dt: dt)
        if chatting {                       // cursor blink, 0.9 s toggle (Chat.java:115)
            chatBlink += dt
            if chatBlink > 0.9 {
                chatBlink = 0
                chatCursorOn.toggle()
                refreshChatEntry()
            }
        }
    }

    // MARK: - banners / messages / chat

    /// Turn-change announcement. HUD.java routes these through the float-up
    /// message queue (showMyTurn/showOtherTurn addMessage, HUD.java:708/755);
    /// the persistent YOUR TURN / DEFENSE art is driven by refreshDynamic.
    func showBanner(_ text: String) {
        flashMessage(text)
        refreshDynamic()
    }

    /// System/game message: queued to float up from screen center and fade out
    /// (HUD.java addMessage → checkMessageQueue → updateGameMessage). NOT chat.
    func flashMessage(_ text: String) {
        if messageQueue.count < 18 { messageQueue.append(text) }   // MessageList cap
    }

    /// The big persistent end-state message — "You Win!" / "You Lose!" /
    /// "<name> Wins!". HUD.showSuccessMessage (HUD.java:163-169): destroys any
    /// previous one, Message_3D(text, centered, scale 1.0, order 80, color 1 =
    /// BLUE sheet) at (400,324). It stays until the match is torn down.
    func showSuccessMessage(_ text: String) {
        successMessage?.removeFromParent()
        let node = textSprite(text, cap: 24, color: .blue)
        node.position = P(400, 324)
        node.zPosition = 80
        addChild(node)
        successMessage = node
    }

    /// The HUD half of spectator mode. Camera.setSpectatorCamera
    /// (Camera.java:278-290) calls hud.hideBar() and posts the persistent
    /// "Spectator Mode" label at (400,550) plus the followed player's name at
    /// (400,570) (refreshSpectatorCamera, Camera.java:426-433).
    func enterSpectatorMode(following name: String) {
        // hideBar (HUD.java:94-131): ALL FOUR bar quads come off with the three
        // markers, the power fill, the weapon button, coin icon, Lives label,
        // cash and respawn bars/text; YOUR TURN/DEFENSE clear; the Camera menu
        // is removed with Help! recreated in its slot at x=165.
        barVisible = false
        for f in hudBits { f.isHidden = true }
        powerFillBody.isHidden = true; powerFillCap.isHidden = true
        powerBone.isHidden = true
        pitchLive.isHidden = true; pitchLastBone.isHidden = true
        for n in barNodes { n.isHidden = true }
        dropdownOpen = false; dropdown.isHidden = true
        yourTurn.isHidden = true
        defenseBanner.isHidden = true
        if let cam = topMenuItems["Camera"], let help = topMenuItems["Help!"] {
            cam.isHidden = true
            // Button_3DMenu(165, 14): title left edge at 165+12 (HUD.java:104-107)
            help.position = CGPoint(x: 177, y: help.position.y)
        }
        if let game { rebuildQueue(game: game) }   // updateNextUp relocates to (55,100)
        if spectatorLabel == nil {
            let l = textSprite("Spectator Mode", cap: 24)
            l.position = P(400, 550)
            l.zPosition = 30
            addChild(l)
            spectatorLabel = l
        }
        refreshSpectatorName(name)
    }

    /// refreshSpectatorCamera (Camera.java:426-433): swap in the current
    /// player's name under the Spectator Mode label on every turn change. The
    /// name is BLUE (Message_3D(..., 30, 1)).
    func refreshSpectatorName(_ name: String) {
        guard spectatorLabel != nil else { return }
        spectatorName?.removeFromParent()
        let l = textSprite(name, cap: 24, color: .blue)
        l.position = P(400, 570)
        l.zPosition = 30
        addChild(l)
        spectatorName = l
    }

    /// Rise 20 px/s from (400,300), alpha 255→0 over 100 px; up to 6 in flight,
    /// the next spawns once every visible message has risen past 24 px.
    private func updateGameMessages(dt: Float) {
        for i in messagesInFlight.indices {
            messagesInFlight[i].pos += dt * 20
            let m = messagesInFlight[i]
            m.node.position = P(400, 300 - CGFloat(m.pos))
            m.node.alpha = CGFloat(max(0, 1 - m.pos / 100))
        }
        messagesInFlight.removeAll { m in
            if m.pos > 100 { m.node.removeFromParent(); return true }
            return false
        }
        if !messageQueue.isEmpty, messagesInFlight.count < 6,
           messagesInFlight.allSatisfy({ $0.pos >= 24 }) {
            let node = textSprite(messageQueue.removeFirst(), cap: 24)
            node.position = P(400, 300)
            node.zPosition = 70
            addChild(node)
            messagesInFlight.append((node, 0))
        }
    }

    // MARK: - chat lines + wrapping

    /// Pen advance of a string in HUD pixels (Text.java: width * 0.75 per glyph,
    /// scaled by capHeight/24 — chat renders at scale 0.75 = cap 18).
    private func textAdvance(_ s: String, cap: CGFloat) -> CGFloat {
        var w: CGFloat = 0
        for ch in s.unicodeScalars {
            var idx = Int(ch.value) - 32
            if idx < 0 || idx >= HUDArt.charWidths.count { idx = 0 }
            w += HUDArt.charWidths[idx] * 0.75 * (cap / 24)
        }
        return w
    }

    /// Text.java wordWrap: tokenize on spaces/commas (kept), accumulate pixel
    /// width against the 250px chat column, hard-split words wider than a line.
    private func wordWrap(_ s: String, width: CGFloat, cap: CGFloat) -> [String] {
        var lines: [String] = [""]
        var lineW: CGFloat = 0
        var token = ""
        func flushToken() {
            guard !token.isEmpty else { return }
            let tw = textAdvance(token, cap: cap)
            if lineW + tw < width {
                lines[lines.count - 1] += token; lineW += tw
            } else if tw < width {
                lines.append(token); lineW = tw
            } else {                       // single token wider than the box
                for ch in token {
                    let cw = textAdvance(String(ch), cap: cap)
                    if lineW + cw >= width { lines.append(""); lineW = 0 }
                    lines[lines.count - 1] += String(ch); lineW += cw
                }
            }
            token = ""
        }
        for ch in s {
            token.append(ch)
            if ch == " " || ch == "," { flushToken() }
        }
        flushToken()
        return lines
    }

    /// Bot/player chat line. The original wires '`name:`msg' — the backticks toggle
    /// the BLUE font sheet, so the name+colon renders blue and the message white
    /// (ref: originals/video_frames frame_020/023). Wrapped to the 250px column.
    func botChat(_ name: String, _ msg: String) {
        let head = "\(name):"
        let headW = textAdvance(head, cap: 18)
        let wrapped = wordWrap(msg, width: 250 - headW, cap: 18)
        var first = true
        for part in wrapped {
            if first {
                chatLines.append([(head, .blue), (part, .white)])
                first = false
            } else {
                chatLines.append([(part, .white)])
            }
        }
        redrawChat()
    }

    private func appendChat(_ s: String, color: HUDArt.FontColor) {
        for part in wordWrap(s, width: 250, cap: 18) {
            chatLines.append([(part, color)])
        }
        redrawChat()
    }

    private func redrawChat() {
        if chatLines.count > 14 {                      // Chat.java: 14 lines expanded
            chatLines.removeFirst(chatLines.count - 14)
        }
        chatContainer.removeAllChildren()
        for (i, segments) in chatLines.enumerated() {
            // ChatBlock.show(15, 360): lines run top-down, oldest first; line
            // pitch = 28 x 0.75 (newline) x 0.75 (group scale) = 15.75 px
            // (Message_3D.java:76 + setAbsoluteScale)
            var x: CGFloat = 15
            for (text, color) in segments {
                let l = textSprite(text, cap: 18, tint: nil, left: true, color: color)
                l.position = CGPoint(x: x, y: P(0, 360 + CGFloat(i) * 15.75).y)
                chatContainer.addChild(l)
                x += textAdvance(text, cap: 18)
            }
        }
    }

    // MARK: - chat entry (Chat.java enableChat/keyDownChat/updateChatType)

    func beginChatEntry() {
        guard !chatting else { return }
        chatting = true
        chatMessage = ""
        chatBlink = 0
        chatCursorOn = true
        refreshChatEntry()
    }

    /// Returns true when the event was consumed by the chat entry line.
    func chatKeyDown(_ e: NSEvent) -> Bool {
        guard chatting else { return false }
        switch e.keyCode {
        case 36, 76:                       // return — post and close
            postChatMessage()
            return true
        case 53:                           // esc — cancel entry
            chatting = false
            chatMessage = ""
            refreshChatEntry()
            return true
        case 51:                           // backspace
            if !chatMessage.isEmpty { chatMessage.removeLast() }
            refreshChatEntry()
            return true
        default:
            for ch in (e.characters ?? "").unicodeScalars
            where ch.value >= 32 && ch.value < 127 {
                // original caps the entry at 750px rendered width
                if textAdvance("SAY : " + chatMessage, cap: 18) < 750 {
                    chatMessage.append(Character(ch))
                }
            }
            refreshChatEntry()
            return true
        }
    }

    private func postChatMessage() {
        if !chatMessage.isEmpty {
            botChat(game?.localHuman?.name ?? "You", chatMessage)
        }
        chatMessage = ""
        chatting = false
        refreshChatEntry()
    }

    private func refreshChatEntry() {
        chatHint.isHidden = chatting
        chatEntry.isHidden = !chatting
        guard chatting else { return }
        // cursor = the 0x7F glyph, blinking on a 0.9 s toggle (Chat.java:115)
        let cursor = chatCursorOn ? String(Character(UnicodeScalar(127))) : ""
        let img = HUDArt.text("SAY : " + chatMessage + cursor, capHeight: 18)
        chatEntry.texture = SKTexture(image: img)
        chatEntry.size = img.size
    }

    // MARK: - results

    func showResults() {
        guard let game, !resultsShown else { return }
        resultsShown = true
        resultsNode.removeAllChildren()
        resultsNode.isHidden = false

        if let rb = Assets.image("MENUS/RESULTS.png") {
            let back = sprite(rb, CGPoint(x: 400, y: 300), z: 0)
            back.size = CGSize(width: 800, height: 600)
            resultsNode.addChild(back)
        }
        // Source layout (Menu_Results_Screen.java:76-138): title with the blue
        // backticked name centered at (400,200); THIS GAME (400,250) / TOTAL
        // (600,250) headers; blue stat names LEFT at x=200 stepping 20px; blue
        // "+N" this-game column centered at 400; white career totals centered
        // at 600; Done at (400,575). All f=1.0 (cap 24).
        let you = game.localHuman ?? game.players[0]
        func centered(_ str: String, _ x: CGFloat, _ y: CGFloat, color: HUDArt.FontColor = .white) {
            let l = textSprite(str, cap: 24, tint: nil, color: color)
            l.position = CGPoint(x: x, y: 600 - y)
            l.zPosition = 2
            resultsNode.addChild(l)
        }
        func left(_ str: String, _ x: CGFloat, _ y: CGFloat, color: HUDArt.FontColor = .white) {
            let l = textSprite(str, cap: 24, tint: nil, left: true, color: color)
            l.position = CGPoint(x: x, y: 600 - y)
            l.zPosition = 2
            resultsNode.addChild(l)
        }
        // "Game Stats For '`name`'": the backticks toggle the BLUE sheet around
        // the name only — white quotes + blue name, composed as three centered
        // segments (Menu_Results_Screen.java:76).
        let headA = "Game Stats For '"
        let fullW = textAdvance(headA + you.name + "'", cap: 24)
        var tx = 400 - fullW / 2
        let titleSegs: [(String, HUDArt.FontColor)] = [(headA, .white), (you.name, .blue), ("'", .white)]
        for (seg, color) in titleSegs {
            let l = textSprite(seg, cap: 24, tint: nil, left: true, color: color)
            l.position = CGPoint(x: tx, y: 600 - 200)
            l.zPosition = 2
            resultsNode.addChild(l)
            tx += textAdvance(seg, cap: 24)
        }
        centered("THIS GAME", 400, 250)
        centered("TOTAL", 600, 250)
        let names = ["Kills", "Misses", "Deaths", "Drownings", "Gold Spent"]
        let thisGame = [you.kills, you.misses, you.deaths, you.drownings, you.goldSpent]
        // career totals persisted across games (the original showed account totals)
        let d = UserDefaults.standard
        for (i, n) in names.enumerated() {
            let key = "career.\(n)"
            let total = d.integer(forKey: key) + thisGame[i]
            d.set(total, forKey: key)
            left(n, 200, 280 + CGFloat(i) * 20, color: .blue)
            centered("+ \(thisGame[i])", 400, 280 + CGFloat(i) * 20, color: .blue)
            centered("\(total)", 600, 280 + CGFloat(i) * 20)
        }
        // Done: the standard 256x32 text button (Button_3D(400,575,"DONE","Done"))
        let exit = sprite(HUDArt.menuButton(hover: false), CGPoint(x: 400, y: 600 - 575), z: 2)
        exit.size = CGSize(width: 256, height: 32)
        exit.name = "results-exit"
        resultsNode.addChild(exit)
        let exitL = textSprite("Done", cap: 24)
        exitL.position = CGPoint(x: 400, y: 600 - 577)   // Button_3D label center +2
        exitL.zPosition = 3
        exitL.name = exit.name
        resultsNode.addChild(exitL)
    }

    // MARK: - top menus + weapon dropdown

    private func layoutDropdown() {
        dropdown.removeAllChildren()
        guard let game, let c = game.controlledCannon else { return }
        // Rollout rows are 32 px per weapon: Items[n].show(TopLeftX+10,
        // TopLeftY+47+n*32) = (561, 102+n*32) with the cost RIGHT-aligned at
        // 771, f=1.0 colored by Enabled (Button_3DWeapon.java:42-43, :95-103;
        // the source draws no ball icons).
        for wt in WeaponType.allCases {
            let rowY = 102 + CGFloat(wt.rawValue) * 32
            let row = sprite(HUDArt.giltDropRow(hover: false), P(677, rowY), z: 60)
            row.size = CGSize(width: 254, height: 32)
            row.name = "weapon-\(wt.rawValue)"
            let e = weaponEnabled(c, wt)
            let nameL = textSprite(wt.displayName, cap: 24, left: true, color: enabledColor(e))
            nameL.position = CGPoint(x: 561 - 677, y: 0)
            nameL.zPosition = 0.1
            nameL.name = row.name
            row.addChild(nameL)
            let costL = textSprite("\(wt.cost)", cap: 24, color: enabledColor(e))
            costL.position = CGPoint(x: 771 - textAdvance("\(wt.cost)", cap: 24) / 2 - 677, y: 0)
            costL.zPosition = 0.1
            costL.name = row.name
            row.addChild(costL)
            dropdown.addChild(row)
        }
    }

    private func layoutTopMenu(_ which: String) {
        menuDropdown.removeAllChildren()
        var items: [(String, String)] = []
        switch which {
        case "Quit": items = [("Exit To Menu", "act-quit")]
        case "Options":
            // Main.java getSettings: Shadows, Sound, and Music only while Sound is on
            items = [(FXSprites.shadowsEnabled ? "Shadows : On" : "Shadows : Off", "act-shadows"),
                     (Audio.shared.sfxVolume > 0 ? "Sound : On" : "Sound : Off", "act-sound")]
            if Audio.shared.sfxVolume > 0 {
                items.append((Audio.shared.musicVolume > 0 ? "Music : On" : "Music : Off", "act-music"))
            }
        case "Camera": items = CameraController.Mode.selectable.map { ("\($0.label)", "act-cam-\($0.rawValue)") }
        case "Help!": items = [("Controls", "act-help-0"),          // HUD.java:1071
                               ("How To Play", "act-help-1"),
                               ("Tutorial", "act-help-2")]
        default: break
        }
        let xs: [String: CGFloat] = ["Quit": 10, "Options": 60, "Camera": 140, "Help!": 230]
        let x0 = xs[which] ?? 10
        for (i, item) in items.enumerated() {
            let row = sprite(HUDArt.rolloutRow(), CGPoint(x: x0 + 90, y: 600 - (34 + CGFloat(i) * 24)), z: 60)
            row.size = CGSize(width: 190, height: 24)
            row.name = item.1
            let l = textSprite(item.0, cap: 18, left: true)
            l.position = CGPoint(x: -86, y: 0)
            l.zPosition = 0.1
            l.name = item.1
            row.addChild(l)
            menuDropdown.addChild(row)
        }
    }

    // MARK: - mouse

    func hover(at p: CGPoint) {}

    func click(at p: CGPoint) -> Bool {
        // The results screen is terminal (stats + Done). Dismiss on a click
        // ANYWHERE, not just the Done sprite: any-click is robust against a
        // hit-test/coordinate mismatch that could otherwise strand the player.
        // (Return/Esc also leave — GameController.keyDown.)
        if resultsShown { game?.onExit?(); return true }
        let hit = nodes(at: p)
        for n in hit {
            guard let name = n.name else { continue }
            if name == "results-exit" { game?.onExit?(); return true }
            if name == "weaponButton" {
                dropdownOpen.toggle()
                dropdown.isHidden = !dropdownOpen
                if dropdownOpen { layoutDropdown(); openMenu = nil; menuDropdown.isHidden = true }
                Audio.shared.play("click")
                return true
            }
            if name.hasPrefix("weapon-"), let idx = Int(name.dropFirst(7)) {
                game?.selectWeapon(idx)
                dropdownOpen = false
                dropdown.isHidden = true
                return true
            }
            if name.hasPrefix("menu-") {
                let which = String(name.dropFirst(5))
                if openMenu == which {
                    openMenu = nil; menuDropdown.isHidden = true
                } else {
                    openMenu = which
                    layoutTopMenu(which)
                    menuDropdown.isHidden = false
                    dropdownOpen = false; dropdown.isHidden = true
                }
                Audio.shared.play("click")
                return true
            }
            if name.hasPrefix("act-") {
                handleMenuAction(name)
                openMenu = nil; menuDropdown.isHidden = true
                return true
            }
        }
        if dropdownOpen { dropdownOpen = false; dropdown.isHidden = true; return true }
        if openMenu != nil { openMenu = nil; menuDropdown.isHidden = true; return true }
        return false
    }

    private func handleMenuAction(_ name: String) {
        let d = UserDefaults.standard
        switch name {
        case "act-quit": game?.onExit?()
        case "act-shadows":
            FXSprites.shadowsEnabled.toggle()
            d.set(FXSprites.shadowsEnabled, forKey: "opt.shadows")
            game?.setShadowsVisible(FXSprites.shadowsEnabled)
        case "act-sound":
            let on = Audio.shared.sfxVolume > 0
            Audio.shared.sfxVolume = on ? 0 : Audio.defaultSfx
            if on { Audio.shared.musicVolume = 0 }   // Main.java:394 — sound off forces music off
            d.set(!on, forKey: "opt.sound")
            d.set(Audio.shared.musicVolume > 0, forKey: "opt.music")
        case "act-music":
            let on = Audio.shared.musicVolume > 0
            Audio.shared.musicVolume = on ? 0 : Audio.defaultMusic
            d.set(!on, forKey: "opt.music")
        case "act-help-0": openHelp("controls.htm")                    // HUD.java:957
        case "act-help-1": openHelp("gettingstarted.htm", fragment: "playing")
        case "act-help-2": openHelp("tutorial1.htm")                   // launchTutorial()
        case let n where n.hasPrefix("act-cam-"):
            if let raw = Int(n.dropFirst(8)), let m = CameraController.Mode(rawValue: raw) {
                game?.camera.setMode(m)
            }
        default: break
        }
    }

    /// The original showed the bundled help HTML through its host browser page
    /// (Main.java:248 launchHelpPage -> HostPage.call, Main.java:219
    /// launchTutorial); the game and its help shared one window. The native
    /// equivalent is the in-window HelpViewer panel. Menu actions arrive on
    /// the render thread; AppKit work hops to main.
    private func openHelp(_ file: String, fragment: String? = nil) {
        guard let host = scnView else { return }
        DispatchQueue.main.async {
            HelpViewer.present(over: host, file: file, fragment: fragment)
        }
    }
}
