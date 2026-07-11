/*
 * Decompiled with CFR 0.152.
 */
public final class QuadTreeNode {
    private static float DESIRED_RESOLUTION = 0.0025f;
    private static int EXCLUDE_WIDTH = 8;
    private static int MAX_QUAD_WIDTH = 32;
    protected static int MIN_QUAD_WIDTH = 2;
    protected static final int ChildNW = 0;
    protected static final int ChildNE = 1;
    protected static final int ChildSW = 2;
    protected static final int ChildSE = 3;
    private static int vertIdx = 0;
    private static int hVertIdx = 0;
    private static int[] vertIndices = new int[9];
    private QuadTreeNode[] Children = null;
    private float m_fBlend = 0.0f;
    private boolean m_bDone = false;
    private LandPatch nodeParent = null;

    private boolean SplitMetric(int n, int n2, int n3) {
        int n4 = n3 >> 1;
        if (n4 == 1) {
            this.m_fBlend = 1.0f;
            return false;
        }
        if (n3 >= MAX_QUAD_WIDTH) {
            this.m_fBlend = 0.0f;
            return true;
        }
        float f = this.nodeParent.PointHeight(n, n2);
        float f2 = this.nodeParent.PointHeight(n + n3, n2);
        float f3 = this.nodeParent.PointHeight(n, n2 + n3);
        float f4 = this.nodeParent.PointHeight(n + n3, n2 + n3);
        float f5 = this.nodeParent.PointHeight(n + n4, n2 + n4);
        float f6 = Math.abs(this.nodeParent.PointHeight(n + n4, n2) - (f + f2) * 0.5f);
        float f7 = Math.abs(this.nodeParent.PointHeight(n + n4, n2 + n3) - (f3 + f4) * 0.5f);
        float f8 = Math.abs(this.nodeParent.PointHeight(n + n3, n2 + n4) - (f2 + f4) * 0.5f);
        float f9 = Math.abs(this.nodeParent.PointHeight(n, n2 + n4) - (f + f3) * 0.5f);
        float f10 = Math.abs(f5 - (f4 + f2) * 0.5f);
        float f11 = Math.abs(f5 - (f + f3) * 0.5f);
        float f12 = 0.0f;
        f12 = Math.max(f12, f6);
        f12 = Math.max(f12, f7);
        f12 = Math.max(f12, f8);
        f12 = Math.max(f12, f9);
        f12 = Math.max(f12, f10);
        if ((f12 = Math.max(f12, f11)) > 3.0f) {
            this.m_fBlend = 0.0f;
            return true;
        }
        return false;
    }

    public void Reset(LandPatch landPatch) {
    }

    public void RecursTessellate(LandPatch landPatch, int n, int n2, int n3, int n4) {
        this.nodeParent = landPatch;
        if (this.Children != null) {
            landPatch.m_ChildStack.push(this.Children);
            this.Children = null;
        }
        this.m_bDone = false;
        int n5 = (n4 >> 1) + 1;
        if (this.SplitMetric(n2, n3, n4)) {
            this.Split(landPatch, n2, n3, n5);
            if (this.Children != null) {
                this.Children[0].RecursTessellate(landPatch, n, n2, n3, n5);
                this.Children[1].RecursTessellate(landPatch, n, n2 + n5, n3, n5);
                this.Children[2].RecursTessellate(landPatch, n, n2, n3 + n5, n5);
                this.Children[3].RecursTessellate(landPatch, n, n2 + n5, n3 + n5, n5);
            }
        }
    }

    private final void MakeVertex(LandPatch landPatch, int n, int n2, float f) {
        int n3;
        QuadTreeNode.vertIndices[QuadTreeNode.vertIdx] = n3 = landPatch.AddVertex(n, f, n2);
        ++vertIdx;
    }

