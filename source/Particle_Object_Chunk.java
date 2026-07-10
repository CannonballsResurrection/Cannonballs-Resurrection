/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Chunk
extends Particle_Object
implements Global {
    private float LifeTime = 0.0f;
    float Angle = 0.0f;
    float Rotation = 0.0f;
    boolean Smoke = true;

    public Particle_Object_Chunk() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Chunks.Shader, 2.0f, 2.0f, 128, 128);
        this.Container.setOption(0, 10);
    }

    public void updateTimeSlice(float f) {
        boolean bl = this.isOnscreen(this.X, this.Y, this.Z);
        if (this.Rotation != 0.0f && bl) {
            this.Angle += this.Rotation * f;
            this.Container.setBitmapOrientation(this.Angle);
        }
        this.LifeTime += f;
        if (this.LifeTime > 5.0f || this.Opacity <= 0.0f) {
            if (this.Smoke && bl) {
                Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
            }
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        float f2 = (float)Math.pow(0.99f, f);
        this.TrajectoryX *= f2;
        this.TrajectoryZ *= f2;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        float f3 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= f3) {
            if (f3 < 1.0f) {
                Main.MainRef.ParticleList.removeFromList(this);
                return;
            }
            if (this.TrajectoryY < 0.0f) {
                if (this.Smoke && bl) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
                }
                Main.MainRef.ParticleList.removeFromList(this);
                return;
            }
            this.Y = f3 + 1.0f;
        }
        if (bl) {
            this.show();
            this.Container.setPosition(this.X, this.Y, this.Z);
            return;
        }
        this.hide();
    }

    void setFrame(int n) {
        int n2 = (int)Math.ceil(n / 4);
        int n3 = n - n2 * 4;
        this.Container.setBitmapTextureRect(0.25f * (float)n2, 0.25f * (float)n3, 0.25f * (float)(n2 + 1), 0.25f * (float)(n3 + 1));
    }

    public void activate(int n, boolean bl, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, boolean bl2) {
        this.Active = true;
        this.Smoke = bl2;
        this.Opacity = 255.0f;
        this.LifeTime = Main.MainRef.random.nextFloat();
        this.X = f2;
        this.Y = f3;
        this.Z = f4;
        this.Rotation = f;
        this.TrajectoryX = f5;
        this.TrajectoryY = f6;
        this.TrajectoryZ = f7;
        this.Container.setBitmapSize(f8, f8);
        if (bl) {
            this.Angle = Main.MainRef.random.nextFloat() * 360.0f;
            this.Container.setBitmapOrientation(this.Angle);
        } else {
            this.Container.setBitmapOrientation(0.0f);
        }
        this.setFrame(n);
        this.show();
    }
}

