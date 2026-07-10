/*
 * Decompiled with CFR 0.152.
 */
public class Packet_Manager
implements Global {
    float GameTime = 0.0f;
    boolean AcceptingPackets = false;

    void resetTime() {
        this.GameTime = 0.0f;
    }

    void closeForPackets() {
        this.AcceptingPackets = false;
    }

    void parseIndividualPacket(Packet packet) {
        Packet packet2;
        Packet packet3;
        if (packet == null) {
            return;
        }
        this.GameTime = 0.0f;
        if (!this.AcceptingPackets) {
            return;
        }
        if (Main.MainRef.network.GameOwner) {
            if (!Main.MainRef.network.GameCreationCompleted) {
                if (packet.Code == 29) {
                    Main.MainRef.network.receiveJoin(packet.Name);
                } else if (packet.Code == 12) {
                    Main.MainRef.network.ClientCheckedIn[packet.Id] = true;
                    Main.MainRef.network.PlayerTeam[packet.Id] = (int)packet.Var1;
                    Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
                } else if (packet.Code == 16) {
                    int n = Main.MainRef.network.findFullSlots() - 1;
                    if (packet.Id < n) {
                        if (Main.MainRef.network.TeamGame) {
                            int n2 = Main.MainRef.network.PlayerTeam[packet.Id];
                            Main.MainRef.network.TeamMemberCount[n2] = Main.MainRef.network.TeamMemberCount[n2] + -1;
                            Main.MainRef.network.PlayerTeam[packet.Id] = Main.MainRef.network.PlayerTeam[n];
                        }
                        Main.MainRef.network.PlayerNames[packet.Id] = Main.MainRef.network.PlayerNames[n];
                        Main.MainRef.network.IsBot[packet.Id] = Main.MainRef.network.IsBot[n];
                        Main.MainRef.network.BotType[packet.Id] = Main.MainRef.network.BotType[n];
                        Main.MainRef.network.ClientCheckedIn[packet.Id] = false;
                        Main.MainRef.network.ClientConfirmed[packet.Id] = Main.MainRef.network.ClientConfirmed[n];
                        Main.MainRef.network.AvailableSlots[n] = false;
                        Main.MainRef.network.ClientCheckedIn[n] = false;
                        Main.MainRef.network.ClientConfirmed[n] = 0;
                        Main.MainRef.network.IsBot[n] = false;
                        Main.MainRef.network.BotType[n] = 0;
                        if (Main.MainRef.network.IsBot[packet.Id]) {
                            Main.MainRef.network.ClientCheckedIn[packet.Id] = true;
                        } else {
                            packet3 = new Packet();
                            packet3.Code = (short)11;
                            packet3.Name = Main.MainRef.network.PlayerNames[packet.Id];
                            packet3.Id = packet.Id;
                            Main.MainRef.network.sendPacket(packet3);
                        }
                    } else {
                        if (Main.MainRef.network.TeamGame) {
                            int n3 = Main.MainRef.network.PlayerTeam[packet.Id];
                            Main.MainRef.network.TeamMemberCount[n3] = Main.MainRef.network.TeamMemberCount[n3] + -1;
                        }
                        Main.MainRef.network.AvailableSlots[packet.Id] = false;
                        Main.MainRef.network.ClientCheckedIn[packet.Id] = false;
                        Main.MainRef.network.ClientConfirmed[packet.Id] = 0;
                        Main.MainRef.network.PlayerNames[packet.Id] = "";
                        Main.MainRef.network.IsBot[packet.Id] = false;
                        Main.MainRef.network.BotType[packet.Id] = 0;
                    }
                    if (Main.MainRef.network.TeamGame && Main.MainRef.network.countActiveTeams() < 2) {
                        int n4 = 0;
                        do {
                            if (Main.MainRef.network.TeamMemberCount[n4] != 0) continue;
                            packet2 = new Packet();
                            packet2.Code = (short)28;
                            packet2.Id = (short)Main.MainRef.network.PlayerNumber;
                            packet2.Var1 = n4;
                            this.parseIndividualPacket(packet2);
                            break;
                        } while (++n4 < 4);
                    }
                    Main.MainRef.network.assignBotTeams();
                    Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
                } else if (packet.Code == 27) {
                    Main.MainRef.network.ClientConfirmed[packet.Id] = 1;
                    Main.MainRef.network.CannonColor[packet.Id] = (int)packet.Var2;
                    Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
                } else if (packet.Code == 28) {
                    int n = Main.MainRef.network.PlayerTeam[packet.Id];
                    if ((int)packet.Var1 != n) {
                        Main.MainRef.network.PlayerTeam[packet.Id] = (int)packet.Var1;
                        int n5 = n;
                        Main.MainRef.network.TeamMemberCount[n5] = Main.MainRef.network.TeamMemberCount[n5] + -1;
                        int n6 = Main.MainRef.network.PlayerTeam[packet.Id];
                        Main.MainRef.network.TeamMemberCount[n6] = Main.MainRef.network.TeamMemberCount[n6] + 1;
                        if (Main.MainRef.network.countActiveTeams() < 2) {
                            int n7 = n;
                            Main.MainRef.network.TeamMemberCount[n7] = Main.MainRef.network.TeamMemberCount[n7] + 1;
                            int n8 = Main.MainRef.network.PlayerTeam[packet.Id];
                            Main.MainRef.network.TeamMemberCount[n8] = Main.MainRef.network.TeamMemberCount[n8] + -1;
                            Main.MainRef.network.PlayerTeam[packet.Id] = n;
                        }
                        Main.MainRef.network.assignBotTeams();
                        Main.MainRef.MenuManager.LobbyScreen.showMasterJoinMembers();
                        Main.MainRef.Wt.exec();
                    }
                } else if (packet.Code == 32) {
                    Main.MainRef.network.CannonColor[packet.Id] = (int)packet.Var1;
                }
            } else {
                if (packet.Code == 29) {
                    Main.MainRef.network.receiveJoin(packet.Name);
                }
                if (packet.Code == 13) {
                    Packet packet4 = new Packet();
                    packet4.Code = 1;
                    packet4.Id = packet.Id;
                    packet4.Name = Main.MainRef.cannon[packet.Id].Name;
                    packet4.X1 = Main.MainRef.cannon[packet.Id].Position.X;
                    packet4.Y1 = Main.MainRef.cannon[packet.Id].Position.Y;
                    packet4.Z1 = Main.MainRef.cannon[packet.Id].Position.Z;
                    packet4.Var1 = Main.MainRef.island.WindDirection;
                    packet4.Var2 = Main.MainRef.island.WindVelocity;
                    Main.MainRef.network.sendPacket(packet4);
                }
            }
        } else if (!Main.MainRef.network.GameOwner) {
            if (!Main.MainRef.network.GameCreationCompleted) {
                if (packet.Code == 0 && Main.MainRef.network.PlayerNumber > -1) {
                    Main.MainRef.network.setUserState(1);
                    Main.MainRef.network.GameCreationCompleted = true;
                    Main.MainRef.ActiveMap = packet.Id;
                    Main.MainRef.CannonCount = (int)packet.Var1;
                    Main.MainRef.ChestCount = (int)packet.Var2;
                    Main.MainRef.HotSeatTime = (int)packet.Var3;
                    Main.MainRef.MaxRespawns = (int)packet.X1;
                    Main.MainRef.StartingCash = (int)packet.Y1;
                    Main.MainRef.TreasureRespawn = (int)packet.Z1;
                    Main.MainRef.MenuManager.activateGame(4);
                    int n = 0;
                    while (n < Main.MainRef.CannonCount) {
                        Main.MainRef.cannon[n] = new Cannon(n, 0, false, -1);
                        ++n;
                    }
                    n = 0;
                    while (n < Main.MainRef.ChestCount) {
                        Main.MainRef.chest[n] = new Chest(n);
                        ++n;
                    }
                } else if (packet.Code == 14) {
                    Main.MainRef.Wt.outDebugString("GOT CLIENT REJECTION");
                    if (Main.MainRef.GameState == 10 && Main.MainRef.MenuManager.ActiveMenu == 2 && (Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 4 || Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 6)) {
                        Main.MainRef.network.destroyPlayer();
                        Main.MainRef.network.closeSession();
                        Main.MainRef.MenuManager.LobbyScreen.hideAll();
                        Main.MainRef.MenuManager.LobbyScreen.showBaseMenu(false);
                    } else if (Main.MainRef.GameState != 10) {
                        Main.MainRef.network.destroyPlayer();
                        Main.MainRef.network.closeSession();
                        Main.MainRef.GameLoop.destroyGame();
                    }
                } else if (packet.Code == 15 && packet.Name.equalsIgnoreCase(Main.MainRef.network.UserName)) {
                    Main.MainRef.Wt.outDebugString("GOT CLIENT REJECTION");
                    if (Main.MainRef.MenuManager.ActiveMenu == 2 && (Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 4 || Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 6)) {
                        Main.MainRef.network.destroyPlayer();
                        Main.MainRef.network.closeSession();
                        Main.MainRef.MenuManager.LobbyScreen.hideAll();
                        Main.MainRef.MenuManager.LobbyScreen.showBaseMenu(false);
                    } else if (Main.MainRef.GameState != 10) {
                        Main.MainRef.network.destroyPlayer();
                        Main.MainRef.network.closeSession();
                        Main.MainRef.GameLoop.destroyGame();
                    }
                } else if (packet.Code == 17 && Main.MainRef.MenuManager.ActiveMenu == 2 && (Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 4 || Main.MainRef.MenuManager.LobbyScreen.InternalMenuState == 5)) {
                    Main.MainRef.MenuManager.LobbyScreen.showClientJoinMembers(packet.Name);
                } else if (packet.Code == 11 && packet.Name.equalsIgnoreCase(Main.MainRef.network.UserName)) {
                    Main.MainRef.network.PlayerNumber = packet.Id;
                    if (Main.MainRef.network.TeamGame) {
                        Main.MainRef.network.MyTeam = (int)packet.Var1;
                    }
                    Packet packet5 = new Packet();
                    packet5.Code = (short)12;
                    packet5.Id = packet.Id;
                    packet5.Var1 = Main.MainRef.network.MyTeam;
                    Main.MainRef.network.sendPacket(packet5);
                }
            } else if (packet.Code == 1 && Main.MainRef.network.GameCreationCompleted) {
                Main.MainRef.GameLoop.updateClientWithBeginInfo(packet);
            } else if (packet.Code == 18 && Main.MainRef.network.GameCreationCompleted) {
                Main.MainRef.GameLoop.updateClientWithChestInfo(packet);
            }
        }
        if (packet.Code == 22) {
            if ((int)packet.Var1 == -1) {
                Main.MainRef.chat.receiveChatText(packet.Name, -1);
            } else {
                Main.MainRef.chat.receiveChatText(packet.Name, (int)packet.Var2);
            }
        }
        if (Main.MainRef.GameState == 3 || Main.MainRef.GameState == 12 || Main.MainRef.GameState == 11 || Main.MainRef.GameState == 13) {
            if (!Main.MainRef.cannon[packet.Id].Disconnected) {
                if (packet.Code == 3 && packet.Id != Main.MainRef.network.PlayerNumber) {
                    Main.MainRef.cannon[packet.Id].remoteClientFire(packet);
                    return;
                }
                if (packet.Code == 4) {
                    if (packet.Id != Main.MainRef.network.PlayerNumber) {
                        Main.MainRef.GameLoop.switchPlayers();
                        return;
                    }
                } else {
                    if (packet.Code == 5 && packet.Id != Main.MainRef.network.PlayerNumber) {
                        if (packet.Var3 > 0.0f) {
                            Main.MainRef.island.crater(packet.Id, packet.X1, packet.Z1, packet.Var1, packet.Var2, packet.conditional, 1.0f, true, 130, 31, 115);
                            return;
                        }
                        Main.MainRef.island.crater(packet.Id, packet.X1, packet.Z1, packet.Var1, packet.Var2, packet.conditional, 1.0f);
                        return;
                    }
                    if (packet.Code == 6 && packet.Id != Main.MainRef.network.PlayerNumber) {
                        if (packet.Var3 > 0.0f) {
                            Main.MainRef.island.molehill(packet.X1, packet.Z1, packet.Var1, packet.Var2, true, 32, 40, 135);
                            return;
                        }
                        Main.MainRef.island.molehill(packet.X1, packet.Z1, packet.Var1, packet.Var2, false, 32, 40, 135);
                        return;
                    }
                    if (packet.Code == 7) {
                        if (!Main.MainRef.cannon[(int)packet.Var2].Disconnected) {
                            boolean bl = false;
                            if (packet.Var1 == -1.0f || packet.Var1 == -3.0f) {
                                bl = true;
                            }
                            if (!Main.MainRef.cannon[packet.Id].Disconnected && Main.MainRef.cannon[packet.Id].Active && !Main.MainRef.cannon[packet.Id].Respawning) {
                                Main.MainRef.cannon[packet.Id].kill(bl);
                            }
                            if (packet.Id == Main.MainRef.network.PlayerNumber && Main.MainRef.GameState == 3) {
                                if (packet.Var1 == -1.0f) {
                                    Main.MainRef.network.MyTurn = false;
                                    Main.MainRef.hud.hideYourTurn();
                                    Main.MainRef.hud.addMessage("You Forfeit The Game!", 0);
                                    Main.MainRef.GameState = 13;
                                    Main.MainRef.GameLoop.GameStateTimeOut = 5000;
                                    if (Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        packet3 = new Packet();
                                        packet3.Code = (short)4;
                                        packet3.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet3);
                                    }
                                } else if (packet.Var1 == -2.0f) {
                                    Main.MainRef.cannon[packet.Id].DoSwitch = false;
                                    Main.MainRef.hud.addMessage("You Were Drowned!", 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns <= Main.MainRef.MaxRespawns) {
                                        Main.MainRef.GameState = 12;
                                    } else {
                                        Main.MainRef.network.MyTurn = false;
                                        Main.MainRef.hud.hideYourTurn();
                                        Main.MainRef.hud.addMessage("You Lose!", 0);
                                        if (Main.MainRef.hud.SuccessMessage == null) {
                                            Main.MainRef.hud.showSuccessMessage("You Lose!");
                                        }
                                        Main.MainRef.GameState = 15;
                                        Main.MainRef.cannon[packet.Id].WaitingTimer = 5.0f;
                                        Main.MainRef.cannon[packet.Id].weapon.hide();
                                    }
                                    if (packet.conditional && Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        Main.MainRef.network.MyTurn = false;
                                        packet3 = new Packet();
                                        packet3.Code = (short)4;
                                        packet3.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet3);
                                        Main.MainRef.GameLoop.switchPlayers();
                                    }
                                    Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                } else if (packet.Var1 == -10.0f) {
                                    Main.MainRef.cannon[packet.Id].DoSwitch = false;
                                    Main.MainRef.hud.addMessage("You Were Detonated!", 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns <= Main.MainRef.MaxRespawns) {
                                        Main.MainRef.GameState = 12;
                                    } else {
                                        Main.MainRef.network.MyTurn = false;
                                        Main.MainRef.hud.hideYourTurn();
                                        Main.MainRef.hud.addMessage("You Lose!", 0);
                                        if (Main.MainRef.hud.SuccessMessage == null) {
                                            Main.MainRef.hud.showSuccessMessage("You Lose!");
                                        }
                                        Main.MainRef.GameState = 15;
                                        Main.MainRef.cannon[packet.Id].WaitingTimer = 5.0f;
                                        Main.MainRef.cannon[packet.Id].weapon.hide();
                                    }
                                    if (packet.conditional && Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        Main.MainRef.network.MyTurn = false;
                                        packet3 = new Packet();
                                        packet3.Code = (short)4;
                                        packet3.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet3);
                                        Main.MainRef.GameLoop.switchPlayers();
                                    }
                                    Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                } else if (packet.Var1 == -3.0f) {
                                    Main.MainRef.network.MyTurn = false;
                                    Main.MainRef.hud.hideYourTurn();
                                    Main.MainRef.hud.addMessage("Connection Lost!", 0);
                                    Main.MainRef.GameState = 15;
                                    Main.MainRef.cannon[packet.Id].WaitingTimer = 5.0f;
                                    Main.MainRef.cannon[packet.Id].weapon.hide();
                                } else if (packet.Var1 == 1.0f) {
                                    Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[(int)packet.Var2] + " " + Global.DEATHMESSAGE[(int)Math.floor(Math.random() * 8.0)] + " You!", 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns <= Main.MainRef.MaxRespawns) {
                                        Main.MainRef.GameState = 12;
                                    } else {
                                        Main.MainRef.network.MyTurn = false;
                                        Main.MainRef.hud.hideYourTurn();
                                        Main.MainRef.hud.addMessage("You Lose!", 0);
                                        if (Main.MainRef.hud.SuccessMessage == null) {
                                            Main.MainRef.hud.showSuccessMessage("You Lose!");
                                        }
                                        Main.MainRef.GameState = 15;
                                        Main.MainRef.cannon[packet.Id].WaitingTimer = 5.0f;
                                        Main.MainRef.cannon[packet.Id].weapon.hide();
                                    }
                                    if (Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        Main.MainRef.network.MyTurn = false;
                                        packet3 = new Packet();
                                        packet3.Code = (short)4;
                                        packet3.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet3);
                                        Main.MainRef.GameLoop.switchPlayers();
                                    }
                                    Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                }
                                if (Main.MainRef.network.TeamGame) {
                                    if (Main.MainRef.GameState == 11 && Main.MainRef.network.countTeamsWithActivePlayers() > 1) {
                                        Main.MainRef.camera.setCamera(99, 1.0E-5f);
                                    }
                                } else if (Main.MainRef.GameState == 11 && Main.MainRef.network.ActivePlayers > 1) {
                                    Main.MainRef.camera.setCamera(99, 1.0E-5f);
                                }
                            }
                            boolean bl2 = false;
                            if (Main.MainRef.cannon[packet.Id].IsBot && Main.MainRef.network.PlayerNumber == Main.MainRef.cannon[packet.Id].BotOwner) {
                                bl2 = true;
                            }
                            if (packet.Id != Main.MainRef.network.PlayerNumber) {
                                if (packet.Var1 == -1.0f) {
                                    Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Forfeits The Game!", 0);
                                } else if (packet.Var1 == -2.0f) {
                                    Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Was Drowned!", 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns > Main.MainRef.MaxRespawns) {
                                        Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Loses!", 0);
                                    }
                                    if (bl2 && Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        Main.MainRef.cannon[packet.Id].DoSwitch = false;
                                        packet2 = new Packet();
                                        packet2.Code = (short)4;
                                        packet2.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet2);
                                        Main.MainRef.GameLoop.switchPlayers();
                                    }
                                } else if (packet.Var1 == -3.0f) {
                                    if (Main.MainRef.network.ActivePlayers > 1 && !Main.MainRef.cannon[packet.Id].Disconnected) {
                                        if (!Main.MainRef.cannon[packet.Id].Active || Main.MainRef.cannon[packet.Id].Respawning) {
                                            Main.MainRef.cannon[packet.Id].hide();
                                            Main.MainRef.cannon[packet.Id].weapon.hide();
                                            Main.MainRef.cannon[packet.Id].Respawning = false;
                                        }
                                        Main.MainRef.cannon[packet.Id].Disconnected = true;
                                        Main.MainRef.cannon[packet.Id].Active = false;
                                        Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Was Disconnected!", 0);
                                        if (Main.MainRef.network.CurrentPlayer == packet.Id) {
                                            Main.MainRef.GameLoop.switchPlayers();
                                        }
                                        Main.MainRef.hud.updateNextUp();
                                    }
                                } else if (packet.Var1 == -10.0f) {
                                    Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Was Detonated!", 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns > Main.MainRef.MaxRespawns) {
                                        Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Loses!", 0);
                                    }
                                    if (bl2 && Main.MainRef.network.CurrentPlayer == packet.Id && Main.MainRef.network.ActivePlayers > 1) {
                                        Main.MainRef.cannon[packet.Id].DoSwitch = false;
                                        packet2 = new Packet();
                                        packet2.Code = (short)4;
                                        packet2.Id = (short)Main.MainRef.network.PlayerNumber;
                                        Main.MainRef.network.sendPacket(packet2);
                                        Main.MainRef.GameLoop.switchPlayers();
                                    }
                                    if (packet.Var2 == (float)Main.MainRef.network.PlayerNumber) {
                                        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash = (int)((float)Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash + packet.Var3);
                                        if (Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash < 0) {
                                            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash = 0;
                                        }
                                        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].updateCash();
                                        Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                                    }
                                } else if (packet.Var1 == 1.0f) {
                                    Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[(int)packet.Var2] + " " + Global.DEATHMESSAGE[(int)Math.floor(Main.MainRef.random.nextFloat() * 8.0f)] + " " + Main.MainRef.network.PlayerNames[packet.Id], 0);
                                    if (Main.MainRef.cannon[packet.Id].Respawns > Main.MainRef.MaxRespawns) {
                                        Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[packet.Id] + " Loses!", 0);
                                    }
                                    if (packet.Var2 == (float)Main.MainRef.network.PlayerNumber) {
                                        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash = (int)((float)Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash + packet.Var3);
                                        if (Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash < 0) {
                                            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash = 0;
                                        }
                                        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].updateCash();
                                        Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                                    }
                                }
                            }
                        }
                        if (Main.MainRef.network.TeamGame) {
                            if (Main.MainRef.network.countTeamsWithActivePlayers() < 2) {
                                Main.MainRef.network.MyTurn = false;
                                Main.MainRef.GameState = 13;
                                Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                Main.MainRef.camera.setCamera(6, 0.0f);
                                Main.MainRef.hud.showSuccessMessage(Global.TEAMNAMES[Main.MainRef.network.findWinningTeam()] + " Team Wins!");
                                return;
                            }
                        } else {
                            if (Main.MainRef.network.ActivePlayers < 2 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Respawns <= Main.MainRef.MaxRespawns && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Active) {
                                Main.MainRef.network.MyTurn = false;
                                Main.MainRef.GameState = 13;
                                Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                Main.MainRef.camera.setCamera(6, 0.0f);
                                Main.MainRef.hud.hideYourTurn();
                                Main.MainRef.hud.showSuccessMessage("You Win!");
                                return;
                            }
                            if (Main.MainRef.network.ActivePlayers < 2) {
                                Main.MainRef.hud.hideYourTurn();
                                Main.MainRef.GameState = 13;
                                Main.MainRef.GameLoop.GameStateTimeOut = 0;
                                Main.MainRef.camera.setCamera(6, 0.0f);
                                int n = 0;
                                while (n < Main.MainRef.CannonCount) {
                                    if (Main.MainRef.cannon[n].Active) {
                                        Main.MainRef.hud.showSuccessMessage(Main.MainRef.network.PlayerNames[n] + " Wins!");
                                    }
                                    ++n;
                                }
                                return;
                            }
                        }
                    } else {
                        if (packet.Code == 19 && Main.MainRef.network.PlayerNumber != packet.Id) {
                            if (!packet.conditional) {
                                Main.MainRef.chest[(int)packet.Var1].kill(false, 0, true);
                                return;
                            }
                            Main.MainRef.chest[(int)packet.Var1].clientRespawn(packet.X1, packet.Y1, packet.Z1, (int)packet.Var2);
                            return;
                        }
                        if (packet.Code == 20 && packet.Id != Main.MainRef.network.PlayerNumber) {
                            Main.MainRef.cannon[packet.Id].weapon.xCrater(packet.Id, packet.X1, packet.Z1, packet.X2, packet.Z2);
                            return;
                        }
                        if (packet.Code == 21) {
                            if (Main.MainRef.island.PropCount < 127) {
                                Main.MainRef.island.prop[Main.MainRef.island.PropCount] = new Prop(Main.MainRef.island.PropCount, packet.X1, packet.Z1, Main.MainRef.random.nextFloat() * 360.0f);
                                Main.MainRef.island.prop[Main.MainRef.island.PropCount].show();
                                Main.MainRef.island.prop[Main.MainRef.island.PropCount].drop();
                                int n = 0;
                                do {
                                    if (Main.MainRef.random.nextFloat() < 0.5f) {
                                        Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(2, false, 0.0f, Main.MainRef.island.prop[Main.MainRef.island.PropCount].X, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Y - 2.0f, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Z, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                                        continue;
                                    }
                                    Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(3, false, 0.0f, Main.MainRef.island.prop[Main.MainRef.island.PropCount].X, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Y - 2.0f, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Z, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                                } while (++n < 6);
                                Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(0, Main.MainRef.island.prop[Main.MainRef.island.PropCount].X, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Y - 4.0f, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Z, false, 0.5f));
                                Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(Main.MainRef.island.prop[Main.MainRef.island.PropCount].X, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Y, Main.MainRef.island.prop[Main.MainRef.island.PropCount].Z));
                                ++Main.MainRef.island.PropCount;
                                return;
                            }
                        } else if (packet.Code == 23) {
                            if (packet.Id != Main.MainRef.network.PlayerNumber) {
                                Main.MainRef.cannon[packet.Id].clientRespawn(packet.X1, packet.Y1, packet.Z1);
                                return;
                            }
                        } else if (packet.Code == 24) {
                            if (packet.Id != Main.MainRef.network.PlayerNumber) {
                                Main.MainRef.cannon[packet.Id].Cash = (int)packet.Var1;
                                if (Main.MainRef.cannon[packet.Id].Cash < 0) {
                                    Main.MainRef.cannon[packet.Id].Cash = 0;
                                    return;
                                }
                            }
                        } else if (packet.Code == 25) {
                            if (packet.Id != Main.MainRef.network.PlayerNumber) {
                                Main.MainRef.cannon[packet.Id].clientTeleport(packet.X1, packet.Y1, packet.Z1, packet.X2, packet.Y2, packet.Z2);
                                return;
                            }
                        } else {
                            if (packet.Code == 26 && Main.MainRef.network.PlayerNumber != packet.Id) {
                                Main.MainRef.island.prop[(int)packet.Var1].kill(false, packet.Id);
                                return;
                            }
                            if (packet.Code == 33 && Main.MainRef.network.PlayerNumber != packet.Id) {
                                Main.MainRef.cannon[packet.Id].weapon.hide();
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            if (packet.Code == 10 && Main.MainRef.cannon[packet.Id] != null) {
                Main.MainRef.cannon[packet.Id].Synced = true;
            }
            if (packet.Code == 31) {
                if (packet.Id == 0) {
                    Main.MainRef.GameLoop.TimeSinceBeginInfo = 0.0f;
                }
                if (Main.MainRef.cannon[packet.Id] != null) {
                    Main.MainRef.cannon[packet.Id].TimeSincePing = 0.0f;
                    Main.MainRef.network.ClientConnectionState[packet.Id] = packet.type;
                }
            }
            if (packet.Code == 34 && Main.MainRef.cannon[packet.Id] != null) {
                Main.MainRef.cannon[packet.Id].TimeSincePing = 200000.0f;
                Main.MainRef.cannon[packet.Id].PacketAliveTime = 200000.0f;
                Main.MainRef.network.ClientConnectionState[packet.Id] = 3;
            }
        }
    }

    void parseIndividualPacketSmall(PacketSmall packetSmall) {
        if (packetSmall == null) {
            return;
        }
        this.GameTime = 0.0f;
        if (this.AcceptingPackets && packetSmall.Id != Main.MainRef.network.PlayerNumber && Main.MainRef.cannon[packetSmall.Id] != null) {
            Main.MainRef.cannon[packetSmall.Id].remoteClientOrient(packetSmall);
        }
    }

    void openForPackets() {
        this.AcceptingPackets = true;
    }

    void updateTime(float f) {
        this.GameTime += f;
        if (this.GameTime > 20.0f) {
            if (Main.MainRef.network.countRealPlayers() > 1) {
                Main.MainRef.Wt.outDebugString("LOST MY CONNECTION " + this.GameTime);
                Main.MainRef.network.MyTurn = false;
                Main.MainRef.hud.hideYourTurn();
                Main.MainRef.hud.addMessage("Connection Lost!", 0);
                Main.MainRef.timer.hide();
                Main.MainRef.GameLoop.GameStateTimeOut = 5000;
                Main.MainRef.GameState = 13;
            }
            this.GameTime = 0.0f;
        }
    }
}

