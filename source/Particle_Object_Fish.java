/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_Fish
extends Particle_Object
implements Global,
WTConstants {
    private float Frame = 0.0f;
    private int LastFrame = 0;
    static VEC3D Temp = new VEC3D();
    boolean facing = false;
    boolean left = false;
    float Angle = -130.0f;

    public Particle_Object_Fish(float f, float f2, float f3, float f4, float f5, float f6) {
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attachSurfaceShader(Main.MainRef.GlobalMedia.Fish.Shader, 7.0f, 7.0f, Main.MainRef.GlobalMedia.Fish.Width / 2, Main.MainRef.GlobalMedia.Fish.Width / 2);
        this.Container.setOption(0, 1);
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.show();
        this.setFrame(0);
        Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
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

    public void updateTimeSlice(float f) {
        this.Frame += f * 15.0f;
        while (this.Frame >= 7.5f) {
            this.Frame -= 7.0f;
        }
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        Main.MainRef.wt_stage.worldToScreen(Temp, this.TrajectoryX + Main.MainRef.camera.X, this.TrajectoryY + Main.MainRef.camera.Y, this.TrajectoryZ + Main.MainRef.camera.Z);
        Particle_Object_Fish.Temp.Z = 0.0f;
        Temp.normalize();
        if (!this.facing) {
            if (Particle_Object_Fish.Temp.X < 0.0f) {
                this.left = true;
                this.Angle = 130.0f;
            }
            this.facing = true;
        }
        float f2 = Library_Math.radiansToDegrees(Temp.angleBetween(0.0f, 1.0f, 0.0f));
        f2 = this.left ? -f2 + 90.0f : (f2 -= 90.0f);
        if (this.Angle < f2) {
            this.Angle += 100.0f * f;
            if (this.Angle > f2) {
                this.Angle = f2;
            }
        } else if (this.Angle > f2) {
            this.Angle -= 100.0f * f;
            if (this.Angle < f2) {
                this.Angle = f2;
            }
        }
        this.Container.setBitmapOrientation(this.Angle);
        if (this.Y < 0.0f && this.TrajectoryY < 0.0f) {
            Main.MainRef.GlobalMedia.Sound_Splash.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            if (this.isOnscreen(this.X, this.Y, this.Z)) {
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
            Main.MainRef.ParticleList.remove(this);
            return;
        }
        this.Container.setPosition(this.X, this.Y, this.Z);
        if ((int)Math.floor(this.Frame) != this.LastFrame) {
            if (this.left) {
                this.setLeftFrame((int)Math.floor(this.Frame));
                return;
            }
            this.setFrame((int)Math.floor(this.Frame));
        }
    }

    void setFrame(int n) {
        int n2 = (int)Math.ceil(n / 4) - 1;
        int n3 = n - n2 * 4;
        this.Container.setBitmapTextureRect(0.5f * (float)n2, 0.25f * (float)n3, 0.5f * (float)(n2 + 1), 0.25f * (float)(n3 + 1));
    }

    void setLeftFrame(int n) {
        int n2 = (int)Math.ceil(n / 4) - 1;
        int n3 = n - n2 * 4;
        this.Container.setBitmapTextureRect(0.5f * (float)(n2 + 1), 0.25f * (float)n3, 0.5f * (float)n2, 0.25f * (float)(n3 + 1));
    }
}

