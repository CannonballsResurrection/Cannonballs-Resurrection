/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTModel
 */
import wildtangent.webdriver.WTModel;

public class Particle_Plane
implements Global {
    Particle_Plane Next;
    Particle_Plane Last;
    WTModel Plane;
    int Type = 2;

    public Particle_Plane() {
        this.Plane = Main.MainRef.Wt.createPlane(1.0f, 1.0f, false, 0.0f, 0.0f, 1);
        this.Plane.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Plane.setSurfaceShader(Main.MainRef.GlobalMedia.SplashRing.Shader);
    }

    void create(float f, float f2, int n) {
        if (this.Type != n) {
            switch (n) {
                case 14: {
                    this.Plane.setSurfaceShader(Main.MainRef.GlobalMedia.SplashRing.Shader);
                    break;
                }
                case 16: {
                    this.Plane.setSurfaceShader(Main.MainRef.GlobalMedia.Shockwave.Shader);
                    break;
                }
                case 17: {
                    this.Plane.setSurfaceShader(Main.MainRef.GlobalMedia.Ray.Shader);
                }
            }
        }
        this.Type = n;
        this.Plane.setAbsoluteScale(f, f2, 1.0f);
    }

    public void destroy() {
        this.Plane.removeTexture();
        this.Plane = null;
    }
}

