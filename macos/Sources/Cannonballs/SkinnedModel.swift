import Foundation
import SceneKit

/// Loads skinned WildTangent actors decoded by tools/wsgo_export_skinned.py
/// (MODELS/<NAME>/skinned.json) and their motions (tools/wsmo_decode.py JSON).
///
/// The original engine skins with a matrix palette: each vertex carries up to
/// 2 (bone, weight/255) influences; bones form a hierarchy of "chunks" whose
/// inverse-bind matrices ship in the geom header. Motions are per-bone gePaths
/// (translation vec3 + rotation quat WXYZ) that compose AFTER the bind-local
/// transform (Genesis3D pose.c: World = ParentWorld × Attachment × MotionSample).
/// We mirror that exactly: bindNode (static bind-local) → motionNode (animated,
/// identity at rest), with SCNSkinner driving the mesh.
enum SkinnedModel {

    struct Part: Decodable {
        let texture: String
        var keyed: Bool?
        var influencesPerVertex: Int?
        let verts: [Float]
        let normals: [Float]
        let uvs: [Float]
        let tris: [UInt16]
        let skinIndices: [UInt16]
        let skinWeights: [Float]
    }
    struct Doc: Decodable {
        let name: String
        var texture: String?
        var influencesPerVertex: Int?
        var verts: [Float]?
        var normals: [Float]?
        var uvs: [Float]?
        var tris: [UInt16]?
        let bones: [Bone]
        var skinIndices: [UInt16]?
        var skinWeights: [Float]?
        var parts: [Part]?

        /// Normalized part list (legacy single-mesh docs become one part).
        var allParts: [Part] {
            if let parts { return parts }
            return [Part(texture: texture ?? "", keyed: false,
                         influencesPerVertex: influencesPerVertex,
                         verts: verts ?? [], normals: normals ?? [], uvs: uvs ?? [],
                         tris: tris ?? [], skinIndices: skinIndices ?? [],
                         skinWeights: skinWeights ?? [])]
        }
    }
    struct Bone: Decodable {
        let name: String
        let parent: Int
        let bindLocal: [Float]   // row-major 4x4, column-vector convention
        let invBind: [Float]
    }

    struct Motion {
        struct Track { let times: [Double]; let values: [[Double]] }
        let duration: Double
        let translation: [String: Track]
        let rotation: [String: Track]   // WXYZ
    }

    /// A built skinned actor: root node plus the motion nodes to drive.
    final class Actor {
        let root: SCNNode
        let motionNodes: [String: SCNNode]
        let doc: Doc
        init(root: SCNNode, motionNodes: [String: SCNNode], doc: Doc) {
            self.root = root; self.motionNodes = motionNodes; self.doc = doc
        }
    }

