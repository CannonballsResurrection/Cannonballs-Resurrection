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

public class Button_3DDrop
extends Button_3D {
    String Keyword = "";
    String[] TextMessages;
    int KeywordCount;
    Message_3D[] Items;
    Message_3D Title;
    WTGroup Rollout;
    WTModel RolloutModel;
    WTGroup RollOver;
    WTModel RollOverModel;
    int CurrentRollover = -1;
    int SelectedItem = 0;
    boolean Unrolled = false;
    boolean RolloverVisible = false;
    boolean Titled = false;

    void unroll() {
        if (!this.Unrolled && !Main.MainRef.RolledOut) {
            Main.MainRef.RolledOut = true;
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Rollout);
            this.Rollout.setPosition(this.ScreenX, this.ScreenY - 0.002736f * ((float)this.KeywordCount * 16.0f + 15.0f), 2.0f);
            int n = 0;
            while (n < this.KeywordCount) {
                this.Items[n].show(this.TopLeftX + 10, this.TopLeftY + 48 + n * 32);
                ++n;
            }
            this.Unrolled = true;
        }
    }

    void setState() {
        if (this.Over && !this.OverSet) {
            this.Model.setTextureRect("front", 0.0f, 0.5f, 1.0f, 1.0f);
            this.OverSet = true;
            Main.MainRef.GlobalMedia.Sound_Over.play(false, 127);
            return;
        }
        if (!this.Over && this.OverSet) {
            this.OverSet = false;
            this.Model.setTextureRect("front", 0.0f, 0.0f, 1.0f, 0.5f);
        }
    }

    public Button_3DDrop(int n, int n2, int n3, String[] stringArray, String string, int n4) {
        this.SelectedItem = n4;
        this.Keyword = string;
        this.TextMessages = stringArray;
        this.KeywordCount = n3;
        this.Items = new Message_3D[this.KeywordCount];
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
        this.Model.setSurfaceShader(Main.MainRef.GlobalMedia.ButtonsDrop.Shader);
        this.Model.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.setState();
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attach((WTObject)this.Model);
        this.Group.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Group.setOption(0, 31);
        this.setState();
        this.RolloutModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.087552f * (float)this.KeywordCount * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RolloutModel.setSurfaceShader(Main.MainRef.GlobalMedia.Rollout.Shader);
        this.RolloutModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Rollout = Main.MainRef.Wt.createGroup();
        this.RolloutModel.setTextureRect("front", 0.0f, 0.0f, 1.0f, (float)this.KeywordCount);
        this.Rollout.attach((WTObject)this.RolloutModel);
        this.Rollout.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Rollout.setOption(0, 33);
        this.RollOverModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.093024f * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RollOverModel.setSurfaceShader(Main.MainRef.GlobalMedia.Buttons.Shader);
        this.RollOverModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.RollOverModel.setTextureRect("front", 0.0f, 0.5f, 1.0f, 0.96875f);
        this.RollOver = Main.MainRef.Wt.createGroup();
        this.RollOver.attach((WTObject)this.RollOverModel);
        this.RollOver.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.RollOver.setOption(0, 34);
        int n5 = 0;
        while (n5 < this.KeywordCount) {
            this.Items[n5] = new Message_3D(this.TextMessages[n5], 0, 1.0f, 35);
            ++n5;
        }
        this.Title = new Message_3D(this.TextMessages[this.SelectedItem], 0, 1.0f, 32);
    }

    public Button_3DDrop(int n, int n2, int n3, String[] stringArray, String string, int n4, int n5, String string2) {
        this.Titled = true;
        this.SelectedItem = n4;
        this.Keyword = string;
        this.TextMessages = stringArray;
        this.KeywordCount = n3;
        this.Items = new Message_3D[this.KeywordCount];
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
        this.Model.setSurfaceShader(Main.MainRef.GlobalMedia.ButtonsDrop.Shader);
        this.Model.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.setState();
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attach((WTObject)this.Model);
        this.Group.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Group.setOption(0, n5 + 11);
        this.setState();
        this.RolloutModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.087552f * (float)this.KeywordCount * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RolloutModel.setSurfaceShader(Main.MainRef.GlobalMedia.Rollout.Shader);
        this.RolloutModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Rollout = Main.MainRef.Wt.createGroup();
        this.RolloutModel.setTextureRect("front", 0.0f, 0.0f, 1.0f, (float)this.KeywordCount);
        this.Rollout.attach((WTObject)this.RolloutModel);
        this.Rollout.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Rollout.setOption(0, n5 + 13);
        this.RollOverModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.093024f * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RollOverModel.setSurfaceShader(Main.MainRef.GlobalMedia.Buttons.Shader);
        this.RollOverModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.RollOverModel.setTextureRect("front", 0.0f, 0.5f, 1.0f, 0.96875f);
        this.RollOver = Main.MainRef.Wt.createGroup();
        this.RollOver.attach((WTObject)this.RollOverModel);
        this.RollOver.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.RollOver.setOption(0, n5 + 14);
        int n6 = 0;
        while (n6 < this.KeywordCount) {
            this.Items[n6] = new Message_3D(this.TextMessages[n6], 0, 1.0f, n5 + 5);
            ++n6;
        }
        this.Title = new Message_3D(string2, 0, 1.0f, n5 + 2);
    }

    public void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
            this.Title.hide();
            this.Visible = false;
        }
    }

    public void destroy() {
        this.reroll();
        this.hide();
        this.hideRollover();
        this.Group.detach();
        this.Model.removeTexture();
        this.Model = null;
        this.Group = null;
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
            if (!Main.MainRef.RolledOut && n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY && n2 <= this.BottomRightY) {
                this.Over = true;
                this.setState();
                if ((n3 & 1) == 1) {
                    Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
                    this.unroll();
                }
            } else {
                this.Over = false;
                this.setState();
            }
        } else if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY && n2 <= this.BottomRightY) {
            this.hideRollover();
            if ((n3 & 1) == 1) {
                this.reroll();
            }
        } else if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY && n2 <= this.BottomRightY + this.KeywordCount * 32) {
            boolean bl = false;
            int n4 = 0;
            while (n4 < this.KeywordCount) {
                if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY + 32 + n4 * 32 && n2 <= this.BottomRightY + 32 + n4 * 32) {
                    this.showRollover();
                    this.RollOver.setPosition(this.ScreenX, this.ScreenY - 0.002736f * ((float)n4 * 32.0f + 31.0f) * Main.MainRef.camera.FOVFactor, 2.0f);
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
                    this.setSelection(n4);
                    this.reroll();
                    return this.Keyword + this.SelectedItem;
                }
            }
        } else {
            this.hideRollover();
            if ((n3 & 1) == 1) {
                this.reroll();
            }
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
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Title.show(this.TopLeftX + 10, this.TopLeftY + 16);
            this.Group.setPosition(this.ScreenX, this.ScreenY, 2.0f);
            this.Visible = true;
        }
    }

    void setSelection(int n) {
        this.SelectedItem = n;
        if (!this.Titled) {
            this.Title.destroy();
            this.Title = new Message_3D(this.TextMessages[this.SelectedItem], 0, 1.0f, 32);
            this.Title.show(this.TopLeftX + 10, this.TopLeftY + 16);
        }
    }
}

