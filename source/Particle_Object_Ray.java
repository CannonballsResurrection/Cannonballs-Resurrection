/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTObject;

public class Particle_Object_Ray
extends Particle_Object
implements Global {
    Particle_Plane Plane;
    float Opacity = 255.0f;
    float Scale = 1.0f;
    float Scale2 = 1.0f;
    float delay = 0.0f;
    float Rate = 0.0f;
    private static VEC3D Temporary2 = new VEC3D();
    private VEC3D Up = new VEC3D();

    public Particle_Object_Ray(float f, float f2, float f3, float f4, float f5, float f6, boolean bl) {
        this.Plane = Main.MainRef.ParticlePlaneList.reservePlane();
        this.X = f;
        this.Z = f2;
        this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z) - 0.25f;
        this.Rate = 140.0f + Main.MainRef.random.nextFloat() * 120.0f;
        if (bl) {
            this.delay = Main.MainRef.random.nextFloat();
        }
        this.Scale = f6;
        this.Scale2 = 0.001f;
        this.Up.fill(f3, f4, f5);
        this.Plane.create(1.0f, 1.0f, 17);
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attach((WTObject)this.Plane.Plane);
        this.Container.setAbsoluteScale(this.Scale, this.Scale2, 1.0f);
        this.Container.setOption(0, 11);
        this.show();
        this.Container.setPosition(this.X, this.Y, this.Z);
        this.Opacity = 255.0f;
        this.Plane.Plane.setOpacity(255);
    }

    void hide() {
        if (this.Visible) {
            this.Visible = false;
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Container);
        }
    }

    public void destroy() {
        this.hide();
        this.Container.detach();
        this.Container = null;
        this.Plane = null;
    }

    public void updateTimeSlice(float f) {
        this.delay -= f;
        if (this.delay <= 0.0f) {
            if (this.Scale2 > 5.0f) {
                this.Opacity -= f * 100.0f;
            }
            this.Scale2 += f * this.Rate;
            this.Scale += f * this.Rate * 0.01f;
            this.Rate = (float)((double)this.Rate * Math.pow(0.1f, f));
            if (this.Opacity < 0.0f) {
                this.hide();
                Main.MainRef.ParticleList.remove(this);
                return;
            }
            this.Plane.Plane.setOpacity((int)this.Opacity);
            this.Container.setAbsoluteScale(this.Scale, this.Scale2, 1.0f);
            Particle_Object.Temporary.fill(this.X, 0.0f, this.Z);
            Particle_Object.Temporary.subtract(Main.MainRef.camera.X, 0.0f, Main.MainRef.camera.Z);
            Particle_Object.Temporary.normalize();
            Particle_Object_Ray.Temporary2.X = this.Up.Y * Particle_Object.Temporary.Z - this.Up.Z * Particle_Object.Temporary.Y;
            Particle_Object_Ray.Temporary2.Y = this.Up.Z * Particle_Object.Temporary.X - this.Up.X * Particle_Object.Temporary.Z;
            Particle_Object_Ray.Temporary2.Z = this.Up.X * Particle_Object.Temporary.Y - this.Up.Y * Particle_Object.Temporary.X;
            Temporary2.normalize();
            Particle_Object.Temporary.X = this.Up.Y * Particle_Object_Ray.Temporary2.Z - this.Up.Z * Particle_Object_Ray.Temporary2.Y;
            Particle_Object.Temporary.Y = this.Up.Z * Particle_Object_Ray.Temporary2.X - this.Up.X * Particle_Object_Ray.Temporary2.Z;
            Particle_Object.Temporary.Z = this.Up.X * Particle_Object_Ray.Temporary2.Y - this.Up.Y * Particle_Object_Ray.Temporary2.X;
            this.Container.setAbsoluteOrientationVector(Particle_Object.Temporary.X, Particle_Object.Temporary.Y, Particle_Object.Temporary.Z, this.Up.X, this.Up.Y, this.Up.Z);
        }
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Container);
        }
    }
}

