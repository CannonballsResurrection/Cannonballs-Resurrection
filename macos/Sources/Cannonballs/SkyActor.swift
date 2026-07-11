import Foundation
import SceneKit

/// The ORIGINAL sky actors (SKIES/<NAME>/actor.wsgo): the sky dome mesh with the
/// horizon-island billboards (+ moon / star layer / outcrops) at their authored
/// positions. Exported by tools/export_skies.py to MODELS/SKY/<NAME>.json.
///
/// The dome is ~51 units in geom space; we scale it to sit inside the fog start
/// distance and follow the camera position (the WT engine attached it to the
/// camera environment), rendering first with no depth write — a true skybox.
enum SkyActor {

    struct Doc: Decodable {
        struct Part: Decodable {
            let name: String
            let texture: String
            let alpha: Bool
            let verts: [Float]
            let uvs: [Float]
            let tris: [UInt16]
        }
        let name: String
        let parts: [Part]
    }

    static func load(_ skyName: String, scale: CGFloat = 12.0) -> SCNNode? {
        guard let data = Assets.data("MODELS/SKY/\(skyName).json"),
              let doc = try? JSONDecoder().decode(Doc.self, from: data) else { return nil }
        let root = SCNNode()
        root.name = "sky-actor"
        for (index, part) in doc.parts.enumerated() {
            let vCount = part.verts.count / 3
            let posSrc = SCNGeometrySource(vertices: (0..<vCount).map {
                SCNVector3(part.verts[$0*3], part.verts[$0*3+1], part.verts[$0*3+2]) })
            let uvSrc = SCNGeometrySource(textureCoordinates: (0..<vCount).map {
                CGPoint(x: CGFloat(part.uvs[$0*2]), y: CGFloat(part.uvs[$0*2+1])) })
            let element = SCNGeometryElement(indices: part.tris, primitiveType: .triangles)
            let geo = SCNGeometry(sources: [posSrc, uvSrc], elements: [element])
            let m = SCNMaterial()
            m.diffuse.contents = Assets.image("MODELS/SKY/textures/\(part.texture)")
            m.lightingModel = .constant
            m.isDoubleSided = true
            m.writesToDepthBuffer = false
            m.readsFromDepthBuffer = false
            if part.alpha {
                // plain alpha blend — a discard modifier here routes the quad through
                // SceneKit's alpha-test depth prepass, whose full-rect depth writes
                // punch billboard-shaped holes in clouds behind (ignores
                // writesToDepthBuffer=false); the 2002 engine alpha-blended these
                m.transparencyMode = .aOne
            }
            if part.name == "skytop" { m.blendMode = .add }   // star layer adds over the dome
            geo.materials = [m]
            let n = SCNNode(geometry: geo)
            n.name = "sky-\(part.name)"
            n.castsShadow = false
            // The sky draws first as one unit (Island.java:1213
            // setOption(0,-100), far below the game's option numbers). WITHIN
            // it, layer by the wsgo's authored part order (NIGHT: skydome2,
            // skytop, isle1, moon, isle2) — pinned by data, not by SceneKit's
            // distance sort, so the moon can never paint over an island cutout.
            n.renderingOrder = -300 + index
            root.addChildNode(n)
        }
        root.scale = SCNVector3(scale, scale, scale)
        return root
    }
}
