/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Coin
extends Particle_Object
implements Global {
    private float Frame = 0.0f;
    private int LastFrame = 0;
    private float LifeTime = 0.0f;

    public Particle_Object_Coin() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Coin.Shader, 2.0f, 2.0f, 128, 128);
        this.Container.setOption(0, 7);
    }

    public void updateTimeSlice(float f) {
        boolean bl = this.isOnscreen(this.X, this.Y, this.Z);
        this.Frame += f * 20.0f;
        while (this.Frame > 15.5f) {
            this.Frame -= 15.0f;
        }
        this.LifeTime += f;
        if (this.LifeTime > 5.0f) {
            if (bl) {
                Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
            }
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        float f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= f2) {
            if (f2 < 1.0f) {
                if (Main.MainRef.random.nextFloat() < 0.25f && bl) {
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
                }
                Main.MainRef.ParticleList.removeFromList(this);
                return;
            }
            this.Y = f2;
            if (this.TrajectoryY < 0.0f) {
                this.TrajectoryY = -this.TrajectoryY / 1.25f;
            }
        }
        if (bl) {
            this.show();
            this.Container.setPosition(this.X, this.Y, this.Z);
            if ((int)Math.floor(this.Frame) != this.LastFrame) {
                this.setFrame((int)Math.floor(this.Frame));
                return;
            }
        } else {
            this.hide();
        }
    }

    void setFrame(int n) {
        int n2 = (int)Math.ceil(n / 4);
        int n3 = n - (n2 - 1) * 4;
        this.Container.setBitmapTextureRect(0.25f * (float)n2, 0.25f * (float)n3, 0.25f * (float)(n2 + 1), 0.25f * (float)(n3 + 1));
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.Active = true;
        this.LifeTime = Main.MainRef.random.nextFloat();
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.Container.setBitmapOrientation(Main.MainRef.random.nextFloat() * 360.0f);
        this.Frame = 0.0f;
        this.LastFrame = 0;
        this.setFrame((int)this.Frame);
        this.show();
    }
}

