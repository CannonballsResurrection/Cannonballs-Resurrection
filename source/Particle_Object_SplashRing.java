/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTObject;

public class Particle_Object_SplashRing
extends Particle_Object
implements Global {
    Particle_Plane Plane;
    int UpdateTimer = 0;
    int LastFrame = 0;
    float Opacity = 255.0f;
    float Rate = 0.0f;
    float Scale = 1.0f;

    public Particle_Object_SplashRing(float f, float f2, float f3, float f4, float f5) {
        this.Plane = Main.MainRef.ParticlePlaneList.reservePlane();
        this.X = f;
        this.Y = f2 + 0.25f;
        this.Z = f3;
        this.Rate = f5;
        this.Scale = f4;
        this.Plane.create(1.0f, 1.0f, 14);
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attach((WTObject)this.Plane.Plane);
        this.Container.setAbsoluteScale(this.Scale, this.Scale, 1.0f);
        this.Container.setAbsoluteOrientation(1.0f, 0.0f, 0.0f, -90.0f);
        Main.MainRef.island.WaterGroup.addObject((WTContainer)this.Container);
        this.Container.setPosition(this.X, this.Y, this.Z);
        this.Container.setOption(0, 2);
        this.Opacity = 255.0f;
        this.Plane.Plane.setOpacity(255);
    }

    public void destroy() {
        Main.MainRef.island.WaterGroup.removeObject((WTContainer)this.Container);
        this.Container.detach();
        this.Container = null;
        this.Plane = null;
    }

    public void updateTimeSlice(float f) {
        this.Opacity -= f * 70.0f;
        this.Scale += this.Rate * (f * 2.0f);
        if (this.Opacity < 0.0f) {
            Main.MainRef.ParticleList.remove(this);
            return;
        }
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            this.Plane.Plane.setOpacity((int)this.Opacity);
            this.Container.setAbsoluteScale(this.Scale, this.Scale, 1.0f);
        }
    }
}

