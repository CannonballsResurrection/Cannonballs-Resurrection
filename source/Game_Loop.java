/*
 * Decompiled with CFR 0.152.
 */
public class Game_Loop
implements Global {
    float RequestPacketTimer = 0.0f;
    float SendPingTimer = 0.0f;
    int GameStateTimeOut = 0;
    int BeginTries = 0;
    boolean StartupDataReceived = false;
    float TimeSinceBeginInfo = 0.0f;

    void masterSetup(float f) {
        int n = 0;
        Main.MainRef.MenuManager.updateLoading(f);
        if (Main.MainRef.island.isLoaded() && Main.MainRef.MediaList.allLoaded()) {
            Main.MainRef.Wt.outDebugString("MASTER ISLAND LOADED");
            Main.MainRef.GameState = 6;
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                Main.MainRef.cannon[n].place(true, false);
                Main.MainRef.cannon[n].toGround();
                Main.MainRef.cannon[n].attachFlags();
                Main.MainRef.cannon[n].Name = Main.MainRef.network.PlayerNames[n];
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                Main.MainRef.chest[n].findPlace();
                Main.MainRef.chest[n].generateTreasure();
                ++n;
            }
            Main.MainRef.network.FirstPlayer = (int)Math.floor(Math.random() * (double)Main.MainRef.CannonCount);
            Main.MainRef.camera.showMouse();
            this.broadcastMasterBegin();
            this.RequestPacketTimer = 0.0f;
            this.BeginTries = 0;
        }
        this.SendPingTimer += f;
        if (this.SendPingTimer > 1000.0f) {
            this.SendPingTimer = 0.0f;
            Packet packet = new Packet();
            packet.Code = (short)31;
            packet.Id = (short)Main.MainRef.network.PlayerNumber;
            packet.type = 0;
            Main.MainRef.network.sendPacket(packet);
        }
        Main.MainRef.MenuManager.showGameConnectionStats();
    }

    void broadcastMasterBegin() {
        int n = 0;
        Packet packet = new Packet();
        n = 0;
        while (n < Main.MainRef.CannonCount) {
            Main.MainRef.cannon[n].Color = Main.MainRef.network.CannonColor[n];
            if (Main.MainRef.network.TeamGame) {
                Main.MainRef.cannon[n].Color = Main.MainRef.network.PlayerTeam[n];
            }
            packet.Code = 1;
            packet.Id = (short)n;
            packet.Name = Main.MainRef.cannon[n].Name;
            packet.type = Main.MainRef.network.BotType[n];
            packet.X1 = Main.MainRef.cannon[n].Position.X;
            packet.Y1 = Main.MainRef.cannon[n].Position.Y;
            packet.Z1 = Main.MainRef.cannon[n].Position.Z;
            packet.X2 = Main.MainRef.network.FirstPlayer;
            packet.Z2 = Main.MainRef.cannon[n].Color;
            packet.Var1 = Main.MainRef.island.WindDirection;
            packet.Var2 = Main.MainRef.island.WindVelocity;
            packet.Var3 = Main.MainRef.network.PlayerTeam[n];
            packet.conditional = Main.MainRef.network.IsBot[n];
            packet.Y2 = Main.MainRef.network.PlayerNumber;
            Main.MainRef.network.sendPacket(packet);
            Main.MainRef.Wt.outDebugString("BROADCASTING PLAYER START " + n);
            ++n;
        }
        n = 0;
        while (n < Main.MainRef.ChestCount) {
            packet.Code = (short)18;
            packet.Id = (short)n;
            packet.X1 = Main.MainRef.chest[n].X;
            packet.Y1 = Main.MainRef.chest[n].Y;
            packet.Z1 = Main.MainRef.chest[n].Z;
            packet.Var1 = Main.MainRef.chest[n].Contents;
            Main.MainRef.network.sendPacket(packet);
            Main.MainRef.Wt.outDebugString("BROADCASTING CHEST START " + n);
            ++n;
        }
        Packet packet2 = new Packet();
        packet2.Code = (short)10;
        packet2.Id = (short)Main.MainRef.network.PlayerNumber;
        Main.MainRef.network.sendPacket(packet2);
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Synced = true;
    }

    void updateGameStateDestroy(float f) {
        this.GameStateTimeOut = (int)((float)this.GameStateTimeOut + f);
        this.updateGameStateActive(f);
        if (this.GameStateTimeOut > 15000) {
            Main.MainRef.timer.hide();
            this.GameStateTimeOut = 0;
            Main.MainRef.MenuManager.showDissolve();
            Main.MainRef.GameState = 17;
        }
    }

    void clientInitialize() {
        this.GameStateTimeOut = 0;
        Main.MainRef.Wt.outDebugString("CLIENT INITIALIZED");
        Main.MainRef.island = new Island(Main.MainRef.MapTracker.MapPaths[Main.MainRef.ActiveMap]);
        Main.MainRef.GameState = 5;
        Main.MainRef.GameLoop.TimeSinceBeginInfo = 0.0f;
        Main.MainRef.network.nullTeams();
    }

    void destroyGame() {
        int n = 0;
        Main.MainRef.Wt.outDebugString("ATTEMPTING GAME DESTRUCTION");
        try {
            Main.MainRef.MenuManager.hideGameConnectionStats();
            Main.MainRef.camera.addCameraToStage();
            Main.MainRef.packetmanager.closeForPackets();
            Main.MainRef.Wt.outDebugString("BEGINNING GAME DESTRUCTION");
            Main.MainRef.network.destroyMatch();
            Main.MainRef.Wt.outDebugString("COMPLETED NETWORK DESTRUCTION");
            Main.MainRef.hud.reset();
            Main.MainRef.timer.hide();
            Main.MainRef.network.MyTurn = false;
            Main.MainRef.Wt.outDebugString("COMPLETED HUD DESTRUCTION");
            Main.MainRef.camera.hideEnvironment();
            Main.MainRef.ParticleList.removePermanent();
            Main.MainRef.ParticleList.destroy();
            Main.MainRef.ChunkList.destroy();
            if (Main.MainRef.island != null) {
                Main.MainRef.island.destroy();
            }
            Main.MainRef.island = null;
            Main.MainRef.chat.clearChat();
            Main.MainRef.Wt.outDebugString("COMPLETED ISLAND DESTRUCTION");
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n] != null) {
                    Main.MainRef.cannon[n].Active = false;
                    Main.MainRef.cannon[n].destroy();
                    Main.MainRef.cannon[n] = null;
                }
                ++n;
            }
            Main.MainRef.CannonCount = 0;
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n] != null) {
                    Main.MainRef.chest[n].destroy();
                    Main.MainRef.chest[n] = null;
                }
                ++n;
            }
            Main.MainRef.ChestCount = 0;
            Main.MainRef.Wt.outDebugString("COMPLETED OBJECT DESTRUCTION");
            Main.MainRef.network.CurrentPlayer = 0;
            Main.MainRef.network.ActivePlayers = 0;
            this.GameStateTimeOut = 0;
            Main.MainRef.network.CreatedGame = false;
            this.StartupDataReceived = false;
            Main.MainRef.network.GameCreationCompleted = false;
            Main.MainRef.network.CreatedGame = false;
            Main.MainRef.network.nullCheckIn();
            Main.MainRef.network.nullSlots();
            Main.MainRef.GameState = 10;
            Main.MainRef.MenuManager.ActiveMenu = -1;
            if (Main.MainRef.SinglePlayer) {
                Main.MainRef.MenuManager.activateMenuInstant(3);
            } else if (!Main.MainRef.network.verifyNetContextConnection()) {
                Main.MainRef.network.leaveLobby();
                Main.MainRef.MenuManager.activateMenuInstant(3);
            } else if (Main.MainRef.network.gameHasLivePlayers() && !Main.MainRef.network.UserName.startsWith("`")) {
                Main.MainRef.MenuManager.activateMenuInstant(5);
            } else {
                Main.MainRef.MenuManager.activateMenuInstant(2);
            }
            Main.MainRef.Wt.outDebugString("COMPLETED GAME DESTRUCTION");
            return;
        }
        catch (Exception exception) {
            Main.MainRef.Wt.outDebugString(exception.toString());
            exception.printStackTrace();
            return;
        }
    }

    void clientMasterSync(float f) {
        float f2;
        int n;
        int n2 = 0;
        boolean bl = true;
        Main.MainRef.MenuManager.updateLoading(f);
        if (Main.MainRef.network.GameOwner) {
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].TimeSincePing = 0.0f;
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Synced = true;
            n2 = 0;
            while (n2 < Main.MainRef.CannonCount) {
                Main.MainRef.cannon[n2].TimeSincePing += f;
                if (!Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < 60000.0f) {
                    bl = false;
                }
                ++n2;
            }
            if (bl) {
                n = 0;
                f2 = 60000.0f;
                n2 = 0;
                while (n2 < Main.MainRef.CannonCount) {
                    if (Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < 60000.0f) {
                        ++n;
                    }
                    if (!Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < f2) {
                        f2 = Main.MainRef.cannon[n2].TimeSincePing;
                    }
                    ++n2;
                }
                if (n <= 1) {
                    bl = false;
                }
                if (n <= 1 && f2 >= 60000.0f) {
                    Main.MainRef.Wt.outDebugString("NO OTHER PLAYERS AND HAVE WAITED 60 SECONDS - LEAVING");
                    Main.MainRef.MenuManager.LobbyScreen.cancelGameCreationAfterMenu();
                    return;
                }
            }
        } else {
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].TimeSincePing = 0.0f;
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Synced = true;
            n2 = 0;
            while (n2 < Main.MainRef.CannonCount) {
                Main.MainRef.cannon[n2].TimeSincePing += f;
                if (!Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < 60000.0f) {
                    bl = false;
                }
                ++n2;
            }
            if (bl) {
                n = 0;
                f2 = 60000.0f;
                n2 = 0;
                while (n2 < Main.MainRef.CannonCount) {
                    if (Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < 60000.0f) {
                        ++n;
                    }
                    if (!Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing < f2) {
                        f2 = Main.MainRef.cannon[n2].TimeSincePing;
                    }
                    ++n2;
                }
                if (n <= 1) {
                    bl = false;
                }
                if (n <= 1 && f2 >= 60000.0f) {
                    Main.MainRef.Wt.outDebugString("NO OTHER PLAYERS AND HAVE WAITED 60 SECONDS - LEAVING");
                    Main.MainRef.network.destroyPlayer();
                    Main.MainRef.network.closeSession();
                    this.destroyGame();
                    return;
                }
            }
        }
        Main.MainRef.MenuManager.showGameConnectionStats();
        if (bl) {
            if (Main.MainRef.network.GameOwner) {
                n2 = 0;
                while (n2 < Main.MainRef.CannonCount) {
                    if (!Main.MainRef.cannon[n2].Synced && Main.MainRef.cannon[n2].TimeSincePing >= 60000.0f) {
                        Main.MainRef.cannon[n2].PacketAliveTime = 20.0f;
                    }
                    ++n2;
                }
            }
            Main.MainRef.network.setHasLivePlayers(false);
            n2 = 0;
            while (n2 < Main.MainRef.CannonCount) {
                if (n2 != Main.MainRef.network.PlayerNumber && !Main.MainRef.cannon[n2].IsBot) {
                    Main.MainRef.network.setHasLivePlayers(true);
                }
                ++n2;
            }
            if (Main.MainRef.GlobalMedia.ShellMusic != null && Main.MainRef.GlobalMedia.ShellMusic.getIsPlaying()) {
                Main.MainRef.GlobalMedia.ShellMusic.stop();
            }
            Main.MainRef.Wt.outDebugString("BEGINNING STARTUP AFTER SYNC");
            Main.MainRef.MenuManager.hideLoading();
            Main.MainRef.MenuManager.hideGameConnectionStats();
            Main.MainRef.hud.resetTargetNames();
            Main.MainRef.GameState = 16;
            if (Main.MainRef.network.TeamGame) {
                n2 = 0;
                while (n2 < Main.MainRef.CannonCount) {
                    Main.MainRef.network.ActiveTeams[Main.MainRef.network.PlayerTeam[n2]] = true;
                    ++n2;
                }
            }
            Main.MainRef.network.CurrentPlayer = Main.MainRef.network.FirstPlayer;
            Main.MainRef.network.MyTurn = Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber;
            if (Main.MainRef.network.TeamGame) {
                Main.MainRef.network.ActiveTeam = Main.MainRef.network.PlayerTeam[Main.MainRef.network.CurrentPlayer];
                Main.MainRef.network.LastTeamPlayer[Main.MainRef.network.PlayerTeam[Main.MainRef.network.CurrentPlayer]] = Main.MainRef.network.CurrentPlayer;
            }
            Main.MainRef.hud.setTargetNameActive(Main.MainRef.network.CurrentPlayer);
            Main.MainRef.hud.show();
            if (Main.MainRef.network.MyTurn) {
                Main.MainRef.hud.showMyTurn();
            } else {
                Main.MainRef.hud.showOtherTurn();
            }
            Main.MainRef.island.show();
            Main.MainRef.island.update(1.0E-4f);
            Main.MainRef.island.positionCamera();
            n2 = 0;
            while (n2 < Main.MainRef.ChestCount) {
                Main.MainRef.chest[n2].spawn();
                ++n2;
            }
            Main.MainRef.island.beginSounds();
            Main.MainRef.camera.showEnvironment();
            n2 = 0;
            while (n2 < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n2].Synced) {
                    Main.MainRef.cannon[n2].TimeSincePing = 0.0f;
                    Main.MainRef.cannon[n2].PacketAliveTime = 0.0f;
                }
                Main.MainRef.cannon[n2].show();
                Main.MainRef.cannon[n2].toGround();
                ++n2;
            }
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].activateCamera();
            Main.MainRef.hud.showWindSpeed();
            Main.MainRef.island.placeMiniMapPlayers();
            Main.MainRef.island.placeMiniMapChests();
            Main.MainRef.Wt.outDebugString("COMPLETED STARTUP");
            Main.MainRef.camera.updateCamera(1.0E-4f, 0);
            Main.MainRef.ParticleList.update(1.0E-4f);
        } else if (Main.MainRef.network.GameOwner) {
            this.RequestPacketTimer += f;
            if (this.RequestPacketTimer > 4000.0f) {
                this.RequestPacketTimer = 0.0f;
                this.broadcastMasterBegin();
                ++this.BeginTries;
                if (this.BeginTries > 100) {
                    Main.MainRef.Wt.outDebugString("TRIED TO START GAME 100 TIMES AND FAILED");
                    Main.MainRef.MenuManager.LobbyScreen.cancelGameCreationAfterMenu();
                    this.BeginTries = 0;
                }
            }
        }
        this.SendPingTimer += f;
        if (this.SendPingTimer > 1000.0f) {
            this.SendPingTimer = 0.0f;
            Packet packet = new Packet();
            packet.Code = (short)31;
            packet.type = 2;
            packet.Id = (short)Main.MainRef.network.PlayerNumber;
            Main.MainRef.network.sendPacket(packet);
        }
    }

    void updateGameStateSpectator(float f) {
        this.GameStateTimeOut = (int)((float)this.GameStateTimeOut + f);
        this.updateGameStateActive(f);
        if (this.GameStateTimeOut > 9000) {
            this.GameStateTimeOut = 0;
            if (Main.MainRef.network.TeamGame) {
                if (Main.MainRef.network.countTeamsWithActivePlayers() < 2) {
                    this.destroyGame();
                    Main.MainRef.timer.hide();
                    return;
                }
            } else if (Main.MainRef.network.ActivePlayers < 2) {
                this.destroyGame();
                Main.MainRef.timer.hide();
            }
        }
    }

    void switchPlayers() {
        boolean bl = false;
        if (Main.MainRef.network.ActivePlayers < 2) {
            return;
        }
        if (Main.MainRef.network.TeamGame && Main.MainRef.network.countTeamsWithActivePlayers() < 2) {
            return;
        }
        Main.MainRef.hud.setTargetNameInActive(Main.MainRef.network.CurrentPlayer);
        if (Main.MainRef.network.TeamGame) {
            bl = false;
            Main.MainRef.network.LastTeamPlayer[Main.MainRef.network.ActiveTeam] = Main.MainRef.network.CurrentPlayer;
            while (!bl) {
                ++Main.MainRef.network.ActiveTeam;
                if (Main.MainRef.network.ActiveTeam >= Main.MainRef.network.TeamCount) {
                    Main.MainRef.network.ActiveTeam = 0;
                }
                if (!Main.MainRef.network.ActiveTeams[Main.MainRef.network.ActiveTeam] || Main.MainRef.network.countPlayersInTeam(Main.MainRef.network.ActiveTeam) <= 0) continue;
                bl = true;
            }
            bl = false;
            if (Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber && Main.MainRef.HotSeatTime > 0) {
                Main.MainRef.timer.hide();
            }
            Main.MainRef.network.CurrentPlayer = Main.MainRef.network.LastTeamPlayer[Main.MainRef.network.ActiveTeam];
            while (!bl) {
                ++Main.MainRef.network.CurrentPlayer;
                if (Main.MainRef.network.CurrentPlayer >= Main.MainRef.CannonCount) {
                    Main.MainRef.network.CurrentPlayer = 0;
                }
                if (!Main.MainRef.cannon[Main.MainRef.network.CurrentPlayer].Active || Main.MainRef.network.PlayerTeam[Main.MainRef.network.CurrentPlayer] != Main.MainRef.network.ActiveTeam) continue;
                bl = true;
            }
        } else {
            if (Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber && Main.MainRef.HotSeatTime > 0) {
                Main.MainRef.timer.hide();
            }
            while (!bl) {
                ++Main.MainRef.network.CurrentPlayer;
                if (Main.MainRef.network.CurrentPlayer >= Main.MainRef.CannonCount) {
                    Main.MainRef.network.CurrentPlayer = 0;
                }
                if (!Main.MainRef.cannon[Main.MainRef.network.CurrentPlayer].Active) continue;
                bl = true;
            }
        }
        Main.MainRef.hud.setTargetNameActive(Main.MainRef.network.CurrentPlayer);
        if (Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber) {
            Main.MainRef.hud.showMyTurn();
        } else {
            Main.MainRef.hud.showOtherTurn();
        }
        if (Main.MainRef.camera.CurrentCameraView == 99) {
            Main.MainRef.camera.refreshSpectatorCamera();
        }
        Main.MainRef.hud.updateNextUp();
    }

    void updateClientWithBeginInfo(Packet packet) {
        int n = 0;
        Main.MainRef.Wt.outDebugString("GOT CANNON START INFO FOR CANNON # " + packet.Id);
        if (Main.MainRef.island != null && Main.MainRef.cannon[packet.Id] != null && !Main.MainRef.cannon[packet.Id].DataReceived) {
            Main.MainRef.cannon[packet.Id].Position.X = packet.X1;
            Main.MainRef.cannon[packet.Id].Position.Y = packet.Y1;
            Main.MainRef.cannon[packet.Id].Position.Z = packet.Z1;
            Main.MainRef.network.FirstPlayer = (int)packet.X2;
            Main.MainRef.cannon[packet.Id].Color = (int)packet.Z2;
            Main.MainRef.cannon[packet.Id].DataReceived = true;
            Main.MainRef.cannon[packet.Id].Name = packet.Name;
            Main.MainRef.cannon[packet.Id].IsBot = packet.conditional;
            Main.MainRef.cannon[packet.Id].BotType = packet.type;
            Main.MainRef.cannon[packet.Id].BotOwner = (int)packet.Y2;
            if (Main.MainRef.cannon[packet.Id].IsBot) {
                Main.MainRef.cannon[packet.Id].Synced = true;
            }
            Main.MainRef.network.PlayerNames[packet.Id] = packet.Name;
            Main.MainRef.island.WindDirection = packet.Var1;
            Main.MainRef.island.WindVelocity = packet.Var2;
            Main.MainRef.network.PlayerTeam[packet.Id] = (int)packet.Var3;
            Main.MainRef.island.windToVEC3D();
            boolean bl = true;
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n] == null || !Main.MainRef.cannon[n].DataReceived) {
                    bl = false;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n] == null || !Main.MainRef.chest[n].DataReceived) {
                    bl = false;
                }
                ++n;
            }
            if (bl) {
                this.StartupDataReceived = true;
                Main.MainRef.Wt.outDebugString("STARTUP DATA READY!");
            }
        }
        this.TimeSinceBeginInfo = 0.0f;
    }

    void updateGameStateActive(float f) {
        float f2 = f;
        float f3 = 40.0f;
        int n = (int)Math.ceil(f / f3);
        if (n > 5) {
            n = 5;
        }
        float f4 = f / (float)n;
        f4 /= 1000.0f;
        Main.MainRef.packetmanager.updateTime(f /= 1000.0f);
        float f5 = f2 / (float)n;
        f5 /= 1000.0f;
        int n2 = n - 1;
        while (n2 >= 0) {
            int n3 = 0;
            while (n3 < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n3].Active) {
                    Main.MainRef.cannon[n3].update(f4);
                } else if (!Main.MainRef.cannon[n3].Active && n3 == Main.MainRef.network.PlayerNumber && Main.MainRef.GameState == 15) {
                    Main.MainRef.cannon[n3].updateDeathWaitingTimer(f);
                }
                Main.MainRef.camera.updateCamera(f4, n2);
                ++n3;
            }
            --n2;
        }
        if (Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber && Main.MainRef.HotSeatTime > 0 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Active && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Respawning && Main.MainRef.GameState == 3) {
            Main.MainRef.timer.update((int)f2);
        }
        Main.MainRef.camera.updateObjects_Environment();
        Main.MainRef.ParticleList.update(f);
        Main.MainRef.ChunkList.update(f);
        Main.MainRef.hud.update(f);
        Main.MainRef.chat.update(f);
        Main.MainRef.island.update(f);
    }

    void timeSlice(float f) {
    }

    void masterInitialize() {
        this.GameStateTimeOut = 0;
        Main.MainRef.island = new Island(Main.MainRef.MapTracker.MapPaths[Main.MainRef.ActiveMap]);
        Main.MainRef.island.reInitWind();
        Main.MainRef.GameState = 2;
        Main.MainRef.network.nullTeams();
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (Main.MainRef.network.IsBot[n]) {
                Main.MainRef.cannon[n] = new Cannon(n, 0, true, Main.MainRef.network.PlayerNumber);
                Main.MainRef.cannon[n].Synced = true;
                Main.MainRef.cannon[n].BotType = Main.MainRef.network.BotType[n];
            } else {
                Main.MainRef.cannon[n] = new Cannon(n, 0, false, -1);
            }
            ++n;
        }
        n = 0;
        while (n < Main.MainRef.ChestCount) {
            Main.MainRef.chest[n] = new Chest(n);
            ++n;
        }
    }

    void updateGameStateRespawn(float f) {
        this.GameStateTimeOut = (int)((float)this.GameStateTimeOut + f);
        this.updateGameStateActive(f);
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].checkForNetworkUpdate(f / 1000.0f);
        if (this.GameStateTimeOut > 4000) {
            this.GameStateTimeOut = 0;
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].playerRespawn();
        }
    }

    void clientSetup(float f) {
        int n = 0;
        Main.MainRef.MenuManager.updateLoading(f);
        if (Main.MainRef.island.isLoaded() && Main.MainRef.MediaList.allLoaded()) {
            if (this.StartupDataReceived) {
                Main.MainRef.Wt.outDebugString("CLIENT RECEIVED STARTUP DATA");
                Main.MainRef.GameState = 6;
                n = 0;
                while (n < Main.MainRef.CannonCount) {
                    Main.MainRef.cannon[n].toGround();
                    Main.MainRef.cannon[n].attachFlags();
                    ++n;
                }
                Main.MainRef.camera.showMouse();
                Packet packet = new Packet();
                packet.Code = (short)10;
                packet.Id = (short)Main.MainRef.network.PlayerNumber;
                Main.MainRef.network.sendPacket(packet);
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Synced = true;
            } else {
                this.SendPingTimer += f;
                if (this.SendPingTimer > 1000.0f) {
                    this.SendPingTimer = 0.0f;
                    Packet packet = new Packet();
                    packet.Code = (short)31;
                    packet.Id = (short)Main.MainRef.network.PlayerNumber;
                    packet.type = 1;
                    Main.MainRef.network.sendPacket(packet);
                }
            }
        } else {
            this.SendPingTimer += f;
            if (this.SendPingTimer > 1000.0f) {
                this.SendPingTimer = 0.0f;
                Packet packet = new Packet();
                packet.Code = (short)31;
                packet.Id = (short)Main.MainRef.network.PlayerNumber;
                packet.type = 0;
                Main.MainRef.network.sendPacket(packet);
            }
        }
        this.TimeSinceBeginInfo += f;
        if (this.TimeSinceBeginInfo > 60000.0f) {
            Main.MainRef.Wt.outDebugString("DID NOT RECEIVE BEGIN INFO WITHIN 60 SECONDS");
            Main.MainRef.network.destroyPlayer();
            Main.MainRef.network.closeSession();
            this.destroyGame();
            return;
        }
        Main.MainRef.MenuManager.showGameConnectionStats();
    }

    void passBotsToNextPlayer() {
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n].IsBot && !Main.MainRef.cannon[Main.MainRef.cannon[n].BotOwner].Active) {
                int n2 = 0;
                while (n2 < Main.MainRef.CannonCount) {
                    if (Main.MainRef.cannon[n2].Active && !Main.MainRef.cannon[n2].IsBot) {
                        Main.MainRef.cannon[n].BotOwner = n2;
                    }
                    ++n2;
                }
            }
            ++n;
        }
    }

    void updateClientWithChestInfo(Packet packet) {
        int n = 0;
        Main.MainRef.Wt.outDebugString("GOT CHEST START INFO FOR CHEST # " + packet.Id);
        if (Main.MainRef.chest[packet.Id] != null && !Main.MainRef.chest[packet.Id].DataReceived) {
            Main.MainRef.chest[packet.Id].X = packet.X1;
            Main.MainRef.chest[packet.Id].Y = packet.Y1;
            Main.MainRef.chest[packet.Id].Z = packet.Z1;
            Main.MainRef.chest[packet.Id].DataReceived = true;
            Main.MainRef.chest[packet.Id].Contents = (int)packet.Var1;
            boolean bl = true;
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n] == null || !Main.MainRef.cannon[n].DataReceived) {
                    bl = false;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n] == null || !Main.MainRef.chest[n].DataReceived) {
                    bl = false;
                }
                ++n;
            }
            if (bl) {
                this.StartupDataReceived = true;
                Main.MainRef.Wt.outDebugString("STARTUP DATA READY!");
            }
        }
        this.TimeSinceBeginInfo = 0.0f;
    }
}

