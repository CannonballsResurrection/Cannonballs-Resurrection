/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTCollisionInfo
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 *  wildtangent.webdriver.WTShadow
 *  wildtangent.webdriver.WTVector3D
 */
import java.util.StringTokenizer;
import wildtangent.webdriver.WTCollisionInfo;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;
import wildtangent.webdriver.WTShadow;
import wildtangent.webdriver.WTVector3D;

public class Prop
implements WTOnLoadEvent,
Global {
    private WTFile File;
    private boolean ReceiversAdded = false;
    private WTShadow Shadow;
    Particle_Object FireObject;
    float FX = 0.0f;
    float FY = 0.0f;
    float FZ = 0.0f;
    VEC3D Right = new VEC3D();
    VEC3D Forward = new VEC3D();
    VEC3D Up = new VEC3D();
    private static VEC3D Temp = new VEC3D();
    private static VEC3D TempVec = new VEC3D();
    private static WTCollisionInfo Collision;
    private static WTVector3D TempVector;
    boolean Loaded = false;
    boolean Parsing = false;
    float x;
    float y;
    float z;
    float Angle = 0.0f;
    String Path = "";
    private WTGroup Prop;
    Media_Object_Actor ActorReference;
    Media_Object_Actor[] DebrisChunk;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    float Radius = 0.0f;
    float Height = 0.0f;
    boolean Destructible = true;
    boolean Visible = false;
    boolean Explosive = false;
    int Id = 0;
    VEC3D[] DebrisLocation;
    int DebrisChunks = 0;
    boolean Oriented = false;
    boolean Exploding = false;
    Particle_Object_Shockwave Shock1;
    Particle_Object_Shockwave Shock2;
    boolean WTCollideable = false;
    boolean onGround = true;
    boolean Shadowed = false;
    boolean ShadowVisible = false;
    boolean ShadowCreated = false;
    boolean LocalDestruction = false;
    int LocalDestroyer = -1;
    float LifeTime = 0.0f;
    boolean hasFire = false;
    boolean Standable = false;

    void kill(boolean bl, int n) {
        this.LocalDestruction = bl;
        this.LocalDestroyer = n;
        this.hide();
        Main.MainRef.island.crater(-1, this.X, this.Z, 4.0f, 20.0f, false, 2.0f);
        Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, this.X, this.Y - 4.0f, this.Z, false, 8.0f));
        this.createChunks();
        if (!this.Explosive) {
            int n2 = 0;
            do {
                float f = Main.MainRef.random.nextFloat() - 0.5f;
                Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X + (f + (float)n2 * this.Height / 12.0f) * this.Up.X, this.Y + (f + (float)n2 * this.Height / 12.0f) * this.Up.Y, this.Z + (f + (float)n2 * this.Height / 12.0f) * this.Up.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f * this.Up.X, Main.MainRef.random.nextFloat() * 3.0f * this.Up.Y, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f * this.Up.Z, Main.MainRef.random.nextFloat() * 1.0f + 0.8f));
            } while (++n2 < 10);
            Temp.fill(0.0f, 0.0f, 1.0f);
            n2 = 0;
            do {
                Temp.rotateY(Library_Math.degreesToRadians(22.5f));
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.multiply(5.0f);
                Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X + Prop.TempVec.X + this.Height * 0.5f * this.Up.X, this.Y + Prop.TempVec.Y + this.Height * 0.5f * this.Up.Y, this.Z + Prop.TempVec.Z + this.Height * 0.5f * this.Up.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f + Prop.TempVec.X * 4.0f, (Main.MainRef.random.nextFloat() * 3.0f + 9.0f) * this.Up.Y * Prop.TempVec.Y, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f + Prop.TempVec.Z * 4.0f, Main.MainRef.random.nextFloat() * 1.0f + 2.0f));
            } while (++n2 < 16);
            Temp.fill(0.0f, 0.0f, 1.0f);
            n2 = 0;
            do {
                Temp.rotateY(Library_Math.degreesToRadians(72.0f));
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.multiply(15.0f);
                TempVec.rotateY(Library_Math.degreesToRadians(72.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 6.0f + Main.MainRef.random.nextFloat() * 3.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, Prop.TempVec.X, (30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f) * this.Up.Y, Prop.TempVec.Z, 6.0f, 1));
            } while (++n2 < 5);
            n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Stars.getNext(this.X + (2.0f + Main.MainRef.random.nextFloat() * 4.0f) * this.Up.X, this.Y + (2.0f + Main.MainRef.random.nextFloat() * 4.0f) * this.Up.Y, this.Z + (2.0f + Main.MainRef.random.nextFloat() * 4.0f) * this.Up.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 1.0f + Main.MainRef.random.nextFloat() * 3.0f));
            } while (++n2 < 20);
        } else {
            Temp.fill(0.0f, 0.0f, 1.0f);
            int n3 = 0;
            do {
                Temp.rotateY(Library_Math.degreesToRadians(30.0f));
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.multiply(12.0f);
                Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X + this.Height * 0.5f * this.Up.X, this.Y + this.Height * 0.5f * this.Up.Y, this.Z + this.Height * 0.5f * this.Up.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f + Prop.TempVec.X, (Main.MainRef.random.nextFloat() * 3.0f + 9.0f) * this.Up.Y, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f + Prop.TempVec.Z, Main.MainRef.random.nextFloat() * 1.0f + 2.0f));
            } while (++n3 < 12);
            Temp.fill(0.0f, 0.0f, 1.0f);
            n3 = 0;
            do {
                Temp.rotateY(Library_Math.degreesToRadians(22.5f));
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.multiply(20.0f);
                Main.MainRef.ParticleList.add(Main.MainRef.Stars.getNext(this.X + this.Height * 0.5f * this.Up.X, this.Y + this.Height * 0.5f * this.Up.Y, this.Z + this.Height * 0.5f * this.Up.Z, Prop.TempVec.X, this.Up.Y * 10.0f, Prop.TempVec.Z, 1.0f + Main.MainRef.random.nextFloat() * 3.0f));
            } while (++n3 < 16);
            if (this.Y > 2.0f) {
                this.Shock1 = new Particle_Object_Shockwave(this.X + this.Up.X * this.Height * 0.5f, this.Y + this.Up.Y * this.Height * 0.5f, this.Z + this.Up.Z * this.Height * 0.5f, this.Up.X, this.Up.Y, this.Up.Z, 1.0f, 40.0f);
                this.Shock2 = new Particle_Object_Shockwave(this.X + this.Up.X * this.Height * 0.5f, this.Y + this.Up.Y * this.Height * 0.5f, this.Z + this.Up.Z * this.Height * 0.5f, -this.Up.X, -this.Up.Y, -this.Up.Z, 1.0f, 40.0f);
                this.Exploding = true;
                this.Height *= 2.0f;
                this.X -= this.Up.X;
                this.Y -= this.Up.Y;
                this.Z -= this.Up.Z;
            }
        }
        Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
    }

    public void createChunks() {
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        int n = 0;
        while (n < this.DebrisChunks) {
            float f4 = this.X + (this.Right.X * this.DebrisLocation[n].X + this.Up.X * this.DebrisLocation[n].Y + this.Forward.X * this.DebrisLocation[n].Z);
            float f5 = this.Y + (this.Right.Y * this.DebrisLocation[n].X + this.Up.Y * this.DebrisLocation[n].Y + this.Forward.Y * this.DebrisLocation[n].Z);
            float f6 = this.Z + (this.Right.Z * this.DebrisLocation[n].X + this.Up.Z * this.DebrisLocation[n].Y + this.Forward.Z * this.DebrisLocation[n].Z);
            f = (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f;
            f2 = 10.0f + Main.MainRef.random.nextFloat() * 20.0f;
            f3 = (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f;
            this.DebrisChunk[n].Model.setOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
            Main.MainRef.ChunkList.add(new Chunk_Object(this.DebrisChunk[n], f4, f5, f6, f, f2, f3));
            this.DebrisChunk[n] = null;
            ++n;
        }
    }

    public void onLoadComplete(WTObject wTObject) {
        if (this.Loaded || this.Parsing) {
            return;
        }
        this.Parsing = true;
        this.parseDescriptor(this.assembleFileString());
    }

    public void verifyLoad() {
        if (!this.Loaded && !this.Parsing && this.File != null && this.File.getIsLoaded()) {
            this.onLoadComplete(null);
        }
    }

    public Prop(int n, String string, float f, float f2, float f3, float f4) {
        this.Id = n;
        this.Prop = Main.MainRef.Wt.createGroup();
        this.Path = string;
        this.DebrisChunk = new Media_Object_Actor[10];
        this.DebrisLocation = new VEC3D[10];
        this.Angle = f4;
        if (!this.Path.equalsIgnoreCase("null")) {
            this.File = Main.MainRef.Wt.readFile(Main.MainRef.MediaPath + this.Path + "/prop.dat");
            this.File.setOnLoad((WTOnLoadEvent)this);
        }
        this.X = f;
        this.Z = f3;
        this.Y = f2;
    }

    public Prop(int n, float f, float f2, float f3) {
        this.Id = n;
        this.Prop = Main.MainRef.Wt.createGroup();
        this.DebrisChunk = new Media_Object_Actor[10];
        this.DebrisLocation = new VEC3D[10];
        this.Angle = f3;
        this.X = f;
        this.Z = f2;
        this.ActorReference = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/OBJECTS/TNT"), true);
        this.Prop.addObject((WTContainer)this.ActorReference.Model);
        this.ActorReference.Model.setPosition(0.0f, 0.0f, 0.0f);
        this.Radius = 5.0f;
        this.Height = 9.0f;
        this.Destructible = true;
        this.Explosive = true;
        this.Oriented = true;
        this.Shadowed = false;
        this.onGround = true;
        this.WTCollideable = false;
        if (this.WTCollideable) {
            this.ActorReference.Model.setCollisionMask(2);
        } else {
            this.ActorReference.Model.setCollisionMask(4);
        }
        if (!this.Destructible) {
            this.Y = this.Angle;
        }
        if (this.onGround) {
            this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            this.Prop.setPosition(this.X, this.Y, this.Z);
            this.Prop.setOrientation(0.0f, 1.0f, 0.0f, -this.Angle);
        } else {
            this.Prop.setPosition(this.X, this.Y, this.Z);
        }
        this.Loaded = true;
    }

    public void loadComplete(String string) {
        this.parseDescriptor(string);
    }

    void hide() {
        if (this.Visible) {
            this.ActorReference.Model.setUserData(null);
            if (this.hasFire && this.FireObject != null) {
                Main.MainRef.ParticleList.remove(this.FireObject);
                this.FireObject = null;
            }
            this.Visible = false;
            Main.MainRef.wt_stage.CollisionGroup.removeObject((WTContainer)this.Prop);
            this.hideShadows();
        }
    }

    void createShadows() {
        if (this.Shadowed && !this.ShadowCreated) {
            this.ShadowCreated = true;
            this.Shadow = Main.MainRef.Wt.createShadow(1);
            this.Shadow.setResolution(128, 128);
            this.Shadow.setAbsoluteOrientationVector(Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].X, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Z, -Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Z, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSunVec[Main.MainRef.ActiveMap].X);
        }
    }

    void hideShadows() {
        if (!this.Shadowed) {
            return;
        }
        if (this.ShadowVisible) {
            this.ShadowVisible = false;
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Shadow);
            this.Shadow.removeCaster((WTGroup)this.ActorReference.Model);
            if (this.ReceiversAdded) {
                if (Main.MainRef.island != null) {
                    Main.MainRef.island.removeReceivers(this.Shadow);
                }
                this.ReceiversAdded = false;
            }
        }
    }

    void destroy() {
        this.hideShadows();
        this.hide();
        if (this.Shock1 != null) {
            this.Shock1.destroy();
            this.Shock2.destroy();
            this.Shock1 = null;
            this.Shock2 = null;
        }
        if (this.ActorReference != null) {
            this.Prop.removeObject((WTContainer)this.ActorReference.Model);
            Main.MainRef.MediaList.remove(this.ActorReference);
        }
        this.ActorReference = null;
        this.Prop = null;
        int n = 0;
        while (n < this.DebrisChunks) {
            if (this.DebrisChunk[n] != null) {
                Main.MainRef.MediaList.remove(this.DebrisChunk[n]);
            }
            this.DebrisChunk[n] = null;
            ++n;
        }
    }

    boolean checkCollision(float f, float f2, float f3, float f4) {
        if (!this.Destructible || !this.Visible) {
            return false;
        }
        Main.MainRef.wt_stage.worldToScreenExtended(Temp, this.X, this.Y, this.Z, this.Up.X, this.Up.Y, this.Up.Z, this.Forward.X, this.Forward.Y, this.Forward.Z, this.Right.X, this.Right.Y, this.Right.Z, f, f2, f3);
        return Library_Math.distance(0.0f, 0.0f, Prop.Temp.X, Prop.Temp.Y) <= this.Radius + f4 && Prop.Temp.Z <= this.Height + f4;
    }

    boolean verifyCollision(float f, float f2, float f3, float f4) {
        Main.MainRef.wt_stage.worldToScreenExtended(Temp, this.X, this.Y, this.Z, this.Up.X, this.Up.Y, this.Up.Z, this.Forward.X, this.Forward.Y, this.Forward.Z, this.Right.X, this.Right.Y, this.Right.Z, f, f2, f3);
        return Library_Math.distance(0.0f, 0.0f, Prop.Temp.X, Prop.Temp.Y) <= this.Radius + f4 && Prop.Temp.Z <= this.Height + f4;
    }

    void update(float f) {
        if (this.LifeTime < 10.0f) {
            this.LifeTime += f;
        }
        if (this.Exploding) {
            if (this.Shock1 != null) {
                this.Shock1.updateTimeSlice(f);
            }
            if (this.Shock2 != null) {
                this.Shock2.updateTimeSlice(f);
            }
            if (this.Shock1 == null || !this.Shock1.Visible) {
                this.Exploding = false;
                if (this.Shock1 != null) {
                    this.Shock1.destroy();
                }
                if (this.Shock2 != null) {
                    this.Shock2.destroy();
                }
                this.Shock1 = null;
                this.Shock2 = null;
                return;
            }
            this.Radius = this.Shock1.Scale;
            if (this.Shock1.Scale > 50.0f) {
                this.Shock1.Scale = 50.0f;
            }
            int n = 0;
            while (n < Main.MainRef.CannonCount) {
                if ((Main.MainRef.cannon[n].Owner == Main.MainRef.network.PlayerNumber || Main.MainRef.cannon[n].IsBot && Main.MainRef.cannon[n].BotOwner == Main.MainRef.network.PlayerNumber) && Main.MainRef.cannon[n].Active && !Main.MainRef.cannon[n].Respawning && this.verifyCollision(Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Y, Main.MainRef.cannon[n].Position.Z, 10.0f)) {
                    Packet packet = new Packet();
                    packet.Code = (short)7;
                    packet.Var1 = -10.0f;
                    packet.Var2 = this.LocalDestroyer;
                    packet.Var3 = Main.MainRef.cannon[n].Cash;
                    packet.Id = (short)Main.MainRef.cannon[n].Owner;
                    packet.conditional = this.LocalDestruction;
                    Main.MainRef.network.sendPacket(packet);
                    Main.MainRef.packetmanager.parseIndividualPacket(packet);
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.island.PropCount) {
                if (Main.MainRef.island.prop[n].Visible && Main.MainRef.island.prop[n].Destructible && Main.MainRef.island.prop[n] != this && this.verifyCollision(Main.MainRef.island.prop[n].X, Main.MainRef.island.prop[n].Y, Main.MainRef.island.prop[n].Z, Main.MainRef.island.prop[n].Radius)) {
                    Main.MainRef.island.prop[n].kill(this.LocalDestruction, this.LocalDestroyer);
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n].Visible && this.verifyCollision(Main.MainRef.chest[n].X, Main.MainRef.chest[n].Y, Main.MainRef.chest[n].Z, Main.MainRef.chest[n].Radius)) {
                    Main.MainRef.chest[n].kill(this.LocalDestruction, this.LocalDestroyer, this.LocalDestruction);
                }
                ++n;
            }
        }
    }

    void parseDescriptor(String string) {
        StringTokenizer stringTokenizer;
        String string2 = null;
        String string3 = null;
        String string4 = "";
        int n = 1;
        StringTokenizer stringTokenizer2 = new StringTokenizer(string, "\n");
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<ACTOR>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    string4 = stringTokenizer.nextToken();
                    this.ActorReference = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor(string4), true);
                    this.Prop.addObject((WTContainer)this.ActorReference.Model);
                    this.ActorReference.Model.setPosition(0.0f, 0.0f, 0.0f);
                } else {
                    Main.MainRef.showAlert("MISSING PATH IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<RADIUS>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Radius = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                } else {
                    Main.MainRef.showAlert("MISSING RADIUS IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<HEIGHT>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Height = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                } else {
                    Main.MainRef.showAlert("MISSING HEIGHT IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<DESTRUCTIBLE>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Destructible = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING DESTRUCTABILITY IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<ORIENTED>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Oriented = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING ORIENTATION IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<EXPLOSIVE>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Explosive = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING ORIENTATION IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<SHADOW>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.Shadowed = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING ORIENTATION IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<ONGROUND>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.onGround = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING ORIENTATION IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        if (stringTokenizer2.hasMoreTokens()) {
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":");
            if (stringTokenizer.hasMoreTokens() && (string3 = stringTokenizer.nextToken()).equalsIgnoreCase("<WTCOLLIDEABLE>")) {
                if (stringTokenizer.hasMoreTokens()) {
                    this.WTCollideable = !stringTokenizer.nextToken().equalsIgnoreCase("NO");
                } else {
                    Main.MainRef.showAlert("MISSING ORIENTATION IN " + this.Path + "/prop.dat" + " - Line " + n);
                }
            }
            ++n;
        }
        while (stringTokenizer2.hasMoreTokens()) {
            String string5;
            string2 = stringTokenizer2.nextToken();
            stringTokenizer = new StringTokenizer(string2, ":,");
            String string6 = stringTokenizer.nextToken();
            if (string6.equalsIgnoreCase("<STANDABLE>")) {
                this.Standable = true;
            }
            if (string6.equalsIgnoreCase("<FIRE>")) {
                if (stringTokenizer.countTokens() < 3) {
                    Main.MainRef.showAlert("INVALID DEBRIS CHUNK");
                } else {
                    string5 = stringTokenizer.nextToken();
                    this.FX = Float.valueOf(string5).floatValue();
                    string5 = stringTokenizer.nextToken();
                    this.FY = Float.valueOf(string5).floatValue();
                    string5 = stringTokenizer.nextToken();
                    this.FZ = Float.valueOf(string5).floatValue();
                    this.hasFire = true;
                }
            }
            if (!string6.equalsIgnoreCase("<DEBRIS>")) continue;
            if (stringTokenizer.countTokens() < 4) {
                Main.MainRef.showAlert("INVALID DEBRIS CHUNK");
                continue;
            }
            String string7 = stringTokenizer.nextToken();
            string5 = stringTokenizer.nextToken();
            this.x = Float.valueOf(string5).floatValue();
            string5 = stringTokenizer.nextToken();
            this.y = Float.valueOf(string5).floatValue();
            string5 = stringTokenizer.nextToken();
            this.z = Float.valueOf(string5).floatValue();
            if (this.DebrisChunks >= 9) continue;
            this.DebrisChunk[this.DebrisChunks] = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor(string7), true);
            this.DebrisLocation[this.DebrisChunks] = new VEC3D();
            this.DebrisLocation[this.DebrisChunks].X = this.x;
            this.DebrisLocation[this.DebrisChunks].Y = this.y;
            this.DebrisLocation[this.DebrisChunks].Z = this.z;
            ++this.DebrisChunks;
        }
        if (this.WTCollideable) {
            this.ActorReference.Model.setCollisionMask(2);
        } else {
            this.ActorReference.Model.setCollisionMask(4);
        }
        if (this.Standable) {
            this.ActorReference.Model.setCollisionMask(10);
        }
        if (this.onGround) {
            this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            this.Prop.setPosition(this.X, this.Y, this.Z);
            this.Prop.setOrientation(0.0f, 1.0f, 0.0f, -this.Angle);
        } else {
            this.Prop.setPosition(this.X, this.Y, this.Z);
            this.Prop.setOrientation(0.0f, 1.0f, 0.0f, -this.Angle);
        }
        this.Loaded = true;
    }

    void drop() {
        if (!this.onGround) {
            this.Prop.setPosition(this.X, this.Y, this.Z);
            this.Forward.fill(0.0f, 0.0f, 1.0f);
            this.Forward.rotateY(Library_Math.degreesToRadians(-this.Angle));
            this.Up.fill(0.0f, 1.0f, 0.0f);
            this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
            this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
            this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
            this.Right.normalize();
            if (this.hasFire) {
                Temp.fill(this.FX, this.FY, this.FZ);
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.add(this.X, this.Y, this.Z);
                this.FireObject.X = Prop.TempVec.X;
                this.FireObject.Y = Prop.TempVec.Y;
                this.FireObject.Z = Prop.TempVec.Z;
                return;
            }
        } else {
            this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            this.Prop.setPosition(this.X, this.Y + 400.0f, this.Z);
            this.Forward.fill(0.0f, 0.0f, 1.0f);
            this.Forward.rotateY(Library_Math.degreesToRadians(-this.Angle));
            if (this.Oriented) {
                Main.MainRef.island.calculateNormal(this.Up, this.X, this.Z);
                Main.MainRef.island.calculateNormal(Temp, this.X - this.Radius, this.Z - this.Radius);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X + this.Radius, this.Z - this.Radius);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X - this.Radius, this.Z + this.Radius);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X + this.Radius, this.Z + this.Radius);
                this.Up.add(Temp);
                this.Up.add(0.0f, 2.0f, 0.0f);
                this.Up.divide(6.0f);
                this.Up.normalize();
            } else {
                this.Up.fill(0.0f, 1.0f, 0.0f);
            }
            this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
            this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
            this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
            this.Right.normalize();
            this.Forward.X = this.Up.Y * this.Right.Z - this.Up.Z * this.Right.Y;
            this.Forward.Y = this.Up.Z * this.Right.X - this.Up.X * this.Right.Z;
            this.Forward.Z = this.Up.X * this.Right.Y - this.Up.Y * this.Right.X;
            this.Prop.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
            if (this.hasFire) {
                Temp.fill(this.FX, this.FY, this.FZ);
                Prop.TempVec.X = this.Right.X * Prop.Temp.X + this.Up.X * Prop.Temp.Y + this.Forward.X * Prop.Temp.Z;
                Prop.TempVec.Y = this.Right.Y * Prop.Temp.X + this.Up.Y * Prop.Temp.Y + this.Forward.Y * Prop.Temp.Z;
                Prop.TempVec.Z = this.Right.Z * Prop.Temp.X + this.Up.Z * Prop.Temp.Y + this.Forward.Z * Prop.Temp.Z;
                TempVec.add(this.X, this.Y, this.Z);
                this.FireObject.X = Prop.TempVec.X;
                this.FireObject.Y = Prop.TempVec.Y;
                this.FireObject.Z = Prop.TempVec.Z;
            }
            Collision = null;
            Collision = this.Prop.checkCollision(this.X, this.Y, this.Z, false, 2);
            if (Collision != null && (TempVector = Collision.getNewPosition()).getY() > this.Y) {
                this.Y = TempVector.getY();
            }
            this.Prop.setPosition(this.X, this.Y, this.Z);
            if (this.Y <= 2.0f) {
                this.kill(false, -1);
            }
        }
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            if (this.hasFire && this.FireObject == null) {
                this.FireObject = Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, 0.0f, 0.0f, 0.0f, true, 999.0f));
            }
            if (this.Standable) {
                this.ActorReference.Model.setUserData((Object)this);
            }
            Main.MainRef.wt_stage.CollisionGroup.addObject((WTContainer)this.Prop);
            if (this.ActorReference.Model.getMotionCount() > 0) {
                this.ActorReference.Model.playMotion("loop", 1, 1, 1.0f, Main.MainRef.random.nextFloat() * 8.0f);
            }
            if (Main.MainRef.ShadowsEnabled) {
                this.createShadows();
                this.showShadows();
            }
        }
    }

    String assembleFileString() {
        String string = "";
        while (!this.File.eof()) {
            string = string + this.File.readLine() + "\n";
        }
        this.File.close();
        this.File = null;
        return string;
    }

    void setShadows(boolean bl) {
        if (bl) {
            this.createShadows();
            this.showShadows();
            return;
        }
        this.hideShadows();
    }

    void showShadows() {
        if (!this.Shadowed) {
            return;
        }
        if (!this.ShadowVisible) {
            if (!this.ShadowCreated) {
                this.createShadows();
            }
            this.ShadowVisible = true;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Shadow);
            this.Shadow.addCaster((WTGroup)this.ActorReference.Model);
            this.Shadow.setCasterColor((WTGroup)this.ActorReference.Model, 51, 131, 126);
            if (!this.ReceiversAdded) {
                Main.MainRef.island.addReceivers(this.Shadow);
                this.ReceiversAdded = true;
            }
        }
    }
}