    private final void MakeBlendedVertex(LandPatch landPatch, int n, int n2, float f, float f2, float f3) {
        float f4 = (1.0f - f3) * f2 + f3 * f;
        this.MakeVertex(landPatch, n, n2, f4);
    }

    protected final QuadTreeNode GetChild(int n) {
        if (this.Children != null) {
            return this.Children[n];
        }
        return null;
    }

    public void Dispose() {
    }

    public void RecursRender(LandPatch landPatch, int n, int n2, int n3, QuadTreeNode quadTreeNode, QuadTreeNode quadTreeNode2, QuadTreeNode quadTreeNode3, QuadTreeNode quadTreeNode4) {
        int n4 = n3 >> 1;
        if (n4 == 0) {
            return;
        }
        if (this.Children != null) {
            QuadTreeNode quadTreeNode5 = null;
            QuadTreeNode quadTreeNode6 = null;
            QuadTreeNode quadTreeNode7 = null;
            QuadTreeNode quadTreeNode8 = null;
            quadTreeNode5 = quadTreeNode != null ? quadTreeNode.GetChild(2) : null;
            quadTreeNode8 = quadTreeNode4 != null ? quadTreeNode4.GetChild(1) : null;
            this.Children[0].RecursRender(landPatch, n, n2, n4, quadTreeNode5, this.Children[2], this.Children[1], quadTreeNode8);
            quadTreeNode7 = quadTreeNode3 != null ? quadTreeNode3.GetChild(0) : null;
            quadTreeNode5 = quadTreeNode != null ? quadTreeNode.GetChild(3) : null;
            this.Children[1].RecursRender(landPatch, n + n4, n2, n4, quadTreeNode5, this.Children[3], quadTreeNode7, this.Children[0]);
            quadTreeNode6 = quadTreeNode2 != null ? quadTreeNode2.GetChild(0) : null;
            quadTreeNode8 = quadTreeNode4 != null ? quadTreeNode4.GetChild(3) : null;
            this.Children[2].RecursRender(landPatch, n, n2 + n4, n4, this.Children[0], quadTreeNode6, this.Children[3], quadTreeNode8);
            quadTreeNode6 = quadTreeNode2 != null ? quadTreeNode2.GetChild(1) : null;
            quadTreeNode7 = quadTreeNode3 != null ? quadTreeNode3.GetChild(2) : null;
            this.Children[3].RecursRender(landPatch, n + n4, n2 + n4, n4, this.Children[1], quadTreeNode6, quadTreeNode7, this.Children[2]);
            return;
        }
        if (!this.m_bDone) {
            this.m_bDone = true;
            vertIdx = 0;
            float f = landPatch.PointHeight(n, n2);
            float f2 = landPatch.PointHeight(n, n2 + n3);
            float f3 = landPatch.PointHeight(n + n3, n2);
            float f4 = landPatch.PointHeight(n + n3, n2 + n3);
            boolean bl = (n / n3 & 1) == 1;
            boolean bl2 = (n2 / n3 & 1) == 1;
            float f5 = bl && bl2 || !bl && !bl2 ? (1.0f - this.m_fBlend) * ((f + f4) * 0.5f) + this.m_fBlend * landPatch.PointHeight(n + n4, n2 + n4) : (1.0f - this.m_fBlend) * ((f3 + f2) * 0.5f) + this.m_fBlend * landPatch.PointHeight(n + n4, n2 + n4);
            this.MakeVertex(landPatch, n + n4, n2 + n4, f5);
            this.MakeVertex(landPatch, n, n2, f);
            if (quadTreeNode4 != null) {
                if (quadTreeNode4.GetChild(1) != null || quadTreeNode4.GetChild(3) != null) {
                    this.MakeVertex(landPatch, n, n2 + n4, landPatch.PointHeight(n, n2 + n4));
                } else {
                    this.MakeBlendedVertex(landPatch, n, n2 + n4, landPatch.PointHeight(n, n2 + n4), (f + f2) * 0.5f, this.m_fBlend < quadTreeNode4.m_fBlend ? this.m_fBlend : quadTreeNode4.m_fBlend);
                }
            }
            this.MakeVertex(landPatch, n, n2 + n3, f2);
            if (quadTreeNode2 != null) {
                if (quadTreeNode2.GetChild(1) != null || quadTreeNode2.GetChild(0) != null) {
                    this.MakeVertex(landPatch, n + n4, n2 + n3, landPatch.PointHeight(n + n4, n2 + n3));
                } else {
                    this.MakeBlendedVertex(landPatch, n + n4, n2 + n3, landPatch.PointHeight(n + n4, n2 + n3), (f2 + f4) * 0.5f, this.m_fBlend < quadTreeNode2.m_fBlend ? this.m_fBlend : quadTreeNode2.m_fBlend);
                }
            }
            this.MakeVertex(landPatch, n + n3, n2 + n3, f4);
            if (quadTreeNode3 != null) {
                if (quadTreeNode3.GetChild(0) != null || quadTreeNode3.GetChild(2) != null) {
                    this.MakeVertex(landPatch, n + n3, n2 + n4, landPatch.PointHeight(n + n3, n2 + n4));
                } else {
                    this.MakeBlendedVertex(landPatch, n + n3, n2 + n4, landPatch.PointHeight(n + n3, n2 + n4), (f3 + f4) * 0.5f, this.m_fBlend < quadTreeNode3.m_fBlend ? this.m_fBlend : quadTreeNode3.m_fBlend);
                }
            }
            this.MakeVertex(landPatch, n + n3, n2, f3);
            if (quadTreeNode != null) {
                if (quadTreeNode.GetChild(3) != null || quadTreeNode.GetChild(2) != null) {
                    this.MakeVertex(landPatch, n + n4, n2, landPatch.PointHeight(n + n4, n2));
                } else {
                    this.MakeBlendedVertex(landPatch, n + n4, n2, landPatch.PointHeight(n + n4, n2), (f3 + f) * 0.5f, this.m_fBlend < quadTreeNode.m_fBlend ? this.m_fBlend : quadTreeNode.m_fBlend);
                }
            }
            int n5 = 1;
            while (n5 < vertIdx - 1) {
                landPatch.AddFace(vertIndices[0], vertIndices[n5], vertIndices[n5 + 1]);
                ++n5;
            }
            landPatch.AddFace(vertIndices[0], vertIndices[vertIdx - 1], vertIndices[1]);
        }
    }

