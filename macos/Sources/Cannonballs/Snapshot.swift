import Foundation
import SceneKit
import AppKit

enum Snapshot {
    /// Render a skinned actor posed at motion time t (self-verification for the
    /// decoded skeleton/skin/motion pipeline).
    static func renderSkinned(name: String, time: Double, to path: String,
                              size: CGSize = CGSize(width: 640, height: 640)) {
        guard let actor = SkinnedModel.load(name) ?? SkinnedModel.load(name, file: "barrel_skinned.json") else {
            FileHandle.standardError.write("no skinned model \(name)\n".data(using: .utf8)!); exit(1)
        }
        if let motion = SkinnedModel.loadMotion("MODELS/\(name)/loop_motion_full.json")
                     ?? SkinnedModel.loadMotion("MODELS/\(name)/fire_motion.json") {
            SkinnedModel.pose(actor, motion: motion, at: time)
        }
        let scene = SCNScene()
        scene.background.contents = NSColor(calibratedWhite: 0.35, alpha: 1)
        let model = actor.root
        let (minB, maxB) = model.boundingBox
        let span = max(maxB.x - minB.x, max(maxB.y - minB.y, maxB.z - minB.z))
        scene.rootNode.addChildNode(model)

        let camNode = SCNNode(); let cam = SCNCamera()
        cam.zFar = 4000; cam.fieldOfView = 45; camNode.camera = cam
        let d = CGFloat(span) * 1.9
        camNode.position = SCNVector3(d * 0.7, d * 0.5, d * 0.9)
        camNode.look(at: SCNVector3(0, CGFloat(minB.y + maxB.y) / 2, 0))
        scene.rootNode.addChildNode(camNode)
        let key = SCNNode(); key.light = SCNLight(); key.light!.type = .directional
        key.light!.intensity = 1100; key.eulerAngles = SCNVector3(-0.9, 0.6, 0)
        scene.rootNode.addChildNode(key)
        let amb = SCNNode(); amb.light = SCNLight(); amb.light!.type = .ambient
        amb.light!.intensity = 500; scene.rootNode.addChildNode(amb)

        let renderer = SCNRenderer(device: MTLCreateSystemDefaultDevice(), options: nil)
        renderer.scene = scene; renderer.pointOfView = camNode
        let image = renderer.snapshot(atTime: 0, with: size, antialiasingMode: .multisampling4X)
        if let tiff = image.tiffRepresentation, let rep = NSBitmapImageRep(data: tiff),
           let png = rep.representation(using: .png, properties: [:]) {
            try? png.write(to: URL(fileURLWithPath: path))
            print("wrote \(path) (skinned \(name) @ t=\(time))")
        }
    }

    /// Render a single model (from Resources/MODELS/<NAME>) on a neutral backdrop.
    static func renderModel(name: String, to path: String, size: CGSize = CGSize(width: 640, height: 640)) {
        let scene = SCNScene()
        scene.background.contents = NSColor(calibratedWhite: 0.35, alpha: 1)
        guard let model = ModelLibrary.node(for: name) else {
            FileHandle.standardError.write("no model \(name)\n".data(using: .utf8)!); exit(1)
        }
        // center + measure
        let (minB, maxB) = model.boundingBox
        let cx = (minB.x + maxB.x) / 2, cy = (minB.y + maxB.y) / 2, cz = (minB.z + maxB.z) / 2
        let span = max(maxB.x - minB.x, max(maxB.y - minB.y, maxB.z - minB.z))
        let holder = SCNNode(); holder.addChildNode(model)
        model.position = SCNVector3(-cx, -cy, -cz)
        scene.rootNode.addChildNode(holder)

        let camNode = SCNNode(); let cam = SCNCamera()
        cam.zFar = 4000; cam.fieldOfView = 45; camNode.camera = cam
        let d = CGFloat(span) * 1.9
        camNode.position = SCNVector3(d * 0.7, d * 0.5, d * 0.9)
        camNode.look(at: SCNVector3(0, 0, 0))
        scene.rootNode.addChildNode(camNode)

        let key = SCNNode(); key.light = SCNLight(); key.light!.type = .directional
        key.light!.intensity = 1100; key.eulerAngles = SCNVector3(-0.9, 0.6, 0)
        scene.rootNode.addChildNode(key)
        let amb = SCNNode(); amb.light = SCNLight(); amb.light!.type = .ambient
        amb.light!.intensity = 500; scene.rootNode.addChildNode(amb)

        let renderer = SCNRenderer(device: MTLCreateSystemDefaultDevice(), options: nil)
        renderer.scene = scene; renderer.pointOfView = camNode
        let image = renderer.snapshot(atTime: 0, with: size, antialiasingMode: .multisampling4X)
        if let tiff = image.tiffRepresentation, let rep = NSBitmapImageRep(data: tiff),
           let png = rep.representation(using: .png, properties: [:]) {
            try? png.write(to: URL(fileURLWithPath: path))
            print("wrote \(path) (model \(name), span \(Int(span)))")
        }
    }

    /// Render one frame of a map offscreen and write it as PNG.
    static func render(map: MapInfo, to path: String, size: CGSize = CGSize(width: 1024, height: 768),
                       camPos: SCNVector3? = nil, camLook: SCNVector3? = nil) {
        let world = World(map: map)
        _ = world.terrain.update(dt: 0.01)

        let camNode = SCNNode()
        let cam = SCNCamera()
        cam.zFar = 4000
        cam.fieldOfView = 55
        camNode.camera = cam
        let c = world.center
        camNode.position = camPos ?? SCNVector3(CGFloat(c.x - 280), 190, CGFloat(c.z + 340))
        camNode.look(at: camLook ?? SCNVector3(CGFloat(c.x), 18, CGFloat(c.z)))
        world.scene.rootNode.addChildNode(camNode)
        // the gameplay sky actor follows the camera (full position incl. Y,
        // Camera.java:583-586); the offline snapshot must too
        if let sky = world.skyActor {
            sky.simdPosition = SIMD3(Float(camNode.position.x),
                                     Float(camNode.position.y),
                                     Float(camNode.position.z))
        }
        // debug: CB_HIDE=name1,name2 hides matching node subtrees for bisecting
        if let hide = ProcessInfo.processInfo.environment["CB_HIDE"] {
            let names = Set(hide.split(separator: ",").map(String.init))
            world.scene.rootNode.enumerateHierarchy { node, _ in
                if let n = node.name, names.contains(n) { node.isHidden = true }
            }
        }

        let renderer = SCNRenderer(device: MTLCreateSystemDefaultDevice(), options: nil)
        renderer.scene = world.scene
        renderer.pointOfView = camNode
        renderer.autoenablesDefaultLighting = false
        let image = renderer.snapshot(atTime: 0.5, with: size, antialiasingMode: .multisampling4X)

        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let png = rep.representation(using: .png, properties: [:]) else {
            FileHandle.standardError.write("snapshot: could not encode PNG\n".data(using: .utf8)!)
            exit(2)
        }
        do {
            try png.write(to: URL(fileURLWithPath: path))
            print("wrote \(path) (\(map.name))")
        } catch {
            FileHandle.standardError.write("snapshot: \(error)\n".data(using: .utf8)!)
            exit(2)
        }
    }
}
