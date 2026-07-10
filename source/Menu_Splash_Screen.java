/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTDrop;

public class Menu_Splash_Screen
implements Global {
    boolean Visible = false;
    boolean ButtonDown = false;
    Media_Object_Actor Logo;
    WTDrop Backdrop;
    float Timer = 0.0f;

    void hide() {
        if (this.Visible) {
            if (this.Logo != null) {
                Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Logo.Model);
                Main.MainRef.MediaList.remove(this.Logo);
                this.Logo = null;
            }
            this.Visible = false;
            System.gc();
        }
    }

    void updateTimeSlice(float f) {
        this.Timer += f / 1000.0f;
        if (this.Timer > 3.0f) {
            Main.MainRef.MenuManager.activateMenu(3);
        }
    }

    boolean isLoaded() {
        if (this.Logo == null) {
            this.Logo = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor("MEDIA/MENUS/WT/intro.wsad"), true);
            return false;
        }
        return this.Logo.isLoaded();
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Logo.Model);
            this.Logo.Model.setPosition(0.0f, 0.0f, 1600.0f);
            this.Logo.Model.playMotion("animation");
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.showMouse();
        }
    }

    void processMouse(int n, int n2, int n3) {
        if (!this.Visible) {
            return;
        }
        if ((n3 & 1) == 1) {
            if (this.ButtonDown) {
                n3 = 0;
                return;
            }
            this.ButtonDown = true;
            return;
        }
        if ((n3 & 1) != 1) {
            this.ButtonDown = false;
            n3 = 0;
        }
    }
}

