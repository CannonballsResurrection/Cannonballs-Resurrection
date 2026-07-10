/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Particle_Object_Cloud
extends Particle_Object
implements Global,
WTConstants {
    private int cloudcount = 30;
    private VEC3D[] Pos = new VEC3D[this.cloudcount];
    private WTGroup[] Groups = new WTGroup[this.cloudcount];
    private float[] Scale = new float[this.cloudcount];
    private int[] ScaleRate = new int[this.cloudcount];
    int Type = 0;

    public Particle_Object_Cloud(int n, float f, float f2, float f3, float f4, float f5, float f6) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.Type = n;
        int n2 = 0;
        while (n2 < this.cloudcount) {
            float f7;
            this.Groups[n2] = Main.MainRef.Wt.createGroup();
            this.Pos[n2] = new VEC3D();
            this.Groups[n2].attachSurfaceShader(Main.MainRef.GlobalMedia.Smoke_Alpha.Shader, 1.0f, 1.0f, Main.MainRef.GlobalMedia.Smoke_Alpha.Width / 2, Main.MainRef.GlobalMedia.Smoke_Alpha.Height / 2);
            this.Groups[n2].setOption(0, 5);
            int n3 = (int)Math.floor(Main.MainRef.random.nextFloat() * 8.0f);
            if (n3 > 3) {
                this.setFrame(this.Groups[n2], n3 - 4, true);
            } else {
                this.setFrame(this.Groups[n2], n3, false);
            }
            this.Scale[n2] = f7 = Main.MainRef.random.nextFloat() * 20.0f + 10.0f;
            this.Groups[n2].setBitmapSize(f7, f7);
            this.ScaleRate[n2] = (int)Math.floor(Main.MainRef.random.nextFloat() * 6.0f);
            this.Pos[n2].fill(Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f);
            this.Pos[n2].normalize();
            this.Pos[n2].multiply(f4 * (0.8f + Main.MainRef.random.nextFloat() * 0.2f));
            this.Pos[n2].X *= f5;
            this.Pos[n2].Y *= f6;
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Groups[n2]);
            this.Groups[n2].setPosition(f + this.Pos[n2].X, f2 + this.Pos[n2].Y, f3 + this.Pos[n2].Z);
            ++n2;
        }
    }

    public void destroy() {
        int n = 0;
        while (n < this.cloudcount) {
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Groups[n]);
            this.Groups[n].detach();
            this.Groups[n] = null;
            this.Pos[n] = null;
            ++n;
        }
    }

    public void updateTimeSlice(float f) {
        int n = 0;
        while (n < this.cloudcount) {
            if (this.isOnscreen(this.X + this.Pos[n].X, this.Y + this.Pos[n].Y, this.Z + this.Pos[n].Z)) {
                this.Groups[n].setBitmapSize(this.Scale[n] + Main.MainRef.SinTable[this.ScaleRate[n]], this.Scale[n] + Main.MainRef.SinTable[this.ScaleRate[n]]);
            }
            ++n;
        }
    }

    void setFrame(WTGroup wTGroup, int n, boolean bl) {
        int n2 = (int)Math.ceil(n / 2);
        int n3 = n - (n2 - 1) * 2;
        if (this.Type == 0) {
            n3 += 2;
        }
        if (bl) {
            wTGroup.setBitmapTextureRect(0.25f * (float)(n3 + 1), 0.5f * (float)n2, 0.25f * (float)n3, 0.5f * (float)(n2 + 1));
            return;
        }
        wTGroup.setBitmapTextureRect(0.25f * (float)n3, 0.5f * (float)n2, 0.25f * (float)(n3 + 1), 0.5f * (float)(n2 + 1));
    }
}

