import Foundation
import SceneKit

// MARK: - Prop stats (from PROPS/<NAME>/prop.dat)

struct PropSpec {
    let name: String
    var radius: Float = 5
    var height: Float = 20
    var destructible = false
    var explosive = false
    var standable = false
    /// <WTCOLLIDEABLE> — the actor mesh joins collision mask 2 (Prop.java:534-536),
    /// the mask projectiles sweep against (Weapon.java:1031).
    var wtCollideable = false
    /// <FIRE>:x,y,z — a persistent fire smoke column at this prop-local offset
    /// (Prop.java:501-512 parse; :628-629 Particle_Object_SmokeColumn(fire)).
    var fire: SIMD3<Float>?
    /// <DEBRIS> pieces: (MODELS/DEBRIS key, local offset) — tumble on destruction
    var debris: [(String, SIMD3<Float>)] = []

    static var cache: [String: PropSpec] = [:]

    static func load(_ name: String) -> PropSpec {
        if let c = cache[name] { return c }
        var spec = PropSpec(name: name)
        if let text = Assets.text("PROPS/\(name)/prop.dat") {
            for line in text.split(whereSeparator: \.isNewline) {
                let f = line.split(separator: ":").map(String.init)
                guard f.count >= 2 else { continue }
                switch f[0] {
                case "<RADIUS>": spec.radius = Float(f[1]) ?? spec.radius
                case "<HEIGHT>": spec.height = Float(f[1]) ?? spec.height
                case "<DESTRUCTIBLE>": spec.destructible = f[1] == "YES"
                case "<EXPLOSIVE>": spec.explosive = f[1] == "YES"
                case "<STANDABLE>": spec.standable = f[1] == "YES"
                case "<WTCOLLIDEABLE>": spec.wtCollideable = f[1] == "YES"
                case "<FIRE>":
                    let v = f[1].split(separator: ",").compactMap { Float($0) }
                    if v.count == 3 { spec.fire = SIMD3(v[0], v[1], v[2]) }
                case "<DEBRIS>":
                    // <DEBRIS>:MEDIA/OBJECTS/X/debrisN.wsad:x,y,z
                    guard f.count >= 3 else { break }
                    let stem = f[1].replacingOccurrences(of: "MEDIA/OBJECTS/", with: "")
                                   .replacingOccurrences(of: ".wsad", with: "")
                                   .replacingOccurrences(of: "/", with: "_")
                    let v = f[2].split(separator: ",").compactMap { Float($0) }
                    if v.count == 3 { spec.debris.append(("DEBRIS/\(stem)", SIMD3(v[0], v[1], v[2]))) }
                default: break
                }
            }
        }
        cache[name] = spec
        return spec
    }
}

// MARK: - Runtime prop

final class Prop {
    let spec: PropSpec
    var position: SIMD3<Float>
    let node: SCNNode
    var alive = true
    var ageAtSpawn: TimeInterval  // game time when created (3 s grace vs collisions)
    /// Explosive shockwave state
    var detonating = false
    var shockScale: Float = 0
    var detonator: Cannon?

    init(spec: PropSpec, position: SIMD3<Float>, rotationDeg: Float, gameTime: TimeInterval) {
        self.spec = spec
        self.position = position
        self.ageAtSpawn = gameTime
        node = PropGeometry.node(for: spec)
        node.position = SCNVector3(position)
        // Prop.java:541 applies setOrientation(0,1,0, -Angle); the clone keeps
        // objects.dat z in file-row space (no Height - z flip — see the
        // <FIREFLY> note in World.loadObjects), and that z-mirror flips yaw
        // handedness, cancelling the sign — +rotationDeg is the correct yaw.
        node.eulerAngles.y = CGFloat(rotationDeg * .pi / 180)
    }

    func checkCollision(x: Float, y: Float, z: Float, pad: Float = 2.0) -> Bool {
        // Prop.java:304 — the proximity cylinder only exists for destructible
        // props; non-destructible ones collide via their mesh (mask 2) instead.
        guard alive, spec.destructible else { return false }
        let dx = x - position.x, dz = z - position.z
        let r = spec.radius + pad
        if dx * dx + dz * dz > r * r { return false }
        // Prop.java:308-309: horizontal distance ≤ Radius+pad AND local height
        // ≤ Height+pad — the cylinder has NO lower bound.
        return y - position.y <= spec.height + pad
    }
}

// MARK: - Texture loading + real bounding boxes

/// Loads and caches the ORIGINAL decoded prop textures staged under
/// Resources/PROPTEX/<MODEL>/. Foliage cutouts are pre-keyed RGBA PNGs.
enum PropTex {
    private static var cache: [String: NSImage?] = [:]