    static func load(_ name: String, file: String = "skinned.json") -> Actor? {
        guard let data = Assets.data("MODELS/\(name)/\(file)"),
              let doc = try? JSONDecoder().decode(Doc.self, from: data) else { return nil }

        let root = SCNNode()
        root.name = "skinned-\(name)"

        // Bone hierarchy: bindNode (bind-local transform) → motionNode (identity).
        var bindNodes: [SCNNode] = [], motionNodes: [SCNNode] = []
        var byName: [String: SCNNode] = [:]
        for bone in doc.bones {
            let bind = SCNNode(); bind.name = "bind-\(bone.name)"
            bind.transform = scnMatrix(rowMajorColumnVector: bone.bindLocal)
            let motion = SCNNode(); motion.name = bone.name
            bind.addChildNode(motion)
            bindNodes.append(bind); motionNodes.append(motion)
            byName[bone.name] = motion
        }
        for (i, bone) in doc.bones.enumerated() {
            (bone.parent >= 0 ? motionNodes[bone.parent] : root).addChildNode(bindNodes[i])
        }

        // Geometry: one skinned mesh node per part (parts share the skeleton).
        for part in doc.allParts {
            let vCount = part.verts.count / 3
            guard vCount > 0 else { continue }
            let posSrc = SCNGeometrySource(vertices: (0..<vCount).map {
                SCNVector3(part.verts[$0*3], part.verts[$0*3+1], part.verts[$0*3+2]) })
            let nrmSrc = SCNGeometrySource(normals: (0..<vCount).map {
                SCNVector3(part.normals[$0*3], part.normals[$0*3+1], part.normals[$0*3+2]) })
            let uvSrc = SCNGeometrySource(textureCoordinates: (0..<vCount).map {
                CGPoint(x: CGFloat(part.uvs[$0*2]), y: CGFloat(part.uvs[$0*2+1])) })
            let element = SCNGeometryElement(indices: part.tris, primitiveType: .triangles)
            let geo = SCNGeometry(sources: [posSrc, nrmSrc, uvSrc], elements: [element])

            let mat = SCNMaterial()
            if let img = Assets.image("MODELS/\(name)/textures/\(part.texture)") {
                mat.diffuse.contents = img
                mat.diffuse.magnificationFilter = .nearest
            }
            mat.lightingModel = .lambert
            mat.ambient.contents = NSColor(calibratedWhite: 0.3, alpha: 1)
            mat.specular.contents = NSColor.black
            if part.keyed == true {
                mat.isDoubleSided = true
                mat.transparencyMode = .aOne
                mat.shaderModifiers = [.surface: "if (_surface.diffuse.a < 0.5) { discard_fragment(); }"]
            }
            geo.materials = [mat]

            let per = part.influencesPerVertex ?? 2
            let weightsData = part.skinWeights.withUnsafeBufferPointer { Data(buffer: $0) }
            let weightSrc = SCNGeometrySource(data: weightsData, semantic: .boneWeights,
                                              vectorCount: vCount, usesFloatComponents: true,
                                              componentsPerVector: per, bytesPerComponent: 4,
                                              dataOffset: 0, dataStride: per * 4)
            let idxData = part.skinIndices.withUnsafeBufferPointer { Data(buffer: $0) }
            let idxSrc = SCNGeometrySource(data: idxData, semantic: .boneIndices,
                                           vectorCount: vCount, usesFloatComponents: false,
                                           componentsPerVector: per, bytesPerComponent: 2,
                                           dataOffset: 0, dataStride: per * 2)
            let invBinds = doc.bones.map { NSValue(scnMatrix4: scnMatrix(rowMajorColumnVector: $0.invBind)) }
            let skinner = SCNSkinner(baseGeometry: geo, bones: motionNodes,
                                     boneInverseBindTransforms: invBinds,
                                     boneWeights: weightSrc, boneIndices: idxSrc)
            let meshNode = SCNNode(geometry: geo)
            meshNode.name = "mesh-\(name)"
            meshNode.skinner = skinner
            skinner.skeleton = motionNodes.first
            root.addChildNode(meshNode)
        }
        return Actor(root: root, motionNodes: byName, doc: doc)
    }

    // MARK: - Motion

    static func loadMotion(_ path: String) -> Motion? {
        guard let data = Assets.data(path),
              let j = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let duration = j["duration"] as? Double,
              let bones = j["bones"] as? [String: Any] else { return nil }
        var tr: [String: Motion.Track] = [:], rot: [String: Motion.Track] = [:]
        for (bone, any) in bones {
            guard let b = any as? [String: Any] else { continue }
            if let t = b["translation"] as? [String: Any],
               let times = t["times"] as? [Double], let vals = t["values"] as? [[Double]] {
                tr[bone] = Motion.Track(times: times, values: vals)
            }
            if let r = b["rotation"] as? [String: Any],
               let times = r["times"] as? [Double], var vals = r["values"] as? [[Double]] {
                // enforce quaternion sign continuity so keyframe lerp takes the short arc
                for k in 1..<vals.count {
                    let dot = zip(vals[k], vals[k-1]).map(*).reduce(0, +)
                    if dot < 0 { vals[k] = vals[k].map { -$0 } }
                }
                rot[bone] = Motion.Track(times: times, values: vals)
            }
        }
        return Motion(duration: duration, translation: tr, rotation: rot)
    }

