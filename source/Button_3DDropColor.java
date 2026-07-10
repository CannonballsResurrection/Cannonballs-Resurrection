/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;

public class Button_3DDropColor
extends Button_3D {
    int[] ColorValues;
    int Colors;
    WTBitmap[] ItemsColor;
    WTGroup[] Items;
    WTBitmap SelectedColor;
    WTGroup Selected;
    String Keyword = "";
    Message_3D Title;
    WTGroup Rollout;
    WTModel RolloutModel;
    WTGroup RollOver;
    WTModel RollOverModel;
    int CurrentRollover = -1;
    int SelectedItem = 0;
    boolean Unrolled = false;
    boolean RolloverVisible = false;

    void unroll() {
        if (!this.Unrolled && !Main.MainRef.RolledOut) {
            Main.MainRef.RolledOut = true;
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Rollout);
            this.Rollout.setPosition(this.ScreenX, this.ScreenY - 0.002736f * ((float)this.Colors * 16.0f + 15.0f) * Main.MainRef.camera.FOVFactor, 2.0f);
            int n = 0;
            while (n < this.Colors) {
                Main.MainRef.camera.Camera.addObject((WTContainer)this.Items[n]);
                this.Items[n].setPosition(this.ScreenX - 0.295488f, this.ScreenY - (float)(32 * n + 35) * 0.002736f, 2.0f);
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

    public Button_3DDropColor(int n, int n2, int n3, int[] nArray, String string) {
        this.Keyword = string;
        this.Colors = n3;
        this.ColorValues = nArray;
        this.Items = new WTGroup[this.Colors];
        this.ItemsColor = new WTBitmap[this.Colors];
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
        this.Group.setOption(0, 11);
        this.setState();
        this.RolloutModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.087552f * (float)this.Colors * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RolloutModel.setSurfaceShader(Main.MainRef.GlobalMedia.Rollout.Shader);
        this.RolloutModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.Rollout = Main.MainRef.Wt.createGroup();
        this.RolloutModel.setTextureRect("front", 0.0f, 0.0f, 1.0f, (float)this.Colors);
        this.Rollout.attach((WTObject)this.RolloutModel);
        this.Rollout.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.Rollout.setOption(0, 13);
        this.RollOverModel = Main.MainRef.Wt.createPlane(0.694944f * Main.MainRef.camera.FOVFactor, 0.093024f * Main.MainRef.camera.FOVFactor, false, 0.0f, 0.0f, 1);
        this.RollOverModel.setSurfaceShader(Main.MainRef.GlobalMedia.Buttons.Shader);
        this.RollOverModel.setMaterial(255, 255, 255, 0.0f, 255, 255, 255);
        this.RollOverModel.setTextureRect("front", 0.0f, 0.5f, 1.0f, 0.96875f);
        this.RollOver = Main.MainRef.Wt.createGroup();
        this.RollOver.attach((WTObject)this.RollOverModel);
        this.RollOver.setOrientation(0.0f, 1.0f, 0.0f, 180.0f);
        this.RollOver.setOption(0, 14);
        int n4 = 0;
        while (n4 < this.Colors) {
            this.ItemsColor[n4] = Main.MainRef.Wt.createBlankBitmap(32, 32);
            this.createColor(this.ItemsColor[n4], this.ColorValues[n4 * 3], this.ColorValues[n4 * 3 + 1], this.ColorValues[n4 * 3 + 2]);
            this.Items[n4] = Main.MainRef.Wt.createGroup();
            this.Items[n4].attachBitmap((WTObject)this.ItemsColor[n4], 0.065664f, 0.049248002f, 16, 16);
            this.Items[n4].setBitmapTextureRect(0.0f, 0.0f, 0.71875f, 0.53125f);
            this.Items[n4].setOption(0, 15);
            ++n4;
        }
        this.SelectedColor = Main.MainRef.Wt.createBlankBitmap(32, 32);
        this.createColor(this.SelectedColor, this.ColorValues[this.SelectedItem * 3], this.ColorValues[this.SelectedItem * 3 + 1], this.ColorValues[this.SelectedItem * 3 + 2]);
        this.Selected = Main.MainRef.Wt.createGroup();
        this.Selected.attachBitmap((WTObject)this.SelectedColor, 0.065664f, 0.049248002f, 16, 24);
        this.Selected.setBitmapTextureRect(0.0f, 0.0f, 0.71875f, 0.53125f);
        this.Selected.setOption(0, 15);
        this.Title = new Message_3D(this.Keyword, 0, 1.0f, 2);
    }

    public void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Selected);
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
        this.Selected.detach();
        this.SelectedColor.destroy();
        this.Selected = null;
        this.SelectedColor = null;
        this.RollOver.detach();
        this.RollOverModel.removeTexture();
        this.RollOver = null;
        this.RollOverModel = null;
        this.Rollout.detach();
        this.RolloutModel.removeTexture();
        this.RolloutModel = null;
        this.Rollout = null;
        int n = 0;
        while (n < this.Colors) {
            this.Items[n].detach();
            this.ItemsColor[n].destroy();
            this.ItemsColor[n] = null;
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
            while (n < this.Colors) {
                Main.MainRef.camera.Camera.removeObject((WTContainer)this.Items[n]);
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
        } else if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY && n2 <= this.BottomRightY + this.Colors * 32) {
            boolean bl = false;
            int n4 = 0;
            while (n4 < this.Colors) {
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
                while (n5 < this.Colors) {
                    if (n >= this.TopLeftX && n <= this.BottomRightX && n2 >= this.TopLeftY + 32 + n5 * 32 && n2 <= this.BottomRightY + 32 + n5 * 32) {
                        n4 = n5;
                    }
                    ++n5;
                }
                if (n4 > -1) {
                    Main.MainRef.GlobalMedia.Sound_Click.play(false, 127);
                    this.SelectedItem = n4;
                    this.createColor(this.SelectedColor, this.ColorValues[this.SelectedItem * 3], this.ColorValues[this.SelectedItem * 3 + 1], this.ColorValues[this.SelectedItem * 3 + 2]);
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

    void createColor(WTBitmap wTBitmap, int n, int n2, int n3) {
        wTBitmap.setColor(0, 0, 0);
        wTBitmap.setDrawColor(n, n2, n3);
        wTBitmap.drawFillRect(2, 2, 20, 14);
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
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Selected);
            this.Title.show(this.TopLeftX + 50, this.TopLeftY + 16);
            this.Group.setPosition(this.ScreenX, this.ScreenY, 2.0f);
            this.Selected.setPosition(this.ScreenX - 0.295488f, this.ScreenY - 0.013680001f, 2.0f);
            this.Visible = true;
        }
    }

    void setSelection(int n) {
        if (n != this.SelectedItem) {
            this.SelectedItem = n;
            this.createColor(this.SelectedColor, this.ColorValues[this.SelectedItem * 3], this.ColorValues[this.SelectedItem * 3 + 1], this.ColorValues[this.SelectedItem * 3 + 2]);
            this.reroll();
        }
    }
}

