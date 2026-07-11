/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTDrop;

public class Menu_Main_Screen
implements Global {
    int InternalMenuState = 0;
    boolean Visible = false;
    boolean ButtonDown = false;
    PopUp PopupMenu;
    Message_3D RegOnly;
    Message_3D Version;
    Message_3D LoginName;
    Message_3D LoginUser;
    Message_3D LoginPass;
    Message_3D LoginMessage1;
    Message_3D LoginMessage2;
    Message_3D LoginMessage3;
    Message_3D LoginMessage4;
    Message_3D LoginMessage5;
    String Login = "";
    Button_3D OptionsMenu;
    Button_3DList ButtonList = new Button_3DList();
    Button_3DList SubButtonList = new Button_3DList();
    float CurrentRot = 0.0f;
    float TargetRot = 0.0f;
    WTBitmap BackgroundJPG;
    WTDrop Backdrop;
    char[] KeyHistory = new char[7];
    float averageTime = 5.0f;
    boolean CursorVisible = false;
    int TopLine = 0;
    float BlinkCount = 0.0f;

    void keyDown(int n) {
        if (n == 9) {
            ++this.TopLine;
            if (this.InternalMenuState == 6 && this.TopLine > 0) {
                this.TopLine = 0;
            }
            if (this.InternalMenuState == 4) {
                if (this.TopLine > 2) {
                    this.TopLine = 0;
                }
            } else if (this.TopLine > 1) {
                this.TopLine = 0;
            }
        }
        if (n == 13) {
            if (this.InternalMenuState == 6) {
                if (Main.MainRef.network.UserName.length() > 0) {
                    this.startSinglePlayer();
                    return;
                }
                return;
            }
            if (this.InternalMenuState == 11) {
                if (Main.MainRef.network.UserName.length() > 0) {
                    this.attemptGuestConnect();
                    return;
                }
                return;
            }
            if (this.InternalMenuState == 4) {
                if (Main.MainRef.network.UserName.length() > 0 && Main.MainRef.network.Password.length() > 0 && Main.MainRef.network.UserEmail.length() > 0) {
                    this.attemptCreate();
                    return;
                }
            } else if (Main.MainRef.network.UserEmail.length() > 0 && Main.MainRef.network.Password.length() > 0) {
                this.attemptConnect();
            }
            return;
        }
        if (this.InternalMenuState == 11) {
            return;
        }
        if (n != 8 && n != 13 && n != 16 && n != 20 && n != 9) {
            if (this.TopLine == 0) {
                if (this.InternalMenuState == 6 && Main.MainRef.network.UserName.length() < 12 && (n >= 48 && n < 58 || n >= 65 && n <= 90 || n >= 97 && n <= 122)) {
                    Main.MainRef.network.UserName = Main.MainRef.network.UserName + "" + (char)n;
                }
                if (this.InternalMenuState == 11) {
                    if (Main.MainRef.network.UserName.length() < 7 && (n >= 48 && n < 58 || n >= 65 && n <= 90 || n >= 97 && n <= 122)) {
                        Main.MainRef.network.UserName = Main.MainRef.network.UserName + "" + (char)n;
                    }
                } else if (Main.MainRef.network.UserEmail.length() < 32 && (n >= 48 && n < 58 || n >= 65 && n <= 90 || n >= 97 && n <= 122 || n == 46 || n == 64 || n == 95 || n == 45)) {
                    Main.MainRef.network.UserEmail = Main.MainRef.network.UserEmail + "" + (char)n;
                }
            } else if (this.TopLine == 1 && Main.MainRef.network.Password.length() < 32) {
                Main.MainRef.network.Password = Main.MainRef.network.Password + "" + (char)n;
            } else if (this.TopLine == 2 && Main.MainRef.network.UserName.length() < 12 && (n >= 48 && n < 58 || n >= 65 && n <= 90 || n >= 97 && n <= 122)) {
                Main.MainRef.network.UserName = Main.MainRef.network.UserName + "" + (char)n;
            }
            this.updateLoginMenuText();
        }
        if (n == 8) {
            if (this.TopLine == 0) {
                if (this.InternalMenuState == 6 || this.InternalMenuState == 11) {
                    if (Main.MainRef.network.UserName.length() > 0) {
                        Main.MainRef.network.UserName = Main.MainRef.network.UserName.substring(0, Main.MainRef.network.UserName.length() - 1);
                    }
                } else if (Main.MainRef.network.UserEmail.length() > 0) {
                    Main.MainRef.network.UserEmail = Main.MainRef.network.UserEmail.substring(0, Main.MainRef.network.UserEmail.length() - 1);
                }
            } else if (this.TopLine == 1 && Main.MainRef.network.Password.length() > 0) {
                Main.MainRef.network.Password = Main.MainRef.network.Password.substring(0, Main.MainRef.network.Password.length() - 1);
            } else if (this.TopLine == 2 && Main.MainRef.network.UserName.length() > 0) {
                Main.MainRef.network.UserName = Main.MainRef.network.UserName.substring(0, Main.MainRef.network.UserName.length() - 1);
            }
            this.updateLoginMenuText();
        }
    }

    void attemptGuestConnect() {
        Main.MainRef.network.UserName = "`guest`0";
        if (this.LoginMessage4 != null) {
            this.LoginMessage4.destroy();
        }
        this.LoginMessage4 = new Message_3D("Verifying User...", 0, 0.75f, 55);
        this.LoginMessage4.show(250.0f, 360.0f);
        Main.MainRef.network.verifyGuestName(Main.MainRef.network.UserName);
        this.InternalMenuState = 12;
    }

    void connect() {
        Main.MainRef.saveSettings("CEMAIL", Main.MainRef.network.UserEmail);
        Main.MainRef.saveSettings("CPASS", Main.MainRef.network.Password);
        Main.MainRef.network.Connected = true;
        Main.MainRef.network.requestRoomListing();
        Main.MainRef.SinglePlayer = false;
        Main.MainRef.MenuManager.activateMenu(2);
    }

    void startSinglePlayer() {
        Main.MainRef.saveSettings("CNAME", Main.MainRef.network.UserName);
        Main.MainRef.SinglePlayer = true;
        Main.MainRef.MenuManager.activateMenu(2);
    }

    void hide() {
        if (this.Visible) {
            this.OptionsMenu = null;
            if (this.RegOnly != null) {
                this.RegOnly.destroy();
            }
            if (this.LoginMessage1 != null) {
                this.LoginMessage1.destroy();
            }
            if (this.LoginMessage2 != null) {
                this.LoginMessage2.destroy();
            }
            if (this.LoginMessage3 != null) {
                this.LoginMessage3.destroy();
            }
            if (this.LoginMessage4 != null) {
                this.LoginMessage4.destroy();
            }
            if (this.LoginMessage5 != null) {
                this.LoginMessage5.destroy();
            }
            this.LoginMessage1 = null;
            this.LoginMessage2 = null;
            this.LoginMessage3 = null;
            this.LoginMessage4 = null;
            this.LoginMessage5 = null;
            if (this.LoginUser != null) {
                this.LoginUser.destroy();
            }
            this.LoginUser = null;
            if (this.LoginName != null) {
                this.LoginName.destroy();
            }
            this.LoginName = null;
            if (this.LoginPass != null) {
                this.LoginPass.destroy();
            }
            this.LoginPass = null;
            if (this.PopupMenu != null) {
                this.PopupMenu.destroy();
            }
            this.PopupMenu = null;
            this.Version.destroy();
            this.Version = null;
            this.ButtonList.destroy();
            this.SubButtonList.destroy();
            Main.MainRef.camera.CameraView.removeDrop(this.Backdrop);
            this.BackgroundJPG.destroy();
            this.BackgroundJPG = null;
            this.Visible = false;
            System.gc();
        }
    }

    void updateTimeSlice(float f) {
        if (this.InternalMenuState == 1 || this.InternalMenuState == 4 || this.InternalMenuState == 6) {
            this.BlinkCount += f;
            if (this.BlinkCount > 300.0f) {
                this.BlinkCount = 0.0f;
                this.CursorVisible = !this.CursorVisible;
                this.updateLoginMenuText();
                return;
            }
        } else if (this.InternalMenuState == 2) {
            if (!Main.MainRef.network.isUMSRequesting()) {
                if (Main.MainRef.network.getUMSResult()) {
                    Main.MainRef.network.UserName = Main.MainRef.network.UMSResultValue;
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    this.LoginMessage4 = null;
                    this.LoginMessage4 = new Message_3D("Connecting...", 0, 0.75f, 55);
                    this.LoginMessage4.show(250.0f, 360.0f);
                    Main.MainRef.network.createNetContext();
                    this.InternalMenuState = 3;
                    return;
                }
                this.InternalMenuState = 1;
                if (this.LoginUser != null) {
                    this.LoginUser.destroy();
                    this.LoginUser = null;
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage5 = null;
                    this.PopupMenu.destroy();
                    this.PopupMenu = new PopUp(400, 300, "Login To Game", true);
                    this.updateLoginMenuText();
                    this.LoginMessage2.show(190.0f, 270.0f);
                    this.LoginMessage3.show(190.0f, 300.0f);
                    this.SubButtonList.destroy();
                    this.SubButtonList.add(new Button_3D(527, 218, "FORGOT", "Forgot Password?", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 218, "CHANGE", "Change Password", 1, 51));
                    this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Login", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 270, 51, "top"));
                    this.SubButtonList.add(new Button_Bar(300, 250, 300, 51, "bottom"));
                    this.SubButtonList.showAll();
                }
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                this.LoginMessage4 = null;
                if (Main.MainRef.network.UserName.startsWith("`guest`")) {
                    Main.MainRef.network.UserName = Main.MainRef.network.UserName.substring(7, Main.MainRef.network.UserName.length());
                }
                this.LoginMessage4 = new Message_3D("Error : " + Main.MainRef.network.UMSResultValue, 0, 0.75f, 55);
                this.LoginMessage4.show(250.0f, 360.0f);
                return;
            }
        } else if (this.InternalMenuState == 12) {
            if (!Main.MainRef.network.isUMSRequesting()) {
                if (Main.MainRef.network.getUMSResult()) {
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    this.LoginMessage4 = null;
                    this.LoginMessage4 = new Message_3D("Connecting...", 0, 0.75f, 55);
                    this.LoginMessage4.show(250.0f, 360.0f);
                    Main.MainRef.network.createNetContext();
                    this.InternalMenuState = 13;
                    return;
                }
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                this.LoginMessage4 = null;
                this.LoginMessage4 = new Message_3D("Error : " + Main.MainRef.network.UMSResultValue, 0, 0.75f, 55);
                this.LoginMessage4.show(250.0f, 360.0f);
                this.InternalMenuState = 11;
                if (this.LoginMessage5 != null) {
                    this.LoginMessage5.destroy();
                }
                this.LoginMessage5 = null;
                this.PopupMenu.destroy();
                this.PopupMenu = new PopUp(400, 300, "Login As A Guest", true);
                this.SubButtonList.destroy();
                this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Login", 1, 52));
                this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                this.SubButtonList.showAll();
                return;
            }
        } else if (this.InternalMenuState == 5) {
            if (!Main.MainRef.network.isUMSRequesting()) {
                if (Main.MainRef.network.getUMSResult()) {
                    this.attemptConnect();
                    return;
                }
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                this.LoginMessage4 = null;
                if (Main.MainRef.network.UserName.startsWith("`guest`")) {
                    Main.MainRef.network.UserName = Main.MainRef.network.UserName.substring(7, Main.MainRef.network.UserName.length());
                }
                this.LoginMessage4 = new Message_3D("Error : " + Main.MainRef.network.UMSResultValue, 0, 0.75f, 55);
                this.LoginMessage4.show(250.0f, 360.0f);
                this.InternalMenuState = 4;
                return;
            }
        } else if (this.InternalMenuState == 3) {
            Main.MainRef.network.pollNetContext(10L);
            if (Main.MainRef.network.isNetContextConnected()) {
                if (Main.MainRef.OnlineDemo) {
                    Main.MainRef.TemporaryOnline = true;
                }
                Main.MainRef.OnlineDemo = false;
                this.connect();
                return;
            }
            if (Main.MainRef.network.didNetContextConnectionFail()) {
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                this.LoginMessage4 = null;
                this.LoginMessage4 = new Message_3D("Connection Failure : " + Main.MainRef.network.NetworkError, 0, 0.75f, 55);
                this.LoginMessage4.show(250.0f, 360.0f);
                this.InternalMenuState = 1;
                if (this.LoginUser != null) {
                    this.LoginUser.destroy();
                    this.LoginUser = null;
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage5 = null;
                    this.PopupMenu.destroy();
                    this.PopupMenu = new PopUp(400, 300, "Login To Game", true);
                    this.updateLoginMenuText();
                    this.LoginMessage2.show(190.0f, 270.0f);
                    this.LoginMessage3.show(190.0f, 300.0f);
                    this.SubButtonList.destroy();
                    this.SubButtonList.add(new Button_3D(527, 218, "FORGOT", "Forgot Password?", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 218, "CHANGE", "Change Password", 1, 51));
                    this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Login", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 270, 51, "top"));
                    this.SubButtonList.add(new Button_Bar(300, 250, 300, 51, "bottom"));
                    this.SubButtonList.showAll();
                }
                return;
            }
        } else if (this.InternalMenuState == 13) {
            Main.MainRef.network.pollNetContext(10L);
            if (Main.MainRef.network.isNetContextConnected()) {
                this.connect();
                return;
            }
            if (Main.MainRef.network.didNetContextConnectionFail()) {
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                this.LoginMessage4 = null;
                this.LoginMessage4 = new Message_3D("Connection Failure : " + Main.MainRef.network.NetworkError, 0, 0.75f, 55);
                this.LoginMessage4.show(250.0f, 360.0f);
                this.InternalMenuState = 11;
                if (this.LoginMessage5 != null) {
                    this.LoginMessage5.destroy();
                }
                this.LoginMessage5 = null;
                this.PopupMenu.destroy();
                this.PopupMenu = new PopUp(400, 300, "Login As A Guest", true);
                this.SubButtonList.destroy();
                this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Login", 1, 52));
                this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                this.SubButtonList.showAll();
                return;
            }
        }
    }

    void attemptConnect() {
        if (this.LoginMessage4 != null) {
            this.LoginMessage4.destroy();
        }
        this.LoginMessage4 = new Message_3D("Verifying User...", 0, 0.75f, 55);
        this.LoginMessage4.show(250.0f, 360.0f);
        Main.MainRef.network.verifyUMSName(Main.MainRef.network.UserEmail, Main.MainRef.network.Password);
        this.InternalMenuState = 2;
    }

    boolean isLoaded() {
        if (this.BackgroundJPG == null) {
            this.BackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/MAIN_SCREEN/image.wjp", Main.MainRef.CacheType);
            return false;
        }
        return this.BackgroundJPG.isLoaded();
    }

    void updateLoginMenuText() {
        if (this.LoginName != null) {
            this.LoginName.destroy();
        }
        this.LoginName = null;
        if (this.LoginPass != null) {
            this.LoginPass.destroy();
        }
        this.LoginName = null;
        if (this.LoginUser != null) {
            this.LoginUser.destroy();
        }
        this.LoginUser = null;
        String string = "";
        int n = 0;
        while (n < Main.MainRef.network.Password.length()) {
            string = string + "*";
            ++n;
        }
        if (this.CursorVisible) {
            if (this.TopLine == 0) {
                this.LoginName = this.InternalMenuState == 6 || this.InternalMenuState == 11 ? new Message_3D(Main.MainRef.network.UserName + '\u007f', 0, 0.75f, 55) : new Message_3D(Main.MainRef.network.UserEmail + '\u007f', 0, 0.75f, 55);
                if (this.InternalMenuState != 6 && this.InternalMenuState != 11) {
                    this.LoginPass = new Message_3D(string, 0, 1.0f, 55);
                }
                if (this.InternalMenuState == 4) {
                    this.LoginUser = new Message_3D(Main.MainRef.network.UserName, 0, 1.0f, 55);
                }
            } else if (this.TopLine == 1) {
                this.LoginName = new Message_3D(Main.MainRef.network.UserEmail, 0, 0.75f, 55);
                if (this.InternalMenuState != 6) {
                    this.LoginPass = new Message_3D(string + '\u007f', 0, 1.0f, 55);
                }
                if (this.InternalMenuState == 4) {
                    this.LoginUser = new Message_3D(Main.MainRef.network.UserName, 0, 1.0f, 55);
                }
            } else if (this.TopLine == 2) {
                this.LoginName = new Message_3D(Main.MainRef.network.UserEmail, 0, 0.75f, 55);
                if (this.InternalMenuState != 6) {
                    this.LoginPass = new Message_3D(string, 0, 1.0f, 55);
                }
                if (this.InternalMenuState == 4) {
                    this.LoginUser = new Message_3D(Main.MainRef.network.UserName + '\u007f', 0, 1.0f, 55);
                }
            }
            this.LoginName.show(260.0f, 270.0f);
            if (this.InternalMenuState != 6 && this.InternalMenuState != 11) {
                this.LoginPass.show(260.0f, 300.0f);
            }
            if (this.InternalMenuState == 4) {
                this.LoginUser.show(260.0f, 330.0f);
                return;
            }
        } else {
            this.LoginName = this.InternalMenuState == 6 || this.InternalMenuState == 11 ? new Message_3D(Main.MainRef.network.UserName, 0, 0.75f, 55) : new Message_3D(Main.MainRef.network.UserEmail, 0, 0.75f, 55);
            this.LoginName.show(260.0f, 270.0f);
            if (this.InternalMenuState != 6 && this.InternalMenuState != 11) {
                this.LoginPass = new Message_3D(string, 0, 1.0f, 55);
                this.LoginPass.show(260.0f, 300.0f);
            }
            if (this.InternalMenuState == 4) {
                this.LoginUser = new Message_3D(Main.MainRef.network.UserName, 0, 1.0f, 55);
                this.LoginUser.show(260.0f, 330.0f);
            }
        }
    }

    void show() {
        if (!this.Visible) {
            if (Main.MainRef.TemporaryOnline) {
                Main.MainRef.TemporaryOnline = false;
                Main.MainRef.OnlineDemo = true;
            }
            Main.MainRef.camera.CameraView.setClipping(6000.0f, 1.0f);
            String string = Main.MainRef.loadSettings("CEMAIL");
            String string2 = Main.MainRef.loadSettings("CPASS");
            String string3 = Main.MainRef.loadSettings("CNAME");
            if (string == null) {
                string = "";
            }
            if (string2 == null) {
                string2 = "";
            }
            if (string3 == null) {
                string3 = "";
            }
            Main.MainRef.network.UserEmail = string;
            Main.MainRef.network.Password = string2;
            Main.MainRef.network.UserName = string3;
            this.InternalMenuState = 0;
            this.ButtonDown = true;
            if (Main.MainRef.GlobalMedia.ShellMusic == null && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/MUSIC/TITLE"), true);
            }
            if (Main.MainRef.GlobalMedia.ShellMusic != null && !Main.MainRef.GlobalMedia.ShellMusic.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic.play(true, 127);
            }
            this.Visible = true;
            this.Backdrop = Main.MainRef.camera.CameraView.addDrop(this.BackgroundJPG, false);
            if (Main.MainRef.OnlineDemo) {
                this.ButtonList.add(new Button_3D(400, 380, "GUEST", "Guest Login", 1));
                this.ButtonList.add(new Button_3D(400, 420, "SINGLE", "Single Player", 1));
                this.ButtonList.add(new Button_3D(400, 460, "STATS", "View Leaderboards", 1));
                this.ButtonList.add(new Button_3D(400, 520, "REGISTER", "Register Now!", 1));
            } else {
                this.ButtonList.add(new Button_3D(400, 320, "PLAY", "User Login", 1));
                this.ButtonList.add(new Button_3D(400, 350, "CREATE", "New User", 1));
                this.ButtonList.add(new Button_3D(400, 400, "GUEST", "Guest Login", 1));
                this.ButtonList.add(new Button_3D(400, 450, "SINGLE", "Single Player", 1));
                this.ButtonList.add(new Button_3D(400, 500, "STATS", "Leaderboards", 1));
            }
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 0));
            String[] stringArray = new String[10];
            stringArray[0] = "Quit Game";
            this.ButtonList.add(new Button_3DMenu(10, 14, 1, stringArray, "Quit", "QUIT", 0));
            int n = Main.MainRef.getSettings(stringArray);
            this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n, stringArray, "Options", "OPTI", 0));
            stringArray[0] = "Game Info";
            stringArray[1] = "Tutorial";
            this.ButtonList.add(new Button_3DMenu(165, 14, 2, stringArray, "Help!", "HELP", 0));
            this.ButtonList.showAll();
            this.Version = new Message_3D(Main.MainRef.VersionNumber, 1, 0.75f);
            this.Version.show(400.0f, 14.0f);
            String string4 = Main.MainRef.loadSettings("CPLAYED");
            if (string4 == null) {
                string4 = "";
            }
            if (!string4.startsWith("YES")) {
                if (this.PopupMenu != null) {
                    this.PopupMenu.destroy();
                }
                this.PopupMenu = new PopUp(400, 300, "View Tutorial?", true);
                this.SubButtonList.destroy();
                this.SubButtonList.add(new Button_3D(527, 412, "YES", "Yes", 1, 52));
                this.SubButtonList.add(new Button_3D(273, 412, "NO", "No", 1, 51));
                this.SubButtonList.showAll();
                this.LoginMessage2 = new Message_3D("This is your first time playing.", 1, 1.0f, 51);
                this.LoginMessage2.show(400.0f, 270.0f);
                this.LoginMessage3 = new Message_3D("Would you like to view the tutorial?", 1, 1.0f, 51);
                this.LoginMessage3.show(400.0f, 295.0f);
                this.LoginMessage4 = new Message_3D("You may also view it at any time", 1, 1.0f, 51);
                this.LoginMessage4.show(400.0f, 320.0f);
                this.LoginMessage5 = new Message_3D("from the help menu.", 1, 1.0f, 51);
                this.LoginMessage5.show(400.0f, 345.0f);
                this.InternalMenuState = 80;
            }
            Main.MainRef.saveSettings("CPLAYED", "YES");
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.showMouse();
        }
    }

    void processMouse(int n, int n2, int n3) {
        String string;
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
        if (this.InternalMenuState == 0) {
            String string2 = this.ButtonList.update(n, n2, n3);
            if (string2 != null) {
                if (string2.startsWith("QUIT")) {
                    Main.MainRef.Wt.restoreResolution();
                    Main.MainRef.close();
                    return;
                }
                if (string2.startsWith("HELP")) {
                    int n4 = Float.valueOf(string2.substring(4, string2.length())).intValue();
                    if (n4 == 0) {
                        Main.MainRef.launchHelp();
                        return;
                    }
                    Main.MainRef.launchTutorial();
                    return;
                }
                if (string2.startsWith("REGISTER")) {
                    Main.MainRef.launchBuy();
                    return;
                }
                if (string2.equalsIgnoreCase("FULLSCREEN")) {
                    this.KeyDown(70);
                    return;
                }
                if (string2.equalsIgnoreCase("EXIT")) {
                    Main.MainRef.Wt.restoreResolution();
                    Main.MainRef.close();
                    return;
                }
                if (string2.equalsIgnoreCase("STATS")) {
                    Main.MainRef.MenuManager.activateMenu(4);
                    return;
                }
                if (string2.equalsIgnoreCase("PLAY")) {
                    this.TopLine = 0;
                    this.InternalMenuState = 1;
                    this.PopupMenu = new PopUp(400, 300, "Login To Game", true);
                    this.updateLoginMenuText();
                    this.LoginMessage2 = new Message_3D("Email:", 0, 1.0f, 51);
                    this.LoginMessage2.show(190.0f, 270.0f);
                    this.LoginMessage3 = new Message_3D("Pass:", 0, 1.0f, 51);
                    this.LoginMessage3.show(190.0f, 300.0f);
                    this.SubButtonList.add(new Button_3D(527, 218, "FORGOT", "Forgot Password", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 218, "CHANGE", "Change Password", 1, 51));
                    this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Login", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 270, 51, "top"));
                    this.SubButtonList.add(new Button_Bar(300, 250, 300, 51, "bottom"));
                    this.SubButtonList.showAll();
                    return;
                }
                if (string2.equalsIgnoreCase("SINGLE")) {
                    Main.MainRef.network.UserName = "";
                    this.TopLine = 0;
                    this.InternalMenuState = 6;
                    this.PopupMenu = new PopUp(400, 300, "Enter Your Name", true);
                    this.updateLoginMenuText();
                    this.LoginMessage2 = new Message_3D("Name:", 0, 1.0f, 51);
                    this.LoginMessage2.show(190.0f, 270.0f);
                    this.SubButtonList.add(new Button_3D(527, 412, "LOGIN", "Begin", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 270, 51, "top"));
                    this.SubButtonList.showAll();
                    return;
                }
                if (string2.equalsIgnoreCase("GUEST")) {
                    Main.MainRef.network.UserName = "`guest`0";
                    this.TopLine = 0;
                    this.InternalMenuState = 20;
                    this.PopupMenu = new PopUp(400, 300, "Terms Of Service", true);
                    this.LoginMessage2 = new Message_3D("I confirm that I am 13 years", 1, 1.0f, 51);
                    this.LoginMessage2.show(400.0f, 270.0f);
                    this.LoginMessage3 = new Message_3D("of age or older, and I agree to", 1, 1.0f, 51);
                    this.LoginMessage3.show(400.0f, 295.0f);
                    this.LoginMessage4 = new Message_3D("the Terms Of Service.", 1, 1.0f, 51);
                    this.LoginMessage4.show(400.0f, 320.0f);
                    this.SubButtonList.add(new Button_3D(400, 372, "TOS", "View Terms Of Service", 1, 52));
                    this.SubButtonList.add(new Button_3D(527, 412, "CONFIRM", "Confirm", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.showAll();
                    return;
                }
                if (string2.equalsIgnoreCase("CREATE")) {
                    Main.MainRef.network.Password = "";
                    Main.MainRef.network.UserName = "";
                    this.TopLine = 0;
                    this.InternalMenuState = 10;
                    this.PopupMenu = new PopUp(400, 300, "Terms Of Service", true);
                    this.LoginMessage2 = new Message_3D("I confirm that I am 13 years", 1, 1.0f, 51);
                    this.LoginMessage2.show(400.0f, 270.0f);
                    this.LoginMessage3 = new Message_3D("of age or older, and I agree to", 1, 1.0f, 51);
                    this.LoginMessage3.show(400.0f, 295.0f);
                    this.LoginMessage4 = new Message_3D("the Terms Of Service.", 1, 1.0f, 51);
                    this.LoginMessage4.show(400.0f, 320.0f);
                    this.SubButtonList.add(new Button_3D(400, 372, "TOS", "View Terms Of Service", 1, 52));
                    this.SubButtonList.add(new Button_3D(527, 412, "CONFIRM", "Confirm", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.showAll();
                }
                if (string2.startsWith("OPTI")) {
                    int n5 = Float.valueOf(string2.substring(4, string2.length())).intValue();
                    Main.MainRef.changeSettings(n5);
                    this.ButtonList.remove(this.OptionsMenu);
                    this.OptionsMenu = null;
                    String[] stringArray = new String[10];
                    int n6 = Main.MainRef.getSettings(stringArray);
                    this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n6, stringArray, "Options", "OPTI", 0));
                    this.ButtonList.showAll();
                    return;
                }
            }
        } else if (this.InternalMenuState == 80) {
            String string3 = this.SubButtonList.update(n, n2, n3);
            if (string3 != null) {
                if (string3.equalsIgnoreCase("YES")) {
                    Main.MainRef.launchTutorial();
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
                if (string3.equalsIgnoreCase("NO")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
            }
        } else if (this.InternalMenuState == 1) {
            String string4 = this.SubButtonList.update(n, n2, n3);
            if (string4 != null) {
                if (string4.equalsIgnoreCase("top")) {
                    this.TopLine = 0;
                }
                if (string4.equalsIgnoreCase("bottom")) {
                    this.TopLine = 1;
                }
                if (string4.equalsIgnoreCase("LOGIN")) {
                    this.KeyDown(13);
                    return;
                }
                if (string4.equalsIgnoreCase("FORGOT")) {
                    Main.MainRef.launchForgot();
                    return;
                }
                if (string4.equalsIgnoreCase("CHANGE")) {
                    Main.MainRef.launchChange();
                    return;
                }
                if (string4.equalsIgnoreCase("CANCEL")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
            }
        } else if (this.InternalMenuState == 4) {
            String string5 = this.SubButtonList.update(n, n2, n3);
            if (string5 != null) {
                if (string5.equalsIgnoreCase("PRIVACY")) {
                    Main.MainRef.launchPrivacy();
                    return;
                }
                if (string5.equalsIgnoreCase("top")) {
                    this.TopLine = 0;
                }
                if (string5.equalsIgnoreCase("bottom")) {
                    this.TopLine = 1;
                }
                if (string5.equalsIgnoreCase("name")) {
                    this.TopLine = 2;
                }
                if (string5.equalsIgnoreCase("CREATE")) {
                    this.KeyDown(13);
                    return;
                }
                if (string5.equalsIgnoreCase("CANCEL")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
            }
        } else if (this.InternalMenuState == 6 || this.InternalMenuState == 11) {
            String string6 = this.SubButtonList.update(n, n2, n3);
            if (string6 != null) {
                if (string6.equalsIgnoreCase("top")) {
                    this.TopLine = 0;
                }
                if (string6.equalsIgnoreCase("LOGIN")) {
                    this.KeyDown(13);
                    return;
                }
                if (string6.equalsIgnoreCase("CANCEL")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
            }
        } else if (this.InternalMenuState == 10) {
            String string7 = this.SubButtonList.update(n, n2, n3);
            if (string7 != null) {
                if (string7.equalsIgnoreCase("CONFIRM")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    Main.MainRef.network.Password = "";
                    this.TopLine = 0;
                    this.InternalMenuState = 4;
                    this.PopupMenu = new PopUp(400, 300, "Create New User", true);
                    this.updateLoginMenuText();
                    this.LoginMessage2 = new Message_3D("Email:", 0, 1.0f, 51);
                    this.LoginMessage2.show(190.0f, 270.0f);
                    this.LoginMessage3 = new Message_3D("Pass:", 0, 1.0f, 51);
                    this.LoginMessage3.show(190.0f, 300.0f);
                    this.LoginMessage5 = new Message_3D("Name:", 0, 1.0f, 51);
                    this.LoginMessage5.show(190.0f, 330.0f);
                    this.SubButtonList.add(new Button_3D(400, 240, "PRIVACY", "Privacy Policy", 1, 52));
                    this.SubButtonList.add(new Button_3D(527, 412, "CREATE", "Create", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 270, 51, "top"));
                    this.SubButtonList.add(new Button_Bar(300, 250, 300, 51, "bottom"));
                    this.SubButtonList.add(new Button_Bar(300, 250, 330, 51, "name"));
                    this.SubButtonList.showAll();
                    return;
                }
                if (string7.equalsIgnoreCase("TOS")) {
                    Main.MainRef.launchTOS();
                }
                if (string7.equalsIgnoreCase("CANCEL")) {
                    this.SubButtonList.destroy();
                    this.InternalMenuState = 0;
                    this.PopupMenu.destroy();
                    this.PopupMenu = null;
                    if (this.LoginMessage1 != null) {
                        this.LoginMessage1.destroy();
                    }
                    if (this.LoginMessage2 != null) {
                        this.LoginMessage2.destroy();
                    }
                    if (this.LoginMessage3 != null) {
                        this.LoginMessage3.destroy();
                    }
                    if (this.LoginMessage4 != null) {
                        this.LoginMessage4.destroy();
                    }
                    if (this.LoginMessage5 != null) {
                        this.LoginMessage5.destroy();
                    }
                    this.LoginMessage1 = null;
                    this.LoginMessage2 = null;
                    this.LoginMessage3 = null;
                    this.LoginMessage4 = null;
                    this.LoginMessage5 = null;
                    if (this.LoginUser != null) {
                        this.LoginUser.destroy();
                    }
                    this.LoginUser = null;
                    if (this.LoginName != null) {
                        this.LoginName.destroy();
                    }
                    this.LoginName = null;
                    if (this.LoginPass != null) {
                        this.LoginPass.destroy();
                    }
                    this.LoginPass = null;
                    return;
                }
            }
        } else if (this.InternalMenuState == 20 && (string = this.SubButtonList.update(n, n2, n3)) != null) {
            if (string.equalsIgnoreCase("CONFIRM")) {
                this.SubButtonList.destroy();
                if (this.LoginMessage1 != null) {
                    this.LoginMessage1.destroy();
                }
                if (this.LoginMessage2 != null) {
                    this.LoginMessage2.destroy();
                }
                if (this.LoginMessage3 != null) {
                    this.LoginMessage3.destroy();
                }
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                if (this.LoginMessage5 != null) {
                    this.LoginMessage5.destroy();
                }
                this.LoginMessage1 = null;
                this.LoginMessage2 = null;
                this.LoginMessage3 = null;
                this.LoginMessage4 = null;
                this.LoginMessage5 = null;
                if (this.LoginUser != null) {
                    this.LoginUser.destroy();
                }
                this.LoginUser = null;
                if (this.LoginName != null) {
                    this.LoginName.destroy();
                }
                this.LoginName = null;
                if (this.LoginPass != null) {
                    this.LoginPass.destroy();
                }
                this.LoginPass = null;
                this.TopLine = 0;
                this.InternalMenuState = 11;
                this.KeyDown(13);
                return;
            }
            if (string.equalsIgnoreCase("TOS")) {
                Main.MainRef.launchTOS();
            }
            if (string.equalsIgnoreCase("CANCEL")) {
                this.SubButtonList.destroy();
                this.InternalMenuState = 0;
                this.PopupMenu.destroy();
                this.PopupMenu = null;
                if (this.LoginMessage1 != null) {
                    this.LoginMessage1.destroy();
                }
                if (this.LoginMessage2 != null) {
                    this.LoginMessage2.destroy();
                }
                if (this.LoginMessage3 != null) {
                    this.LoginMessage3.destroy();
                }
                if (this.LoginMessage4 != null) {
                    this.LoginMessage4.destroy();
                }
                if (this.LoginMessage5 != null) {
                    this.LoginMessage5.destroy();
                }
                this.LoginMessage1 = null;
                this.LoginMessage2 = null;
                this.LoginMessage3 = null;
                this.LoginMessage4 = null;
                this.LoginMessage5 = null;
                if (this.LoginUser != null) {
                    this.LoginUser.destroy();
                }
                this.LoginUser = null;
                if (this.LoginName != null) {
                    this.LoginName.destroy();
                }
                this.LoginName = null;
                if (this.LoginPass != null) {
                    this.LoginPass.destroy();
                }
                this.LoginPass = null;
                return;
            }
        }
    }

    void attemptCreate() {
        if (!Main.MainRef.network.UserName.equalsIgnoreCase(Text.doFilter(Main.MainRef.network.UserName))) {
            if (this.LoginMessage4 != null) {
                this.LoginMessage4.destroy();
            }
            this.LoginMessage4 = null;
            if (Main.MainRef.network.UserName.startsWith("`guest`")) {
                Main.MainRef.network.UserName = Main.MainRef.network.UserName.substring(7, Main.MainRef.network.UserName.length());
            }
            this.LoginMessage4 = new Message_3D("Error : Unacceptable name", 0, 0.75f, 55);
            this.LoginMessage4.show(250.0f, 360.0f);
            this.InternalMenuState = 4;
            if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
            }
            return;
        }
        if (this.LoginMessage4 != null) {
            this.LoginMessage4.destroy();
        }
        this.LoginMessage4 = new Message_3D("Creating User...", 0, 0.75f, 55);
        this.LoginMessage4.show(250.0f, 360.0f);
        Main.MainRef.network.createUMSName(Main.MainRef.network.UserEmail, Main.MainRef.network.Password, Main.MainRef.network.UserName);
        this.InternalMenuState = 5;
    }

    void KeyDown(int n) {
        if (this.InternalMenuState == 0) {
            if (n == 70) {
                Main.MainRef.wt_stage.toggleFullscreen();
                return;
            }
        } else if (this.InternalMenuState == 1 || this.InternalMenuState == 4 || this.InternalMenuState == 6 || this.InternalMenuState == 11) {
            this.keyDown(n);
        }
    }
}

