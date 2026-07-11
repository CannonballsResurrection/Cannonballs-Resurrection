/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTLight
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTStage
 *  wildtangent.webdriver.WTVector3D
 */
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTLight;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTStage;
import wildtangent.webdriver.WTVector3D;

public class WT_Stage
implements Global,
WTConstants {
    WTStage Stage;
    WTLight Ambient;
    WTLight Directional;
    WTGroup StageGroup;
    WTGroup CollisionGroup;
    WTVector3D TempVector;
    int ScreenWidth = 0;
    int ScreenHeight = 0;
    int FarClip = 6000;

    void hideRealPointer() {
        Main.MainRef.Wt.setMouseCursorState(0);
    }

    public WT_Stage(int n, int n2) {
        this.ScreenWidth = n;
        this.ScreenHeight = n2;
        this.Stage = Main.MainRef.Wt.createStage();
        this.StageGroup = Main.MainRef.Wt.createGroup();
        this.Stage.addObject((WTContainer)this.StageGroup);
        this.Ambient = Main.MainRef.Wt.createLight(0);
        this.StageGroup.addObject((WTContainer)this.Ambient);
        this.Ambient.setColor(60, 60, 60);
        this.CollisionGroup = Main.MainRef.Wt.createGroup();
        this.Stage.addObject((WTContainer)this.CollisionGroup);
        this.CollisionGroup.setCollisionMask(0xFFFFFFF);
        this.Directional = Main.MainRef.Wt.createLight(3);
        this.StageGroup.addObject((WTContainer)this.Directional);
        this.Directional.setColor(255, 255, 255);
        this.StageGroup.setCollisionMask(0);
        Main.MainRef.Wt.focus();
    }

    void worldToScreen(VEC3D vEC3D, float f, float f2, float f3) {
        vEC3D.X = Main.MainRef.camera.Right.X * (f -= Main.MainRef.camera.X) + Main.MainRef.camera.Right.Y * (f2 -= Main.MainRef.camera.Y) + Main.MainRef.camera.Right.Z * (f3 -= Main.MainRef.camera.Z);
        vEC3D.Y = Main.MainRef.camera.Up.X * f + Main.MainRef.camera.Up.Y * f2 + Main.MainRef.camera.Up.Z * f3;
        vEC3D.Z = Main.MainRef.camera.Forward.X * f + Main.MainRef.camera.Forward.Y * f2 + Main.MainRef.camera.Forward.Z * f3;
        if (f == 0.0f && f2 == 0.0f && f3 == 0.0f) {
            vEC3D.X = 0.0f;
            vEC3D.Y = 0.0f;
            vEC3D.Z = -1.0f;
            return;
        }
        float f4 = vEC3D.Z;
        if (f4 < 0.0f) {
            f4 *= -1.0f;
        }
        vEC3D.X /= f4;
        vEC3D.Y /= f4;
    }

    void toggleFullscreen() {
        boolean bl = Main.MainRef.camera.MouseVisible;
        this.showRealPointer();
        if (!Main.MainRef.Wt.getInitStatus(5)) {
            Main.MainRef.camera.hideMouse();
            Main.MainRef.Wt.exec();
            Main.MainRef.Wt.setResolution(this.ScreenWidth, this.ScreenHeight);
            Main.MainRef.Wt.focus();
            Main.MainRef.camera.setViewRect();
            if (bl) {
                Main.MainRef.camera.showMouse();
            }
        } else {
            Main.MainRef.Wt.restoreResolution();
            Main.MainRef.Wt.focus();
        }
        this.hideRealPointer();
    }

    void destroy() {
        this.StageGroup.removeObject((WTContainer)this.Ambient);
        this.Stage = null;
        this.TempVector = null;
    }

    void worldToScreenExtended(VEC3D vEC3D, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, float f14, float f15) {
        f = f13 - f;
        f2 = f14 - f2;
        f3 = f15 - f3;
        vEC3D.X = f10 * f + f11 * f2 + f12 * f3;
        vEC3D.Y = f7 * f + f8 * f2 + f9 * f3;
        vEC3D.Z = f4 * f + f5 * f2 + f6 * f3;
        if (f == 0.0f && f2 == 0.0f && f3 == 0.0f) {
            vEC3D.X = 0.0f;
            vEC3D.Y = 0.0f;
            vEC3D.Z = -1.0f;
        }
    }

    void showRealPointer() {
        Main.MainRef.Wt.setMouseCursorState(1);
    }

    void removeObjectFromParent(WTGroup wTGroup) {
        WTObject wTObject = wTGroup.getOwner();
        if (wTObject == null) {
            return;
        }
        int n = wTObject.getObjectType();
        if (n == 8194) {
            ((WTStage)wTGroup.getOwner()).removeObject((WTContainer)wTGroup);
            return;
        }
        if (n == 54) {
            ((WTGroup)wTGroup.getOwner()).removeObject((WTContainer)wTGroup);
        }
    }
}