    /// Attach looping keyframe animations for the motion. One shared phase offset
    /// keeps all bone tracks in sync (the original starts at a random phase).
    static func animate(_ actor: Actor, motion: Motion, phase: Double) {
        for (bone, node) in actor.motionNodes {
            if let t = motion.translation[bone], t.values.count > 1 {
                let anim = CAKeyframeAnimation(keyPath: "position")
                anim.keyTimes = t.times.map { NSNumber(value: $0 / motion.duration) }
                anim.values = t.values.map { NSValue(scnVector3: SCNVector3($0[0], $0[1], $0[2])) }
                configure(anim, duration: motion.duration, phase: phase)
                node.addAnimation(anim, forKey: "motion-t")
            }
            if let r = motion.rotation[bone], r.values.count > 1 {
                let anim = CAKeyframeAnimation(keyPath: "orientation")
                anim.keyTimes = r.times.map { NSNumber(value: $0 / motion.duration) }
                // stored WXYZ; SceneKit orientation is (x, y, z, w)
                anim.values = r.values.map { NSValue(scnVector4: SCNVector4($0[1], $0[2], $0[3], $0[0])) }
                configure(anim, duration: motion.duration, phase: phase)
                node.addAnimation(anim, forKey: "motion-r")
            }
        }
    }

    /// Play the motion once (e.g. the cannon's fire recoil), then snap back to rest.
    static func playOnce(_ actor: Actor, motion: Motion) {
        for (bone, node) in actor.motionNodes {
            if let t = motion.translation[bone], t.values.count > 1 {
                let anim = CAKeyframeAnimation(keyPath: "position")
                anim.keyTimes = t.times.map { NSNumber(value: $0 / motion.duration) }
                anim.values = t.values.map { NSValue(scnVector3: SCNVector3($0[0], $0[1], $0[2])) }
                anim.duration = motion.duration
                anim.calculationMode = .linear
                node.addAnimation(anim, forKey: "fire-t")
            }
            if let r = motion.rotation[bone], r.values.count > 1 {
                let anim = CAKeyframeAnimation(keyPath: "orientation")
                anim.keyTimes = r.times.map { NSNumber(value: $0 / motion.duration) }
                anim.values = r.values.map { NSValue(scnVector4: SCNVector4($0[1], $0[2], $0[3], $0[0])) }
                anim.duration = motion.duration
                anim.calculationMode = .linear
                node.addAnimation(anim, forKey: "fire-r")
            }
        }
    }

    private static func configure(_ anim: CAKeyframeAnimation, duration: Double, phase: Double) {
        anim.duration = duration
        anim.repeatCount = .infinity
        anim.calculationMode = .linear
        anim.timeOffset = phase
        anim.isRemovedOnCompletion = false
    }

    /// Statically pose the actor at motion time t (for offscreen verification renders).
    static func pose(_ actor: Actor, motion: Motion, at t: Double) {
        func sample(_ track: Motion.Track) -> [Double] {
            let times = track.times, vals = track.values
            if t <= times[0] { return vals[0] }
            for k in 0..<(times.count - 1) where times[k] <= t && t <= times[k+1] {
                let a = (t - times[k]) / max(times[k+1] - times[k], 1e-9)
                return zip(vals[k], vals[k+1]).map { (1 - a) * $0 + a * $1 }
            }
            return vals[vals.count - 1]
        }
        for (bone, node) in actor.motionNodes {
            if let tr = motion.translation[bone] {
                let v = sample(tr); node.position = SCNVector3(v[0], v[1], v[2])
            }
            if let r = motion.rotation[bone] {
                var q = sample(r)
                let n = (q.map { $0 * $0 }.reduce(0, +)).squareRoot()
                if n > 0 { q = q.map { $0 / n } }
                node.orientation = SCNVector4(q[1], q[2], q[3], q[0])
            }
        }
    }

    /// Row-major column-vector-convention 4x4 (our tools' output) -> SCNMatrix4
    /// (SceneKit is row-vector convention, so this is a transpose).
    private static func scnMatrix(rowMajorColumnVector t: [Float]) -> SCNMatrix4 {
        SCNMatrix4(
            m11: CGFloat(t[0]), m12: CGFloat(t[4]), m13: CGFloat(t[8]),  m14: CGFloat(t[12]),
            m21: CGFloat(t[1]), m22: CGFloat(t[5]), m23: CGFloat(t[9]),  m24: CGFloat(t[13]),
            m31: CGFloat(t[2]), m32: CGFloat(t[6]), m33: CGFloat(t[10]), m34: CGFloat(t[14]),
            m41: CGFloat(t[3]), m42: CGFloat(t[7]), m43: CGFloat(t[11]), m44: CGFloat(t[15]))
    }
}
