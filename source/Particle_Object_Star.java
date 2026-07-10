/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Star
extends Particle_Object
implements Global {
    private float Frame = 0.0f;
    private int LastFrame = 0;
    private float LifeTime = 0.0f;

    public Particle_Object_Star() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Star.Shader, 2.0f, 2.0f, 128, 128);
        this.Container.setOption(0, 12);
    }

    public void updateTimeSlice(float f) {
        float f2 = this.Opacity;
        if (this.TrajectoryY < 0.0f || this.Opacity < 255.0f) {
            this.Opacity -= f * 180.0f;
        }
        this.Frame += f * 10.0f;
        while (this.Frame > 15.5f) {
            this.Frame -= 15.0f;
        }
        this.LifeTime += f;
        if (this.LifeTime > 5.0f || this.Opacity <= 0.0f) {
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        float f3 = (float)Math.pow(0.99f, f);
        this.TrajectoryX *= f3;
        this.TrajectoryZ *= f3;
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f * 0.75f;
        float f4 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= f4) {
            if (f4 < 1.0f) {
                Main.MainRef.ParticleList.removeFromList(this);
                return;
            }
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 0.5f + 0.3f));
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            this.show();
            this.Container.setPosition(this.X, this.Y, this.Z);
            if ((int)Math.floor(this.Frame) != this.LastFrame) {
                this.setFrame((int)Math.floor(this.Frame));
            }
            if ((int)this.Opacity != (int)f2) {
                this.Container.setBitmapOpacity((int)this.Opacity);
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
        this.Opacity = 255.0f;
        this.LifeTime = Main.MainRef.random.nextFloat();
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.Container.setBitmapSize(f7, f7);
        this.Container.setBitmapOpacity(255);
        this.Container.setBitmapOrientation(Main.MainRef.random.nextFloat() * 360.0f);
        this.Frame = Main.MainRef.random.nextFloat() * 15.0f;
        this.LastFrame = 0;
        this.setFrame((int)this.Frame);
        this.show();
    }
}

