/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTCollisionInfo
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTVector3D
 */
import wildtangent.webdriver.WTCollisionInfo;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTVector3D;

public class Entity_Object_LensFlare
extends Particle_Object
implements Global {
    public static final VEC3D ScreenPosition = new VEC3D();
    WTVector3D TempVector;
    WTGroup Group;
    WTGroup[] Flare;
    WTCollisionInfo Collision;
    VEC3D Temp = new VEC3D();
    VEC3D Temp2 = new VEC3D();
    VEC3D LastCamera = new VEC3D();
    boolean Visible = false;
    boolean FlareVisible = false;
    int LastOpacity = -1;
    boolean LastOcclusion = false;
    boolean FlareCreated = false;
    boolean TotallyHidden = true;
    float OcclusionFading = 0.0f;

    public Entity_Object_LensFlare(float f, float f2, float f3) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.Group = Main.MainRef.Wt.createGroup();
        this.createFlare();
        this.Temp.fill(this.X, this.Y, this.Z);
        this.Temp.normalize();
        this.Temp.multiply(-1.0f);
        Main.MainRef.wt_stage.Directional.setOrientationVector(this.Temp.X, this.Temp.Y, this.Temp.Z, -this.Temp.Z, this.Temp.Y, this.Temp.X);
    }

    public void hide() {
        if (this.Visible) {
            this.Visible = false;
        }
    }

    void createFlare() {
        this.Group.setOption(0, 1);
        this.Flare = new WTGroup[6];
        this.Flare[0] = Main.MainRef.Wt.createGroup();
        this.Flare[0].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens1.Shader, 0.21888001f, 0.21888001f, Main.MainRef.GlobalMedia.Lens1.Width / 2, Main.MainRef.GlobalMedia.Lens1.Width / 2);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Flare[0]);
        this.Flare[0].setOption(0, -10);
        this.Flare[1] = Main.MainRef.Wt.createGroup();
        this.Flare[1].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens2.Shader, 1.368f, 1.368f, Main.MainRef.GlobalMedia.Lens2.Width / 2, Main.MainRef.GlobalMedia.Lens2.Width / 2);
        this.Group.addObject((WTContainer)this.Flare[1]);
        this.Flare[1].setOption(0, 22);
        this.Flare[2] = Main.MainRef.Wt.createGroup();
        this.Flare[2].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens3.Shader, 0.38304f, 0.38304f, Main.MainRef.GlobalMedia.Lens3.Width / 2, Main.MainRef.GlobalMedia.Lens3.Width / 2);
        this.Group.addObject((WTContainer)this.Flare[2]);
        this.Flare[2].setOption(0, 23);
        this.Flare[3] = Main.MainRef.Wt.createGroup();
        this.Flare[3].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens4.Shader, 1.368f, 1.368f, Main.MainRef.GlobalMedia.Lens4.Width / 2, Main.MainRef.GlobalMedia.Lens4.Width / 2);
        this.Group.addObject((WTContainer)this.Flare[3]);
        this.Flare[3].setOption(0, 24);
        this.Flare[4] = Main.MainRef.Wt.createGroup();
        this.Flare[4].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens5.Shader, 0.0684f, 0.0684f, Main.MainRef.GlobalMedia.Lens5.Width / 2, Main.MainRef.GlobalMedia.Lens5.Width / 2);
        this.Group.addObject((WTContainer)this.Flare[4]);
        this.Flare[4].setOption(0, 25);
        this.Flare[5] = Main.MainRef.Wt.createGroup();
        this.Flare[5].attachSurfaceShader(Main.MainRef.GlobalMedia.Lens6.Shader, 0.08208001f, 0.08208001f, Main.MainRef.GlobalMedia.Lens6.Width / 2, Main.MainRef.GlobalMedia.Lens6.Width / 2);
        this.Group.addObject((WTContainer)this.Flare[5]);
        this.Flare[5].setOption(0, 26);
        this.FlareCreated = true;
    }

    public void destroy() {
        this.Totallyhide();
        Main.MainRef.camera.Camera.removeObject((WTContainer)this.Flare[0]);
        this.Group.removeObject((WTContainer)this.Flare[1]);
        this.Group.removeObject((WTContainer)this.Flare[2]);
        this.Group.removeObject((WTContainer)this.Flare[3]);
        this.Group.removeObject((WTContainer)this.Flare[4]);
        this.Group.removeObject((WTContainer)this.Flare[5]);
        this.Flare[0].detach();
        this.Flare[1].detach();
        this.Flare[2].detach();
        this.Flare[3].detach();
        this.Flare[4].detach();
        this.Flare[5].detach();
        this.Flare = null;
    }

    public void updateTimeSlice(float f) {
        f *= 4.0f;
        if (this.Visible) {
            if (this.OcclusionFading < 1.0f) {
                this.OcclusionFading += f;
                if (this.OcclusionFading > 1.0f) {
                    this.OcclusionFading = 1.0f;
                }
            }
        } else if (!this.TotallyHidden && this.OcclusionFading > 0.0f) {
            this.OcclusionFading -= f;
            if (this.OcclusionFading < 0.0f) {
                this.OcclusionFading = 0.0f;
                this.Totallyhide();
            }
        }
        Main.MainRef.wt_stage.worldToScreen(ScreenPosition, this.X + Main.MainRef.camera.X, this.Y + Main.MainRef.camera.Y, this.Z + Main.MainRef.camera.Z);
        int n = 0;
        boolean bl = false;
        if (Entity_Object_LensFlare.ScreenPosition.Z < 0.0f) {
            this.OcclusionFading = 0.0f;
            this.Totallyhide();
        }
        ScreenPosition.multiply(2.0f);
        if (!((double)Entity_Object_LensFlare.ScreenPosition.X > -1.2 && (double)Entity_Object_LensFlare.ScreenPosition.X < 1.2 && Entity_Object_LensFlare.ScreenPosition.Y > -1.0f && Entity_Object_LensFlare.ScreenPosition.Y < 1.0f && Entity_Object_LensFlare.ScreenPosition.Z > 0.0f)) {
            bl = true;
        }
        if (!bl) {
            if (this.LastCamera.X == Main.MainRef.camera.X && this.LastCamera.Y == Main.MainRef.camera.Y && this.LastCamera.Z == Main.MainRef.camera.Z) {
                bl = this.LastOcclusion;
            } else {
                this.Temp.fill(this.X + Main.MainRef.camera.X, this.Y + Main.MainRef.camera.Y, this.Z + Main.MainRef.camera.Z);
                this.Collision = null;
                this.Collision = Main.MainRef.camera.CameraView.checkCollision(this.Temp.X, this.Temp.Y, this.Temp.Z, false, 0xFFFFFFF);
                if (this.Collision != null) {
                    bl = true;
                }
                this.LastCamera.fill(Main.MainRef.camera.X, Main.MainRef.camera.Y, Main.MainRef.camera.Z);
            }
            this.LastOcclusion = bl;
        }
        if (bl) {
            this.hide();
        } else {
            this.show();
        }
        if (!this.TotallyHidden) {
            n = (int)Library_Math.distance(0.0f, 0.0f, Entity_Object_LensFlare.ScreenPosition.X * 2.0f, Entity_Object_LensFlare.ScreenPosition.Y * 2.0f);
            if (n < 0) {
                n = 0;
            }
            if (n > 50) {
                n = 50;
            }
            this.Flare[0].setPosition(Entity_Object_LensFlare.ScreenPosition.X, Entity_Object_LensFlare.ScreenPosition.Y, 2.0f);
            this.Flare[1].setPosition(Entity_Object_LensFlare.ScreenPosition.X, Entity_Object_LensFlare.ScreenPosition.Y, 0.0f);
            this.Flare[2].setPosition(Entity_Object_LensFlare.ScreenPosition.X * 0.3f, Entity_Object_LensFlare.ScreenPosition.Y * 0.3f, 0.0f);
            this.Flare[3].setPosition(0.0f, 0.0f, 0.0f);
            this.Flare[4].setPosition(Entity_Object_LensFlare.ScreenPosition.X * 0.5f, Entity_Object_LensFlare.ScreenPosition.Y * 0.5f, 0.0f);
            this.Flare[5].setPosition(Entity_Object_LensFlare.ScreenPosition.X * -0.5f, Entity_Object_LensFlare.ScreenPosition.Y * -0.5f, 0.0f);
            int n2 = Math.round((float)(250 - n * 2) * this.OcclusionFading);
            int n3 = Math.round((float)(200 - n * 4) * this.OcclusionFading);
            this.Flare[1].setBitmapOpacity(n3);
            this.Flare[2].setBitmapOpacity(n3);
            this.Flare[1].setAbsoluteScale(0.5f + this.OcclusionFading * 0.5f);
            this.Flare[2].setAbsoluteScale(0.5f + this.OcclusionFading * 0.5f);
            this.Flare[3].setBitmapOpacity(n3);
            this.Flare[4].setBitmapOpacity(n3);
            this.Flare[5].setBitmapOpacity(n3);
            n2 = (int)((float)(50 - n) * this.OcclusionFading);
            this.LastOpacity = n;
            this.Flare[0].setBitmapOrientation(Entity_Object_LensFlare.ScreenPosition.X * 20.0f);
            return;
        }
        if (Entity_Object_LensFlare.ScreenPosition.Z > 0.0f) {
            this.Flare[0].setPosition(Entity_Object_LensFlare.ScreenPosition.X, Entity_Object_LensFlare.ScreenPosition.Y, 2.0f);
        } else {
            this.Flare[0].setPosition(0.0f, 0.0f, 0.0f);
        }
        this.Flare[0].setBitmapOrientation(Entity_Object_LensFlare.ScreenPosition.X * 20.0f);
    }

    public void show() {
        if (!this.Visible) {
            this.Visible = true;
            if (this.TotallyHidden) {
                Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
                this.Group.setPosition(0.0f, 0.0f, 2.0f);
                this.TotallyHidden = false;
                this.OcclusionFading = 0.0f;
            }
        }
    }

    public void Totallyhide() {
        if (!this.TotallyHidden) {
            this.TotallyHidden = true;
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
        }
    }
}

