/*
 * Decompiled with CFR 0.152.
 */
public class Library_Math {
    public static float Pi = (float)Math.PI;

    public static float camDistance3DCheap(float f, float f2, float f3) {
        return 0.5f * ((Main.MainRef.camera.X - f) * (Main.MainRef.camera.X - f)) + (Main.MainRef.camera.Y - f2) * (Main.MainRef.camera.Y - f2) + (Main.MainRef.camera.Z - f3) * (Main.MainRef.camera.Z - f3);
    }

    public static float distance3D(float f, float f2, float f3, float f4, float f5, float f6) {
        return (float)Math.sqrt((f - f4) * (f - f4) + (f2 - f5) * (f2 - f5) + (f3 - f6) * (f3 - f6));
    }

    public static float radiansToDegrees(float f) {
        return f * 57.295578f;
    }

    public static float distance3DCheap(float f, float f2, float f3, float f4, float f5, float f6) {
        return (f - f4) * (f - f4) + (f2 - f5) * (f2 - f5) + (f3 - f6) * (f3 - f6);
    }

    public static float camDistance3D(float f, float f2, float f3) {
        return (float)Math.sqrt((Main.MainRef.camera.X - f) * (Main.MainRef.camera.X - f) + (Main.MainRef.camera.Y - f2) * (Main.MainRef.camera.Y - f2) + (Main.MainRef.camera.Z - f3) * (Main.MainRef.camera.Z - f3));
    }

    public static float degreesToRadians(float f) {
        return Pi * f / 180.0f;
    }

    public static float distance(float f, float f2, float f3, float f4) {
        return (float)Math.sqrt((f - f3) * (f - f3) + (f2 - f4) * (f2 - f4));
    }
}

