/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;

public class HUD
implements Global {
    WTGroup HUDGroup;
    WTGroup MiniArrowGroup;
    WTGroup ArrowGroup;
    Scroll_Bar ChatBar;
    Button_3D OptionsMenu;
    Button_3D MinMax;
    Button_3D Up;
    Button_3D Down;
    Button_3D CameraMenu;
    Button_3D HelpMenu;
    Button_3DList ButtonList = new Button_3DList();
    Button_Static CoinIcon;
    Button_3DWeapon Weapon;
    PopUp ChatMenu;
    Button_Bar TitleBar;
    Message_3D Version;
    Message_3D SpectatorMessage;
    Message_3D SpectatorName;
    Message_3D SuccessMessage;
    Button_Bar WindBar;
    Message_3D Wind;
    Button_Bar CashBar;
    Message_3D CashMessage;
    Button_Bar RespawnBar;
    Message_3D RespawnsMessage;
    Message_3D RespawnMessage;
    WTGroup Reticle;
    WTGroup MiniMap;
    Message_3D[] NextUp = new Message_3D[8];
    WTModel PowerBarModel;
    WTGroup PowerBar;
    WTGroup PitchMarker1;
    WTGroup PitchMarker2;
    WTGroup PowerMarker1;
    Media_Object_Actor Arrow;
    WTGroup[] HUDBits = new WTGroup[4];
    WTGroup YourTurn;
    WTGroup Defense;
    Media_Object_Shader HUDTex;
    Media_Object_Shader PowerBarTex;
    Media_Object_Shader YourTurnTex;
    Media_Object_Shader DefenseTex;
    Media_Object_Shader ReticleTex;
    Media_Object_Shader MiniMapTex;
    Message_3D[] TargetNames = new Message_3D[8];
    Button_Bar[] TargetNamesBack = new Button_Bar[8];
    Button_Static[] TargetArrow = new Button_Static[8];
    Button_Static[] TargetFlag = new Button_Static[8];
    WTGroup TurnArrow;
    int MessageCount = 0;
    String[] MessageList = new String[20];
    int[] MessageListColor = new int[20];
    Message_3D[] GameMessages = new Message_3D[6];
    float[] MessagePosition = new float[6];
    boolean[] MessageInUse = new boolean[6];
    int[] Enabled = new int[12];
    boolean ButtonDown = false;
    boolean ModelsAttached = false;
    boolean BarVisible = false;
    boolean Visible = false;
    boolean ReticleVisible = false;
    boolean YourTurnVisible = false;
    boolean DefenseVisible = false;
    boolean PowerBarVisible = false;
    VEC3D TempVector = new VEC3D();
    int YourTurnAngle = 0;
    int CoinFrame = 0;
    float CoinTimer = 0.0f;
    boolean HoldChatUp = false;
    boolean HoldChatDown = false;

    void updateDragBarLocation() {
        if (this.ChatBar != null) {
            this.ChatBar.update(Main.MainRef.chat.ChatTopLine, Main.MainRef.chat.ChatLines);
        }
    }

    void hideBar() {
        if (this.BarVisible) {
            this.hideYourTurn();
            this.hideDefense();
            this.BarVisible = false;
            this.ButtonList.remove(this.CameraMenu);
            this.CameraMenu = null;
            this.ButtonList.remove(this.HelpMenu);
            this.HelpMenu = null;
            String[] stringArray = new String[10];
            stringArray[0] = "Controls";
            stringArray[1] = "How To Play";
            stringArray[2] = "Tutorial";
            this.HelpMenu = this.ButtonList.add(new Button_3DMenu(165, 14, 3, stringArray, "Help!", "HELP", 80));
            this.HUDGroup.removeObject((WTContainer)this.HUDBits[0]);
            this.HUDGroup.removeObject((WTContainer)this.HUDBits[1]);
            this.HUDGroup.removeObject((WTContainer)this.HUDBits[2]);
            this.HUDGroup.removeObject((WTContainer)this.HUDBits[3]);
            this.PowerMarker1.setPosition(0.0f, 0.0f, -1.0f);
            this.PitchMarker1.setPosition(0.0f, 0.0f, -1.0f);
            this.PitchMarker2.setPosition(0.0f, 0.0f, -1.0f);
            if (this.Weapon != null) {
                this.ButtonList.remove(this.Weapon);
                this.Weapon = null;
            }
            this.ButtonList.remove(this.CoinIcon);
            this.CoinIcon = null;
            this.RespawnsMessage.destroy();
            this.RespawnsMessage = null;
            this.CashBar.destroy();
            this.CashBar = null;
            this.RespawnBar.destroy();
            this.RespawnBar = null;
            if (this.CashMessage != null) {
                this.CashMessage.destroy();
                this.CashMessage = null;
            }
            if (this.RespawnMessage != null) {
                this.RespawnMessage.destroy();
                this.RespawnMessage = null;
            }
            this.updateNextUp();
            this.ButtonList.showAll();
        }
    }

    void hideDefense() {
        if (this.DefenseVisible) {
            Main.MainRef.hud.createWeaponButton();
            this.DefenseVisible = false;
            this.HUDGroup.removeObject((WTContainer)this.Defense);
        }
    }

    void updateGameMessage(float f) {
        int n = 0;
        do {
            if (!this.MessageInUse[n]) continue;
            int n2 = n;
            this.MessagePosition[n2] = this.MessagePosition[n2] + f * 20.0f;
            this.GameMessages[n].show(400.0f, 300.0f - this.MessagePosition[n]);
            this.GameMessages[n].setOpacity(255 - (int)(this.MessagePosition[n] / 100.0f * 255.0f));
            if (!(this.MessagePosition[n] > 100.0f)) continue;
            this.MessageInUse[n] = false;
            this.GameMessages[n].destroy();
            this.GameMessages[n] = null;
        } while (++n < 6);
    }

    void showSuccessMessage(String string) {
        if (this.SuccessMessage != null) {
            this.SuccessMessage.destroy();
        }
        this.SuccessMessage = null;
        this.SuccessMessage = new Message_3D(string, 1, 1.0f, 80, 1);
        this.SuccessMessage.show(400.0f, 324.0f);
    }

    void hide() {
        if (this.Visible) {
            this.hideReticle();
            this.hideDefense();
            this.hideBar();
            this.hideNextUp();
            this.OptionsMenu = null;
            this.HelpMenu = null;
            if (this.ChatBar != null) {
                this.ButtonList.remove(this.ChatBar);
                this.ChatBar = null;
            }
            if (this.RespawnsMessage != null) {
                this.RespawnsMessage.destroy();
                this.RespawnsMessage = null;
            }
            if (this.RespawnMessage != null) {
                this.RespawnMessage.destroy();
                this.RespawnMessage = null;
            }
            if (this.CashMessage != null) {
                this.CashMessage.destroy();
                this.CashMessage = null;
            }
            if (this.Version != null) {
                this.Version.destroy();
            }
            this.Version = null;
            this.ButtonList.destroy();
            this.CoinIcon = null;
            this.ChatMenu.destroy();
            this.ChatMenu = null;
            this.TitleBar.destroy();
            this.TitleBar = null;
            this.WindBar.destroy();
            this.WindBar = null;
            if (this.CashBar != null) {
                this.CashBar.destroy();
            }
            this.CashBar = null;
            if (this.RespawnBar != null) {
                this.RespawnBar.destroy();
            }
            this.RespawnBar = null;
            this.Weapon = null;
            if (this.SpectatorMessage != null) {
                this.SpectatorMessage.destroy();
                this.SpectatorMessage = null;
            }
            if (this.SpectatorName != null) {
                this.SpectatorName.destroy();
                this.SpectatorName = null;
            }
            if (this.Wind != null) {
                this.Wind.destroy();
                this.Wind = null;
            }
            this.Up = null;
            this.Down = null;
            this.MinMax = null;
            this.Visible = false;
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.HUDGroup);
            Main.MainRef.chat.hide();
        }
    }

    void setCoinFrame(int n) {
        if (!this.BarVisible) {
            return;
        }
        int n2 = (int)Math.ceil(n / 4);
        int n3 = n - (n2 - 1) * 4;
        this.CoinIcon.setUV(0.25f * (float)n2, 0.25f * (float)n3, 0.25f * (float)(n2 + 1), 0.25f * (float)(n3 + 1));
    }

    void update(float f) {
        if (!this.Visible) {
            return;
        }
        int n = 0;
        n = 0;
        while (n < Main.MainRef.CannonCount) {
            Main.MainRef.wt_stage.worldToScreen(this.TempVector, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Y, Main.MainRef.cannon[n].Position.Z);
            this.TempVector.multiply(2.0f);
            if (this.TempVector.Z > 0.0f && Main.MainRef.cannon[n].Active && Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber && (Main.MainRef.camera.CurrentCameraView != 99 || Main.MainRef.network.CurrentPlayer != n)) {
                this.TempVector.Y = !this.BarVisible ? 0.69768006f : 0.53352004f;
                this.TargetNames[n].showInUnits(this.TempVector.X + 0.013680001f, this.TempVector.Y);
                this.TargetNamesBack[n].show(this.TempVector.X, this.TempVector.Y);
                if (Main.MainRef.network.CurrentPlayer == n) {
                    this.TargetArrow[n].show(this.TempVector.X, this.TempVector.Y - 0.002736f * (23.0f + Main.MainRef.SinTable[1] * 5.0f + 5.0f));
                } else {
                    this.TargetArrow[n].show(this.TempVector.X, this.TempVector.Y - 0.062928006f);
                }
                if (Main.MainRef.network.TeamGame) {
                    this.TargetFlag[n].show(this.TempVector.X - 0.087552f, this.TempVector.Y - 0.062928006f);
                }
            } else {
                this.TargetNames[n].hide();
                this.TargetNamesBack[n].hide();
                this.TargetArrow[n].hide();
                if (Main.MainRef.network.TeamGame) {
                    this.TargetFlag[n].hide();
                }
            }
            ++n;
        }
        if (this.YourTurnVisible) {
            this.YourTurn.setBitmapSize(1.0f - Math.abs(Main.MainRef.SinTable[1] * 0.1f), 1.0f - Math.abs(Main.MainRef.SinTable[1] * 0.1f));
        }
        if (this.DefenseVisible) {
            this.Defense.setBitmapSize(1.0f - Math.abs(Main.MainRef.SinTable[1] * 0.1f), 1.0f - Math.abs(Main.MainRef.SinTable[1] * 0.1f));
        }
        if (!this.BarVisible) {
            this.TurnArrow.setPosition(-0.93024004f - 0.002736f * (23.0f + Main.MainRef.SinTable[1] * 5.0f + 5.0f), 0.5472f, 1.0f);
        } else {
            this.TurnArrow.setPosition(-0.76608f - 0.002736f * (23.0f + Main.MainRef.SinTable[1] * 5.0f + 5.0f), 0.38304f, 1.0f);
        }
        this.CoinTimer += f;
        if (this.CoinTimer > 0.05f) {
            this.CoinTimer = 0.0f;
            ++this.CoinFrame;
            if (this.CoinFrame > 15) {
                this.CoinFrame = 0;
            }
        }
        this.setCoinFrame(this.CoinFrame);
        this.ArrowGroup.setPosition(2.6f, -1.4f, 5.0f);
        this.ArrowGroup.setAbsoluteOrientation(0.0f, 1.0f, 0.0f, Main.MainRef.island.WindDirection);
        this.checkMessageQueue();
        this.updateGameMessage(f);
        if (this.HoldChatUp) {
            Main.MainRef.chat.ChatTopLine += -1;
            Main.MainRef.chat.updateChatText();
            this.updateDragBarLocation();
            this.HoldChatUp = true;
        }
        if (this.HoldChatDown) {
            ++Main.MainRef.chat.ChatTopLine;
            Main.MainRef.chat.updateChatText();
            this.updateDragBarLocation();
            this.HoldChatDown = true;
        }
    }

    void showWindSpeed() {
        if (this.Wind != null) {
            this.Wind.destroy();
            this.Wind = null;
        }
        this.Wind = new Message_3D((int)Main.MainRef.island.WindVelocity + " mph", 1, 1.0f, 80);
        this.Wind.show(719.0f, 540.0f);
    }

    void attachModels() {
        this.Reticle.attachSurfaceShader(this.ReticleTex.Shader, 0.175104f, 0.175104f, 32, 32);
        this.YourTurn.attachSurfaceShader(this.YourTurnTex.Shader, 0.350208f, 0.350208f, 64, 64);
        this.Defense.attachSurfaceShader(this.DefenseTex.Shader, 0.350208f, 0.350208f, 64, 64);
        this.PowerMarker1.attachSurfaceShader(this.HUDTex.Shader, 0.21888001f, 0.079344004f, 128, 128);
        this.PowerMarker1.setBitmapTextureRect(0.0f, 0.890625f, 0.30859375f, 0.99609375f);
        this.PowerMarker1.setBitmapOrientation(-90.0f);
        this.PitchMarker2.attachSurfaceShader(this.HUDTex.Shader, 0.21888001f, 0.079344004f, 128, 128);
        this.PitchMarker2.setBitmapTextureRect(0.0f, 0.890625f, 0.30859375f, 0.99609375f);
        this.PitchMarker1.attachSurfaceShader(this.HUDTex.Shader, 0.14227201f, 0.051984f, 128, 128);
        this.PitchMarker1.setBitmapTextureRect(0.0f, 0.8125f, 0.19921875f, 0.8828125f);
        this.TurnArrow.attachSurfaceShader(Main.MainRef.GlobalMedia.Arrow.Shader, 0.087552f, 0.087552f, 16, 16);
        this.TurnArrow.setBitmapTextureRect(1.0f, 0.0f, 0.0f, 1.0f);
        this.MiniArrowGroup.attachSurfaceShader(Main.MainRef.GlobalMedia.MapBits.Shader, 0.041040003f, 0.098496005f, 32, 32);
        this.MiniArrowGroup.setBitmapTextureRect(0.03125f, 0.28125f, 0.25f, 0.828125f);
        this.MiniMap.attachSurfaceShader(this.MiniMapTex.Shader, 0.350208f, 0.350208f, 64, 64);
        this.PowerBar.attach((WTObject)this.PowerBarModel);
        this.HUDBits[0].attachSurfaceShader(this.HUDTex.Shader, 0.700416f, 0.169632f, 128, 128);
        this.HUDBits[0].setBitmapTextureRect(0.0f, 0.0f, 0.99609375f, 0.23828125f);
        this.HUDBits[0].setOption(0, 71);
        this.HUDBits[1].attachSurfaceShader(this.HUDTex.Shader, 0.700416f, 0.169632f, 128, 128);
        this.HUDBits[1].setBitmapTextureRect(0.0f, 0.2421875f, 0.99609375f, 0.48046875f);
        this.HUDBits[1].setOption(0, 71);
        this.HUDBits[2].attachSurfaceShader(this.HUDTex.Shader, 0.098496005f, 0.169632f, 128, 128);
        this.HUDBits[2].setBitmapTextureRect(0.859375f, 0.484375f, 0.99609375f, 0.72265625f);
        this.HUDBits[2].setOption(0, 71);
        this.HUDBits[3].attachSurfaceShader(this.HUDTex.Shader, 0.58003205f, 0.147744f, 128, 128);
        this.HUDBits[3].setBitmapTextureRect(0.0f, 0.515625f, 0.83203125f, 0.72265625f);
        this.HUDBits[3].setBitmapOrientation(-90.0f);
        this.HUDBits[3].setOption(0, 71);
    }

    /*
     * Unable to fully structure code
     */
    void updateNextUp() {
        if (!this.Visible) {
            return;
        }
        var1_1 = 0;
        do {
            if (this.NextUp[var1_1] != null) {
                this.NextUp[var1_1].destroy();
            }
            this.NextUp[var1_1] = null;
        } while (++var1_1 < 6);
        var1_1 = Main.MainRef.network.CurrentPlayer;
        var2_2 = Main.MainRef.network.PlayerTeam[var1_1];
        var3_3 = 115;
        var4_4 = 160;
        if (!this.BarVisible) {
            var3_3 = 55;
            var4_4 = 100;
        }
        var5_5 = new int[4];
        var6_6 = 0;
        do {
            var5_5[var6_6] = Main.MainRef.network.LastTeamPlayer[var6_6];
        } while (++var6_6 < 4);
        var5_5[var2_2] = var1_1;
        var6_6 = 0;
        do {
            if (var6_6 == 0) {
                this.NextUp[var6_6] = new Message_3D(Main.MainRef.cannon[var1_1].Name, 0, 1.0f, 80);
                this.NextUp[var6_6].show(var3_3, var4_4 + var6_6 * 24);
            } else {
                this.NextUp[var6_6] = new Message_3D('`' + Main.MainRef.cannon[var1_1].Name + '`', 0, 0.75f, 80);
                this.NextUp[var6_6].show(var3_3 + 10, var4_4 + var6_6 * 24);
            }
            var7_7 = false;
            if (!Main.MainRef.network.TeamGame) ** GOTO lbl53
            var7_7 = false;
            while (!var7_7) {
                if (++var2_2 >= Main.MainRef.network.TeamCount) {
                    var2_2 = 0;
                }
                if (!Main.MainRef.network.ActiveTeams[var2_2] || Main.MainRef.network.countPlayersInTeam(var2_2) <= 0) continue;
                var7_7 = true;
            }
            var1_1 = var5_5[var2_2];
            var7_7 = false;
            while (!var7_7) {
                if (++var1_1 >= Main.MainRef.CannonCount) {
                    var1_1 = 0;
                }
                if (!Main.MainRef.cannon[var1_1].Active || Main.MainRef.network.PlayerTeam[var1_1] != var2_2) continue;
                var5_5[var2_2] = var1_1;
                var7_7 = true;
            }
            continue;
lbl-1000:
            // 1 sources

            {
                if (++var1_1 >= Main.MainRef.CannonCount) {
                    var1_1 = 0;
                }
                if (!Main.MainRef.cannon[var1_1].Active) continue;
                var7_7 = true;
lbl53:
                // 3 sources

                ** while (!var7_7)
            }
lbl54:
            // 2 sources

        } while (++var6_6 < 6);
    }

    void setTargetNameInActive(int n) {
        if (this.TargetNames[n] != null) {
            this.TargetNames[n].destroy();
        }
        this.TargetNames[n] = null;
        this.TargetNames[n] = new Message_3D(Main.MainRef.network.PlayerNames[n], 1, 1.0f, 25 + n * 3);
        if (this.TargetNamesBack[n] != null) {
            this.TargetNamesBack[n].destroy();
        }
        this.TargetNamesBack[n] = null;
        float f = this.TargetNames[n].getPixelWidth() + 20;
        this.TargetNamesBack[n] = new Button_Bar(Main.MainRef.GlobalMedia.InactiveBar, (int)f, 28, 0, 0, 35 + n * 3, null);
        if (this.TargetArrow[n] == null) {
            this.TargetArrow[n] = new Button_Static(Main.MainRef.GlobalMedia.Arrow, 0.0f, 0.0f, 32, 32, 0, 0, 25 + n);
        }
        if (Main.MainRef.network.TeamGame && this.TargetFlag[n] == null) {
            switch (Main.MainRef.network.PlayerTeam[n]) {
                case 0: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 0.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 1: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 24.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 2: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 0.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 3: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 24.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
            }
        }
    }

    void keyDownGameActive(int n) {
        if (Main.MainRef.chat.Chatting) {
            Main.MainRef.chat.keyDownChat(n);
            return;
        }
        if (Main.MainRef.PrimaryController == 0) {
            if (n == 39 && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveRight) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveRight = true;
            }
            if (n == 37 && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveLeft) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveLeft = true;
            }
            if (n == 38 && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveForward) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveForward = true;
            }
            if (n == 40 && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveBack) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveBack = true;
            }
        }
        if ((n == 81 || n == 113 && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].weapon.Active) && Main.MainRef.GameState == 3) {
            Packet packet = new Packet();
            packet.Code = (short)7;
            packet.Var1 = -1.0f;
            packet.Id = (short)Main.MainRef.network.PlayerNumber;
            packet.Var2 = (short)Main.MainRef.network.PlayerNumber;
            Main.MainRef.network.sendPacket(packet);
            Main.MainRef.packetmanager.parseIndividualPacket(packet);
            return;
        }
        if (n == 67 || n == 99) {
            Main.MainRef.chat.enableChat(0);
        }
        if (n == 82 && Main.MainRef.network.TeamGame) {
            Main.MainRef.chat.enableChat(1);
        }
        if (n == 86 || n == 118) {
            Main.MainRef.camera.cycleCamera(1.0E-4f);
        }
        if (n == 43 || n == 61) {
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].plusWeapon();
            this.Weapon.setSelection(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].getWeapon());
        }
        if (n == 45) {
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].minusWeapon();
            this.Weapon.setSelection(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].getWeapon());
        }
        if (n == 32) {
            Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].triggerFire();
        }
    }

    void loadMedia() {
        this.HUDTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/UI/image.png", "MEDIA/IMAGES/UI/alpha.png", false, false), true);
        this.HUDTex.noDepth();
        this.PowerBarTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/POWERBAR/image.png", "MEDIA/IMAGES/POWERBAR/alpha.png", false, false), true);
        this.PowerBarTex.noDepth();
        this.YourTurnTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/YOURTURN/image.png", "MEDIA/IMAGES/YOURTURN/alpha.png", false, false), true);
        this.YourTurnTex.noDepth();
        this.DefenseTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/DEFENSE/image.png", "MEDIA/IMAGES/DEFENSE/alpha.png", false, false), true);
        this.DefenseTex.noDepth();
        this.ReticleTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/RETICLE/image.png", "MEDIA/IMAGES/RETICLE/alpha.png", false, false), true);
        this.ReticleTex.noDepth();
        this.MiniMapTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/MAP/image.png", "MEDIA/IMAGES/MAP/alpha.png", false, false, false), true);
        this.MiniMapTex.noDepth();
    }

    void showBar() {
        if (!this.BarVisible) {
            this.BarVisible = true;
            this.createWeaponButton();
            this.HUDGroup.addObject((WTContainer)this.HUDBits[0]);
            this.HUDGroup.addObject((WTContainer)this.HUDBits[1]);
            this.HUDGroup.addObject((WTContainer)this.HUDBits[2]);
            this.HUDGroup.addObject((WTContainer)this.HUDBits[3]);
            this.HUDBits[0].setPosition(-0.730512f, 0.648432f, 1.0f);
            this.HUDBits[1].setPosition(-0.030096002f, 0.648432f, 1.0f);
            this.HUDBits[2].setPosition(0.36936003f, 0.648432f, 1.0f);
            this.HUDBits[3].setPosition(-0.98769605f, 0.2736f, 1.0f);
            this.CoinIcon = (Button_Static)this.ButtonList.add(new Button_Static(Main.MainRef.GlobalMedia.CoinIcon, 0.0f, 0.0f, 64, 64, 684, 135, 59));
            this.CoinFrame = 0;
            this.setCoinFrame(this.CoinFrame);
            this.RespawnsMessage = new Message_3D("Lives", 1, 1.0f, 80);
            this.RespawnsMessage.show(721.0f, 236.0f);
            this.CashBar = new Button_Bar(120, 655, 210, 50, null);
            this.CashBar.show();
            this.RespawnBar = new Button_Bar(120, 655, 260, 50, null);
            this.RespawnBar.show();
            this.ButtonList.showAll();
        }
    }

    void showDefense() {
        if (!this.DefenseVisible) {
            this.hideYourTurn();
            Main.MainRef.hud.createWeaponButton();
            this.DefenseVisible = true;
            this.HUDGroup.addObject((WTContainer)this.Defense);
            this.Defense.setPosition(0.0f, -0.49248f, 1.0f);
        }
    }

    void addMessage(String string, int n) {
        if (this.MessageCount < 18) {
            this.MessageList[this.MessageCount] = string;
            this.MessageListColor[this.MessageCount] = n;
            ++this.MessageCount;
        }
    }

    void hideReticle() {
        if (this.ReticleVisible) {
            this.ReticleVisible = false;
            this.HUDGroup.removeObject((WTContainer)this.Reticle);
        }
    }

    void createWeaponButton() {
        if (!this.BarVisible) {
            return;
        }
        if (this.Weapon != null) {
            this.ButtonList.remove(this.Weapon);
            this.Weapon = null;
        }
        int n = 0;
        do {
            this.Enabled[n] = Global.OFFENSIVE[n] ? 1 : 2;
            if (Global.WEAPONCOST[n] > Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Cash) {
                this.Enabled[n] = 0;
            }
            if ((Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`")) && n >= 7) {
                this.Enabled[n] = 0;
            }
            if (Main.MainRef.network.CurrentPlayer == Main.MainRef.network.PlayerNumber || !Global.OFFENSIVE[n]) continue;
            this.Enabled[n] = 0;
        } while (++n < 12);
        this.Weapon = (Button_3DWeapon)this.ButtonList.add(new Button_3DWeapon(677, 69, 12, Global.WEAPONNAME, Global.WEAPONCOST, this.Enabled, "WEAP", Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CurrentWeapon));
        this.ButtonList.showAll();
    }

    void updateCashDisplay(int n) {
        if (!this.Visible || !this.BarVisible) {
            return;
        }
        if (this.CashMessage != null) {
            this.CashMessage.destroy();
            this.CashMessage = null;
        }
        this.CashMessage = new Message_3D("" + n, 1, 1.0f, 80);
        this.CashMessage.show(721.0f, 210.0f);
    }

    void updateRespawnDisplay(int n) {
        if (!this.Visible || !this.BarVisible) {
            return;
        }
        n = Main.MainRef.MaxRespawns - n;
        if (this.RespawnMessage != null) {
            this.RespawnMessage.destroy();
            this.RespawnMessage = null;
        }
        this.RespawnMessage = new Message_3D("" + n, 1, 1.0f, 80);
        this.RespawnMessage.show(721.0f, 260.0f);
    }

    void reset() {
        if (this.SuccessMessage != null) {
            this.SuccessMessage.destroy();
        }
        this.SuccessMessage = null;
        int n = 0;
        this.MessageCount = 0;
        int n2 = 0;
        do {
            if (this.GameMessages[n2] != null) {
                this.GameMessages[n2].destroy();
            }
            this.GameMessages[n2] = null;
            this.MessageInUse[n2] = false;
        } while (++n2 < 6);
        n = 0;
        do {
            if (this.TargetNames[n] != null) {
                this.TargetNames[n].destroy();
                this.TargetNames[n] = null;
            }
            if (this.TargetNamesBack[n] != null) {
                this.TargetNamesBack[n].destroy();
            }
            this.TargetNamesBack[n] = null;
            if (this.TargetArrow[n] != null) {
                this.TargetArrow[n].destroy();
            }
            this.TargetArrow[n] = null;
            if (this.TargetFlag[n] != null) {
                this.TargetFlag[n].destroy();
            }
            this.TargetFlag[n] = null;
        } while (++n < 8);
        this.hide();
    }

    public HUD() {
        this.loadMedia();
        this.createModels();
        this.createGroups();
    }

    void showYourTurn() {
        if (!this.YourTurnVisible) {
            this.hideDefense();
            Main.MainRef.hud.createWeaponButton();
            this.YourTurnVisible = true;
            this.HUDGroup.addObject((WTContainer)this.YourTurn);
            this.YourTurn.setPosition(0.0f, -0.49248f, 1.0f);
        }
    }

    void keyDownSpectator(int n) {
        if (Main.MainRef.chat.Chatting) {
            Main.MainRef.chat.keyDownChat(n);
            return;
        }
        if (n == 81 || n == 113) {
            Main.MainRef.GameLoop.destroyGame();
            this.hide();
        }
        if (n == 67 || n == 99) {
            Main.MainRef.chat.enableChat(0);
        }
        if (n == 82 && Main.MainRef.network.TeamGame) {
            Main.MainRef.chat.enableChat(1);
        }
    }

    void showMyTurn() {
        this.addMessage("Your Turn!", 0);
        Main.MainRef.network.MyTurn = true;
        this.showYourTurn();
        if (Main.MainRef.HotSeatTime > 0) {
            Main.MainRef.timer.show();
            Main.MainRef.timer.setTime(Global.HOTSEATTIMES[Main.MainRef.HotSeatTime], 0);
        }
        if (Main.MainRef.network.TeamGame) {
            this.addMessage(Global.TEAMNAMES[Main.MainRef.network.ActiveTeam] + " Team", 0);
        }
        Main.MainRef.GlobalMedia.Sound_DrumRoll.play(false, 128);
    }

    void createGroups() {
        this.HUDGroup = Main.MainRef.Wt.createGroup();
        this.MiniArrowGroup = Main.MainRef.Wt.createGroup();
        this.MiniArrowGroup.setOption(0, 100);
        this.MiniMap = Main.MainRef.Wt.createGroup();
        this.MiniMap.setOption(0, 25);
        this.ArrowGroup = Main.MainRef.Wt.createGroup();
        this.TurnArrow = Main.MainRef.Wt.createGroup();
        this.TurnArrow.setOption(0, 89);
        this.Reticle = Main.MainRef.Wt.createGroup();
        this.Reticle.setOption(0, 75);
        this.YourTurn = Main.MainRef.Wt.createGroup();
        this.YourTurn.setOption(0, 76);
        this.Defense = Main.MainRef.Wt.createGroup();
        this.Defense.setOption(0, 76);
        this.PowerBar = Main.MainRef.Wt.createGroup();
        this.PowerBar.setOption(0, 72);
        this.PitchMarker1 = Main.MainRef.Wt.createGroup();
        this.PitchMarker1.setOption(0, 73);
        this.PitchMarker2 = Main.MainRef.Wt.createGroup();
        this.PitchMarker2.setOption(0, 74);
        this.PowerMarker1 = Main.MainRef.Wt.createGroup();
        this.PowerMarker1.setOption(0, 73);
        this.HUDBits[0] = Main.MainRef.Wt.createGroup();
        this.HUDBits[1] = Main.MainRef.Wt.createGroup();
        this.HUDBits[2] = Main.MainRef.Wt.createGroup();
        this.HUDBits[3] = Main.MainRef.Wt.createGroup();
    }

    void showOtherTurn() {
        if (!this.Visible) {
            return;
        }
        this.hideYourTurn();
        Main.MainRef.network.MyTurn = false;
        Main.MainRef.hud.addMessage(Main.MainRef.network.PlayerNames[Main.MainRef.network.CurrentPlayer] + "'s Turn!", 0);
        if (Main.MainRef.network.TeamGame) {
            this.addMessage(Global.TEAMNAMES[Main.MainRef.network.ActiveTeam] + " Team", 0);
        }
        Main.MainRef.GlobalMedia.Sound_DrumRoll.play(false, 128);
        Main.MainRef.hud.showDefense();
    }

    void hideYourTurn() {
        if (this.YourTurnVisible) {
            Main.MainRef.hud.createWeaponButton();
            this.YourTurnVisible = false;
            this.HUDGroup.removeObject((WTContainer)this.YourTurn);
        }
    }

    void keyUpGameActive(int n) {
        if (!(n != 70 && n != 102 || Main.MainRef.chat.Chatting)) {
            Main.MainRef.wt_stage.toggleFullscreen();
            return;
        }
        if (Main.MainRef.chat.Chatting) {
            Main.MainRef.chat.keyUpChat(n);
            return;
        }
        if (Main.MainRef.PrimaryController == 0) {
            if (n == 39 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveRight) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveRight = false;
            }
            if (n == 37 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveLeft) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveLeft = false;
            }
            if (n == 38 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveForward) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveForward = false;
            }
            if (n == 40 && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveBack) {
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveBack = false;
            }
        }
    }

    void showPowerBar() {
        if (!this.PowerBarVisible) {
            this.PowerBarVisible = true;
            this.HUDGroup.addObject((WTContainer)this.PowerBar);
        }
    }

    void hideNextUp() {
        int n = 0;
        do {
            if (this.NextUp[n] != null) {
                this.NextUp[n].destroy();
            }
            this.NextUp[n] = null;
        } while (++n < 6);
    }

    void swapChatSize() {
        Main.MainRef.ChatMinimized = !Main.MainRef.ChatMinimized;
        this.ButtonList.remove(this.Up);
        this.ButtonList.remove(this.Down);
        this.ButtonList.remove(this.MinMax);
        this.Up = null;
        this.Down = null;
        this.MinMax = null;
        Main.MainRef.chat.ChatTopLine = -999;
        this.ChatMenu.destroy();
        this.ChatMenu = null;
        if (Main.MainRef.ChatMinimized) {
            this.ChatMenu = new PopUp(5, 486, 295, 88, "Chat", false, true);
            this.Up = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 270, 519, "CHATUP", 51));
            this.Down = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 270, 546, "CHATDOWN", 51));
            this.MinMax = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 269, 489, "MINMAX", 51));
            if (this.ChatBar != null) {
                this.ButtonList.remove(this.ChatBar);
                this.ChatBar = null;
            }
        } else {
            this.ChatMenu = new PopUp(5, 320, 295, "Chat", false, true);
            this.Up = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 270, 351, "CHATUP", 51));
            this.Down = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 270, 546, "CHATDOWN", 51));
            this.MinMax = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 269, 323, "MINMAX", 51));
            if (this.ChatBar != null) {
                this.ButtonList.remove(this.ChatBar);
                this.ChatBar = null;
            }
            this.ChatBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(270, 378, 168, 14, 51));
        }
        Main.MainRef.chat.updateChatText();
        this.ButtonList.showAll();
    }

    void updateGameMouse(int n, int n2, int n3) {
        int n4;
        if (!this.Visible) {
            return;
        }
        if ((n3 & 1) == 1 && !Main.MainRef.ChatMinimized && n > 270 && n2 > 378 && n < 297 && n2 < 546) {
            n4 = Main.MainRef.chat.ChatLines - 14;
            if (n4 < 1) {
                n4 = 1;
            }
            float f = n2 - 378;
            f /= 140.0f;
            Main.MainRef.chat.ChatTopLine = (int)(f *= (float)n4);
            Main.MainRef.chat.updateChatText();
            this.updateDragBarLocation();
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
            this.ButtonDown = false;
            n3 = 0;
            this.HoldChatUp = false;
            this.HoldChatDown = false;
        }
        String string = this.ButtonList.update(n, n2, n3);
        if (string != null) {
            if (string.startsWith("OPTI")) {
                n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                Main.MainRef.changeSettings(n4);
                this.ButtonList.remove(this.OptionsMenu);
                this.OptionsMenu = null;
                String[] stringArray = new String[10];
                int n5 = Main.MainRef.getSettings(stringArray);
                this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n5, stringArray, "Options", "OPTI", 80));
                this.ButtonList.showAll();
                return;
            }
            if (string.equalsIgnoreCase("CHATUP")) {
                Main.MainRef.chat.ChatTopLine += -1;
                Main.MainRef.chat.updateChatText();
                this.updateDragBarLocation();
                this.HoldChatUp = true;
                this.HoldChatDown = false;
                this.ButtonDown = false;
            }
            if (string.equalsIgnoreCase("CHATDOWN")) {
                ++Main.MainRef.chat.ChatTopLine;
                Main.MainRef.chat.updateChatText();
                this.updateDragBarLocation();
                this.HoldChatDown = true;
                this.HoldChatUp = false;
                this.ButtonDown = false;
            }
            if (string.equalsIgnoreCase("MINMAX")) {
                this.swapChatSize();
                Main.MainRef.saveMinimizedSettings();
            }
            if (string.equalsIgnoreCase("FULLSCREEN")) {
                Main.MainRef.wt_stage.toggleFullscreen();
                return;
            }
            if (string.startsWith("QUIT")) {
                if (Main.MainRef.GameState == 3) {
                    Packet packet = new Packet();
                    packet.Code = (short)7;
                    packet.Var1 = -1.0f;
                    packet.Id = (short)Main.MainRef.network.PlayerNumber;
                    packet.Var2 = (short)Main.MainRef.network.PlayerNumber;
                    Main.MainRef.network.sendPacket(packet);
                    Main.MainRef.packetmanager.parseIndividualPacket(packet);
                    return;
                }
                Main.MainRef.GameLoop.destroyGame();
                this.hide();
                return;
            }
            if (string.startsWith("CAME")) {
                n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                switch (n4) {
                    case 0: {
                        Main.MainRef.camera.setCamera(0, 1.0E-4f);
                        return;
                    }
                    case 1: {
                        Main.MainRef.camera.setCamera(1, 1.0E-4f);
                        return;
                    }
                    case 2: {
                        Main.MainRef.camera.setCamera(2, 1.0E-4f);
                        return;
                    }
                    case 3: {
                        Main.MainRef.camera.setCamera(3, 1.0E-4f);
                        return;
                    }
                    case 4: {
                        Main.MainRef.camera.setCamera(4, 1.0E-4f);
                        return;
                    }
                }
                return;
            }
            if (string.startsWith("HELP")) {
                n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                if (n4 == 0) {
                    Main.MainRef.launchHelpPage("controls.htm");
                    return;
                }
                if (n4 == 1) {
                    Main.MainRef.launchHelpPage("gettingstarted.htm#playing");
                    return;
                }
                Main.MainRef.launchTutorial();
                return;
            }
            if (string.startsWith("WEAP")) {
                n4 = Float.valueOf(string.substring(4, string.length())).intValue();
                Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].setWeapon(n4);
                this.Weapon.setSelection(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].getWeapon());
            }
        }
    }

    void checkMessageQueue() {
        int n = 0;
        if (this.MessageCount > 0) {
            boolean bl = true;
            int n2 = 0;
            do {
                if (!this.MessageInUse[n2] || !(this.MessagePosition[n2] < 24.0f)) continue;
                bl = false;
            } while (++n2 < 6);
            if (bl) {
                n2 = 0;
                do {
                    if (this.MessageInUse[n2]) continue;
                    this.MessageInUse[n2] = true;
                    this.GameMessages[n2] = new Message_3D(this.MessageList[0], 1, 1.0f, 80, this.MessageListColor[0]);
                    this.MessagePosition[n2] = 0.0f;
                    this.GameMessages[n2].show(400.0f, 300.0f - this.MessagePosition[n2]);
                    n = 0;
                    while (n < this.MessageCount) {
                        this.MessageList[n] = this.MessageList[n + 1];
                        this.MessageListColor[n] = this.MessageListColor[n + 1];
                        ++n;
                    }
                    this.MessageCount += -1;
                    return;
                } while (++n2 < 6);
            }
        }
    }

    void createModels() {
        this.Arrow = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/ARROW"), true);
        this.PowerBarModel = Main.MainRef.Wt.createPatch(3, 2, 1.20384f, 0.065664f, 0.0f, 0.0f);
        this.PowerBarModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.PowerBarModel.setSurfaceShader(this.PowerBarTex.Shader);
        this.PowerBarModel.setPatchTileUV(0, 0, 0.1f, 0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 0.0f, 1.0f);
        this.PowerBarModel.setPatchTileUV(1, 0, 0.5f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.5f, 1.0f);
    }

    void showReticle() {
        if (!this.ReticleVisible) {
            this.ReticleVisible = true;
            this.HUDGroup.addObject((WTContainer)this.Reticle);
        }
    }

    void hidePowerBar() {
        if (this.PowerBarVisible) {
            this.PowerBarVisible = false;
            this.HUDGroup.removeObject((WTContainer)this.PowerBar);
        }
    }

    void show() {
        if (!this.Visible) {
            Main.MainRef.chat.ChatTopLine = -999;
            this.Visible = true;
            Main.MainRef.chat.ChatTopLine = -999;
            if (!this.ModelsAttached) {
                this.attachModels();
                this.assemble();
                this.ModelsAttached = true;
            }
            if (Main.MainRef.ChatMinimized) {
                this.ChatMenu = new PopUp(5, 486, 295, 88, "Chat", false, true);
                this.Up = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 270, 519, "CHATUP", 51));
                this.Down = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 270, 546, "CHATDOWN", 51));
                this.MinMax = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 269, 489, "MINMAX", 51));
            } else {
                this.ChatMenu = new PopUp(5, 320, 295, "Chat", false, true);
                this.ChatBar = (Scroll_Bar)this.ButtonList.add(new Scroll_Bar(270, 378, 168, 14, 51));
                this.Up = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 0.0f, 26, 27, 26.0f, 0.0f, 270, 351, "CHATUP", 51));
                this.Down = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 27.0f, 26, 27, 26.0f, 27.0f, 270, 546, "CHATDOWN", 51));
                this.MinMax = this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 269, 323, "MINMAX", 51));
            }
            Main.MainRef.chat.updateChatText();
            this.TitleBar = new Button_Bar(Main.MainRef.GlobalMedia.TopBar, 800, 30, 0, 15, 50, null);
            this.TitleBar.show();
            this.WindBar = new Button_Bar(120, 655, 540, 50, null);
            this.WindBar.show();
            String[] stringArray = new String[10];
            stringArray[0] = "Forfeit Game";
            this.ButtonList.add(new Button_3DMenu(10, 14, 1, stringArray, "Quit", "QUIT", 80));
            this.showBar();
            int n = Main.MainRef.getSettings(stringArray);
            this.OptionsMenu = this.ButtonList.add(new Button_3DMenu(70, 14, n, stringArray, "Options", "OPTI", 80));
            stringArray[0] = "Cannon Camera";
            stringArray[1] = "Shot Camera";
            stringArray[2] = "Medium Camera";
            stringArray[3] = "High Camera";
            stringArray[4] = "Barrel Camera";
            this.CameraMenu = this.ButtonList.add(new Button_3DMenu(165, 14, 5, stringArray, "Camera", "CAME", 80));
            this.ButtonList.add(new Button_3D(Main.MainRef.GlobalMedia.Controls, 0.0f, 70.0f, 28, 26, 28.0f, 70.0f, 767, 1, "FULLSCREEN", 51));
            stringArray[0] = "Controls";
            stringArray[1] = "How To Play";
            stringArray[2] = "Tutorial";
            this.HelpMenu = this.ButtonList.add(new Button_3DMenu(260, 14, 3, stringArray, "Help!", "HELP", 80));
            this.ButtonList.showAll();
            Main.MainRef.camera.Camera.addObject((WTContainer)this.HUDGroup);
            this.HUDGroup.setPosition(0.0f, 0.0f, 1.0f);
            Main.MainRef.chat.show();
            Main.MainRef.chat.updateChatText();
            this.updateNextUp();
        }
    }

    void resetTargetNames() {
        int n = 0;
        n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n].Active) {
                this.setTargetNameInActive(n);
            }
            ++n;
        }
    }

    void updateWeaponDisplay(int n) {
    }

    void keyUpSpectator(int n) {
        if (!(n != 70 && n != 102 || Main.MainRef.chat.Chatting)) {
            Main.MainRef.wt_stage.toggleFullscreen();
            return;
        }
        if (Main.MainRef.chat.Chatting) {
            Main.MainRef.chat.keyUpChat(n);
        }
    }

    void assemble() {
        this.Reticle.setPosition(0.0f, 0.0f, 1.0f);
        this.HUDGroup.addObject((WTContainer)this.MiniMap);
        this.MiniMap.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.MiniMap.setPosition(0.87552005f, -0.1368f, 1.0f);
        this.HUDGroup.addObject((WTContainer)this.MiniArrowGroup);
        this.MiniArrowGroup.setPosition(0.0f, 0.0f, -1.0f);
        this.TurnArrow.setBitmapOrientation(-90.0f);
        this.HUDGroup.addObject((WTContainer)this.TurnArrow);
        this.HUDGroup.addObject((WTContainer)this.PowerMarker1);
        this.PowerMarker1.setPosition(0.0f, 0.0f, -1.0f);
        this.HUDGroup.addObject((WTContainer)this.PitchMarker1);
        this.PitchMarker1.setPosition(0.0f, 0.0f, -1.0f);
        this.HUDGroup.addObject((WTContainer)this.PitchMarker2);
        this.PitchMarker2.setPosition(0.0f, 0.0f, -1.0f);
        this.PowerBar.setPosition(-0.84816f, 0.667584f, 1.0f);
        this.PowerBar.setOrientation(1.0f, 0.0f, 0.0f, -90.0f);
        this.HUDGroup.addObject((WTContainer)this.ArrowGroup);
        this.ArrowGroup.setPosition(2.6f, -1.4f, 5.0f);
        this.ArrowGroup.addObject((WTContainer)this.Arrow.Model);
        this.Arrow.Model.setPosition(0.0f, 0.0f, 0.0f);
        this.Arrow.Model.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
        this.Arrow.Model.setOption(0, 90);
    }

    void setTargetNameActive(int n) {
        if (this.TargetNames[n] != null) {
            this.TargetNames[n].destroy();
        }
        this.TargetNames[n] = null;
        this.TargetNames[n] = new Message_3D(Main.MainRef.network.PlayerNames[n], 1, 1.0f, 25 + n * 3);
        if (this.TargetNamesBack[n] != null) {
            this.TargetNamesBack[n].destroy();
        }
        this.TargetNamesBack[n] = null;
        float f = this.TargetNames[n].getPixelWidth() + 20;
        this.TargetNamesBack[n] = new Button_Bar(Main.MainRef.GlobalMedia.ActiveBar, (int)f, 28, 0, 0, 35 + n * 3, null);
        if (this.TargetArrow[n] == null) {
            this.TargetArrow[n] = new Button_Static(Main.MainRef.GlobalMedia.Arrow, 0.0f, 0.0f, 32, 32, 0, 0, 25 + n);
        }
        if (Main.MainRef.network.TeamGame && this.TargetFlag[n] == null) {
            switch (Main.MainRef.network.PlayerTeam[n]) {
                case 0: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 0.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 1: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 0.0f, 24.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 2: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 0.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
                case 3: {
                    this.TargetFlag[n] = new Button_Static(Main.MainRef.GlobalMedia.Flags, 24.0f, 24.0f, 24, 24, 0, 0, 25 + n);
                    return;
                }
            }
        }
    }
}

