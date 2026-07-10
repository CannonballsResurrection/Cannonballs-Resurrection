/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTObject;

public class Particle_Object_Shockwave
extends Particle_Object
implements Global {
    Particle_Plane Plane;
    int UpdateTimer = 0;
    int LastFrame = 0;
    float Opacity = 255.0f;
    float Rate = 0.0f;
    float Scale = 1.0f;

    public Particle_Object_Shockwave(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        this.Plane = Main.MainRef.ParticlePlaneList.reservePlane();
        this.X = f;
        this.Y = f2 + 0.25f;
        this.Z = f3;
        this.Rate = f8;
        this.Scale = f7;
        this.Plane.create(1.0f, 1.0f, 16);
        this.Container = Main.MainRef.Wt.createGroup();
        this.Container.attach((WTObject)this.Plane.Plane);
        this.Container.setAbsoluteScale(this.Scale, this.Scale, 1.0f);
        this.Container.setOption(0, 11);
        this.Container.setAbsoluteOrientationVector(f4, f5, f6, -f6, f5, f4);
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
        this.Opacity -= f * 140.0f;
        this.Scale += this.Rate * (f * 2.0f);
        if (this.Opacity < 0.0f) {
            this.hide();
            return;
        }
        this.Plane.Plane.setOpacity((int)this.Opacity);
        this.Container.setAbsoluteScale(this.Scale, this.Scale, 1.0f);
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Container);
        }
    }
}

