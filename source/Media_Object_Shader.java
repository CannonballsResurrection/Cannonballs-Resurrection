/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTSurfaceShader
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTSurfaceShader;

public class Media_Object_Shader
extends Media_Object
implements Global,
WTConstants {
    WTBitmap Image;
    UberImage RawImage;
    UberImage MaskImage;
    WTSurfaceShader Shader;
    int Width = 0;
    int Height = 0;
    int HalfWidth = 0;
    int HalfHeight = 0;
    boolean is24bit = false;
    boolean isBlank = false;
    boolean isJPEG = false;
    boolean HasMask = false;
    boolean ShaderCreated = false;
    int R = 0;
    int G = 0;
    int B = 0;
    boolean ColorKey = false;
    boolean Additive = false;
    boolean isStatic = true;
    float BaseR = 1.0f;
    float BaseG = 1.0f;
    float BaseB = 1.0f;

    void noFilter() {
        this.Shader.setTextureFiltering(0, false);
    }

    void makeStatic() {
        if (!this.isStatic) {
            this.isStatic = true;
            if (this.RawImage != null) {
                this.RawImage.MakeStatic();
                this.RawImage.bitmap = null;
                this.RawImage.destroy();
                this.RawImage = null;
            }
            if (this.MaskImage != null) {
                this.MaskImage.MakeStatic();
                this.MaskImage.bitmap = null;
                this.MaskImage.destroy();
                this.MaskImage = null;
            }
        }
    }

    boolean isStored() {
        return this.RawImage.isStored();
    }

    void addMask(String string, boolean bl) {
        this.HasMask = true;
        this.Loaded = false;
        String string2 = string;
        this.Type = 1;
        String string3 = string2.toLowerCase();
        if (string3.endsWith(".jpg")) {
            string2 = string2.substring(0, string2.length() - 3) + "png";
        }
        this.is24bit = true;
        this.MaskImage = new UberImage(string2, bl);
    }

    public Media_Object_Shader(String string, boolean bl, boolean bl2) {
        this.Additive = bl2;
        String string2 = this.Path = string;
        this.Type = 1;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string3 = string2.toUpperCase();
        if (string3.endsWith(".JPG")) {
            string2 = string2.substring(0, string2.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string2, bl);
    }

    public Media_Object_Shader(String string, boolean bl, int n, int n2, int n3) {
        this.Additive = false;
        String string2 = this.Path = string;
        this.Type = 1;
        this.isStatic = false;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string3 = string2.toUpperCase();
        if (string3.endsWith(".JPG")) {
            string2 = string2.substring(0, string2.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string2, bl, n, n2, n3);
    }

    public Media_Object_Shader(String string, boolean bl, boolean bl2, boolean bl3) {
        this.isStatic = bl3;
        this.Additive = bl2;
        String string2 = this.Path = string;
        this.Type = 1;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string3 = string2.toUpperCase();
        if (string3.endsWith(".JPG")) {
            string2 = string2.substring(0, string2.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string2, bl);
    }

    public Media_Object_Shader(String string, String string2, boolean bl, boolean bl2, boolean bl3) {
        this.isStatic = bl3;
        this.Additive = bl2;
        String string3 = this.Path = string;
        this.Type = 1;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string4 = string3.toLowerCase();
        if (string4.endsWith(".jpg")) {
            string3 = string3.substring(0, string3.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string3, bl);
        this.addMask(string2, bl);
    }

    public Media_Object_Shader(String string, String string2, boolean bl, boolean bl2) {
        this.Additive = bl2;
        String string3 = this.Path = string;
        this.Type = 1;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string4 = string3.toLowerCase();
        if (string4.endsWith(".jpg")) {
            string3 = string3.substring(0, string3.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string3, bl);
        this.addMask(string2, bl);
    }

    public Media_Object_Shader(String string, int n, int n2, int n3, boolean bl) {
        this.ColorKey = true;
        this.R = n;
        this.G = n2;
        this.B = n3;
        String string2 = this.Path = string;
        this.Type = 1;
        this.Shader = Main.MainRef.Wt.createSurfaceShader();
        String string3 = string2.toLowerCase();
        if (string3.endsWith(".jpg")) {
            string2 = string2.substring(0, string2.length() - 3) + "png";
        }
        this.is24bit = true;
        this.RawImage = new UberImage(string2, bl);
    }

    void store() {
        this.RawImage.store();
    }

    void restore() {
        this.RawImage.restore();
    }

    void blur() {
        this.RawImage.blur();
    }

    public void destroy() {
        if (!this.Loaded) {
            return;
        }
        this.Shader = null;
        if (this.RawImage != null) {
            this.RawImage.destroy();
        }
        if (this.MaskImage != null) {
            this.MaskImage.destroy();
        }
        if (this.Image != null) {
            this.Image.destroy();
        }
        this.RawImage = null;
        this.MaskImage = null;
        this.Image = null;
    }

    public boolean isLoaded() {
        if (this.isJPEG) {
            return this.Loaded;
        }
        if (this.Loaded) {
            return true;
        }
        if (this.is24bit) {
            if (this.RawImage != null && !this.ShaderCreated && this.RawImage.isLoaded()) {
                this.ShaderCreated = true;
                this.Image = this.RawImage.bitmap;
                if (this.ColorKey) {
                    this.Image.setColorKey(this.R, this.G, this.B);
                }
                this.Width = this.RawImage.getWidth();
                this.Height = this.RawImage.getHeight();
                this.HalfWidth = this.Width / 2;
                this.HalfHeight = this.Height / 2;
                this.Shader.setNumLayers(1);
                if (this.Additive) {
                    this.Shader.setFrameBufferOperation(5);
                } else if (this.RawImage.img_colortype == 6 || this.ColorKey) {
                    this.Shader.setFrameBufferOperation(1);
                } else {
                    this.Shader.setFrameBufferOperation(1);
                }
                this.Shader.setTexture(0, this.Image);
                this.Shader.setLayerType(0, 3);
                this.Shader.setLayerSource(0, 2);
                if (this.ColorKey) {
                    this.Shader.setTextureNumMipMapLevels(0, 0);
                }
                if (this.isStatic) {
                    this.RawImage.MakeStatic();
                    this.RawImage.bitmap = null;
                    this.RawImage.destroy();
                    this.RawImage = null;
                }
                if (!this.HasMask) {
                    this.Loaded = true;
                    return true;
                }
                return false;
            }
        } else if (this.RawImage != null && !this.ShaderCreated && this.RawImage.isLoaded()) {
            this.ShaderCreated = true;
            this.Image = this.RawImage.bitmap;
            this.Width = this.RawImage.getWidth();
            this.Height = this.RawImage.getHeight();
            this.HalfWidth = this.Width / 2;
            this.HalfHeight = this.Height / 2;
            this.Shader.setNumLayers(1);
            if (this.Additive) {
                this.Shader.setFrameBufferOperation(5);
            } else {
                this.Shader.setFrameBufferOperation(1);
            }
            this.Shader.setTexture(0, this.Image);
            this.Shader.setLayerType(0, 3);
            this.Shader.setLayerSource(0, 2);
            if (this.isStatic) {
                this.RawImage.MakeStatic();
                this.RawImage.bitmap = null;
                this.RawImage.destroy();
                this.RawImage = null;
            }
            if (!this.HasMask) {
                this.Loaded = true;
                return true;
            }
            return false;
        }
        if (this.HasMask && this.ShaderCreated && this.MaskImage != null && this.MaskImage.isLoaded()) {
            this.Shader.setFrameBufferOperation(1);
            this.Image.setTextureOpacityMask(this.MaskImage.bitmap);
            if (this.isStatic) {
                this.MaskImage.MakeStatic();
                this.MaskImage.bitmap = null;
                this.MaskImage.destroy();
                this.MaskImage = null;
            }
            this.Loaded = true;
            return true;
        }
        return false;
    }

    void blendPixel(int n, int n2, float f, float f2, float f3, float f4) {
        this.RawImage.blendPixel(n, n2, f, f2, f3, f4);
    }

    void setFullbright() {
        this.Shader.setGeometryEmissiveOverride(true);
        this.Shader.setGeometryEmissiveColor(255, 255, 255);
        this.Shader.setGeometryDiffuseOverride(true);
        this.Shader.setGeometryDiffuseColor(255, 255, 255, 255);
    }

    public void tint(int n, int n2, int n3) {
        this.RawImage.Tint(n, n2, n3);
    }

    void noDepth() {
        this.Shader.setDepthOption(0, 0.0f);
        this.Shader.setDepthOption(1, 0.0f);
    }

    void putPixel(int n, int n2, float f, float f2, float f3) {
        this.RawImage.putPixel(n, n2, f, f2, f3);
    }

    void getPixel(VEC3D vEC3D, int n, int n2) {
        this.RawImage.getPixel(vEC3D, n, n2);
    }

    void getStoredPixel(VEC3D vEC3D, int n, int n2) {
        this.RawImage.getStoredPixel(vEC3D, n, n2);
    }

    void pushTexture() {
        this.RawImage.pushTexture();
    }
}

