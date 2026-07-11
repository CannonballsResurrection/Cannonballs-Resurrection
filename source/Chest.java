/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTCollisionInfo
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTVector3D
 */
import wildtangent.webdriver.WTCollisionInfo;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTVector3D;

public class Chest
implements Global {
    private static WTVector3D TempVec;
    private static WTCollisionInfo Collision;
    WTGroup MapIcon;
    WTGroup Chest;
    Media_Object_Actor ChestActor;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    boolean Visible = false;
    boolean DataReceived = false;
    int Contents = 0;
    int Id = 0;
    WTModel ShadowModel;
    WTGroup Shadow;
    float Radius = 6.0f;

    void kill(boolean bl, int n, boolean bl2) {
        this.hide();
        Main.MainRef.island.placeMiniMapChests();
        Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
        Main.MainRef.island.crater(-1, this.X, this.Z, 4.0f, 20.0f, true, 1.5f);
        Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, this.X, this.Y - 4.0f, this.Z, false, 8.0f));
        if (this.Contents != 5) {
            int n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Coins.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 1.0f));
            } while (++n2 < 30);
        }
        if (bl) {
            if (bl2) {
                this.giveTreasure(n);
            }
            if (Main.MainRef.TreasureRespawn == 1) {
                if (this.respawn()) {
                    Main.MainRef.island.placeMiniMapChests();
                    Packet packet = new Packet();
                    packet.Code = (short)19;
                    packet.Id = (short)Main.MainRef.network.PlayerNumber;
                    packet.Var1 = this.Id;
                    packet.X1 = this.X;
                    packet.Y1 = this.Y;
                    packet.Z1 = this.Z;
                    packet.Var2 = this.Contents;
                    packet.conditional = true;
                    Main.MainRef.network.sendPacket(packet);
                    return;
                }
            } else {
                Packet packet = new Packet();
                packet.Code = (short)19;
                packet.Id = (short)Main.MainRef.network.PlayerNumber;
                packet.Var1 = this.Id;
                packet.conditional = false;
                Main.MainRef.network.sendPacket(packet);
            }
        }
    }

    boolean findPlace() {
        boolean bl = false;
        boolean bl2 = false;
        int n = 0;
        int n2 = 0;
        while (!bl && n2 < 300) {
            this.X = Main.MainRef.random.nextFloat() * (float)Main.MainRef.island.Width * Main.MainRef.island.VertexScale;
            this.Z = Main.MainRef.random.nextFloat() * (float)Main.MainRef.island.Height * Main.MainRef.island.VertexScale;
            bl2 = false;
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n].Active && Library_Math.distance(this.X, this.Z, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Z) < 30.0f) {
                    bl2 = true;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n].Id != this.Id && Library_Math.distance(this.X, this.Z, Main.MainRef.chest[n].X, Main.MainRef.chest[n].Z) < 30.0f) {
                    bl2 = true;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.island.PropCount) {
                if (Main.MainRef.island.prop[n].Visible && Main.MainRef.island.prop[n].Destructible && Library_Math.distance(this.X, this.Z, Main.MainRef.island.prop[n].X, Main.MainRef.island.prop[n].Z) < 20.0f) {
                    bl2 = true;
                }
                ++n;
            }
            if (Main.MainRef.island.getTerrainHeight(this.X, this.Z) > 2.0f && !bl2) {
                bl = true;
            }
            ++n2;
        }
        return bl;
    }

    void spawn() {
        this.show();
        this.drop();
    }

    boolean respawn() {
        this.generateTreasure();
        if (this.findPlace()) {
            this.show();
            this.drop();
            return true;
        }
        return false;
    }

    public Chest(int n) {
        this.Id = n;
        this.Chest = Main.MainRef.Wt.createGroup();
        this.ChestActor = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/CHEST"), true);
        this.Chest.addObject((WTContainer)this.ChestActor.Model);
        this.Chest.setCollisionMask(4);
        this.ShadowModel = Main.MainRef.Wt.createPatch(4, 4, 0.25f, 0.25f, -0.5f, 0.5f, false);
        this.ShadowModel.setSurfaceShader(Main.MainRef.GlobalMedia.Shadow_Alpha.Shader);
        this.ShadowModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Shadow = Main.MainRef.Wt.createGroup();
        this.Shadow.attach((WTObject)this.ShadowModel);
        this.MapIcon = Main.MainRef.Wt.createGroup();
        this.MapIcon.attachSurfaceShader(Main.MainRef.GlobalMedia.MapBits.Shader, 0.041040003f, 0.041040003f, 32, 32);
        this.MapIcon.setBitmapTextureRect(0.265625f, 0.03125f, 0.484375f, 0.25f);
        Main.MainRef.hud.HUDGroup.addObject((WTContainer)this.MapIcon);
        this.MapIcon.setPosition(0.0f, 0.0f, 0.0f);
        this.MapIcon.setOption(0, 26 + n);
    }

    void hide() {
        if (this.Visible) {
            this.Visible = false;
            Main.MainRef.wt_stage.CollisionGroup.removeObject((WTContainer)this.Chest);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Shadow);
        }
    }

    void generateTreasure() {
        float f = Main.MainRef.random.nextFloat();
        if (f <= 0.3f) {
            this.Contents = 0;
            return;
        }
        if (f <= 0.6f) {
            this.Contents = 1;
            return;
        }
        if (f <= 0.8f) {
            this.Contents = 2;
            return;
        }
        if (f <= 0.9f) {
            this.Contents = 3;
            return;
        }
        if (f <= 0.95f) {
            this.Contents = 4;
            return;
        }
        if (f <= 1.0f) {
            this.Contents = 5;
        }
    }

    void destroy() {
        this.hide();
        Main.MainRef.hud.HUDGroup.removeObject((WTContainer)this.MapIcon);
        this.MapIcon.detach();
        this.MapIcon = null;
        if (this.Chest != null) {
            this.Chest.removeObject((WTContainer)this.ChestActor.Model);
        }
        if (this.ChestActor != null) {
            Main.MainRef.MediaList.remove(this.ChestActor);
        }
        this.Chest = null;
        this.ChestActor = null;
        this.Shadow.detach();
        this.ShadowModel = null;
        this.Shadow = null;
    }

    void updateShadow() {
        float f = this.X - 6.0f;
        float f2 = this.Z + 6.0f;
        if (this.Visible) {
            int n = 0;
            do {
                int n2 = 0;
                do {
                    float f3 = Main.MainRef.island.getTerrainHeight(f, f2);
                    this.ShadowModel.setPatchPtPos(n2, n, f, f3 + 0.2f, f2);
                    f += 4.0f;
                } while (++n2 < 4);
                f = this.X - 6.0f;
                f2 -= 4.0f;
            } while (++n < 4);
        }
    }

    void drop() {
        this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z) + 2.0f;
        this.Chest.setPosition(this.X, 600.0f, this.Z);
        Collision = null;
        Collision = this.Chest.checkCollision(this.X, -4.0f, this.Z, false, 8);
        if (Collision != null && (TempVec = Collision.getNewPosition()).getY() > this.Y) {
            this.Y = TempVec.getY();
        }
        this.Chest.setPosition(this.X, this.Y, this.Z);
        this.updateShadow();
        if (this.Y <= 2.0f) {
            this.kill(Main.MainRef.network.GameOwner, 0, false);
        }
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.wt_stage.CollisionGroup.addObject((WTContainer)this.Chest);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Shadow);
            this.ChestActor.Model.playMotion("loop", 1, 1, 1.0f, Main.MainRef.random.nextFloat() * 2.0f);
        }
    }

    void giveTreasure(int n) {
        switch (this.Contents) {
            case 0: {
                Main.MainRef.cannon[n].Cash += 100;
                Main.MainRef.cannon[n].updateCash();
                if (Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber) break;
                Main.MainRef.hud.addMessage("100 Gold Dubloons!", 0);
                Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                return;
            }
            case 1: {
                Main.MainRef.cannon[n].Cash += 250;
                Main.MainRef.cannon[n].updateCash();
                if (Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber) break;
                Main.MainRef.hud.addMessage("250 Gold Dubloons!", 0);
                Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                return;
            }
            case 2: {
                Main.MainRef.cannon[n].Cash += 500;
                Main.MainRef.cannon[n].updateCash();
                if (Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber) break;
                Main.MainRef.hud.addMessage("500 Gold Dubloons!", 0);
                Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                return;
            }
            case 3: {
                Main.MainRef.cannon[n].Cash += 1000;
                Main.MainRef.cannon[n].updateCash();
                if (Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber) break;
                Main.MainRef.hud.addMessage("1000 Gold Dubloons!", 0);
                Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                return;
            }
            case 4: {
                Main.MainRef.cannon[n].Cash += 1500;
                Main.MainRef.cannon[n].updateCash();
                if (Main.MainRef.cannon[n].Owner != Main.MainRef.network.PlayerNumber) break;
                Main.MainRef.hud.addMessage("1500 Gold Dubloons!", 0);
                Main.MainRef.GlobalMedia.Sound_Cash.play(false, 128);
                return;
            }
            case 5: {
                if (Main.MainRef.cannon[n].Owner == Main.MainRef.network.PlayerNumber) {
                    Main.MainRef.hud.addMessage("You Were Teleported!", 0);
                }
                Main.MainRef.cannon[n].playerTeleport();
                return;
            }
        }
    }

    void clientRespawn(float f, float f2, float f3, int n) {
        this.hide();
        Main.MainRef.island.placeMiniMapChests();
        Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
        Main.MainRef.island.crater(-1, this.X, this.Z, 4.0f, 20.0f, true, 1.5f);
        Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, this.X, this.Y - 4.0f, this.Z, false, 8.0f));
        if (this.Contents != 5) {
            int n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Coins.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 1.0f));
            } while (++n2 < 30);
        }
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.Contents = n;
        this.drop();
        this.show();
        Main.MainRef.island.placeMiniMapChests();
    }
}

