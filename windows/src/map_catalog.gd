class_name MapCatalog
# Port of macos/Sources/Cannonballs/MapCatalog.swift.


class MapInfo:
	var name: String          # "Tropicali"
	var dir_name: String      # "TROPICALI" (folder under Resources/MAPS)
	var sky_name: String      # BLUE / NIGHT / PURPLE / GREEN / DESERT
	var music_track: String   # "track1" / "track2"
	var map_scale: float      # heightmap vertical scale
	var sun_vector: Vector3
	var ambient_rgb: Color    # also used as water tint
	var sun_rgb: Color
	var has_sun: bool

	func thumb_path() -> String:
		return "MAPS/%s/thumb.png" % dir_name

	func texture_path() -> String:
		return "MAPS/%s/image.png" % dir_name

	func heightmap_path() -> String:
		return "MAPS/%s/heightmap96.dat" % dir_name

	func objects_path() -> String:
		return "MAPS/%s/objects.dat" % dir_name


static var maps: Array = _load()


static func by_name(name: String):
	for m in maps:
		if m.name.to_lower() == name.to_lower() or m.dir_name.to_lower() == name.to_lower():
			return m
	return null


static func _load() -> Array:
	var raw := Assets.text("maplist.dat")
	if raw.is_empty():
		return []
	var out: Array = []
	for line in raw.split("\n", false):
		var f := line.strip_edges().split(":")
		# <MAP>:Name:MEDIA/MAPS/DIR:MEDIA/SKIES/SKY:thumb:MEDIA/MUSIC/TRACKn:scale:sunX,sunY,sunZ:ambient:sun:hasSun
		if f.size() < 11 or f[0] != "<MAP>":
			continue
		var sun := _vec(f[7])
		var amb := _vec(f[8])
		var sun_c := _vec(f[9])
		if sun.size() != 3 or amb.size() != 3 or sun_c.size() != 3:
			continue
		var m := MapInfo.new()
		m.name = f[1]
		m.dir_name = f[2].get_file()
		m.sky_name = f[3].get_file()
		m.music_track = f[5].get_file().to_lower()   # track1/track2
		m.map_scale = f[6].to_float() if f[6].is_valid_float() else 64.0
		m.sun_vector = Vector3(sun[0], sun[1], sun[2])
		m.ambient_rgb = Color(amb[0] / 255.0, amb[1] / 255.0, amb[2] / 255.0)
		m.sun_rgb = Color(sun_c[0] / 255.0, sun_c[1] / 255.0, sun_c[2] / 255.0)
		m.has_sun = f[10].strip_edges() == "1"
		out.append(m)
	return out


static func _vec(s: String) -> Array:
	var out: Array = []
	for part in s.split(","):
		if part.is_valid_float():
			out.append(part.to_float())
	return out
