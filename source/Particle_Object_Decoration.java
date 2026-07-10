/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTActor
 *  wildtangent.webdriver.WTContainer
 */
import wildtangent.webdriver.WTActor;
import wildtangent.webdriver.WTContainer;

public class Particle_Object_Decoration
extends Particle_Object {
    VEC3D Right = new VEC3D();
    VEC3D Forward = new VEC3D();
    VEC3D Up = new VEC3D();
    static VEC3D Temp = new VEC3D();
    WTActor Actor;
    boolean placed = false;
    float Angle = 0.0f;
    boolean OnWater = false;

    public Particle_Object_Decoration(String string, float f, float f2, float f3, float f4, float f5, boolean bl) {
        this.OnWater = bl;
        this.Actor = string.endsWith(".wsad") || string.endsWith(".wsdf") ? Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + string, Main.MainRef.CacheType) : Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + string + "/actor.wsad", Main.MainRef.CacheType);
        this.Actor.setOrientation(0.0f, 1.0f, 0.0f, f3);
        this.Actor.setAbsoluteScale(f4, f5, f4);
        this.X = f;
        this.Y = -1.0f;
        this.Z = f2;
        Main.MainRef.island.Map.addObject((WTContainer)this.Actor);
    }

    public void destroy() {
        Main.MainRef.island.Map.removeObject((WTContainer)this.Actor);
        this.Actor = null;
    }

    public void updateTimeSlice(float f) {
        if (Main.MainRef.island.TerrainChanged || !this.placed) {
            if (!this.placed) {
                if (this.Actor.getMotionCount() > 0) {
                    this.Actor.playMotion("loop", 1, 1, 1.0f, Main.MainRef.random.nextFloat() * 8.0f);
                }
                this.placed = true;
            }
            float f2 = this.Y;
            this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            if (this.Y <= 0.0f) {
                if (this.OnWater) {
                    this.Y = 0.0f;
                } else {
                    Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
                    Main.MainRef.ParticleList.remove(this);
                    return;
                }
            }
            if (this.Y != f2) {
                this.Actor.setPosition(this.X, this.Y, this.Z);
                this.Forward.fill(0.0f, 0.0f, 1.0f);
                this.Forward.rotateY(Library_Math.degreesToRadians(this.Angle));
                Main.MainRef.island.calculateNormal(this.Up, this.X, this.Z);
                Main.MainRef.island.calculateNormal(Temp, this.X - 10.0f, this.Z - 10.0f);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X + 10.0f, this.Z - 10.0f);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X - 10.0f, this.Z + 10.0f);
                this.Up.add(Temp);
                Main.MainRef.island.calculateNormal(Temp, this.X + 10.0f, this.Z + 10.0f);
                this.Up.add(Temp);
                this.Up.add(0.0f, 2.0f, 0.0f);
                this.Up.divide(6.0f);
                this.Up.normalize();
                this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
                this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
                this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
                this.Right.normalize();
                this.Forward.X = this.Up.Y * this.Right.Z - this.Up.Z * this.Right.Y;
                this.Forward.Y = this.Up.Z * this.Right.X - this.Up.X * this.Right.Z;
                this.Forward.Z = this.Up.X * this.Right.Y - this.Up.Y * this.Right.X;
                this.Actor.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
            }
        }
    }
}

