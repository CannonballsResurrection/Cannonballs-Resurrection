/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Message_3D
implements Global {
    int LetterCount = 0;
    WTGroup Group;
    private Letter_Object[] Letters;
    float FontSize = 24.0f;
    boolean Visible = false;
    int Length = 0;
    int PixelWidth = 0;
    int Opacity = 255;

    public Message_3D(String string, int n, float f) {
        this.create(string, n, f, 0, 0);
    }

    public Message_3D(String string, int n, float f, int n2) {
        this.create(string, n, f, n2, 0);
    }

    public Message_3D(String string, int n, float f, int n2, int n3) {
        this.create(string, n, f, n2, n3);
    }

    void create(String string, int n, float f, int n2, int n3) {
        int n4;
        boolean bl = true;
        this.Group = Main.MainRef.Wt.createGroup();
        int n5 = 0;
        int n6 = string.length();
        this.Letters = new Letter_Object[n6];
        this.Length = n6;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        boolean bl2 = false;
        float f5 = 0.0f;
        n5 = 0;
        while (n5 < n6) {
            n4 = string.charAt(n5);
            if ((n4 = (char)(n4 - 32)) < 0) {
                n4 = 0;
            }
            if (n4 > 95) {
                n4 = 0;
            }
            if (n4 != 126) {
                f4 += Text.CharacterWidthTrebuchet[n4] * 0.75f;
            }
            ++n5;
        }
        this.PixelWidth = Math.round(f4 * f);
        if (n == 1) {
            f2 -= f4 / 2.0f * 0.002736f;
        } else if (n == 2) {
            f2 -= f4 * 0.002736f;
        }
        int n7 = 0;
        int n8 = 0;
        f5 = f2;
        n5 = 0;
        while (n5 < n6) {
            n4 = string.charAt(n5);
            if (n4 == 13) {
                f2 = f5;
                f3 -= 28.0f * f * 0.002736f * Main.MainRef.camera.FOVFactor;
            } else if (n4 == 96) {
                bl = !bl;
            } else {
                if ((n4 = (char)(n4 - 32)) < 0) {
                    n4 = 0;
                }
                if (n4 > 95) {
                    n4 = 0;
                }
                n7 = n4;
                n8 = (int)Math.floor(n7 / 10);
                n7 -= n8 * 10;
                this.Letters[this.LetterCount] = Main.MainRef.Letters.getNext();
                this.Letters[this.LetterCount].Group.setAbsoluteScale(1.0f);
                this.Letters[this.LetterCount].Group.detach();
                if (bl) {
                    switch (n3) {
                        case 0: {
                            this.Letters[this.LetterCount].Group.attachSurfaceShader(Main.MainRef.TextManager.Trebuchet.Shader, Text.CharacterWidthTrebuchet[n4] * 0.002736f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 128, 128);
                            break;
                        }
                        case 1: {
                            this.Letters[this.LetterCount].Group.attachSurfaceShader(Main.MainRef.TextManager.BlueTrebuchet.Shader, Text.CharacterWidthTrebuchet[n4] * 0.002736f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 128, 128);
                            break;
                        }
                        case 2: {
                            this.Letters[this.LetterCount].Group.attachSurfaceShader(Main.MainRef.TextManager.GrayTrebuchet.Shader, Text.CharacterWidthTrebuchet[n4] * 0.002736f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 128, 128);
                        }
                    }
                    if (bl2) {
                        this.Letters[this.LetterCount].Group.setOption(0, 11 + n2 + n3 * 2);
                    } else {
                        this.Letters[this.LetterCount].Group.setOption(0, 12 + n2 + n3 * 2);
                    }
                } else {
                    this.Letters[this.LetterCount].Group.attachSurfaceShader(Main.MainRef.TextManager.BlueTrebuchet.Shader, Text.CharacterWidthTrebuchet[n4] * 0.002736f * Main.MainRef.camera.FOVFactor, 0.065664f * Main.MainRef.camera.FOVFactor, 128, 128);
                    if (bl2) {
                        this.Letters[this.LetterCount].Group.setOption(0, 11 + n2 + 2);
                    } else {
                        this.Letters[this.LetterCount].Group.setOption(0, 12 + n2 + 2);
                    }
                }
                this.Letters[this.LetterCount].Group.setBitmapTextureRect((float)n7 * this.FontSize * 0.00390625f, (float)n8 * this.FontSize * 0.00390625f, ((float)n7 * this.FontSize + Text.CharacterWidthTrebuchet[n4]) * 0.00390625f, ((float)n8 * this.FontSize + this.FontSize) * 0.00390625f);
                bl2 = !bl2;
                this.Group.addObject((WTContainer)this.Letters[this.LetterCount].Group);
                this.Letters[this.LetterCount].Group.setPosition(f2, f3, 0.0f);
                ++this.LetterCount;
                f2 += Text.CharacterWidthTrebuchet[n4] * 0.75f * 0.002736f;
            }
            ++n5;
        }
        this.Group.setAbsoluteScale(f);
    }

    void hide() {
        if (this.Visible) {
            Main.MainRef.camera.Camera.removeObject((WTContainer)this.Group);
            this.Visible = false;
        }
    }

    void destroy() {
        this.hide();
        int n = 0;
        while (n < this.LetterCount) {
            if (this.Opacity != 255) {
                this.Letters[n].Group.setBitmapOpacity(255);
            }
            this.Group.removeObject((WTContainer)this.Letters[n].Group);
            Main.MainRef.Letters.add(this.Letters[n]);
            this.Letters[n] = null;
            ++n;
        }
        this.Letters = null;
        this.Group = null;
    }

    void setOpacity(int n) {
        if (this.Opacity != n) {
            int n2 = 0;
            while (n2 < this.LetterCount) {
                this.Letters[n2].Group.setBitmapOpacity(n);
                ++n2;
            }
        }
        this.Opacity = n;
    }

    int getPixelWidth() {
        return this.PixelWidth;
    }

    public void show(float f, float f2) {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Visible = true;
        }
        float f3 = f - 400.0f;
        float f4 = f2 - 300.0f;
        f4 *= -1.0f;
        this.Group.setPosition(f3 *= 0.002736f * Main.MainRef.camera.FOVFactor, f4 *= 0.002736f * Main.MainRef.camera.FOVFactor, 2.0f);
    }

    public void showInUnits(float f, float f2) {
        if (!this.Visible) {
            Main.MainRef.camera.Camera.addObject((WTContainer)this.Group);
            this.Visible = true;
        }
        this.Group.setPosition(f, f2, 2.0f);
    }
}

