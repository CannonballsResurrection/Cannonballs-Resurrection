/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Scroll_Bar
extends Button_3D
implements Global {
    String Keyword = "";
    float TopX;
    float TopY;
    float Y;
    float Height;
    float Lines;
    WTGroup Group;
    boolean Over = false;
    boolean OverSet = true;
    boolean Visible = false;
    int TopLeftX = 0;
    int TopLeftY = 0;
    int TopLeftShift = 0;

    void setState() {
        if (this.Over && !this.OverSet) {
            this.Group.setBitmapTextureRect(0.53125f, 0.39583334f, 0.6614584f, 0.5260417f);
            this.OverSet = true;
            return;
        }
        if (!this.Over && this.OverSet) {
            this.OverSet = false;
            this.Group.setBitmapTextureRect(0.53125f, 0.53125f, 0.6614584f, 0.6614584f);
        }
    }

    public Scroll_Bar(int n, int n2, int n3, int n4, int n5) {
        this.TopLeftX = n;
        this.TopLeftY = n2;
        this.TopX = (n += 13) - 400;
        this.TopY = (n2 += 13) - 300;
        this.TopY *= -1.0f;
        this.Y = 0.0f;
        this.Height = n3 -= 26;
        this.Lines = n4;
        this.TopX *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.TopY *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attachSurfaceShader(Main.MainRef.GlobalMedia.Controls.Shader, 0.071136005f * Main.MainRef.camera.FOVFactor, 0.071136005f * Main.MainRef.camera.FOVFactor, 96, 96);
        this.Group.setOption(0, 10 + n5);
        this.setState();
        this.show();
        this.update(0, 0);
    }

    public void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
            this.Visible = false;
        }
    }

    public void destroy() {
        this.hide();
        this.Group.detach();
        this.Group = null;
    }

    String checkBounds(int n, int n2, int n3) {
        if (!Main.MainRef.RolledOut && n >= this.TopLeftX && n <= this.TopLeftX + 26 && n2 >= this.TopLeftY + this.TopLeftShift && n2 <= this.TopLeftY + this.TopLeftShift + 26) {
            this.Over = true;
            this.setState();
            if ((n3 & 1) == 1) {
                // empty if block
            }
        } else {
            this.Over = false;
            this.setState();
        }
        return null;
    }

    void update(int n, int n2) {
        float f;
        if (n < 0) {
            n = 0;
        }
        if ((f = (float)n2 - this.Lines) < 1.0f) {
            f = 1.0f;
        }
        this.Y = this.Height * ((float)n / f);
        this.TopLeftShift = (int)this.Y;
        this.Group.setPosition(this.TopX, this.TopY - this.Y * 0.002736f * Main.MainRef.camera.FOVFactor, 2.0f);
    }

    public void show() {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Group.setPosition(0.0f, 0.0f, 0.0f);
            this.Visible = true;
        }
    }
}

