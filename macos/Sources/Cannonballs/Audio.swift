import Foundation
import AVFoundation

/// Music (bundled MP3s) + synthesized SFX WAVs. Missing files fail silently.
final class Audio {
    static let shared = Audio()

    enum Music {
        case title
        case track(String)   // "track1" / "track2"
    }

    private var musicPlayer: AVAudioPlayer?
    private var currentMusicKey: String?
    private var sfxPlayers: [AVAudioPlayer] = []
    private var loopPlayers: [String: AVAudioPlayer] = [:]
    static let defaultSfx: Float = 0.55
    static let defaultMusic: Float = 0.4
    var sfxVolume: Float = Audio.defaultSfx {
        didSet {                       // muting must silence already-running loops
            guard sfxVolume != oldValue, sfxVolume == 0 else { return }
            for p in sfxPlayers { p.stop() }
            sfxPlayers.removeAll()
            stopLoops()
        }
    }
    var musicVolume: Float = Audio.defaultMusic {
        didSet { musicPlayer?.volume = musicVolume }   // applies to the playing track
    }

    private init() {
        let d = UserDefaults.standard
        if d.object(forKey: "opt.sound") != nil {
            sfxVolume = d.bool(forKey: "opt.sound") ? Audio.defaultSfx : 0
        }
        if d.object(forKey: "opt.music") != nil {
            musicVolume = d.bool(forKey: "opt.music") ? Audio.defaultMusic : 0
        }
    }

    func playMusic(_ music: Music) {
        let key: String
        switch music {
        case .title: key = "title"
        case .track(let t): key = t
        }
        guard key != currentMusicKey else { return }
        currentMusicKey = key
        musicPlayer?.stop()
        let url = Assets.url("MUSIC/\(key).mp3")
        guard let p = try? AVAudioPlayer(contentsOf: url) else { return }
        p.numberOfLoops = -1
        p.volume = musicVolume
        p.play()
        musicPlayer = p
    }

    func stopMusic() {
        musicPlayer?.stop()
        musicPlayer = nil
        currentMusicKey = nil
    }

    /// One-shot SFX by base name (Resources/SFX/<name>.wav).
    func play(_ name: String, volume: Float = 1.0) {
        let url = Assets.url("SFX/\(name).wav")
        guard FileManager.default.fileExists(atPath: url.path),
              let p = try? AVAudioPlayer(contentsOf: url) else { return }
        p.volume = sfxVolume * volume
        p.play()
        sfxPlayers.append(p)
        if sfxPlayers.count > 24 {
            sfxPlayers.removeAll { !$0.isPlaying }
        }
    }

    /// Start a named loop (aiming servo, roller grind). No-op if already running.
    func startLoop(_ name: String, volume: Float = 0.5) {
        guard loopPlayers[name] == nil else { return }
        let url = Assets.url("SFX/\(name).wav")
        guard FileManager.default.fileExists(atPath: url.path),
              let p = try? AVAudioPlayer(contentsOf: url) else { return }
        p.numberOfLoops = -1
        p.volume = sfxVolume * volume
        p.play()
        loopPlayers[name] = p
    }

    func stopLoop(_ name: String) {
        loopPlayers[name]?.stop()
        loopPlayers[name] = nil
    }

    func stopLoops() {
        for (_, p) in loopPlayers { p.stop() }
        loopPlayers.removeAll()
    }
}