    /// `key` is "<MODEL>/<file>", e.g. "PALM2/frond.png".
    static func image(_ key: String) -> NSImage? {
        if let c = cache[key] { return c }
        let img = Assets.image("PROPTEX/\(key)")
        cache[key] = img
        return img
    }
}

/// Real per-model bounding-box dims [W, H, D] in world units, taken from the
/// decoded models (bbox_tex.json). Used to size procedural stand-ins to the
/// originals' true proportions. objects.dat name -> model.
enum PropDims {
    // [width(X), height(Y), depth(Z)]
    static let table: [String: SIMD3<Float>] = [
        "PALM":        [109.3, 112.0, 107.2],   // PALM2
        "FERNTREE":    [55.3, 33.3, 65.6],
        "BRUSH2":      [41.2, 27.5, 37.3],
        "TAILS":       [12.9, 32.6, 12.7],
        "TIKKI1":      [15.8, 56.6, 15.6],
        "TIKKI2":      [15.8, 56.6, 15.6],
        "TIKKI3":      [15.8, 56.6, 15.6],
        "TNT":         [10.6, 13.2, 11.2],
        "CHEST":       [15.2, 10.4, 11.1],
        "HUT":         [43.3, 40.8, 43.3],
        "TORCH":       [2.3, 33.1, 2.2],
        "TORCHBEARER": [10.0, 14.1, 9.1],
        "OBELISK":     [27.7, 88.7, 28.0],
        "LIGHTHOUSE":  [104.6, 65.3, 103.0],    // H from max.y (bbox min.y is high)
        "MOUND":       [151.8, 110.9, 200.7],
        "FIREHEAD":    [72.9, 123.4, 76.6],
        "SHIP":        [160.1, 226.7, 350.9],
        "MAST":        [160.1, 226.7, 350.9],   // uses ship's height for the mast
        "BRIDGE":      [243.9, 44.4, 315.2],
        "LIGHTBEAM":   [372.1, 107.7, 107.7],
        "MOUNDBEAM":   [372.1, 107.7, 107.7],
    ]

    static func dims(_ name: String) -> SIMD3<Float>? { table[name] }
}

// MARK: - Procedural geometry (skinned with original decoded textures)

enum PropGeometry {

    static func material(_ color: NSColor, emissive: NSColor? = nil) -> SCNMaterial {
        let m = SCNMaterial()
        m.diffuse.contents = color
        if let e = emissive { m.emission.contents = e }
        m.lightingModel = .lambert
        return m
    }

    /// A material skinned with a decoded texture. `crop` (in unit UV space,
    /// origin bottom-left) selects an atlas sub-region. `keyed` = cutout with
    /// alpha (double-sided, aOne transparency).
    static func texMaterial(_ key: String,
                            crop: (x: CGFloat, y: CGFloat, w: CGFloat, h: CGFloat)? = nil,
                            keyed: Bool = false,
                            fallback: NSColor = NSColor(calibratedWhite: 0.55, alpha: 1)) -> SCNMaterial {
        let m = SCNMaterial()
        if let img = PropTex.image(key) {
            m.diffuse.contents = img
        } else {
            m.diffuse.contents = fallback
        }
        m.diffuse.wrapS = .clamp
        m.diffuse.wrapT = .clamp
        if let c = crop {
            m.diffuse.contentsTransform = SCNMatrix4Mult(
                SCNMatrix4MakeScale(c.w, c.h, 1),
                SCNMatrix4MakeTranslation(c.x, c.y, 0))
        }
        m.lightingModel = .lambert
        if keyed {
            m.isDoubleSided = true
            m.transparencyMode = .aOne
            m.writesToDepthBuffer = true
            m.diffuse.magnificationFilter = .linear
        }
        return m
    }

    /// A vertical crossed-billboard cutout (2-3 intersecting quads) using a keyed
    /// texture. Faces are double-sided; a Y-axis billboard constraint keeps it
    /// oriented toward the camera.
    static func crossedBillboard(key: String,
                                 width: CGFloat, height: CGFloat,
                                 planes: Int = 2, yOffset: CGFloat = 0,
                                 billboard: Bool = false) -> SCNNode {
        let root = SCNNode()
        let mat = texMaterial(key, keyed: true)
        for i in 0..<planes {
            let quad = SCNPlane(width: width, height: height)
            quad.materials = [mat]
            let node = SCNNode(geometry: quad)
            node.position.y = height / 2 + yOffset
            node.eulerAngles.y = CGFloat(i) * (.pi / CGFloat(planes))
            root.addChildNode(node)
        }
        if billboard {
            let bc = SCNBillboardConstraint()
            bc.freeAxes = .Y
            root.constraints = [bc]
        }
        return root
    }

