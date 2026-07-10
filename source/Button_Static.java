/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Button_Static
extends Button_3D
implements Global {
    Button_3D Next;
    Button_3D Last;
    WTGroup Group;
    int X = 0;
    int Y = 0;
    float ScreenX = 0.0f;
    float ScreenY = 0.0f;
    boolean Visible = false;
    float TargetY = 0.0f;
    float TargetX = 0.0f;
    boolean Large = true;
    boolean Centered = false;
    float UVX;
    float UVY;
    float Width;
    float Height;

    public Button_Static(Media_Object_Shader media_Object_Shader, float f, float f2, int n, int n2, int n3, int n4, int n5) {
        this.X = n3;
        this.Y = n4;
        this.ScreenX = this.X - 400;
        this.ScreenY = this.Y - 300;
        this.ScreenY *= -1.0f;
        this.ScreenX += (float)n / 2.0f;
        this.ScreenY -= (float)n2 / 2.0f;
        this.ScreenX *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.ScreenY *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.Width = (float)n * (1.0f / (float)media_Object_Shader.Width);
        this.Height = (float)n2 * (1.0f / (float)media_Object_Shader.Height);
        this.UVX = f * (1.0f / (float)media_Object_Shader.Width);
        this.UVY = f2 * (1.0f / (float)media_Object_Shader.Height);
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attachSurfaceShader(media_Object_Shader.Shader, (float)n * 0.002736f * Main.MainRef.camera.FOVFactor, (float)n2 * 0.002736f * Main.MainRef.camera.FOVFactor, media_Object_Shader.Width / 2, media_Object_Shader.Height / 2);
        this.Group.setBitmapTextureRect(this.UVX, this.UVY, this.UVX + this.Width, this.UVY + this.Height);
        this.Group.setOption(0, 10 + n5);
        this.show();
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
        return null;
    }

    void setUV(float f, float f2, float f3, float f4) {
        this.Group.setBitmapTextureRect(f, f2, f3, f4);
    }

    public void show() {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Group.setPosition(this.ScreenX, this.ScreenY, 2.0f);
            this.Visible = true;
        }
    }

    public void show(float f, float f2) {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Visible = true;
        }
        this.Group.setPosition(f, f2, 2.0f);
    }
}

