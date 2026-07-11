/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTDrop;

public class Menu_Stats_Screen
implements Global {
    boolean Visible = false;
    boolean ButtonDown = false;
    Message_3D Version;
    Button_3D OptionsMenu;
    Button_3DList ButtonList = new Button_3DList();
    WTBitmap BackgroundJPG;
    WTDrop Backdrop;
    boolean LBLoaded = false;
    Message_3D[] LBTitles = new Message_3D[6];
    Message_3D[][] LBStats = new Message_3D[6][10];
    Message_3D[][] LBNames = new Message_3D[6][10];

    void keyDown(int n) {
    }

    void hide() {
        if (this.Visible) {
            Main.MainRef.MenuManager.hideLoading();
            int n = 0;
            do {
                if (this.LBTitles[n] != null) {
                    this.LBTitles[n].destroy();
                }
                this.LBTitles[n] = null;
                int n2 = 0;
                do {
                    if (this.LBNames[n][n2] != null) {
                        this.LBNames[n][n2].destroy();
                    }
                    this.LBNames[n][n2] = null;
                    if (this.LBStats[n][n2] != null) {
                        this.LBStats[n][n2].destroy();
                    }
                    this.LBStats[n][n2] = null;
                } while (++n2 < 10);
            } while (++n < 6);
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
        if (!this.LBLoaded && !Main.MainRef.network.isLeaderboardRequesting()) {
            Main.MainRef.MenuManager.hideLoading();
            this.LBLoaded = true;
            int n = 0;
            do {
                int n2 = 0;
                int n3 = n;
                while (n3 > 2) {
                    n3 -= 3;
                    ++n2;
                }
                this.LBTitles[n] = new Message_3D(Main.MainRef.network.LBTableNames[n], 1, 1.0f, 0);
                this.LBTitles[n].show(160 + n2 * 480, 50 + n3 * 180);
                int n4 = 0;
                while (n4 < Main.MainRef.network.LBTableCount[n]) {
                    if (Main.MainRef.network.LBTableUsers[n][n4] == null) {
                        return;
                    }
                    if (Main.MainRef.network.LBTableUsers[n][n4] == null) {
                        return;
                    }
                    this.LBNames[n][n4] = new Message_3D(Main.MainRef.network.LBTableUsers[n][n4], 0, 0.75f, 0, 1);
                    this.LBStats[n][n4] = new Message_3D(Main.MainRef.network.LBTableValues[n][n4], 2, 0.75f, 0);
                    this.LBNames[n][n4].show(60 + n2 * 480, 70 + n3 * 180 + n4 * 16);
                    this.LBStats[n][n4].show(260 + n2 * 480, 70 + n3 * 180 + n4 * 16);
                    ++n4;
                }
            } while (++n < 6);
        }
    }

    boolean isLoaded() {
        if (this.BackgroundJPG == null) {
            this.LBLoaded = false;
            Main.MainRef.network.retrieveLeaderboard();
            this.BackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/RESULTS_SCREEN/image.wjp", Main.MainRef.CacheType);
            return false;
        }
        return this.BackgroundJPG.isLoaded();
    }

    void show() {
        if (!this.Visible) {
            Main.MainRef.MenuManager.showLoading();
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
            if (string.equalsIgnoreCase("QUIT")) {
                Main.MainRef.close();
            }
            if (string.equalsIgnoreCase("DONE")) {
                if (Main.MainRef.network.isNetContextConnected()) {
                    Main.MainRef.MenuManager.activateMenu(2);
                    return;
                }
                Main.MainRef.MenuManager.activateMenu(3);
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

