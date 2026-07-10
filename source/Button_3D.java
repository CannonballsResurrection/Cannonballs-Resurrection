/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;

public class Button_3D
implements Global {
    Button_3D Next;
    Button_3D Last;
    WTGroup Group;
    WTModel Model;
    String Keyword = "";
    int X = 0;
    int Y = 0;
    int TopLeftX = 0;
    int TopLeftY = 0;
    int BottomRightX = 0;
    int BottomRightY = 0;
    float ScreenX = 0.0f;
    float ScreenY = 0.0f;
    boolean Over = false;
    boolean OverSet = true;
    boolean Visible = false;
    float TargetY = 0.0f;
    float TargetX = 0.0f;
    Message_3D Message;
    boolean Large = true;
    int Centered = 0;
    int Type = 0;
    float UVX;
    float UVY;
    float UVX2;
    float UVY2;
    float Width;
    float Height;

    void setState() {
        if (this.Over && !this.OverSet) {
            this.OverSet = true;
            if (this.Type == 0) {
                this.Model.setTextureRect("front", 0.0f, 0.5f, 1.0f, 1.0f);
            } else {
                this.Group.setBitmapTextureRect(this.UVX2, this.UVY2, this.UVX2 + this.Width, this.UVY2 + this.Height);
            }
            Main.MainRef.GlobalMedia.Sound_Over.play(false, 127);
            return;
        }
        if (!this.Over && this.OverSet) {
            this.OverSet = false;
            if (this.Type == 0) {
                this.Model.setTextureRect("front", 0.0f, 0.0f, 1.0f, 0.5f);
                return;
            }
            this.Group.setBitmapTextureRect(this.UVX, this.UVY, this.UVX + this.Width, this.UVY + this.Height);
        }
    }

    public Button_3D() {
    }

    public Button_3D(int n, int n2, String string, String string2, int n3, int n4) {
        this.create(n, n2, string, string2, n3, n4);
    }

    public Button_3D(int n, int n2, String string, String string2, int n3) {
        this.create(n, n2, string, string2, n3, 0);
    }

    void create(int n, int n2, String string, String string2, int n3, int n4) {
        this.Centered = n3;
        this.Keyword = string;
        this.X = n;
        this.Y = n2;
        this.TopLeftX = n - 126;
        this.TopLeftY = n2 - 14;
        this.BottomRightX = n + 126;
        this.BottomRightY = n2 + 14;
        this.ScreenX = this.X - 400;
        this.ScreenY = this.Y - 300;
        this.ScreenY *= -1.0f;
        this.ScreenX *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.ScreenY *= 0.002736f * Main.MainRef.camera.FOVFactor;
        this.Model = Main.MainRef.Wt.createPlane(0.700416f * Main.MainRef.camera.FOVFactor, 0.087552f * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.Model.setSurfaceShader(Main.MainRef.GlobalMedia.Buttons.Shader);
        this.Model.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.setState();
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attach((WTObject)this.Model);
        this.Group.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Message = new Message_3D(string2, this.Centered, 1.0f, n4);
        this.Group.setOption(0, 10 + n4);
    }

    public Button_3D(Media_Object_Shader media_Object_Shader, float f, float f2, int n, int n2, float f3, float f4, int n3, int n4, String string, int n5) {
        this.Type = 1;
        this.Keyword = string;
        this.X = n3;
        this.Y = n4;
        this.TopLeftX = n3;
        this.TopLeftY = n4;
        this.BottomRightX = n3 + n;
        this.BottomRightY = n4 + n2;
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
        this.UVX2 = f3 * (1.0f / (float)media_Object_Shader.Width);
        this.UVY2 = f4 * (1.0f / (float)media_Object_Shader.Height);
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attachSurfaceShader(media_Object_Shader.Shader, (float)n * 0.002736f * Main.MainRef.camera.FOVFactor, (float)n2 * 0.002736f * Main.MainRef.camera.FOVFactor, media_Object_Shader.Width / 2, media_Object_Shader.Height / 2);
        this.setState();
        this.Group.setOption(0, 10 + n5);
    }

    public void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
            if (this.Type == 0) {
                this.Message.hide();
            }
            this.Visible = false;
        }
    }

    public void destroy() {
        this.hide();
        this.Group.detach();
        if (this.Model != null) {
            this.Model.removeTexture();
        }
        this.Model = null;
        this.Group = null;
        if (this.Message != null) {
            this.Message.destroy();
        }
        this.Message = null;
    }

    String checkBounds(int n, int n2, int n3) {
        if (!Main.MainRef.RolledOut && n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY && n2 <= this.BottomRightY) {
            this.Over = true;
            this.setState();
            if ((n3 & 1) == 1) {
                Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
                return this.Keyword;
            }
        } else {
            this.Over = false;
            this.setState();
        }
        return null;
    }

    public void show() {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            if (this.Type == 0) {
                if (this.Centered == 0) {
                    this.Message.show(this.TopLeftX + 10, this.TopLeftY + 16);
                } else if (this.Centered == 2) {
                    this.Message.show(this.TopLeftX + 246, this.TopLeftY + 16);
                } else {
                    this.Message.show(this.TopLeftX + 128, this.TopLeftY + 16);
                }
            }
            this.Group.setPosition(this.ScreenX, this.ScreenY, 2.0f);
            this.Visible = true;
        }
    }
}

