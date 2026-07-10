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

public class Button_3DMenu
extends Button_3D {
    String MTitle = "";
    String Keyword = "";
    String[] TextMessages;
    int KeywordCount;
    Message_3D[] Items;
    Message_3D Title;
    WTGroup Rollout;
    WTModel RolloutModel;
    WTGroup RollOver;
    WTModel RollOverModel;
    boolean Unrolled = false;
    boolean RolloverVisible = false;
    float WordWidth = 64.0f;
    int CurrentRollover = -1;

    void unroll() {
        if (!this.Unrolled && !Main.MainRef.RolledOut) {
            Main.MainRef.RolledOut = true;
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Rollout);
            this.Rollout.setPosition(this.ScreenX, this.ScreenY - 0.002736f * ((float)this.KeywordCount * 16.0f + 16.0f) * Main.MainRef.camera.FOVFactor, 2.0f);
            int n = 0;
            while (n < this.KeywordCount) {
                this.Items[n].show(this.TopLeftX + 10, this.TopLeftY + 48 + n * 32);
                ++n;
            }
            this.Unrolled = true;
        }
    }

    public Button_3DMenu(int n, int n2, int n3, String[] stringArray, String string, String string2, int n4) {
        this.MTitle = string;
        this.Keyword = string2;
        this.TextMessages = stringArray;
        this.KeywordCount = n3;
        this.Items = new Message_3D[this.KeywordCount];
        this.X = n += 128;
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
        this.RolloutModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.087552f * (float)this.KeywordCount * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RolloutModel.setSurfaceShader(Main.MainRef.GlobalMedia.Rollout.Shader);
        this.RolloutModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Rollout = Main.MainRef.Wt.createGroup();
        this.Rollout.attach((WTObject)this.RolloutModel);
        this.Rollout.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Rollout.setOption(0, n4 + 13);
        this.RolloutModel.setTextureRect("front", 0.0f, 0.0f, 1.0f, (float)this.KeywordCount);
        this.RollOverModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.093024f * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RollOverModel.setSurfaceShader(Main.MainRef.GlobalMedia.Buttons.Shader);
        this.RollOverModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.RollOverModel.setTextureRect("front", 0.0f, 0.5f, 1.0f, 0.96875f);
        this.RollOver = Main.MainRef.Wt.createGroup();
        this.RollOver.attach((WTObject)this.RollOverModel);
        this.RollOver.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.RollOver.setOption(0, n4 + 14);
        int n5 = 0;
        while (n5 < this.KeywordCount) {
            this.Items[n5] = new Message_3D(this.TextMessages[n5], 0, 1.0f, n4 + 5);
            ++n5;
        }
        this.Title = new Message_3D(this.MTitle, 0, 1.0f, n4 + 12);
        this.WordWidth = this.Title.getPixelWidth();
    }

    public void hide() {
        if (this.Visible) {
            this.Title.hide();
            this.Visible = false;
        }
    }

    public void destroy() {
        this.reroll();
        this.hide();
        this.hideRollover();
        this.Title.destroy();
        this.Title = null;
        this.RollOver.detach();
        this.RollOverModel.removeTexture();
        this.RollOver = null;
        this.RollOverModel = null;
        this.Rollout.detach();
        this.RolloutModel.removeTexture();
        this.RolloutModel = null;
        this.Rollout = null;
        int n = 0;
        while (n < this.KeywordCount) {
            this.Items[n].destroy();
            this.Items[n] = null;
            ++n;
        }
    }

    void reroll() {
        if (this.Unrolled) {
            this.CurrentRollover = -1;
            Main.MainRef.RolledOut = false;
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Rollout);
            int n = 0;
            while (n < this.KeywordCount) {
                this.Items[n].hide();
                ++n;
            }
            this.hideRollover();
            this.Unrolled = false;
        }
    }

    void showRollover() {
        if (!this.RolloverVisible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.RollOver);
            this.RolloverVisible = true;
        }
    }

    String checkBounds(int n, int n2, int n3) {
        if (!this.Visible) {
            return null;
        }
        if (!this.Unrolled) {
            if (!Main.MainRef.RolledOut && n >= this.TopLeftX && (float)n <= (float)this.TopLeftX + this.WordWidth && n2 >= this.TopLeftY && n2 <= this.BottomRightY) {
                this.Over = true;
                this.unroll();
                Main.MainRef.GlobalMedia.Sound_Over.play(false, 127);
            } else {
                this.Over = false;
            }
        } else if (n >= this.TopLeftX && (float)n <= (float)this.TopLeftX + this.WordWidth && n2 >= this.TopLeftY && n2 <= this.BottomRightY + 3 || n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY + 28 && n2 <= this.BottomRightY + this.KeywordCount * 32) {
            boolean bl = false;
            int n4 = 0;
            while (n4 < this.KeywordCount) {
                if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY + 28 + n4 * 32 && n2 <= this.BottomRightY + 28 + n4 * 32) {
                    this.showRollover();
                    this.RollOver.setPosition(this.ScreenX, this.ScreenY - 0.002736f * ((float)n4 * 32.0f + 32.0f) * Main.MainRef.camera.FOVFactor, 2.0f);
                    bl = true;
                    if (n4 != this.CurrentRollover) {
                        Main.MainRef.GlobalMedia.Sound_Over.play(false, 127);
                    }
                    this.CurrentRollover = n4;
                }
                ++n4;
            }
            if (!bl) {
                this.hideRollover();
            }
            if ((n3 & 1) == 1) {
                n4 = -1;
                int n5 = 0;
                while (n5 < this.KeywordCount) {
                    if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY + 32 + n5 * 32 && n2 <= this.BottomRightY + 32 + n5 * 32) {
                        n4 = n5;
                    }
                    ++n5;
                }
                if (n4 > -1) {
                    Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
                    this.reroll();
                    return this.Keyword + n4;
                }
            }
        } else {
            this.hideRollover();
            this.reroll();
        }
        return null;
    }

    void hideRollover() {
        if (this.RolloverVisible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.RollOver);
            this.RolloverVisible = false;
        }
    }

    public void show() {
        if (!this.Visible) {
            this.Title.show(this.TopLeftX + 10, this.TopLeftY + 16);
            this.Visible = true;
        }
    }
}

