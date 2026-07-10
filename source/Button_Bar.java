/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Button_Bar
extends Button_3D
implements Global {
    Button_3D Next;
    Button_3D Last;
    WTGroup Group1;
    WTGroup Group2;
    WTGroup Group3;
    float ScreenX = 0.0f;
    float ScreenY = 0.0f;
    boolean Visible = false;
    String Keyword = null;
    float centerwidth;

    public Button_Bar(int n, int n2, int n3, int n4, String string) {
        this.Keyword = string;
        if (n < 21) {
            n = 21;
        }
        this.centerwidth = n - 20;
        this.TopLeftX = n2;
        this.TopLeftY = n3 - 12;
        this.BottomRightX = n2 + n;
        this.BottomRightY = n3 + 12;
        float f = n2;
        float f2 = n3;
        this.ScreenX = f - 400.0f;
        this.ScreenY = f2 - 300.0f;
        this.ScreenY *= -1.0f;
        this.ScreenX *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.ScreenY *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.Group1 = Main.MainRef.Wt.createGroup();
        this.Group1.attachSurfaceShader(Main.MainRef.GlobalMedia.TextBar.Shader, 0.027360002f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 32, 32);
        this.Group1.setBitmapTextureRect(1.0f, 0.0f, 0.84375f, 0.375f);
        this.Group1.setOption(0, 10 + n4);
        this.Group3 = Main.MainRef.Wt.createGroup();
        this.Group3.attachSurfaceShader(Main.MainRef.GlobalMedia.TextBar.Shader, this.centerwidth * 0.002736f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 32, 32);
        this.Group3.setBitmapTextureRect(0.3125f, 0.0f, 0.625f, 0.375f);
        this.Group3.setOption(0, 11 + n4);
        this.Group2 = Main.MainRef.Wt.createGroup();
        this.Group2.attachSurfaceShader(Main.MainRef.GlobalMedia.TextBar.Shader, 0.027360002f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 32, 32);
        this.Group2.setBitmapTextureRect(0.84375f, 0.0f, 1.0f, 0.375f);
        this.Group2.setOption(0, 10 + n4);
    }

    public Button_Bar(Media_Object_Shader media_Object_Shader, int n, int n2, int n3, int n4, int n5, String string) {
        this.Keyword = string;
        if (n < 21) {
            n = 21;
        }
        this.centerwidth = n - 20;
        float f = n3;
        float f2 = n4;
        this.ScreenX = f - 400.0f;
        this.ScreenY = f2 - 300.0f;
        this.ScreenY *= -1.0f;
        this.ScreenX *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.ScreenY *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.Group1 = Main.MainRef.Wt.createGroup();
        this.Group1.attachSurfaceShader(media_Object_Shader.Shader, 0.027360002f * Main.MainRef.camera.FOVFactor, (float)n2 * 0.002736f * Main.MainRef.camera.FOVFactor, media_Object_Shader.Width / 2, media_Object_Shader.Height / 2);
        this.Group1.setBitmapTextureRect(0.0f, 0.0f, 10.0f * (1.0f / (float)media_Object_Shader.Width), (float)n2 * (1.0f / (float)media_Object_Shader.Height));
        this.Group1.setOption(0, n5);
        this.Group3 = Main.MainRef.Wt.createGroup();
        this.Group3.attachSurfaceShader(media_Object_Shader.Shader, this.centerwidth * 0.002736f * Main.MainRef.camera.FOVFactor, (float)n2 * 0.002736f * Main.MainRef.camera.FOVFactor, media_Object_Shader.Width / 2, media_Object_Shader.Height / 2);
        this.Group3.setBitmapTextureRect(20.0f * (1.0f / (float)media_Object_Shader.Width), 0.0f, 40.0f * (1.0f / (float)media_Object_Shader.Width), (float)n2 * (1.0f / (float)media_Object_Shader.Height));
        this.Group3.setOption(0, n5);
        this.Group2 = Main.MainRef.Wt.createGroup();
        this.Group2.attachSurfaceShader(media_Object_Shader.Shader, 0.027360002f * Main.MainRef.camera.FOVFactor, (float)n2 * 0.002736f * Main.MainRef.camera.FOVFactor, media_Object_Shader.Width / 2, media_Object_Shader.Height / 2);
        this.Group2.setBitmapTextureRect((float)(media_Object_Shader.Width - 10) * (1.0f / (float)media_Object_Shader.Width), 0.0f, (float)media_Object_Shader.Width * (1.0f / (float)media_Object_Shader.Width), (float)n2 * (1.0f / (float)media_Object_Shader.Height));
        this.Group2.setOption(0, n5);
    }

    public void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group1);
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group2);
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group3);
            this.Visible = false;
        }
    }

    public void destroy() {
        this.hide();
        this.Group1.detach();
        this.Group2.detach();
        this.Group3.detach();
        this.Group1 = null;
        this.Group2 = null;
        this.Group3 = null;
    }

    String checkBounds(int n, int n2, int n3) {
        if (n > this.TopLeftX && n < this.BottomRightX && n2 > this.TopLeftY && n2 < this.BottomRightY && (n3 & 1) == 1) {
            return this.Keyword;
        }
        return null;
    }

    public void show() {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group1);
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group2);
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group3);
            this.Group1.setPosition(this.ScreenX + 0.013680001f, this.ScreenY, 2.0f);
            this.Group3.setPosition(this.ScreenX + (10.0f + this.centerwidth * 0.5f) * 0.002736f, this.ScreenY, 2.0f);
            this.Group2.setPosition(this.ScreenX + (15.0f + this.centerwidth) * 0.002736f, this.ScreenY, 2.0f);
            this.Visible = true;
        }
    }

    public void show(float f, float f2) {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group1);
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group2);
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group3);
            this.Visible = true;
        }
        this.Group1.setPosition(f + (-5.0f - this.centerwidth * 0.5f) * 0.002736f, f2, 2.0f);
        this.Group3.setPosition(f, f2, 2.0f);
        this.Group2.setPosition(f + (5.0f + this.centerwidth * 0.5f) * 0.002736f, f2, 2.0f);
    }
}

