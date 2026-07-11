/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Menu_Manager
implements Global {
    Menu_Lobby_Screen LobbyScreen;
    Menu_Main_Screen MainScreen;
    Menu_Stats_Screen StatsScreen;
    Menu_Results_Screen ResultsScreen;
    Menu_Splash_Screen SplashScreen;
    Media_Object_Actor Transition;
    WTGroup Hourglass;
    int ActiveMenu = -1;
    int QueuedMenu = -1;
    int QueuedGamestate = -1;
    int LastMenu = -1;
    boolean Loaded = false;
    boolean Parsing = false;
    boolean TextLoaded = false;
    float LoadVerificationTimeOut = 0.0f;
    int LoadingFrame = 0;
    float LoadingTime = 0.0f;
    boolean LoadingVisible = false;
    float DissolveScale = 10.0f;
    boolean DissolveVisible = false;
    Message_3D[] ConnectionStats = new Message_3D[8];
    Message_3D[] ConnectionStats2 = new Message_3D[8];
    String[] LastConnectionMessages = new String[8];

    public void deactivateMenu(int n) {
        this.hideMenu(n);
    }

    public void hideMenu(int n) {
        switch (n) {
            case 2: {
                this.LobbyScreen.hide();
                break;
            }
            case 3: {
                this.MainScreen.hide();
                break;
            }
            case 4: {
                this.StatsScreen.hide();
                break;
            }
            case 5: {
                this.ResultsScreen.hide();
                break;
            }
            case 6: {
                this.SplashScreen.hide();
            }
        }
        System.gc();
    }

    void KeyDown(int n) {
        switch (this.ActiveMenu) {
            case 3: {
                this.MainScreen.KeyDown(n);
                return;
            }
            case 4: {
                this.StatsScreen.KeyDown(n);
                return;
            }
            case 2: {
                this.LobbyScreen.keyDown(n);
                return;
            }
            case 5: {
                this.ResultsScreen.keyDown(n);
                return;
            }
        }
    }

    void showGameConnectionStats() {
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            String string = "";
            if (Main.MainRef.cannon[n].TimeSincePing > 10000.0f) {
                string = "not responding";
            } else if (Main.MainRef.cannon[n].Synced) {
                string = "synced";
            } else {
                switch (Main.MainRef.network.ClientConnectionState[n]) {
                    case 0: {
                        string = "...loading island";
                        break;
                    }
                    case 1: {
                        string = "...syncing";
                        break;
                    }
                    case 2: {
                        string = "synced";
                    }
                }
            }
            if (Main.MainRef.network.ClientConnectionState[n] == 3) {
                string = "has left";
            }
            if (this.LastConnectionMessages[n] == null || !string.equals(this.LastConnectionMessages[n])) {
                this.LastConnectionMessages[n] = string;
                if (this.ConnectionStats[n] != null) {
                    this.ConnectionStats[n].destroy();
                    this.ConnectionStats2[n].destroy();
                }
                this.ConnectionStats[n] = null;
                this.ConnectionStats2[n] = null;
                this.ConnectionStats[n] = new Message_3D("`" + Main.MainRef.network.PlayerNames[n] + "`", 0, 0.75f, 115);
                this.ConnectionStats[n].show(250.0f, 378 + n * 18);
                this.ConnectionStats2[n] = new Message_3D(string, 2, 0.75f, 115);
                this.ConnectionStats2[n].show(550.0f, 378 + n * 18);
            }
            ++n;
        }
    }

    public void processMouseMenu(int n, int n2, int n3) {
        switch (this.ActiveMenu) {
            case 3: {
                this.MainScreen.processMouse(n, n2, n3);
                return;
            }
            case 4: {
                this.StatsScreen.processMouse(n, n2, n3);
                return;
            }
            case 5: {
                this.ResultsScreen.processMouse(n, n2, n3);
                return;
            }
            case 2: {
                this.LobbyScreen.processMouse(n, n2, n3);
                return;
            }
        }
    }

    public void activateMenu(int n) {
        Main.MainRef.GameState = 10;
        this.LastMenu = this.ActiveMenu;
        this.ActiveMenu = 102;
        this.showDissolve();
        this.QueuedMenu = n;
    }

    void switchMusic(boolean bl) {
        if (bl) {
            if (Main.MainRef.GlobalMedia.ShellMusic == null && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/MUSIC/TITLE"), true);
            }
            if (Main.MainRef.GlobalMedia.ShellMusic != null && !Main.MainRef.GlobalMedia.ShellMusic.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic.play(true, 127);
                return;
            }
        } else if (Main.MainRef.GlobalMedia.ShellMusic != null) {
            Main.MainRef.GlobalMedia.ShellMusic.stop();
        }
    }

    void showLoading() {
        if (!this.LoadingVisible) {
            if (this.Hourglass == null) {
                this.Hourglass = Main.MainRef.Wt.createGroup();
                this.Hourglass.attachSurfaceShader(Main.MainRef.GlobalMedia.Hourglass.Shader, 0.175104f, 0.350208f, 128, 128);
                this.Hourglass.setOption(0, 127);
            }
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Hourglass);
            this.Hourglass.setPosition(0.0f, 0.0f, 2.0f);
            this.LoadingVisible = true;
            this.LoadingFrame = 0;
            this.setFrame(this.LoadingFrame);
        }
    }

    public Menu_Manager() {
        this.LobbyScreen = new Menu_Lobby_Screen();
        this.MainScreen = new Menu_Main_Screen();
        this.StatsScreen = new Menu_Stats_Screen();
        this.SplashScreen = new Menu_Splash_Screen();
        this.ResultsScreen = new Menu_Results_Screen();
        this.Transition = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/MENUS/TRANSITION"), true);
        this.Transition.Model.setOption(0, 124);
    }

    void showLoading(float f, float f2, float f3) {
        if (!this.LoadingVisible) {
            if (this.Hourglass == null) {
                this.Hourglass = Main.MainRef.Wt.createGroup();
                this.Hourglass.attachSurfaceShader(Main.MainRef.GlobalMedia.Hourglass.Shader, 0.175104f, 0.350208f, 128, 128);
                this.Hourglass.setOption(0, 127);
            }
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Hourglass);
            this.Hourglass.setPosition(f, f2, f3);
            this.LoadingVisible = true;
            this.LoadingFrame = 0;
            this.setFrame(this.LoadingFrame);
        }
    }

    void hideLoading() {
        if (this.LoadingVisible) {
            this.LoadingVisible = false;
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Hourglass);
        }
    }

    public void updateActiveMenu(float f) {
        if (f > 200.0f) {
            f = 200.0f;
        }
        switch (this.ActiveMenu) {
            case 2: {
                this.LobbyScreen.updateTimeSlice(f);
                return;
            }
            case 3: {
                this.MainScreen.updateTimeSlice(f);
                return;
            }
            case 4: {
                this.StatsScreen.updateTimeSlice(f);
                return;
            }
            case 6: {
                this.SplashScreen.updateTimeSlice(f);
                return;
            }
            case 5: {
                this.ResultsScreen.updateTimeSlice(f);
                return;
            }
            case 101: {
                this.DissolveScale += f / 1000.0f;
                if (this.DissolveScale > 1.0f) {
                    this.DissolveScale = 1.0f;
                }
                this.Transition.Model.setAbsoluteScale(this.DissolveScale);
                if (!(this.DissolveScale >= 1.0f)) break;
                this.hideDissolve();
                this.ActiveMenu = this.QueuedMenu;
                return;
            }
            case 102: {
                this.DissolveScale -= f / 1000.0f;
                if (this.DissolveScale <= 0.001f) {
                    this.DissolveScale = 0.001f;
                }
                this.Transition.Model.setAbsoluteScale(this.DissolveScale);
                if (!(this.DissolveScale <= 0.001f)) break;
                this.hideMenu(this.LastMenu);
                this.ActiveMenu = 100;
                this.showLoading();
                return;
            }
            case 103: {
                this.DissolveScale += f / 1000.0f;
                if (this.DissolveScale > 1.0f) {
                    this.DissolveScale = 1.0f;
                }
                this.Transition.Model.setAbsoluteScale(this.DissolveScale);
                if (!(this.DissolveScale >= 1.0f)) break;
                this.hideDissolve();
                this.ActiveMenu = this.QueuedMenu;
                return;
            }
            case 104: {
                this.DissolveScale -= f / 1000.0f;
                if (this.DissolveScale <= 0.001f) {
                    this.DissolveScale = 0.001f;
                }
                this.Transition.Model.setAbsoluteScale(this.DissolveScale);
                if (!(this.DissolveScale <= 0.001f)) break;
                this.hideMenu(this.LastMenu);
                Main.MainRef.GameState = this.QueuedGamestate;
                this.showLoading();
                return;
            }
            case 100: {
                this.updateLoading(f);
                if (!this.isMenuLoaded(this.QueuedMenu)) break;
                System.gc();
                this.hideLoading();
                this.ActiveMenu = 101;
                this.showMenu(this.QueuedMenu);
                return;
            }
        }
    }

    public void dissolveOffToGame(float f) {
        this.DissolveScale += f / 1000.0f;
        if (this.DissolveScale > 1.0f) {
            this.DissolveScale = 1.0f;
        }
        this.Transition.Model.setAbsoluteScale(this.DissolveScale);
        if (this.DissolveScale >= 1.0f) {
            this.hideDissolve();
            Main.MainRef.GameState = 3;
        }
    }

    public void dissolveOnToMenu(float f) {
        this.DissolveScale -= f / 1000.0f;
        if (this.DissolveScale <= 0.001f) {
            this.DissolveScale = 0.001f;
        }
        this.Transition.Model.setAbsoluteScale(this.DissolveScale);
    }

    public void activateGame(int n) {
        Main.MainRef.GameState = 10;
        this.LastMenu = this.ActiveMenu;
        this.ActiveMenu = 104;
        this.showDissolve();
        this.QueuedGamestate = n;
    }

    void hideGameConnectionStats() {
        int n = 0;
        do {
            if (this.ConnectionStats[n] == null) continue;
            this.LastConnectionMessages[n] = "";
            this.ConnectionStats[n].destroy();
            this.ConnectionStats[n] = null;
            this.ConnectionStats2[n].destroy();
            this.ConnectionStats2[n] = null;
        } while (++n < 8);
    }

    void setFrame(int n) {
        int n2 = (int)Math.ceil(n / 4);
        int n3 = n - (n2 - 1) * 4;
        this.Hourglass.setBitmapTextureRect(0.25f * (float)n3, 0.5f * (float)n2, 0.25f * (float)(n3 + 1), 0.5f * (float)(n2 + 1));
    }

    public boolean isMenuLoaded(int n) {
        switch (n) {
            case 2: {
                return this.LobbyScreen.isLoaded();
            }
            case 4: {
                return this.StatsScreen.isLoaded();
            }
            case 5: {
                return this.ResultsScreen.isLoaded();
            }
            case 3: {
                return this.MainScreen.isLoaded();
            }
            case 6: {
                return this.SplashScreen.isLoaded();
            }
        }
        return false;
    }

    boolean isLoaded() {
        return this.Transition.isLoaded();
    }

    void KeyUp(int n) {
        switch (this.ActiveMenu) {
            case 2: {
                this.LobbyScreen.keyUp(n);
                return;
            }
        }
    }

    void updateLoading(float f) {
        this.LoadingTime += (f /= 1000.0f);
        if (this.LoadingTime > 0.4f) {
            this.LoadingTime = 0.0f;
            ++this.LoadingFrame;
            if (this.LoadingFrame > 5) {
                this.LoadingFrame = 0;
            }
            this.setFrame(this.LoadingFrame);
        }
    }

    public void showMenu(int n) {
        Main.MainRef.camera.positionForMenu();
        switch (n) {
            case 2: {
                this.LobbyScreen.show();
                return;
            }
            case 3: {
                this.MainScreen.show();
                return;
            }
            case 4: {
                this.StatsScreen.show();
                return;
            }
            case 5: {
                this.ResultsScreen.show();
                return;
            }
            case 6: {
                this.SplashScreen.show();
                return;
            }
        }
    }

    public void activateMenuInstant(int n) {
        this.showDissolve();
        this.DissolveScale = 0.001f;
        this.Transition.Model.setAbsoluteScale(this.DissolveScale);
        Main.MainRef.GameState = 10;
        this.LastMenu = this.ActiveMenu;
        this.ActiveMenu = 100;
        this.showLoading();
        this.QueuedMenu = n;
    }

    void showDissolve() {
        if (!this.DissolveVisible) {
            this.DissolveVisible = true;
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Transition.Model);
            this.Transition.Model.setPosition(0.0f, 0.0f, 2.0f);
            this.DissolveScale = 1.0f;
            this.Transition.Model.setAbsoluteScale(this.DissolveScale);
        }
    }

    void hideDissolve() {
        if (this.DissolveVisible) {
            this.DissolveVisible = false;
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Transition.Model);
        }
    }
}

