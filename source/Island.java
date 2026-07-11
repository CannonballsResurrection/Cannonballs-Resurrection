/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 *  wildtangent.webdriver.WTShadow
 *  wildtangent.webdriver.WTSurfaceShader
 */
import java.util.StringTokenizer;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;
import wildtangent.webdriver.WTShadow;
import wildtangent.webdriver.WTSurfaceShader;

public class Island
implements Global,
WTOnLoadEvent,
WTConstants {
    Media_Object_Sound Music;
    WTModel WaterMesh;
    WTGroup WaterLevel;
    WTModel WaterMesh2;
    WTGroup WaterLevel2;
    WTModel WaterMesh3;
    WTGroup WaterLevel3;
    WTGroup WaterGroup;
    Media_Object_Actor Environment;
    Entity_Object_LensFlare LensFlare;
    VEC3D NormalTemp1 = new VEC3D();
    VEC3D NormalTemp2 = new VEC3D();
    VEC3D NormalTemp3 = new VEC3D();
    VEC3D Temp = new VEC3D();
    VEC3D Temp2 = new VEC3D();
    LandPatch[][] LandPatches;
    Prop[] prop = new Prop[128];
    int PropCount = 0;
    Media_Object_Sound SoundOcean;
    WTGroup Map;
    WTFile FileHeightMap;
    WTFile FileProps;
    WTSurfaceShader TerrainShader;
    Media_Object_Shader TerrainMap;
    Media_Object_Shader Shoreline;
    int TexWidth = 512;
    int TexHeight = 512;
    int Width = 96;
    int Height = 96;
    int SubWidth = 32;
    int SubHeight = 32;
    int SubDivision = this.Width / this.SubWidth;
    float VertexScale = 640 / this.Width;
    int MaxTerrainHeight = 100;
    float[][] HeightMap;
    float[][] TargetHeightMap;
    float WaterOffsetX1 = 0.0f;
    float WaterOffsetY1 = 0.0f;
    float WaterOffsetX2 = 0.0f;
    float WaterOffsetY2 = 0.0f;
    int FileLoadedCount = 0;
    boolean Loaded = false;
    boolean Visible = false;
    float CloudOffsetX = 0.0f;
    float CloudOffsetY = 0.0f;
    float WindX = 0.0f;
    float WindZ = 0.0f;
    float WindVelocity = 0.0f;
    float WindDirection = 0.0f;
    float lightX = -0.4f;
    float lightY = 0.0f;
    float lightZ = -0.2f;
    boolean TerrainChanged = false;
    int WaterFrame = 0;
    float WaterTimer = 0.0f;
    float WaterOffsetX = 0.0f;
    float WaterOffsetZ = 0.0f;
    LandPatch patchtest;
    boolean HeightmapSynced = true;
    int TerrainChangeOwner = -1;
    int LastWaterFrame = 1;
    int testMapX;
    int testMapZ;
    float testLX;
    float testLZ;
    float side;
    float returnHeight;
    float testHeight1;
    float testHeight2;

    void beginSounds() {
        if (this.SoundOcean == null && Main.MainRef.SoundsEnabled) {
            this.SoundOcean = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/SOUNDS/OCEAN"), true);
        }
        if (this.SoundOcean != null && !this.SoundOcean.getIsPlaying()) {
            this.SoundOcean.play(true, 127);
        }
    }

    public void onLoadComplete(WTObject wTObject) {
        ++this.FileLoadedCount;
        if (this.FileLoadedCount >= 2) {
            this.parseData();
        }
    }

    void stopSounds() {
        if (this.SoundOcean != null) {
            this.SoundOcean.stop();
        }
    }

    void windToVEC3D() {
        this.WindX = (float)Math.sin(Library_Math.degreesToRadians(this.WindDirection));
        this.WindZ = (float)Math.cos(Library_Math.degreesToRadians(this.WindDirection));
        this.WindX *= this.WindVelocity * 0.2f;
        this.WindZ *= this.WindVelocity * 0.2f;
        this.WindX = this.WindX;
    }

    void hide() {
        if (this.Visible) {
            int n = 0;
            while (n < this.PropCount) {
                if (this.prop[n] != null) {
                    this.prop[n].hide();
                }
                ++n;
            }
            n = 0;
            while (n < this.SubDivision) {
                int n2 = 0;
                while (n2 < this.SubDivision) {
                    if (this.LandPatches[n][n2] != null) {
                        this.LandPatches[n][n2].Dispose();
                    }
                    this.LandPatches[n][n2] = null;
                    ++n2;
                }
                ++n;
            }
            if (this.Music != null && this.Music.getIsPlaying()) {
                this.Music.stop();
            }
            this.Visible = false;
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.WaterGroup);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.WaterLevel);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.WaterLevel2);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.WaterLevel3);
            Main.MainRef.wt_stage.CollisionGroup.removeObject((WTContainer)this.Map);
            Main.MainRef.camera.Environment.removeObject((WTContainer)this.Environment.Model);
            if (Main.MainRef.MapTracker.HasSun[Main.MainRef.ActiveMap]) {
                if (this.LensFlare != null) {
                    this.LensFlare.destroy();
                }
                this.LensFlare = null;
            }
        }
    }

    void setWaterFrame(int n) {
        if (n != this.LastWaterFrame) {
            int n2 = (int)Math.ceil(n / 8);
            int n3 = n - n2 * 8;
            Main.MainRef.GlobalMedia.Water.Image.copyRect(Main.MainRef.GlobalMedia.WaterAnimation.Image, 0, n3 * 64, n2 * 64, 64, 64, 0, 0, 64, 64);
        }
        this.LastWaterFrame = n;
    }

    void updatePatches() {
        int n;
        int n2;
        int n3 = 0;
        while (n3 < this.SubDivision) {
            int n4 = 0;
            while (n4 < this.SubDivision) {
                this.LandPatches[n3][n4].Reset(this);
                ++n4;
            }
            ++n3;
        }
        LandPatch landPatch = null;
        LandPatch landPatch2 = null;
        LandPatch landPatch3 = null;
        LandPatch landPatch4 = null;
        int[] nArray = new int[5];
        boolean bl = true;
        while (bl) {
            bl = false;
            n2 = 0;
            while (n2 < this.SubDivision) {
                n = 0;
                while (n < this.SubDivision) {
                    landPatch = null;
                    landPatch2 = null;
                    landPatch3 = null;
                    landPatch4 = null;
                    if (n2 > 0) {
                        landPatch4 = this.LandPatches[n2 - 1][n];
                    }
                    if (n2 < this.SubDivision - 1) {
                        landPatch3 = this.LandPatches[n2 + 1][n];
                    }
                    if (n > 0) {
                        landPatch2 = this.LandPatches[n2][n - 1];
                    }
                    if (n < this.SubDivision - 1) {
                        landPatch = this.LandPatches[n2][n + 1];
                    }
                    nArray[0] = this.LandPatches[n2][n].m_nNextQuad;
                    nArray[1] = landPatch != null ? landPatch.m_nNextQuad : 0;
                    nArray[2] = landPatch2 != null ? landPatch2.m_nNextQuad : 0;
                    nArray[3] = landPatch3 != null ? landPatch3.m_nNextQuad : 0;
                    nArray[4] = landPatch4 != null ? landPatch4.m_nNextQuad : 0;
                    this.LandPatches[n2][n].Seam(landPatch, landPatch2, landPatch3, landPatch4);
                    if (nArray[0] != this.LandPatches[n2][n].m_nNextQuad || nArray[1] != (landPatch != null ? landPatch.m_nNextQuad : 0) || nArray[2] != (landPatch2 != null ? landPatch2.m_nNextQuad : 0) || nArray[3] != (landPatch3 != null ? landPatch3.m_nNextQuad : 0) || nArray[4] != (landPatch4 != null ? landPatch4.m_nNextQuad : 0)) {
                        bl = true;
                    }
                    ++n;
                }
                ++n2;
            }
        }
        n2 = 0;
        while (n2 < this.SubDivision) {
            n = 0;
            while (n < this.SubDivision) {
                landPatch = null;
                landPatch2 = null;
                landPatch3 = null;
                landPatch4 = null;
                if (n2 > 0) {
                    landPatch4 = this.LandPatches[n2 - 1][n];
                }
                if (n2 < this.SubDivision - 1) {
                    landPatch3 = this.LandPatches[n2 + 1][n];
                }
                if (n > 0) {
                    landPatch2 = this.LandPatches[n2][n - 1];
                }
                if (n < this.SubDivision - 1) {
                    landPatch = this.LandPatches[n2][n + 1];
                }
                this.LandPatches[n2][n].Render(landPatch, landPatch2, landPatch3, landPatch4);
                this.LandPatches[n2][n].UpdateMesh();
                this.LandPatches[n2][n].showMesh();
                ++n;
            }
            ++n2;
        }
    }

    void createMiniMap() {
        this.createShoreline();
        int n = 0;
        int n2 = 0;
        if (!Main.MainRef.hud.MiniMapTex.isStored()) {
            Main.MainRef.hud.MiniMapTex.store();
        } else {
            Main.MainRef.hud.MiniMapTex.restore();
        }
        n = 0;
        while (n < this.Width) {
            n2 = 0;
            while (n2 < this.Height) {
                float f = this.HeightMap[n][n2] / Main.MainRef.MapTracker.MapScale[Main.MainRef.ActiveMap];
                if (f < 0.4f && f > 0.0f) {
                    float f2;
                    float f3;
                    float f4;
                    if ((f -= 0.04f) < 0.0f) {
                        f *= -6.0f;
                    }
                    float f5 = f * 2.0f;
                    f5 = 1.0f - f5;
                    if (f < 0.04f) {
                        f4 = 20.0f;
                        f3 = 20.0f;
                        f2 = 20.0f;
                    } else {
                        f4 = 139.0f;
                        f3 = 55.0f;
                        f2 = 24.0f;
                    }
                    float f6 = Main.MainRef.random.nextFloat() * 40.0f - 20.0f;
                    float f7 = f5 - Main.MainRef.random.nextFloat() * 0.5f;
                    if (f7 < 0.0f) {
                        f7 = 0.0f;
                    }
                    if (f < 0.025f) {
                        f7 = 0.9f;
                    }
                    Main.MainRef.hud.MiniMapTex.blendPixel(n + 16, n2 + 16, f7, f4 - f6, f3 - f6, f2 - f6);
                }
                ++n2;
            }
            ++n;
        }
        Main.MainRef.hud.MiniMapTex.pushTexture();
    }

    void update(float f) {
        this.WaterTimer += f;
        if (this.WaterTimer > 0.08f) {
            ++this.WaterFrame;
            if (this.WaterFrame > 31) {
                this.WaterFrame = 0;
            }
            this.setWaterFrame(this.WaterFrame);
            this.WaterTimer = 0.0f;
        }
        while (this.WaterOffsetX < -1.0f) {
            this.WaterOffsetX += 1.0f;
        }
        while (this.WaterOffsetZ < -1.0f) {
            this.WaterOffsetZ += 1.0f;
        }
        this.WaterOffsetX = (float)((double)this.WaterOffsetX - (double)f * 0.1);
        this.WaterOffsetZ -= f * 0.088f;
        Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordOffset(0, 0, this.WaterOffsetX);
        Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordOffset(0, 1, this.WaterOffsetZ);
        this.WaterLevel.setPosition((float)(this.Width / 2) * this.VertexScale, 0.5f + Main.MainRef.SinTable[3] * 0.5f, (float)(this.Height / 2) * this.VertexScale);
        this.WaterLevel2.setPosition((float)(this.Width / 2) * this.VertexScale, 0.75f + Main.MainRef.SinTable[3] * 0.5f, (float)(this.Height / 2) * this.VertexScale);
        this.WaterLevel3.setPosition((float)(this.Width / 2) * this.VertexScale, 0.65f + Main.MainRef.SinTable[3] * 0.5f, (float)(this.Height / 2) * this.VertexScale);
        this.WaterLevel2.setAbsoluteScale(1.0f + Main.MainRef.SinTable[4] * 0.1f, 1.0f + Main.MainRef.SinTable[4] * 0.09f, 1.0f);
        this.WaterLevel3.setAbsoluteScale(1.0f + Main.MainRef.SinTable[7] * 0.095f, 1.0f + Main.MainRef.SinTable[7] * 0.1f, 1.0f);
        this.WaterGroup.setPosition(0.0f, 0.5f + Main.MainRef.SinTable[3] * 0.5f, 0.0f);
        this.CloudOffsetX -= this.WindX * f * 0.01f;
        this.CloudOffsetY += this.WindZ * f * 0.01f;
        while (this.CloudOffsetX > 1.0f) {
            this.CloudOffsetX -= 1.0f;
        }
        while (this.CloudOffsetX < -1.0f) {
            this.CloudOffsetX += 1.0f;
        }
        while (this.CloudOffsetY > 1.0f) {
            this.CloudOffsetY -= 1.0f;
        }
        while (this.CloudOffsetY < -1.0f) {
            this.CloudOffsetY += 1.0f;
        }
        this.TerrainShader.setTextureCoordOffset(2, 0, this.CloudOffsetX);
        this.TerrainShader.setTextureCoordOffset(2, 1, this.CloudOffsetY);
        if (this.Visible && Main.MainRef.MapTracker.HasSun[Main.MainRef.ActiveMap]) {
            this.LensFlare.updateTimeSlice(f);
        }
        if (this.TerrainChanged) {
            this.TerrainChanged = false;
        }
        if (Math.random() < (double)0.0075f) {
            float f2 = (float)(this.Width / 2) * this.VertexScale + (Main.MainRef.random.nextFloat() - 0.5f) * 800.0f;
            float f3 = (float)(this.Height / 2) * this.VertexScale + (Main.MainRef.random.nextFloat() - 0.5f) * 800.0f;
            float f4 = 2.0f + Main.MainRef.random.nextFloat() * 2.0f;
            float f5 = 2.0f + Main.MainRef.random.nextFloat() * 2.0f;
            if (f5 < f4) {
                f5 = f4;
            }
            if (this.getTerrainHeight(f2, f3) <= 0.0f) {
                Main.MainRef.ParticleList.add(new Particle_Object_Fish(f2, 0.0f, f3, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f, 30.0f + Main.MainRef.random.nextFloat() * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f));
            }
        }
        if (!this.HeightmapSynced) {
            this.syncHeightmap(f);
        }
        int n = 0;
        while (n < this.PropCount) {
            this.prop[n].update(f);
            ++n;
        }
    }

    void molehillAbsolute(float f, float f2, float f3, float f4, boolean bl, int n, int n2, int n3) {
        int n4;
        float f5 = 0.0f;
        float f6 = f / this.VertexScale * ((float)this.TexWidth / (float)this.Width);
        float f7 = (float)this.TexHeight - f2 / this.VertexScale * ((float)this.TexHeight / (float)this.Height);
        Main.MainRef.GlobalMedia.Sound_Quake.playDepth(false, Library_Math.camDistance3D(f, f5, f2));
        int n5 = 0;
        do {
            this.Temp.fill((Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f, f4 * 2.0f, (Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f);
            this.Temp2.setEqual(this.Temp);
            this.Temp2.normalize();
            this.Temp.add(f, 0.0f, f2);
            Main.MainRef.ParticleList.add(new Particle_Object_Ray(this.Temp.X, this.Temp.Z, this.Temp2.X, this.Temp2.Y, this.Temp2.Z, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
        } while (++n5 < 30);
        if (bl) {
            int n6 = 0;
            do {
                int n7 = 0;
                do {
                    n5 = (int)f6 - 16 + n6;
                    n4 = (int)f7 - 16 + n7;
                    if (n5 < 0 || n4 < 0 || n5 > 511 || n4 > 511) continue;
                    Main.MainRef.GlobalMedia.SplatTex.getPixel(this.Temp, n6, n7);
                    float f8 = 1.0f - this.Temp.X / 255.0f;
                    this.TerrainMap.blendPixel(n5, n4, f8, n, n2, n3);
                } while (++n7 < 32);
            } while (++n6 < 32);
            this.TerrainMap.pushTexture();
        }
        if ((f5 = this.getTerrainHeight(f, f2)) + f3 > (float)this.MaxTerrainHeight) {
            f3 = (float)this.MaxTerrainHeight - f5;
        }
        n5 = 0;
        while (n5 < this.Width) {
            n4 = 0;
            while (n4 < this.Height) {
                float f9 = Library_Math.distance((float)n5 * this.VertexScale, (float)(this.Height - n4) * this.VertexScale, f, f2);
                if (f9 < f4) {
                    float f10 = (1.0f - f9 / f4) * f3;
                    if (this.TargetHeightMap[n5][n4] < 0.0f) {
                        this.TargetHeightMap[n5][n4] = 0.0f;
                    }
                    float[] fArray = this.TargetHeightMap[n5];
                    int n8 = n4;
                    fArray[n8] = fArray[n8] + f10;
                    this.HeightMap[n5][n4] = this.TargetHeightMap[n5][n4];
                    this.HeightmapSynced = false;
                }
                ++n4;
            }
            ++n5;
        }
        f5 = this.getTerrainHeight(f, f2);
        this.TerrainChanged = true;
    }

    void syncHeightmap(float f) {
        boolean bl = false;
        int n = 0;
        while (n < this.Width) {
            int n2 = 0;
            while (n2 < this.Height) {
                if (this.HeightMap[n][n2] > this.TargetHeightMap[n][n2]) {
                    float[] fArray = this.HeightMap[n];
                    int n3 = n2;
                    fArray[n3] = fArray[n3] - 30.0f * f;
                    if (this.HeightMap[n][n2] < this.TargetHeightMap[n][n2]) {
                        this.HeightMap[n][n2] = this.TargetHeightMap[n][n2];
                    }
                    bl = true;
                } else if (this.HeightMap[n][n2] < this.TargetHeightMap[n][n2]) {
                    float[] fArray = this.HeightMap[n];
                    int n4 = n2;
                    fArray[n4] = fArray[n4] + 30.0f * f;
                    if (this.HeightMap[n][n2] > this.TargetHeightMap[n][n2]) {
                        this.HeightMap[n][n2] = this.TargetHeightMap[n][n2];
                    }
                    bl = true;
                }
                ++n2;
            }
            ++n;
        }
        if (!bl) {
            this.HeightmapSynced = true;
            return;
        }
        this.TerrainChanged = true;
        this.updatePatches();
        this.allToGround();
    }

    void switchShadows(boolean bl) {
        int n = 0;
        while (n < this.PropCount) {
            if (bl) {
                this.prop[n].showShadows();
            } else {
                this.prop[n].hideShadows();
            }
            ++n;
        }
    }

    void calculateFaceNormalTopRight(VEC3D vEC3D, int n, int n2) {
        this.NormalTemp2.fill(this.VertexScale, this.HeightMap[n + 1][n2] - this.HeightMap[n][n2], 0.0f);
        this.NormalTemp3.fill(this.VertexScale, this.HeightMap[n + 1][n2 + 1] - this.HeightMap[n][n2], -this.VertexScale);
        vEC3D.MakeCrossProduct(this.NormalTemp2, this.NormalTemp3);
        vEC3D.normalize();
    }

    void crater(int n, float f, float f2, float f3, float f4, boolean bl, float f5) {
        this.crater(n, f, f2, f3, f4, bl, f5, false, 0, 0, 0);
    }

    void crater(int n, float f, float f2, float f3, float f4, boolean bl, float f5, boolean bl2, int n2, int n3, int n4) {
        int n5;
        int n6;
        float f6 = 0.0f;
        boolean bl3 = false;
        this.TerrainChangeOwner = n;
        float f7 = f / this.VertexScale * ((float)this.TexWidth / (float)this.Width);
        float f8 = (float)this.TexHeight - f2 / this.VertexScale * ((float)this.TexHeight / (float)this.Height);
        if (bl2) {
            Main.MainRef.GlobalMedia.Sound_Quake.playDepth(false, Library_Math.camDistance3D(f, f6, f2));
            n6 = 0;
            do {
                this.Temp.fill((Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f, f4 * 2.0f, (Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f);
                this.Temp2.setEqual(this.Temp);
                this.Temp2.normalize();
                this.Temp.add(f, 0.0f, f2);
                Main.MainRef.ParticleList.add(new Particle_Object_Ray(this.Temp.X, this.Temp.Z, this.Temp2.X, this.Temp2.Y, this.Temp2.Z, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
            } while (++n6 < 30);
        }
        n6 = 0;
        do {
            n5 = 0;
            do {
                float f9;
                int n7 = (int)f7 - 16 + n6;
                int n8 = (int)f8 - 16 + n5;
                if (n7 < 0 || n8 < 0 || n7 > 511 || n8 > 511) continue;
                if (!bl2) {
                    Main.MainRef.GlobalMedia.ScorchTex.getPixel(this.Temp, n6, n5);
                    f9 = 1.0f - this.Temp.X / 255.0f;
                    this.TerrainMap.blendPixel(n7, n8, f9, 0.0f, 0.0f, 0.0f);
                    continue;
                }
                Main.MainRef.GlobalMedia.SplatTex.getPixel(this.Temp, n6, n5);
                f9 = 1.0f - this.Temp.X / 255.0f;
                this.TerrainMap.blendPixel(n7, n8, f9, n2, n3, n4);
            } while (++n5 < 32);
        } while (++n6 < 32);
        this.TerrainMap.pushTexture();
        n6 = 0;
        while (n6 < this.Width) {
            n5 = 0;
            while (n5 < this.Height) {
                float f10 = Library_Math.distance((float)n6 * this.VertexScale, (float)(this.Height - n5) * this.VertexScale, f, f2);
                if (f10 < f4) {
                    this.HeightmapSynced = false;
                    float f11 = (1.0f - f10 / f4) * f3;
                    float[] fArray = this.TargetHeightMap[n6];
                    int n9 = n5;
                    fArray[n9] = fArray[n9] - f11;
                    if (this.TargetHeightMap[n6][n5] <= 0.0f) {
                        Main.MainRef.hud.MiniMapTex.getStoredPixel(this.Temp, 16 + n6, 16 + n5);
                        Main.MainRef.hud.MiniMapTex.putPixel(16 + n6, 16 + n5, this.Temp.X, this.Temp.Y, this.Temp.Z);
                        bl3 = true;
                    }
                }
                ++n5;
            }
            ++n6;
        }
        if (bl3) {
            Main.MainRef.hud.MiniMapTex.pushTexture();
        }
        f6 = this.getTerrainHeight(f, f2);
        if (bl) {
            Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(f + Main.MainRef.random.nextFloat() - 0.5f, f6 + 2.0f, f2 + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 1.0f + (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, (Main.MainRef.random.nextFloat() - 0.5f) / 3.0f, 60.0f));
            Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(1, f, f6 - 4.0f, f2, false, 8.0f));
            n6 = 0;
            do {
                Main.MainRef.ParticleList.add(new Particle_Object_FireTrail(f + Main.MainRef.random.nextFloat() - 0.5f, f6 + 2.0f, f2 + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f * f5, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 20.0f * f5, 6.0f));
            } while (++n6 < 7);
        }
        this.TerrainChanged = true;
    }

    void calculateVertexNormal(VEC3D vEC3D, int n, int n2) {
        int n3 = 0;
        vEC3D.fill(0.0f, 0.0f, 0.0f);
        if (n < this.Width - 1 && n2 < this.Height - 1) {
            this.calculateFaceNormalTopRight(this.Temp, n, n2);
            vEC3D.add(this.Temp);
            this.calculateFaceNormalBottomLeft(this.Temp, n, n2);
            vEC3D.add(this.Temp);
            n3 += 2;
        }
        if (n < this.Width - 1 && n2 > 0) {
            this.calculateFaceNormalBottomLeft(this.Temp, n, n2 - 1);
            vEC3D.add(this.Temp);
            ++n3;
        }
        if (n > 0 && n2 > 0) {
            this.calculateFaceNormalTopRight(this.Temp, n - 1, n2 - 1);
            vEC3D.add(this.Temp);
            this.calculateFaceNormalBottomLeft(this.Temp, n - 1, n2 - 1);
            vEC3D.add(this.Temp);
            n3 += 2;
        }
        if (n > 0 && n2 < this.Height - 1) {
            this.calculateFaceNormalTopRight(this.Temp, n - 1, n2);
            vEC3D.add(this.Temp);
            ++n3;
        }
        vEC3D.divide(n3);
    }

    void calculateNormal(VEC3D vEC3D, float f, float f2) {
        int n;
        float f3;
        if (!(f > 0.0f && f2 > 0.0f && f < this.VertexScale * (float)this.Width && f2 < this.VertexScale * (float)this.Height)) {
            vEC3D.fill(0.0f, 1.0f, 0.0f);
            return;
        }
        f2 = (float)this.Width * this.VertexScale - f2;
        int n2 = (int)Math.floor(f / this.VertexScale);
        float f4 = f / this.VertexScale - (float)n2;
        if (f4 < (f3 = f2 / this.VertexScale - (float)(n = (int)Math.floor(f2 / this.VertexScale)))) {
            this.calculateFaceNormalTopRight(vEC3D, n2, n);
            return;
        }
        this.calculateFaceNormalBottomLeft(vEC3D, n2, n);
    }

    void molehill(float f, float f2, float f3, float f4, boolean bl, int n, int n2, int n3) {
        int n4;
        float f5 = 0.0f;
        float f6 = f / this.VertexScale * ((float)this.TexWidth / (float)this.Width);
        float f7 = (float)this.TexHeight - f2 / this.VertexScale * ((float)this.TexHeight / (float)this.Height);
        Main.MainRef.GlobalMedia.Sound_Quake.playDepth(false, Library_Math.camDistance3D(f, f5, f2));
        int n5 = 0;
        do {
            this.Temp.fill((Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f, f4 * 2.0f, (Main.MainRef.random.nextFloat() - 0.5f) * f4 * 1.5f);
            this.Temp2.setEqual(this.Temp);
            this.Temp2.normalize();
            this.Temp.add(f, 0.0f, f2);
            Main.MainRef.ParticleList.add(new Particle_Object_Ray(this.Temp.X, this.Temp.Z, this.Temp2.X, this.Temp2.Y, this.Temp2.Z, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, true));
        } while (++n5 < 30);
        if (bl) {
            int n6 = 0;
            do {
                int n7 = 0;
                do {
                    n5 = (int)f6 - 16 + n6;
                    n4 = (int)f7 - 16 + n7;
                    if (n5 < 0 || n4 < 0 || n5 > 511 || n4 > 511) continue;
                    Main.MainRef.GlobalMedia.SplatTex.getPixel(this.Temp, n6, n7);
                    float f8 = 1.0f - this.Temp.X / 255.0f;
                    this.TerrainMap.blendPixel(n5, n4, f8, n, n2, n3);
                } while (++n7 < 32);
            } while (++n6 < 32);
            this.TerrainMap.pushTexture();
        }
        if ((f5 = this.getTerrainHeight(f, f2)) + f3 > (float)this.MaxTerrainHeight) {
            f3 = (float)this.MaxTerrainHeight - f5;
        }
        n5 = 0;
        while (n5 < this.Width) {
            n4 = 0;
            while (n4 < this.Height) {
                float f9 = Library_Math.distance((float)n5 * this.VertexScale, (float)(this.Height - n4) * this.VertexScale, f, f2);
                if (f9 < f4) {
                    float f10 = (1.0f - f9 / f4) * f3;
                    if (this.TargetHeightMap[n5][n4] < 0.0f) {
                        this.TargetHeightMap[n5][n4] = 0.0f;
                    }
                    float[] fArray = this.TargetHeightMap[n5];
                    int n8 = n4;
                    fArray[n8] = fArray[n8] + f10;
                    this.HeightmapSynced = false;
                }
                ++n4;
            }
            ++n5;
        }
        f5 = this.getTerrainHeight(f, f2);
        this.TerrainChanged = true;
    }

    void placeMiniMapChests() {
        int n = 0;
        n = 0;
        while (n < Main.MainRef.ChestCount) {
            if (Main.MainRef.chest[n].Visible) {
                Main.MainRef.chest[n].MapIcon.setPosition(0.744192f + Main.MainRef.chest[n].X / this.VertexScale * 0.002736f, -0.268128f + Main.MainRef.chest[n].Z / this.VertexScale * 0.002736f, 1.0f);
            } else {
                Main.MainRef.chest[n].MapIcon.setPosition(0.0f, 0.0f, -1.0f);
            }
            ++n;
        }
    }

    float getTerrainHeight(float f, float f2) {
        if (!(f > 0.0f && f2 > 0.0f && f < this.VertexScale * (float)this.Width && f2 < this.VertexScale * (float)this.Height)) {
            return 0.0f;
        }
        f2 = (float)this.Width * this.VertexScale - f2;
        this.testMapX = (int)Math.floor(f / this.VertexScale);
        this.testMapZ = (int)Math.floor(f2 / this.VertexScale);
        this.testLX = f / this.VertexScale - (float)this.testMapX;
        this.testLZ = f2 / this.VertexScale - (float)this.testMapZ;
        if (this.testLX < this.testLZ) {
            this.testHeight1 = this.HeightMap[this.testMapX][this.testMapZ] + (this.HeightMap[this.testMapX][this.testMapZ + 1] - this.HeightMap[this.testMapX][this.testMapZ]) * this.testLZ;
            this.testHeight2 = this.HeightMap[this.testMapX + 1][this.testMapZ + 1] - (this.HeightMap[this.testMapX][this.testMapZ + 1] - this.HeightMap[this.testMapX][this.testMapZ]) + (this.HeightMap[this.testMapX][this.testMapZ + 1] - this.HeightMap[this.testMapX][this.testMapZ]) * this.testLZ;
            this.returnHeight = this.testHeight1 + (this.testHeight2 - this.testHeight1) * this.testLX;
        } else {
            this.testHeight2 = this.HeightMap[this.testMapX + 1][this.testMapZ] + (this.HeightMap[this.testMapX + 1][this.testMapZ + 1] - this.HeightMap[this.testMapX + 1][this.testMapZ]) * this.testLZ;
            this.testHeight1 = this.HeightMap[this.testMapX][this.testMapZ] + (this.HeightMap[this.testMapX + 1][this.testMapZ + 1] - this.HeightMap[this.testMapX + 1][this.testMapZ]) * this.testLZ;
            this.returnHeight = this.testHeight1 + (this.testHeight2 - this.testHeight1) * this.testLX;
        }
        return this.returnHeight;
    }

    void switchMusic(boolean bl) {
        if (bl) {
            if (this.Music == null && Main.MainRef.SoundsEnabled) {
                this.Music = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound(Main.MainRef.MapTracker.MapMusic[Main.MainRef.ActiveMap]), true);
            }
            if (this.Music != null && !this.Music.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                this.Music.play(true, 127);
                return;
            }
        } else if (this.Music != null) {
            this.Music.stop();
        }
    }

    public Island(String string) {
        if (Main.MainRef.SoundsEnabled) {
            this.SoundOcean = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound("MEDIA/SOUNDS/OCEAN"), true);
        }
        this.LandPatches = new LandPatch[this.SubDivision][this.SubDivision];
        this.HeightMap = new float[this.Width + 2][this.Height + 2];
        this.TargetHeightMap = new float[this.Width + 2][this.Height + 2];
        this.clear();
        this.TerrainMap = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader(string + "/image.png", false, false, false), true);
        this.Shoreline = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/SHORELINE/image.png", false, true, false), true);
        this.Map = Main.MainRef.Wt.createGroup();
        this.Map.setCollisionMask(4);
        this.WaterMesh = Main.MainRef.Wt.createPlane(4000.0f, 4000.0f, false, 0.0f, 0.0f, 5);
        this.WaterMesh.setSurfaceShader(Main.MainRef.GlobalMedia.Water.Shader);
        this.WaterMesh2 = Main.MainRef.Wt.createPlane((float)this.Width * 1.2f * this.VertexScale, (float)this.Height * 1.2f * this.VertexScale, false, 0.0f, 0.0f, 10);
        this.WaterMesh2.setSurfaceShader(this.Shoreline.Shader);
        this.WaterMesh2.setTextureRect("front", 1.0f, 1.0f, 0.0f, 0.0f);
        this.WaterMesh3 = Main.MainRef.Wt.createPlane((float)this.Width * 1.15f * this.VertexScale, (float)this.Height * 1.15f * this.VertexScale, false, 0.0f, 0.0f, 10);
        this.WaterMesh3.setSurfaceShader(this.Shoreline.Shader);
        this.WaterMesh3.setTextureRect("front", 1.0f, 1.0f, 0.0f, 0.0f);
        this.WaterLevel = Main.MainRef.Wt.createGroup();
        this.WaterLevel.attach((WTObject)this.WaterMesh);
        this.WaterLevel.setAbsoluteOrientation(1.0f, 0.0f, 0.0f, -90.0f);
        this.WaterLevel2 = Main.MainRef.Wt.createGroup();
        this.WaterLevel2.attach((WTObject)this.WaterMesh2);
        this.WaterLevel2.setAbsoluteOrientation(1.0f, 0.0f, 0.0f, -90.0f);
        this.WaterLevel2.setRotation(0.0f, 0.0f, 1.0f, -3.0f);
        this.WaterLevel3 = Main.MainRef.Wt.createGroup();
        this.WaterLevel3.attach((WTObject)this.WaterMesh2);
        this.WaterLevel3.setAbsoluteOrientation(1.0f, 0.0f, 0.0f, -90.0f);
        this.WaterLevel3.setRotation(0.0f, 0.0f, 1.0f, 5.0f);
        this.WaterGroup = Main.MainRef.Wt.createGroup();
        this.WaterLevel.setPosition((float)(this.Width / 2) * this.VertexScale, 1.0f, (float)(this.Height / 2) * this.VertexScale);
        this.FileHeightMap = Main.MainRef.Wt.readFile(Main.MainRef.MediaPath + string + "/heightmap" + this.Width + ".dat");
        this.FileHeightMap.setOnLoad((WTOnLoadEvent)this);
        this.FileProps = Main.MainRef.Wt.readFile(Main.MainRef.MediaPath + string + "/objects.dat");
        this.FileProps.setOnLoad((WTOnLoadEvent)this);
        this.Environment = (Media_Object_Actor)Main.MainRef.MediaList.add(new Media_Object_Actor(Main.MainRef.MapTracker.MapEnvironments[Main.MainRef.ActiveMap]), true);
        if (Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
            this.Music = (Media_Object_Sound)Main.MainRef.MediaList.add(new Media_Object_Sound(Main.MainRef.MapTracker.MapMusic[Main.MainRef.ActiveMap]), true);
        }
    }

    void reInitWind() {
        this.WindDirection = Main.MainRef.random.nextFloat() * 360.0f;
        this.WindVelocity = (int)(Main.MainRef.random.nextFloat() * 80.0f);
        this.windToVEC3D();
    }

    void destroy() {
        int n = 0;
        this.stopSounds();
        this.hide();
        this.WaterGroup = null;
        this.WaterLevel.detach();
        this.WaterMesh.removeTexture();
        this.WaterMesh = null;
        this.WaterLevel = null;
        this.WaterLevel2.detach();
        this.WaterMesh2.removeTexture();
        this.WaterMesh2 = null;
        this.WaterLevel2 = null;
        this.WaterLevel3.detach();
        this.WaterMesh3.removeTexture();
        this.WaterMesh3 = null;
        this.WaterLevel3 = null;
        if (this.Music != null) {
            Main.MainRef.MediaList.remove(this.Music);
        }
        this.Music = null;
        if (this.SoundOcean != null) {
            Main.MainRef.MediaList.remove(this.SoundOcean);
        }
        this.SoundOcean = null;
        n = 0;
        while (n < this.PropCount) {
            if (this.prop[n] != null) {
                this.prop[n].destroy();
            }
            this.prop[n] = null;
            ++n;
        }
        if (this.Environment != null) {
            Main.MainRef.MediaList.remove(this.Environment);
        }
        this.Environment = null;
        if (this.TerrainMap != null) {
            Main.MainRef.MediaList.remove(this.TerrainMap);
        }
        if (this.Shoreline != null) {
            Main.MainRef.MediaList.remove(this.Shoreline);
        }
        this.TerrainMap = null;
        this.Shoreline = null;
        this.TerrainShader = null;
        this.Map = null;
    }

    void parseData() {
        int n = 0;
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        while (!this.FileHeightMap.eof()) {
            f4 = this.FileHeightMap.readByte();
            if (f4 < 0.0f) {
                f4 = 256.0f + f4;
            }
            f4 = f4 / 256.0f * Main.MainRef.MapTracker.MapScale[Main.MainRef.ActiveMap] - 4.0f;
            if (f2 < (float)this.Height && f < (float)this.Width) {
                this.HeightMap[(int)f][(int)f2] = f4;
            }
            if ((f += 1.0f) != (float)this.Width) continue;
            f = 0.0f;
            f2 += 1.0f;
        }
        this.FileHeightMap.close();
        n = 0;
        while (n <= this.Height) {
            this.HeightMap[0][n] = this.HeightMap[1][n];
            ++n;
        }
        n = 0;
        while (n <= this.Height) {
            this.HeightMap[this.Width][n] = this.HeightMap[this.Width - 1][n];
            this.HeightMap[this.Width + 1][n] = this.HeightMap[this.Width][n];
            ++n;
        }
        n = 0;
        while (n <= this.Width) {
            this.HeightMap[n][0] = this.HeightMap[n][1];
            ++n;
        }
        n = 0;
        while (n <= this.Width) {
            this.HeightMap[n][this.Height] = this.HeightMap[n][this.Height - 1];
            this.HeightMap[n][this.Height + 1] = this.HeightMap[n][this.Height];
            ++n;
        }
        int n2 = 0;
        while (n2 < this.Width) {
            int n3 = 0;
            while (n3 < this.Height) {
                this.TargetHeightMap[n2][n3] = this.HeightMap[n2][n3];
                ++n3;
            }
            ++n2;
        }
        while (!this.FileProps.eof()) {
            float f5;
            float f6;
            float f7;
            String string;
            String string2 = this.FileProps.readLine();
            StringTokenizer stringTokenizer = new StringTokenizer(string2, ":,");
            if (stringTokenizer.countTokens() < 1) continue;
            String string3 = stringTokenizer.nextToken();
            if (string3.equalsIgnoreCase("<PROP>")) {
                string = stringTokenizer.nextToken();
                f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f7 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                this.prop[this.PropCount] = new Prop(this.PropCount, string, f * this.VertexScale, 0.0f, f3 * this.VertexScale, f7);
                ++this.PropCount;
            }
            if (string3.equalsIgnoreCase("<PROPPOS>")) {
                string = stringTokenizer.nextToken();
                f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f2 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f7 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                this.prop[this.PropCount] = new Prop(this.PropCount, string, f * this.VertexScale, f2, f3 * this.VertexScale, f7);
                ++this.PropCount;
            }
            if (string3.equalsIgnoreCase("<DECORATION>")) {
                string = stringTokenizer.nextToken();
                f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f7 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f6 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f5 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                Main.MainRef.ParticleList.add(new Particle_Object_Decoration(string, f * this.VertexScale, f3 * this.VertexScale, f7, f6, f5, false));
            }
            if (string3.equalsIgnoreCase("<DECORATIONWATER>")) {
                string = stringTokenizer.nextToken();
                f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f7 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f6 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f5 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                Main.MainRef.ParticleList.add(new Particle_Object_Decoration(string, f * this.VertexScale, f3 * this.VertexScale, f7, f6, f5, true));
            }
            if (string3.equalsIgnoreCase("<CLOUD>")) {
                int n4 = Float.valueOf(stringTokenizer.nextToken()).intValue();
                f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f2 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f6 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                f5 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                float f8 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                Main.MainRef.ParticleList.add(new Particle_Object_Cloud(n4, f * this.VertexScale, f2, f3 * this.VertexScale, f6, f5, f8));
            }
            if (!string3.equalsIgnoreCase("<FIREFLY>")) continue;
            f = Float.valueOf(stringTokenizer.nextToken()).floatValue();
            f2 = Float.valueOf(stringTokenizer.nextToken()).floatValue();
            f3 = (float)this.Height - Float.valueOf(stringTokenizer.nextToken()).floatValue();
            Main.MainRef.ParticleList.add(new Particle_Object_Firefly(f * this.VertexScale, f2, f3 * this.VertexScale));
        }
        this.FileProps.close();
        this.FileHeightMap = null;
        this.FileProps = null;
        this.Loaded = true;
    }

    void positionCamera() {
        Main.MainRef.camera.X = (float)(this.Width / 2) * this.VertexScale;
        Main.MainRef.camera.Z = (float)(this.Height / 2) * this.VertexScale;
        Main.MainRef.camera.Y = 200.0f;
        Main.MainRef.camera.positionCamera();
    }

    boolean isLoaded() {
        if (!this.Loaded) {
            return false;
        }
        if (!this.TerrainMap.isLoaded()) {
            return false;
        }
        if (!this.Shoreline.isLoaded()) {
            return false;
        }
        this.Shoreline.Shader.setNumLayers(2);
        this.Shoreline.Shader.setTexture(1, Main.MainRef.GlobalMedia.Water.Image);
        this.Shoreline.Shader.setLayerType(1, 5);
        this.Shoreline.Shader.setLayerSource(1, 2);
        if (Main.MainRef.GlobalMedia.Water.Shader.getNumLayers() < 2) {
            Main.MainRef.GlobalMedia.Water.Shader.setNumLayers(2);
            Main.MainRef.GlobalMedia.Water.Shader.setTexture(1, Main.MainRef.GlobalMedia.Water.Image);
            Main.MainRef.GlobalMedia.Water.Shader.setLayerType(1, 5);
            Main.MainRef.GlobalMedia.Water.Shader.setLayerSource(1, 2);
            Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordScale(0, 0, 70.0f);
            Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordScale(0, 1, 70.0f);
            Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordScale(1, 0, 10.0f);
            Main.MainRef.GlobalMedia.Water.Shader.setTextureCoordScale(1, 1, 10.0f);
        }
        if (this.TerrainShader == null) {
            this.TerrainShader = Main.MainRef.Wt.createSurfaceShader();
            this.TerrainShader.setFrameBufferOperation(1);
            this.TerrainShader.setNumLayers(3);
            this.TerrainShader.setTexture(0, this.TerrainMap.Image);
            this.TerrainShader.setLayerType(0, 3);
            this.TerrainShader.setLayerSource(0, 2);
            this.TerrainShader.setTexture(1, Main.MainRef.GlobalMedia.Grit.Image);
            this.TerrainShader.setLayerType(1, 5);
            this.TerrainShader.setTextureCoordScale(1, 0, 40.0f);
            this.TerrainShader.setTextureCoordScale(1, 1, 40.0f);
            this.TerrainShader.setLayerSource(1, 2);
            this.TerrainShader.setTexture(2, Main.MainRef.GlobalMedia.CloudShadow.Image);
            this.TerrainShader.setLayerType(2, 3);
            this.TerrainShader.setTextureCoordScale(2, 0, 4.0f);
            this.TerrainShader.setTextureCoordScale(2, 1, 4.0f);
            this.TerrainShader.setLayerSource(2, 2);
        }
        int n = 0;
        while (n < this.PropCount) {
            if (!this.prop[n].Loaded) {
                return false;
            }
            ++n;
        }
        return true;
    }

    void placeMiniMapPlayers() {
        int n = 0;
        n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n].Active && !Main.MainRef.cannon[n].Respawning) {
                if (n == Main.MainRef.network.PlayerNumber) {
                    Main.MainRef.cannon[n].MapIcon.setPosition(0.0f, 0.0f, -1.0f);
                    if (Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Active) {
                        Main.MainRef.hud.MiniArrowGroup.setPosition(0.744192f + Main.MainRef.cannon[n].Position.X / this.VertexScale * 0.002736f, -0.268128f + Main.MainRef.cannon[n].Position.Z / this.VertexScale * 0.002736f, 1.0f);
                    } else {
                        Main.MainRef.hud.MiniArrowGroup.setPosition(0.0f, 0.0f, -1.0f);
                    }
                } else {
                    Main.MainRef.cannon[n].MapIcon.setPosition(0.744192f + Main.MainRef.cannon[n].Position.X / this.VertexScale * 0.002736f, -0.268128f + Main.MainRef.cannon[n].Position.Z / this.VertexScale * 0.002736f, 1.0f);
                }
            } else {
                Main.MainRef.cannon[n].MapIcon.setPosition(0.0f, 0.0f, -1.0f);
                if (n == Main.MainRef.network.PlayerNumber) {
                    Main.MainRef.hud.MiniArrowGroup.setPosition(0.0f, 0.0f, -1.0f);
                }
            }
            ++n;
        }
    }

    void removeReceivers(WTShadow wTShadow) {
        if (wTShadow == null) {
            return;
        }
        int n = 0;
        while (n < this.SubDivision) {
            int n2 = 0;
            while (n2 < this.SubDivision) {
                if (this.LandPatches[n2][n] != null && this.LandPatches[n2][n].meshGroupA != null) {
                    wTShadow.removeReceiver(this.LandPatches[n2][n].meshGroupA);
                }
                ++n2;
            }
            ++n;
        }
    }

    void calculateFaceNormalBottomLeft(VEC3D vEC3D, int n, int n2) {
        this.NormalTemp2.fill(this.VertexScale, this.HeightMap[n + 1][n2 + 1] - this.HeightMap[n][n2], -this.VertexScale);
        this.NormalTemp3.fill(0.0f, this.HeightMap[n][n2 + 1] - this.HeightMap[n][n2], -this.VertexScale);
        vEC3D.MakeCrossProduct(this.NormalTemp2, this.NormalTemp3);
        vEC3D.normalize();
    }

    void groove(int n, VEC3D vEC3D, VEC3D vEC3D2, float f, float f2, boolean bl) {
        int n2;
        boolean bl2 = false;
        this.TerrainChangeOwner = n;
        this.Temp.setEqual(vEC3D2);
        this.Temp.subtract(vEC3D);
        this.Temp.Y = 0.0f;
        float f3 = this.Temp.length() / 20.0f;
        this.Temp.normalize();
        int n3 = 0;
        do {
            Main.MainRef.ParticleList.add(new Particle_Object_Ray(vEC3D.X + this.Temp.X * (float)n3 * f3, vEC3D.Z + this.Temp.Z * (float)n3 * f3, 0.0f, 1.0f, 0.0f, 0.5f + Main.MainRef.random.nextFloat() * 2.0f, false));
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(2, false, 0.0f, vEC3D.X + this.Temp.X * (float)n3 * f3 + Main.MainRef.random.nextFloat() - 0.5f, this.getTerrainHeight(vEC3D.X + this.Temp.X * (float)n3 * f3, vEC3D.Z + this.Temp.Z * (float)n3 * f3), vEC3D.Z + this.Temp.Z * (float)n3 * f3 + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
                continue;
            }
            Main.MainRef.ParticleList.add(Main.MainRef.Chunks.getNextChunk(3, false, 0.0f, vEC3D.X + this.Temp.X * (float)n3 * f3 + Main.MainRef.random.nextFloat() - 0.5f, this.getTerrainHeight(vEC3D.X + this.Temp.X * (float)n3 * f3, vEC3D.Z + this.Temp.Z * (float)n3 * f3), vEC3D.Z + this.Temp.Z * (float)n3 * f3 + Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 30.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 30.0f, 0.1f + Main.MainRef.random.nextFloat() * 1.0f, true));
        } while (++n3 < 20);
        n3 = 0;
        do {
            n2 = 0;
            do {
                float f4;
                float f5 = vEC3D.X / this.VertexScale * ((float)this.TexWidth / (float)this.Width);
                float f6 = (float)this.TexHeight - vEC3D.Z / this.VertexScale * ((float)this.TexHeight / (float)this.Height);
                int n4 = (int)f5 - 16 + n3;
                int n5 = (int)f6 - 16 + n2;
                if (n4 >= 0 && n5 >= 0 && n4 <= 511 && n5 <= 511) {
                    Main.MainRef.GlobalMedia.ScorchTex.getPixel(this.Temp, n3, n2);
                    f4 = 1.0f - this.Temp.X / 255.0f;
                    this.TerrainMap.blendPixel(n4, n5, f4, 0.0f, 0.0f, 0.0f);
                }
                f5 = vEC3D2.X / this.VertexScale * ((float)this.TexWidth / (float)this.Width);
                f6 = (float)this.TexHeight - vEC3D2.Z / this.VertexScale * ((float)this.TexHeight / (float)this.Height);
                n4 = (int)f5 - 16 + n3;
                n5 = (int)f6 - 16 + n2;
                if (n4 < 0 || n5 < 0 || n4 > 511 || n5 > 511) continue;
                Main.MainRef.GlobalMedia.ScorchTex.getPixel(this.Temp, n3, n2);
                f4 = 1.0f - this.Temp.X / 255.0f;
                this.TerrainMap.blendPixel(n4, n5, f4, 0.0f, 0.0f, 0.0f);
            } while (++n2 < 32);
        } while (++n3 < 32);
        if (bl) {
            this.TerrainMap.pushTexture();
        }
        n3 = 0;
        while (n3 < this.Width) {
            n2 = 0;
            while (n2 < this.Height) {
                this.Temp.fill((float)n3 * this.VertexScale, 0.0f, (float)(this.Height - n2) * this.VertexScale);
                float f7 = this.Temp.distanceToLine(vEC3D, vEC3D2);
                if (f7 < f2) {
                    this.HeightmapSynced = false;
                    float f8 = (1.0f - f7 / f2) * f;
                    float[] fArray = this.TargetHeightMap[n3];
                    int n6 = n2;
                    fArray[n6] = fArray[n6] - f8;
                    if (this.TargetHeightMap[n3][n2] <= 0.0f) {
                        Main.MainRef.hud.MiniMapTex.getStoredPixel(this.Temp, 16 + n3, 16 + n2);
                        Main.MainRef.hud.MiniMapTex.putPixel(16 + n3, 16 + n2, this.Temp.X, this.Temp.Y, this.Temp.Z);
                        bl2 = true;
                    }
                }
                ++n2;
            }
            ++n3;
        }
        if (bl2) {
            Main.MainRef.hud.MiniMapTex.pushTexture();
        }
        this.TerrainChanged = true;
    }

    void show() {
        if (!this.Visible) {
            int n;
            int n2;
            int n3 = 0;
            while (n3 < this.SubDivision) {
                int n4 = 0;
                while (n4 < this.SubDivision) {
                    this.LandPatches[n3][n4] = new LandPatch();
                    this.LandPatches[n3][n4].Create("", (float)(n3 * this.SubWidth) * this.VertexScale, (float)this.Height * this.VertexScale - (float)((n4 + 1) * this.SubHeight) * this.VertexScale, n3 * 32, n4 * 32, 1.0f / (float)this.SubDivision, 1.0f / (float)this.SubDivision * (float)n3, 1.0f / (float)this.SubDivision * (float)n4);
                    this.LandPatches[n3][n4].Reset(this);
                    ++n4;
                }
                ++n3;
            }
            LandPatch landPatch = null;
            LandPatch landPatch2 = null;
            LandPatch landPatch3 = null;
            LandPatch landPatch4 = null;
            int[] nArray = new int[5];
            boolean bl = true;
            while (bl) {
                bl = false;
                n2 = 0;
                while (n2 < this.SubDivision) {
                    n = 0;
                    while (n < this.SubDivision) {
                        landPatch = null;
                        landPatch2 = null;
                        landPatch3 = null;
                        landPatch4 = null;
                        if (n2 > 0) {
                            landPatch4 = this.LandPatches[n2 - 1][n];
                        }
                        if (n2 < this.SubDivision - 1) {
                            landPatch3 = this.LandPatches[n2 + 1][n];
                        }
                        if (n > 0) {
                            landPatch2 = this.LandPatches[n2][n - 1];
                        }
                        if (n < this.SubDivision - 1) {
                            landPatch = this.LandPatches[n2][n + 1];
                        }
                        nArray[0] = this.LandPatches[n2][n].m_nNextQuad;
                        nArray[1] = landPatch != null ? landPatch.m_nNextQuad : 0;
                        nArray[2] = landPatch2 != null ? landPatch2.m_nNextQuad : 0;
                        nArray[3] = landPatch3 != null ? landPatch3.m_nNextQuad : 0;
                        nArray[4] = landPatch4 != null ? landPatch4.m_nNextQuad : 0;
                        this.LandPatches[n2][n].Seam(landPatch, landPatch2, landPatch3, landPatch4);
                        if (nArray[0] != this.LandPatches[n2][n].m_nNextQuad || nArray[1] != (landPatch != null ? landPatch.m_nNextQuad : 0) || nArray[2] != (landPatch2 != null ? landPatch2.m_nNextQuad : 0) || nArray[3] != (landPatch3 != null ? landPatch3.m_nNextQuad : 0) || nArray[4] != (landPatch4 != null ? landPatch4.m_nNextQuad : 0)) {
                            bl = true;
                        }
                        ++n;
                    }
                    ++n2;
                }
            }
            n2 = 0;
            while (n2 < this.SubDivision) {
                n = 0;
                while (n < this.SubDivision) {
                    landPatch = null;
                    landPatch2 = null;
                    landPatch3 = null;
                    landPatch4 = null;
                    if (n2 > 0) {
                        landPatch4 = this.LandPatches[n2 - 1][n];
                    }
                    if (n2 < this.SubDivision - 1) {
                        landPatch3 = this.LandPatches[n2 + 1][n];
                    }
                    if (n > 0) {
                        landPatch2 = this.LandPatches[n2][n - 1];
                    }
                    if (n < this.SubDivision - 1) {
                        landPatch = this.LandPatches[n2][n + 1];
                    }
                    this.LandPatches[n2][n].Render(landPatch, landPatch2, landPatch3, landPatch4);
                    this.LandPatches[n2][n].UpdateMesh();
                    this.LandPatches[n2][n].showMesh();
                    ++n;
                }
                ++n2;
            }
            if (this.Music != null && !this.Music.getIsPlaying() && Main.MainRef.MusicEnabled && Main.MainRef.SoundsEnabled) {
                this.Music.play(true, 127);
            }
            this.Visible = true;
            this.createMiniMap();
            Main.MainRef.wt_stage.CollisionGroup.addObject((WTContainer)this.Map);
            this.Map.setPosition(0.0f, 0.0f, 0.0f);
            Main.MainRef.camera.Environment.addObject((WTContainer)this.Environment.Model);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.WaterLevel);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.WaterLevel2);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.WaterLevel3);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.WaterGroup);
            this.Environment.Model.setOption(0, -100);
            if (Main.MainRef.MapTracker.HasSun[Main.MainRef.ActiveMap]) {
                this.LensFlare = new Entity_Object_LensFlare(Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].X, Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].Z);
            } else {
                this.Temp.fill(Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].X, Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].Y, Main.MainRef.MapTracker.MapSun[Main.MainRef.ActiveMap].Z);
                this.Temp.normalize();
                this.Temp.multiply(-1.0f);
                Main.MainRef.wt_stage.Directional.setOrientationVector(this.Temp.X, this.Temp.Y, this.Temp.Z, -this.Temp.Z, this.Temp.Y, this.Temp.X);
            }
            n2 = 0;
            while (n2 < this.PropCount) {
                this.prop[n2].show();
                ++n2;
            }
            this.allToGround();
            this.setWaterFrame(this.WaterFrame);
        }
    }

    void clear() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < this.Width) {
            n2 = 0;
            while (n2 < this.Height) {
                this.HeightMap[n][n2] = 0.0f;
                ++n2;
            }
            ++n;
        }
    }

    void switchSounds(boolean bl) {
        if (bl) {
            this.beginSounds();
            return;
        }
        this.stopSounds();
    }

    void addReceivers(WTShadow wTShadow) {
        int n = 0;
        while (n < this.SubDivision) {
            int n2 = 0;
            while (n2 < this.SubDivision) {
                wTShadow.addReceiver(this.LandPatches[n2][n].meshGroupA);
                ++n2;
            }
            ++n;
        }
    }

    void createShoreline() {
        int n = 0;
        int n2 = 0;
        n = 0;
        while (n < this.Width) {
            n2 = 0;
            while (n2 < this.Height) {
                float f = this.HeightMap[n][n2] / Main.MainRef.MapTracker.MapScale[Main.MainRef.ActiveMap];
                if (f < 0.2f && f > 0.0f) {
                    if ((f -= 0.04f) < 0.0f) {
                        f *= -10.0f;
                    }
                    float f2 = f * 2.0f;
                    f2 = 1.0f - f2;
                    float f3 = 99.0f;
                    float f4 = 151.0f;
                    float f5 = 254.0f;
                    float f6 = Main.MainRef.random.nextFloat() * 20.0f - 10.0f;
                    float f7 = f2 - Main.MainRef.random.nextFloat() * 0.4f;
                    if (f7 < 0.0f) {
                        f7 = 0.0f;
                    }
                    if (f < 0.025f) {
                        f7 = 0.95f;
                    }
                    this.Shoreline.putPixel(n * 2, n2 * 2, (f3 - f6) * f7, (f4 - f6) * f7, (f5 - f6) * f7);
                    f6 = Main.MainRef.random.nextFloat() * 40.0f - 20.0f;
                    this.Shoreline.putPixel(n * 2 + 1, n2 * 2, (f3 - f6) * f7, (f4 - f6) * f7, (f5 - f6) * f7);
                    f6 = Main.MainRef.random.nextFloat() * 40.0f - 20.0f;
                    this.Shoreline.putPixel(n * 2, n2 * 2 + 1, (f3 - f6) * f7, (f4 - f6) * f7, (f5 - f6) * f7);
                    f6 = Main.MainRef.random.nextFloat() * 40.0f - 20.0f;
                    this.Shoreline.putPixel(n * 2 + 1, n2 * 2 + 1, (f3 - f6) * f7, (f4 - f6) * f7, (f5 - f6) * f7);
                }
                ++n2;
            }
            ++n;
        }
        this.Shoreline.blur();
        this.Shoreline.pushTexture();
    }

    void allToGround() {
        int n = 0;
        while (n < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n].Active && !Main.MainRef.cannon[n].Respawning) {
                Main.MainRef.cannon[n].toGround();
            }
            ++n;
        }
        n = 0;
        while (n < Main.MainRef.ChestCount) {
            if (Main.MainRef.chest[n].Visible) {
                Main.MainRef.chest[n].drop();
            }
            ++n;
        }
        n = 0;
        while (n < this.PropCount) {
            if (this.prop[n].Visible) {
                this.prop[n].drop();
            }
            ++n;
        }
    }
}