    // (the old model.json "scale" reader is gone: that number was a fabricated
    // heuristic — prop.dat HEIGHT / corrupted-legacy-OBJ height — with no
    // counterpart in the original engine, which never scales actors)

    static func node(for spec: PropSpec) -> SCNNode {
        // LIGHTBEAM needs its animated additive build (the OBJ route renders the
        // pure-black base texture as an opaque black cross).
        if spec.name == "LIGHTBEAM" {
            let n = lightBeam(dims: PropDims.dims(spec.name), height: 120)
            n.name = "prop-LIGHTBEAM"
            return n
        }
        // MOUNDBEAM (Old Gods + Ziggurat) is the other light-beam actor; it must
        // not fall through to the lit-opaque skinned path below.
        if spec.name == "MOUNDBEAM" {
            let n = moundBeam(dims: PropDims.dims(spec.name), height: 120)
            n.name = "prop-MOUNDBEAM"
            return n
        }
        // Prefer the SOLVED-pipeline export (correct UVs/materials — the 2026-07-09
        // audit showed the old heuristic OBJ route corrupted many props), then the
        // OBJ model, then the procedural stand-in.
        if let actor = SkinnedModel.load(spec.name) {
            // NATIVE scale. The original never scales prop actors: Prop.java:388
            // uses the 1-arg Media_Object_Actor ctor, whose finalscale stays -1
            // so onLoadComplete never calls setAbsoluteScale
            // (Media_Object_Actor.java:20-32); the scaled 4-arg ctor is dead
            // code game-wide. The old model.json "scale" (prop.dat HEIGHT /
            // legacy-OBJ height) was a fabricated heuristic — it shrank SHIP
            // 0.32x and blew LIGHTHOUSE up 5.69x.
            let root = SCNNode()
            root.addChildNode(actor.root)
            root.name = "prop-\(spec.name)"
            return root
        }
        if let real = ModelLibrary.node(for: spec.name) {
            real.name = "prop-\(spec.name)"
            return real
        }
        let n: SCNNode
        // Prefer the real bbox dims; fall back to prop.dat radius/height.
        let d = PropDims.dims(spec.name)
        switch spec.name {
        case "PALM": n = palm(dims: d)
        case "FERNTREE": n = fern(dims: d)
        case "TNT": n = tntBarrel(dims: d, height: spec.height, radius: spec.radius)
        case "TIKKI1", "TIKKI2", "TIKKI3": n = tiki(dims: d, height: spec.height, variant: spec.name)
        case "TORCH": n = torch(dims: d, height: spec.height)
        case "TORCHBEARER": n = torchBearer(dims: d, height: spec.height)
        case "HUT": n = hut(dims: d, radius: spec.radius, height: spec.height)
        case "OBELISK": n = obelisk(dims: d, height: spec.height, radius: spec.radius)
        case "MAST": n = mast(dims: d, height: 60)
        case "BRIDGE": n = bridge(dims: d)
        case "LIGHTHOUSE": n = lighthouse(dims: d, height: spec.height)
        case "FIREHEAD": n = firehead(dims: d, radius: spec.radius, height: spec.height)
        case "MOUND": n = mound(dims: d, radius: 14, height: 30)
        case "LIGHTBEAM": n = lightBeam(dims: d, height: 120)
        case "MOUNDBEAM": n = proceduralBeam(dims: d, height: 120)
        case "SHIP": n = shipHull(dims: d)
        default: n = genericProp(radius: spec.radius, height: spec.height)
        }
        n.name = "prop-\(spec.name)"
        return n
    }

    /// World height to render a prop at, from the game's prop.dat HEIGHT.
    /// The real bbox provides proportions; this provides scale.
    private static func aspect(_ dims: SIMD3<Float>?, height: Float) -> (w: CGFloat, h: CGFloat, d: CGFloat) {
        guard let d = dims, d.y > 0 else {
            return (CGFloat(height) * 0.6, CGFloat(height), CGFloat(height) * 0.6)
        }
        let s = CGFloat(height) / CGFloat(d.y)
        return (CGFloat(d.x) * s, CGFloat(height), CGFloat(d.z) * s)
    }

    // Trees — crossed keyed billboards using the decoded foliage cutouts.

