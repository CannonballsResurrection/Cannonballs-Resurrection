/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTModel
 *  wildtangent.webdriver.WTObject
 */
import java.util.Stack;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTModel;
import wildtangent.webdriver.WTObject;

public final class LandPatch {
    public static int EXCLUDESIZE = 8;
    public static VEC3D TempVec = new VEC3D();
    protected static int[] tuvidx = null;
    protected int meshVertIdx = 0;
    protected int meshFaceIdx = 0;
    protected float[] verts = null;
    protected float[] uvs = null;
    protected float[] norms = null;
    protected int[] faces = null;
    protected QuadTreeNode root = null;
    protected char[] vertexMap = null;
    protected boolean isLoaded = false;
    WTGroup meshGroupA = null;
    private WTModel meshA = null;
    protected float positionX = 0.0f;
    protected float positionZ = 0.0f;
    protected final int POOL_SIZE = 100;
    protected QuadTreeNode[] m_aQuadPool = null;
    protected int m_nNextQuad = 0;
    protected Stack m_ChildStack = null;
    protected int vertsWide = 81;
    protected int vertsHigh = 81;
    protected float sizeScale = 0.0f;
    private boolean Created = false;
    private boolean Visible = false;
    int topleftX = 0;
    int topleftY = 0;
    float UVScale = 0.0f;
    float UOffset = 0.0f;
    float VOffset = 0.0f;

    public void Render(LandPatch landPatch, LandPatch landPatch2, LandPatch landPatch3, LandPatch landPatch4) {
        int n = this.vertsWide + 1 >> 1;
        QuadTreeNode quadTreeNode = this.root.GetChild(0);
        QuadTreeNode quadTreeNode2 = this.root.GetChild(1);
        QuadTreeNode quadTreeNode3 = this.root.GetChild(2);
        QuadTreeNode quadTreeNode4 = this.root.GetChild(3);
        if (quadTreeNode != null) {
            quadTreeNode.RecursRender(this, 0, 0, n, landPatch != null ? landPatch.root.GetChild(2) : null, quadTreeNode3, quadTreeNode2, landPatch4 != null ? landPatch4.root.GetChild(1) : null);
        }
        if (quadTreeNode2 != null) {
            quadTreeNode2.RecursRender(this, n, 0, n, landPatch != null ? landPatch.root.GetChild(3) : null, quadTreeNode4, landPatch3 != null ? landPatch3.root.GetChild(0) : null, quadTreeNode);
        }
        if (quadTreeNode3 != null) {
            quadTreeNode3.RecursRender(this, 0, n, n, quadTreeNode, landPatch2 != null ? landPatch2.root.GetChild(0) : null, quadTreeNode4, landPatch4 != null ? landPatch4.root.GetChild(3) : null);
        }
        if (quadTreeNode4 != null) {
            quadTreeNode4.RecursRender(this, n, n, n, quadTreeNode2, landPatch2 != null ? landPatch2.root.GetChild(1) : null, landPatch3 != null ? landPatch3.root.GetChild(2) : null, quadTreeNode3);
        }
    }

    public float PointHeight(int n, int n2) {
        try {
            if (n2 > this.vertsHigh || n > this.vertsWide) {
                return 0.0f;
            }
            return Main.MainRef.island.HeightMap[n + this.topleftX][this.topleftY + this.vertsHigh - n2];
        }
        catch (Exception exception) {
            return 0.0f;
        }
    }

    public void ResetData() {
        this.meshVertIdx = 0;
        this.meshFaceIdx = 0;
        this.m_nNextQuad = 0;
    }

    public boolean Create(String string, float f, float f2, int n, int n2, float f3, float f4, float f5) {
        this.UVScale = f3;
        this.UOffset = f4;
        this.VOffset = f5;
        this.topleftX = n;
        this.topleftY = n2;
        this.positionX = f;
        this.positionZ = f2;
        this.root = new QuadTreeNode();
        return true;
    }

    public void Seam(LandPatch landPatch, LandPatch landPatch2, LandPatch landPatch3, LandPatch landPatch4) {
        int n = this.vertsWide + 1 >> 1;
        QuadTreeNode quadTreeNode = this.root.GetChild(0);
        QuadTreeNode quadTreeNode2 = this.root.GetChild(1);
        QuadTreeNode quadTreeNode3 = this.root.GetChild(2);
        QuadTreeNode quadTreeNode4 = this.root.GetChild(3);
        if (quadTreeNode != null) {
            quadTreeNode.RecursSeam(this, 0, 0, n, landPatch != null ? landPatch.root.GetChild(2) : null, quadTreeNode3, quadTreeNode2, landPatch4 != null ? landPatch4.root.GetChild(1) : null);
        }
        if (quadTreeNode2 != null) {
            quadTreeNode2.RecursSeam(this, n, 0, n, landPatch != null ? landPatch.root.GetChild(3) : null, quadTreeNode4, landPatch3 != null ? landPatch3.root.GetChild(0) : null, quadTreeNode);
        }
        if (quadTreeNode3 != null) {
            quadTreeNode3.RecursSeam(this, 0, n, n, quadTreeNode, landPatch2 != null ? landPatch2.root.GetChild(0) : null, quadTreeNode4, landPatch4 != null ? landPatch4.root.GetChild(3) : null);
        }
        if (quadTreeNode4 != null) {
            quadTreeNode4.RecursSeam(this, n, n, n, quadTreeNode2, landPatch2 != null ? landPatch2.root.GetChild(1) : null, landPatch3 != null ? landPatch3.root.GetChild(2) : null, quadTreeNode3);
        }
    }

