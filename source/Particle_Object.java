/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Particle_Object {
    Particle_Object Next = null;
    Particle_Object Last = null;
    Particle_Object Next2;
    Particle_Object Last2;
    WTGroup Container;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    float TrajectoryX = 0.0f;
    float TrajectoryY = 0.0f;
    float TrajectoryZ = 0.0f;
    float Opacity = 255.0f;
    boolean FromStore = false;
    boolean Visible = false;
    static VEC3D Temporary = new VEC3D();
    private static VEC3D Screen = new VEC3D();
    boolean Active = false;

    void hide() {
        if (this.Visible) {
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Container);
            this.Visible = false;
        }
    }

    public void destroy() {
        this.hide();
        this.Container.detach();
        this.Container = null;
    }

    public void updateTimeSlice(float f) {
    }

    public void activate(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
    }

    boolean isOnscreen(float f, float f2, float f3) {
        Main.MainRef.wt_stage.worldToScreen(Screen, f, f2, f3);
        if (Particle_Object.Screen.Z < 0.0f) {
            return false;
        }
        return (double)Particle_Object.Screen.X > -0.6 && (double)Particle_Object.Screen.X < 0.6 && (double)Particle_Object.Screen.Y > -0.5 && (double)Particle_Object.Screen.Y < 0.5;
    }

    void show() {
        if (!this.Visible) {
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Container);
            this.Container.setPosition(this.X, this.Y, this.Z);
            this.Visible = true;
        }
    }
}

