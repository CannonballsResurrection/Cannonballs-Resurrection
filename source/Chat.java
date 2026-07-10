/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTDrop;

public class Chat
implements Global {
    WTDrop Drop;
    boolean Visible = false;
    int ChatType = 0;
    int[] ChatTeam = new int[21];
    String[] ChatMessages = new String[21];
    boolean Chatting = false;
    boolean ShiftDown = false;
    boolean ChatBarVisible = false;
    String ChatMessage = "";
    boolean Attached = false;
    Button_Bar ChatBar;
    Message_3D ChatEntry;
    Message_3D ChatBlock;
    float BlinkCount = 0.0f;
    int ChatLines = 0;
    int ChatTopLine = -999;
    boolean CursorVisible = false;

    void clearMessages() {
        this.ChatLines = 0;
        int n = 0;
        n = 0;
        do {
            this.ChatMessages[n] = "";
            this.ChatTeam[n] = 0;
        } while (++n < 20);
    }

    void keyDownChat(int n) {
        if (n == 16) {
            this.ShiftDown = true;
        }
        if (n == 13) {
            this.postChatMessage();
            this.ChatMessage = "";
            this.Chatting = false;
            this.updateChatText();
            this.hideChatBar();
        }
        if (this.ChatEntry.getPixelWidth() < 750 && n != 8 && n != 16 && n != 13 && n != 20) {
            this.ChatMessage = this.ChatMessage + "" + (char)n;
            this.updateChatType();
        }
        if (n == 8 && this.ChatMessage.length() > 0) {
            this.ChatMessage = this.ChatMessage.substring(0, this.ChatMessage.length() - 1);
            this.updateChatType();
        }
    }

    public Chat() {
        this.clearMessages();
    }

    void hide() {
        if (this.Visible) {
            if (this.ChatBlock != null) {
                this.ChatBlock.destroy();
            }
            this.ChatBlock = null;
            this.Visible = false;
            Main.MainRef.camera.CameraView.removeDrop(this.Drop);
            if (this.ChatEntry != null) {
                this.ChatEntry.destroy();
            }
            this.ChatEntry = null;
        }
    }

    void clearChat() {
        this.ChatTopLine = -999;
        this.ChatLines = 0;
        this.Chatting = false;
        this.ChatMessage = "";
        this.clearMessages();
        this.hideChatBar();
    }

    void showChatBar() {
        if (!this.ChatBarVisible) {
            if (this.ChatBar == null) {
                this.ChatBar = new Button_Bar(770, 5, 587, 5, null);
            }
            this.ChatBarVisible = true;
            this.ChatBar.show();
        }
    }

    void postChatMessageFromBot(int n, String string) {
        if (string.length() > 0) {
            Packet packet = new Packet();
            packet.Code = (short)22;
            packet.Name = '`' + Main.MainRef.cannon[n].Name + ":" + '`' + string;
            packet.Id = (short)n;
            if (this.ChatType == 0) {
                packet.Var1 = -1.0f;
            } else if (this.ChatType == 1) {
                packet.Var1 = 1.0f;
                packet.Var2 = Main.MainRef.network.PlayerTeam[n];
            }
            Main.MainRef.network.sendPacket(packet);
            Main.MainRef.packetmanager.parseIndividualPacket(packet);
        }
    }

    void update(float f) {
        if (this.ChatBarVisible) {
            this.BlinkCount += f;
            if (this.BlinkCount > 0.9f) {
                this.BlinkCount = 0.0f;
                this.CursorVisible = !this.CursorVisible;
                this.updateChatType();
            }
        }
    }

    void enableChat(int n) {
        this.Chatting = true;
        this.ChatType = n;
        this.showChatBar();
        this.updateChatType();
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveLeft = false;
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveRight = false;
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveForward = false;
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveBack = false;
    }

    void receiveChatText(String string, int n) {
        string = Text.doFilter(string);
        int n2 = 0;
        if (n == -1) {
            n2 = 0;
            do {
                this.ChatMessages[n2] = this.ChatMessages[n2 + 1];
                this.ChatTeam[n2] = this.ChatTeam[n2 + 1];
            } while (++n2 < 19);
            this.ChatMessages[19] = string;
            this.ChatTeam[19] = -1;
            if (Main.MainRef.GameState == 10) {
                this.updateLobbyChatWindow();
                return;
            }
            this.updateChatText();
            return;
        }
        if (n == Main.MainRef.network.MyTeam) {
            n2 = 0;
            do {
                this.ChatMessages[n2] = this.ChatMessages[n2 + 1];
                this.ChatTeam[n2] = this.ChatTeam[n2 + 1];
            } while (++n2 < 19);
            this.ChatMessages[19] = string;
            this.ChatTeam[19] = 1;
            this.updateChatText();
        }
    }

    void hideChatBar() {
        if (this.ChatBarVisible) {
            this.ChatBarVisible = false;
            this.ChatBar.hide();
        }
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.showMouse();
            this.updateChatType();
        }
    }

    void updateLobbyChatWindow() {
        Main.MainRef.MenuManager.LobbyScreen.updateLobbyChatWindowJoin();
    }

    void updateChatType() {
        if (this.ChatEntry != null) {
            this.ChatEntry.destroy();
        }
        this.ChatEntry = null;
        if (this.CursorVisible) {
            this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage + '\u007f', 0, 0.75f, 80);
            this.ChatEntry.show(14.0f, 587.0f);
            return;
        }
        this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage, 0, 0.75f, 80);
        this.ChatEntry.show(14.0f, 587.0f);
    }

    void postChatMessage() {
        if (this.ChatMessage.length() > 0) {
            this.ChatMessage = Text.doFilter(this.ChatMessage);
            Packet packet = new Packet();
            packet.Code = (short)22;
            packet.Name = '`' + Main.MainRef.network.UserName + ":" + '`' + this.ChatMessage;
            packet.Id = (short)Main.MainRef.network.PlayerNumber;
            if (this.ChatType == 0) {
                packet.Var1 = -1.0f;
            } else if (this.ChatType == 1) {
                packet.Var1 = 1.0f;
                packet.Var2 = Main.MainRef.network.MyTeam;
            }
            Main.MainRef.network.sendPacket(packet);
            Main.MainRef.packetmanager.parseIndividualPacket(packet);
        }
    }

    void keyUpChat(int n) {
        if (n == 16) {
            this.ShiftDown = false;
        }
    }

    void updateChatText() {
        int n;
        int n2 = 14;
        if (Main.MainRef.ChatMinimized) {
            n2 = 3;
        }
        boolean bl = false;
        if (this.ChatTopLine == this.ChatLines - n2) {
            bl = true;
        }
        if (this.ChatLines < n2) {
            bl = true;
        }
        String string = Main.MainRef.TextManager.wordWrap(this.ChatMessages, 20, 250.0f, 0.75f);
        this.ChatLines = n = Main.MainRef.TextManager.TextLines;
        if (this.ChatTopLine == -999) {
            this.ChatTopLine = n - n2;
        }
        if (this.ChatTopLine + n2 > n || bl) {
            this.ChatTopLine = n - n2;
        }
        if (this.ChatTopLine < 0) {
            this.ChatTopLine = 0;
        }
        string = Main.MainRef.TextManager.trimBlock(string, this.ChatTopLine, n2);
        Main.MainRef.hud.updateDragBarLocation();
        if (this.ChatBlock != null) {
            this.ChatBlock.destroy();
        }
        this.ChatBlock = null;
        this.ChatBlock = new Message_3D(string, 0, 0.75f, 80);
        if (Main.MainRef.ChatMinimized) {
            this.ChatBlock.show(15.0f, 528.0f);
        } else {
            this.ChatBlock.show(15.0f, 360.0f);
        }
        if (this.Chatting) {
            if (this.ChatEntry != null) {
                this.ChatEntry.destroy();
            }
            this.ChatEntry = null;
            if (this.CursorVisible) {
                this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage + '\u007f', 0, 0.75f, 80);
                this.ChatEntry.show(14.0f, 587.0f);
                return;
            }
            this.ChatEntry = new Message_3D("SAY : " + this.ChatMessage, 0, 0.75f, 80);
            this.ChatEntry.show(14.0f, 587.0f);
            return;
        }
        if (this.ChatEntry != null) {
            this.ChatEntry.destroy();
        }
        this.ChatEntry = null;
        this.ChatEntry = new Message_3D("Press 'C' to Chat", 0, 0.75f, 80);
        this.ChatEntry.show(14.0f, 587.0f);
    }
}

