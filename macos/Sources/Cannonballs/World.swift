import Foundation
import SceneKit

/// A fully assembled island world: scene graph + terrain + props.
final class World {
    let map: MapInfo
    let terrain: Terrain
    let scene: SCNScene
    var props: [Prop] = []
    private var decorations: [SCNNode] = []   // terrain-following (Particle_Object_Decoration)
    var skyActor: SCNNode?                    // original sky dome + horizon islands
    let propsRoot = SCNNode()
    let effectsRoot = SCNNode()   // splashes, smoke, etc.
    let waterColor: NSColor

    init(map: MapInfo) {
        self.map = map
        terrain = Terrain(map: map)
        scene = SCNScene()
        waterColor = NSColor(rgb: map.ambientRGB)

        scene.rootNode.addChildNode(terrain.node)
        propsRoot.name = "props"
        scene.rootNode.addChildNode(propsRoot)
        scene.rootNode.addChildNode(effectsRoot)

        buildWater()
        buildSkyAndLights()
        loadObjects()
        WorldDressing.addShoreline(to: self)
        WorldDressing.addCloudShadows(to: terrain.material)
    }

    var center: SIMD3<Float> {
        let half = terrain.worldSize / 2
        return SIMD3(half, 0, half)
    }

    // MARK: - Water

    private func buildWater() {
        let plane = SCNPlane(width: 4000, height: 4000)
        let m = SCNMaterial()
        // Original: tiled WATER texture, tinted by the map's water color, the
        // 32-frame WATERANIMATION cycle, and the UV offsets scrolling both axes
        // (Island.java:313-331).
        if let waterTex = PropTex.image("water.png") ?? Assets.image("PROPTEX/water.png") {
            m.diffuse.contents = waterTex
            m.diffuse.wrapS = .repeat
            m.diffuse.wrapT = .repeat
            m.diffuse.contentsTransform = SCNMatrix4MakeScale(70, 70, 1)    // original coord scale 70
            m.multiply.contents = waterColor                                  // full map tint (no lightening)
            // UV scroll on BOTH axes (Island.java:327-331: WaterOffsetX -= dt *
            // 0.1, WaterOffsetZ -= dt * 0.088) — linear repeating 0→-1 sweeps
            // at those per-second rates.
            let scrollX = CABasicAnimation(keyPath: "contentsTransform.translation.x")
            scrollX.fromValue = 0; scrollX.toValue = -1.0
            scrollX.duration = 1.0 / 0.1; scrollX.repeatCount = .infinity
            m.diffuse.addAnimation(scrollX, forKey: "waterscrollx")
            let scrollY = CABasicAnimation(keyPath: "contentsTransform.translation.y")
            scrollY.fromValue = 0; scrollY.toValue = -1.0
            scrollY.duration = 1.0 / 0.088; scrollY.repeatCount = .infinity
            m.diffuse.addAnimation(scrollY, forKey: "waterscrolly")
            // the ORIGINAL animated water: cycle the 32 WATERANIMATION tiles @0.08s
            WorldDressing.animateWater(m)
        } else {
            m.diffuse.contents = waterColor
        }
        m.transparency = 0.9
        m.lightingModel = .constant   // original: WaterAnimation.setFullbright()
        plane.materials = [m]
        let water = SCNNode(geometry: plane)
        water.eulerAngles.x = -.pi / 2
        water.position = SCNVector3(CGFloat(center.x), 0.15, CGFloat(center.z))
        water.name = "water"
        // subtle bob
        let bob = CABasicAnimation(keyPath: "position.y")
        bob.fromValue = 0.05; bob.toValue = 0.55
        bob.duration = 2.6; bob.autoreverses = true; bob.repeatCount = .infinity
        bob.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
        water.addAnimation(bob, forKey: "bob")
        scene.rootNode.addChildNode(water)
        // opaque deep-water disk far below so ocean isn't see-through to sky
        let deep = SCNNode(geometry: SCNPlane(width: 4000, height: 4000))
        let dm = SCNMaterial()
        dm.diffuse.contents = waterColor.blended(withFraction: 0.55, of: .black) ?? waterColor
        dm.lightingModel = .constant
        deep.geometry?.materials = [dm]
        deep.eulerAngles.x = -.pi / 2
        deep.position = SCNVector3(CGFloat(center.x), -3.5, CGFloat(center.z))
        scene.rootNode.addChildNode(deep)
    }

