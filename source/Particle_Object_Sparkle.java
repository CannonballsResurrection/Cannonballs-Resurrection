/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_Sparkle
extends Particle_Object
implements Global,
WTConstants {
    VEC3D Pos = new VEC3D();
    float Scale = 1.0f;
    float Angle = 0.0f;
    float Angle1 = 0.0f;
    int SinVal = 0;
    float HidTimer = 5.0f;
    float length = 1.0f;
    boolean left = false;
    boolean up = false;
    float TrajectoryX = 0.0f;
    float TrajectoryY = 0.0f;
    float TrajectoryZ = 0.0f;
    float rate = 0.0f;

    public Particle_Object_Sparkle() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Firefly.Shader, 2.0f, 2.0f, Main.MainRef.GlobalMedia.Firefly.Width / 2, Main.MainRef.GlobalMedia.Firefly.Width / 2);
        this.Container.setOption(0, 6);
        this.Scale = 0.5f + Main.MainRef.random.nextFloat();
        this.SinVal = (int)Math.floor(Main.MainRef.random.nextFloat() * 6.0f);
        if (Main.MainRef.random.nextFloat() < 0.5f) {
            boolean bl = this.left = !this.left;
        }
        if (Main.MainRef.random.nextFloat() < 0.5f) {
            this.up = !this.up;
        }
    }

    public void updateTimeSlice(float f) {
        this.Angle = this.left ? (this.Angle += 1.0f * f) : (this.Angle -= 1.0f * f);
        if (this.Angle < -1.0f) {
            this.Angle = -1.0f;
        }
        if (this.Angle > 1.0f) {
            this.Angle = 1.0f;
        }
        if (this.rate < 1.0f) {
            this.rate += 0.1f * f;
        }
        this.Angle1 = this.up ? (this.Angle1 += 1.0f * f) : (this.Angle1 -= 1.0f * f);
        if (this.Angle1 < -1.0f) {
            this.Angle1 = -1.0f;
        }
        if (this.Angle1 > 1.0f) {
            this.Angle1 = 1.0f;
        }
        if (Main.MainRef.random.nextFloat() < 0.1f) {
            boolean bl = this.left = !this.left;
        }
        if (Main.MainRef.random.nextFloat() < 0.1f) {
            this.up = !this.up;
        }
        this.Pos.rotateY(Library_Math.degreesToRadians(this.Angle));
        this.Pos.rotateX(Library_Math.degreesToRadians(this.Angle1));
        this.HidTimer -= f;
        if (this.HidTimer <= 0.0f) {
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            this.show();
            this.Container.setPosition(this.X + this.Pos.X * this.rate, this.Y + this.Pos.Y * 0.2f * this.rate, this.Z + this.Pos.Z * this.rate);
            this.Container.setBitmapSize(this.Scale + Main.MainRef.SinTable[this.SinVal], this.Scale + Main.MainRef.SinTable[this.SinVal]);
            return;
        }
        this.hide();
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.Active = true;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.show();
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.rate = 0.0f;
        this.HidTimer = 3.0f + Main.MainRef.random.nextFloat() * 2.0f;
        this.Pos.fill(Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f);
        this.Pos.normalize();
        this.Pos.multiply(2.0f + Main.MainRef.random.nextFloat() * 10.0f);
        this.Container.setPosition(this.X + this.Pos.X, this.Y + this.Pos.Y * 0.2f, this.Z + this.Pos.Z);
        this.updateTimeSlice(0.0f);
    }
}