    public void Split(LandPatch landPatch, int n, int n2, int n3) {
        if (landPatch.m_aQuadPool != null) {
            if (landPatch.m_nNextQuad >= landPatch.m_aQuadPool.length) {
                QuadTreeNode[] quadTreeNodeArray = new QuadTreeNode[landPatch.m_aQuadPool.length + landPatch.POOL_SIZE];
                System.arraycopy(landPatch.m_aQuadPool, 0, quadTreeNodeArray, 0, landPatch.m_aQuadPool.length);
                landPatch.m_aQuadPool = quadTreeNodeArray;
            }
            if (this.Children == null) {
                this.Children = landPatch.m_ChildStack.isEmpty() ? new QuadTreeNode[4] : (QuadTreeNode[])landPatch.m_ChildStack.pop();
            }
            if (landPatch.m_aQuadPool[landPatch.m_nNextQuad] == null) {
                int n4 = MIN_QUAD_WIDTH;
                int n5 = landPatch.m_nNextQuad;
                int n6 = 0;
                while (n6 < 4) {
                    landPatch.m_aQuadPool[n5] = new QuadTreeNode();
                    ++n6;
                    ++n5;
                }
                MIN_QUAD_WIDTH = n4;
            }
            System.arraycopy(landPatch.m_aQuadPool, landPatch.m_nNextQuad, this.Children, 0, 4);
            int n7 = 0;
            do {
                this.Children[n7].nodeParent = landPatch;
                this.Children[n7].Children = null;
                this.Children[n7].m_bDone = false;
            } while (++n7 < 4);
            landPatch.m_nNextQuad += 4;
        }
    }