    // MARK: - Sky, fog, lights

    private func buildSkyAndLights() {
        let horizon = NSColor(rgb: map.ambientRGB)
        let zenith: NSColor
        if map.skyName == "NIGHT" {
            zenith = NSColor(calibratedRed: 0.05, green: 0.04, blue: 0.18, alpha: 1)
        } else if map.skyName == "PURPLE" {
            zenith = NSColor(calibratedRed: 0.45, green: 0.25, blue: 0.7, alpha: 1)
        } else if map.skyName == "DESERT" {
            zenith = NSColor(calibratedRed: 0.55, green: 0.65, blue: 0.95, alpha: 1)
        } else {
            zenith = NSColor(calibratedRed: 0.35, green: 0.55, blue: 0.95, alpha: 1)
        }
        scene.background.contents = World.gradientImage(top: zenith, bottom: horizon.blended(withFraction: 0.35, of: .white) ?? horizon)

        // Real sky: the decoded panorama as the wrap-around background, PLUS the
        // original SKIES actor geometry (dome + horizon-island billboards + moon/
        // stars) following the camera. (Software rasterizer keeps its gradient sky.)
        if let skyImg = Assets.image("SKIES/\(map.skyName).png") {
            scene.background.contents = skyImg
        }
        if let sky = SkyActor.load(map.skyName) {
            sky.position = SCNVector3(CGFloat(center.x), 0, CGFloat(center.z))
            scene.rootNode.addChildNode(sky)
            skyActor = sky
        }

        scene.fogColor = horizon.blended(withFraction: 0.3, of: .white) ?? horizon
        scene.fogStartDistance = 420
        scene.fogEndDistance = 1600
        scene.fogDensityExponent = 1.5

        let sun = SCNLight()
        sun.type = .directional
        sun.color = NSColor(rgb: map.sunRGB)
        sun.intensity = map.skyName == "NIGHT" ? 620 : 950
        // the 2002 engine had NO shadow mapping: gameplay objects carry SHADOW
        // blob patches instead (FXSprites.blobShadow)
        sun.castsShadow = false
        let sunNode = SCNNode()
        sunNode.light = sun
        let dir = simd_normalize(map.sunVector)
        sunNode.position = SCNVector3(center + dir * 400)
        sunNode.look(at: SCNVector3(center))
        scene.rootNode.addChildNode(sunNode)

        let ambient = SCNLight()
        ambient.type = .ambient
        ambient.color = NSColor(rgb: map.ambientRGB).blended(withFraction: 0.5, of: .white)
        ambient.intensity = map.skyName == "NIGHT" ? 380 : 520
        let ambientNode = SCNNode()
        ambientNode.light = ambient
        scene.rootNode.addChildNode(ambientNode)

        // No procedural sun/moon balls: the original's ONLY celestial visuals
        // are the sky actor's authored parts (the NIGHT actor's `moon`
        // billboard, Island.java:758/1208-1213) and, on HasSun maps, the
        // Entity_Object_LensFlare (Island.java:1214-1215). The old world-space
        // moon sphere parallaxed across the camera-following horizon-island
        // billboards and painted over them.
    }

    static func gradientImage(top: NSColor, bottom: NSColor) -> NSImage {
        let img = NSImage(size: NSSize(width: 8, height: 256))
        img.lockFocus()
        NSGradient(starting: bottom, ending: top)?.draw(in: NSRect(x: 0, y: 0, width: 8, height: 256), angle: 90)
        img.unlockFocus()
        return img
    }

    // MARK: - objects.dat

