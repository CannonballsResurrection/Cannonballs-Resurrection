/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 *  wildtangent.webdrivermp.WTMPConstants
 *  wildtangent.webdrivermp.WTMPMessage
 *  wildtangent.webdrivermp.WTMPSession
 *  wildtangent.webdrivermp.WTMultiplayer
 */
import com.wildtangent.dmmp.client.ClientContext;
import com.wildtangent.dmmp.client.NativeSystemInitializer;
import com.wildtangent.dmmp.community.chat.ChatEvent;
import com.wildtangent.dmmp.community.lobby.GameCreationMessage;
import com.wildtangent.dmmp.community.lobby.GameRemovalMessage;
import com.wildtangent.dmmp.community.lobby.MatchmakingLobby;
import com.wildtangent.dmmp.community.lobby.MatchmakingLobbyEvent;
import com.wildtangent.dmmp.community.player.PlayerAttributeMessage;
import com.wildtangent.dmmp.community.room.Room;
import com.wildtangent.dmmp.community.room.RoomEnterMessage;
import com.wildtangent.dmmp.community.room.RoomEvent;
import com.wildtangent.dmmp.community.room.RoomLeaveMessage;
import com.wildtangent.dmmp.community.room.RoomListingMessage;
import com.wildtangent.dmmp.shared.dobject.Channel;
import com.wildtangent.dmmp.shared.dobject.DObject;
import com.wildtangent.dmmp.shared.dobject.DObjectEvent;
import com.wildtangent.dmmp.shared.dobject.DObjectSubscriber;
import com.wildtangent.dmmp.shared.dobject.NetworkErrorHandler;
import com.wildtangent.dmmp.shared.dobject.NetworkNode;
import com.wildtangent.dmmp.shared.service.ServiceMessage;
import com.wildtangent.dmmp.shared.service.ServiceRequester;
import com.wildtangent.dmmp.shared.util.IntIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;
import wildtangent.webdrivermp.WTMPConstants;
import wildtangent.webdrivermp.WTMPMessage;
import wildtangent.webdrivermp.WTMPSession;
import wildtangent.webdrivermp.WTMultiplayer;

