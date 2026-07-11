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

public class Weapon
implements Global {
    private static WTVector3D TempVec;
    WTGroup Projectile;
    WTModel ShadowModel;
    WTGroup Shadow;
    WTGroup Collider;
    WTGroup Decal;
    WTGroup Decal2;
    WTGroup Decal3;
    private static VEC3D Temp;
    private static VEC3D Temp2;
    private static VEC3D Temp3;
    VEC3D Forward = new VEC3D();
    VEC3D Up = new VEC3D();
    VEC3D Right = new VEC3D();
    float Rotation1 = 0.0f;
    boolean WasFired = false;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    float TrajectoryX = 0.0f;
    float TrajectoryY = 0.0f;
    float TrajectoryZ = 0.0f;
    float LastX = 0.0f;
    float LastY = 0.0f;
    float LastZ = 0.0f;
    private static WTCollisionInfo Collision;
    float TimeAlive = 0.0f;
    boolean Local = false;
    boolean Active = false;
    boolean SoundLoopPlaying = false;
    int Owner = 0;
    int Type = 0;
    int ProjectileModel = 1;
    boolean ActorVisible = true;
    boolean FiredOnTurn = false;
    float LastHitX = 0.0f;
    float LastHitZ = 0.0f;

    void updateXShot(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        Temp.fill(this.TrajectoryX, this.TrajectoryY, this.TrajectoryZ);
        Temp.Normalize();
        this.Projectile.setOrientationVector(Weapon.Temp.X, Weapon.Temp.Y, Weapon.Temp.Z, -Weapon.Temp.Z, Weapon.Temp.Y, Weapon.Temp.X);
        if (Main.MainRef.random.nextFloat() < 0.3f) {
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 0.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
        }
        if (Main.MainRef.random.nextFloat() < 0.4f) {
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 0.0f, 0.0f, 3.0f + Main.MainRef.random.nextFloat() * 2.0f));
            } else {
                Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 0.0f, 0.0f, 3.0f + Main.MainRef.random.nextFloat() * 2.0f));
            }
        }
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                ++Main.MainRef.network.StatMiss;
            }
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2 || n == 1 || n == 2 || n == 3) {
            Main.MainRef.GlobalMedia.Sound_Explosion2.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            if (this.Local) {
                if (this.Owner == Main.MainRef.network.PlayerNumber && n == 0) {
                    ++Main.MainRef.network.StatMiss;
                }
                this.xCrater(this.Owner, this.X, this.Z, this.TrajectoryX, this.TrajectoryZ);
                Temp.fill(this.TrajectoryX, 0.0f, this.TrajectoryZ);
                Temp.normalize();
                Temp.rotateY(Library_Math.degreesToRadians(-45.0f));
                Temp.multiply(30.0f);
                Temp2.setEqual(Temp);
                Temp2.multiply(-1.0f);
                Temp.add(this.X, 0.0f, this.Z);
                Temp2.add(this.X, 0.0f, this.Z);
                this.checkForHitLine(Temp, Temp2, 10.0f);
                Temp.fill(this.TrajectoryX, 0.0f, this.TrajectoryZ);
                Temp.normalize();
                Temp.rotateY(Library_Math.degreesToRadians(45.0f));
                Temp.multiply(30.0f);
                Temp2.setEqual(Temp);
                Temp2.multiply(-1.0f);
                Temp.add(this.X, 0.0f, this.Z);
                Temp2.add(this.X, 0.0f, this.Z);
                this.checkForHitLine(Temp, Temp2, 10.0f);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)20;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.X2 = this.TrajectoryX;
                packet.Z2 = this.TrajectoryZ;
                packet.Var1 = 4.0f;
                packet.Var2 = 20.0f;
                packet.conditional = true;
                Main.MainRef.network.sendPacket(packet);
            }
            this.hide();
        } else if (n >= 4) {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void updateSpikeRoller(float f) {
        float f2 = 0.0f;
        float f3 = 0.0f;
        int n = 0;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        Temp.fill(this.TrajectoryX, 0.0f, this.TrajectoryZ);
        Main.MainRef.GlobalMedia.SpikeBall.Model.setRotation(1.0f, 0.0f, 0.0f, Temp.length() * 0.5f);
        Temp.Normalize();
        n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        if (n == 5) {
            this.Collider.setPosition(this.X, this.Y + 400.0f, this.Z);
            Collision = null;
            Collision = this.Collider.checkCollision(this.X, this.Y - 5.0f, this.Z, false, 8);
            if (Collision != null) {
                TempVec = Collision.getNewPosition();
                f2 = TempVec.getY();
                TempVec = Collision.getImpactNormal();
                this.Up.fill(TempVec.getX(), TempVec.getY(), TempVec.getZ());
            }
        } else {
            f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z) + 1.0f;
        }
        if (n != 0 && n != 5 && this.SoundLoopPlaying) {
            this.SoundLoopPlaying = false;
            Main.MainRef.GlobalMedia.Sound_GrindLoop.stop();
        }
        this.Forward.setEqual(Temp);
        if (this.Y <= f2 + 0.5f && n != 5) {
            Main.MainRef.island.calculateNormal(this.Up, this.X, this.Z);
            Main.MainRef.island.calculateNormal(Temp, this.X - 3.0f, this.Z - 3.0f);
            this.Up.add(Temp);
            Main.MainRef.island.calculateNormal(Temp, this.X + 3.0f, this.Z - 3.0f);
            this.Up.add(Temp);
            Main.MainRef.island.calculateNormal(Temp, this.X - 3.0f, this.Z + 3.0f);
            this.Up.add(Temp);
            Main.MainRef.island.calculateNormal(Temp, this.X + 3.0f, this.Z + 3.0f);
            this.Up.add(Temp);
            this.Up.add(0.0f, 1.0f, 0.0f);
            this.Up.divide(5.0f);
            this.Up.normalize();
        }
        this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
        this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
        this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
        this.Right.normalize();
        this.Forward.X = this.Up.Y * this.Right.Z - this.Up.Z * this.Right.Y;
        this.Forward.Y = this.Up.Z * this.Right.X - this.Up.X * this.Right.Z;
        this.Forward.Z = this.Up.X * this.Right.Y - this.Up.Y * this.Right.X;
        this.Projectile.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
        if (this.Y <= f2 + 0.5f) {
            int n2 = 0;
            do {
                if (!(Main.MainRef.random.nextFloat() < 0.2f)) continue;
                float f4 = (Main.MainRef.random.nextFloat() - 0.5f) * 2.0f;
                f4 = f4 > 0.0f ? (f4 += 1.0f) : (f4 -= 1.0f);
                if (Main.MainRef.random.nextFloat() < 0.5f) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(2, true, 0.0f, this.X + this.Right.X * f4, this.Y + this.Right.Y * f4 - 2.0f, this.Z + this.Right.Z * f4, (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.X * f4 * 10.0f, 10.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.Y * f4 * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.X * f4 * 10.0f, 0.25f + Main.MainRef.random.nextFloat(), false));
                    continue;
                }
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(3, true, 0.0f, this.X + this.Right.X * f4, this.Y + this.Right.Y * f4 - 2.0f, this.Z + this.Right.Z * f4, (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.X * f4 * 10.0f, 10.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.Y * f4 * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f + this.Right.X * f4 * 10.0f, 0.25f + Main.MainRef.random.nextFloat(), false));
            } while (++n2 < 2);
            if (Main.MainRef.random.nextFloat() < 0.3f) {
                Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X, this.Y - 2.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 0.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            }
        }
        if (n == 0 || n == 5) {
            if (n != 5 && this.Y <= 1.8f && f2 <= 1.0f) {
                if (this.Owner == Main.MainRef.network.PlayerNumber) {
                    ++Main.MainRef.network.StatMiss;
                }
                if (this.SoundLoopPlaying) {
                    this.SoundLoopPlaying = false;
                    Main.MainRef.GlobalMedia.Sound_GrindLoop.stop();
                }
                Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
                this.hide();
            } else if (this.Y <= f2 + 0.5f) {
                this.TrajectoryX += Main.MainRef.island.WindX * f * 0.2f;
                this.TrajectoryY += -32.0f * f;
                this.TrajectoryZ += Main.MainRef.island.WindZ * f * 0.2f;
                this.Y = f2 + 0.1f;
                this.TrajectoryY = 0.0f;
                f3 = Library_Math.camDistance3D(this.X, this.Y, this.Z);
                if (!this.SoundLoopPlaying) {
                    this.SoundLoopPlaying = true;
                    Main.MainRef.GlobalMedia.Sound_GrindLoop.playDepth(true, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                } else if (this.SoundLoopPlaying) {
                    Main.MainRef.GlobalMedia.Sound_GrindLoop.setDepth(Library_Math.camDistance3D(this.X, this.Y, this.Z));
                }
            } else {
                this.TrajectoryX += Main.MainRef.island.WindX * f;
                this.TrajectoryY += -32.0f * f;
                this.TrajectoryZ += Main.MainRef.island.WindZ * f;
                if (this.SoundLoopPlaying) {
                    this.SoundLoopPlaying = false;
                    Main.MainRef.GlobalMedia.Sound_GrindLoop.stop();
                }
            }
        } else if (n >= 4) {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void endUserTurn(boolean bl) {
        Main.MainRef.cannon[this.Owner].WaitingTimer = 3.0f;
        Main.MainRef.cannon[this.Owner].DoSwitch = bl;
    }

    public Weapon() {
        this.Decal = Main.MainRef.Wt.createGroup();
        this.Decal.setOption(0, 15);
        this.Decal2 = Main.MainRef.Wt.createGroup();
        this.Decal2.setOption(0, 15);
        this.Decal3 = Main.MainRef.Wt.createGroup();
        this.Decal3.setOption(0, 15);
        this.Collider = Main.MainRef.Wt.createGroup();
        Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Collider);
        this.Projectile = Main.MainRef.Wt.createGroup();
        this.ShadowModel = Main.MainRef.Wt.createPatch(4, 4, 0.25f, 0.25f, -0.5f, 0.5f, false);
        this.ShadowModel.setSurfaceShader(Main.MainRef.GlobalMedia.Shadow_Alpha.Shader);
        this.ShadowModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Shadow = Main.MainRef.Wt.createGroup();
        this.Shadow.attach((WTObject)this.ShadowModel);
    }

    void updateTargetTeleport(float f) {
        int n;
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        if (Main.MainRef.random.nextFloat() < 0.4f) {
            Main.MainRef.ParticleList.add(Main.MainRef.Sparkles.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 2.0f, Main.MainRef.random.nextFloat() * 2.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 2.0f, 1.0f));
        }
        int n2 = this.checkForHit(this.X, this.Y, this.Z, 12.0f, 10.0f, 6.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (n2 == 5 || this.Y <= f2 && n2 == 0) {
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            n = 0;
            do {
                if (Main.MainRef.random.nextFloat() < 0.5f) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(2, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                    continue;
                }
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(3, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
            } while (++n < 20);
            if (this.Local) {
                Main.MainRef.cannon[this.Owner].playerTeleport(this.X, this.Z);
            }
            this.hide();
        } else if (n2 > 0) {
            Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            n = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 1.0f));
            } while (++n < 10);
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
        this.Decal3.setBitmapOrientation(Main.MainRef.random.nextFloat() * 360.0f);
        this.Decal3.setBitmapSize(4.0f + Main.MainRef.SinTable[2] * 0.3f, 4.0f + Main.MainRef.SinTable[2] * 0.3f);
        float f3 = Main.MainRef.random.nextFloat();
        this.Decal2.setBitmapSize(2.0f + f3 * 10.0f, 0.25f * f3);
    }

    void hide() {
        if (this.Active) {
            if (this.SoundLoopPlaying) {
                this.SoundLoopPlaying = false;
                Main.MainRef.GlobalMedia.Sound_GrindLoop.stop();
            }
            if (this.Local) {
                Packet packet = new Packet();
                packet.Code = (short)33;
                packet.Id = (short)this.Owner;
                Main.MainRef.network.sendPacket(packet);
            }
            this.LastHitX = this.X;
            this.LastHitZ = this.Z;
            if (this.ActorVisible) {
                this.ActorVisible = false;
                switch (this.Type) {
                    case 0: 
                    case 1: 
                    case 2: 
                    case 4: {
                        this.Projectile.removeObject((WTContainer)this.Decal);
                        this.Decal.detach();
                        break;
                    }
                    case 11: {
                        this.Projectile.removeObject((WTContainer)this.Decal3);
                        this.Decal3.detach();
                        this.Projectile.removeObject((WTContainer)this.Decal2);
                        this.Decal2.detach();
                        break;
                    }
                    case 5: 
                    case 7: {
                        this.Projectile.removeObject((WTContainer)Main.MainRef.GlobalMedia.Mortar.Model);
                        break;
                    }
                    case 9: {
                        this.Projectile.removeObject((WTContainer)Main.MainRef.GlobalMedia.TNT.Model);
                        break;
                    }
                    case 8: {
                        this.Projectile.removeObject((WTContainer)Main.MainRef.GlobalMedia.SpikeBall.Model);
                        break;
                    }
                    case 6: {
                        this.Projectile.removeObject((WTContainer)Main.MainRef.GlobalMedia.BounceBall.Model);
                    }
                }
            }
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Shadow);
            this.Active = false;
            this.Projectile.setConstantRotation(0.0f, 1.0f, 0.0f, 0.0f);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Projectile);
            if (this.Local && this.WasFired) {
                this.endUserTurn(this.FiredOnTurn);
            }
            this.WasFired = false;
        }
    }

    void fireNonProjectile() {
        if (this.Local) {
            this.endUserTurn(this.FiredOnTurn);
            switch (this.Type) {
                case 3: {
                    this.doTower();
                    return;
                }
                case 10: {
                    this.doTeleport();
                    return;
                }
            }
        }
    }

    void updateProjectile(float f) {
        this.TimeAlive += f;
        this.LastX = this.X;
        this.LastY = this.Y;
        this.LastZ = this.Z;
        switch (this.Type) {
            case 0: {
                this.updateCannonBall(f);
                break;
            }
            case 11: {
                this.updateTargetTeleport(f);
                break;
            }
            case 9: {
                this.updateTNT(f);
                break;
            }
            case 1: {
                this.updateMoleHill(f);
                break;
            }
            case 2: {
                this.updateCrater(f);
                break;
            }
            case 4: {
                this.updateSuperCrater(f);
                break;
            }
            case 5: {
                this.updateXShot(f);
                break;
            }
            case 6: {
                this.updateBouncer(f);
                break;
            }
            case 8: {
                this.updateSpikeRoller(f);
                break;
            }
            case 7: {
                this.updateDumbfire(f);
            }
        }
        this.updateShadow();
    }

    void doTower() {
        Main.MainRef.island.molehill(Main.MainRef.cannon[this.Owner].Position.X, Main.MainRef.cannon[this.Owner].Position.Z, 40.0f, 30.0f, false, 0, 0, 0);
        Packet packet = new Packet();
        packet.Id = (short)this.Owner;
        packet.Code = (short)6;
        packet.X1 = Main.MainRef.cannon[this.Owner].Position.X;
        packet.Z1 = Main.MainRef.cannon[this.Owner].Position.Z;
        packet.Var1 = 40.0f;
        packet.Var2 = 30.0f;
        packet.Var3 = 0.0f;
        Main.MainRef.network.sendPacket(packet);
    }

    void updateCannonBall(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                ++Main.MainRef.network.StatMiss;
            }
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2) {
            if (this.Owner == Main.MainRef.network.PlayerNumber && n == 0) {
                ++Main.MainRef.network.StatMiss;
            }
            Main.MainRef.GlobalMedia.Sound_Explosion2.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            if (this.Local) {
                Main.MainRef.island.crater(this.Owner, this.X, this.Z, 4.0f, 20.0f, true, 1.0f);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)5;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.Var1 = 4.0f;
                packet.Var2 = 20.0f;
                packet.Var3 = 0.0f;
                packet.conditional = true;
                Main.MainRef.network.sendPacket(packet);
            }
            this.hide();
        } else if (n >= 4) {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void updateMoleHill(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2 || n == 1) {
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            int n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(0, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, f2 + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n2 < 20);
            this.hide();
            this.Collider.setPosition(this.X, this.Y + 400.0f, this.Z);
            Collision = this.Collider.checkCollision(this.X, this.Y - 22.0f, this.Z, false, 8);
            if (Collision == null && this.Local) {
                Main.MainRef.island.molehill(this.X, this.Z, 10.0f, 20.0f, true, 32, 40, 135);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)6;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.Var1 = 10.0f;
                packet.Var2 = 20.0f;
                packet.Var3 = 1.0f;
                Main.MainRef.network.sendPacket(packet);
            }
        } else if (n > 0) {
            this.hide();
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            int n3 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(0, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n3 < 20);
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void updateCrater(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 15.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2 || n == 1) {
            int n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(1, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, f2 + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n2 < 20);
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            if (this.Local) {
                Main.MainRef.island.crater(this.Owner, this.X, this.Z, 10.0f, 30.0f, false, 1.0f, true, 130, 31, 115);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)5;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.Var1 = 10.0f;
                packet.Var2 = 30.0f;
                packet.Var3 = 1.0f;
                packet.conditional = false;
                Main.MainRef.network.sendPacket(packet);
            }
            this.hide();
        } else if (n > 0) {
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            int n3 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(1, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n3 < 20);
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void updateDumbfire(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        Temp.fill(this.TrajectoryX, this.TrajectoryY, this.TrajectoryZ);
        Temp.Normalize();
        this.Projectile.setOrientationVector(Weapon.Temp.X, Weapon.Temp.Y, Weapon.Temp.Z, -Weapon.Temp.Z, Weapon.Temp.Y, Weapon.Temp.X);
        if (Main.MainRef.random.nextFloat() < 0.3f) {
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 0.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
        }
        if (Main.MainRef.random.nextFloat() < 0.4f) {
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 0.0f, 0.0f, 3.0f + Main.MainRef.random.nextFloat() * 2.0f));
            } else {
                Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 0.0f, 0.0f, 3.0f + Main.MainRef.random.nextFloat() * 2.0f));
            }
        }
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.TimeAlive > 7.0f) {
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                ++Main.MainRef.network.StatMiss;
            }
            this.TimeAlive = 0.0f;
            this.hide();
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
        }
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                ++Main.MainRef.network.StatMiss;
            }
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2 || n == 1 || n == 2 || n == 3) {
            if (this.Owner == Main.MainRef.network.PlayerNumber && n == 0) {
                ++Main.MainRef.network.StatMiss;
            }
            Main.MainRef.GlobalMedia.Sound_Explosion3.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            if (this.Local) {
                Main.MainRef.island.crater(this.Owner, this.X, this.Z, 6.0f, 20.0f, true, 1.0f);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)5;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.Var1 = 6.0f;
                packet.Var2 = 20.0f;
                packet.Var3 = 0.0f;
                packet.conditional = true;
                Main.MainRef.network.sendPacket(packet);
            }
            this.hide();
        } else if (n >= 4) {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void destroy() {
        this.hide();
        this.Projectile = null;
        Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Collider);
        this.Collider = null;
    }

    void updateShadow() {
        float f = this.X - 1.5f;
        float f2 = this.Z + 1.5f;
        if (this.Active) {
            int n = 0;
            do {
                int n2 = 0;
                do {
                    float f3 = Main.MainRef.island.getTerrainHeight(f, f2);
                    this.ShadowModel.setPatchPtPos(n2, n, f, f3 + 0.2f, f2);
                    f += 1.0f;
                } while (++n2 < 4);
                f = this.X - 2.0f;
                f2 -= 1.5f;
            } while (++n < 4);
        }
    }

    static {
        Temp = new VEC3D();
        Temp2 = new VEC3D();
        Temp3 = new VEC3D();
    }

    void doTeleport() {
        Main.MainRef.cannon[this.Owner].playerTeleport();
    }

    void xCrater(int n, float f, float f2, float f3, float f4) {
        Temp.fill(f3, 0.0f, f4);
        Temp.normalize();
        Temp.rotateY(Library_Math.degreesToRadians(-45.0f));
        Temp.multiply(32.0f);
        Temp2.setEqual(Temp);
        Temp2.multiply(-1.0f);
        Temp.add(f, 0.0f, f2);
        Temp2.add(f, 0.0f, f2);
        Main.MainRef.island.groove(n, Temp, Temp2, 4.0f, 8.0f, false);
        Temp.fill(f3, 0.0f, f4);
        Temp.normalize();
        Temp.rotateY(Library_Math.degreesToRadians(45.0f));
        Temp.multiply(32.0f);
        Temp2.setEqual(Temp);
        Temp2.multiply(-1.0f);
        Temp.add(f, 0.0f, f2);
        Temp2.add(f, 0.0f, f2);
        Main.MainRef.island.groove(n, Temp, Temp2, 4.0f, 8.0f, false);
        Main.MainRef.island.crater(n, f, f2, 4.0f, 20.0f, true, 1.0f);
    }

    void updateBouncer(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z) + 1.5f;
        if (n == 0 || n == 5) {
            if (n == 5) {
                this.Collider.setPosition(this.X, this.Y + 400.0f, this.Z);
                Collision = null;
                Collision = this.Collider.checkCollision(this.X, this.Y - 5.0f, this.Z, false, 8);
                if (Collision != null) {
                    TempVec = Collision.getNewPosition();
                    f2 = TempVec.getY();
                }
            }
            if (n != 5 && this.Y <= 2.0f && f2 <= 1.5f) {
                if (this.Owner == Main.MainRef.network.PlayerNumber) {
                    ++Main.MainRef.network.StatMiss;
                }
                Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
                this.hide();
            } else if (this.Y <= f2) {
                Main.MainRef.GlobalMedia.Sound_Clang.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                this.Y = f2 + 1.0f;
                if (this.TrajectoryY < 0.0f) {
                    this.TrajectoryY *= -0.85f;
                    Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X, this.Y - 2.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 0.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
                    this.Projectile.setConstantRotation(Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, 300.0f);
                    int n2 = 0;
                    do {
                        if (Main.MainRef.random.nextFloat() < 0.5f) {
                            Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(2, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                            continue;
                        }
                        Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(3, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                    } while (++n2 < 4);
                }
                if (this.TrajectoryY < 0.01f) {
                    if (this.Owner == Main.MainRef.network.PlayerNumber) {
                        ++Main.MainRef.network.StatMiss;
                    }
                    Main.MainRef.GlobalMedia.Sound_Explosion2.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                    if (this.Local) {
                        Main.MainRef.island.crater(this.Owner, this.X, this.Z, 4.0f, 20.0f, true, 1.0f);
                        Packet packet = new Packet();
                        packet.Id = (short)this.Owner;
                        packet.Code = (short)5;
                        packet.X1 = this.X;
                        packet.Z1 = this.Z;
                        packet.Var1 = 4.0f;
                        packet.Var2 = 20.0f;
                        packet.Var3 = 0.0f;
                        packet.conditional = true;
                        Main.MainRef.network.sendPacket(packet);
                    }
                    this.hide();
                }
            }
        } else if (n >= 4) {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void show() {
        if (!this.Active) {
            this.Active = true;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Projectile);
            this.Projectile.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Shadow);
        }
    }

    void updateTNT(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        Temp.fill(this.TrajectoryX, this.TrajectoryY, this.TrajectoryZ);
        Temp.Normalize();
        this.Projectile.setOrientationVector(Weapon.Temp.X, Weapon.Temp.Y, Weapon.Temp.Z, -Weapon.Temp.Z, Weapon.Temp.Y, Weapon.Temp.X);
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (n == 0 || n == 5) {
            if (n != 5 && this.Y <= 0.0f && f2 <= 0.0f) {
                Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
                this.hide();
            } else if (this.Y <= f2 || n == 5) {
                if (this.Local) {
                    Packet packet = new Packet();
                    packet.Id = (short)this.Owner;
                    packet.Code = (short)21;
                    packet.X1 = this.X;
                    packet.Z1 = this.Z;
                    Main.MainRef.network.sendPacket(packet);
                    Main.MainRef.packetmanager.parseIndividualPacket(packet);
                }
                this.hide();
            }
        } else {
            Main.MainRef.GlobalMedia.Sound_Explosion1.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 40.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 6.0f));
            this.hide();
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    void updateSuperCrater(float f) {
        float f2 = 0.0f;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryX += Main.MainRef.island.WindX * f;
        this.TrajectoryY += -32.0f * f;
        this.TrajectoryZ += Main.MainRef.island.WindZ * f;
        int n = this.checkForHit(this.X, this.Y, this.Z, 8.0f, 6.0f, 2.0f);
        f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= 0.0f && f2 <= 0.0f) {
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.111f, this.Z, 6.0f, 1.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.1115f, this.Z, 4.0f, 4.0f));
            this.hide();
        } else if (this.Y <= f2 || n == 1) {
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            int n2 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(1, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, f2 + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n2 < 20);
            if (this.Local) {
                Main.MainRef.island.crater(this.Owner, this.X, this.Z, 20.0f, 50.0f, false, 1.0f, true, 130, 31, 115);
                Packet packet = new Packet();
                packet.Id = (short)this.Owner;
                packet.Code = (short)5;
                packet.X1 = this.X;
                packet.Z1 = this.Z;
                packet.Var1 = 20.0f;
                packet.Var2 = 50.0f;
                packet.Var3 = 1.0f;
                packet.conditional = false;
                Main.MainRef.network.sendPacket(packet);
            }
            this.hide();
        } else if (n > 0) {
            Main.MainRef.GlobalMedia.Sound_Splat.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            int n3 = 0;
            do {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(1, false, 0.0f, this.X + Main.MainRef.random.nextFloat() - 0.5f, this.Y + 2.0f, this.Z + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n3 < 20);
        }
        this.Projectile.setPosition(this.X, this.Y, this.Z);
    }

    int checkForHit(float f, float f2, float f3, float f4, float f5, float f6) {
        int n = 0;
        if (this.Active) {
            this.Collider.setPosition(this.LastX, this.LastY, this.LastZ);
            Collision = null;
            Collision = this.Collider.checkCollision(this.X, this.Y, this.Z, false, 2);
            if (Collision != null) {
                WTObject wTObject = Collision.getHitObject();
                Prop prop = (Prop)wTObject.getUserData();
                if (prop != null && prop.Standable) {
                    return 5;
                }
                this.hide();
                return 4;
            }
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n].Active && n != this.Owner && !Main.MainRef.cannon[n].Dying && Library_Math.distance3D(f, f2, f3, Main.MainRef.cannon[n].Position.X, Main.MainRef.cannon[n].Position.Y, Main.MainRef.cannon[n].Position.Z) < f4) {
                    if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                        if (this.Owner == Main.MainRef.network.PlayerNumber) {
                            ++Main.MainRef.network.StatKills;
                        }
                        Packet packet = new Packet();
                        packet.Code = (short)7;
                        packet.Var1 = 1.0f;
                        packet.Var2 = this.Owner;
                        packet.Var3 = Main.MainRef.cannon[n].Cash;
                        packet.Id = (short)n;
                        Main.MainRef.network.sendPacket(packet);
                        Main.MainRef.packetmanager.parseIndividualPacket(packet);
                        Main.MainRef.cannon[n].Dying = true;
                        if (Main.MainRef.cannon[this.Owner].IsBot && Main.MainRef.cannon[this.Owner].BotOwner == Main.MainRef.network.PlayerNumber && Main.MainRef.random.nextFloat() < 0.25f) {
                            Main.MainRef.network.postBotMessage(this.Owner, 4, n);
                        }
                    }
                    this.hide();
                    return 1;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n].Visible && Library_Math.distance3D(f, f2, f3, Main.MainRef.chest[n].X, Main.MainRef.chest[n].Y, Main.MainRef.chest[n].Z) < f5) {
                    if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                        Main.MainRef.chest[n].kill(true, this.Owner, true);
                    }
                    this.hide();
                    return 2;
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.island.PropCount) {
                if (Main.MainRef.island.prop[n].Visible && Main.MainRef.island.prop[n].LifeTime > 3.0f && Main.MainRef.island.prop[n].checkCollision(f, f2, f3, f6)) {
                    this.hide();
                    if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                        Main.MainRef.island.prop[n].kill(true, this.Owner);
                        Packet packet = new Packet();
                        packet.Code = (short)26;
                        packet.Id = (short)Main.MainRef.network.PlayerNumber;
                        packet.Var1 = n;
                        Main.MainRef.network.sendPacket(packet);
                    }
                    return 3;
                }
                ++n;
            }
        }
        return 0;
    }

    int checkForHitLine(VEC3D vEC3D, VEC3D vEC3D2, float f) {
        int n = 0;
        if (this.Active) {
            n = 0;
            while (n < Main.MainRef.CannonCount) {
                if (Main.MainRef.cannon[n].Active && n != this.Owner && !Main.MainRef.cannon[n].Dying) {
                    Temp3.fill(Main.MainRef.cannon[n].Position.X, 0.0f, Main.MainRef.cannon[n].Position.Z);
                    if (Temp3.distanceToLine(vEC3D, vEC3D2) <= f) {
                        if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                                ++Main.MainRef.network.StatKills;
                                Main.MainRef.network.StatMiss += -1;
                            }
                            Packet packet = new Packet();
                            packet.Code = (short)7;
                            packet.Var1 = 1.0f;
                            packet.Var2 = this.Owner;
                            packet.Var3 = Main.MainRef.cannon[n].Cash;
                            packet.Id = (short)n;
                            Main.MainRef.network.sendPacket(packet);
                            Main.MainRef.packetmanager.parseIndividualPacket(packet);
                            Main.MainRef.cannon[n].Dying = true;
                            if (Main.MainRef.cannon[this.Owner].IsBot && Main.MainRef.cannon[this.Owner].BotOwner == Main.MainRef.network.PlayerNumber && Main.MainRef.random.nextFloat() < 0.25f) {
                                Main.MainRef.network.postBotMessage(this.Owner, 4, n);
                            }
                            if (this.Owner == Main.MainRef.network.PlayerNumber) {
                                Main.MainRef.network.pollBotsForCompliments(this.Owner);
                            }
                        }
                        this.hide();
                        return 1;
                    }
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.ChestCount) {
                if (Main.MainRef.chest[n].Visible) {
                    Temp3.fill(Main.MainRef.chest[n].X, 0.0f, Main.MainRef.chest[n].Z);
                    if (Temp3.distanceToLine(vEC3D, vEC3D2) <= f) {
                        if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                            Main.MainRef.chest[n].kill(true, this.Owner, true);
                        }
                        this.hide();
                        return 2;
                    }
                }
                ++n;
            }
            n = 0;
            while (n < Main.MainRef.island.PropCount) {
                if (Main.MainRef.island.prop[n].Visible && Main.MainRef.island.prop[n].LifeTime > 3.0f) {
                    Temp3.fill(Main.MainRef.island.prop[n].X, 0.0f, Main.MainRef.island.prop[n].Z);
                    if (Main.MainRef.island.prop[n].Destructible && Main.MainRef.island.prop[n].Visible && Temp3.distanceToLine(vEC3D, vEC3D2) <= f) {
                        this.hide();
                        if (Global.PROJECTILEIMPACT[this.Type] && this.Local) {
                            Main.MainRef.island.prop[n].kill(true, this.Owner);
                            Packet packet = new Packet();
                            packet.Code = (short)26;
                            packet.Id = (short)Main.MainRef.network.PlayerNumber;
                            packet.Var1 = n;
                            Main.MainRef.network.sendPacket(packet);
                        }
                        return 3;
                    }
                }
                ++n;
            }
        }
        return 0;
    }

    void fire(int n, float f, float f2, float f3, float f4, float f5, float f6, float f7, int n2, boolean bl) {
        this.WasFired = true;
        this.FiredOnTurn = false;
        this.Local = bl;
        this.Owner = n;
        this.Type = n2;
        if (Main.MainRef.network.CurrentPlayer == this.Owner && this.Owner == Main.MainRef.network.PlayerNumber) {
            this.FiredOnTurn = true;
        }
        if (Main.MainRef.network.CurrentPlayer == this.Owner && Main.MainRef.cannon[this.Owner].IsBot && Main.MainRef.cannon[this.Owner].BotOwner == Main.MainRef.network.PlayerNumber) {
            this.FiredOnTurn = true;
        }
        this.TimeAlive = 0.0f;
        this.SoundLoopPlaying = false;
        this.Projectile.setAbsoluteScale(1.0f);
        this.Up.fill(0.0f, 1.0f, 0.0f);
        switch (n2) {
            case 0: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Whistle.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 9: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Whistle.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 1: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Whistle.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 2: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Whistle.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 4: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Whistle.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 5: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Hum.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 6: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Hum.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 7: {
                Main.MainRef.GlobalMedia.Sound_LaunchMissile.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Hum.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 8: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Hum.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                break;
            }
            case 11: {
                Main.MainRef.GlobalMedia.Sound_LaunchCannon.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
                Main.MainRef.GlobalMedia.Sound_Hum.playDepth(false, Library_Math.camDistance3D(f, f2, f3));
            }
        }
        if (Global.PROJECTILE[this.Type]) {
            this.fireProjectile(f, f2, f3, f4, f5, f6, f7);
            return;
        }
        this.fireNonProjectile();
    }

    void fireProjectile(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.ActorVisible = false;
        switch (this.Type) {
            case 0: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)this.Decal);
                this.Decal.attachSurfaceShader(Main.MainRef.GlobalMedia.Weapons.Shader, 2.0f, 2.0f, Main.MainRef.GlobalMedia.Weapons.Width / 2, Main.MainRef.GlobalMedia.Weapons.Width / 2);
                this.Decal.setBitmapTextureRect(0.0f, 0.0f, 0.5f, 0.5f);
                break;
            }
            case 11: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)this.Decal3);
                this.Projectile.addObject((WTContainer)this.Decal2);
                this.Decal3.setBitmapTextureRect(0.0f, 0.0f, 1.0f, 1.0f);
                this.Decal3.attachSurfaceShader(Main.MainRef.GlobalMedia.Sparkle.Shader, 4.0f, 4.0f, Main.MainRef.GlobalMedia.Sparkle.Width / 2, Main.MainRef.GlobalMedia.Sparkle.Width / 2);
                this.Decal2.attachSurfaceShader(Main.MainRef.GlobalMedia.Sparkle.Shader, 6.0f, 6.0f, Main.MainRef.GlobalMedia.Sparkle.Width / 2, Main.MainRef.GlobalMedia.Sparkle.Width / 2);
                this.Rotation1 = 0.0f;
                break;
            }
            case 5: 
            case 7: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)Main.MainRef.GlobalMedia.Mortar.Model);
                Main.MainRef.GlobalMedia.Mortar.Model.setPosition(0.0f, 0.0f, 0.0f);
                Main.MainRef.GlobalMedia.Mortar.Model.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
                break;
            }
            case 9: {
                this.ActorVisible = true;
                this.Projectile.setAbsoluteScale(0.5f);
                this.Projectile.addObject((WTContainer)Main.MainRef.GlobalMedia.TNT.Model);
                Main.MainRef.GlobalMedia.TNT.Model.setPosition(0.0f, 0.0f, 0.0f);
                Main.MainRef.GlobalMedia.TNT.Model.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
                Main.MainRef.GlobalMedia.TNT.Model.setRotation(1.0f, 0.0f, 0.0f, -90.0f);
                break;
            }
            case 8: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)Main.MainRef.GlobalMedia.SpikeBall.Model);
                Main.MainRef.GlobalMedia.SpikeBall.Model.setPosition(0.0f, 0.0f, 0.0f);
                Main.MainRef.GlobalMedia.SpikeBall.Model.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
                break;
            }
            case 6: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)Main.MainRef.GlobalMedia.BounceBall.Model);
                Main.MainRef.GlobalMedia.BounceBall.Model.setPosition(0.0f, 0.0f, 0.0f);
                Main.MainRef.GlobalMedia.BounceBall.Model.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
                this.Projectile.setConstantRotation(Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, 300.0f);
                break;
            }
            case 1: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)this.Decal);
                this.Decal.attachSurfaceShader(Main.MainRef.GlobalMedia.Weapons.Shader, 2.0f, 2.0f, Main.MainRef.GlobalMedia.Weapons.Width / 2, Main.MainRef.GlobalMedia.Weapons.Width / 2);
                this.Decal.setBitmapTextureRect(0.5f, 0.0f, 1.0f, 0.5f);
                break;
            }
            case 2: 
            case 4: {
                this.ActorVisible = true;
                this.Projectile.addObject((WTContainer)this.Decal);
                this.Decal.attachSurfaceShader(Main.MainRef.GlobalMedia.Weapons.Shader, 2.0f, 2.0f, Main.MainRef.GlobalMedia.Weapons.Width / 2, Main.MainRef.GlobalMedia.Weapons.Width / 2);
                this.Decal.setBitmapTextureRect(0.0f, 0.5f, 0.5f, 1.0f);
            }
        }
        this.show();
        this.TrajectoryX = f4 * f7;
        this.TrajectoryY = f5 * f7;
        this.TrajectoryZ = f6 * f7;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.Projectile.setPosition(this.X, this.Y, this.Z);
        this.updateProjectile(1.0E-4f);
    }
}