    private func loadObjects() {
        guard let text = Assets.text(map.objectsPath) else { return }
        let vs = Terrain.vertexScale
        for line in text.split(whereSeparator: \.isNewline) {
            let f = line.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
            guard let tag = f.first else { continue }
            func floats(_ s: String) -> [Float] { s.split(separator: ",").compactMap { Float($0) } }
            switch tag {
            case "<PROP>":
                // <PROP>:MEDIA/PROPS/N:x,z:rotDeg
                guard f.count >= 4, let c = optPair(floats(f[2])) else { continue }
                let name = (f[1] as NSString).lastPathComponent
                let spec = PropSpec.load(name)
                let wx = c.0 * vs, wz = c.1 * vs
                let wy = terrain.height(x: wx, z: wz)
                addProp(Prop(spec: spec, position: SIMD3(wx, wy, wz), rotationDeg: Float(f[3]) ?? 0, gameTime: 0))
            case "<PROPPOS>":
                // <PROPPOS>:MEDIA/PROPS/N:x,y,z:rot — explicit height
                guard f.count >= 4 else { continue }
                let v = floats(f[2]); guard v.count == 3 else { continue }
                let name = (f[1] as NSString).lastPathComponent
                let spec = PropSpec.load(name)
                addProp(Prop(spec: spec, position: SIMD3(v[0] * vs, v[1], v[2] * vs), rotationDeg: Float(f[3]) ?? 0, gameTime: 0))
            case "<DECORATION>", "<DECORATIONWATER>":
                // :path:x,z:rotDeg:sx,sy
                guard f.count >= 5, let c = optPair(floats(f[2])) else { continue }
                let wx = c.0 * vs, wz = c.1 * vs
                let scale = optPair(floats(f[4])) ?? (1, 1)
                let n = PropGeometry.decoration(path: f[1], scale: scale)
                // water decorations pin Y = 0.0 (Particle_Object_Decoration.java:50)
                let y: Float = tag == "<DECORATIONWATER>" ? 0.0 : terrain.height(x: wx, z: wz)
                n.position = SCNVector3(CGFloat(wx), CGFloat(y), CGFloat(wz))
                n.eulerAngles.y = CGFloat((Float(f[3]) ?? 0) * .pi / 180)
                propsRoot.addChildNode(n)
                if tag == "<DECORATION>" { decorations.append(n) }
            case "<CLOUD>":
                // :layer:x,h,z:radius,xMul,yMul (Particle_Object_Cloud ctor args)
                guard f.count >= 4 else { continue }
                let v = floats(f[2]); guard v.count == 3 else { continue }
                let params = floats(f[3])
                FXSprites.cloud(at: SIMD3(v[0] * vs, v[1], v[2] * vs),
                                radius: params.first ?? 40,
                                xMul: params.count > 1 ? params[1] : 1,
                                yMul: params.count > 2 ? params[2] : 1,
                                in: self)
            case "<FIREFLY>":
                // <FIREFLY>:x,y,z — a drifting glowing mote (night maps).
                // NO z flip: objects.dat coordinates are heightmap-ROW space
                // for every tag. Island.java:882-927 flips them all
                // (Height - z) into world space, and getTerrainHeight flips
                // world z back (Island.java: f2 = Width*VertexScale - f2), so
                // the flips cancel; the clone keeps terrain AND objects in
                // file-row space directly. The old (grid - z) here was a
                // double flip that mirrored fireflies across the map.
                let v = floats(f[1]); guard v.count == 3 else { continue }
                let n = PropGeometry.firefly()
                n.position = SCNVector3(CGFloat(v[0] * vs),
                                        CGFloat(v[1]),
                                        CGFloat(v[2] * vs))
                scene.rootNode.addChildNode(n)
            // NOTE: VOLCANO/objects.dat contains <FIRE> lines, but the
            // original's objects.dat parser has no <FIRE> handler
            // (Island.java:879-927; only Prop.java:501 reads <FIRE>, from
            // prop.dat) — those lines are dead data the 2002 game ignored,
            // so the clone ignores them too.
            default:
                break
            }
        }
    }

    private func optPair(_ v: [Float]) -> (Float, Float)? {
        v.count >= 2 ? (v[0], v[1]) : nil
    }

    /// Decorations follow the terrain like Particle_Object_Decoration: re-ground
    /// after deformation; sinking below the water removes them in a smoke puff.
    func regroundDecorations() {
        decorations.removeAll { n in
            let x = Float(n.position.x), z = Float(n.position.z)
            let y = terrain.height(x: x, z: z)
            if y <= 0 {
                FXSprites.smoke(at: SIMD3(x, y, z), trajectory: SIMD3.random(in: -0.5...0.5),
                                scale: .random(in: 0.3...0.8), in: self)
                n.removeFromParentNode()
                return true
            }
            n.position.y = CGFloat(y)
            return false
        }
    }

