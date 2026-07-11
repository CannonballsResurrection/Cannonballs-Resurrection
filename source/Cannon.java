/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTCollisionInfo
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTShadow
 *  wildtangent.webdriver.WTVector3D
 */
import wildtangent.webdriver.WTCollisionInfo;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTShadow;
import wildtangent.webdriver.WTVector3D;

public class Cannon
implements Global,
WTConstants {
    Packet gamepacket = new Packet();
    Packet gamepacket2 = new Packet();
    PacketSmall gamepacketsmall = new PacketSmall();
    boolean ReceiversAdded = false;
    private WTShadow Shadow;
    float NetworkUpdateTimer = 0.0f;
    float WaitingTimer = 0.0f;
    Media_Object_Shader CannonTex;
    WTVector3D TempVec;
    WTCollisionInfo Collision;
    boolean DoSwitch = false;
    String Name = "";
    VEC3D BarrelTargetVector = new VEC3D();
    VEC3D CameraTargetVector = new VEC3D();
    VEC3D CameraTargetFinal = new VEC3D();
    VEC3D BarrelTargetFinal = new VEC3D();
    VEC3D Temp = new VEC3D();
    VEC3D Temp2 = new VEC3D();
    VEC3D Position = new VEC3D(0.0f, 0.0f, 0.0f);
    float TiltAngle = 0.0f;
    float CurrentTiltTarget;
    float ActiveTilt = 0.0f;
    float LastTiltMarker = -1000.0f;
    float LastSpin = 0.0f;
    float LastTilt = 0.0f;
    float SpinAngle = 0.0f;
    float CurrentSpinTarget;
    float TiltAcceleration = 0.0f;
    float RemoteTiltTarget = 0.0f;
    float RemoteSpinAngle = 0.0f;
    float PowerLevel = 0.0f;
    float LastPowerLevel = -1000.0f;
    float MaxSpinSpeed = 100.0f;
    float MaxTiltAngle = 60.0f;
    float MinTiltAngle = 30.0f;
    int CurrentWeapon = 0;
    int Cash = 0;
    int Respawns = 0;
    int Owner = 0;
    boolean Active = false;
    boolean Dying = false;
    boolean Respawning = false;
    boolean Synced = false;
    boolean DataReceived = false;
    boolean TurnSoundActive = false;
    boolean TiltSoundActive = false;
    boolean ButtonDown = false;
    boolean ButtonDownRight = false;
    boolean BarActive = false;
    boolean Ascending = false;
    boolean ActiveLeft = false;
    boolean ActiveRight = false;
    boolean ActiveForward = false;
    boolean ActiveBack = false;
    boolean Visible = false;
    WTVector3D TempVector;
    Weapon weapon;
    WTGroup MapIcon;
    WTGroup Cannon;
    WTGroup CannonPivot;
    WTGroup BarrelTilt;
    Media_Object_Actor CannonBarrelActor;
    Media_Object_Actor CannonBaseActor;
    Media_Object_Actor CannonStandActor;
    float PacketAliveTime = 0.0f;
    boolean Disconnected = false;
    float TimeSincePing = 0.0f;
    int Color = 0;
    int AppliedColor = -1;
    boolean IsBot = false;
    int BotOwner = -1;
    int CurrentTarget = -1;
    int BotState = 0;
    float BotTimer = 0.0f;
    float BotTimeTarget = 0.0f;
    float TargetDistance = 0.0f;
    float SpinOffset = 0.0f;
    float TiltOffset = 0.0f;
    boolean HasBaseAdjustment = false;
    boolean LineOfSight = false;
    VEC3D LastBotPosition = new VEC3D();
    VEC3D LastTargetPosition = new VEC3D();
    int BotType = 1;
    boolean HasGreeted = false;

    boolean canAffordWeapon(int n) {
        return this.Cash >= Global.WEAPONCOST[n];
    }

    void spinLeft(float f) {
        this.CurrentSpinTarget -= 1.3f * f;
        if (this.CurrentSpinTarget < -1.0f) {
            this.CurrentSpinTarget = -1.0f;
        }
    }

    void playerRespawn() {
        this.Dying = false;
        Main.MainRef.GameState = 3;
        this.Respawning = false;
        this.place(false, true);
        this.toGround();
        this.show();
        this.gamepacket.Code = (short)23;
        this.gamepacket.Id = (short)this.Owner;
        this.gamepacket.X1 = this.Position.X;
        this.gamepacket.Y1 = this.Position.Y;
        this.gamepacket.Z1 = this.Position.Z;
        Main.MainRef.network.sendPacket(this.gamepacket);
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void checkForNetworkUpdate(float f) {
        this.NetworkUpdateTimer += f;
        if (this.NetworkUpdateTimer > 0.2f) {
            this.NetworkUpdateTimer = 0.0f;
            this.gamepacketsmall.Id = (short)this.Owner;
            this.gamepacketsmall.Var1 = (short)(this.SpinAngle * 100.0f);
            this.gamepacketsmall.Var2 = (short)(this.ActiveTilt * 100.0f);
            Main.MainRef.network.sendPacketUnGuaranteed(this.gamepacketsmall);
        }
    }

    void findNearestTarget() {
        this.CurrentTarget = -1;
        float f = 100000.0f;
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            float f2;
            if (Main.MainRef.network.TeamGame) {
                if (Main.MainRef.network.PlayerTeam[n] != Main.MainRef.network.PlayerTeam[this.Owner] && n != this.Owner && Main.MainRef.cannon[n].Active && (f2 = Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Z)) < f) {
                    f = f2;
                    this.CurrentTarget = n;
                }
            } else if (n != this.Owner && Main.MainRef.cannon[n].Active && (f2 = Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Z)) < f) {
                f = f2;
                this.CurrentTarget = n;
            }
            ++n;
        }
        if (this.CurrentTarget >= 0) {
            this.LastTargetPosition.setEqual(Main.MainRef.cannon[this.CurrentTarget].Position);
            this.LastBotPosition.setEqual(this.Position);
            if (this.HasGreeted && Main.MainRef.random.nextFloat() < 0.01f) {
                Main.MainRef.network.postBotMessage(this.Owner, 1, this.CurrentTarget);
            }
        }
        this.HasBaseAdjustment = false;
    }

    void ensureNearestTarget() {
        int n = this.CurrentTarget;
        this.CurrentTarget = -1;
        float f = 100000.0f;
        int n2 = 0;
        while (n2 < Main.MainRef.CannonCount) {
            float f2;
            if (Main.MainRef.network.TeamGame) {
                if (Main.MainRef.network.PlayerTeam[n2] != Main.MainRef.network.PlayerTeam[this.Owner] && n2 != this.Owner && Main.MainRef.cannon[n2].Active && (f2 = Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.cannon[n2].Position.X, Main.MainRef.cannon[n2].Position.Z)) < f) {
                    f = f2;
                    this.CurrentTarget = n2;
                }
            } else if (n2 != this.Owner && Main.MainRef.cannon[n2].Active && (f2 = Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.cannon[n2].Position.X, Main.MainRef.cannon[n2].Position.Z)) < f) {
                f = f2;
                this.CurrentTarget = n2;
            }
            ++n2;
        }
        if (this.CurrentTarget != n) {
            this.HasBaseAdjustment = false;
        }
    }

    void triggerFire() {
        if (this.WaitingTimer == 0.0f) {
            boolean bl = false;
            if ((Main.MainRef.OnlineDemo || Main.MainRef.network.UserName.startsWith("`")) && this.CurrentWeapon >= 7) {
                if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                    Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                    Main.MainRef.hud.addMessage("This Weapon Only Available", 0);
                    Main.MainRef.hud.addMessage("To Registered Players!", 0);
                }
                return;
            }
            if (Main.MainRef.network.MyTurn) {
                bl = true;
            } else {
                if (Global.OFFENSIVE[this.CurrentWeapon]) {
                    if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                        Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                        Main.MainRef.hud.addMessage("Only Defensive Items", 0);
                        Main.MainRef.hud.addMessage("Can Be Used On Your Off Turn!", 0);
                    }
                    return;
                }
                bl = true;
            }
            if (this.CurrentWeapon == 3) {
                this.weapon.Collider.setPosition(this.Position.X, this.Position.Y + 400.0f, this.Position.Z);
                this.Collision = this.weapon.Collider.checkCollision(this.Position.X, this.Position.Y - 12.0f, this.Position.Z, false, 8);
                if (this.Collision != null) {
                    if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                        Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                        Main.MainRef.hud.addMessage("Can't Use Tower On Object!", 0);
                    }
                    return;
                }
            }
            if (bl) {
                this.ButtonDown = false;
                if (!this.BarActive) {
                    if (!this.weapon.Active) {
                        this.activatePowerBar();
                        return;
                    }
                } else {
                    this.fire();
                    this.deactivatePowerBar();
                }
            }
        }
    }

    void hide() {
        if (this.Visible) {
            this.Visible = false;
            Main.MainRef.wt_stage.CollisionGroup.removeObject((WTContainer)this.Cannon);
            if (Main.MainRef.CannonShadowsEnabled) {
                this.Shadow.removeCaster((WTGroup)this.CannonBarrelActor.Model);
                this.Shadow.removeCaster((WTGroup)this.CannonBaseActor.Model);
                this.Shadow.removeCaster((WTGroup)this.CannonStandActor.Model);
            }
        }
    }

    void remoteClientFire(Packet packet) {
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        this.SpinAngle = packet.Var1;
        this.TiltAngle = this.ActiveTilt = packet.Var2;
        this.PowerLevel = packet.Var3;
        this.CurrentWeapon = (int)packet.X1;
        f = packet.X2;
        f2 = packet.Y2;
        f3 = packet.Z2;
        this.CannonPivot.setOrientation(0.0f, 1.0f, 0.0f, this.SpinAngle);
        this.BarrelTilt.setOrientation(1.0f, 0.0f, 0.0f, this.ActiveTilt);
        this.CannonBarrelActor.Model.playMotion("fire");
        if (!this.weapon.Active) {
            this.weapon.fire(this.Owner, this.Position.X + f * 5.0f, this.Position.Y + f2 * 5.0f, this.Position.Z + f3 * 5.0f, f, f2, f3, (this.PowerLevel + 0.5f) * 100.0f, this.CurrentWeapon, false);
            int n = 0;
            do {
                float f4 = 2.0f + Main.MainRef.random.nextFloat() * 8.0f;
                Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + f * 5.0f, this.Position.Y + f2 * 5.0f, this.Position.Z + f3 * 5.0f, f * f4 + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, f2 * f4 + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, f3 * f4 + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, 0.2f + Main.MainRef.random.nextFloat()));
            } while (++n < 20);
        }
    }

    void updateWaitingTimer(float f) {
        if (this.WaitingTimer > 0.0f) {
            this.WaitingTimer -= f;
            if (this.WaitingTimer < 0.0f) {
                this.WaitingTimer = 0.0f;
                if ((this.Owner == Main.MainRef.network.PlayerNumber || this.IsBot && this.BotOwner == Main.MainRef.network.PlayerNumber) && this.DoSwitch) {
                    this.gamepacket.Code = (short)4;
                    this.gamepacket.Id = (short)this.Owner;
                    Main.MainRef.network.sendPacket(this.gamepacket);
                    Main.MainRef.GameLoop.switchPlayers();
                }
            }
        }
    }

    void updateRespawns() {
        if (this.Owner == Main.MainRef.network.PlayerNumber) {
            Main.MainRef.hud.updateRespawnDisplay(this.Respawns);
        }
    }

    void place(boolean bl, boolean bl2) {
        boolean bl3 = false;
        int n = 0;
        int n2 = 0;
        float f = this.Position.X;
        float f2 = this.Position.Z;
        while (!bl3) {
            this.Position.X = Main.MainRef.random.nextFloat() * (float)(Main.MainRef.island.Width - 3) * Main.MainRef.island.VertexScale + 1.0f;
            this.Position.Z = Main.MainRef.random.nextFloat() * (float)(Main.MainRef.island.Height - 3) * Main.MainRef.island.VertexScale + 1.0f;
            boolean bl4 = false;
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n].Active && n != this.Owner && Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Z) < 100.0f) {
                    bl4 = true;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.island.PropCount) {
                if ((Main.MainRef.island.prop[n].Visible || bl) && Main.MainRef.island.prop[n].Destructible) {
                    if (Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.island.prop[n].X, Main.MainRef.island.prop[n].Z) < 20.0f) {
                        bl4 = true;
                    }
                } else if ((Main.MainRef.island.prop[n].Visible || bl) && !Main.MainRef.island.prop[n].Destructible && Main.MainRef.island.prop[n].WTCollideable && Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.island.prop[n].X, Main.MainRef.island.prop[n].Z) < 10.0f + Main.MainRef.island.prop[n].Radius) {
                    bl4 = true;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if ((Main.MainRef.chest[n].Visible || bl) && Library_Math.distance(this.Position.X, this.Position.Z, Main.MainRef.chest[n].X, Main.MainRef.chest[n].Z) < 10.0f) {
                    bl4 = true;
                }
                ++n;
            }
            if (bl2 && Library_Math.distance(this.Position.X, this.Position.Z, f, f2) < 100.0f) {
                bl4 = true;
            }
            if (bl4) continue;
            this.Collision = null;
            this.Cannon.setPosition(this.Position.X, 600.0f, this.Position.Z);
            this.Collision = this.Cannon.checkCollision(this.Position.X, -4.0f, this.Position.Z, false, 8);
            if (Main.MainRef.island.getTerrainHeight(this.Position.X, this.Position.Z) > 2.0f) {
                bl3 = true;
                continue;
            }
            if (this.Collision != null) {
                bl3 = true;
                continue;
            }
            if (!(Main.MainRef.island.getTerrainHeight(this.Position.X, this.Position.Z) <= 2.0f) || ++n2 <= 200) continue;
            Main.MainRef.island.molehillAbsolute(this.Position.X, this.Position.Z, 10.0f, 30.0f, false, 0, 0, 0);
            this.gamepacket.Id = (short)this.Owner;
            this.gamepacket.Code = (short)6;
            this.gamepacket.X1 = this.Position.X;
            this.gamepacket.Z1 = this.Position.Z;
            this.gamepacket.Var1 = 10.0f;
            this.gamepacket.Var2 = 30.0f;
            this.gamepacket.Var3 = 0.0f;
            Main.MainRef.network.sendPacket(this.gamepacket);
            bl3 = true;
        }
    }

    void update(float f) {
        if (!this.Respawning) {
            if (Main.MainRef.network.PlayerNumber != this.Owner && Main.MainRef.network.PlayerNumber != this.BotOwner) {
                if (Main.MainRef.GameState != 13) {
                    this.checkPacketLife(f);
                }
                this.updateRemoteClient(f);
            } else if (this.IsBot && this.BotOwner == Main.MainRef.network.PlayerNumber) {
                this.updateLocalClientBot(f);
            } else {
                this.updateLocalClient(f);
            }
        } else if (this.IsBot && this.BotOwner == Main.MainRef.network.PlayerNumber) {
            this.AIThink(f);
        }
        if (this.weapon.Active) {
            this.weapon.updateProjectile(f);
        }
        if (Main.MainRef.network.PlayerNumber != this.Owner && Main.MainRef.network.PlayerNumber != this.BotOwner && Main.MainRef.GameState != 13) {
            this.checkPacketLife(f);
        }
    }

    void updateLocalClient(float f) {
        float f2 = 0.0f;
        this.LastSpin = this.SpinAngle;
        this.LastTilt = this.ActiveTilt;
        if (this.BarActive) {
            this.updatePowerBar(f);
        } else {
            this.SpinAngle += this.CurrentSpinTarget * this.MaxSpinSpeed * f;
            while (this.SpinAngle > 180.0f) {
                this.SpinAngle -= 360.0f;
            }
            while (this.SpinAngle < -180.0f) {
                this.SpinAngle += 360.0f;
            }
            this.TiltAngle = this.CurrentTiltTarget > 0.0f ? this.MinTiltAngle * this.CurrentTiltTarget : this.MaxTiltAngle * this.CurrentTiltTarget;
            f2 = (this.TiltAngle - this.ActiveTilt) / 5.0f;
            this.ActiveTilt += f2;
            Main.MainRef.hud.MiniArrowGroup.setBitmapOrientation(this.SpinAngle);
            this.Temp.setEqual(this.CameraTargetVector);
            this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
            this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
            this.Temp.add(this.Position);
            this.CameraTargetFinal.setEqual(this.Temp);
            this.Temp.setEqual(this.BarrelTargetVector);
            this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
            this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
            this.Temp.add(this.Position);
            this.BarrelTargetFinal.setEqual(this.Temp);
            this.CannonPivot.setOrientation(0.0f, 1.0f, 0.0f, this.SpinAngle);
            this.BarrelTilt.setOrientation(1.0f, 0.0f, 0.0f, this.ActiveTilt);
            this.updateTiltMarker();
            this.updateLastPowerMarker();
            this.checkForNetworkUpdate(f);
        }
        if (Main.MainRef.PrimaryController == 0) {
            if (this.ActiveForward) {
                this.tiltUp(f);
            }
            if (this.ActiveBack) {
                this.tiltDown(f);
            }
            if (this.ActiveLeft) {
                this.spinLeft(f);
            }
            if (this.ActiveRight) {
                this.spinRight(f);
            }
            this.dampAngles(f);
        }
        this.updateMovementSound();
        this.updateWaitingTimer(f);
    }

    void updateMovementSound() {
        if (Math.abs(this.SpinAngle - this.LastSpin) < 1.0f) {
            if (this.TurnSoundActive) {
                Main.MainRef.GlobalMedia.Sound_TurnStop.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
                this.TurnSoundActive = false;
                Main.MainRef.GlobalMedia.Sound_TurnLoop.stop();
            }
        } else if (!this.TurnSoundActive) {
            Main.MainRef.GlobalMedia.Sound_TurnLoop.playDepth(true, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
            this.TurnSoundActive = true;
        } else {
            Main.MainRef.GlobalMedia.Sound_TurnLoop.setDepth(Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        }
        if (this.ActiveTilt >= this.LastTilt - 0.05f && this.ActiveTilt <= this.LastTilt + 0.05f) {
            if (this.TiltSoundActive) {
                this.TiltSoundActive = false;
                Main.MainRef.GlobalMedia.Sound_Tilt.stop();
                return;
            }
        } else if (this.ActiveTilt != this.LastTilt) {
            if (!this.TiltSoundActive) {
                Main.MainRef.GlobalMedia.Sound_Tilt.playDepth(true, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
                this.TiltSoundActive = true;
                return;
            }
            Main.MainRef.GlobalMedia.Sound_Tilt.setDepth(Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        }
    }

    void plusWeapon() {
        if (!this.BarActive) {
            ++this.CurrentWeapon;
            if (this.CurrentWeapon == 12) {
                this.CurrentWeapon = 0;
            }
            this.updateWeapon();
            Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
        }
    }

    int getWeapon() {
        return this.CurrentWeapon;
    }

    void setWeapon(int n) {
        if (!this.BarActive) {
            this.CurrentWeapon = n;
            this.updateWeapon();
        }
    }

    void updateLocalClientBot(float f) {
        this.AIThink(f);
        float f2 = 0.0f;
        float f3 = this.RemoteSpinAngle - this.SpinAngle;
        while (f3 > 180.0f) {
            f3 -= 360.0f;
        }
        while (f3 < -180.0f) {
            f3 += 360.0f;
        }
        if (f3 > 0.0f) {
            f2 = this.MaxSpinSpeed;
        } else if (f3 < 0.0f) {
            f2 = -this.MaxSpinSpeed;
        }
        f2 *= f;
        if (Math.abs(f2) > Math.abs(f3)) {
            f2 = f3;
        }
        this.SpinAngle += f2;
        f3 = this.RemoteTiltTarget - this.ActiveTilt;
        while (f3 > 180.0f) {
            f3 -= 360.0f;
        }
        while (f3 < -180.0f) {
            f3 += 360.0f;
        }
        f2 = 0.0f;
        if (f3 > 0.0f) {
            f2 = this.MaxSpinSpeed;
        } else if (f3 < 0.0f) {
            f2 = -this.MaxSpinSpeed;
        }
        f2 *= f;
        if (Math.abs(f2) > Math.abs(f3)) {
            f2 = f3;
        }
        this.ActiveTilt += f2;
        while (this.SpinAngle > 180.0f) {
            this.SpinAngle -= 360.0f;
        }
        while (this.SpinAngle < -180.0f) {
            this.SpinAngle += 360.0f;
        }
        this.CannonPivot.setOrientation(0.0f, 1.0f, 0.0f, this.SpinAngle);
        this.BarrelTilt.setOrientation(1.0f, 0.0f, 0.0f, this.ActiveTilt);
        this.Temp.setEqual(this.CameraTargetVector);
        this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
        this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
        this.Temp.add(this.Position);
        this.CameraTargetFinal.setEqual(this.Temp);
        this.Temp.setEqual(this.BarrelTargetVector);
        this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
        this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
        this.Temp.add(this.Position);
        this.BarrelTargetFinal.setEqual(this.Temp);
        this.checkForNetworkUpdate(f);
        this.updateWaitingTimer(f);
    }

    void updatePowerBar(float f) {
        if (this.Ascending) {
            float f2 = f / 2.0f;
            if (f2 > 0.01f) {
                f2 = 0.01f;
            }
            this.PowerLevel += f2;
        } else {
            float f3 = f / 2.0f;
            if (f3 > 0.01f) {
                f3 = 0.01f;
            }
            this.PowerLevel -= f3;
        }
        if (this.PowerLevel <= 0.0f) {
            this.PowerLevel = 0.0f;
            this.deactivatePowerBar();
            this.fire();
        }
        if (this.PowerLevel > 1.0f) {
            this.PowerLevel = 1.0f;
            this.Ascending = false;
        }
        if (this.PowerLevel * 440.0f >= 12.0f) {
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 0, this.PowerLevel * 440.0f * 0.002736f, 0.0f, 0.0f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 1, this.PowerLevel * 440.0f * 0.002736f, 0.0f, -0.065664f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 0, (this.PowerLevel * 440.0f - 12.0f) * 0.002736f, 0.0f, 0.0f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 1, (this.PowerLevel * 440.0f - 12.0f) * 0.002736f, 0.0f, -0.065664f);
            return;
        }
        Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 0, this.PowerLevel * 440.0f * 0.002736f, 0.0f, 0.0f);
        Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 1, this.PowerLevel * 440.0f * 0.002736f, 0.0f, -0.065664f);
        Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 0, 0.0f, 0.0f, 0.0f);
        Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 1, 0.0f, 0.0f, -0.065664f);
    }

    void activatePowerBar() {
        if (this.Cash < Global.WEAPONCOST[this.CurrentWeapon]) {
            if (!Main.MainRef.GlobalMedia.Sound_TimeUp.getIsPlaying()) {
                Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                Main.MainRef.hud.addMessage("Not Enough Gold For That Weapon!", 0);
            }
            return;
        }
        if (Global.PROJECTILE[this.CurrentWeapon]) {
            this.PowerLevel = 0.0f;
            this.LastTiltMarker = this.TiltAngle;
            this.Ascending = true;
            this.BarActive = true;
            Main.MainRef.hud.showPowerBar();
            if (this.PowerLevel * 440.0f >= 12.0f) {
                Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 0, this.PowerLevel * 440.0f * 0.002736f, 0.0f, 0.0f);
                Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 1, this.PowerLevel * 440.0f * 0.002736f, 0.0f, -0.065664f);
                Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 0, (this.PowerLevel * 440.0f - 12.0f) * 0.002736f, 0.0f, 0.0f);
                Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 1, (this.PowerLevel * 440.0f - 12.0f) * 0.002736f, 0.0f, -0.065664f);
                return;
            }
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 0, this.PowerLevel * 440.0f * 0.002736f, 0.0f, 0.0f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(2, 1, this.PowerLevel * 440.0f * 0.002736f, 0.0f, -0.065664f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 0, 0.0f, 0.0f, 0.0f);
            Main.MainRef.hud.PowerBarModel.setPatchPtPos(1, 1, 0.0f, 0.0f, -0.065664f);
            return;
        }
        this.fire();
    }

    void dampAngles(float f) {
        this.CurrentTiltTarget += this.TiltAcceleration * f;
        if (this.CurrentTiltTarget > 1.0f) {
            this.CurrentTiltTarget = 1.0f;
            this.TiltAcceleration = 0.0f;
        }
        if (this.CurrentTiltTarget < -1.0f) {
            this.TiltAcceleration = 0.0f;
            this.CurrentTiltTarget = -1.0f;
        }
        if (!this.ActiveLeft && !this.ActiveRight) {
            this.CurrentSpinTarget = (float)((double)this.CurrentSpinTarget * Math.pow(0.001f, f));
        }
        if (!this.ActiveForward && !this.ActiveBack) {
            this.TiltAcceleration = (float)((double)this.TiltAcceleration * Math.pow(0.001f, f));
        }
    }

    void AIThink(float f) {
        switch (this.BotState) {
            case 0: {
                float f2;
                if (this.CurrentTarget < 0) {
                    switch (this.BotType) {
                        case 4: {
                            this.findRandomTarget();
                            break;
                        }
                        case 2: {
                            this.findNearestTarget();
                            break;
                        }
                        case 1: {
                            if (Main.MainRef.random.nextFloat() < 0.5f) {
                                this.findNearestTarget();
                                break;
                            }
                            this.findRandomTarget();
                            break;
                        }
                        case 3: {
                            this.findNearestTarget();
                        }
                    }
                }
                if (this.CurrentTarget >= 0 && !Main.MainRef.cannon[this.CurrentTarget].Active) {
                    this.CurrentTarget = -1;
                }
                this.BotTimer += f;
                if (this.CurrentTarget < 0) break;
                if (Main.MainRef.cannon[this.CurrentTarget].Position.X != this.LastTargetPosition.X || Main.MainRef.cannon[this.CurrentTarget].Position.Z != this.LastTargetPosition.Z || Math.abs(Main.MainRef.cannon[this.CurrentTarget].Position.Z - this.LastTargetPosition.Z) > 10.0f) {
                    this.HasBaseAdjustment = false;
                    this.SpinOffset = 0.0f;
                    this.weapon.LastHitX = 0.0f;
                    this.weapon.LastHitZ = 0.0f;
                    this.LastTargetPosition.setEqual(Main.MainRef.cannon[this.CurrentTarget].Position);
                    this.PowerLevel = 0.0f;
                    this.TiltOffset = 0.0f;
                    this.LineOfSight = false;
                    this.CurrentTarget = -1;
                    return;
                }
                if (this.Position.X != this.LastBotPosition.X || this.Position.Z != this.LastBotPosition.Z || Math.abs(this.Position.Z - this.LastBotPosition.Z) > 10.0f) {
                    this.HasBaseAdjustment = false;
                    this.SpinOffset = 0.0f;
                    this.weapon.LastHitX = 0.0f;
                    this.weapon.LastHitZ = 0.0f;
                    this.LastBotPosition.setEqual(this.Position);
                    this.PowerLevel = 0.0f;
                    this.TiltOffset = 0.0f;
                    this.LineOfSight = false;
                    this.CurrentTarget = -1;
                    return;
                }
                this.Temp.fill(Main.MainRef.cannon[this.CurrentTarget].Position.X, 0.0f, Main.MainRef.cannon[this.CurrentTarget].Position.Z);
                this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                this.TargetDistance = this.Temp.length();
                float f3 = Library_Math.radiansToDegrees((float)Math.atan2(this.Temp.X, this.Temp.Z));
                this.Temp.fill(Main.MainRef.cannon[this.CurrentTarget].Position.X, 0.0f, Main.MainRef.cannon[this.CurrentTarget].Position.Z);
                this.Temp.add(Main.MainRef.island.WindX * this.TargetDistance * -0.02f, 0.0f, Main.MainRef.island.WindZ * this.TargetDistance * -0.02f);
                this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                this.RemoteSpinAngle = f2 = Library_Math.radiansToDegrees((float)Math.atan2(this.Temp.X, this.Temp.Z));
                if (!(this.HasBaseAdjustment || this.weapon.LastHitX == 0.0f && this.weapon.LastHitZ == 0.0f)) {
                    this.Temp2.fill(this.weapon.LastHitX, 0.0f, this.weapon.LastHitZ);
                    this.Temp2.subtract(this.Position.X, 0.0f, this.Position.Z);
                    this.Temp2.rotateY(Library_Math.degreesToRadians(-f3));
                    float f4 = this.TargetDistance / 300.0f;
                    if (f4 > 1.0f) {
                        f4 = 1.0f;
                    }
                    if (Math.abs(this.Temp2.X) < 5.0f) {
                        f4 *= 0.5f;
                    }
                    switch (this.BotType) {
                        case 1: {
                            f4 += Main.MainRef.random.nextFloat() * 2.0f;
                            break;
                        }
                        case 4: {
                            f4 += Main.MainRef.random.nextFloat() * 1.0f;
                        }
                    }
                    if (this.Temp2.X < 0.0f) {
                        this.SpinOffset += (1.0f + Main.MainRef.random.nextFloat()) * 4.0f * f4;
                    } else if (this.Temp2.X > 0.0f) {
                        this.SpinOffset -= (1.0f + Main.MainRef.random.nextFloat()) * 4.0f * f4;
                    }
                }
                this.RemoteSpinAngle += this.SpinOffset;
                this.Temp.X = Main.MainRef.cannon[this.CurrentTarget].Position.Y - this.Position.Y;
                this.Temp.Z = this.TargetDistance;
                this.RemoteTiltTarget = f2 = Library_Math.radiansToDegrees((float)Math.atan2(this.Temp.X, this.Temp.Z));
                if (!this.LineOfSight) {
                    this.RemoteTiltTarget += 45.0f;
                }
                if (!this.HasBaseAdjustment) {
                    this.TiltOffset = (Main.MainRef.random.nextFloat() - 0.1f) * 20.0f;
                }
                if (this.LineOfSight) {
                    this.TiltOffset = 0.0f;
                }
                this.RemoteTiltTarget += this.TiltOffset;
                while (this.RemoteSpinAngle > 180.0f) {
                    this.RemoteSpinAngle -= 360.0f;
                }
                while (this.RemoteSpinAngle < -180.0f) {
                    this.RemoteSpinAngle += 360.0f;
                }
                this.RemoteTiltTarget *= -1.0f;
                if (this.RemoteTiltTarget > this.MaxTiltAngle) {
                    this.RemoteTiltTarget = this.MaxTiltAngle;
                }
                if (this.RemoteTiltTarget < -this.MinTiltAngle) {
                    this.RemoteTiltTarget = -this.MinTiltAngle;
                }
                if (this.PowerLevel == 0.0f) {
                    this.PowerLevel = this.TargetDistance / 700.0f;
                    if (this.PowerLevel > 1.0f) {
                        this.PowerLevel = 1.0f;
                    }
                }
                this.HasBaseAdjustment = true;
                if (!(this.BotTimer > this.BotTimeTarget)) break;
                this.BotTimer = 0.0f;
                this.BotTimeTarget = Main.MainRef.random.nextFloat() * 4.0f;
                if (this.RemoteSpinAngle == this.SpinAngle && this.RemoteTiltTarget == this.ActiveTilt && this.WaitingTimer == 0.0f && !this.weapon.Active) {
                    if (Main.MainRef.network.CurrentPlayer == this.Owner) {
                        this.setWeapon(0);
                        boolean bl = true;
                        float f5 = 0.0f;
                        if (this.weapon.LastHitX != 0.0f || this.weapon.LastHitZ != 0.0f) {
                            this.Temp.fill(this.weapon.LastHitX, 0.0f, this.weapon.LastHitZ);
                            this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                            f5 = this.Temp.length();
                            boolean bl2 = false;
                            float f6 = 1.0f;
                            float f7 = 0.0f;
                            switch (this.BotType) {
                                case 4: {
                                    f6 = 1.0f;
                                    f7 = 30.0f;
                                    break;
                                }
                                case 2: {
                                    f6 = 1.0f;
                                    f7 = 10.0f;
                                    break;
                                }
                                case 1: {
                                    f6 = 0.5f;
                                    f7 = -5.0f;
                                    break;
                                }
                                case 3: {
                                    f6 = 1.0f;
                                    f7 = 0.0f;
                                }
                            }
                            if (!bl2) {
                                this.Temp.setEqual(Main.MainRef.cannon[this.CurrentTarget].Position);
                                this.Temp.subtract(this.Position);
                                this.Temp.normalize();
                                this.weapon.Collider.setPosition(this.Position.X + this.Temp.X * 6.0f, this.Position.Y + this.Temp.Y * 6.0f, this.Position.Z + this.Temp.Z * 6.0f);
                                this.Collision = null;
                                this.Collision = this.weapon.Collider.checkCollision(Main.MainRef.cannon[this.CurrentTarget].Position.X - this.Temp.X * 6.0f, Main.MainRef.cannon[this.CurrentTarget].Position.Y - this.Temp.Y * 6.0f, Main.MainRef.cannon[this.CurrentTarget].Position.Z - this.Temp.Z * 6.0f, false, 0xFFFFFFF);
                                if (this.Collision == null) {
                                    if (Main.MainRef.random.nextFloat() < 0.75f * f6 || this.LineOfSight) {
                                        if (this.canAffordWeapon(7)) {
                                            if (!this.LineOfSight) {
                                                this.LineOfSight = true;
                                                return;
                                            }
                                            bl2 = true;
                                            this.setWeapon(7);
                                            bl = true;
                                            this.LineOfSight = false;
                                        } else {
                                            this.LineOfSight = false;
                                        }
                                    } else {
                                        this.LineOfSight = false;
                                    }
                                } else {
                                    this.LineOfSight = false;
                                }
                            }
                            this.Temp.fill(this.weapon.LastHitX, 0.0f, this.weapon.LastHitZ);
                            this.Temp.subtract(Main.MainRef.cannon[this.CurrentTarget].Position.X, 0.0f, Main.MainRef.cannon[this.CurrentTarget].Position.Z);
                            if (Math.abs(this.Temp2.X) < 10.0f && f5 < this.TargetDistance && !bl2 && Main.MainRef.random.nextFloat() < 0.5f * f6) {
                                if (Main.MainRef.random.nextFloat() < 0.1f && this.BotType != 2 && this.canAffordWeapon(6)) {
                                    bl2 = true;
                                    this.setWeapon(6);
                                    bl = false;
                                }
                                if (!bl2 && this.canAffordWeapon(8)) {
                                    bl2 = true;
                                    this.setWeapon(8);
                                    bl = false;
                                }
                            }
                            if (!bl2 && this.Temp.length() < 35.0f + f7 && Main.MainRef.random.nextFloat() < 0.5f * f6) {
                                if (Main.MainRef.cannon[this.CurrentTarget].Position.Y < 20.0f && this.BotType != 4 && !bl2 && Main.MainRef.random.nextFloat() < 0.5f && this.canAffordWeapon(4)) {
                                    bl2 = true;
                                    this.setWeapon(4);
                                    bl = false;
                                }
                                if (!bl2 && Main.MainRef.random.nextFloat() < 0.25f && this.canAffordWeapon(9)) {
                                    bl2 = true;
                                    this.setWeapon(9);
                                    bl = false;
                                }
                                if (!bl2 && this.canAffordWeapon(5)) {
                                    bl2 = true;
                                    this.setWeapon(5);
                                    bl = false;
                                }
                            }
                        }
                        if (bl) {
                            float f8 = 1.0f;
                            if (f5 != 0.0f && Math.abs(f5 - this.TargetDistance) < 15.0f) {
                                f8 = 0.2f;
                            }
                            switch (this.BotType) {
                                case 1: {
                                    this.PowerLevel += (Main.MainRef.random.nextFloat() - 0.5f) * 0.2f;
                                }
                            }
                            if (f5 < this.TargetDistance) {
                                this.PowerLevel += (0.05f + Main.MainRef.random.nextFloat() * 0.1f) * f8;
                            } else if (f5 > this.TargetDistance) {
                                this.PowerLevel -= (0.05f + Main.MainRef.random.nextFloat() * 0.1f) * f8;
                            }
                            if (this.PowerLevel > 1.0f) {
                                this.PowerLevel = 1.0f;
                            }
                            if (this.PowerLevel < 0.0f) {
                                this.PowerLevel = 0.0f;
                            }
                        }
                        this.fire();
                        if (this.BotType == 4) {
                            this.findRandomTarget();
                        }
                        if (bl) {
                            this.HasBaseAdjustment = false;
                        }
                    } else {
                        boolean bl;
                        if (!this.HasGreeted) {
                            if (Main.MainRef.random.nextFloat() < 0.25f) {
                                Main.MainRef.network.postBotMessage(this.Owner, 5, 0);
                            }
                            this.HasGreeted = true;
                        }
                        if (!(bl = false) && this.CurrentTarget >= 0 && this.Cash >= 520) {
                            float f9 = 0.0f;
                            switch (this.BotType) {
                                case 1: {
                                    f9 = 0.01f;
                                    break;
                                }
                                case 2: {
                                    f9 = 0.25f;
                                    break;
                                }
                                case 3: {
                                    f9 = 0.125f;
                                    break;
                                }
                                case 4: {
                                    f9 = 0.15f;
                                }
                            }
                            if (Main.MainRef.random.nextFloat() < f9) {
                                this.setWeapon(1);
                                float f10 = 0.0f;
                                if (this.weapon.LastHitX != 0.0f || this.weapon.LastHitZ != 0.0f) {
                                    this.Temp.fill(this.weapon.LastHitX, 0.0f, this.weapon.LastHitZ);
                                    this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                                    f10 = this.Temp.length();
                                }
                                float f11 = 1.0f;
                                if (f10 != 0.0f && Math.abs(f10 - this.TargetDistance) < 15.0f) {
                                    f11 = 0.2f;
                                }
                                if (f10 < this.TargetDistance) {
                                    this.PowerLevel += (0.05f + Main.MainRef.random.nextFloat() * 0.1f) * f11;
                                } else if (f10 > this.TargetDistance) {
                                    this.PowerLevel -= (0.05f + Main.MainRef.random.nextFloat() * 0.1f) * f11;
                                }
                                if (this.PowerLevel > 1.0f) {
                                    this.PowerLevel = 1.0f;
                                }
                                if (this.PowerLevel < 0.0f) {
                                    this.PowerLevel = 0.0f;
                                }
                                this.fire();
                                this.HasBaseAdjustment = false;
                                bl = true;
                            }
                        }
                        if (!bl && this.CurrentTarget >= 0 && Main.MainRef.random.nextFloat() < 0.1f && Main.MainRef.cannon[this.CurrentTarget].Position.Y > this.Position.Y + 30.0f && this.canAffordWeapon(3)) {
                            this.setWeapon(3);
                            this.fire();
                            bl = true;
                            this.HasBaseAdjustment = false;
                        }
                        if (!bl && this.findDeathThreat()) {
                            boolean bl3 = false;
                            switch (this.BotType) {
                                case 1: {
                                    if (!(Main.MainRef.random.nextFloat() < 0.025f)) break;
                                    bl3 = true;
                                    break;
                                }
                                case 2: {
                                    if (!(Main.MainRef.random.nextFloat() < 0.1f)) break;
                                    bl3 = true;
                                    break;
                                }
                                case 3: {
                                    if (!(Main.MainRef.random.nextFloat() < 0.2f)) break;
                                    bl3 = true;
                                    break;
                                }
                                case 4: {
                                    if (!(Main.MainRef.random.nextFloat() < 0.15f)) break;
                                    bl3 = true;
                                }
                            }
                            if (bl3 && this.canAffordWeapon(10)) {
                                this.setWeapon(10);
                                this.fire();
                                bl = true;
                                this.HasBaseAdjustment = false;
                            }
                        }
                        this.BotTimer = 0.0f;
                        switch (this.BotType) {
                            case 1: {
                                this.BotTimeTarget = 3.0f + Main.MainRef.random.nextFloat() * 5.0f;
                                break;
                            }
                            case 2: {
                                this.BotTimeTarget = 1.0f + Main.MainRef.random.nextFloat() * 4.0f;
                                break;
                            }
                            case 3: {
                                this.BotTimeTarget = 2.0f + Main.MainRef.random.nextFloat() * 4.0f;
                                break;
                            }
                            case 4: {
                                this.BotTimeTarget = 1.0f + Main.MainRef.random.nextFloat() * 3.0f;
                            }
                        }
                    }
                }
                if (this.BotType == 1 || this.BotType == 4) break;
                int n = this.CurrentTarget;
                this.ensureNearestTarget();
                if (this.CurrentTarget == n) break;
                this.HasBaseAdjustment = false;
                this.LineOfSight = false;
                return;
            }
            case 1: {
                this.BotTimer += f;
                if (!(this.BotTimer > 3.0f)) break;
                this.botRespawn();
                return;
            }
        }
    }

    void playerTeleport() {
        this.Respawning = true;
        Main.MainRef.island.placeMiniMapPlayers();
        this.Respawning = false;
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        this.gamepacket.X2 = this.Position.X;
        this.gamepacket.Y2 = this.Position.Y;
        this.gamepacket.Z2 = this.Position.Z;
        this.place(false, true);
        this.toGround();
        this.gamepacket.Code = (short)25;
        this.gamepacket.Id = (short)this.Owner;
        this.gamepacket.X1 = this.Position.X;
        this.gamepacket.Y1 = this.Position.Y;
        this.gamepacket.Z1 = this.Position.Z;
        Main.MainRef.network.sendPacket(this.gamepacket);
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void botRespawn() {
        this.Dying = false;
        this.Respawning = false;
        this.BotState = 0;
        this.place(false, true);
        this.toGround();
        this.show();
        this.gamepacket.Code = (short)23;
        this.gamepacket.Id = (short)this.Owner;
        this.gamepacket.X1 = this.Position.X;
        this.gamepacket.Y1 = this.Position.Y;
        this.gamepacket.Z1 = this.Position.Z;
        Main.MainRef.network.sendPacket(this.gamepacket);
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void clientRespawn(float f, float f2, float f3) {
        this.Dying = false;
        this.Respawning = false;
        this.Position.X = f;
        this.Position.Y = f2;
        this.Position.Z = f3;
        this.toGround();
        this.show();
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void updateLastPowerMarker() {
        if (this.LastPowerLevel != -1000.0f) {
            Main.MainRef.hud.PowerMarker1.setPosition(-0.84816f + this.LastPowerLevel * 440.0f * 0.002736f, 0.63475204f, 1.0f);
            return;
        }
        Main.MainRef.hud.PowerMarker1.setPosition(0.0f, 0.0f, -1.0f);
    }

    void kill(boolean bl) {
        int n;
        if (Main.MainRef.network.PlayerNumber == this.Owner && Main.MainRef.camera.CurrentCameraView == 4) {
            Main.MainRef.camera.setCamera(1, 1.0E-5f);
        }
        if (this.Owner == Main.MainRef.network.PlayerNumber) {
            ++Main.MainRef.network.StatDeaths;
        }
        if ((n = this.Cash / 20) > 75) {
            n = 75;
        }
        int n2 = 0;
        while (n2 < n) {
            Main.MainRef.ParticleList.add(Main.MainRef.Coins.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 1.0f));
            ++n2;
        }
        this.Cash = (int)Math.floor(Global.STARTINGCASH[Main.MainRef.StartingCash] / 2);
        this.updateCash();
        ++this.Respawns;
        if (this.Respawns > Main.MainRef.MaxRespawns || bl) {
            Main.MainRef.network.ActivePlayers += -1;
            this.weapon.hide();
            this.Active = false;
            Main.MainRef.GameLoop.passBotsToNextPlayer();
        } else {
            this.Respawning = true;
            this.Active = true;
            this.updateRespawns();
        }
        Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.crater(-1, this.Position.X, this.Position.Z, 8.0f, 30.0f, false, 1.0f);
        Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, this.Position.X, this.Position.Y - 4.0f, this.Position.Z, false, 8.0f));
        Main.MainRef.GlobalMedia.Sound_Tilt.stop();
        Main.MainRef.GlobalMedia.Sound_TurnLoop.stop();
        Main.MainRef.GlobalMedia.Sound_TurnStop.stop();
        Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 50.0f));
        Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 2.0f));
        Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 2.0f));
        Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 2.0f));
        Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 2.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.Position.X + Main.MainRef.random.nextFloat() - 0.5f, this.Position.Y + 2.0f, this.Position.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        this.hide();
        Main.MainRef.island.placeMiniMapPlayers();
        if (Main.MainRef.network.PlayerNumber == this.Owner) {
            Main.MainRef.timer.hide();
        }
        if (this.IsBot && this.BotOwner == Main.MainRef.network.PlayerNumber) {
            if (Main.MainRef.random.nextFloat() < 0.25f) {
                Main.MainRef.network.postBotMessage(this.Owner, 3, 0);
            }
            this.BotState = 1;
            this.BotTimer = 0.0f;
        }
    }

    void playerTeleport(float f, float f2) {
        this.Respawning = true;
        Main.MainRef.island.placeMiniMapPlayers();
        this.Respawning = false;
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        this.gamepacket.X2 = this.Position.X;
        this.gamepacket.Y2 = this.Position.Y;
        this.gamepacket.Z2 = this.Position.Z;
        this.Position.X = f;
        this.Position.Z = f2;
        this.toGround();
        this.gamepacket.Code = (short)25;
        this.gamepacket.Id = (short)this.Owner;
        this.gamepacket.X1 = this.Position.X;
        this.gamepacket.Y1 = this.Position.Y;
        this.gamepacket.Z1 = this.Position.Z;
        Main.MainRef.network.sendPacket(this.gamepacket);
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void minusWeapon() {
        if (!this.BarActive) {
            this.CurrentWeapon += -1;
            if (this.CurrentWeapon < 0) {
                this.CurrentWeapon = 11;
            }
            this.updateWeapon();
            Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
        }
    }

    void updateWeapon() {
        if (Main.MainRef.network.PlayerNumber == this.Owner) {
            Main.MainRef.hud.updateWeaponDisplay(this.CurrentWeapon);
        }
    }

    void findRandomTarget() {
        int n = this.CurrentTarget;
        this.CurrentTarget = -1;
        if (!Main.MainRef.network.TeamGame && Main.MainRef.network.ActivePlayers > 1 || Main.MainRef.network.TeamGame && Main.MainRef.network.countTeamsWithActivePlayers() > 1) {
            boolean bl = false;
            while (!bl) {
                int n2 = (int)Math.floor(Main.MainRef.random.nextFloat() * (float)Main.MainRef.CannonCount);
                if (Main.MainRef.network.TeamGame) {
                    if (Main.MainRef.network.PlayerTeam[n2] == Main.MainRef.network.PlayerTeam[this.Owner] || n2 == this.Owner || !Main.MainRef.cannon[n2].Active) continue;
                    this.CurrentTarget = n2;
                    bl = true;
                    continue;
                }
                if (n2 == this.Owner || !Main.MainRef.cannon[n2].Active) continue;
                this.CurrentTarget = n2;
                bl = true;
            }
        }
        if (this.CurrentTarget >= 0 && this.CurrentTarget != n && this.HasGreeted && Main.MainRef.random.nextFloat() < 0.1f) {
            Main.MainRef.network.postBotMessage(this.Owner, 1, this.CurrentTarget);
        }
        if (this.CurrentTarget != n) {
            this.HasBaseAdjustment = false;
        }
    }

    void toGround() {
        if (this.Respawning) {
            return;
        }
        this.Position.Y = Main.MainRef.island.getTerrainHeight(this.Position.X, this.Position.Z) + 6.0f;
        this.Collision = null;
        this.Cannon.setPosition(this.Position.X, 4000.0f, this.Position.Z);
        this.Collision = this.Cannon.checkCollision(this.Position.X, -4.0f, this.Position.Z, false, 8);
        if (this.Collision != null) {
            this.TempVec = this.Collision.getNewPosition();
            if (this.TempVec.getY() + 6.0f > this.Position.Y) {
                this.Position.Y = this.TempVec.getY() + 6.0f;
            }
        }
        this.Cannon.setPosition(this.Position.X, this.Position.Y, this.Position.Z);
        if (this.Position.Y <= 6.0f && (this.Owner == Main.MainRef.network.PlayerNumber || this.IsBot && this.BotOwner == Main.MainRef.network.PlayerNumber)) {
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                ++Main.MainRef.network.StatDrowning;
            }
            this.gamepacket.Code = (short)7;
            this.gamepacket.Var1 = -2.0f;
            this.gamepacket.Id = (short)this.Owner;
            this.gamepacket.Var2 = (short)this.Owner;
            Main.MainRef.network.sendPacket(this.gamepacket);
            this.gamepacket.conditional = Main.MainRef.island.TerrainChangeOwner == this.Owner;
            Main.MainRef.packetmanager.parseIndividualPacket(this.gamepacket);
        }
    }

    public Cannon(int n, int n2, boolean bl, int n3) {
        this.BotTimeTarget = Main.MainRef.random.nextFloat() * 4.0f + 2.0f;
        this.BotOwner = n3;
        this.IsBot = bl;
        this.Color = n2;
        this.Owner = n;
        this.Active = true;
        this.SpinAngle = Main.MainRef.random.nextFloat() * 360.0f;
        this.LastTiltMarker = -1000.0f;
        this.LastPowerLevel = -1000.0f;
        this.Cannon = Main.MainRef.Wt.createGroup();
        this.CannonPivot = Main.MainRef.Wt.createGroup();
        this.BarrelTilt = Main.MainRef.Wt.createGroup();
        this.AppliedColor = 0;
        this.CannonTex = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/CANNON/image.png", false, Global.COLORRGB[this.Color * 3], Global.COLORRGB[this.Color * 3 + 1], Global.COLORRGB[this.Color * 3 + 2]), true);
        this.CannonBarrelActor = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/CANNON/barrel.wsad"), true);
        this.CannonBaseActor = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/CANNON/stone.wsad"), true);
        this.CannonStandActor = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/CANNON/base.wsad"), true);
        this.Cannon.setCollisionMask(4);
        this.Cannon.addObject((WTContainer)this.CannonBaseActor.Model);
        this.Cannon.addObject((WTContainer)this.CannonPivot);
        this.CannonPivot.addObject((WTContainer)this.CannonStandActor.Model);
        this.CannonPivot.addObject((WTContainer)this.BarrelTilt);
        this.BarrelTilt.addObject((WTContainer)this.CannonBarrelActor.Model);
        this.CameraTargetVector.fill(0.0f, 6.0f, -20.0f);
        this.BarrelTargetVector.fill(0.0f, 0.0f, 10.0f);
        ++Main.MainRef.network.ActivePlayers;
        this.weapon = new Weapon();
        this.Cash = Global.STARTINGCASH[Main.MainRef.StartingCash];
        this.MapIcon = Main.MainRef.Wt.createGroup();
        this.MapIcon.attachSurfaceShader(Main.MainRef.GlobalMedia.MapBits.Shader, 0.041040003f, 0.041040003f, 32, 32);
        this.MapIcon.setBitmapTextureRect(0.03125f, 0.03125f, 0.25f, 0.25f);
        Main.MainRef.hud.HUDGroup.addObject((WTContainer)this.MapIcon);
        this.MapIcon.setPosition(0.0f, 0.0f, 0.0f);
        this.MapIcon.setOption(0, 71 + n);
        if (Main.MainRef.CannonShadowsEnabled) {
            this.Shadow = Main.MainRef.Wt.createShadow(1);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Shadow);
        }
    }

    void remoteClientOrient(PacketSmall packetSmall) {
        this.PacketAliveTime = 0.0f;
        this.RemoteSpinAngle = (float)packetSmall.Var1 / 100.0f;
        this.RemoteTiltTarget = (float)packetSmall.Var2 / 100.0f;
    }

    void updateRemoteClient(float f) {
        float f2 = 0.0f;
        float f3 = this.RemoteSpinAngle - this.SpinAngle;
        while (f3 > 180.0f) {
            f3 -= 360.0f;
        }
        while (f3 < -180.0f) {
            f3 += 360.0f;
        }
        if (f3 > 0.0f) {
            f2 = this.MaxSpinSpeed;
        } else if (f3 < 0.0f) {
            f2 = -this.MaxSpinSpeed;
        }
        f2 *= f;
        if (Math.abs(f2) > Math.abs(f3)) {
            f2 = f3;
        }
        this.SpinAngle += f2;
        f3 = this.RemoteTiltTarget - this.ActiveTilt;
        while (f3 > 180.0f) {
            f3 -= 360.0f;
        }
        while (f3 < -180.0f) {
            f3 += 360.0f;
        }
        f2 = 0.0f;
        if (f3 > 0.0f) {
            f2 = this.MaxSpinSpeed;
        } else if (f3 < 0.0f) {
            f2 = -this.MaxSpinSpeed;
        }
        f2 *= f;
        if (Math.abs(f2) > Math.abs(f3)) {
            f2 = f3;
        }
        this.ActiveTilt += f2;
        while (this.SpinAngle > 180.0f) {
            this.SpinAngle -= 360.0f;
        }
        while (this.SpinAngle < -180.0f) {
            this.SpinAngle += 360.0f;
        }
        this.CannonPivot.setOrientation(0.0f, 1.0f, 0.0f, this.SpinAngle);
        this.BarrelTilt.setOrientation(1.0f, 0.0f, 0.0f, this.ActiveTilt);
        this.Temp.setEqual(this.CameraTargetVector);
        this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
        this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
        this.Temp.add(this.Position);
        this.CameraTargetFinal.setEqual(this.Temp);
        this.Temp.setEqual(this.BarrelTargetVector);
        this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
        this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
        this.Temp.add(this.Position);
        this.BarrelTargetFinal.setEqual(this.Temp);
    }

    void tiltUp(float f) {
        this.TiltAcceleration -= 3.0f * f;
        if (this.TiltAcceleration < -3.0f) {
            this.TiltAcceleration = -3.0f;
        }
    }

    void destroy() {
        this.Active = false;
        Main.MainRef.hud.HUDGroup.removeObject((WTContainer)this.MapIcon);
        this.MapIcon.detach();
        this.MapIcon = null;
        Main.MainRef.GlobalMedia.Sound_Tilt.stop();
        Main.MainRef.GlobalMedia.Sound_TurnLoop.stop();
        Main.MainRef.GlobalMedia.Sound_TurnStop.stop();
        this.hide();
        this.BarrelTilt.removeObject((WTContainer)this.CannonBarrelActor.Model);
        this.CannonPivot.removeObject((WTContainer)this.BarrelTilt);
        this.CannonPivot.removeObject((WTContainer)this.CannonStandActor.Model);
        this.Cannon.removeObject((WTContainer)this.CannonBaseActor.Model);
        this.Cannon.removeObject((WTContainer)this.CannonPivot);
        Main.MainRef.MediaList.remove(this.CannonTex);
        this.CannonTex = null;
        Main.MainRef.MediaList.remove(this.CannonBarrelActor);
        Main.MainRef.MediaList.remove(this.CannonStandActor);
        Main.MainRef.MediaList.remove(this.CannonBaseActor);
        this.CannonBarrelActor = null;
        this.CannonStandActor = null;
        this.CannonBaseActor = null;
        this.weapon.destroy();
        this.weapon = null;
    }

    void updateCash() {
        if (this.Owner == Main.MainRef.network.PlayerNumber) {
            Main.MainRef.hud.updateCashDisplay(this.Cash);
            Main.MainRef.hud.createWeaponButton();
            this.gamepacket2.Code = (short)24;
            this.gamepacket2.Var1 = this.Cash;
            this.gamepacket2.Id = (short)this.Owner;
            Main.MainRef.network.sendPacket(this.gamepacket2);
        }
    }

    void activateCamera() {
        Main.MainRef.camera.CurrentCameraView = 1;
        this.CameraTargetVector.fill(0.0f, 6.0f, -20.0f);
    }

    /*
     * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
     * Unable to fully structure code
     * Enabled aggressive block sorting
     */
    boolean findDeathThreat() {
        var1_1 = 0;
        while (var1_1 < Main.MainRef.CannonCount) {
            block15: {
                if (var1_1 == this.Owner || !Main.MainRef.cannon[var1_1].Active) break block15;
                if (Main.MainRef.network.TeamGame) ** GOTO lbl31
                this.Temp.fill(Main.MainRef.cannon[var1_1].weapon.LastHitX, 0.0f, Main.MainRef.cannon[var1_1].weapon.LastHitZ);
                this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                var2_2 = this.Temp.length();
                switch (this.BotType) {
                    case 1: {
                        if (!(var2_2 < 30.0f)) break;
                        return true;
                    }
                    case 2: {
                        if (!(var2_2 < 40.0f)) break;
                        if (Main.MainRef.random.nextFloat() < 0.5f) {
                            this.CurrentTarget = var1_1;
                            this.LastTargetPosition.setEqual(Main.MainRef.cannon[this.CurrentTarget].Position);
                            this.HasBaseAdjustment = false;
                            this.LineOfSight = false;
                        }
                        return true;
                    }
                    case 3: {
                        if (!(var2_2 < 50.0f)) break;
                        return true;
                    }
                    case 4: {
                        if (!(var2_2 < 40.0f)) break;
                        if (Main.MainRef.random.nextFloat() < 0.5f) {
                            this.CurrentTarget = var1_1;
                            this.LastTargetPosition.setEqual(Main.MainRef.cannon[this.CurrentTarget].Position);
                            this.HasBaseAdjustment = false;
                            this.LineOfSight = false;
                        }
                        return true;
                    }
lbl31:
                    // 1 sources

                    if (Main.MainRef.network.PlayerTeam[var1_1] == Main.MainRef.network.PlayerTeam[this.Owner]) break;
                    this.Temp.fill(Main.MainRef.cannon[var1_1].weapon.LastHitX, 0.0f, Main.MainRef.cannon[var1_1].weapon.LastHitZ);
                    this.Temp.subtract(this.Position.X, 0.0f, this.Position.Z);
                    var2_2 = this.Temp.length();
                    switch (this.BotType) {
                        case 1: {
                            if (!(var2_2 < 40.0f) || !(Main.MainRef.random.nextFloat() < 0.1f)) break;
                            return true;
                        }
                        case 2: {
                            if (!(var2_2 < 40.0f) || !(Main.MainRef.random.nextFloat() < 0.4f)) break;
                            return true;
                        }
                        case 3: {
                            if (!(var2_2 < 50.0f)) break;
                            return true;
                        }
                        case 4: {
                            if (!(var2_2 < 60.0f) || !(Main.MainRef.random.nextFloat() < 0.6f)) break;
                            return true;
                        }
                    }
                    break;
                }
            }
            ++var1_1;
        }
        return false;
    }

    void clientTeleport(float f, float f2, float f3, float f4, float f5, float f6) {
        this.Respawning = true;
        Main.MainRef.island.placeMiniMapPlayers();
        this.Respawning = false;
        this.Position.X = f;
        this.Position.Y = f2;
        this.Position.Z = f3;
        this.toGround();
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(f4, f5, f6));
        Main.MainRef.ParticleList.add(new Particle_Object_Teleport(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Teleport.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.Position.X, this.Position.Y, this.Position.Z));
        Main.MainRef.island.placeMiniMapPlayers();
    }

    void updateDeathWaitingTimer(float f) {
        if (this.WaitingTimer > 0.0f) {
            this.WaitingTimer -= f;
            if (this.WaitingTimer < 0.0f) {
                this.WaitingTimer = 0.0f;
                if (this.Owner == Main.MainRef.network.PlayerNumber && Main.MainRef.network.ActivePlayers > 1) {
                    Main.MainRef.GameState = 11;
                    Main.MainRef.camera.setSpectatorCamera();
                    return;
                }
                if (this.Owner == Main.MainRef.network.PlayerNumber) {
                    Main.MainRef.GameState = 13;
                    Main.MainRef.GameLoop.GameStateTimeOut = 0;
                    Main.MainRef.camera.setCamera(6, 0.0f);
                }
            }
        }
    }

    void deactivatePowerBar() {
        Main.MainRef.hud.hidePowerBar();
        this.BarActive = false;
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            if (this.Color != this.AppliedColor) {
                this.CannonTex.tint(Global.COLORRGB[this.Color * 3], Global.COLORRGB[this.Color * 3 + 1], Global.COLORRGB[this.Color * 3 + 2]);
                this.AppliedColor = this.Color;
            }
            if (this.CannonTex.Shader.getNumLayers() < 2) {
                this.CannonTex.Shader.setNumLayers(2);
                this.CannonTex.Shader.setTexture(1, Main.MainRef.GlobalMedia.Reflection.Image);
                this.CannonTex.Shader.setLayerType(1, 4);
                this.CannonTex.Shader.setLayerSource(1, 2);
                this.CannonTex.Shader.setTextureCoordGenMethod(1, 11);
            }
            this.CannonBarrelActor.Model.setSurfaceShader(this.CannonTex.Shader);
            Main.MainRef.wt_stage.CollisionGroup.addObject((WTContainer)this.Cannon);
            this.Cannon.setPosition(this.Position.X, this.Position.Y, this.Position.Z);
            this.updateCash();
            this.updateRespawns();
            this.updateWeapon();
            if (Main.MainRef.CannonShadowsEnabled) {
                this.Shadow.setResolution(128, 128);
                this.Shadow.setAbsoluteOrientationVector(Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].X, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Z, -Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Z, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].X);
                if (!this.ReceiversAdded) {
                    Main.MainRef.island.addReceivers(this.Shadow);
                    this.ReceiversAdded = true;
                }
                this.Shadow.addCaster((WTGroup)this.CannonBarrelActor.Model);
                this.Shadow.addCaster((WTGroup)this.CannonBaseActor.Model);
                this.Shadow.addCaster((WTGroup)this.CannonStandActor.Model);
                this.Shadow.setCasterColor((WTGroup)this.CannonBarrelActor.Model, 51, 131, 126);
                this.Shadow.setCasterColor((WTGroup)this.CannonBaseActor.Model, 51, 131, 126);
                this.Shadow.setCasterColor((WTGroup)this.CannonStandActor.Model, 51, 131, 126);
            }
        }
    }

    void tiltDown(float f) {
        this.TiltAcceleration += 3.0f * f;
        if (this.TiltAcceleration > 3.0f) {
            this.TiltAcceleration = 3.0f;
        }
    }

    void spinRight(float f) {
        this.CurrentSpinTarget += 1.3f * f;
        if (this.CurrentSpinTarget > 1.0f) {
            this.CurrentSpinTarget = 1.0f;
        }
    }

    void updateTiltMarker() {
        float f = 0.0f;
        f = (-this.TiltAngle + this.MinTiltAngle) / (this.MinTiltAngle + this.MaxTiltAngle) * 165.0f;
        Main.MainRef.hud.PitchMarker1.setPosition(-0.990432f, 0.054720003f + f * 0.002736f, 1.0f);
        if (this.LastTiltMarker != -1000.0f) {
            f = (-this.LastTiltMarker + this.MinTiltAngle) / (this.MinTiltAngle + this.MaxTiltAngle) * 165.0f;
            Main.MainRef.hud.PitchMarker2.setPosition(-0.990432f, 0.054720003f + f * 0.002736f, 1.0f);
            return;
        }
        Main.MainRef.hud.PitchMarker2.setPosition(0.0f, 0.0f, -1.0f);
    }

    void tint(int n) {
        if (n != this.AppliedColor) {
            this.Color = n;
            this.AppliedColor = n;
            this.CannonTex.tint(Global.COLORRGB[this.Color * 3], Global.COLORRGB[this.Color * 3 + 1], Global.COLORRGB[this.Color * 3 + 2]);
        }
    }

    void attachFlags() {
    }

    void fire() {
        if (!this.weapon.Active) {
            this.Cash -= Global.WEAPONCOST[this.CurrentWeapon];
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                Main.MainRef.network.StatCash += Global.WEAPONCOST[this.CurrentWeapon];
            }
            if (this.Cash < 0) {
                this.Cash = 0;
            }
            this.updateCash();
            this.Temp.fill(0.0f, 0.0f, 1.0f);
            this.Temp.rotateX(Library_Math.degreesToRadians(-this.ActiveTilt));
            this.Temp.rotateY(Library_Math.degreesToRadians(this.SpinAngle));
            this.CannonBarrelActor.Model.playMotion("fire");
            this.weapon.fire(this.Owner, this.Position.X + this.Temp.X * 5.0f, this.Position.Y + this.Temp.Y * 5.0f, this.Position.Z + this.Temp.Z * 5.0f, this.Temp.X, this.Temp.Y, this.Temp.Z, (this.PowerLevel + 0.5f) * 100.0f, this.CurrentWeapon, true);
            if (Global.PROJECTILE[this.CurrentWeapon]) {
                this.LastPowerLevel = this.PowerLevel;
                this.gamepacket.Code = (short)3;
                this.gamepacket.Id = (short)this.Owner;
                this.gamepacket.X2 = this.Temp.X;
                this.gamepacket.Y2 = this.Temp.Y;
                this.gamepacket.Z2 = this.Temp.Z;
                this.gamepacket.Var1 = this.SpinAngle;
                this.gamepacket.Var2 = this.ActiveTilt;
                this.gamepacket.Var3 = this.PowerLevel;
                this.gamepacket.X1 = this.CurrentWeapon;
                Main.MainRef.network.sendPacket(this.gamepacket);
                int n = 0;
                do {
                    float f = 2.0f + Main.MainRef.random.nextFloat() * 8.0f;
                    Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.Position.X + this.Temp.X * 5.0f, this.Position.Y + this.Temp.Y * 5.0f, this.Position.Z + this.Temp.Z * 5.0f, this.Temp.X * f + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, this.Temp.Y * f + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, this.Temp.Z * f + (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, 0.2f + Main.MainRef.random.nextFloat()));
                } while (++n < 20);
            }
        }
    }

    void checkPacketLife(float f) {
        if (this.IsBot) {
            return;
        }
        this.PacketAliveTime += f;
        if (this.PacketAliveTime > 25.0f) {
            Main.MainRef.Wt.outDebugString("BCASTING NET DEATH FOR PLAYER " + this.Owner);
            this.gamepacket.Code = (short)7;
            this.gamepacket.Var1 = -3.0f;
            this.gamepacket.Id = (short)this.Owner;
            this.gamepacket.Var2 = (short)this.Owner;
            this.gamepacket.conditional = true;
            Main.MainRef.network.sendPacket(this.gamepacket);
            Main.MainRef.packetmanager.parseIndividualPacket(this.gamepacket);
            this.PacketAliveTime = 0.0f;
        }
    }
}