    public void RecursSeam(LandPatch landPatch, int n, int n2, int n3, QuadTreeNode quadTreeNode, QuadTreeNode quadTreeNode2, QuadTreeNode quadTreeNode3, QuadTreeNode quadTreeNode4) {
        int n4 = n3 >> 1;
        if (n4 == 0) {
            return;
        }
        if (this.Children != null) {
            boolean bl = this.Children[0].GetChild(0) != null;
            boolean bl2 = this.Children[1].GetChild(1) != null;
            boolean bl3 = this.Children[2].GetChild(2) != null;
            boolean bl4 = this.Children[3].GetChild(3) != null;
            QuadTreeNode quadTreeNode5 = null;
            if (quadTreeNode != null && (quadTreeNode5 = quadTreeNode.GetChild(2)) == null && (bl || bl2)) {
                quadTreeNode.Split(quadTreeNode.nodeParent, 0, 0, 0);
                quadTreeNode5 = quadTreeNode.GetChild(2);
            }
            QuadTreeNode quadTreeNode6 = null;
            if (quadTreeNode4 != null && (quadTreeNode6 = quadTreeNode4.GetChild(1)) == null && (bl || bl3)) {
                quadTreeNode4.Split(quadTreeNode4.nodeParent, 0, 0, 0);
                quadTreeNode6 = quadTreeNode4.GetChild(1);
            }
            QuadTreeNode quadTreeNode7 = null;
            if (quadTreeNode3 != null && (quadTreeNode7 = quadTreeNode3.GetChild(0)) == null && (bl2 || bl4)) {
                quadTreeNode3.Split(quadTreeNode3.nodeParent, 0, 0, 0);
                quadTreeNode7 = quadTreeNode3.GetChild(0);
            }
            QuadTreeNode quadTreeNode8 = null;
            if (quadTreeNode2 != null && (quadTreeNode8 = quadTreeNode2.GetChild(0)) == null && (bl3 || bl4)) {
                quadTreeNode2.Split(quadTreeNode2.nodeParent, 0, 0, 0);
                quadTreeNode8 = quadTreeNode2.GetChild(0);
            }
            quadTreeNode5 = quadTreeNode != null ? quadTreeNode.GetChild(2) : null;
            quadTreeNode6 = quadTreeNode4 != null ? quadTreeNode4.GetChild(1) : null;
            this.Children[0].RecursSeam(landPatch, n, n2, n4, quadTreeNode5, this.Children[2], this.Children[1], quadTreeNode6);
            quadTreeNode7 = quadTreeNode3 != null ? quadTreeNode3.GetChild(0) : null;
            quadTreeNode5 = quadTreeNode != null ? quadTreeNode.GetChild(3) : null;
            this.Children[1].RecursSeam(landPatch, n + n4, n2, n4, quadTreeNode5, this.Children[3], quadTreeNode7, this.Children[0]);
            quadTreeNode8 = quadTreeNode2 != null ? quadTreeNode2.GetChild(0) : null;
            quadTreeNode6 = quadTreeNode4 != null ? quadTreeNode4.GetChild(3) : null;
            this.Children[2].RecursSeam(landPatch, n, n2 + n4, n4, this.Children[0], quadTreeNode8, this.Children[3], quadTreeNode6);
            quadTreeNode8 = quadTreeNode2 != null ? quadTreeNode2.GetChild(1) : null;
            quadTreeNode7 = quadTreeNode3 != null ? quadTreeNode3.GetChild(2) : null;
            this.Children[3].RecursSeam(landPatch, n + n4, n2 + n4, n4, this.Children[1], quadTreeNode8, quadTreeNode7, this.Children[2]);
        }
    }
}