    public int GetVertsWide() {
        return this.vertsWide + 1;
    }

    public int AddFace(int n, int n2, int n3) {
        int n4 = this.meshFaceIdx * 3;
        if (n4 + 3 > this.faces.length) {
            int[] nArray = new int[this.faces.length + 300];
            System.arraycopy(this.faces, 0, nArray, 0, this.faces.length);
            this.faces = nArray;
        }
        this.faces[n4] = n3;
        this.faces[n4 + 1] = n2;
        this.faces[n4 + 2] = n;
        return this.meshFaceIdx++;
    }

    public void showMesh() {
        if (!this.Visible) {
            this.Visible = true;
            Main.MainRef.island.Map.addObject((WTContainer)this.meshGroupA);
            this.meshGroupA.setPosition(this.positionX, 0.0f, this.positionZ);
        }
    }

    public void UpdateArrays() {
        if (tuvidx == null || tuvidx.length <= this.meshVertIdx) {
            tuvidx = new int[this.meshVertIdx * 2];
            int n = 0;
            while (n < tuvidx.length) {
                LandPatch.tuvidx[n] = n;
                ++n;
            }
        }
    }

    public void Reset(Island island) {
        if (!this.Created) {
            this.Created = true;
            this.sizeScale = island.VertexScale;
            this.vertsWide = island.SubWidth;
            this.vertsHigh = island.SubHeight;
            this.vertexMap = new char[(island.SubWidth + 2) * (island.SubWidth + 2)];
            this.verts = new float[3];
            this.uvs = new float[2];
            this.norms = new float[3];
            this.faces = new int[3];
            this.m_aQuadPool = new QuadTreeNode[100];
            this.m_ChildStack = new Stack();
        }
        if (this.meshA != null) {
            this.meshA.clear();
        }
        this.m_nNextQuad = 0;
        this.meshVertIdx = 0;
        this.meshFaceIdx = 0;
        this.root.Reset(this);
        int n = this.vertexMap.length;
        int n2 = 0;
        while (n2 < n) {
            this.vertexMap[n2] = 65535;
            ++n2;
        }
        if (this.meshGroupA == null) {
            this.meshGroupA = Main.MainRef.Wt.createGroup();
            this.meshA = Main.MainRef.Wt.createBlankMesh();
        }
        this.ResetData();
        this.root.RecursTessellate(this, 2, 0, 0, this.vertsWide + 1);
    }

    public void UpdateMesh() {
        this.UpdateArrays();
        this.meshA.addVertices(this.verts, this.norms, this.meshVertIdx * 3, 0, 0);
        this.meshA.setVerticesUVs(tuvidx, this.uvs, this.meshVertIdx, 0, 0);
        this.meshA.addFaces(this.faces, this.meshFaceIdx * 3, 0);
        this.meshGroupA.attach((WTObject)this.meshA);
        this.meshA.setSurfaceShader(Main.MainRef.island.TerrainShader);
    }

    public int AddVertex(int n, float f, int n2) {
        int n3 = n + n2 * (this.vertsWide + 1);
        if (this.vertexMap[n3] != '\uffff') {
            return this.vertexMap[n3];
        }
        int n4 = this.meshVertIdx * 3;
        if (n4 + 3 > this.verts.length) {
            float[] fArray = new float[this.verts.length + 300];
            System.arraycopy(this.verts, 0, fArray, 0, this.verts.length);
            this.verts = fArray;
            float[] fArray2 = new float[this.norms.length + 300];
            System.arraycopy(this.norms, 0, fArray2, 0, this.norms.length);
            this.norms = fArray2;
            float[] fArray3 = new float[this.uvs.length + 300];
            System.arraycopy(this.uvs, 0, fArray3, 0, this.uvs.length);
            this.uvs = fArray3;
        }
        Main.MainRef.island.calculateVertexNormal(TempVec, n + this.topleftX, this.topleftY + this.vertsHigh - n2);
        float f2 = (float)n * this.sizeScale;
        float f3 = (float)n2 * this.sizeScale;
        this.verts[n4] = f2;
        this.norms[n4] = LandPatch.TempVec.X;
        this.verts[n4 + 1] = f;
        this.norms[n4 + 1] = LandPatch.TempVec.Y;
        this.verts[n4 + 2] = f3;
        this.norms[n4 + 2] = LandPatch.TempVec.Z;
        n4 = this.meshVertIdx * 2;
        float f4 = this.UOffset + (float)n / (float)this.vertsWide * this.UVScale;
        float f5 = this.VOffset + (float)(this.vertsHigh - n2) / (float)this.vertsHigh * this.UVScale;
        this.uvs[n4] = f4;
        this.uvs[n4 + 1] = f5;
        this.vertexMap[n3] = (char)this.meshVertIdx;
        return this.meshVertIdx++;
    }

    public void Dispose() {
        if (this.meshA != null) {
            this.meshA.clear();
        }
        this.hideMesh();
        if (this.m_ChildStack != null) {
            this.m_ChildStack.removeAllElements();
        }
        this.vertexMap = null;
        this.m_ChildStack = null;
        this.isLoaded = false;
        this.verts = null;
        this.uvs = null;
        this.norms = null;
        this.faces = null;
        this.m_aQuadPool = null;
        tuvidx = null;
        this.meshA = null;
        this.meshGroupA = null;
    }

    public void hideMesh() {
        if (this.Visible) {
            this.Visible = false;
            Main.MainRef.island.Map.removeObject((WTContainer)this.meshGroupA);
        }
    }

    public int GetVertsHigh() {
        return this.vertsHigh + 1;
    }
}

