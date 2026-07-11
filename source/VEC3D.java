/*
 * Decompiled with CFR 0.152.
 */
public final class VEC3D {
    public float X = 0.0f;
    public float Y = 0.0f;
    public float Z = 0.0f;
    static float l = 0.0f;

    public final void subtract(VEC3D vEC3D) {
        this.X -= vEC3D.X;
        this.Y -= vEC3D.Y;
        this.Z -= vEC3D.Z;
    }

    public final void Scale(float f) {
        this.X *= f;
        this.Y *= f;
        this.Z *= f;
    }

    public static final void subtract(VEC3D vEC3D, VEC3D vEC3D2, VEC3D vEC3D3) {
        vEC3D3.X = vEC3D.X - vEC3D2.X;
        vEC3D3.Y = vEC3D.Y - vEC3D2.Y;
        vEC3D3.Z = vEC3D.Z - vEC3D2.Z;
    }

    public final void rotateY(float f) {
        float f2 = this.X;
        float f3 = this.Z;
        this.X = (float)((double)f2 * Math.cos(f) + (double)f3 * Math.sin(f));
        this.Z = (float)((double)(-f2) * Math.sin(f) + (double)f3 * Math.cos(f));
    }

    public final void normalize() {
        l = (float)Math.sqrt(this.X * this.X + this.Y * this.Y + this.Z * this.Z);
        if (l == 0.0f) {
            return;
        }
        this.X /= l;
        this.Y /= l;
        this.Z /= l;
    }

    public final float getX() {
        return this.X;
    }

    public final void setX(float f) {
        this.X = f;
    }

    public final void normalize(float f) {
        if (f == 0.0f) {
            return;
        }
        this.X /= f;
        this.Y /= f;
        this.Z /= f;
    }

    public static float dot(VEC3D vEC3D, VEC3D vEC3D2) {
        return vEC3D.X * vEC3D2.X + vEC3D.Y * vEC3D2.Y + vEC3D.Z * vEC3D2.Z;
    }

    public final float element(int n) {
        switch (n) {
            case 0: {
                return this.X;
            }
            case 1: {
                return this.Y;
            }
            case 2: {
                return this.Z;
            }
        }
        return 0.0f;
    }

    public float dot(VEC3D vEC3D) {
        return this.X * vEC3D.X + this.Y * vEC3D.Y + this.Z * vEC3D.Z;
    }

