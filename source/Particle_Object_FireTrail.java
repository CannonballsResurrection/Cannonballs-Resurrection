/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_FireTrail
extends Particle_Object
implements Global,
WTConstants {
    float LifeTime = 0.0f;
    int Type = 0;

    public Particle_Object_FireTrail(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
    }

    public Particle_Object_FireTrail(float f, float f2, float f3, float f4, float f5, float f6, float f7, int n) {
        this.Type = n;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
    }

    public void destroy() {
    }

    public void updateTimeSlice(float f) {
        boolean bl = this.isOnscreen(this.X, this.Y, this.Z);
        this.LifeTime += f;
        if (this.LifeTime > 6.0f) {
            Main.MainRef.ParticleList.remove(this);
            return;
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        float f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= f2) {
            if (f2 < 1.0f) {
                if (bl) {
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                    Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.4f, this.Z, 3.0f, 2.0f));
                }
                Main.MainRef.ParticleList.remove(this);
                return;
            }
            this.Y = f2;
            if (this.TrajectoryY < 0.0f) {
                this.TrajectoryY = -this.TrajectoryY / 1.25f;
            }
        }
        if (bl) {
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                if (this.Type == 0) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.8f + 0.3f));
                } else {
                    Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.8f + 0.3f));
                }
            }
            if (Main.MainRef.random.nextFloat() < 0.3f) {
                Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 0.0f, 0.0f, 3.0f + Main.MainRef.random.nextFloat() * 2.0f));
            }
        }
    }
}

