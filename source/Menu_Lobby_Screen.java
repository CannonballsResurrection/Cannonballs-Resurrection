/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTDrop
 */
import java.util.StringTokenizer;
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTDrop;

public class Menu_Lobby_Screen
implements Global,
WTConstants {
    boolean CannonVisible = false;
    int InternalMenuState = 0;
    int CannonColor = 0;
    WTBitmap BackgroundJPG;
    WTBitmap CreateBackgroundJPG;
    WTDrop BackDrop;
    Message_3D Version;
    Message_3D TitleChat;
    Message_3D TitlePlayers;
    Message_3D TitleRoom;
    Message_3D MyName;
    Message_3D ChatEntry;
    Message_3D ErrorMessage1;
    Message_3D ErrorMessage2;
    Message_3D[] TempMessages = new Message_3D[30];
    Message_3D ChatHistory;
    Message_3D[] UserNames = new Message_3D[40];
    Message_3D[][] GameNames = new Message_3D[40][6];
    PopUp PopupMenu;
    Button_3DList ButtonList = new Button_3DList();
    Button_3DList SubButtonList = new Button_3DList();
    Button_3DList IconList = new Button_3DList();
    Button_3DDropColor ColorButton;
    Button_3D MuteButton;
    Button_3D OptionsMenu;
    Button_Static Thumbnail;
    Scroll_Bar GameBar;
    Scroll_Bar UserBar;
    Scroll_Bar ChatBar;
    boolean Visible = false;
    boolean ButtonDown = false;
    boolean ShiftDown = false;
    boolean CursorVisible = false;
    float BlinkCount = 0.0f;
    int TeamSelection = 0;
    int HotSeatSelection = 0;
    int RespawnSelection = 2;
    int StartingCashSelection = 4;
    int TreasureRespawnSelection = 1;
    Tips DailyTips;
    String LastChatHistory;
    String[] LastLobbyWindow = new String[40];
    String[] LastPlayerWindow = new String[40];
    int[] LastClientIn = new int[40];
    int LastGameCount = 0;
    int LastPlayerCount = 0;
    boolean[] ButtonRolloverActive = new boolean[100];
    boolean[] TeamButtonRolloverActive = new boolean[4];
    boolean ClientsCheckedIn = false;
    String ChatMessage = "";
    int LastX = 0;
    int LastY = 0;
    int GameTopLine = 0;
    int PlayerTopLine = 0;
    int ChatTopLine = -999;
    int ChatLines = 11;
    boolean HoldGameUp = false;
    boolean HoldGameDown = false;
    boolean HoldPlayerUp = false;
    boolean HoldPlayerDown = false;
    boolean HoldChatUp = false;
    boolean HoldChatDown = false;
    int GameNumber = 0;
    float RoomRequestTimer = 0.0f;
    float ChatDelayTimer = 10.0f;
    String Address = "";
    String[] MuteList = new String[400];
    int MuteCount = 0;
    boolean GuestChatMuted = false;

    void createNewLobbyGame() {
        Main.MainRef.network.TeamCount = Global.TEAMCOUNT[this.TeamSelection];
        Main.MainRef.network.TeamGame = false;
        if (Main.MainRef.network.TeamCount > 1) {
            Main.MainRef.network.TeamGame = true;
        }
        if (Main.MainRef.network.createNewMatch(Main.MainRef.network.MyIP + "|" + Main.MainRef.network.UserName + ":" + Main.MainRef.ActiveMap + ":" + "1/" + Main.MainRef.MaxGamePlayerCount + ":" + Global.HOTSEATNAMES[this.HotSeatSelection] + ":" + this.RespawnSelection + ":" + Main.MainRef.network.TeamCount, Main.MainRef.MaxGamePlayerCount)) {
            if (Main.MainRef.network.TeamGame) {
                this.hideAll();
                this.showCreateJoinMenu();
                this.InternalMenuState = 2;
            } else {
                this.hideAll();
                this.showCreateJoinMenu();
                this.InternalMenuState = 2;
            }
            this.showMasterJoinMembers();
        }
    }

    void removeMuteName(String string) {
        int n = 0;
        while (n < this.MuteCount) {
            if (this.MuteList[n].equalsIgnoreCase(string)) {
                this.MuteList[n] = this.MuteList[this.MuteCount - 1];
                this.MuteCount += -1;
                return;
            }
            ++n;
        }
    }

    void hideCreateMenu() {
        if (this.ErrorMessage1 != null) {
            this.ErrorMessage1.destroy();
        }
        this.ErrorMessage1 = null;
        if (this.ErrorMessage2 != null) {
            this.ErrorMessage2.destroy();
        }
        this.ErrorMessage2 = null;
        if (this.Thumbnail != null) {
            this.Thumbnail.destroy();
        }
        this.Thumbnail = null;
        this.clearTempMessages();
        this.PopupMenu.destroy();
        this.PopupMenu = null;
        this.SubButtonList.destroy();
    }

    void clearMuteList() {
        this.MuteCount = 0;
    }

    void showConnectMenu() {
        this.SubButtonList.destroy();
        this.clearTempMessages();
        if (this.PopupMenu != null) {
            this.PopupMenu.destroy();
        }
        this.PopupMenu = null;
        this.PopupMenu = new PopUp(400, 300, "Connect to Game", true);
        this.TempMessages[0] = new Message_3D("Attempting To Connect", 1, 1.0f, 51);
        this.TempMessages[0].show(400.0f, 380.0f);
        Main.MainRef.MenuManager.showLoading();
        this.SubButtonList.add(new Button_3D(400, 412, "Cancel", "CANCEL", 1, 51));
        this.SubButtonList.showAll();
    }

    void showErrorMenu() {
        this.SubButtonList.destroy();
        Main.MainRef.packetmanager.closeForPackets();
        this.clearTempMessages();
        if (this.PopupMenu != null) {
            this.PopupMenu.destroy();
        }
        this.PopupMenu = null;
        this.PopupMenu = new PopUp(400, 300, "Error!", true);
        this.TempMessages[0] = new Message_3D("Connection Failure!", 1, 1.0f, 51);
        this.TempMessages[0].show(400.0f, 280.0f);
        this.TempMessages[1] = new Message_3D("The connection may have been blocked by a firewall,", 1, 0.75f, 51);
        this.TempMessages[1].show(400.0f, 310.0f);
        this.TempMessages[2] = new Message_3D("or the creator may have terminated the game.", 1, 0.75f, 51);
        this.TempMessages[2].show(400.0f, 334.0f);
        this.SubButtonList.add(new Button_3D(400, 412, "OK", "OK", 1, 52));
        this.SubButtonList.add(new Button_3D(400, 382, "HELP", "Help!", 1, 52));
        this.SubButtonList.showAll();
    }

    void hide() {
        if (this.Visible) {
            this.hideAll();
            Main.MainRef.network.nullChat();
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.CameraView.removeDrop(this.BackDrop);
            this.BackgroundJPG.destroy();
            this.BackgroundJPG = null;
            this.CreateBackgroundJPG.destroy();
            this.CreateBackgroundJPG = null;
            this.Visible = false;
        }
    }

    void addMuteName(String string) {
        if (this.MuteCount < 400) {
            this.MuteList[this.MuteCount] = string;
            ++this.MuteCount;
        }
    }

    void updateTimeSlice(float f) {
        this.ChatDelayTimer += f / 1000.0f;
        Main.MainRef.network.pollNetContext(10L);
        this.BlinkCount += f;
        if (this.BlinkCount > 900.0f) {
            this.BlinkCount = 0.0f;
            boolean bl = this.CursorVisible = !this.CursorVisible;
            if (!Main.MainRef.SinglePlayer) {
                this.updateLobbyChatText();
            }
            if (this.InternalMenuState == 8) {
                this.updateEmailMenuText();
            }
        }
        if (this.InternalMenuState == 0 || this.InternalMenuState == 1 || this.InternalMenuState == 6 || this.InternalMenuState == 8) {
            this.checkLobbyUpdate();
        }
        if (this.InternalMenuState == 0 || this.InternalMenuState == 4 || this.InternalMenuState == 2) {
            if (this.HoldPlayerUp) {
                this.PlayerTopLine += -1;
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldPlayerUp = true;
            }
            if (this.HoldPlayerDown) {
                ++this.PlayerTopLine;
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldPlayerDown = true;
            }
            if (this.HoldGameUp) {
                this.GameTopLine += -1;
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldGameUp = true;
            }
            if (this.HoldGameDown) {
                ++this.GameTopLine;
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldGameDown = true;
            }
            if (this.HoldChatUp) {
                this.ChatTopLine += -1;
                if (this.InternalMenuState == 0) {
                    this.updateLobbyChatWindow();
                } else {
                    this.updateLobbyChatWindowJoin();
                }
                this.updateDragBarLocation();
                this.HoldChatUp = true;
            }
            if (this.HoldChatDown) {
                ++this.ChatTopLine;
                if (this.InternalMenuState == 0) {
                    this.updateLobbyChatWindow();
                } else {
                    this.updateLobbyChatWindowJoin();
                }
                this.updateDragBarLocation();
                this.HoldChatDown = true;
                return;
            }
        } else if (this.InternalMenuState == 6) {
            int n = Main.MainRef.network.joinGame(this.GameNumber);
            if (n == 1) {
                this.hideAll();
                this.showJoinMenu();
                Main.MainRef.MenuManager.hideLoading();
                this.InternalMenuState = 4;
                return;
            }
            if (n == 0) {
                this.showErrorMenu();
                Main.MainRef.MenuManager.hideLoading();
                this.InternalMenuState = 7;
                return;
            }
            Main.MainRef.MenuManager.updateLoading(f);
        }
    }

    void cancelGameCreation() {
        if (!Main.MainRef.SinglePlayer) {
            Main.MainRef.network.setUserState(0);
            Main.MainRef.network.notifyJoinersOfCancel();
        }
        Main.MainRef.GameLoop.StartupDataReceived = false;
        Main.MainRef.network.GameCreationCompleted = false;
        Main.MainRef.network.nullCheckIn();
        Main.MainRef.network.nullSlots();
        Main.MainRef.network.CreatedGame = false;
        Main.MainRef.network.destroyMatch();
        this.InternalMenuState = 0;
    }

    void checkLobbyUpdate() {
        if (Main.MainRef.network.isRoomInfoUpdated()) {
            Main.MainRef.network.setRoomInfoUpdated(false);
            this.updateLobbyGames();
            this.updateLobbyChatWindow();
        }
    }

    void showBaseMenu(boolean bl) {
        String[] stringArray;
        Main.MainRef.network.zeroStats();
        Main.MainRef.network.setHasLivePlayers(false);
        this.ButtonList.destroy();
        if (!Main.MainRef.SinglePlayer) {
            Main.MainRef.network.setUserState(0);
            Main.MainRef.packetmanager.closeForPackets();
            this.ChatTopLine = -999;
            this.clearHistory();
            this.TitleChat = new Message_3D("Chat", 0, 1.0f);
            this.TitleChat.show(14.0f, 384.0f);
            this.TitlePlayers = new Message_3D("Players:1", 0, 1.0f);
            this.TitlePlayers.show(632.0f, 82.0f);
            this.TitleRoom = new Message_3D("Open Games in '" + Main.MainRef.network.MasterRooms[Main.MainRef.network.ActiveRoomNumber].roomName + "'", 0, 1.0f);
            this.TitleRoom.show(14.0f, 82.0f);
            stringArray = Main.MainRef.TextManager.wordWrapTrim("Name:" + Main.MainRef.network.UserName, 232.0f, 1.0f);
            this.MyName = new Message_3D((String)stringArray, 0, 1.0f);
            this.MyName.show(536.0f, 16.0f);
            this.ButtonList.add(new Button_3D(400, 48, "CREATE", "Begin a New Game", 1));
            this.MuteButton = this.GuestChatMuted ? this.ButtonList.add(new Button_3D(400, 383, "GMUTE", "UnMute Guest Chat", 1)) : this.ButtonList.add(new Button_3D(400, 383, "GMUTE", "Mute Guest Chat", 1));
            String[] stringArray2 = new String[]{"I Am Online", "I Am Away"};
            this.ButtonList.add(new Button_3DDrop(665, 48, 2, stringArray2, "PLAY", 0));
            String[] stringArray3 = new String[Main.MainRef.network.MasterRoomCount];
            int n = 0;
            while (n < Main.MainRef.network.MasterRoomCount) {
                stringArray3[n] = Main.MainRef.network.MasterRooms[n].roomName;
                ++n;
            }
            if (Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`")) {
                Button_3DDrop button_3DDrop = (Button_3DDrop)this.ButtonList.add(new Button_3DDrop(135, 48, Main.MainRef.network.MasterRoomCount - 1, stringArray3, "ROOM", 0));
                button_3DDrop.setSelection(Main.MainRef.network.ActiveRoomNumber);
            } else {
                Button_3DDrop button_3DDrop = (Button_3DDrop)this.ButtonList.add(new Button_3DDrop(135, 48, Main.MainRef.network.MasterRoomCount, stringArray3, "ROOM", 0));
                button_3DDrop.setSelection(Main.MainRef.network.ActiveRoomNumber);
            }
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 595, 101, "GAMEUP", 0));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 595, 332, "GAMEDOWN", 0));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 765, 101, "PLAYERUP", 0));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 765, 332, "PLAYERDOWN", 0));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 765, 400, "CHATUP", 0));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 765, 546, "CHATDOWN", 0));
            this.GameBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(595, 127, 205, 8, 0));
            this.UserBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(765, 127, 205, 13, 0));
            this.ChatBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(765, 427, 119, 11, 0));
            this.updateDragBarLocation();
            this.GameNames[39][5] = new Message_3D("Creator", 0, 0.75f);
            this.GameNames[39][5].show(10.0f, 112.0f);
            this.GameNames[39][0] = new Message_3D("Map", 0, 0.75f);
            this.GameNames[39][0].show(130.0f, 112.0f);
            this.GameNames[39][1] = new Message_3D("Players", 1, 0.75f);
            this.GameNames[39][1].show(280.0f, 112.0f);
            this.GameNames[39][2] = new Message_3D("Hotseat", 1, 0.75f);
            this.GameNames[39][2].show(350.0f, 112.0f);
            this.GameNames[39][3] = new Message_3D("Lives", 1, 0.75f);
            this.GameNames[39][3].show(415.0f, 112.0f);
            this.GameNames[39][4] = new Message_3D("TeamPlay", 1, 0.75f);
            this.GameNames[39][4].show(480.0f, 112.0f);
            if (bl) {
                Main.MainRef.network.receiveChatText("`SYSTEM:`" + this.DailyTips.getRandomTip());
            }
            this.updateLobbyGames();
            this.updateLobbyChatWindow();
            this.updateLobbyChatText();
        }
        this.BackDrop.detach();
        this.BackDrop.attach(this.BackgroundJPG);
        stringArray = new String[10];
        stringArray[0] = "Exit Lobby";
        stringArray[1] = "Quit Game";
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 0));
        this.ButtonList.add(new Button_3DMenu(10, 14, 2, stringArray, "Quit", "QUIT", 34));
        int n = Main.MainRef.getSettingsWithStats(stringArray);
        this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n, stringArray, "Options", "OPTI", 34));
        stringArray[0] = "Getting Started";
        stringArray[1] = "The Lobby";
        stringArray[2] = "Tutorial";
        this.ButtonList.add(new Button_3DMenu(165, 14, 3, stringArray, "Help!", "HELP", 34));
        stringArray[0] = "Invite a Friend!";
        if (!Main.MainRef.SinglePlayer && !Main.MainRef.DPName.equalsIgnoreCase("shockwave")) {
            this.ButtonList.add(new Button_3DMenu(230, 14, 1, stringArray, "E-Mail", "EMAI", 34));
        }
        this.ButtonList.showAll();
        this.Version = new Message_3D(Main.MainRef.VersionNumber, 1, 0.75f);
        this.Version.show(400.0f, 16.0f);
        this.InternalMenuState = 0;
    }

    void showCreateJoinMenu() {
        Main.MainRef.chat.clearChat();
        this.ChatTopLine = -999;
        this.clearHistory();
        this.BackDrop.detach();
        this.BackDrop.attach(this.CreateBackgroundJPG);
        this.TitleRoom = new Message_3D("Joining Players", 0, 1.0f);
        this.TitleRoom.show(14.0f, 82.0f);
        if (!this.CannonVisible) {
            if (Main.MainRef.GlobalMedia.CannonTex.Shader.getNumLayers() < 2) {
                Main.MainRef.GlobalMedia.CannonTex.Shader.setNumLayers(2);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setTexture(1, Main.MainRef.GlobalMedia.Reflection.Image);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setLayerType(1, 4);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setLayerSource(1, 2);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setTextureCoordGenMethod(1, 11);
            }
            Main.MainRef.camera.Camera.addObject((WTContainer)Main.MainRef.GlobalMedia.CannonGroup);
            Main.MainRef.GlobalMedia.CannonBarrelActor.Model.setSurfaceShader(Main.MainRef.GlobalMedia.CannonTex.Shader);
            Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
            Main.MainRef.GlobalMedia.CannonGroup.setPosition(8.208f, 4.0f, 30.0f);
            Main.MainRef.GlobalMedia.CannonGroup.setOrientation(0.0f, 1.0f, 0.0f, -135.0f);
            Main.MainRef.GlobalMedia.CannonBarrelActor.Model.setOrientation(1.0f, 0.0f, 0.0f, -20.0f);
            Main.MainRef.GlobalMedia.CannonGroup.setConstantRotation(0.0f, 1.0f, 0.0f, 50.0f);
            this.CannonVisible = true;
        }
        this.ChatBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(765, 427, 119, 11, 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 765, 400, "CHATUP", 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 765, 546, "CHATDOWN", 0));
        this.ButtonList.add(new Button_3D(610, 70, "CANCEL", "Abandon This Game", 1));
        if (Main.MainRef.network.TeamGame) {
            this.ColorButton = (Button_3DDropColor)this.ButtonList.add(new Button_3DDropColor(610, 320, Main.MainRef.network.TeamCount, Global.COLORRGB, "Team"));
            this.ColorButton.setSelection(this.CannonColor);
        } else {
            this.ColorButton = (Button_3DDropColor)this.ButtonList.add(new Button_3DDropColor(610, 320, 4, Global.COLORRGB, "Color"));
            this.ColorButton.setSelection(this.CannonColor);
        }
        this.ButtonList.showAll();
        this.Version = new Message_3D(Main.MainRef.VersionNumber, 1, 1.0f);
        this.Version.show(400.0f, 16.0f);
    }

    void hideJoinMenu() {
    }

    void clearTempMessages() {
        int n = 0;
        do {
            if (this.TempMessages[n] != null) {
                this.TempMessages[n].destroy();
            }
            this.TempMessages[n] = null;
        } while (++n < 30);
    }

    boolean joinGame(int n) {
        this.GameNumber = n;
        if (Main.MainRef.network.Connected) {
            String string;
            String string2 = Main.MainRef.network.GameNames[n];
            StringTokenizer stringTokenizer = new StringTokenizer(string2, ":");
            String string3 = string = stringTokenizer.nextToken();
            string = stringTokenizer.nextToken();
            string = stringTokenizer.nextToken();
            string = stringTokenizer.nextToken();
            string = stringTokenizer.nextToken();
            Main.MainRef.network.TeamCount = Float.valueOf(stringTokenizer.nextToken()).intValue();
            Main.MainRef.network.TeamGame = false;
            if (Main.MainRef.network.TeamCount > 1) {
                Main.MainRef.network.TeamGame = true;
            }
            return true;
        }
        return false;
    }

    void processMouse(int n, int n2, int n3) {
        float f;
        int n4;
        this.LastX = n;
        this.LastY = n2;
        if (this.InternalMenuState == 0 && (n3 & 1) == 1) {
            if (n > 595 && n2 > 127 && n < 623 && n2 < 331) {
                n4 = Main.MainRef.network.NumGames - 8;
                if (n4 < 1) {
                    n4 = 1;
                }
                f = n2 - 127;
                f /= 200.0f;
                this.GameTopLine = (int)(f *= (float)n4);
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldPlayerUp = false;
                this.HoldPlayerDown = false;
                this.HoldGameUp = false;
                this.HoldGameDown = false;
                this.HoldChatUp = false;
                this.HoldChatDown = false;
            }
            if (n > 765 && n2 > 127 && n < 792 && n2 < 331) {
                n4 = Main.MainRef.network.NumClients - 13;
                if (n4 < 1) {
                    n4 = 1;
                }
                f = n2 - 127;
                f /= 200.0f;
                this.PlayerTopLine = (int)(f *= (float)n4);
                this.updateLobbyGames();
                this.updateDragBarLocation();
                this.HoldPlayerUp = false;
                this.HoldPlayerDown = false;
                this.HoldGameUp = false;
                this.HoldGameDown = false;
                this.HoldChatUp = false;
                this.HoldChatDown = false;
            }
        }
        if ((this.InternalMenuState == 0 || this.InternalMenuState == 4 || this.InternalMenuState == 2) && (n3 & 1) == 1 && n > 765 && n2 > 428 && n < 792 && n2 < 546) {
            n4 = this.InternalMenuState == 0 ? this.ChatLines - 11 : Main.MainRef.chat.ChatLines - 11;
            if (n4 < 1) {
                n4 = 1;
            }
            f = n2 - 428;
            f /= 60.0f;
            this.ChatTopLine = (int)(f *= (float)n4);
            if (this.InternalMenuState == 0) {
                this.updateLobbyChatWindow();
            } else {
                this.updateLobbyChatWindowJoin();
            }
            this.updateDragBarLocation();
            this.HoldPlayerUp = false;
            this.HoldPlayerDown = false;
            this.HoldGameUp = false;
            this.HoldGameDown = false;
            this.HoldChatUp = false;
            this.HoldChatDown = false;
        }
        if ((n3 & 1) == 1) {
            if (this.ButtonDown) {
                n3 = 0;
            } else {
                this.ButtonDown = true;
            }
        } else if ((n3 & 1) != 1) {
            this.HoldPlayerUp = false;
            this.HoldPlayerDown = false;
            this.HoldGameUp = false;
            this.HoldGameDown = false;
            this.HoldChatUp = false;
            this.HoldChatDown = false;
            this.ButtonDown = false;
            n3 = 0;
        }
        switch (this.InternalMenuState) {
            case 8: {
                String string = this.SubButtonList.update(n, n2, n3);
                if (string == null) break;
                if (string.equalsIgnoreCase("SEND")) {
                    if (this.Address.length() > 0) {
                        this.sendEmail();
                    }
                    return;
                }
                if (!string.equalsIgnoreCase("CANCEL")) break;
                this.InternalMenuState = 0;
                this.hideAll();
                this.showBaseMenu(false);
                return;
            }
            case 0: {
                String string = this.IconList.update(n, n2, n3);
                if (string != null) {
                    if (string.startsWith("MUTE")) {
                        String string2 = string.substring(4, string.length());
                        if (!string2.equalsIgnoreCase(Main.MainRef.network.UserName)) {
                            this.addMuteName(string2);
                            this.updateLobbyGames(true, false);
                        }
                        return;
                    }
                    if (string.startsWith("UNMUTE")) {
                        String string3 = string.substring(6, string.length());
                        if (!string3.equalsIgnoreCase(Main.MainRef.network.UserName)) {
                            this.removeMuteName(string3);
                            this.updateLobbyGames(true, false);
                        }
                        return;
                    }
                }
                if ((string = this.ButtonList.update(n, n2, n3)) == null) break;
                if (string.startsWith("GMUTE")) {
                    this.GuestChatMuted = !this.GuestChatMuted;
                    this.ButtonList.remove(this.MuteButton);
                    this.MuteButton = this.GuestChatMuted ? this.ButtonList.add(new Button_3D(400, 383, "GMUTE", "UnMute Guest Chat", 1)) : this.ButtonList.add(new Button_3D(400, 383, "GMUTE", "Mute Guest Chat", 1));
                    this.ButtonList.showAll();
                }
                if (string.startsWith("EMAI")) {
                    this.PopupMenu = new PopUp(400, 300, "Invite A Friend Via E-Mail", true);
                    this.Address = "";
                    this.updateEmailMenuText();
                    this.TempMessages[0] = new Message_3D("e-mail:", 0, 1.0f, 51);
                    this.TempMessages[0].show(180.0f, 300.0f);
                    this.SubButtonList.add(new Button_3D(527, 412, "SEND", "Send", 1, 52));
                    this.SubButtonList.add(new Button_3D(273, 412, "CANCEL", "Cancel", 1, 51));
                    this.SubButtonList.add(new Button_Bar(300, 250, 300, 51, null));
                    this.SubButtonList.showAll();
                    this.InternalMenuState = 8;
                    return;
                }
                if (string.startsWith("OPTI")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    if (n4 == 0) {
                        Main.MainRef.MenuManager.activateMenu(4);
                        return;
                    }
                    Main.MainRef.changeSettings(n4 - 1);
                    this.ButtonList.remove(this.OptionsMenu);
                    this.OptionsMenu = null;
                    String[] stringArray = new String[10];
                    int n5 = Main.MainRef.getSettingsWithStats(stringArray);
                    this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n5, stringArray, "Options", "OPTI", 34));
                    this.ButtonList.showAll();
                    return;
                }
                if (string.equalsIgnoreCase("CHATUP")) {
                    this.ChatTopLine += -1;
                    this.updateLobbyChatWindow();
                    this.updateDragBarLocation();
                    this.HoldChatUp = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CHATDOWN")) {
                    ++this.ChatTopLine;
                    this.updateLobbyChatWindow();
                    this.updateDragBarLocation();
                    this.HoldChatDown = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("PLAYERUP")) {
                    this.PlayerTopLine += -1;
                    this.updateLobbyGames();
                    this.updateDragBarLocation();
                    this.HoldPlayerUp = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("PLAYERDOWN")) {
                    ++this.PlayerTopLine;
                    this.updateLobbyGames();
                    this.updateDragBarLocation();
                    this.HoldPlayerDown = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("GAMEUP")) {
                    this.GameTopLine += -1;
                    this.updateLobbyGames();
                    this.updateDragBarLocation();
                    this.HoldGameUp = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("GAMEDOWN")) {
                    ++this.GameTopLine;
                    this.updateLobbyGames();
                    this.updateDragBarLocation();
                    this.HoldGameDown = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CREATE")) {
                    this.InternalMenuState = 1;
                    this.showCreateMenu();
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("FULLSCREEN")) {
                    Main.MainRef.wt_stage.toggleFullscreen();
                    return;
                }
                if (string.startsWith("QUIT")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    if (n4 == 0) {
                        Main.MainRef.network.leaveLobby();
                        Main.MainRef.MenuManager.activateMenu(3);
                        return;
                    }
                    Main.MainRef.close();
                    return;
                }
                if (string.startsWith("HELP")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    if (n4 == 0) {
                        Main.MainRef.launchHelpPage("gettingstarted.htm");
                        return;
                    }
                    if (n4 == 1) {
                        Main.MainRef.launchHelpPage("gamemenu.htm");
                        return;
                    }
                    Main.MainRef.launchTutorial();
                    return;
                }
                if (string.startsWith("PLAY")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    if (n4 == 1) {
                        n4 = 2;
                    }
                    Main.MainRef.network.setUserState(n4);
                    return;
                }
                if (string.startsWith("ROOM")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    if (Main.MainRef.network.changeRooms(n4)) {
                        this.hideAll();
                        this.clearHistory();
                        Main.MainRef.network.nullChat();
                        Main.MainRef.chat.clearChat();
                        this.showBaseMenu(true);
                    }
                    return;
                }
                if (string.startsWith("GREGISTER")) {
                    Main.MainRef.launchBuy();
                    return;
                }
                if (!string.startsWith("LINE")) break;
                n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                if (n4 < Main.MainRef.network.NumGames && this.joinGame(n4)) {
                    Main.MainRef.trackMatchJoin();
                    Main.MainRef.packetmanager.openForPackets();
                    this.showConnectMenu();
                    this.InternalMenuState = 6;
                }
                return;
            }
            case 6: {
                String string = this.SubButtonList.update(n, n2, n3);
                if (string == null || !string.equalsIgnoreCase("CANCEL")) break;
                Main.MainRef.packetmanager.closeForPackets();
                this.InternalMenuState = 0;
                Main.MainRef.MenuManager.hideLoading();
                this.hideAll();
                this.showBaseMenu(false);
                return;
            }
            case 7: {
                String string = this.SubButtonList.update(n, n2, n3);
                if (string == null) break;
                if (string.equalsIgnoreCase("OK")) {
                    this.InternalMenuState = 0;
                    this.hideAll();
                    this.showBaseMenu(false);
                    return;
                }
                if (!string.equalsIgnoreCase("HELP")) break;
                Main.MainRef.launchHelpPage("network.htm");
                return;
            }
            case 1: {
                String string = this.SubButtonList.update(n, n2, n3);
                if (string == null) break;
                if (string.equalsIgnoreCase("CREATE")) {
                    if ((Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`")) && Main.MainRef.ActiveMap > 0) {
                        if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                            Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                        }
                        return;
                    }
                    Main.MainRef.packetmanager.openForPackets();
                    this.InternalMenuState = 0;
                    this.createNewLobbyGame();
                    return;
                }
                if (string.equalsIgnoreCase("CANCEL")) {
                    if (Main.MainRef.SinglePlayer) {
                        Main.MainRef.MenuManager.activateMenu(3);
                        return;
                    }
                    this.InternalMenuState = 0;
                    this.hideCreateMenu();
                    return;
                }
                if (string.equalsIgnoreCase("MAPUP")) {
                    ++Main.MainRef.ActiveMap;
                    if (Main.MainRef.ActiveMap == Main.MainRef.MapTracker.Maps) {
                        Main.MainRef.ActiveMap = 0;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("MAPDOWN")) {
                    Main.MainRef.ActiveMap += -1;
                    if (Main.MainRef.ActiveMap < 0) {
                        Main.MainRef.ActiveMap = Main.MainRef.MapTracker.Maps - 1;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("TEAMUP")) {
                    ++this.TeamSelection;
                    if (this.TeamSelection >= 4) {
                        this.TeamSelection = 3;
                    }
                    if (Main.MainRef.MaxGamePlayerCount < Global.TEAMPLAYERREQUIREMENT[this.TeamSelection]) {
                        Main.MainRef.MaxGamePlayerCount = Global.TEAMPLAYERREQUIREMENT[this.TeamSelection];
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("TEAMDOWN")) {
                    this.TeamSelection += -1;
                    if (this.TeamSelection < 0) {
                        this.TeamSelection = 0;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("CASHUP")) {
                    ++this.StartingCashSelection;
                    if (this.StartingCashSelection >= 7) {
                        this.StartingCashSelection = 7;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("CASHDOWN")) {
                    this.StartingCashSelection += -1;
                    if (this.StartingCashSelection < 0) {
                        this.StartingCashSelection = 0;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("PLAYERSUP")) {
                    ++Main.MainRef.MaxGamePlayerCount;
                    if (Main.MainRef.MaxGamePlayerCount >= Main.MainRef.MaxGamePlayers) {
                        Main.MainRef.MaxGamePlayerCount = Main.MainRef.MaxGamePlayers;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("PLAYERSDOWN")) {
                    Main.MainRef.MaxGamePlayerCount += -1;
                    if (Main.MainRef.MaxGamePlayerCount < 2) {
                        Main.MainRef.MaxGamePlayerCount = 2;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("SEATUP")) {
                    ++this.HotSeatSelection;
                    if (this.HotSeatSelection >= 5) {
                        this.HotSeatSelection = 5;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("SEATDOWN")) {
                    this.HotSeatSelection += -1;
                    if (this.HotSeatSelection < 0) {
                        this.HotSeatSelection = 0;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("RESPAWNUP")) {
                    ++this.RespawnSelection;
                    if (this.RespawnSelection >= 10) {
                        this.RespawnSelection = 10;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("RESPAWNDOWN")) {
                    this.RespawnSelection += -1;
                    if (this.RespawnSelection < 0) {
                        this.RespawnSelection = 0;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("TREASUREUP")) {
                    ++this.TreasureRespawnSelection;
                    if (this.TreasureRespawnSelection >= 1) {
                        this.TreasureRespawnSelection = 1;
                    }
                    this.updateCreateMenu();
                }
                if (string.equalsIgnoreCase("TREASUREDOWN")) {
                    this.TreasureRespawnSelection += -1;
                    if (this.TreasureRespawnSelection < 0) {
                        this.TreasureRespawnSelection = 0;
                    }
                    this.updateCreateMenu();
                }
                if (!string.equalsIgnoreCase("FULLSCREEN")) break;
                Main.MainRef.wt_stage.toggleFullscreen();
                return;
            }
            case 2: {
                String string = this.SubButtonList.update(n, n2, n3);
                if (string != null) {
                    if (string.startsWith("BOT")) {
                        n4 = Float.valueOf(string.substring(3, string.length())).intValue();
                        if (n4 > 0) {
                            Main.MainRef.network.addBot(n4);
                        }
                        return;
                    }
                    if (string.startsWith("LINE")) {
                        n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                        if (n4 > 0) {
                            Packet packet = new Packet();
                            packet.Code = (short)15;
                            packet.Name = Main.MainRef.network.PlayerNames[n4];
                            Main.MainRef.network.sendPacket(packet);
                            packet.Code = (short)16;
                            packet.Id = (short)n4;
                            Main.MainRef.packetmanager.parseIndividualPacket(packet);
                        }
                        return;
                    }
                }
                if ((string = this.ButtonList.update(n, n2, n3)) == null) break;
                if (string.startsWith("Color")) {
                    n4 = Float.valueOf(string.substring(5, string.length())).intValue();
                    if (n4 != this.CannonColor) {
                        this.CannonColor = n4;
                        Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
                    }
                    Main.MainRef.network.postSwapColor(this.CannonColor);
                    return;
                }
                if (string.startsWith("Team")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    Main.MainRef.network.postSwapTeam(n4);
                    return;
                }
                if (string.startsWith("OPTI")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    Main.MainRef.changeSettings(n4);
                    this.ButtonList.remove(this.OptionsMenu);
                    this.OptionsMenu = null;
                    String[] stringArray = new String[10];
                    int n6 = Main.MainRef.getSettings(stringArray);
                    this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n6, stringArray, "Options", "OPTI", 34));
                    this.ButtonList.showAll();
                    return;
                }
                if (string.equalsIgnoreCase("CHATUP")) {
                    this.ChatTopLine += -1;
                    this.updateLobbyChatWindowJoin();
                    this.updateDragBarLocation();
                    this.HoldChatUp = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CHATDOWN")) {
                    ++this.ChatTopLine;
                    this.updateLobbyChatWindowJoin();
                    this.updateDragBarLocation();
                    this.HoldChatDown = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CANCEL")) {
                    this.InternalMenuState = 0;
                    this.cancelGameCreation();
                    this.hideAll();
                    this.showBaseMenu(false);
                    this.InternalMenuState = 1;
                    this.showCreateMenu();
                    return;
                }
                if (string.equalsIgnoreCase("BEGIN")) {
                    this.InternalMenuState = 0;
                    if (Main.MainRef.SinglePlayer) {
                        Main.MainRef.trackSPMatchLaunch();
                    } else {
                        Main.MainRef.trackMatchHost();
                    }
                    Main.MainRef.network.postGameBeginEvent();
                    return;
                }
                if (!string.equalsIgnoreCase("FULLSCREEN")) break;
                Main.MainRef.wt_stage.toggleFullscreen();
                return;
            }
            case 4: {
                String string = this.ButtonList.update(n, n2, n3);
                if (string == null) break;
                if (string.startsWith("Color")) {
                    n4 = Float.valueOf(string.substring(5, string.length())).intValue();
                    if (n4 != this.CannonColor) {
                        this.CannonColor = n4;
                        Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
                    }
                    Main.MainRef.network.postSwapColor(this.CannonColor);
                    return;
                }
                if (string.startsWith("Team")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    Main.MainRef.network.postSwapTeam(n4);
                    return;
                }
                if (string.startsWith("OPTI")) {
                    n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                    Main.MainRef.changeSettings(n4);
                    this.ButtonList.remove(this.OptionsMenu);
                    this.OptionsMenu = null;
                    String[] stringArray = new String[10];
                    int n7 = Main.MainRef.getSettings(stringArray);
                    this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n7, stringArray, "Options", "OPTI", 34));
                    this.ButtonList.showAll();
                    return;
                }
                if (string.equalsIgnoreCase("CHATUP")) {
                    this.ChatTopLine += -1;
                    this.updateLobbyChatWindowJoin();
                    this.updateDragBarLocation();
                    this.HoldChatUp = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CHATDOWN")) {
                    ++this.ChatTopLine;
                    this.updateLobbyChatWindowJoin();
                    this.updateDragBarLocation();
                    this.HoldChatDown = true;
                    this.ButtonDown = false;
                }
                if (string.equalsIgnoreCase("CANCEL")) {
                    this.InternalMenuState = 0;
                    this.hideAll();
                    this.showBaseMenu(false);
                    this.killJoin();
                    return;
                }
                if (string.equalsIgnoreCase("CONFIRM")) {
                    Main.MainRef.network.postGameConfirmEvent(0);
                    Button_3D button_3D = this.ButtonList.Root;
                    while (button_3D != null) {
                        if (button_3D.Keyword.equalsIgnoreCase("CONFIRM")) {
                            this.ButtonList.remove(button_3D);
                            return;
                        }
                        button_3D = button_3D.Next;
                    }
                    return;
                }
                if (!string.equalsIgnoreCase("FULLSCREEN")) break;
                Main.MainRef.wt_stage.toggleFullscreen();
                return;
            }
        }
    }

    void updateLobbyGames() {
        this.updateLobbyGames(false, true);
    }

    void updateLobbyGames(boolean bl, boolean bl2) {
        int n = 0;
        if (Main.MainRef.network.Connected) {
            int n2;
            if (this.GameTopLine + 8 > Main.MainRef.network.NumGames) {
                this.GameTopLine = Main.MainRef.network.NumGames - 8;
            }
            if (this.GameTopLine < 0) {
                this.GameTopLine = 0;
            }
            if ((n2 = this.GameTopLine + 8) > Main.MainRef.network.NumGames) {
                n2 = Main.MainRef.network.NumGames;
            }
            int n3 = 0;
            boolean bl3 = false;
            if (n2 - this.GameTopLine != this.LastGameCount) {
                bl3 = true;
            }
            n = this.GameTopLine;
            while (n < n2) {
                if (!this.LastLobbyWindow[n3].equalsIgnoreCase(Main.MainRef.network.GameNames[n])) {
                    bl3 = true;
                    break;
                }
                ++n3;
                ++n;
            }
            n3 = 0;
            this.LastGameCount = n2 - this.GameTopLine;
            if (bl3) {
                int n4 = 0;
                do {
                    if (this.GameNames[n4][0] == null) continue;
                    int n5 = 0;
                    do {
                        if (this.GameNames[n4][n5] != null) {
                            this.GameNames[n4][n5].destroy();
                        }
                        this.GameNames[n4][n5] = null;
                    } while (++n5 < 6);
                } while (++n4 < 39);
                Button_3D button_3D = this.ButtonList.Root;
                while (button_3D != null) {
                    Button_3D button_3D2 = button_3D.Next;
                    if (button_3D.Keyword.startsWith("LINE") || button_3D.Keyword.startsWith("LIGHT") || button_3D.Keyword.startsWith("GREG")) {
                        this.ButtonList.remove(button_3D);
                    }
                    button_3D = button_3D2;
                }
                n = this.GameTopLine;
                while (n < n2) {
                    String string;
                    this.LastLobbyWindow[n3] = string = Main.MainRef.network.GameNames[n];
                    StringTokenizer stringTokenizer = new StringTokenizer(string, ":");
                    String string2 = stringTokenizer.nextToken();
                    this.GameNames[n3][5] = new Message_3D(string2, 0, 0.75f);
                    this.GameNames[n3][5].show(10.0f, 140 + n3 * 28);
                    string2 = stringTokenizer.nextToken();
                    int n6 = 16;
                    int n7 = Float.valueOf(string2).intValue();
                    String string3 = "???";
                    if (n7 < Main.MainRef.MapTracker.Maps) {
                        string3 = Main.MainRef.MapTracker.MapNames[n7];
                    }
                    if (string3.length() < 16) {
                        n6 = string3.length();
                    }
                    string3 = string3.substring(0, n6);
                    this.GameNames[n3][0] = new Message_3D(string3, 0, 0.75f);
                    this.GameNames[n3][0].show(130.0f, 140 + n3 * 28);
                    if (n7 > 0 && (Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`"))) {
                        this.GameNames[n3][1] = new Message_3D("`Registered Players Only`", 0, 0.75f);
                        this.GameNames[n3][1].show(260.0f, 140 + n3 * 28);
                        if (Main.MainRef.OnlineDemo) {
                            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 105.0f, 154.0f, 74, 26, 105.0f, 128.0f, 490, 128 + n3 * 28, "GREGISTER", 0));
                        }
                    } else {
                        Button_3D button_3D3;
                        string2 = stringTokenizer.nextToken();
                        char c = string2.charAt(0);
                        char c2 = string2.charAt(2);
                        this.GameNames[n3][1] = new Message_3D(string2, 1, 0.75f);
                        this.GameNames[n3][1].show(280.0f, 140 + n3 * 28);
                        string2 = stringTokenizer.nextToken();
                        this.GameNames[n3][2] = new Message_3D(string2, 1, 0.75f);
                        this.GameNames[n3][2].show(350.0f, 140 + n3 * 28);
                        string2 = stringTokenizer.nextToken();
                        this.GameNames[n3][3] = new Message_3D(string2, 1, 0.75f);
                        this.GameNames[n3][3].show(415.0f, 140 + n3 * 28);
                        string2 = stringTokenizer.nextToken();
                        if (string2.equalsIgnoreCase("0")) {
                            string2 = "no";
                        }
                        this.GameNames[n3][4] = new Message_3D(string2, 1, 0.75f);
                        this.GameNames[n3][4].show(480.0f, 140 + n3 * 28);
                        if (c < c2 && !string3.startsWith("???")) {
                            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 57.0f, 102.0f, 45, 26, 57.0f, 76.0f, 520, 128 + n3 * 28, "LINE" + n, 0));
                        }
                        if (Main.MainRef.network.isConnectionValid(Main.MainRef.network.GameIP[n], Main.MainRef.network.GamePublicIP[n])) {
                            button_3D3 = this.ButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.IconGreen, 0.0f, 0.0f, 24, 24, 566, 128 + n3 * 28, 5));
                            button_3D3.Keyword = "LIGHT";
                        } else {
                            button_3D3 = this.ButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.IconRed, 0.0f, 0.0f, 24, 24, 566, 128 + n3 * 28, 5));
                            button_3D3.Keyword = "LIGHT";
                        }
                    }
                    ++n3;
                    ++n;
                }
            }
            this.ButtonList.showAll();
            n3 = 0;
            if (this.PlayerTopLine + 13 > Main.MainRef.network.NumClients) {
                this.PlayerTopLine = Main.MainRef.network.NumClients - 13;
            }
            if (this.PlayerTopLine < 0) {
                this.PlayerTopLine = 0;
            }
            if ((n2 = this.PlayerTopLine + 13) > Main.MainRef.network.NumClients) {
                n2 = Main.MainRef.network.NumClients;
            }
            bl3 = false;
            if (n2 - this.PlayerTopLine != this.LastPlayerCount) {
                bl3 = true;
            }
            n = this.PlayerTopLine;
            while (n < n2) {
                if (!this.LastPlayerWindow[n3].equalsIgnoreCase(Main.MainRef.network.ClientNames[n])) {
                    bl3 = true;
                    break;
                }
                if (this.LastClientIn[n3] != Main.MainRef.network.ClientIn[n]) {
                    bl3 = true;
                    break;
                }
                ++n3;
                ++n;
            }
            n3 = 0;
            if (bl3 || bl) {
                this.IconList.destroy();
                this.TitlePlayers.destroy();
                this.TitlePlayers = null;
                this.TitlePlayers = new Message_3D("Players:" + Main.MainRef.network.NumClients, 0, 1.0f);
                this.TitlePlayers.show(632.0f, 82.0f);
                int n8 = 0;
                do {
                    if (this.UserNames[n8] != null) {
                        this.UserNames[n8].destroy();
                    }
                    this.UserNames[n8] = null;
                } while (++n8 < 40);
                n = this.PlayerTopLine;
                while (n < n2) {
                    this.LastPlayerWindow[n3] = Main.MainRef.network.ClientNames[n];
                    this.LastClientIn[n3] = Main.MainRef.network.ClientIn[n];
                    String string = Main.MainRef.TextManager.wordWrapTrim(Main.MainRef.network.ClientNames[n], 120.0f, 0.75f);
                    this.UserNames[n3] = new Message_3D(string, 0, 0.65f);
                    this.UserNames[n3].show(670.0f, 110 + n3 * 20);
                    if (Main.MainRef.network.ClientIn[n] == 2) {
                        this.IconList.add(new Button_Static(Main.MainRef.GlobalMedia.IconAway, 0.0f, 0.0f, 16, 16, 625, 100 + n3 * 20, 0));
                    } else if (Main.MainRef.network.ClientIn[n] == 1) {
                        this.IconList.add(new Button_Static(Main.MainRef.GlobalMedia.IconPlay, 0.0f, 0.0f, 16, 16, 625, 100 + n3 * 20, 0));
                    }
                    if (this.isNameMuted(Main.MainRef.network.ClientNames[n])) {
                        this.IconList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 126.0f, 0.0f, 16, 16, 126.0f, 16.0f, 643, 100 + n3 * 20, "UNMUTE" + Main.MainRef.network.ClientNames[n], 21));
                    } else {
                        this.IconList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 110.0f, 0.0f, 16, 16, 110.0f, 16.0f, 643, 100 + n3 * 20, "MUTE" + Main.MainRef.network.ClientNames[n], 21));
                    }
                    ++n3;
                    ++n;
                }
            }
            this.LastPlayerCount = n2 - this.PlayerTopLine;
        }
        this.IconList.showAll();
        this.updateDragBarLocation();
        if (bl2) {
            this.processMouse(this.LastX, this.LastY, 0);
        }
    }

    void killJoin() {
        Main.MainRef.GameLoop.StartupDataReceived = false;
        Main.MainRef.network.GameCreationCompleted = false;
        Main.MainRef.network.nullCheckIn();
        Main.MainRef.network.nullSlots();
        Packet packet = new Packet();
        packet.Code = (short)16;
        packet.Id = (short)Main.MainRef.network.PlayerNumber;
        Main.MainRef.network.sendPacket(packet);
        Main.MainRef.network.CreatedGame = false;
        Main.MainRef.network.destroyMatch();
    }

    void updateLobbyChatWindowJoin() {
        int n;
        boolean bl = false;
        if (this.ChatTopLine == Main.MainRef.chat.ChatLines - 11) {
            bl = true;
        }
        if (Main.MainRef.chat.ChatLines < 11) {
            bl = true;
        }
        String string = Main.MainRef.TextManager.wordWrap(Main.MainRef.chat.ChatMessages, 20, 757.0f, 0.75f);
        Main.MainRef.chat.ChatLines = n = Main.MainRef.TextManager.TextLines;
        if (this.ChatTopLine == -999) {
            this.ChatTopLine = n - 11;
        }
        if (this.ChatTopLine + 11 > n || bl) {
            this.ChatTopLine = n - 11;
        }
        if (this.ChatTopLine < 0) {
            this.ChatTopLine = 0;
        }
        string = Main.MainRef.TextManager.trimBlock(string, this.ChatTopLine, 11);
        this.updateDragBarLocation();
        boolean bl2 = false;
        if (!this.LastChatHistory.equalsIgnoreCase(string)) {
            bl2 = true;
        }
        if (bl2) {
            if (this.ChatHistory != null) {
                this.ChatHistory.destroy();
            }
            this.ChatHistory = null;
            this.ChatHistory = new Message_3D(string, 0, 0.75f);
            this.ChatHistory.show(10.0f, 406.0f);
            this.LastChatHistory = string;
        }
    }

    void clearHistory() {
        this.GameTopLine = 0;
        this.PlayerTopLine = 0;
        this.LastGameCount = 0;
        this.LastPlayerCount = 0;
        this.LastChatHistory = "";
        int n = 0;
        do {
            this.LastLobbyWindow[n] = "";
            this.LastPlayerWindow[n] = "";
        } while (++n < 40);
    }

    void sendEmail() {
        Main.MainRef.network.sendEmail(Main.MainRef.network.UserName, this.Address);
        this.SubButtonList.destroy();
        Main.MainRef.packetmanager.closeForPackets();
        this.clearTempMessages();
        if (this.PopupMenu != null) {
            this.PopupMenu.destroy();
        }
        this.PopupMenu = null;
        this.PopupMenu = new PopUp(400, 300, "Done!", true);
        this.TempMessages[0] = new Message_3D("Your Invitation Has Been Sent!", 1, 1.0f, 51);
        this.TempMessages[0].show(400.0f, 300.0f);
        this.TempMessages[1] = new Message_3D("The person you have invited should shortly", 1, 0.75f, 51);
        this.TempMessages[1].show(400.0f, 330.0f);
        this.TempMessages[2] = new Message_3D("receive an invitation in their inbox!", 1, 0.75f, 51);
        this.TempMessages[2].show(400.0f, 354.0f);
        this.SubButtonList.add(new Button_3D(400, 412, "OK", "OK", 1, 52));
        this.SubButtonList.showAll();
        this.InternalMenuState = 7;
    }

    void cancelGameCreationAfterMenu() {
        Main.MainRef.network.setUserState(0);
        Main.MainRef.network.destroyPlayer();
        Main.MainRef.network.notifyJoinersOfCancel();
        Main.MainRef.GameLoop.StartupDataReceived = false;
        Main.MainRef.network.GameCreationCompleted = false;
        Main.MainRef.network.nullCheckIn();
        Main.MainRef.network.nullSlots();
        Main.MainRef.network.CreatedGame = false;
        Main.MainRef.network.destroyMatch();
        Main.MainRef.GameLoop.destroyGame();
    }

    void KeyDown(int n) {
    }

    void showCreateMenu() {
        this.clearTempMessages();
        if (this.PopupMenu != null) {
            this.PopupMenu.destroy();
        }
        this.PopupMenu = null;
        this.PopupMenu = new PopUp(400, 300, "New Game Settings", true);
        this.SubButtonList.add(new Button_3D(527, 412, "Create", "CREATE", 1, 52));
        this.SubButtonList.add(new Button_3D(273, 412, "Cancel", "CANCEL", 1, 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 148, 287, "MAPDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 278, 287, "MAPUP", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 203, "PLAYERSDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 203, "PLAYERSUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 217, 51, null));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 233, "RESPAWNDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 233, "RESPAWNUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 247, 51, null));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 263, "CASHDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 263, "CASHUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 277, 51, null));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 293, "SEATDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 293, "SEATUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 307, 51, null));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 323, "TREASUREDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 323, "TREASUREUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 337, 51, null));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 78.0f, 0.0f, 26, 27, 78.0f, 27.0f, 540, 353, "TEAMDOWN", 51));
        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 52.0f, 0.0f, 26, 27, 52.0f, 27.0f, 620, 353, "TEAMUP", 51));
        this.SubButtonList.add(new Button_Bar(50, 568, 367, 51, null));
        this.SubButtonList.showAll();
        this.updateCreateMenu();
    }

    void hideAll() {
        if (this.CannonVisible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)Main.MainRef.GlobalMedia.CannonGroup);
            this.CannonVisible = false;
        }
        if (this.ErrorMessage1 != null) {
            this.ErrorMessage1.destroy();
        }
        this.ErrorMessage1 = null;
        if (this.ErrorMessage2 != null) {
            this.ErrorMessage2.destroy();
        }
        this.ErrorMessage2 = null;
        this.MuteButton = null;
        this.ColorButton = null;
        this.OptionsMenu = null;
        this.clearTempMessages();
        this.IconList.destroy();
        this.LastChatHistory = "";
        int n = 0;
        do {
            this.LastLobbyWindow[n] = "";
            this.LastPlayerWindow[n] = "";
        } while (++n < 40);
        if (this.PopupMenu != null) {
            this.PopupMenu.destroy();
        }
        this.PopupMenu = null;
        if (this.Thumbnail != null) {
            this.Thumbnail.destroy();
        }
        this.Thumbnail = null;
        n = 0;
        do {
            if (this.GameNames[n][0] == null) continue;
            int n2 = 0;
            do {
                if (this.GameNames[n][n2] != null) {
                    this.GameNames[n][n2].destroy();
                }
                this.GameNames[n][n2] = null;
            } while (++n2 < 6);
        } while (++n < 40);
        n = 0;
        do {
            if (this.UserNames[n] != null) {
                this.UserNames[n].destroy();
            }
            this.UserNames[n] = null;
        } while (++n < 40);
        if (this.ChatHistory != null) {
            this.ChatHistory.destroy();
        }
        this.ChatHistory = null;
        if (this.ChatEntry != null) {
            this.ChatEntry.destroy();
        }
        this.ChatEntry = null;
        if (this.Version != null) {
            this.Version.destroy();
        }
        this.Version = null;
        if (this.TitlePlayers != null) {
            this.TitlePlayers.destroy();
        }
        this.TitlePlayers = null;
        if (this.TitleRoom != null) {
            this.TitleRoom.destroy();
        }
        this.TitleRoom = null;
        if (this.TitleChat != null) {
            this.TitleChat.destroy();
        }
        this.TitleChat = null;
        if (this.MyName != null) {
            this.MyName.destroy();
        }
        this.MyName = null;
        this.ButtonList.destroy();
        this.SubButtonList.destroy();
        this.GameBar = null;
        this.ChatBar = null;
        this.UserBar = null;
    }

    void keyDown(int n) {
        if (Main.MainRef.SinglePlayer) {
            return;
        }
        if (this.InternalMenuState == 0 || this.InternalMenuState == 2 || this.InternalMenuState == 4 || this.InternalMenuState == 1) {
            if (n == 16) {
                this.ShiftDown = true;
            }
            if (n == 13) {
                if ((double)this.ChatDelayTimer > 2.5 || this.ChatMessage.length() > 10) {
                    this.ChatDelayTimer = 0.0f;
                    if (this.InternalMenuState == 0 || this.InternalMenuState == 1) {
                        Main.MainRef.network.postChatMessage(this.ChatMessage);
                        this.ChatMessage = "";
                        this.updateLobbyChatText();
                    } else {
                        Main.MainRef.chat.ChatMessage = this.ChatMessage;
                        Main.MainRef.chat.postChatMessage();
                        this.ChatMessage = "";
                        Main.MainRef.chat.ChatMessage = "";
                        this.updateLobbyChatText();
                    }
                } else if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                    Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                }
            }
            if (this.ChatEntry.getPixelWidth() < 770 && n != 8 && n != 16 && n != 13 && n != 20) {
                this.ChatMessage = this.ChatMessage + "" + (char)n;
                this.updateLobbyChatText();
            }
            if (n == 8 && this.ChatMessage.length() > 0) {
                this.ChatMessage = this.ChatMessage.substring(0, this.ChatMessage.length() - 1);
                this.updateLobbyChatText();
                return;
            }
        } else if (this.InternalMenuState == 8) {
            if (n == 13 && this.Address.length() > 0) {
                this.sendEmail();
                return;
            }
            if ((n >= 48 && n < 58 || n >= 65 && n <= 90 || n >= 97 && n <= 122 || n == 46 || n == 64 || n == 95 || n == 45) && this.TempMessages[10].getPixelWidth() < 270) {
                this.Address = this.Address + "" + (char)n;
                this.updateEmailMenuText();
            }
            if (n == 8 && this.Address.length() > 0) {
                this.Address = this.Address.substring(0, this.Address.length() - 1);
                this.updateEmailMenuText();
            }
        }
    }

    boolean isNameMuted(String string) {
        int n = 0;
        while (n < this.MuteCount) {
            if (this.MuteList[n].equalsIgnoreCase(string)) {
                return true;
            }
            ++n;
        }
        return false;
    }

    void updateCreateMenu() {
        if (this.Thumbnail != null) {
            this.Thumbnail.destroy();
        }
        this.Thumbnail = null;
        this.Thumbnail = new Button_Static(Main.MainRef.MapTracker.MapThumbnail[Main.MainRef.ActiveMap], 0.0f, 0.0f, 100, 100, 177, 250, 51);
        if ((Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`")) && Main.MainRef.ActiveMap > 0) {
            this.Thumbnail.Group.setBitmapOpacity(100);
            if (this.ErrorMessage1 != null) {
                this.ErrorMessage1.destroy();
            }
            this.ErrorMessage1 = null;
            if (this.ErrorMessage2 != null) {
                this.ErrorMessage2.destroy();
            }
            this.ErrorMessage2 = null;
            this.ErrorMessage1 = new Message_3D("REGISTERED", 1, 0.75f, 52);
            this.ErrorMessage1.show(232.0f, 290.0f);
            this.ErrorMessage2 = new Message_3D("ONLY!", 1, 0.75f, 52);
            this.ErrorMessage2.show(232.0f, 310.0f);
        } else {
            if (this.ErrorMessage1 != null) {
                this.ErrorMessage1.destroy();
            }
            this.ErrorMessage1 = null;
            if (this.ErrorMessage2 != null) {
                this.ErrorMessage2.destroy();
            }
            this.ErrorMessage2 = null;
        }
        this.clearTempMessages();
        this.TempMessages[0] = new Message_3D("Maximum # of Players", 0, 0.75f, 52);
        this.TempMessages[0].show(340.0f, 220.0f);
        this.TempMessages[1] = new Message_3D("" + Main.MainRef.MaxGamePlayerCount, 1, 0.75f, 52);
        this.TempMessages[1].show(600.0f, 218.0f);
        this.TempMessages[2] = new Message_3D("Starting # of Lives", 0, 0.75f, 52);
        this.TempMessages[2].show(340.0f, 250.0f);
        this.TempMessages[3] = new Message_3D("" + this.RespawnSelection, 1, 0.75f, 52);
        this.TempMessages[3].show(600.0f, 248.0f);
        this.TempMessages[4] = new Message_3D("Starting Gold", 0, 0.75f, 52);
        this.TempMessages[4].show(340.0f, 280.0f);
        this.TempMessages[5] = new Message_3D("" + Global.STARTINGCASH[this.StartingCashSelection], 1, 0.75f, 52);
        this.TempMessages[5].show(600.0f, 278.0f);
        this.TempMessages[6] = new Message_3D("HotSeat Mode", 0, 0.75f, 52);
        this.TempMessages[6].show(340.0f, 310.0f);
        this.TempMessages[7] = new Message_3D("" + Global.HOTSEATNAMES[this.HotSeatSelection], 1, 0.75f, 52);
        this.TempMessages[7].show(600.0f, 308.0f);
        this.TempMessages[8] = new Message_3D("Treasures Respawn", 0, 0.75f, 52);
        this.TempMessages[8].show(340.0f, 340.0f);
        this.TempMessages[9] = new Message_3D("" + Global.TREASURERESPAWN[this.TreasureRespawnSelection], 1, 0.75f, 52);
        this.TempMessages[9].show(600.0f, 338.0f);
        this.TempMessages[10] = new Message_3D("Team Play", 0, 0.75f, 52);
        this.TempMessages[10].show(340.0f, 370.0f);
        this.TempMessages[11] = new Message_3D("" + Global.TEAMDESCRIPTION[this.TeamSelection], 1, 0.75f, 52);
        this.TempMessages[11].show(600.0f, 368.0f);
    }

    void showJoinMenu() {
        Main.MainRef.chat.clearChat();
        this.ChatTopLine = -999;
        this.clearHistory();
        this.BackDrop.detach();
        this.BackDrop.attach(this.CreateBackgroundJPG);
        this.TitleRoom = new Message_3D("Joining Players", 0, 1.0f);
        this.TitleRoom.show(14.0f, 82.0f);
        if (!this.CannonVisible) {
            if (Main.MainRef.GlobalMedia.CannonTex.Shader.getNumLayers() < 2) {
                Main.MainRef.GlobalMedia.CannonTex.Shader.setNumLayers(2);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setTexture(1, Main.MainRef.GlobalMedia.Reflection.Image);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setLayerType(1, 4);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setLayerSource(1, 2);
                Main.MainRef.GlobalMedia.CannonTex.Shader.setTextureCoordGenMethod(1, 11);
            }
            Main.MainRef.camera.Camera.addObject((WTContainer)Main.MainRef.GlobalMedia.CannonGroup);
            Main.MainRef.GlobalMedia.CannonBarrelActor.Model.setSurfaceShader(Main.MainRef.GlobalMedia.CannonTex.Shader);
            Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
            Main.MainRef.GlobalMedia.CannonGroup.setPosition(8.208f, 4.0f, 30.0f);
            Main.MainRef.GlobalMedia.CannonGroup.setOrientation(0.0f, 1.0f, 0.0f, -135.0f);
            Main.MainRef.GlobalMedia.CannonBarrelActor.Model.setOrientation(1.0f, 0.0f, 0.0f, -20.0f);
            Main.MainRef.GlobalMedia.CannonGroup.setConstantRotation(0.0f, 1.0f, 0.0f, 50.0f);
            this.CannonVisible = true;
        }
        this.ChatBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(765, 427, 119, 11, 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 765, 400, "CHATUP", 0));
        this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 765, 546, "CHATDOWN", 0));
        this.ButtonList.add(new Button_3D(610, 70, "CANCEL", "Leave This Game", 1));
        this.ButtonList.add(new Button_3D(610, 350, "CONFIRM", "Confirm Your Join", 1));
        if (Main.MainRef.network.TeamGame) {
            this.ColorButton = (Button_3DDropColor)this.ButtonList.add(new Button_3DDropColor(610, 320, Main.MainRef.network.TeamCount, Global.COLORRGB, "Team"));
            this.ColorButton.setSelection(this.CannonColor);
        } else {
            this.ColorButton = (Button_3DDropColor)this.ButtonList.add(new Button_3DDropColor(610, 320, 4, Global.COLORRGB, "Color"));
            this.ColorButton.setSelection(this.CannonColor);
        }
        this.ButtonList.showAll();
        this.Version = new Message_3D(Main.MainRef.VersionNumber, 1, 1.0f);
        this.Version.show(400.0f, 16.0f);
    }

    boolean isLoaded() {
        if (this.BackgroundJPG == null) {
            this.RoomRequestTimer = 0.0f;
            if (!Main.MainRef.SinglePlayer) {
                Main.MainRef.network.requestPlayerPublicIP();
                Main.MainRef.network.setUserState(0);
                this.BackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/LOBBY_SCREEN/image.wjp", Main.MainRef.CacheType);
            } else {
                this.BackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/RESULTS_SCREEN/image.wjp", Main.MainRef.CacheType);
            }
            this.CreateBackgroundJPG = Main.MainRef.Wt.createBitmap(Main.MainRef.MediaPath + "MEDIA/MENUS/LOBBY_SCREEN/join.wjp", Main.MainRef.CacheType);
            return false;
        }
        if (!Main.MainRef.SinglePlayer) {
            if (this.DailyTips == null) {
                this.DailyTips = new Tips();
            }
            if (!this.DailyTips.isLoaded()) {
                return false;
            }
        }
        if (!this.BackgroundJPG.isLoaded()) {
            return false;
        }
        if (!this.CreateBackgroundJPG.isLoaded()) {
            return false;
        }
        if (!Main.MainRef.SinglePlayer) {
            if (!Main.MainRef.network.amInRoom()) {
                this.RoomRequestTimer += 1.0f;
                if (this.RoomRequestTimer > 500.0f) {
                    this.RoomRequestTimer = 0.0f;
                    Main.MainRef.network.createNetContext();
                    int n = 0;
                    while (!Main.MainRef.network.isNetContextConnected() && n < 100) {
                        Main.MainRef.network.pollNetContext(10L);
                        ++n;
                    }
                    if (!Main.MainRef.network.isNetContextConnected()) {
                        Main.MainRef.network.leaveLobby();
                        Main.MainRef.MenuManager.activateMenuInstant(3);
                        this.BackgroundJPG.destroy();
                        this.BackgroundJPG = null;
                        this.CreateBackgroundJPG.destroy();
                        this.CreateBackgroundJPG = null;
                        return false;
                    }
                    Main.MainRef.network.requestRoomListing();
                }
                return false;
            }
            if (!Main.MainRef.network.hasPublicIP()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
     * Unable to fully structure code
     * Enabled aggressive block sorting
     */
    void showMasterJoinMembers() {
        block22: {
            block21: {
                if (this.InternalMenuState != 2) {
                    return;
                }
                var1_1 = 0;
                var2_2 = 0;
                var4_3 = "";
                this.SubButtonList.destroy();
                var1_1 = 0;
                while (var1_1 < Main.MainRef.MaxGamePlayerCount) {
                    if (Main.MainRef.network.AvailableSlots[var1_1] && Main.MainRef.network.ClientCheckedIn[var1_1]) {
                        var4_3 = var4_3 + "" + Main.MainRef.network.PlayerNames[var1_1] + ":" + Main.MainRef.network.ClientConfirmed[var1_1] + ":" + Main.MainRef.network.PlayerTeam[var1_1] + ":";
                    }
                    ++var1_1;
                }
                this.clearTempMessages();
                var3_4 = new StringTokenizer(var4_3, ":");
                while (var3_4.hasMoreTokens()) {
                    var5_5 = var3_4.nextToken();
                    this.TempMessages[var2_2] = Main.MainRef.network.IsBot[var2_2] != false ? new Message_3D("`" + var5_5 + "`", 0, 0.75f) : new Message_3D(var5_5, 0, 0.75f);
                    var5_5 = var3_4.nextToken();
                    if (var5_5.equals("1")) {
                        this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.IconCheck, 0.0f, 0.0f, 24, 24, 330, 100 + var2_2 * 32, 20));
                    }
                    if (var2_2 != 0) {
                        this.SubButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 57.0f, 154.0f, 45, 26, 57.0f, 128.0f, 355, 100 + var2_2 * 32, "LINE" + var2_2, 0));
                    }
                    var6_6 = Float.valueOf(var3_4.nextToken()).intValue();
                    if (!Main.MainRef.network.TeamGame) ** GOTO lbl51
                    if (var2_2 == Main.MainRef.network.PlayerNumber) {
                        if (this.ColorButton != null) {
                            this.ColorButton.setSelection(var6_6);
                        }
                        if (var6_6 != this.CannonColor) {
                            this.CannonColor = var6_6;
                            Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
                        }
                    }
                    this.TempMessages[var2_2].show(40.0f, 112 + var2_2 * 32);
                    switch (var6_6) {
                        case 0: {
                            this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 0.0f, 24, 24, 10, 100 + var2_2 * 32, 20));
                            break;
                        }
                        case 1: {
                            this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 24.0f, 24, 24, 10, 100 + var2_2 * 32, 20));
                            break;
                        }
                        case 2: {
                            this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 0.0f, 24, 24, 10, 100 + var2_2 * 32, 20));
                            break;
                        }
                        case 3: {
                            this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 24.0f, 24, 24, 10, 100 + var2_2 * 32, 20));
                            break;
                        }
lbl51:
                        // 1 sources

                        this.TempMessages[var2_2].show(10.0f, 112 + var2_2 * 32);
                        break;
                    }
                    ++var2_2;
                }
                var6_6 = var2_2;
                var7_7 = new String[]{"none", "Dummy", "Aggressive", "Thinker", "Crazy"};
                while (var2_2 < Main.MainRef.MaxGamePlayerCount) {
                    this.SubButtonList.add(new Button_3DDrop(200, 112 + var2_2 * 32, 5, var7_7, "BOT", 0, (7 - var2_2) * 10, "Add AI Player"));
                    ++var2_2;
                }
                Main.MainRef.network.updateCurrentMatch(Main.MainRef.network.MyIP + "|" + Main.MainRef.network.UserName + ":" + Main.MainRef.ActiveMap + ":" + var6_6 + "/" + Main.MainRef.MaxGamePlayerCount + ":" + Global.HOTSEATNAMES[this.HotSeatSelection] + ":" + this.RespawnSelection + ":" + Main.MainRef.network.TeamCount);
                if (Main.MainRef.network.confirmedAndCheckedIn()) break block21;
                var8_8 = this.ButtonList.Root;
                if (true) ** GOTO lbl75
            }
            var8_8 = this.ButtonList.Root;
            var9_9 = false;
            if (true) ** GOTO lbl83
            do {
                if (var8_8.Keyword.equalsIgnoreCase("BEGIN")) {
                    this.ButtonList.remove((Button_3D)var8_8);
                    break;
                }
                var8_8 = var8_8.Next;
lbl75:
                // 2 sources

            } while (var8_8 != null);
            this.ClientsCheckedIn = false;
            break block22;
            do {
                if (var8_8.Keyword.equalsIgnoreCase("BEGIN")) {
                    var9_9 = true;
                    break;
                }
                var8_8 = var8_8.Next;
lbl83:
                // 2 sources

            } while (var8_8 != null);
            this.ClientsCheckedIn = true;
            if (!var9_9) {
                this.ButtonList.add(new Button_3D(610, 350, "BEGIN", "Begin The Game!", 1));
                this.ButtonList.showAll();
            }
        }
        var8_8 = new Packet();
        var8_8.Code = (short)17;
        var8_8.Name = var4_3;
        Main.MainRef.network.sendPacket((Packet)var8_8);
        this.SubButtonList.showAll();
    }

    void keyUp(int n) {
        if (n == 16) {
            this.ShiftDown = false;
        }
    }

    void updateLobbyChatText() {
        if (this.ChatEntry != null) {
            this.ChatEntry.destroy();
        }
        this.ChatEntry = null;
        if (this.CursorVisible) {
            this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage + '\u007f', 0, 0.75f);
            this.ChatEntry.show(10.0f, 587.0f);
            return;
        }
        this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage, 0, 0.75f);
        this.ChatEntry.show(10.0f, 587.0f);
    }

    void show() {
        if (!this.Visible) {
            Main.MainRef.network.zeroStats();
            Main.MainRef.network.setHasLivePlayers(false);
            if (!Main.MainRef.SinglePlayer) {
                Main.MainRef.network.retrieveUserList();
                Main.MainRef.network.retrieveGameList();
                Main.MainRef.network.setUserState(0);
            }
            this.clearHistory();
            this.ShiftDown = false;
            Main.MainRef.camera.hideMouse();
            this.InternalMenuState = 1;
            if (Main.MainRef.hud != null) {
                Main.MainRef.hud.hide();
            }
            this.BackDrop = Main.MainRef.camera.CameraView.addDrop(this.BackgroundJPG, false);
            this.Visible = true;
            this.showBaseMenu(true);
            Main.MainRef.camera.showMouse();
            if (Main.MainRef.GlobalMedia.ShellMusic == null && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/MUSIC/TITLE"), true);
            }
            if (Main.MainRef.GlobalMedia.ShellMusic != null && !Main.MainRef.GlobalMedia.ShellMusic.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                Main.MainRef.GlobalMedia.ShellMusic.play(true, 127);
            }
            Main.MainRef.chat.clearChat();
            if (Main.MainRef.SinglePlayer) {
                this.InternalMenuState = 1;
                this.showCreateMenu();
                this.updateCreateMenu();
            }
        }
    }

    void updateLobbyChatWindow() {
        int n;
        boolean bl = false;
        if (this.ChatTopLine == this.ChatLines - 11) {
            bl = true;
        }
        String string = Main.MainRef.TextManager.wordWrap(Main.MainRef.network.ChatMessages, 20, 757.0f, 0.75f);
        this.ChatLines = n = Main.MainRef.TextManager.TextLines;
        if (this.ChatTopLine == -999) {
            this.ChatTopLine = n - 11;
        }
        if (this.ChatTopLine + 11 > n || bl) {
            this.ChatTopLine = n - 11;
        }
        if (this.ChatTopLine < 0) {
            this.ChatTopLine = 0;
        }
        string = Main.MainRef.TextManager.trimBlock(string, this.ChatTopLine, 11);
        this.updateDragBarLocation();
        boolean bl2 = false;
        if (!this.LastChatHistory.equals(string)) {
            bl2 = true;
        }
        if (bl2) {
            if (this.ChatHistory != null) {
                this.ChatHistory.destroy();
            }
            this.ChatHistory = null;
            this.ChatHistory = new Message_3D(string, 0, 0.75f);
            this.ChatHistory.show(10.0f, 406.0f);
            this.LastChatHistory = string;
        }
    }

    /*
     * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
     * Unable to fully structure code
     * Enabled aggressive block sorting
     */
    void showClientJoinMembers(String var1_1) {
        if (this.InternalMenuState != 4) {
            return;
        }
        this.SubButtonList.destroy();
        var4_2 = 0;
        this.clearTempMessages();
        var2_3 = new StringTokenizer(var1_1, ":");
        var5_4 = false;
        while (var2_3.hasMoreTokens()) {
            Main.MainRef.network.PlayerNames[var4_2] = var3_5 = var2_3.nextToken();
            this.TempMessages[var4_2] = new Message_3D(var3_5, 0, 0.75f);
            var3_5 = var2_3.nextToken();
            if (var3_5.equals("1")) {
                this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.IconCheck, 0.0f, 0.0f, 24, 24, 330, 100 + var4_2 * 32, 20));
            }
            var6_6 = Float.valueOf(var2_3.nextToken()).intValue();
            if (!Main.MainRef.network.TeamGame) ** GOTO lbl42
            if (var4_2 == Main.MainRef.network.PlayerNumber) {
                var5_4 = true;
                if (this.ColorButton != null) {
                    this.ColorButton.setSelection(var6_6);
                }
                if (var6_6 != this.CannonColor) {
                    this.CannonColor = var6_6;
                    Main.MainRef.GlobalMedia.CannonTex.tint(Global.COLORRGB[this.CannonColor * 3], Global.COLORRGB[this.CannonColor * 3 + 1], Global.COLORRGB[this.CannonColor * 3 + 2]);
                }
            }
            this.TempMessages[var4_2].show(40.0f, 112 + var4_2 * 32);
            switch (var6_6) {
                case 0: {
                    this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 0.0f, 24, 24, 10, 100 + var4_2 * 32, 20));
                    break;
                }
                case 1: {
                    this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 24.0f, 24, 24, 10, 100 + var4_2 * 32, 20));
                    break;
                }
                case 2: {
                    this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 0.0f, 24, 24, 10, 100 + var4_2 * 32, 20));
                    break;
                }
                case 3: {
                    this.SubButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 24.0f, 24, 24, 10, 100 + var4_2 * 32, 20));
                    break;
                }