public class Network
implements Global,
WTMPConstants,
NetworkErrorHandler,
DObjectSubscriber,
ServiceRequester,
WTOnLoadEvent {
    String UserEmail = "";
    String UserName = "";
    String Password = "";
    int RoomCount = -99;
    int NumClients = 0;
    String[] ClientNames = new String[500];
    int[] ClientIDs = new int[500];
    int[] ClientIn = new int[500];
    int NumGames = 0;
    String[] GameNames = new String[500];
    String[] GameIP = new String[500];
    String[] GamePublicIP = new String[500];
    int[] HostingPlayerIDs = new int[500];
    int ChatCount = 0;
    String[] ChatMessages = new String[50];
    int InsultCount = 5;
    String[] ChatInsult = new String[]{"I'm gonna get you,", "You better watch out", "Here comes the pain", "I got you in my sights", "You're going down"};
    int GreetingCount = 9;
    String[] ChatGreeting = new String[]{"Hi everybody", "Hello", "Hi guys!", "Greets", "Hola", "Hey", "Hey folks", "What's up?", "Howdy"};
    int DeathCount = 4;
    String[] ChatDeath = new String[]{"Ow!", "Ouch!", "That hurt!", "Good grief!"};
    int KillCount = 7;
    String[] ChatKill = new String[]{"Oh yeah!", "I am the king!", "Sweet!", "Take that!", "Woohoo!", "I rule!", "Bow down before me!!"};
    int ComplimentCount = 5;
    String[] ChatCompliment = new String[]{"Nice shot,", "That was a nice one,", "Good shot", "Nice moves", "What a shot", "That was sweet"};
    boolean CreatedGame = false;
    WTMultiplayer wtMulti;
    WTMPSession wtSession;
    WTFile TrackFile;
    WTFile UMSFile;
    WTFile LBFile;
    WTFile SFile;
    boolean UMSRequesting = false;
    boolean UMSResult = false;
    String UMSResultString = "";
    String UMSResultValue = "";
    int TeamCount = 0;
    String[] PlayerNames = new String[32];
    int[] PlayerTeam = new int[32];
    int[] TeamMemberCount = new int[32];
    int[] LastTeamPlayer = new int[32];
    boolean[] ClientCheckedIn = new boolean[32];
    int[] ClientConfirmed = new int[32];
    int[] ClientConnectionState = new int[32];
    boolean[] AvailableSlots = new boolean[32];
    int[] CannonColor = new int[32];
    boolean[] IsBot = new boolean[32];
    int[] BotType = new int[32];
    int FirstPlayer = 0;
    int PlayerNumber = 0;
    int MyTeam = 0;
    int ActivePlayers = 0;
    int ActiveTeam = 0;
    boolean[] ActiveTeams = new boolean[32];
    boolean GameCreationCompleted = false;
    boolean MyTurn = false;
    boolean GameOwner = false;
    boolean Connected = false;
    boolean TeamGame = false;
    int CurrentPlayer = 0;
    String MyIP = "0.0.0.0";
    String MyPublicIP = "0.0.0.0";
    int SessionID = 0;
    WTMPMessage msg;
    ClientContext NetContext;
    boolean NetContextConnected = false;
    boolean NetContextConnectionFailed = false;
    boolean RoomListingRetrieved = false;
    boolean RoomInfoUpdated = false;
    int PlayerID = 0;
    String LobbyHostID = "";
    int HostPort = 8000;
    DObject ActiveRoom;
    int ActiveRoomID;
    boolean InRoom = false;
    String NetworkError = "";
    String UserGUID = "";
    String UserCredentials = "";
    String UMSPath = "http://cannonballs.wildtangent.com/Cannonballs/AccountManagement/UserManagement.asmx/";
    String LBPath = "http://cannonballs.wildtangent.com/Cannonballs/Leaderboard/Persist/LeaderBoard.asmx/";
    RoomListingMessage.RoomListing[] MasterRooms;
    int MasterRoomCount = 0;
    int ActiveRoomNumber = 0;
    String[] LBTableNames = new String[]{"Most Kills", "Water Baby", "Most Deaths", "Wildest Shot", "Moneybags", "Cheapskate"};
    String[][] LBTableUsers = new String[6][10];
    String[][] LBTableValues = new String[6][10];
    int[] LBTableCount = new int[6];
    String[] StatValues = new String[5];
    boolean RequestingLeaderboard = false;
    boolean RequestingStats = false;
    int StatKills = 0;
    int StatDrowning = 0;
    int StatCash = 0;
    int StatMiss = 0;
    int StatDeaths = 0;
    boolean HasLivePlayers = false;
    Gameplay gameplay = new Gameplay();
    int RoomTries = 0;

    public void removeGame() {
        Channel channel = this.NetContext.getDObjectManager().getChannel("net");
        GameRemovalMessage.removeGame(channel, this.ActiveRoom, this);
    }

    void updateStats() {
        this.RequestingLeaderboard = true;
        String string = this.LBPath + "Submit?guid=" + this.UserGUID + "&kills=" + this.StatKills + "&drownings=" + this.StatDrowning + "&deaths=" + this.StatDeaths + "&misses=" + this.StatMiss + "&moneySpent=" + this.StatCash;
        Main.MainRef.Wt.outDebugString(string);
        this.LBFile = Main.MainRef.Wt.readFile(this.LBPath + "Submit?guid=" + this.UserGUID + "&kills=" + this.StatKills + "&drownings=" + this.StatDrowning + "&deaths=" + this.StatDeaths + "&misses=" + this.StatMiss + "&moneySpent=" + this.StatCash);
        this.LBFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.LBFile.setName("LB");
    }

    void postBotMessage(int n, int n2, int n3) {
        switch (n2) {
            case 1: {
                int n4 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)this.InsultCount);
                Main.MainRef.chat.postChatMessageFromBot(n, this.ChatInsult[n4] + " " + Main.MainRef.cannon[n3].Name + "!");
                return;
            }
            case 4: {
                int n5 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)this.KillCount);
                Main.MainRef.chat.postChatMessageFromBot(n, this.ChatKill[n5]);
                return;
            }
            case 3: {
                int n6 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)this.DeathCount);
                Main.MainRef.chat.postChatMessageFromBot(n, this.ChatDeath[n6]);
                return;
            }
            case 5: {
                int n7 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)this.GreetingCount);
                Main.MainRef.chat.postChatMessageFromBot(n, this.ChatGreeting[n7]);
                return;
            }
            case 2: {
                int n8 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)this.ComplimentCount);
                Main.MainRef.chat.postChatMessageFromBot(n, this.ChatCompliment[n8] + " " + Main.MainRef.cannon[n3].Name + "!");
                return;
            }
        }
    }

    String findAvailableBot() {
        String string = "";
        boolean bl = false;
        while (!bl) {
            bl = true;
            string = Global.BOTNAMES[(int)Math.floor(Main.MainRef.random.nextFloat() * 12.0f)];
            int n = 0;
            while (n < Main.MainRef.MaxGamePlayerCount) {
                if (this.AvailableSlots[n] && this.ClientCheckedIn[n] && string.equalsIgnoreCase(this.PlayerNames[n])) {
                    bl = false;
                }
                ++n;
            }
        }
        return string;
    }

    public void clearPackets() {
        boolean bl = true;
        int n = 0;
        do {
            this.msg.clear();
            int n2 = this.wtSession.receive(0, 0, 0, this.msg);
            if (n2 == 0) {
                ++n;
                continue;
            }
            bl = false;
        } while (bl);
    }

    public void tryNextRoom() {
        int n = this.ActiveRoomNumber + 1;
        ++this.RoomTries;
        if (this.RoomTries > 10) {
            return;
        }
        if (this.UserName.startsWith("`")) {
            if (n >= this.MasterRoomCount - 1) {
                n = 0;
            }
            Channel channel = this.NetContext.getDObjectManager().getChannel("net");
            this.ActiveRoomNumber = n;
            this.ActiveRoomID = this.MasterRooms[this.ActiveRoomNumber].roomId;
            RoomEnterMessage.enterRoom(this.ActiveRoomID, channel, this, this, "" + Main.MainRef.UserState);
            return;
        }
        if (n >= this.MasterRoomCount) {
            n = 0;
        }
        Channel channel = this.NetContext.getDObjectManager().getChannel("net");
        this.ActiveRoomNumber = n;
        this.ActiveRoomID = this.MasterRooms[this.ActiveRoomNumber].roomId;
        RoomEnterMessage.enterRoom(this.ActiveRoomID, channel, this, this, "" + Main.MainRef.UserState);
    }

    public void onLoadComplete(WTObject wTObject) {
        if (this.UMSRequesting && this.UMSFile != null && wTObject.getName().equals("UMS")) {
            this.UMSRequesting = false;
            String string = this.UMSFile.readLine();
            this.UMSResult = string.equalsIgnoreCase("yes");
            if (!this.UMSFile.eof()) {
                this.UMSResultValue = this.UMSFile.readLine();
            }
            if (!this.UMSFile.eof()) {
                this.UserGUID = this.UMSFile.readLine();
            }
            if (!this.UMSFile.eof()) {
                this.UserCredentials = this.UMSFile.readLine();
            }
            this.UMSFile.close();
            this.UMSFile = null;
        }
        if (this.RequestingLeaderboard && this.LBFile != null && wTObject.getName().equals("LB")) {
            this.RequestingLeaderboard = false;
            int n = 0;
            while (!this.LBFile.eof() && n < 6) {
                int n2;
                this.LBTableCount[n] = n2 = Float.valueOf(this.LBFile.readLine()).intValue();
                if (this.LBTableCount[n] > 10) {
                    this.LBTableCount[n] = 10;
                }
                int n3 = 0;
                int n4 = 0;
                while (n4 < n2) {
                    if (n3 < 10) {
                        this.LBTableUsers[n][n4] = this.LBFile.readLine();
                        this.LBTableValues[n][n4] = this.LBFile.readLine();
                    } else {
                        this.LBFile.readLine();
                        this.LBFile.readLine();
                    }
                    ++n3;
                    ++n4;
                }
                ++n;
            }
            this.LBFile.close();
            this.LBFile = null;
        }
        if (this.RequestingStats && this.SFile != null && wTObject.getName().equals("STATS")) {
            int n = 0;
            do {
                this.StatValues[n] = "0";
            } while (++n < 5);
            this.RequestingStats = false;
            if (!this.SFile.eof()) {
                this.SFile.readLine();
            }
            n = 0;
            while (!this.SFile.eof()) {
                if (n >= 5) {
                    this.SFile.readLine();
                } else {
                    this.StatValues[n] = this.SFile.readLine();
                }
                ++n;
            }
            this.SFile.close();
            this.SFile = null;
        }
    }

    public void receiveJoin(String string) {
        if (Main.MainRef.network.GameOwner && !Main.MainRef.network.GameCreationCompleted) {
            int n = this.findAvailableSlot();
            if (n > 0 && !this.playerExists(string)) {
                Main.MainRef.Wt.outDebugString("ACCEPTED PLAYER " + string);
                this.AvailableSlots[n] = true;
                this.PlayerNames[n] = string;
                Packet packet = new Packet();
                packet.Code = (short)11;
                packet.Name = string;
                packet.Id = (short)n;
                this.IsBot[n] = packet.conditional;
                if (this.TeamGame) {
                    packet.Var1 = this.findLowestTeam();
                    int n2 = this.findLowestTeam();
                    this.TeamMemberCount[n2] = this.TeamMemberCount[n2] + 1;
                }
                this.sendPacket(packet);
                Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
                return;
            }
            Main.MainRef.Wt.outDebugString("REJECTING CLIENT A " + string);
            Packet packet = new Packet();
            packet.Code = (short)15;
            packet.Name = string;
            this.sendPacket(packet);
            return;
        }
        if (Main.MainRef.network.GameOwner && Main.MainRef.network.GameCreationCompleted) {
            Main.MainRef.Wt.outDebugString("REJECTING CLIENT B " + string);
            Packet packet = new Packet();
            packet.Code = (short)15;
            packet.Name = string;
            this.sendPacket(packet);
        }
    }

    Packet receivePacket(byte[] byArray) {
        Packet packet = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            packet = (Packet)object;
        }
        catch (Exception exception) {}
        return packet;
    }

    void addMatchToLobby(String string) {
        Channel channel = this.NetContext.getDObjectManager().getChannel("net");
        GameCreationMessage.createGame(channel, this.ActiveRoom, this, string);
    }

    void retrieveLeaderboard() {
        this.RequestingLeaderboard = true;
        this.LBFile = Main.MainRef.Wt.readFile(this.LBPath + "RetrieveTable");
        this.LBFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.LBFile.setName("LB");
    }

    void unsubscribe() {
        if (this.ActiveRoom != null) {
            this.ActiveRoom.unsubscribe(this);
            this.ActiveRoom = null;
        }
    }

    public void requestPlayerPublicIP() {
        PlayerAttributeMessage.getPlayerAttribute(this.NetContext.getChannel(), this, "ip");
    }

    public boolean hasPublicIP() {
        return !this.MyPublicIP.equalsIgnoreCase("0.0.0.0");
    }

    void verifyUMSName(String string, String string2) {
        this.UMSFile = null;
        this.UMSRequesting = true;
        this.UMSResult = false;
        this.UMSResultValue = "Unknown-Check your network connection";
        this.gameplay.SetScreenBuffer(Main.MainRef.cryptkey.buffer);
        string = this.gameplay.GetPlayerName(0, string);
        string2 = this.gameplay.GetPlayerName(0, string2);
        String string3 = this.UMSPath + "VerifyUser?email=" + URLEncoder.encode(string) + "&pass=" + URLEncoder.encode(string2);
        this.UMSFile = Main.MainRef.Wt.readFile(string3);
        this.UMSFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.UMSFile.setName("UMS");
    }

    int findMostFullRoom() {
        int n = -1;
        int n2 = -1;
        int n3 = 0;
        while (n3 < this.MasterRoomCount) {
            if (this.MasterRooms[n3].playerCount > n2 && this.MasterRooms[n3].playerCount < 100) {
                n = n3;
                n2 = this.MasterRooms[n3].playerCount;
            }
            ++n3;
        }
        if (n == -1) {
            n3 = 0;
            while (n3 < this.MasterRoomCount) {
                if (this.MasterRooms[n3].playerCount > n2) {
                    n = n3;
                    n2 = this.MasterRooms[n3].playerCount;
                }
                ++n3;
            }
        }
        return n;
    }

    public boolean isRoomInfoUpdated() {
        return this.RoomInfoUpdated;
    }

    void postSwapTeam(int n) {
        Packet packet = new Packet();
        packet.Code = (short)28;
        packet.Id = (short)this.PlayerNumber;
        packet.Var1 = n;
        this.MyTeam = n;
        this.sendPacket(packet);
        Main.MainRef.packetmanager.parseIndividualPacket(packet);
    }

    int findLowestTeam() {
        int n = 0;
        int n2 = 4;
        int n3 = 0;
        n = 0;
        while (n < this.TeamCount) {
            if (this.TeamMemberCount[n] == 0) {
                return n;
            }
            if (this.TeamMemberCount[n] < n2) {
                n2 = this.TeamMemberCount[n];
                n3 = n;
            }
            ++n;
        }
        return n3;
    }

    void postSwapColor(int n) {
        Packet packet = new Packet();
        packet.Code = (short)32;
        packet.Id = (short)this.PlayerNumber;
        packet.Var1 = n;
        this.sendPacket(packet);
        Main.MainRef.packetmanager.parseIndividualPacket(packet);
    }

    boolean getUMSResult() {
        return this.UMSResult;
    }

    PacketSmall receivePacketSmall(byte[] byArray) {
        PacketSmall packetSmall = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            packetSmall = (PacketSmall)object;
        }
        catch (Exception exception) {}
        return packetSmall;
    }

    void nullSlots() {
        int n = 0;
        n = 0;
        do {
            this.AvailableSlots[n] = false;
            this.IsBot[n] = false;
            this.ClientConnectionState[n] = 0;
        } while (++n < 20);
    }

    int countRealPlayers() {
        int n = 0;
        int n2 = 0;
        while (n2 < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n2].Active && !Main.MainRef.cannon[n2].IsBot) {
                ++n;
            }
            ++n2;
        }
        return n;
    }

    public void pollNetContext(long l) {
        if (this.NetContext != null) {
            this.NetContext.poll(l);
            if (!this.NetContextConnected && this.NetContext.isConnected()) {
                this.NetContextConnected = true;
                this.PlayerID = this.NetContext.getNodeManager().getLocalNode().getGlobalId();
            }
        }
    }

    void nullCheckIn() {
        int n = 0;
        this.TeamMemberCount[0] = 0;
        this.TeamMemberCount[1] = 0;
        this.TeamMemberCount[2] = 0;
        this.TeamMemberCount[3] = 0;
        n = 0;
        do {
            this.ClientConnectionState[n] = 0;
            this.ClientCheckedIn[n] = false;
            this.ClientConfirmed[n] = 0;
        } while (++n < 20);
    }

    int findFullSlots() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n]) {
                ++n2;
            }
            ++n;
        }
        return n2;
    }

    void updateCurrentMatch(String string) {
        if (this.ActiveRoom != null) {
            Main.MainRef.Wt.outDebugString("UPDATE MATCH " + this.PlayerID + " " + string);
            this.ActiveRoom.set(MatchmakingLobby.getGameInfoKey(this.PlayerID), string);
        }
    }

    void nullChat() {
        int n = 0;
        n = 0;
        do {
            this.ChatMessages[n] = "";
        } while (++n < 20);
    }

    public void setUserState(int n) {
        if (Main.MainRef.UserState == 1 && n != Main.MainRef.UserState) {
            this.resubscribe();
        }
        if (this.ActiveRoom != null && n != Main.MainRef.UserState) {
            this.ActiveRoom.set(Room.getPlayerInfoKey(this.PlayerID), "" + n);
            if (n == 1) {
                this.unsubscribe();
            }
            Main.MainRef.UserState = n;
        }
    }

    int countSlotsConfirmed() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n] && this.ClientConfirmed[n] == 1) {
                ++n2;
            }
            ++n;
        }
        return n2;
    }

    boolean isConnectionValid(String string, String string2) {
        boolean bl = false;
        boolean bl2 = false;
        if (this.MyIP.equalsIgnoreCase(this.MyPublicIP)) {
            bl = true;
        }
        if (!(this.MyIP.startsWith("10.") || this.MyIP.startsWith("172.16.") || this.MyIP.startsWith("192.168.") || this.MyIP.startsWith("192.168.1."))) {
            bl = true;
        }
        if (string.equalsIgnoreCase(string2)) {
            bl2 = true;
        }
        if (!(string.startsWith("10.") || string.startsWith("172.16.") || string.startsWith("192.168.") || string.startsWith("192.168.1."))) {
            bl2 = true;
        }
        return bl ? bl2 : this.MyPublicIP.equalsIgnoreCase(string2);
    }

    void resubscribe() {
        if (this.ActiveRoom == null) {
            this.NetContext.subscribe(this.ActiveRoomID, this);
        }
    }

    void pollBotsForCompliments(int n) {
        int n2 = 0;
        while (n2 < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n2].IsBot && Main.MainRef.cannon[n2].BotOwner == Main.MainRef.network.PlayerNumber && Main.MainRef.random.nextFloat() < 0.1f) {
                this.postBotMessage(n2, 2, n);
            }
            ++n2;
        }
    }

    boolean createNewMatch(String string, int n) {
        this.CreatedGame = true;
        int n2 = this.wtSession.hostSession("cannonballs", "5BFDB060-06A4-11D0-9C4F-00A0C905425E", 0, "", n);
        if (n2 != 0) {
            Main.MainRef.Wt.outDebugString("ERROR " + n2);
            return false;
        }
        this.SessionID = this.wtSession.createPlayer(this.UserName, 0);
        if (this.SessionID == -1) {
            Main.MainRef.Wt.outDebugString("SESSION HOST FAILED ID " + this.SessionID);
            Main.MainRef.Wt.stop();
            this.wtSession.closeSession();
            Main.MainRef.Wt.start();
            return false;
        }
        if (!Main.MainRef.SinglePlayer) {
            this.addMatchToLobby(string);
        }
        this.nullCheckIn();
        this.nullSlots();
        this.MyTurn = true;
        this.GameOwner = true;
        this.PlayerNumber = 0;
        this.PlayerNames[0] = this.UserName;
        if (Main.MainRef.network.TeamGame) {
            this.PlayerTeam[0] = 0;
            this.TeamMemberCount[0] = this.TeamMemberCount[0] + 1;
            this.MyTeam = 0;
        }
        this.ClientCheckedIn[0] = true;
        this.AvailableSlots[0] = true;
        this.ClientConfirmed[0] = 1;
        return true;
    }

    public void retrieveUserList() {
        if (this.ActiveRoom != null) {
            this.NumClients = Room.getPlayerCount(this.ActiveRoom);
            IntIterator intIterator = Room.getPlayersIterator(this.ActiveRoom);
            int n = 0;
            while (intIterator.hasNext()) {
                this.ClientIDs[n] = intIterator.next();
                this.ClientNames[n] = Room.getPlayerName(this.ActiveRoom, this.ClientIDs[n]);
                if (this.ClientIDs[n] == this.PlayerID) {
                    this.UserName = this.ClientNames[n];
                }
                String string = Room.getPlayerInfo(this.ActiveRoom, this.ClientIDs[n]);
                this.ClientIn[n] = Float.valueOf(string).intValue();
                ++n;
            }
            this.NumClients = n;
        }
    }

    public void retrieveGameList() {
        if (this.ActiveRoom != null) {
            this.NumGames = MatchmakingLobby.getGameCount(this.ActiveRoom);
            IntIterator intIterator = MatchmakingLobby.getGameHostIdsIterator(this.ActiveRoom);
            int n = 0;
            while (intIterator.hasNext()) {
                this.HostingPlayerIDs[n] = intIterator.next();
                String string = MatchmakingLobby.getGameInfo(this.ActiveRoom, this.HostingPlayerIDs[n]);
                StringTokenizer stringTokenizer = new StringTokenizer(string, "|");
                this.GameIP[n] = stringTokenizer.nextToken();
                this.GameNames[n] = stringTokenizer.nextToken();
                this.GamePublicIP[n] = MatchmakingLobby.getHostIP(this.ActiveRoom, this.HostingPlayerIDs[n]);
                ++n;
            }
        }
    }

    public boolean didNetContextConnectionFail() {
        return this.NetContextConnectionFailed;
    }

    void assignBotTeams() {
        int n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            int n2;
            if (this.AvailableSlots[n] && this.ClientCheckedIn[n] && this.IsBot[n] && this.TeamMemberCount[n2 = this.PlayerTeam[n]] > 1) {
                this.PlayerTeam[n] = this.findLowestTeam();
                int n3 = n2;
                this.TeamMemberCount[n3] = this.TeamMemberCount[n3] + -1;
                int n4 = this.PlayerTeam[n];
                this.TeamMemberCount[n4] = this.TeamMemberCount[n4] + 1;
            }
            ++n;
        }
    }

    void leaveLobby() {
        this.destroyNetContext();
    }

    int countActiveTeams() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < this.TeamCount) {
            if (this.TeamMemberCount[n] > 0) {
                ++n2;
            }
            ++n;
        }
        return n2;
    }

    void zeroStats() {
        this.StatKills = 0;
        this.StatDrowning = 0;
        this.StatCash = 0;
        this.StatMiss = 0;
        this.StatDeaths = 0;
    }

    void retrieveStats() {
        this.RequestingStats = true;
        this.SFile = Main.MainRef.Wt.readFile(this.LBPath + "RetrieveStats?guid=" + this.UserGUID);
        this.SFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.SFile.setName("STATS");
    }

    void resetRoomTries() {
        this.RoomTries = 0;
    }

    int countSlotsCheckedIn() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n] && this.ClientCheckedIn[n]) {
                ++n2;
            }
            ++n;
        }
        return n2;
    }

    boolean confirmedAndCheckedIn() {
        return this.countSlotsConfirmed() == this.countSlotsCheckedIn() && this.countSlotsConfirmed() > 1;
    }

    public void receivePackets() {
        boolean bl = true;
        byte[] byArray = null;
        int n = 0;
        do {
            this.msg.clear();
            int n2 = this.wtSession.receive(0, 0, 0, this.msg);
            if (n2 == 0) {
                if (!this.msg.isSystemMessage()) {
                    int n3 = this.msg.readInt();
                    if (n3 == 1) {
                        n3 = this.msg.readInt();
                        byArray = this.msg.readData(n3);
                        Main.MainRef.packetmanager.parseIndividualPacketSmall(this.receivePacketSmall(byArray));
                    } else {
                        byArray = this.msg.readData(n3);
                        Main.MainRef.packetmanager.parseIndividualPacket(this.receivePacket(byArray));
                    }
                }
                ++n;
                continue;
            }
            bl = false;
        } while (bl);
    }

    public void handleEvent(DObjectEvent dObjectEvent) {
        Main.MainRef.Wt.outDebugString("HANDLE EVENT : " + dObjectEvent.toLog());
        if (RoomEvent.isRoomEvent(dObjectEvent)) {
            RoomEvent.getRoomEvent(dObjectEvent);
            this.RoomInfoUpdated = true;
            this.retrieveUserList();
        }
        if (ChatEvent.isChatEvent(dObjectEvent)) {
            ChatEvent chatEvent = ChatEvent.getChatEvent(dObjectEvent);
            String string = chatEvent.getSpeaker();
            String string2 = chatEvent.getMessage();
            string2 = Text.doFilter(string2);
            if (Main.MainRef.MenuManager.LobbyScreen.isNameMuted(string)) {
                return;
            }
            if (Main.MainRef.MenuManager.LobbyScreen.GuestChatMuted && string.startsWith("`guest")) {
                return;
            }
            string = '`' + string + ":" + '`' + string2;
            this.receiveChatText(string);
            this.RoomInfoUpdated = true;
        }
        if (MatchmakingLobbyEvent.isLobbyEvent(dObjectEvent)) {
            MatchmakingLobbyEvent.getLobbyEvent(dObjectEvent);
            this.RoomInfoUpdated = true;
            this.retrieveGameList();
        }
        if (dObjectEvent.getDObjectId() == this.ActiveRoomID && dObjectEvent.getEventType() == 1 && this.InRoom) {
            this.ActiveRoom = dObjectEvent.getDObject();
            Main.MainRef.Wt.outDebugString("Requesting game list");
            this.RoomInfoUpdated = true;
            this.retrieveGameList();
        }
    }

    void postGameBeginEvent() {
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (!this.ClientCheckedIn[n]) {
                Main.MainRef.Wt.outDebugString("REJECTING CLIENT C ");
                Packet packet = new Packet();
                packet.Code = (short)15;
                packet.Name = this.PlayerNames[n];
                this.sendPacket(packet);
            }
            ++n;
        }
        if (!Main.MainRef.SinglePlayer) {
            this.removeGame();
            Main.MainRef.network.setUserState(1);
        }
        Main.MainRef.network.GameCreationCompleted = true;
        Main.MainRef.MenuManager.activateGame(1);
        Main.MainRef.camera.showMouse();
        Main.MainRef.CannonCount = Main.MainRef.network.findFullSlots();
        Packet packet = new Packet();
        Main.MainRef.ChestCount = (int)(Math.random() * 5.0 + 3.0);
        packet.Code = 0;
        packet.Id = (short)Main.MainRef.ActiveMap;
        packet.Var1 = Main.MainRef.CannonCount;
        packet.Var2 = Main.MainRef.ChestCount;
        Main.MainRef.HotSeatTime = Main.MainRef.MenuManager.LobbyScreen.HotSeatSelection;
        Main.MainRef.MaxRespawns = Main.MainRef.MenuManager.LobbyScreen.RespawnSelection;
        packet.Var3 = Main.MainRef.MenuManager.LobbyScreen.HotSeatSelection;
        packet.X1 = Main.MainRef.MenuManager.LobbyScreen.RespawnSelection;
        packet.Y1 = Main.MainRef.MenuManager.LobbyScreen.StartingCashSelection;
        packet.Z1 = Main.MainRef.MenuManager.LobbyScreen.TreasureRespawnSelection;
        Main.MainRef.StartingCash = Main.MainRef.MenuManager.LobbyScreen.StartingCashSelection;
        Main.MainRef.TreasureRespawn = Main.MainRef.MenuManager.LobbyScreen.TreasureRespawnSelection;
        packet.Name = this.UserName;
        this.sendPacket(packet);
    }

    int findWinningTeam() {
        int n = 0;
        int n2 = 0;
        n2 = 0;
        while (n2 < this.TeamCount) {
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (this.PlayerTeam[n] == n2 && Main.MainRef.cannon[n].Active) {
                    return n2;
                }
                ++n;
            }
            ++n2;
        }
        return 0;
    }

    public void handleError(NetworkNode networkNode, Throwable throwable) {
        this.NetworkError = throwable.toString();
        this.NetworkError = this.NetworkError.equalsIgnoreCase("java.net.ConnectException: Connection refused") ? "Lobby Not Responding" : (this.NetworkError.equalsIgnoreCase("java.io.IOException: Connection refused") ? "Lobby Not Responding" : (this.NetworkError.equalsIgnoreCase("java.io.IOException: Host unreachable") ? "Unable To Reach Lobby" : "Connection Failure"));
        Main.MainRef.Wt.outDebugString("NetworkError : " + throwable.toString());
        this.NetContextConnectionFailed = true;
    }

    void initialize() {
        this.wtSession = this.wtMulti.createSession();
        this.MyIP = this.wtSession.getLocalIPAddress();
        this.msg = this.wtMulti.createMessage();
    }

    void sendPacketUnGuaranteed(PacketSmall packetSmall) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(packetSmall);
            objectOutputStream.flush();
            byte[] byArray = byteArrayOutputStream.toByteArray();
            this.msg.clear();
            this.msg.writeInt(1);
            this.msg.writeInt(byArray.length);
            this.msg.writeDataFromByteArray(byArray, byArray.length);
            int n = this.wtSession.send(this.SessionID, 0, 0, this.msg);
            return;
        }
        catch (Exception exception) {
            return;
        }
    }

    int countPlayersInTeam(int n) {
        int n2 = 0;
        int n3 = 0;
        while (n3 < Main.MainRef.CannonCount) {
            if (this.PlayerTeam[n3] == n && Main.MainRef.cannon[n3].Active) {
                ++n2;
            }
            ++n3;
        }
        return n2;
    }

    void setHasLivePlayers(boolean bl) {
        this.HasLivePlayers = bl;
    }

    int countTeamsWithActivePlayers() {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        n2 = 0;
        while (n2 < this.TeamCount) {
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (this.PlayerTeam[n] == n2 && Main.MainRef.cannon[n].Active) {
                    ++n3;
                    break;
                }
                ++n;
            }
            ++n2;
        }
        return n3;
    }

    boolean isLeaderboardRequesting() {
        return this.RequestingLeaderboard;
    }

    String getUMSResultString() {
        return this.UMSResultString;
    }

    void destroyNetContext() {
        if (this.NetContext != null) {
            this.ActiveRoom = null;
            this.InRoom = false;
            this.RoomListingRetrieved = false;
            this.NetContext.shutdown();
            this.NetContext = null;
            this.NetContextConnected = false;
            this.NetContextConnectionFailed = false;
        }
    }

    public void requestRoomListing() {
        if (this.NetContextConnected) {
            Main.MainRef.Wt.outDebugString("REQUESTING ROOM LISTING");
            Channel channel = this.NetContext.getDObjectManager().getChannel("net");
            RoomListingMessage.requestRoomListing(channel, this, "cannonballs.lobby");
        }
    }

    boolean playerExists(String string) {
        int n = 0;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n] && this.PlayerNames[n].equals(string)) {
                return true;
            }
            ++n;
        }
        return false;
    }

    public void handleServiceResponse(ServiceMessage serviceMessage) {
        Channel channel;
        Main.MainRef.Wt.outDebugString(serviceMessage.toString());
        if (PlayerAttributeMessage.isPlayerAttributeMessage(serviceMessage)) {
            this.MyPublicIP = PlayerAttributeMessage.getPlayerAttribute(serviceMessage, "ip");
            Main.MainRef.Wt.outDebugString("GOT PLAYER'S PUBLIC IP " + this.MyPublicIP);
        }
        if (RoomListingMessage.isRoomListingMessage(serviceMessage)) {
            if (RoomListingMessage.isRoomListingSuccess(serviceMessage)) {
                this.RoomTries = 0;
                this.RoomListingRetrieved = true;
                this.MasterRooms = null;
                this.MasterRooms = RoomListingMessage.getRoomListings(serviceMessage);
                this.MasterRoomCount = this.MasterRooms.length;
                this.ActiveRoomNumber = this.UserName.startsWith("`") ? this.findMostFullGuestRoom() : this.findMostFullRoom();
                if (this.ActiveRoomNumber >= this.MasterRoomCount) {
                    this.ActiveRoomNumber = this.MasterRoomCount - 1;
                }
                channel = this.NetContext.getDObjectManager().getChannel("net");
                this.ActiveRoomID = this.MasterRooms[this.ActiveRoomNumber].roomId;
                RoomEnterMessage.enterRoom(this.ActiveRoomID, channel, this, this, "" + Main.MainRef.UserState);
            } else {
                Main.MainRef.Wt.outDebugString("ROOM LISTING FAILURE : " + RoomListingMessage.getFailureReason(serviceMessage));
            }
        }
        if (RoomEnterMessage.isRoomEnterMessage(serviceMessage)) {
            channel = this.NetContext.getDObjectManager().getChannel("net");
            if (RoomEnterMessage.isRoomEnterSuccess(serviceMessage)) {
                this.InRoom = true;
                this.ActiveRoom = this.NetContext.getDObjectManager().getLocalDObject(channel, this.ActiveRoomID);
                this.setUserState(0);
                if (Main.MainRef.MenuManager.ActiveMenu == 2 && Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 0) {
                    Main.MainRef.MenuManager.LobbyScreen.hideAll();
                    this.nullChat();
                    Main.MainRef.MenuManager.LobbyScreen.showBaseMenu(true);
                    return;
                }
            } else {
                Main.MainRef.Wt.outDebugString("ROOM ENTRY FAILURE : " + RoomEnterMessage.getFailureReason(serviceMessage));
                RoomEnterMessage.handleFailure(serviceMessage, this.ActiveRoomID, channel, this);
                if (RoomEnterMessage.getFailureReason(serviceMessage).startsWith("room_full")) {
                    this.tryNextRoom();
                }
            }
        }
    }

    int findAvailableSlot() {
        int n = 0;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (!this.AvailableSlots[n]) {
                return n;
            }
            ++n;
        }
        return -1;
    }

    void destroyMatch() {
        this.destroyPlayer();
        this.closeSession();
    }

    boolean findSlotsConfirmed() {
        int n = 0;
        boolean bl = true;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n] && this.ClientConfirmed[n] != 0) {
                bl = false;
            }
            ++n;
        }
        return bl;
    }

    void notifyJoinersOfCancel() {
        this.removeGame();
        Packet packet = new Packet();
        packet.Code = (short)14;
        this.sendPacket(packet);
    }

    public boolean changeRooms(int n) {
        if (n != this.ActiveRoomNumber) {
            Channel channel = this.NetContext.getDObjectManager().getChannel("net");
            RoomLeaveMessage.leaveRoom(this.ActiveRoomID, channel, this, this);
            if (n >= this.MasterRoomCount) {
                n = this.MasterRoomCount - 1;
            }
            this.ActiveRoomNumber = n;
            this.ActiveRoomID = this.MasterRooms[this.ActiveRoomNumber].roomId;
            RoomEnterMessage.enterRoom(this.ActiveRoomID, channel, this, this, "" + Main.MainRef.UserState);
            return true;
        }
        return false;
    }

    void verifyGuestName(String string) {
        this.UMSFile = null;
        this.UMSRequesting = true;
        this.UMSResult = false;
        this.UMSResultValue = "Unknown-Check your network connection";
        this.UMSFile = Main.MainRef.Wt.readFile(this.UMSPath + "VerifyGuest?displayname=" + string + "&realname=pablo");
        this.UMSFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.UMSFile.setName("UMS");
    }

    void addBot(int n) {
        int n2 = this.findAvailableSlot();
        if (n2 > 0) {
            this.BotType[n2] = n;
            this.AvailableSlots[n2] = true;
            this.PlayerNames[n2] = this.findAvailableBot();
            this.IsBot[n2] = true;
            this.ClientConfirmed[n2] = 1;
            this.ClientCheckedIn[n2] = true;
            if (Main.MainRef.network.TeamGame) {
                this.PlayerTeam[n2] = this.findLowestTeam();
                int n3 = this.findLowestTeam();
                this.TeamMemberCount[n3] = this.TeamMemberCount[n3] + 1;
            }
            Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
        }
    }

    void destroyPlayer() {
        if (this.wtMulti != null) {
            this.wtSession.destroyPlayer(this.SessionID);
        }
    }

    int joinGame(int n) {
        this.PlayerNumber = -1;
        boolean bl = false;
        int n2 = this.wtSession.connectToSession(this.GameIP[n], "5BFDB060-06A4-11d0-9C4F-00A0C905425E", 0, "", 20000);
        if (n2 != 0) {
            if (n2 != 32) {
                Main.MainRef.Wt.stop();
                this.wtSession.closeSession();
                Main.MainRef.Wt.start();
                Main.MainRef.Wt.outDebugString("SESSION CONNECT FAILED ERROR " + n2);
                return 0;
            }
            return 2;
        }
        bl = true;
        this.SessionID = this.wtSession.createPlayer(this.UserName, 0);
        if (this.SessionID == -1) {
            Main.MainRef.Wt.outDebugString("SESSION CONNECT FAILED ID " + this.SessionID);
            Main.MainRef.Wt.stop();
            this.wtSession.closeSession();
            Main.MainRef.Wt.start();
            return 0;
        }
        Packet packet = new Packet();
        packet.Code = (short)29;
        packet.Name = this.UserName;
        packet.conditional = false;
        this.sendPacket(packet);
        this.MyTurn = false;
        this.GameOwner = false;
        return 1;
    }

    void postChatMessage(String string) {
        if (this.NetContextConnected) {
            if (string.length() <= 0) {
                return;
            }
            string = Text.doFilter(string);
            ChatEvent.sendSpeakEvent(this.ActiveRoom, string);
        }
    }

    void sendEmail(String string, String string2) {
        if (this.Connected) {
            String string3 = this.UMSPath + "SendEmail?email=" + string2 + "&displayname=" + string;
            this.TrackFile = null;
            this.TrackFile = Main.MainRef.Wt.readFile(string3, 1);
        }
    }

    void nullTeams() {
        int n = 0;
        n = 0;
        do {
            this.ActiveTeams[n] = false;
            this.LastTeamPlayer[n] = 0;
        } while (++n < 20);
    }

    void shutdown() {
        Packet packet;
        if (Main.MainRef.MenuManager.ActiveMenu == 2 && this.CreatedGame || this.GameOwner && Main.MainRef.GameState == 6 || this.GameOwner && Main.MainRef.GameState == 1 || this.GameOwner && Main.MainRef.GameState == 2) {
            this.notifyJoinersOfCancel();
        }
        if (Main.MainRef.GameState == 3) {
            packet = new Packet();
            packet.Code = (short)7;
            packet.Var1 = -1.0f;
            packet.Id = (short)this.PlayerNumber;
            packet.Var2 = (short)this.PlayerNumber;
            this.sendPacket(packet);
            Main.MainRef.packetmanager.parseIndividualPacket(packet);
        }
        if (!this.GameOwner && Main.MainRef.GameState == 6 || !this.GameOwner && Main.MainRef.GameState == 4 || !this.GameOwner && Main.MainRef.GameState == 5) {
            packet = new Packet();
            packet.Code = (short)34;
            packet.Id = (short)this.PlayerNumber;
            this.sendPacket(packet);
        }
        if (this.wtMulti != null) {
            this.wtMulti.shutdown();
        }
        this.destroyNetContext();
    }

    boolean findSlotsCheckedIn() {
        int n = 0;
        boolean bl = true;
        n = 0;
        while (n < Main.MainRef.MaxGamePlayerCount) {
            if (this.AvailableSlots[n] && !this.ClientCheckedIn[n]) {
                bl = false;
            }
            ++n;
        }
        return bl;
    }

    int findMostFullGuestRoom() {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        while (n3 < this.MasterRoomCount - 1) {
            if (this.MasterRooms[n3].playerCount > n2 && this.MasterRooms[n3].playerCount < 100) {
                n = n3;
                n2 = this.MasterRooms[n3].playerCount;
            }
            ++n3;
        }
        if (n == -1) {
            n3 = 0;
            while (n3 < this.MasterRoomCount - 1) {
                if (this.MasterRooms[n3].playerCount > n2) {
                    n = n3;
                    n2 = this.MasterRooms[n3].playerCount;
                }
                ++n3;
            }
        }
        return n;
    }

    public boolean amInRoom() {
        return this.ActiveRoom != null && this.InRoom;
    }

    public boolean verifyNetContextConnection() {
        if (this.NetContext == null) {
            this.NetContextConnected = true;
            return false;
        }
        if (this.NetContext.isConnected()) {
            return true;
        }
        this.NetContextConnected = false;
        return false;
    }

    void closeSession() {
        this.clearPackets();
        Main.MainRef.Wt.stop();
        this.wtSession.closeSession();
        Main.MainRef.Wt.start();
    }

    void sendPacket(Packet packet) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(packet);
            objectOutputStream.flush();
            byte[] byArray = byteArrayOutputStream.toByteArray();
            this.msg.clear();
            this.msg.writeInt(byArray.length);
            this.msg.writeDataFromByteArray(byArray, byArray.length);
            int n = this.wtSession.send(this.SessionID, 0, 1, this.msg);
            return;
        }
        catch (Exception exception) {
            Main.MainRef.Wt.outDebugString("Exception on send: " + exception.toString());
            return;
        }
    }

    public Network(String string, int n) {
        this.LobbyHostID = string;
        this.HostPort = n;
        this.nullChat();
        this.nullCheckIn();
        this.nullSlots();
    }

    public void setRoomInfoUpdated(boolean bl) {
        this.RoomInfoUpdated = bl;
    }

    public boolean isNetContextConnected() {
        return this.NetContextConnected;
    }

    void postGameConfirmEvent(int n) {
        Packet packet = new Packet();
        packet.Code = (short)27;
        packet.Id = (short)this.PlayerNumber;
        packet.Z2 = n;
        this.sendPacket(packet);
    }

    boolean isStatsRequesting() {
        return this.RequestingStats;
    }

    boolean isUMSRequesting() {
        return this.UMSRequesting;
    }

    void receiveChatText(String string) {
        int n = 0;
        n = 0;
        do {
            this.ChatMessages[n] = this.ChatMessages[n + 1];
        } while (++n < 19);
        this.ChatMessages[19] = string;
    }

    boolean gameHasLivePlayers() {
        return this.HasLivePlayers;
    }

    void createNetContext() {
        if (this.NetContext == null) {
            this.NetContextConnected = false;
            this.NetContextConnectionFailed = false;
            NativeSystemInitializer nativeSystemInitializer = new NativeSystemInitializer();
            this.NetContext = nativeSystemInitializer.createNetworkClient(this.LobbyHostID, (short)this.HostPort, this.UserName, this.UserCredentials, this);
            return;
        }
        this.destroyNetContext();
        NativeSystemInitializer nativeSystemInitializer = new NativeSystemInitializer();
        this.NetContext = nativeSystemInitializer.createNetworkClient(this.LobbyHostID, (short)this.HostPort, this.UserName, this.UserCredentials, this);
    }

    void createUMSName(String string, String string2, String string3) {
        this.UMSFile = null;
        this.UMSRequesting = true;
        this.UMSResult = false;
        this.UMSResultValue = "Unknown-Check your network connection";
        this.gameplay.SetScreenBuffer(Main.MainRef.cryptkey.buffer);
        string = this.gameplay.GetPlayerName(0, string);
        string2 = this.gameplay.GetPlayerName(0, string2);
        string3 = this.gameplay.GetPlayerName(0, string3);
        String string4 = this.UMSPath + "NewUser?email=" + URLEncoder.encode(string) + "&pass=" + URLEncoder.encode(string2) + "&displayname=" + URLEncoder.encode(string3) + "&dpname=" + Main.MainRef.DPName;
        this.UMSFile = Main.MainRef.Wt.readFile(string4);
        this.UMSFile.setOnLoadedWithChildren((WTOnLoadEvent)this);
        this.UMSFile.setName("UMS");
    }
}

