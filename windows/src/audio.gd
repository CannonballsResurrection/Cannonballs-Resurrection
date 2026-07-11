class_name Audio
extends Node
# Port of macos/Sources/Cannonballs/Audio.swift.
# Music (bundled MP3s) + synthesized SFX WAVs. Missing files fail silently.
#
# The Swift class is a process singleton (Audio.shared). Godot's
# AudioStreamPlayers only play from inside the scene tree, so here Audio is a
# Node the game adds exactly once at boot (add_child(Audio.new())); _init
# publishes the instance as the static `shared`, so call sites keep the exact
# Swift shape: Audio.shared.play("quake").
#
# Streams are loaded at runtime from shared/Resources via Assets
# (AudioStreamWAV/AudioStreamMP3.load_from_file — never res:// imports).


static var shared: Audio

# Swift: enum Music { title / track(String) }. GDScript has no associated
# values, so play_music() takes the key string directly: "title", or a map's
# music_track ("track1" / "track2").

const DEFAULT_SFX := 0.55    # Audio.defaultSfx
const DEFAULT_MUSIC := 0.4   # Audio.defaultMusic

# UserDefaults analogue for the option toggles read in _init. The HUDScene
# port writes the same keys (opt.sound / opt.music, HUDScene.swift:1073-1081).
const OPTIONS_PATH := "user://options.cfg"

var _music_player: AudioStreamPlayer = null
var _current_music_key := ""
var _sfx_players: Array[AudioStreamPlayer] = []
var _loop_players := {}      # name -> AudioStreamPlayer

# One-shot stream cache (Swift re-opens the WAV per AVAudioPlayer; caching the
# immutable stream is inaudible). Loop streams are NOT cached — see start_loop.
static var _sfx_cache := {}

var sfx_volume := DEFAULT_SFX:
	set(value):
		# muting must silence already-running loops
		var changed := value != sfx_volume
		sfx_volume = value
		if not changed or value != 0.0:
			return
		for p in _sfx_players:
			p.stop()
			p.queue_free()
		_sfx_players.clear()
		stop_loops()

var music_volume := DEFAULT_MUSIC:
	set(value):
		music_volume = value
		if _music_player != null:   # applies to the playing track
			_music_player.volume_db = _db(music_volume)


func _init() -> void:
	shared = self
	var d := ConfigFile.new()
	if d.load(OPTIONS_PATH) == OK:
		if d.has_section_key("options", "opt.sound"):
			sfx_volume = DEFAULT_SFX if d.get_value("options", "opt.sound") else 0.0
		if d.has_section_key("options", "opt.music"):
			music_volume = DEFAULT_MUSIC if d.get_value("options", "opt.music") else 0.0


func play_music(key: String) -> void:
	if key == _current_music_key:
		return
	_current_music_key = key
	if _music_player != null:
		_music_player.stop()
		_music_player.queue_free()
		_music_player = null
	var p := Assets.path("MUSIC/%s.mp3" % key)
	if not FileAccess.file_exists(p):   # missing files fail silently
		return
	var stream := AudioStreamMP3.load_from_file(p)
	if stream == null:
		return
	stream.loop = true   # numberOfLoops = -1
	var player := AudioStreamPlayer.new()
	player.stream = stream
	player.volume_db = _db(music_volume)
	add_child(player)
	player.play()
	_music_player = player


func stop_music() -> void:
	if _music_player != null:
		_music_player.stop()
		_music_player.queue_free()
	_music_player = null
	_current_music_key = ""


## One-shot SFX by base name (Resources/SFX/<name>.wav).
func play(sfx_name: String, volume: float = 1.0) -> void:
	var stream := _sfx_stream(sfx_name)
	if stream == null:
		return
	var player := AudioStreamPlayer.new()
	player.stream = stream
	player.volume_db = _db(sfx_volume * volume)
	add_child(player)
	player.play()
	_sfx_players.append(player)
	if _sfx_players.size() > 24:
		for q in _sfx_players.duplicate():
			if not q.playing:
				_sfx_players.erase(q)
				q.queue_free()


## Start a named loop (aiming servo, roller grind). No-op if already running.
func start_loop(sfx_name: String, volume: float = 0.5) -> void:
	if _loop_players.has(sfx_name):
		return
	var p := Assets.path("SFX/%s.wav" % sfx_name)
	if not FileAccess.file_exists(p):
		return
	# Fresh (uncached) stream: loop points live on the stream itself and must
	# not leak into one-shot play() of the same file.
	var stream := AudioStreamWAV.load_from_file(p)
	if stream == null:
		return
	# AVAudioPlayer numberOfLoops = -1 → whole-clip forward loop. loop_end is
	# in frames; the decoded originals are all 16-bit mono PCM.
	var bytes_per_frame := 2 if stream.format == AudioStreamWAV.FORMAT_16_BITS else 1
	if stream.stereo:
		bytes_per_frame *= 2
	stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
	stream.loop_begin = 0
	@warning_ignore("integer_division")
	stream.loop_end = stream.data.size() / bytes_per_frame
	var player := AudioStreamPlayer.new()
	player.stream = stream
	player.volume_db = _db(sfx_volume * volume)
	add_child(player)
	player.play()
	_loop_players[sfx_name] = player


func stop_loop(sfx_name: String) -> void:
	if _loop_players.has(sfx_name):
		_loop_players[sfx_name].stop()
		_loop_players[sfx_name].queue_free()
	_loop_players.erase(sfx_name)


func stop_loops() -> void:
	for p in _loop_players.values():
		p.stop()
		p.queue_free()
	_loop_players.clear()


## AVAudioPlayer volume is linear 0..1; Godot players take dB. Every
## linear→dB conversion in the game goes through here
## (linear_to_db(0.0) = -inf = fully silent, matching AVAudioPlayer volume 0).
static func _db(linear: float) -> float:
	return linear_to_db(linear)


func _sfx_stream(sfx_name: String) -> AudioStreamWAV:
	if _sfx_cache.has(sfx_name):
		return _sfx_cache[sfx_name]
	var p := Assets.path("SFX/%s.wav" % sfx_name)
	var stream: AudioStreamWAV = null
	if FileAccess.file_exists(p):   # missing files fail silently
		stream = AudioStreamWAV.load_from_file(p)
	_sfx_cache[sfx_name] = stream
	return stream