    func addProp(_ prop: Prop) {
        props.append(prop)
        propsRoot.addChildNode(prop.node)
        // prop.dat <FIRE>: a persistent fire smoke column at the prop-local
        // offset (Prop.java:628-629, SmokeColumn(1, ..., true, 999)); dies with
        // the prop (Prop.java:242-243)
        if let f = prop.spec.fire {
            let firePos = prop.node.simdConvertPosition(f, to: nil)
            FXSprites.smokeColumn(at: firePos, duration: 999, fire: true,
                                  alive: { [weak prop] in prop?.alive ?? false },
                                  in: self)
        }
    }

    func destroyProp(_ prop: Prop) {
        prop.alive = false
        prop.node.removeFromParentNode()
    }

    /// First live prop whose collision cylinder contains the point (older than 3 s of game time).
    func propHit(x: Float, y: Float, z: Float, gameTime: TimeInterval, pad: Float = 2.0) -> Prop? {
        props.first { $0.alive && gameTime - $0.ageAtSpawn > 3 && $0.checkCollision(x: x, y: y, z: z, pad: pad) }
    }

    /// Anything solid above this XZ position (blocks Tower / Molehill)?
    func objectAbove(x: Float, z: Float) -> Bool {
        props.contains { p in
            guard p.alive else { return false }
            let dx = x - p.position.x, dz = z - p.position.z
            return dx * dx + dz * dz < p.spec.radius * p.spec.radius
        }
    }

    /// Weapon.java:1029-1031 — the swept segment test from the shot's last
    /// position to its current one against the 3D meshes of WTCOLLIDEABLE
    /// props (Prop.java:534-536 setCollisionMask(2)). This — not the
    /// destructible-prop cylinder — is what blocked shots on the Nightbridge
    /// deck, the ship hull, etc.
    func collideSegment(from a: SIMD3<Float>, to b: SIMD3<Float>) -> Prop? {
        for p in props where p.alive && p.spec.wtCollideable {
            // hitTestWithSegment takes points in the receiving node's space
            let la = p.node.simdConvertPosition(a, from: nil)
            let lb = p.node.simdConvertPosition(b, from: nil)
            let hits = p.node.hitTestWithSegment(
                from: SCNVector3(la), to: SCNVector3(lb),
                options: [SCNHitTestOption.backFaceCulling.rawValue: false])
            if !hits.isEmpty { return p }
        }
        return nil
    }

    /// Weapon.java:165-171 (also 590-591, 828-830) — the mask-8 probe: a ray
    /// from (x, y+400, z) straight down to (x, y-drop, z) against STANDABLE
    /// prop meshes (Prop.java:538-540 setCollisionMask(10)). Yields the deck
    /// surface height + impact normal under the point.
    func standableSurface(x: Float, y: Float, z: Float, drop: Float) -> (height: Float, normal: SIMD3<Float>)? {
        let a = SIMD3(x, y + 400.0, z), b = SIMD3(x, y - drop, z)
        var best: (height: Float, normal: SIMD3<Float>)?
        for p in props where p.alive && p.spec.standable {
            let la = p.node.simdConvertPosition(a, from: nil)
            let lb = p.node.simdConvertPosition(b, from: nil)
            guard let hit = p.node.hitTestWithSegment(
                from: SCNVector3(la), to: SCNVector3(lb),
                options: [SCNHitTestOption.backFaceCulling.rawValue: false]).first
            else { continue }
            // node-level hit tests report coordinates in the hit child's
            // space; convert to world explicitly
            let wp = hit.node.simdConvertPosition(SIMD3<Float>(hit.localCoordinates), to: nil)
            var wn = hit.node.simdConvertVector(SIMD3<Float>(hit.localNormal), to: nil)
            if simd_length(wn) > 0.0001 { wn = simd_normalize(wn) }
            if best == nil || wp.y > best!.height {
                best = (wp.y, wn)
            }
        }
        return best
    }
}