lbl42:
                // 1 sources

                this.TempMessages[var4_2].show(10.0f, 112 + var4_2 * 32);
                break;
            }
            ++var4_2;
        }
        this.SubButtonList.showAll();
        if (var4_2 == 1 && var5_4) {
            Main.MainRef.Wt.outDebugString("CLIENT DUMPED SELF! ");
            this.InternalMenuState = 0;
            this.hideAll();
            this.showBaseMenu(false);
            this.killJoin();
        }
    }

    void updateEmailMenuText() {
        if (this.TempMessages[10] != null) {
            this.TempMessages[10].destroy();
        }
        this.TempMessages[10] = null;
        if (this.CursorVisible) {
            this.TempMessages[10] = new Message_3D(this.Address + '\u007f', 0, 0.75f, 55);
            this.TempMessages[10].show(260.0f, 300.0f);
            return;
        }
        this.TempMessages[10] = new Message_3D(this.Address, 0, 0.75f, 55);
        this.TempMessages[10].show(260.0f, 300.0f);
    }

    void updateDragBarLocation() {
        if (this.GameBar != null) {
            this.GameBar.update(this.GameTopLine, Main.MainRef.network.NumGames);
        }
        if (this.UserBar != null) {
            this.UserBar.update(this.PlayerTopLine, Main.MainRef.network.NumClients);
        }
        if (this.ChatBar != null) {
            if (this.InternalMenuState == 0) {
                this.ChatBar.update(this.ChatTopLine, this.ChatLines);
                return;
            }
            this.ChatBar.update(this.ChatTopLine, Main.MainRef.chat.ChatLines);
        }
    }
}

