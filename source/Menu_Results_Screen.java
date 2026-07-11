/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTDrop;

public class Menu_Results_Screen
implements Global {
    boolean Visible = false;
    boolean ButtonDown = false;
    Message_3D Version;
    Button_3D OptionsMenu;
    Button_3DList ButtonList = new Button_3DList();
    WTBitmap BackgroundJPG;
    WTDrop Backdrop;
    boolean LBLoaded = false;
    Message_3D Name;
    Message_3D Col1;
    Message_3D Col2;
    Message_3D[] StatNames = new Message_3D[5];
    Message_3D[] Stats = new Message_3D[5];
    Message_3D[] StatsBonus = new Message_3D[5];

    void keyDown(int n) {
    }

    void hide() {
        if (this.Visible) {
            Main.MainRef.MenuManager.hideLoading();
            if (this.Name != null) {
                this.Name.destroy();
            }
            this.Name = null;
            if (this.Col1 != null) {
                this.Col1.destroy();
            }
            this.Col1 = null;
            if (this.Col2 != null) {
                this.Col2.destroy();
            }
            this.Col2 = null;
            int n = 0;
            do {
                if (this.StatNames[n] != null) {
                    this.StatNames[n].destroy();
                }
                this.StatNames[n] = null;
                if (this.Stats[n] != null) {
                    this.Stats[n].destroy();
                }
                this.Stats[n] = null;
                if (this.StatsBonus[n] != null) {
                    this.StatsBonus[n].destroy();
                }
                this.StatsBonus[n] = null;
            } while (++n < 5);
            this.Version.destroy();
            this.Version = null;
            this.ButtonList.destroy();
            Main.MainRef.camera.CameraView.removeDrop(this.Backdrop);
            this.BackgroundJPG.destroy();
            this.BackgroundJPG = null;
            this.Visible = false;
            System.gc();
        }
    }

    void updateTimeSlice(float f) {
        if (!this.LBLoaded && !Main.MainRef.network.isStatsRequesting()) {
            Main.MainRef.MenuManager.hideLoading();
            this.LBLoaded = true;
            this.Name = new Message_3D("Game Stats For '`" + Main.MainRef.network.UserName + "`'", 1, 1.0f);
            this.Name.show(400.0f, 200.0f);
            this.Col1 = new Message_3D("THIS GAME", 1, 1.0f);
            this.Col1.show(400.0f, 250.0f);
            this.Col2 = new Message_3D("TOTAL", 1, 1.0f);
            this.Col2.show(600.0f, 250.0f);
            this.StatNames[0] = new Message_3D("`Kills`", 0, 1.0f);
            this.StatNames[0].show(200.0f, 280.0f);
            this.StatNames[1] = new Message_3D("`Misses`", 0, 1.0f);
            this.StatNames[1].show(200.0f, 300.0f);
            this.StatNames[2] = new Message_3D("`Deaths`", 0, 1.0f);
            this.StatNames[2].show(200.0f, 320.0f);
            this.StatNames[3] = new Message_3D("`Drownings`", 0, 1.0f);
            this.StatNames[3].show(200.0f, 340.0f);
            this.StatNames[4] = new Message_3D("`Gold Spent`", 0, 1.0f);
            this.StatNames[4].show(200.0f, 360.0f);
            this.Stats[0] = new Message_3D("" + (Float.valueOf(Main.MainRef.network.StatValues[4]).intValue() + Main.MainRef.network.StatKills), 1, 1.0f);
            this.Stats[0].show(600.0f, 280.0f);
            this.Stats[1] = new Message_3D("" + (Float.valueOf(Main.MainRef.network.StatValues[1]).intValue() + Main.MainRef.network.StatMiss), 1, 1.0f);
            this.Stats[1].show(600.0f, 300.0f);
            this.Stats[2] = new Message_3D("" + (Float.valueOf(Main.MainRef.network.StatValues[2]).intValue() + Main.MainRef.network.StatDeaths), 1, 1.0f);
            this.Stats[2].show(600.0f, 320.0f);
            this.Stats[3] = new Message_3D("" + (Float.valueOf(Main.MainRef.network.StatValues[3]).intValue() + Main.MainRef.network.StatDrowning), 1, 1.0f);
            this.Stats[3].show(600.0f, 340.0f);
            this.Stats[4] = new Message_3D("" + (Float.valueOf(Main.MainRef.network.StatValues[0]).intValue() + Main.MainRef.network.StatCash), 1, 1.0f);
            this.Stats[4].show(600.0f, 360.0f);
            this.StatsBonus[0] = new Message_3D("`+ " + Main.MainRef.network.StatKills + "`", 1, 1.0f);
            this.StatsBonus[0].show(400.0f, 280.0f);
            this.StatsBonus[1] = new Message_3D("`+ " + Main.MainRef.network.StatMiss + "`", 1, 1.0f);
            this.StatsBonus[1].show(400.0f, 300.0f);
            this.StatsBonus[2] = new Message_3D("`+ " + Main.MainRef.network.StatDeaths + "`", 1, 1.0f);
            this.StatsBonus[2].show(400.0f, 320.0f);
            this.StatsBonus[3] = new Message_3D("`+ " + Main.MainRef.network.StatDrowning + "`", 1, 1.0f);
            this.StatsBonus[3].show(400.0f, 340.0f);
            this.StatsBonus[4] = new Message_3D("`+ " + Main.MainRef.network.StatCash + "`", 1, 1.0f);
            this.StatsBonus[4].show(400.0f, 360.0f);
            Main.MainRef.network.updateStats();
        }
    }

    boolean isLoaded() {
        if (this.BackgroundJPG == null) {
            this.LBLoaded = false;
            Main.MainRef.network.retrieveStats();
            this.BackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/RESULTS_SCREEN/image.wjp", Main.MainRef.CacheType);
            return false;
        }
        return this.BackgroundJPG.isLoaded();
    }

    void show() {
        if (!this.Visible) {
            this.ButtonDown = true;
            if (Main.MainRef.GlobalMedia.ShellMusic == null && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/MUSIC/TITLE"), true);
            }
            if (Main.MainRef.GlobalMedia.ShellMusic != null && !Main.MainRef.GlobalMedia.ShellMusic.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic.play(true, 127);
            }
            this.Visible = true;
            this.Backdrop = Main.MainRef.camera.CameraView.addDrop(this.BackgroundJPG, false);
            this.ButtonList.add(new Button_3D(400, 575, "DONE", "Done", 1));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 0));
            this.ButtonList.showAll();
            this.Version = new Message_3D(Main.MainRef.VersionNumber, 1, 0.75f);
            this.Version.show(400.0f, 14.0f);
            Main.MainRef.MenuManager.showLoading();
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.showMouse();
        }
    }

    void processMouse(int n, int n2, int n3) {
        if (!this.Visible) {
            return;
        }
        if ((n3 & 1) == 1) {
            if (this.ButtonDown) {
                n3 = 0;
            } else {
                this.ButtonDown = true;
            }
        } else if ((n3 & 1) != 1) {
            this.ButtonDown = false;
            n3 = 0;
        }
        String string = this.ButtonList.update(n, n2, n3);
        if (string != null) {
            if (string.equalsIgnoreCase("DONE")) {
                Main.MainRef.MenuManager.activateMenu(2);
                return;
            }
            if (string.equalsIgnoreCase("FULLSCREEN")) {
                this.KeyDown(70);
                return;
            }
        }
    }

    void KeyDown(int n) {
        if (n == 70) {
            Main.MainRef.wt_stage.toggleFullscreen();
        }
    }
}

