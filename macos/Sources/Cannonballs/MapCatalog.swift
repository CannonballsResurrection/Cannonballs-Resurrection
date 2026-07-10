import Foundation
import AppKit

struct MapInfo {
    let name: String          // "Tropicali"
    let dirName: String       // "TROPICALI" (folder under Resources/MAPS)
    let skyName: String       // BLUE / NIGHT / PURPLE / GREEN / DESERT
    let musicTrack: String    // "track1" / "track2"
    let mapScale: Float       // heightmap vertical scale
    let sunVector: SIMD3<Float>
    let ambientRGB: (Float, Float, Float)  // also used as water tint
    let sunRGB: (Float, Float, Float)
    let hasSun: Bool

    var thumbPath: String { "MAPS/\(dirName)/thumb.png" }
    var texturePath: String { "MAPS/\(dirName)/image.png" }
    var heightmapPath: String { "MAPS/\(dirName)/heightmap96.dat" }
    var objectsPath: String { "MAPS/\(dirName)/objects.dat" }
}

enum MapCatalog {
    static let maps: [MapInfo] = load()

    static func byName(_ name: String) -> MapInfo? {
        maps.first { $0.name.lowercased() == name.lowercased() || $0.dirName.lowercased() == name.lowercased() }
    }

    private static func load() -> [MapInfo] {
        guard let text = Assets.text("maplist.dat") else { return [] }
        var out: [MapInfo] = []
        for line in text.split(whereSeparator: \.isNewline) {
            let f = line.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
            // <MAP>:Name:MEDIA/MAPS/DIR:MEDIA/SKIES/SKY:thumb:MEDIA/MUSIC/TRACKn:scale:sunX,sunY,sunZ:ambient:sun:hasSun
            guard f.count >= 11, f[0] == "<MAP>" else { continue }
            let dir = (f[2] as NSString).lastPathComponent
            let sky = (f[3] as NSString).lastPathComponent
            let music = (f[5] as NSString).lastPathComponent.lowercased() // track1/track2
            let scale = Float(f[6]) ?? 64
            func vec(_ s: String) -> [Float] { s.split(separator: ",").compactMap { Float($0) } }
            let sun = vec(f[7]); let amb = vec(f[8]); let sunC = vec(f[9])
            guard sun.count == 3, amb.count == 3, sunC.count == 3 else { continue }
            out.append(MapInfo(
                name: f[1], dirName: dir, skyName: sky, musicTrack: music,
                mapScale: scale,
                sunVector: SIMD3(sun[0], sun[1], sun[2]),
                ambientRGB: (amb[0] / 255, amb[1] / 255, amb[2] / 255),
                sunRGB: (sunC[0] / 255, sunC[1] / 255, sunC[2] / 255),
                hasSun: f[10].trimmingCharacters(in: .whitespaces) == "1"))
        }
        return out
    }
}

extension NSColor {
    convenience init(rgb: (Float, Float, Float), alpha: CGFloat = 1) {
        self.init(calibratedRed: CGFloat(rgb.0), green: CGFloat(rgb.1), blue: CGFloat(rgb.2), alpha: alpha)
    }
}
