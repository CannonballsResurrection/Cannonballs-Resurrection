/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_Splash
extends Particle_Object
implements Global,
WTConstants {
    public Particle_Object_Splash(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
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
        if (Main.MainRef.random.nextFloat() < 0.5f && bl) {
            Main.MainRef.ParticleList.add(Main.MainRef.Splash.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, Main.MainRef.random.nextFloat() * -0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, 0.1f + Main.MainRef.random.nextFloat()));
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        if (this.Y <= 1.0f && this.TrajectoryY < 0.0f) {
            if (bl) {
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.4f, this.Z, 3.0f, 2.0f));
            }
            Main.MainRef.ParticleList.remove(this);
            return;
        }
    }
}

