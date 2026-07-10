/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Smoke
extends Particle_Object {
    private float Scale = 0.0f;
    private float accel = 20.0f;
    private float rise = 1.0f;
    int Type = 0;

    public Particle_Object_Smoke(int n) {
        this.Type = n;
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Smoke_Alpha.Shader, 1.0f, 1.0f, Main.MainRef.GlobalMedia.Smoke_Alpha.Width / 2, Main.MainRef.GlobalMedia.Smoke_Alpha.Height / 2);
        this.Container.setOption(0, 5);
    }

    public void updateTimeSlice(float f) {
        this.Scale += f * this.accel;
        if (this.accel > -8.0f) {
            this.accel -= f * 20.0f;
        }
        if (this.Scale < 0.05f) {
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        float f2 = (float)Math.pow(0.7f, f);
        this.TrajectoryX *= f2;
        this.TrajectoryY *= f2;
        this.TrajectoryZ *= f2;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.Y += this.rise * f;
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            this.show();
            this.Container.setBitmapSize(this.Scale, this.Scale);
            this.Container.setPosition(this.X, this.Y, this.Z);
            return;
        }
        this.hide();
    }

    void setFrame(int n, boolean bl) {
        int n2 = (int)Math.ceil(n / 2);
        int n3 = n - (n2 - 1) * 2;
        if (this.Type == 0) {
            n3 += 2;
        }
        if (bl) {
            this.Container.setBitmapTextureRect(0.25f * (float)(n3 + 1), 0.5f * (float)n2, 0.25f * (float)n3, 0.5f * (float)(n2 + 1));
            return;
        }
        this.Container.setBitmapTextureRect(0.25f * (float)n3, 0.5f * (float)n2, 0.25f * (float)(n3 + 1), 0.5f * (float)(n2 + 1));
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.Active = true;
        this.Scale = f7;
        this.accel = Main.MainRef.random.nextFloat() * 10.0f + 10.0f;
        this.rise = Main.MainRef.random.nextFloat() * 3.0f + 1.0f;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        int n = (int)Math.floor(Main.MainRef.random.nextFloat() * 8.0f);
        if (n > 3) {
            this.setFrame(n - 4, true);
        } else {
            this.setFrame(n, false);
        }
        this.show();
        this.Container.setBitmapSize(this.Scale, this.Scale);
    }
}