    static func palm(dims: SIMD3<Float>?) -> SCNNode {
        // Real PALM2 is ~112 tall; render at world height 45 (prop.dat).
        let a = aspect(dims, height: 45)
        let root = SCNNode()
        // Trunk: a slim tapered cylinder wrapped with the bark texture.
        let trunkH = a.h * 0.7
        let trunkR = max(a.w * 0.03, 1.4)
        let trunkG = SCNCylinder(radius: trunkR, height: trunkH)
        // repeat the bark vertically so it reads as bark, not one stretched cell.
        let bark = texMaterial("PALM2/trunk.png",
                               fallback: NSColor(calibratedRed: 0.55, green: 0.42, blue: 0.15, alpha: 1))
        bark.diffuse.wrapT = .repeat
        bark.diffuse.contentsTransform = SCNMatrix4MakeScale(1, 4, 1)
        trunkG.materials = [bark]
        let trunk = SCNNode(geometry: trunkG)
        trunk.position.y = trunkH / 2
        trunk.eulerAngles.z = 0.05
        root.addChildNode(trunk)
        // Crown: drooping frond billboards fanning out from the top.
        let frondL = a.w * 0.55           // frond length
        let frondH = frondL * 0.5         // frond crop is ~2:1 wide
        for i in 0..<6 {
            let plane = SCNPlane(width: frondL, height: frondH)
            plane.materials = [texMaterial("PALM2/frond.png", keyed: true)]
            let fr = SCNNode(geometry: plane)
            // pivot at inner edge so fronds radiate outward from the crown
            fr.pivot = SCNMatrix4MakeTranslation(-frondL / 2, 0, 0)
            fr.position = SCNVector3(0, trunkH, 0)
            fr.eulerAngles = SCNVector3(-0.45, CGFloat(i) * (2 * .pi / 6), 0)
            root.addChildNode(fr)
        }
        return root
    }

    static func fern(dims: SIMD3<Float>?) -> SCNNode {
        // FERNTREE bbox is squat/wide; render short and bushy.
        let a = aspect(dims, height: 14)
        let root = SCNNode()
        let w = max(a.w, 12.0)
        let leaf = crossedBillboard(key: "FERNTREE/frond.png", width: w, height: a.h * 1.4, planes: 3)
        root.addChildNode(leaf)
        return root
    }

    // Barrels, tikis, torches

    static func tntBarrel(dims: SIMD3<Float>?, height: Float, radius: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let r = a.w / 2
        // barrelo.jpg: left half = side (TNT), right half = top.
        let barrel = SCNCylinder(radius: r, height: a.h)
        let side = texMaterial("TNT/barrelo.jpg", crop: (0, 0, 0.5, 1))
        let top = texMaterial("TNT/barrelo.jpg", crop: (0.5, 0, 0.5, 1))
        barrel.materials = [side, top, top]   // side, top cap, bottom cap
        let node = SCNNode(geometry: barrel)
        node.position.y = a.h / 2
        root.addChildNode(node)
        Cannon.styleCannonMaterials(root)     // unlit + opaque — no ambient-wash ghosting
        return root
    }