    public final void Set(float f, float f2, float f3) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
    }

    public final void Add(VEC3D vEC3D) {
        this.X += vEC3D.X;
        this.Y += vEC3D.Y;
        this.Z += vEC3D.Z;
    }

    public final void add(float f, float f2, float f3) {
        this.X += f;
        this.Y += f2;
        this.Z += f3;
    }

    public final void add(VEC3D vEC3D) {
        this.X += vEC3D.X;
        this.Y += vEC3D.Y;
        this.Z += vEC3D.Z;
    }

    public static final void add(VEC3D vEC3D, VEC3D vEC3D2, VEC3D vEC3D3) {
        vEC3D3.X = vEC3D.X + vEC3D2.X;
        vEC3D3.Y = vEC3D.Y + vEC3D2.Y;
        vEC3D3.Z = vEC3D.Z + vEC3D2.Z;
    }

    public final void Copy(VEC3D vEC3D) {
        this.X = vEC3D.X;
        this.Y = vEC3D.Y;
        this.Z = vEC3D.Z;
    }

    public final void fill(float f, float f2, float f3) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
    }

    public final void rotateX(float f) {
        float f2 = this.Y;
        float f3 = this.Z;
        this.Y = (float)((double)f2 * Math.cos(f) + (double)f3 * Math.sin(f));
        this.Z = (float)((double)(-f2) * Math.sin(f) + (double)f3 * Math.cos(f));
    }

    public float angleBetween(VEC3D vEC3D) {
        return (float)Math.acos(this.X * vEC3D.X + this.Y * vEC3D.Y + this.Z * vEC3D.Z / (this.length() * vEC3D.length()));
    }

    public float angleBetween(float f, float f2, float f3) {
        return (float)Math.acos(this.X * f + this.Y * f2 + this.Z * f3);
    }

    public final void Sub(VEC3D vEC3D) {
        this.X -= vEC3D.X;
        this.Y -= vEC3D.Y;
        this.Z -= vEC3D.Z;
    }

    float distanceToLine(VEC3D vEC3D, VEC3D vEC3D2) {
        float f = Library_Math.distance3D(vEC3D2.X, vEC3D2.Y, vEC3D2.Z, vEC3D.X, vEC3D.Y, vEC3D.Z);
        float f2 = ((this.X - vEC3D.X) * (vEC3D2.X - vEC3D.X) + (this.Y - vEC3D.Y) * (vEC3D2.Y - vEC3D.Y) + (this.Z - vEC3D.Z) * (vEC3D2.Z - vEC3D.Z)) / (f * f);
        if (f2 < 0.0f || f2 > 1.0f) {
            return 1000000.0f;
        }
        float f3 = vEC3D.X + f2 * (vEC3D2.X - vEC3D.X);
        float f4 = vEC3D.Y + f2 * (vEC3D2.Y - vEC3D.Y);
        float f5 = vEC3D.Z + f2 * (vEC3D2.Z - vEC3D.Z);
        return Library_Math.distance3D(this.X, this.Y, this.Z, f3, f4, f5);
    }

    public final void Invert() {
        this.X *= -1.0f;
        this.Y *= -1.0f;
        this.Z *= -1.0f;
    }

    public final float length() {
        return (float)Math.sqrt(this.X * this.X + this.Y * this.Y + this.Z * this.Z);
    }

    public final float getZ() {
        return this.Z;
    }

    public final void setZ(float f) {
        this.Z = f;
    }

    public final void MakeCrossProduct(VEC3D vEC3D, VEC3D vEC3D2) {
        this.X = vEC3D.Y * vEC3D2.Z - vEC3D.Z * vEC3D2.Y;
        this.Y = vEC3D.Z * vEC3D2.X - vEC3D.X * vEC3D2.Z;
        this.Z = vEC3D.X * vEC3D2.Y - vEC3D.Y * vEC3D2.X;
    }

    public final float Lengthsquared() {
        return this.X * this.X + this.Y * this.Y + this.Z * this.Z;
    }

    public static final void addScaled(VEC3D vEC3D, VEC3D vEC3D2, float f, VEC3D vEC3D3) {
        vEC3D3.X = vEC3D.X + vEC3D2.X * f;
        vEC3D3.Y = vEC3D.Y + vEC3D2.Y * f;
        vEC3D3.Z = vEC3D.Z + vEC3D2.Z * f;
    }

    public final boolean isEqual(VEC3D vEC3D) {
        return this.X == vEC3D.X && this.Y == vEC3D.Y && this.Z == vEC3D.Z;
    }

    public final void addScaled(VEC3D vEC3D, float f) {
        this.X += vEC3D.X * f;
        this.Y += vEC3D.Y * f;
        this.Z += vEC3D.Z * f;
    }

    public VEC3D() {
        this.Z = 0.0f;
        this.Y = 0.0f;
        this.X = 0.0f;
    }

    public VEC3D(float f, float f2, float f3) {
        this.X = f;
        this.Y = f2;
        this.Z = f3;
    }

    public VEC3D(VEC3D vEC3D) {
        this.X = vEC3D.X;
        this.Y = vEC3D.Y;
        this.Z = vEC3D.Z;
    }

    public final void Multiply(VEC3D vEC3D) {
        this.X *= vEC3D.X;
        this.Y *= vEC3D.Y;
        this.Z *= vEC3D.Z;
    }

    public final void multiply(float f) {
        this.X *= f;
        this.Y *= f;
        this.Z *= f;
    }

    public final void multiply(VEC3D vEC3D) {
        this.X *= vEC3D.X;
        this.Y *= vEC3D.Y;
        this.Z *= vEC3D.Z;
    }

    public final void rotateZ(float f) {
        float f2 = this.Y;
        float f3 = this.X;
        this.X = (float)((double)f3 * Math.cos(f) + (double)f2 * Math.sin(f));
        this.Y = (float)((double)(-f3) * Math.sin(f) + (double)f2 * Math.cos(f));
    }

    public final float Length() {
        return (float)Math.sqrt(this.X * this.X + this.Y * this.Y + this.Z * this.Z);
    }

    public final float SquaredLength() {
        return this.X * this.X + this.Y * this.Y + this.Z * this.Z;
    }

    public final float getY() {
        return this.Y;
    }

    public final void setY(float f) {
        this.Y = f;
    }

    public final void Normalize() {
        this.normalize(this.Length());
    }

    public final void Divide(VEC3D vEC3D) {
        this.X = vEC3D.X != 0.0f ? (this.X /= vEC3D.X) : 0.0f;
        this.Y = vEC3D.Y != 0.0f ? (this.Y /= vEC3D.Y) : 0.0f;
        if (vEC3D.Z != 0.0f) {
            this.Z /= vEC3D.Z;
            return;
        }
        vEC3D.Z = 0.0f;
    }

    public final void divide(float f) {
        this.X /= f;
        this.Y /= f;
        this.Z /= f;
    }

    public final void setEqual(VEC3D vEC3D) {
        this.X = vEC3D.X;
        this.Y = vEC3D.Y;
        this.Z = vEC3D.Z;
    }

    public final void subtract(float f, float f2, float f3) {
        this.X -= f;
        this.Y -= f2;
        this.Z -= f3;
    }
}

