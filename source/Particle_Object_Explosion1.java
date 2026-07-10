/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Explosion1
extends Particle_Object {
    private float Frame = 0.0f;
    private int LastFrame = 0;

    public Particle_Object_Explosion1() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Explosion1_Shader.Shader, 1.0f, 1.0f, Main.MainRef.GlobalMedia.Explosion1_Shader.Width / 2, Main.MainRef.GlobalMedia.Explosion1_Shader.Width / 2);
        this.Container.setOption(0, 8);
    }

    public void updateTimeSlice(float f) {
        this.Frame += f * 20.0f;
        if (this.Frame > 15.0f) {
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.Container.setPosition(this.X, this.Y, this.Z);
        if ((int)Math.floor(this.Frame) != this.LastFrame) {
            this.setFrame((int)Math.floor(this.Frame));
        }
    }

    void setFrame(int n) {
        int n2 = (int)Math.ceil(n / 4);
        int n3 = n - n2 * 4;
        this.Container.setBitmapTextureRect(0.25f * (float)n3, 0.25f * (float)n2, 0.25f * (float)(n3 + 1), 0.25f * (float)(n2 + 1));
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.Active = true;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.Container.setBitmapSize(1.0f + f7, 1.0f + f7);
        this.Frame = 0.0f;
        this.LastFrame = 0;
        this.setFrame((int)this.Frame);
        this.show();
    }
}

