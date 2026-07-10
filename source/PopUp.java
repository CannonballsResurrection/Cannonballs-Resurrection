/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class PopUp
implements Global {
    WTGroup Left;
    WTGroup Right;
    WTGroup Fade;
    Message_3D Message;

    public PopUp(int n, int n2, String string, boolean bl) {
        this.activate(n, n2, string, bl, 60);
    }

    public PopUp(int n, int n2, String string, boolean bl, int n3) {
        this.activate(n, n2, string, bl, n3);
    }

    public PopUp(int n, int n2, int n3, String string, boolean bl, boolean bl2) {
        float f = (float)n3 / 2.0f;
        this.Left = Main.MainRef.Wt.createGroup();
        this.Left.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupLeft.Shader, f * 0.002736f, 0.700416f, 128, 128);
        this.Left.setBitmapTextureRect(0.0f, 0.0f, f * 0.00390625f, 1.0f);
        this.Right = Main.MainRef.Wt.createGroup();
        this.Right.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupRightScroll.Shader, f * 0.002736f, 0.700416f, 128, 128);
        this.Right.setBitmapTextureRect(1.0f - f * 0.00390625f, 0.0f, 1.0f, 1.0f);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Left);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Right);
        this.Left.setOption(0, 52);
        this.Right.setOption(0, 52);
        this.Message = new Message_3D(string, 0, 1.0f, 80);
        float f2 = n - 400;
        float f3 = n2 - 300;
        f3 *= -1.0f;
        f2 *= 0.002736f;
        f3 *= 0.002736f;
        if (bl) {
            this.Message.show(n - 256 + 10, n2 - 128 + 16);
            this.Left.setPosition(f2 - f / 2.0f * 0.002736f, f3, 2.0f);
            this.Right.setPosition(f2 + f / 2.0f * 0.002736f, f3, 2.0f);
            return;
        }
        this.Message.show(n + 10, n2 + 16);
        this.Left.setPosition(f2 + f / 2.0f * 0.002736f, f3 - 0.350208f, 2.0f);
        this.Right.setPosition(f2 + f / 2.0f * 3.0f * 0.002736f, f3 - 0.350208f, 2.0f);
    }

    public PopUp(int n, int n2, int n3, int n4, String string, boolean bl, boolean bl2) {
        float f = (float)n3 / 2.0f;
        this.Left = Main.MainRef.Wt.createGroup();
        this.Left.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupSLeft.Shader, f * 0.002736f, 0.24076802f, 128, 64);
        this.Left.setBitmapTextureRect(0.0f, 0.0f, f * 0.00390625f, 0.6875f);
        this.Right = Main.MainRef.Wt.createGroup();
        this.Right.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupSRightScroll.Shader, f * 0.002736f, 0.24076802f, 128, 64);
        this.Right.setBitmapTextureRect(1.0f - f * 0.00390625f, 0.0f, 1.0f, 0.6875f);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Left);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Right);
        this.Left.setOption(0, 52);
        this.Right.setOption(0, 52);
        this.Message = new Message_3D(string, 0, 1.0f, 80);
        float f2 = n - 400;
        float f3 = n2 - 300;
        f3 *= -1.0f;
        this.Message.show(n + 10, n2 + 16);
        this.Left.setPosition((f2 *= 0.002736f) + f / 2.0f * 0.002736f, (f3 *= 0.002736f) - 0.12038401f, 2.0f);
        this.Right.setPosition(f2 + f / 2.0f * 3.0f * 0.002736f, f3 - 0.12038401f, 2.0f);
    }

    void destroy() {
        Main.MainRef.camera.Camera.removeObject((WTContainer)this.Left);
        Main.MainRef.camera.Camera.removeObject((WTContainer)this.Right);
        this.Left.detach();
        this.Right.detach();
        this.Left = null;
        this.Right = null;
        if (this.Fade != null) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Fade);
            this.Fade.detach();
        }
        this.Fade = null;
        this.Message.destroy();
        this.Message = null;
    }

    void activate(int n, int n2, String string, boolean bl, int n3) {
        this.Left = Main.MainRef.Wt.createGroup();
        this.Left.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupLeft.Shader, 0.700416f, 0.700416f, 128, 128);
        this.Right = Main.MainRef.Wt.createGroup();
        this.Right.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupRight.Shader, 0.700416f, 0.700416f, 128, 128);
        this.Fade = Main.MainRef.Wt.createGroup();
        this.Fade.attachSurfaceShader(Main.MainRef.GlobalMedia.PopupLeft.Shader, 2.21616f, 1.6689601f, 128, 128);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Left);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Right);
        Main.MainRef.camera.Camera.addObject((WTContainer)this.Fade);
        this.Left.setOption(0, n3);
        this.Right.setOption(0, n3);
        this.Fade.setOption(0, n3 - 1);
        this.Fade.setPosition(0.0f, 0.0f, 2.0f);
        this.Left.setBitmapTextureRect(0.0f, 0.0f, 0.99f, 1.0f);
        this.Right.setBitmapTextureRect(0.01f, 0.0f, 1.0f, 1.0f);
        this.Fade.setBitmapTextureRect(0.1f, 0.2f, 0.9f, 0.8f);
        this.Message = new Message_3D(string, 0, 1.0f, 80);
        float f = n - 400;
        float f2 = n2 - 300;
        f2 *= -1.0f;
        f *= 0.002736f;
        f2 *= 0.002736f;
        if (bl) {
            this.Message.show(n - 256 + 10, n2 - 128 + 16);
            this.Left.setPosition(f - 0.350208f, f2, 2.0f);
            this.Right.setPosition(f + 0.350208f, f2, 2.0f);
            return;
        }
        this.Message.show(n + 10, n2 + 16);
        this.Left.setPosition(f + 0.350208f, f2 - 0.350208f, 2.0f);
        this.Right.setPosition(f + 1.050624f, f2 - 0.350208f, 2.0f);
    }
}