    static func tiki(dims: SIMD3<Float>?, height: Float, variant: String) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let tex = "TIKKI/\(variant.lowercased())o.jpg"   // TIKKI1 -> tikki1o.jpg
        // tikki1o/tikki2o/tikki3o — carving faces the front; wrap on all sides.
        let front = texMaterial(tex)
        let box = SCNBox(width: a.w, height: a.h, length: a.d, chamferRadius: a.w * 0.08)
        box.materials = [front]   // single material wraps all faces
        let body = SCNNode(geometry: box)
        body.position.y = a.h / 2
        root.addChildNode(body)
        return root
    }

    static func torch(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let h = a.h
        let pole = SCNCylinder(radius: max(a.w / 2, 0.4), height: h)
        pole.materials = [texMaterial("TORCH/torch.png")]
        let node = SCNNode(geometry: pole)
        node.position.y = h / 2
        root.addChildNode(node)
        root.addChildNode(flame(at: SCNVector3(0, h + 0.6, 0), size: 1.2))
        return root
    }

    static func torchBearer(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let box = SCNBox(width: a.w, height: a.h, length: a.d, chamferRadius: 0.4)
        box.materials = [texMaterial("TORCHBEARER/torch_bearer.jpg")]
        let body = SCNNode(geometry: box)
        body.position.y = a.h / 2
        root.addChildNode(body)
        root.addChildNode(flame(at: SCNVector3(0, a.h + 1, 0), size: 1.1))
        return root
    }

    static func flame(at pos: SCNVector3, size: CGFloat) -> SCNNode {
        let f = SCNNode(geometry: SCNSphere(radius: size))
        f.geometry?.materials = [material(NSColor.orange, emissive: NSColor.orange)]
        f.position = pos
        let light = SCNLight()
        light.type = .omni
        light.color = NSColor.orange
        light.intensity = 250
        light.attenuationEndDistance = 40
        f.light = light
        let pulse = CABasicAnimation(keyPath: "scale")
        pulse.fromValue = SCNVector3(1, 1, 1); pulse.toValue = SCNVector3(1.25, 1.5, 1.25)
        pulse.duration = 0.35; pulse.autoreverses = true; pulse.repeatCount = .infinity
        f.addAnimation(pulse, forKey: "flicker")
        return f
    }

    /// A firefly: the FIREFLY sprite as a small glowing billboard (night maps
    /// place these via <FIREFLY> entries). Ported from
    /// Particle_Object_Firefly.java: a wandering 10-30-unit orbit arm with turn
    /// rates clamped to ±1°, a blink cycle (dark 1-3 s, lit 1-7 s), a size
    /// pulse Scale + SinTable[SinVal], and ±10-unit anchor jitter.
    static func firefly() -> SCNNode {
        let plane = SCNPlane(width: 1, height: 1)   // unit quad; setBitmapSize drives node scale
        let m = SCNMaterial()
        m.diffuse.contents = PropTex.image("firefly.png") ?? NSColor.yellow
        m.emission.contents = m.diffuse.contents
        m.lightingModel = .constant
        m.blendMode = .add                 // glow
        m.writesToDepthBuffer = false
        plane.materials = [m]
        let n = SCNNode(geometry: plane)
        n.constraints = [SCNBillboardConstraint()]
        // Particle_Object_Firefly.java ctor state: size 2-3, one of six SinTable
        // pulse phases, ±10-unit anchor jitter, a 10-30-unit orbit arm, 0-5 s
        // until the first blink. The anchor jitter is applied lazily on the
        // first tick (World positions the node after this builder returns).
        var anchor = SIMD3<Float>.zero
        var arm = SIMD3<Float>.zero
        var placed = false
        let scale = 2 + Float.random(in: 0..<1)
        // setBitmapSize(Scale + SinTable[SinVal]); SinTable rates for index 0-5
        // transcribed from Main.java:307-324 (updateSinTable)
        let sinRates: [Float] = [2.0, -2.75, 3.5, 1.0, 1.5, -2.5]
        let sinRate = sinRates[Int.random(in: 0..<6)]
        var angle: Float = 0, angle1: Float = 0
        var left = Bool.random(), up = Bool.random()
        var hidTimer = Float.random(in: 0..<5)
        // (VEC3D.rotateY/rotateX axis-sign convention is unobservable here: it
        // only mirrors a random walk, so the standard right-handed form is used)
        func rotY(_ v: SIMD3<Float>, _ a: Float) -> SIMD3<Float> {
            SIMD3(v.x * cos(a) + v.z * sin(a), v.y, -v.x * sin(a) + v.z * cos(a))
        }
        func rotX(_ v: SIMD3<Float>, _ a: Float) -> SIMD3<Float> {
            SIMD3(v.x, v.y * cos(a) - v.z * sin(a), v.y * sin(a) + v.z * cos(a))
        }
        var last: CGFloat = 0
        n.runAction(.repeatForever(.customAction(duration: 1e9) { node, elapsed in
            let dt = Float(elapsed - last)
            last = elapsed
            if !placed {
                placed = true
                anchor = node.simdPosition + SIMD3<Float>.random(in: -10...10)
                let dir = simd_normalize(SIMD3<Float>.random(in: -0.5...0.5))
                arm = dir * (10 + Float.random(in: 0..<1) * 20)
            }
            // wandering turn rates, clamped to ±1°; the 10% direction flips and
            // the per-frame arm rotation follow the original frame cadence
            // (Particle_Object_Firefly.java updateTimeSlice)
            angle = max(-1, min(1, angle + (left ? 1 : -1) * dt))
            angle1 = max(-1, min(1, angle1 + (up ? 1 : -1) * dt))
            if Float.random(in: 0..<1) < 0.1 { left.toggle() }
            if Float.random(in: 0..<1) < 0.1 { up.toggle() }
            arm = rotY(arm, angle * .pi / 180)
            arm = rotX(arm, angle1 * .pi / 180)
            // blink: HidTimer counts down; dark 1-3 s, lit 1-7 s
            hidTimer -= dt
            if !node.isHidden && hidTimer <= 0 {
                node.isHidden = true
                hidTimer = 1 + Float.random(in: 0..<2)
            } else if node.isHidden && hidTimer <= 0 {
                node.isHidden = false
                hidTimer = 1 + Float.random(in: 0..<6)
            }
            if !node.isHidden {
                node.simdPosition = anchor + SIMD3(arm.x, arm.y * 0.2, arm.z)
                let size = scale + sin(sinRate * Float(elapsed))
                node.simdScale = SIMD3(repeating: size)
            }
        }))
        return n
    }

    // Buildings / stone

    static func hut(dims: SIMD3<Float>?, radius: Float, height: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let wallH = a.h * 0.55
        // hut.jpg atlas: right-side panels = wall thatch; octagon = roof.
        let wall = SCNCylinder(radius: a.w / 2, height: wallH)
        wall.radialSegmentCount = 8
        wall.materials = [texMaterial("HUT/hut.jpg", crop: (0.55, 0.0, 0.45, 0.5))]
        let wallN = SCNNode(geometry: wall)
        wallN.position.y = wallH / 2
        root.addChildNode(wallN)
        let roof = SCNCone(topRadius: 0, bottomRadius: a.w * 0.62, height: a.h * 0.5)
        roof.radialSegmentCount = 8
        roof.materials = [texMaterial("HUT/hut.jpg", crop: (0.0, 0.0, 0.5, 0.5))]  // octagon roof region
        let roofN = SCNNode(geometry: roof)
        roofN.position.y = wallH + a.h * 0.25
        root.addChildNode(roofN)
        return root
    }

    static func obelisk(dims: SIMD3<Float>?, height: Float, radius: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        // obelisk.jpg is a tall carved slab; use a slightly tapered box.
        let box = SCNBox(width: a.w, height: a.h, length: a.d, chamferRadius: a.w * 0.05)
        box.materials = [texMaterial("OBELISK/obelisk.jpg")]
        let shaft = SCNNode(geometry: box)
        shaft.position.y = a.h / 2
        root.addChildNode(shaft)
        // small capstone pyramid
        let cap = SCNPyramid(width: a.w, height: a.h * 0.14, length: a.d)
        cap.materials = [texMaterial("OBELISK/obelisk.jpg", crop: (0, 0.85, 1, 0.15))]
        let capN = SCNNode(geometry: cap)
        capN.position.y = a.h
        root.addChildNode(capN)
        return root
    }

    static func mast(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        let root = SCNNode()
        let h = CGFloat(height)
        let pole = SCNCylinder(radius: 1.0, height: h)
        pole.materials = [texMaterial("SHIP/boato.jpg", crop: (0.86, 0.1, 0.08, 0.7),
                                      fallback: NSColor(calibratedRed: 0.42, green: 0.3, blue: 0.16, alpha: 1))]
        let poleN = SCNNode(geometry: pole)
        poleN.position.y = h / 2
        root.addChildNode(poleN)
        let sailG = SCNPlane(width: 22, height: h * 0.5)
        sailG.materials = [texMaterial("SHIP/boato.jpg", crop: (0.68, 0.35, 0.16, 0.35))]
        sailG.firstMaterial?.isDoubleSided = true
        let sail = SCNNode(geometry: sailG)
        sail.position = SCNVector3(0, h * 0.6, 2)
        root.addChildNode(sail)
        return root
    }

    static func bridge(dims: SIMD3<Float>?) -> SCNNode {
        let root = SCNNode()
        let deckG = SCNBox(width: 16, height: 2.5, length: 44, chamferRadius: 0.4)
        deckG.materials = [texMaterial("BRIDGE/bridge.jpg", crop: (0, 0.55, 1, 0.45))]  // stone brick region
        let deck = SCNNode(geometry: deckG)
        deck.position.y = 20
        root.addChildNode(deck)
        for dz: CGFloat in [-16, 16] {
            let pillarG = SCNCylinder(radius: 3.2, height: 20)
            pillarG.materials = [texMaterial("BRIDGE/bridge.jpg", crop: (0, 0.0, 1, 0.5))]
            let pillar = SCNNode(geometry: pillarG)
            pillar.position = SCNVector3(0, 10, dz)
            root.addChildNode(pillar)
        }
        return root
    }

    static func lighthouse(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        let root = SCNNode()
        let h = CGFloat(height)
        let towerG = SCNCone(topRadius: 3, bottomRadius: 5.5, height: h * 0.85)
        towerG.materials = [texMaterial("LIGHTHOUSE/lighthouseo.jpg")]
        let towerBody = SCNNode(geometry: towerG)
        towerBody.position.y = h * 0.425
        root.addChildNode(towerBody)
        let lamp = SCNNode(geometry: SCNCylinder(radius: 2.6, height: h * 0.12))
        lamp.geometry?.materials = [material(NSColor.yellow, emissive: .yellow)]
        lamp.position.y = h * 0.91
        root.addChildNode(lamp)
        let capG = SCNCone(topRadius: 0, bottomRadius: 3.2, height: h * 0.1)
        capG.materials = [texMaterial("LIGHTHOUSE/lighthouseo.jpg", crop: (0, 0.85, 1, 0.15))]
        let cap = SCNNode(geometry: capG)
        cap.position.y = h
        root.addChildNode(cap)
        return root
    }

    static func firehead(dims: SIMD3<Float>?, radius: Float, height: Float) -> SCNNode {
        let a = aspect(dims, height: height)
        let root = SCNNode()
        let h = a.h
        // firehovelo.jpg: the face (eyes + open mouth) is the lower-right region.
        let headG = SCNBox(width: a.w, height: h, length: a.d, chamferRadius: 3)
        let faceMat = texMaterial("FIREHEAD/firehovelo.jpg", crop: (0.35, 0.0, 0.65, 0.6))
        let sideMat = texMaterial("FIREHEAD/firehovelo.jpg", crop: (0.0, 0.0, 0.5, 0.6))
        // box materials: [front, right, back, left, top, bottom]
        headG.materials = [faceMat, sideMat, sideMat, sideMat, sideMat, sideMat]
        let head = SCNNode(geometry: headG)
        head.position.y = h / 2
        root.addChildNode(head)
        root.addChildNode(flame(at: SCNVector3(0, h * 0.25, CGFloat(a.d) * 0.5), size: 2.5))
        return root
    }

    static func mound(dims: SIMD3<Float>?, radius: Float, height: Float) -> SCNNode {
        // MOUND bbox is large (~152x111x201). Render as a broad textured dome.
        let a = aspect(dims, height: 40)
        let root = SCNNode()
        let coneG = SCNCone(topRadius: a.w * 0.15, bottomRadius: a.w / 2, height: a.h)
        coneG.materials = [texMaterial("MOUND/moundo.jpg", fallback: NSColor(calibratedWhite: 0.45, alpha: 1))]
        let cone = SCNNode(geometry: coneG)
        cone.position.y = a.h / 2
        root.addChildNode(cone)
        return root
    }

    /// Additive light-beam material pass, shared by LIGHTBEAM and MOUNDBEAM.
    /// The render style comes from the actors' own material records (the gMat
    /// chunk inside each .wsad): LIGHTBEAM's material carries style 2, and its
    /// correct visual outcome is beyond doubt (a lighthouse beam is light, not
    /// structure) — solving style 2 = additive. beam.wsad (MOUNDBEAM) carries
    /// the same style 2 plus emissive 255,255,255, where every opaque structure
    /// actor checked (mound.wsad, PALM2, TORCH) carries style 0 and emissive 0.
    private static func applyBeamLightMaterials(_ node: SCNNode) {
        node.enumerateHierarchy { n, _ in
            guard let geo = n.geometry else { return }
            for m in geo.materials {
                m.lightingModel = .constant
                m.emission.contents = m.diffuse.contents
                m.blendMode = .add                 // beam of light: additive
                m.isDoubleSided = true
                m.writesToDepthBuffer = false
                m.transparency = 0.55
            }
        }
    }

    static func lightBeam(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        // The real LIGHTBEAM actor: a 194-unit textured beam plane playing its
        // original decoded "loop" motion — a continuous 13.3s sweep around the
        // vertical (from resources/loop.wsmo, 400 uniform quat keys).
        if let actor = SkinnedModel.load("LIGHTBEAM"),
           let motion = SkinnedModel.loadMotion("MODELS/LIGHTBEAM/loop_motion_full.json") {
            applyBeamLightMaterials(actor.root)
            // native geom scale — the original never scales actors (Prop.java:388)
            SkinnedModel.animate(actor, motion: motion, phase: Double.random(in: 0..<motion.duration))
            let root = SCNNode()
            root.addChildNode(actor.root)
            return root
        }
        return proceduralBeam(dims: dims, height: height)
    }

    static func moundBeam(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        // The real MOUNDBEAM actor (MEDIA/OBJECTS/MOUND/beam.wsad, per
        // PROPS/MOUNDBEAM/prop.dat): a static beam mesh UV-mapped to the green
        // glow strip of the mound atlas (moundo.jpg). Unlike LIGHTBEAM it ships
        // no motion resource, so it does not animate.
        if let actor = SkinnedModel.load("MOUNDBEAM") {
            applyBeamLightMaterials(actor.root)
            let root = SCNNode()
            root.addChildNode(actor.root)
            return root
        }
        return proceduralBeam(dims: dims, height: height)
    }

    static func proceduralBeam(dims: SIMD3<Float>?, height: Float) -> SCNNode {
        let beam = SCNNode(geometry: SCNCylinder(radius: 2.2, height: CGFloat(height)))
        let m = material(NSColor(calibratedRed: 0.7, green: 0.9, blue: 1, alpha: 1), emissive: NSColor(calibratedRed: 0.6, green: 0.85, blue: 1, alpha: 1))
        if let img = PropTex.image("LIGHTBEAM/lightbeamo.jpg") {
            m.emission.contents = img
            m.diffuse.contents = img
        }
        m.transparency = 0.35
        m.writesToDepthBuffer = false
        beam.geometry?.materials = [m]
        beam.position.y = CGFloat(height) / 2
        return beam
    }

    static func shipHull(dims: SIMD3<Float>?) -> SCNNode {
        let root = SCNNode()
        // boato.jpg is a full ship atlas; map the hull-side region to the hull box.
        let hullG = SCNBox(width: 26, height: 14, length: 70, chamferRadius: 3)
        let hullMat = texMaterial("SHIP/boato.jpg", crop: (0.0, 0.0, 0.45, 0.4))
        hullG.materials = [hullMat]
        let hull = SCNNode(geometry: hullG)
        hull.position.y = 7
        root.addChildNode(hull)
        let deckRailG = SCNBox(width: 28, height: 2, length: 72, chamferRadius: 1)
        deckRailG.materials = [texMaterial("SHIP/boato.jpg", crop: (0.4, 0.85, 0.3, 0.12))]
        let deckRail = SCNNode(geometry: deckRailG)
        deckRail.position.y = 14
        root.addChildNode(deckRail)
        return root
    }

    static func genericProp(radius: Float, height: Float) -> SCNNode {
        let n = SCNNode(geometry: SCNCylinder(radius: CGFloat(radius) * 0.7, height: CGFloat(height)))
        n.geometry?.materials = [material(NSColor(calibratedWhite: 0.5, alpha: 1))]
        n.position.y = CGFloat(height) / 2
        let root = SCNNode()
        root.addChildNode(n)
        return root
    }

    // Decorations

    /// <DECORATION>/<DECORATIONWATER> actors. Particle_Object_Decoration.java:24:
    /// a path ending ".wsad" loads that actor file, a bare directory loads its
    /// actor.wsad. The originals: OBJECTS/BRUSH2 = the purple fern bush,
    /// OBJECTS/PALM2/brush1.wsad = the GREEN bush (palm-atlas leaves),
    /// OBJECTS/TAILS/tail1-3.wsad = cattails. All decode through the solved
    /// pipeline; no tinting anywhere in the Java — color is all texture.
    static func decoration(path: String, scale: (Float, Float)) -> SCNNode {
        let root = SCNNode()
        let stem = ((path as NSString).lastPathComponent as NSString)
            .deletingPathExtension.uppercased()
        let model: String
        switch stem {
        case "BRUSH1": model = "BRUSH1"
        case "TAIL1": model = "TAILS"
        case "TAIL2": model = "TAILS2"
        case "TAIL3": model = "TAILS3"
        default: model = stem                       // bare dir, e.g. BRUSH2
        }
        if let actor = SkinnedModel.load(model) {
            root.addChildNode(actor.root)
            // Particle_Object_Decoration.java:41-44: a decoration whose actor
            // has a motion plays "loop" at a random 0-8 s phase (cattail sway).
            // No decoration loop motion is exported yet, so this arms only once
            // MODELS/<model>/loop_motion_full.json appears.
            if let motion = SkinnedModel.loadMotion("MODELS/\(model)/loop_motion_full.json") {
                SkinnedModel.animate(actor, motion: motion, phase: Double.random(in: 0..<8))
            }
        } else if path.lowercased().contains("tail") {
            // fallback stand-ins if an export is missing
            root.addChildNode(crossedBillboard(key: "TAILS/cattails.png", width: 16, height: 20, planes: 2))
        } else {
            root.addChildNode(crossedBillboard(key: "BRUSH2/brush.png", width: 18, height: 16, planes: 3))
        }
        // Particle_Object_Decoration.java:26: setAbsoluteScale(sx, sy, sx) —
        // first file float scales X and Z, second scales Y
        root.scale = SCNVector3(CGFloat(scale.0), CGFloat(scale.1), CGFloat(scale.0))
        return root
    }

    static func cloud(size: Float, opacity: Float) -> SCNNode {
        let root = SCNNode()
        let m = material(.white)
        m.transparency = CGFloat(max(0.25, min(opacity, 0.9)))
        m.lightingModel = .constant
        var seed: UInt64 = 12345
        func rnd() -> CGFloat { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return CGFloat(Double(seed >> 33) / Double(UInt32.max)) }
        for _ in 0..<5 {
            let puff = SCNNode(geometry: SCNSphere(radius: CGFloat(size) * (0.25 + rnd() * 0.3)))
            puff.geometry?.materials = [m]
            puff.position = SCNVector3((rnd() - 0.5) * CGFloat(size) * 1.6, (rnd() - 0.5) * CGFloat(size) * 0.3, (rnd() - 0.5) * CGFloat(size) * 0.8)
            puff.scale = SCNVector3(1, 0.45, 1)
            root.addChildNode(puff)
        }
        return root
    }
}
