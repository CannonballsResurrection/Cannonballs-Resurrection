/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WT
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTEventCallback
 *  wildtangent.webdriver.wt3dLib
 *  wildtangent.webdrivermp.wtMultiplayerLib
 *  wtgutils.com.gameutils
 */
import com.wildtangent.dmmp.client.log.WTDebugLog;
import com.wildtangent.dmmp.shared.log.LogFactory;
import java.applet.Applet;
import java.util.Random;
import netscape.javascript.JSObject;
import wildtangent.webdriver.WT;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTEventCallback;
import wildtangent.webdriver.wt3dLib;
import wildtangent.webdrivermp.wtMultiplayerLib;
import wtgutils.com.gameutils;

public class Main
extends Applet
implements Global,
WTConstants {
    WT Wt;
    IO io;
    Media_List MediaList;
    Global_Media GlobalMedia;
    WT_Stage wt_stage;
    Particle_Plane_List ParticlePlaneList;
    Particle_List ParticleList;
    Particle_List_Store Smoke;
    Particle_List_Store SmokeBlack;
    Particle_List_Store Coins;
    Particle_List_Store Stars;
    Particle_List_Store Splash;
    Particle_List_Store Explosions;
    Particle_List_Store Chunks;
    Particle_List_Store Sparkles;
    Key cryptkey;
    boolean OnlineDemo = true;
    boolean TemporaryOnline = false;
    private String s_Guid;
    private String s_BuyPage;
    private String s_Version;
    String LobbyHostID = "";
    int LobbyHostPort = 4000;
    String DPName = "";
    float[] SinTable = new float[20];
    float[] SinPosition = new float[20];
    Menu_Manager MenuManager;
    Network network;
    Packet_Manager packetmanager;
    Chunk_List ChunkList;
    Game_Loop GameLoop;
    Island island;
    Chat chat;
    Timer timer;
    Map_Tracker MapTracker;
    Letter_List_Store Letters;
    String VersionNumber = "Cannonballs! v1.87";
    boolean UseImagePack = false;
    int Renderer = 1;
    int MaximumParticleCount = 150;
    HUD hud;
    Text TextManager;
    Camera camera;
    JSObject HostPage;
    int ScreenWidth = 800;
    int ScreenHeight = 600;
    int GameState = -1;
    public static Main MainRef;
    boolean CheatsEnabled = false;
    String MediaPath = "";
    String HelpUrl = "instructions.htm";
    boolean RolledOut = false;
    boolean FPSOn = false;
    boolean Busy = false;
    boolean ADPCM = true;
    boolean YAxisReversed = false;
    int PrimaryController = 0;
    boolean SoundsEnabled = true;
    boolean MusicEnabled = true;
    int DebrisDetail = 2;
    int ParticleDetail = 2;
    boolean DynamicLightsEnabled = true;
    int MediaDetail = 2;
    boolean CobrandEnabled = false;
    String CobrandPath;
    float LoadVerificationTimeOut = 0.0f;
    boolean Caching = true;
    Cannon[] cannon = new Cannon[16];
    Chest[] chest = new Chest[64];
    int MaxGamePlayerCount = 8;
    int MaxGamePlayers = 8;
    int CannonCount = 0;
    int MaxCannonCount = 8;
    int ChestCount = 0;
    int ActiveMap = 0;
    int MaxRespawns = 0;
    int HotSeatTime = 0;
    int StartingCash = 0;
    int TreasureRespawn = 0;
    float LastCount = 0.0f;
    int CacheType = 2;
    Random random;
    int UserState = 0;
    boolean ShadowsEnabled = true;
    boolean CannonShadowsEnabled = false;
    boolean ChatMinimized = false;
    boolean in16bit = true;
    boolean SinglePlayer = true;
    float TimePlayed = 0.0f;
    int MatchHosted = 0;
    int MatchJoined = 0;
    int SPMatch = 0;
    boolean Privacy = true;

    public String loadSettings(String string) {
        Object[] objectArray = new Object[]{string};
        Object object = this.HostPage.call("loadSettings", objectArray);
        String string2 = object.toString();
        return string2;
    }

    public void end() {
        this.sendTracking();
        if (this.GameState == 10 && Main.MainRef.MenuManager.ActiveMenu == 2 && Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 2) {
            Main.MainRef.MenuManager.LobbyScreen.cancelGameCreation();
        }
        if (this.GameState == 10 && Main.MainRef.MenuManager.ActiveMenu == 2 && Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 4) {
            Main.MainRef.MenuManager.LobbyScreen.killJoin();
        }
        if (this.network != null) {
            this.network.leaveLobby();
            this.network.shutdown();
        }
        this.Wt.stop();
        this.Wt.shutdown();
        MainRef = null;
        System.gc();
    }

    public void updatePreload(float f) {
        int n = this.MediaList.Media_Count;
        int n2 = this.MediaList.countLoaded();
        float f2 = 0.0f;
        if (n != 0) {
            f2 = (float)n2 / (float)n;
        }
        if (f2 != this.LastCount) {
            this.updateBar((int)Math.ceil(f2 * 126.0f));
        }
        this.LastCount = f2;
        if (!this.cryptkey.isLoaded()) {
            return;
        }
        if (!this.MediaList.allLoaded()) {
            return;
        }
        if (!this.TextManager.isLoaded()) {
            return;
        }
        this.Letters = new Letter_List_Store();
        int n3 = 0;
        do {
            this.Letters.add(new Letter_Object());
        } while (++n3 < 2000);
        n3 = 0;
        do {
            this.Smoke.add(new Particle_Object_Smoke(0));
            this.Chunks.add(new Particle_Object_Chunk());
            this.SmokeBlack.add(new Particle_Object_Smoke(1));
            this.random.nextFloat();
            this.Explosions.add(new Particle_Object_Explosion1());
            this.Splash.add(new Particle_Object_SplashDrop());
        } while (++n3 < 700);
        n3 = 0;
        do {
            this.Coins.add(new Particle_Object_Coin());
            this.Stars.add(new Particle_Object_Star());
            this.Sparkles.add(new Particle_Object_Sparkle());
        } while (++n3 < 100);
        this.showWT();
        this.GameState = 10;
        this.wt_stage.hideRealPointer();
        this.MenuManager.activateMenuInstant(6);
    }

    void showAlert(String string) {
        Object[] objectArray = new Object[10];
        objectArray[0] = string;
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("doalert", objectArray);
    }

    public void launchHelp() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchHelp", null);
    }

    void trackSPMatchLaunch() {
        ++this.SPMatch;
    }

    boolean checkOnline() {
        Object object = this.HostPage.call("checkOnline", null);
        String string = object.toString();
        return string.startsWith("1");
    }

    public void launchTutorial() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchTutorial", null);
    }

    void showWT() {
        this.HostPage.call("show", null);
    }

    void setSinStart() {
        this.SinTable[5] = 0.5f;
        this.SinTable[4] = 0.5f;
        this.SinTable[2] = 0.3f;
        this.SinTable[7] = 0.5f;
    }

    public boolean isDemo() {
        return this.OnlineDemo;
    }

    public void launchChange() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchChange", null);
    }

    public void launchHelpPage(String string) {
        Object[] objectArray = new Object[]{string};
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchHelpPage", objectArray);
    }

    void initializeClassObjects() {
        this.io = new IO();
        if (this.Wt.getInitStatus(3)) {
            this.ADPCM = false;
            this.SoundsEnabled = false;
        }
        this.cryptkey = new Key();
        this.wt_stage = new WT_Stage(this.ScreenWidth, this.ScreenHeight);
        this.camera = new Camera(this.ScreenWidth, this.ScreenHeight, this.wt_stage.FarClip);
        this.MediaList = new Media_List();
        this.GlobalMedia = new Global_Media();
        this.TextManager = new Text();
        this.MenuManager = new Menu_Manager();
        this.hud = new HUD();
        this.ParticlePlaneList = new Particle_Plane_List();
        int n = 0;
        while (n < this.MaximumParticleCount) {
            this.ParticlePlaneList.add(new Particle_Plane());
            ++n;
        }
        this.MapTracker = new Map_Tracker("MEDIA");
        this.ParticleList = new Particle_List();
        this.Chunks = new Particle_List_Store();
        this.Smoke = new Particle_List_Store();
        this.SmokeBlack = new Particle_List_Store();
        this.Explosions = new Particle_List_Store();
        this.Coins = new Particle_List_Store();
        this.Stars = new Particle_List_Store();
        this.Splash = new Particle_List_Store();
        this.Sparkles = new Particle_List_Store();
        this.ChunkList = new Chunk_List();
        this.network = new Network(this.LobbyHostID, this.LobbyHostPort);
        this.GameLoop = new Game_Loop();
        this.chat = new Chat();
        this.timer = new Timer();
        this.GameState = 14;
    }

    void sendTracking() {
        if (this.Privacy) {
            this.Wt.setOptionString(this.getGUID(), "trCannonballs", "LC=1,TP=" + (int)this.TimePlayed + ",MHost=" + this.MatchHosted + ",MJoin=" + this.MatchJoined + ",SPlay=" + this.SPMatch);
        }
    }

    public void launchForgot() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchForgot", null);
    }

    void updateSinTable(float f) {
        this.SinPosition[0] = this.SinPosition[0] + 2.0f * (f /= 1000.0f);
        this.SinTable[0] = (float)Math.sin(this.SinPosition[0]);
        this.SinPosition[1] = this.SinPosition[1] - 2.75f * f;
        this.SinTable[1] = (float)Math.sin(this.SinPosition[1]);
        this.SinPosition[2] = this.SinPosition[2] + 3.5f * f;
        this.SinTable[2] = (float)Math.sin(this.SinPosition[2]);
        this.SinPosition[3] = this.SinPosition[3] + 1.0f * f;
        this.SinTable[3] = (float)Math.sin(this.SinPosition[3]);
        this.SinPosition[4] = this.SinPosition[4] + 1.5f * f;
        this.SinTable[4] = (float)Math.sin(this.SinPosition[4]);
        this.SinPosition[5] = this.SinPosition[5] - 2.5f * f;
        this.SinTable[5] = (float)Math.sin(this.SinPosition[5]);
        this.SinPosition[6] = this.SinPosition[6] + 20.0f * f;
        this.SinTable[6] = (float)Math.sin(this.SinPosition[6]);
        this.SinPosition[7] = this.SinPosition[7] + 1.5f * f;
        this.SinTable[7] = (float)Math.sin(this.SinPosition[3]);
    }

    void saveSettings() {
        if (this.ShadowsEnabled) {
            this.saveSettings("CBSHAD", "1");
        } else {
            this.saveSettings("CBSHAD", "0");
        }
        if (this.SoundsEnabled) {
            this.saveSettings("CBSOUND", "1");
        } else {
            this.saveSettings("CBSOUND", "0");
        }
        if (this.MusicEnabled) {
            this.saveSettings("CBMUSIC", "1");
            return;
        }
        this.saveSettings("CBMUSIC", "0");
    }

    public void saveSettings(String string, String string2) {
        Object[] objectArray = new Object[]{string, string2};
        this.HostPage.call("saveSettings", objectArray);
    }

    void trackMatchHost() {
        ++this.MatchHosted;
    }

    void close() {
        this.Wt.restoreResolution();
        if (this.network != null) {
            this.network.leaveLobby();
        }
        if (this.wt_stage != null) {
            this.wt_stage.showRealPointer();
        }
        this.HostPage.call("destroy", null);
    }

    public void launchTOS() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchTOS", null);
    }

    void changeSettings(int n) {
        boolean bl = this.ShadowsEnabled;
        boolean bl2 = this.MusicEnabled;
        boolean bl3 = this.SoundsEnabled;
        switch (n) {
            case 0: {
                this.ShadowsEnabled = !this.ShadowsEnabled;
                break;
            }
            case 1: {
                this.SoundsEnabled = !this.SoundsEnabled;
                break;
            }
            case 2: {
                boolean bl4 = this.MusicEnabled = !this.MusicEnabled;
            }
        }
        if (!this.in16bit) {
            this.ShadowsEnabled = false;
        }
        if (this.ShadowsEnabled != bl && this.island != null) {
            this.island.switchShadows(this.ShadowsEnabled);
        }
        if (!this.SoundsEnabled) {
            this.MusicEnabled = false;
        }
        if (this.MusicEnabled != bl2) {
            if (this.island != null) {
                this.island.switchMusic(this.MusicEnabled);
            }
            if (this.GameState == 10) {
                this.MenuManager.switchMusic(this.MusicEnabled);
            }
        }
        if (this.SoundsEnabled != bl3 && this.island != null) {
            this.island.switchSounds(this.SoundsEnabled);
        }
        this.saveSettings();
    }

    void checkUnlock() {
        try {
            gameutils.setProduct((String)"{94DB8142-B70B-4665-8DE9-11038094235A}", null, (WT)this.Wt);
            Class clazz = gameutils.loadClass((String)"PropsC12874D6EA954BBB8C477C2452BFF557", (String)"PropsC12874D6EA954BBB8C477C2452BFF557");
            if (clazz != null) {
                Object t = clazz.newInstance();
                this.OnlineDemo = false;
                t = null;
            } else {
                this.OnlineDemo = true;
            }
        }
        catch (Error error) {
            this.OnlineDemo = true;
        }
        catch (Exception exception) {
            this.OnlineDemo = true;
        }
        this.s_Guid = "94DB8142-B70B-4665-8DE9-11038094235A";
        this.s_BuyPage = "http://register.wildtangent.com/product_purchase.asp?pguid=94DB8142-B70B-4665-8DE9-11038094235A";
        this.s_Version = "10/06/2003  10:15 AM";
    }

    public void begin(Object object, Object object2) {
        MainRef = this;
        this.random = new Random();
        try {
            this.HostPage = JSObject.getWindow((Applet)this);
            this.parseParams();
            this.Wt = wt3dLib.getWT((Object)object);
            this.Wt.setMaxFramesPerSecond(60);
            this.Wt.designedForVersion("3.1.0.0");
            this.Wt.setOption(0, 0);
            this.Wt.setOption(1, 1);
            LogFactory.setLogClassName("com.wildtangent.dmmp.client.log.WTDebugLog");
            WTDebugLog.setWebdriver(this.Wt);
            this.in16bit = this.Wt.getInitStatus(6);
            if (this.Wt.getInitStatus(2)) {
                this.close();
                return;
            }
            this.loadSettings();
            this.Privacy = this.checkOnline();
            Object object3 = this.HostPage.call("getLaunchType", null);
            String string = object3.toString();
            if (!string.equalsIgnoreCase(" ")) {
                int n = -1;
                if (string != null) {
                    n = Float.valueOf(string).intValue();
                }
                if (n == 1) {
                    this.MusicEnabled = true;
                } else if (n == 0) {
                    this.MusicEnabled = false;
                }
                if (!this.SoundsEnabled) {
                    this.MusicEnabled = false;
                }
            }
            if (this.Wt.getInitStatus(1)) {
                this.showAlert("256 COLOR MODE IS UNSUPPORTED");
                this.HostPage.call("destroy", null);
                return;
            }
            this.Wt.setErrorHandling(0);
            this.checkUnlock();
            this.initializeClassObjects();
            this.initializeWTCallbacks();
            this.network.wtMulti = wtMultiplayerLib.getWTMultiplayer((Object)object2);
            this.network.initialize();
            this.packetmanager = new Packet_Manager();
        }
        catch (Exception exception) {
            Main.MainRef.Wt.outDebugString("EXCEPTION! " + exception.toString());
        }
        this.Wt.start();
        this.setSinStart();
    }

    int getSettings(String[] stringArray) {
        stringArray[0] = this.ShadowsEnabled ? "Shadows : On" : "Shadows : Off";
        if (!this.SoundsEnabled) {
            stringArray[1] = "Sound : Off";
            return 2;
        }
        stringArray[1] = "Sound : On";
        stringArray[2] = this.MusicEnabled ? "Music : On" : "Music : Off";
        return 3;
    }

    void trackMatchJoin() {
        ++this.MatchJoined;
    }

    public void launchPrivacy() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchPrivacy", null);
    }

    void updateBar(int n) {
        Object[] objectArray = new Object[10];
        objectArray[0] = n + "";
        this.HostPage.call("updateBar", objectArray);
    }

    void initializeWTCallbacks() {
        this.Wt.setNotifyKeyboardEvent(true);
        this.Wt.setOnKeyboardEvent((WTEventCallback)this.io);
        this.Wt.setNotifyMouseEvent(1);
        this.Wt.setOnMouseEvent((WTEventCallback)this.io);
        this.Wt.setOnRenderEvent((WTEventCallback)this.io);
        this.Wt.setNotifyRenderEvent(true);
    }

    int getSettingsWithStats(String[] stringArray) {
        stringArray[0] = "View Leaderboards";
        stringArray[1] = this.ShadowsEnabled ? "Shadows : On" : "Shadows : Off";
        if (!this.SoundsEnabled) {
            stringArray[2] = "Sound : Off";
            return 3;
        }
        stringArray[2] = "Sound : On";
        stringArray[3] = this.MusicEnabled ? "Music : On" : "Music : Off";
        return 4;
    }

    public void launchBuy() {
        if (this.Wt != null) {
            this.Wt.restoreResolution();
        }
        this.HostPage.call("launchBuy", null);
    }

    String getGUID() {
        Object object = this.HostPage.call("get_product_guid", null);
        String string = object.toString();
        return string;
    }

    void parseParams() {
        String string = "";
        string = this.getParameter("COBRAND");
        if (string != null) {
            this.CobrandPath = string;
            this.CobrandEnabled = true;
        }
        if ((string = this.getParameter("DPNAME")) != null) {
            this.DPName = string;
        }
        if ((string = this.getParameter("LOBBY")) != null) {
            this.LobbyHostID = string;
        }
        if ((string = this.getParameter("LOBBYPORT")) != null) {
            this.LobbyHostPort = Float.valueOf(string).intValue();
        }
        this.HelpUrl = (string = this.getParameter("HELP")) != null ? this.getCodeBase() + string : this.getCodeBase() + this.HelpUrl;
        string = this.getParameter("HELPABSOLUTE");
        if (string != null) {
            this.HelpUrl = string;
        }
        if ((string = this.getParameter("MEDIAPATH")) != null) {
            this.MediaPath = string;
        }
    }

    void loadSettings() {
        String string = this.loadSettings("CBMIN");
        if (string.startsWith("1")) {
            this.ChatMinimized = true;
        } else if (string.startsWith("0")) {
            this.ChatMinimized = false;
        }
        string = this.loadSettings("CBSHAD");
        if (string.startsWith("1")) {
            this.ShadowsEnabled = true;
        } else if (string.startsWith("0")) {
            this.ShadowsEnabled = false;
        }
        string = this.loadSettings("CBSOUND");
        if (string.startsWith("1")) {
            this.SoundsEnabled = true;
        } else if (string.startsWith("0")) {
            this.SoundsEnabled = false;
        }
        string = this.loadSettings("CBMUSIC");
        if (string.startsWith("1")) {
            this.MusicEnabled = true;
        } else if (string.startsWith("0")) {
            this.MusicEnabled = false;
        }
        if (!this.in16bit) {
            this.ShadowsEnabled = false;
        }
    }

    public boolean JavaCapable() {
        try {
            wt3dLib wt3dLib2 = new wt3dLib();
            wt3dLib2 = null;
        }
        catch (Exception exception) {
            return false;
        }
        return true;
    }

    void saveMinimizedSettings() {
        if (this.ChatMinimized) {
            this.saveSettings("CBMIN", "1");
            return;
        }
        this.saveSettings("CBMIN", "0");
    }
}

