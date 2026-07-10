/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_SplashDrop
extends Particle_Object
implements Global,
WTConstants {
    public Particle_Object_SplashDrop() {
        this.FromStore = true;
        this.Container = Main.MainRef.Wt.createGroup();
        if (Main.MainRef.random.nextFloat() <= 0.5f) {
            this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Splash.Shader, 1.0f, 1.0f, Main.MainRef.GlobalMedia.Splash.Width / 2, Main.MainRef.GlobalMedia.Splash.Width / 2);
            this.Container.setOption(0, 3);
            return;
        }
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Splash2.Shader, 1.0f, 1.0f, Main.MainRef.GlobalMedia.Splash.Width / 2, Main.MainRef.GlobalMedia.Splash.Width / 2);
        this.Container.setOption(0, 4);
    }

    public void updateTimeSlice(float f) {
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        if (this.Y < 0.0f && this.TrajectoryY < 0.0f) {
            Main.MainRef.ParticleList.removeFromList(this);
            return;
        }
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            this.show();
            this.Container.setPosition(this.X, this.Y, this.Z);
            return;
        }
        this.hide();
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.Active = true;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.Container.setBitmapSize(f7, f7);
        this.show();
    }
}

