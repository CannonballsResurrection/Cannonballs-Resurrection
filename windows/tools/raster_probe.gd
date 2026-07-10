extends SceneTree
# THROWAWAY probe (delete after verification): builds World map 0, places the
# camera at the terrain-uitest framing (src/boot.gd), renders one frame through
# SceneRasterizer, saves snapshots/uitest-software-probe.png. Run:
#   godot --headless --path windows --script tools/raster_probe.gd


func _init() -> void:
	process_frame.connect(_run, CONNECT_ONE_SHOT)


func _run() -> void:
	var maps := MapCatalog.maps
	if maps.is_empty():
		printerr("no maps")
		quit(1)
		return
	var world := World.new(maps[0])
	root.add_child(world.root)
	world.update(0.01)   # warm terrain, like main.swift:108 (--rasterscene)

	var cam := Camera3D.new()
	cam.far = 4000
	cam.fov = 55
	root.add_child(cam)
	var c := world.center()
	cam.look_at_from_position(c + Vector3(0, 250, -400), c, Vector3.UP)

	var s := SceneRasterizer.settings(world.map)
	var t0 := Time.get_ticks_usec()
	var snap := SceneRasterizer.snapshot(world.root, cam, Vector2i(640, 480))
	var t1 := Time.get_ticks_usec()
	var raster := SceneRasterizer.rasterize(snap, s)
	var t2 := Time.get_ticks_usec()
	var img := raster.image(s.sky_bottom, s.gamma)
	var t3 := Time.get_ticks_usec()
	print("snapshot %.1f ms | rasterize %.1f ms | dither %.1f ms | %d draw calls" %
			[(t1 - t0) / 1000.0, (t2 - t1) / 1000.0, (t3 - t2) / 1000.0, snap.calls.size()])

	var dir := ProjectSettings.globalize_path("res://snapshots")
	DirAccess.make_dir_recursive_absolute(dir)
	var out := dir.path_join("uitest-software-probe.png")
	img.save_png(out)
	print("probe wrote ", out)

	# Second framing: the macOS --rasterscene camera (main.swift:109-113) for a
	# direct comparison against `Cannonballs --rasterscene TROPICALI ref.png`.
	cam.look_at_from_position(Vector3(c.x - 280, 190, c.z + 340), Vector3(c.x, 18, c.z), Vector3.UP)
	var raster2 := SceneRasterizer.render_frame(world.root, cam, Vector2i(640, 480), s)
	var out2 := dir.path_join("uitest-software-probe2.png")
	raster2.image(s.sky_bottom, s.gamma).save_png(out2)
	print("probe wrote ", out2)
	quit(0)
