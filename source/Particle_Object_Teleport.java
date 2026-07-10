/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Object_Teleport
extends Particle_Object
implements Global {
    VEC3D Pos = new VEC3D();
    float LifeTime = 0.0f;
    float update = 0.0f;
    float scale = 1.0f;

    public Particle_Object_Teleport(float f, float f2, float f3) {
        this.X = f;
        this.Y = f2 - 2.0f;
        this.Z = f3;
        this.Pos.fill(0.0f, 0.0f, 2.0f);
        Main.MainRef.ParticleList.add(new Particle_Object_SmokeColumn(0, this.X, this.Y - 4.0f, this.Z, false, 0.5f));
        Particle_Object.Temporary.fill(0.0f, 0.0f, 20.0f);
        int n = 0;
        do {
            Particle_Object.Temporary.rotateY(Library_Math.degreesToRadians(30.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Stars.getNext(this.X, this.Y, this.Z, Particle_Object.Temporary.X, 10.0f, Particle_Object.Temporary.Z, 1.0f + Main.MainRef.random.nextFloat() * 2.0f));
            Main.MainRef.ParticleList.add(Main.MainRef.Sparkles.getNext(this.X + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, this.Y + Main.MainRef.random.nextFloat() * 2.0f, this.Z + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 5.0f, 2.0f + Main.MainRef.random.nextFloat() * 5.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 1.0f));
        } while (++n < 12);
    }

    public void destroy() {
    }

    public void updateTimeSlice(float f) {
        this.Pos.rotateY(Library_Math.degreesToRadians(f * 500.0f));
        this.scale += f * 2.5f;
        this.update += f;
        if (this.update > 0.01f) {
            this.update = 0.0f;
            Main.MainRef.ParticleList.add(new Particle_Object_Ray(this.X + this.Pos.X * this.scale, this.Z + this.Pos.Z * this.scale, 0.0f, 1.0f, 0.0f, 2.0f + Main.MainRef.random.nextFloat() * 2.0f, false));
        }
        this.LifeTime += f;
        if (this.LifeTime > 1.5f) {
            Main.MainRef.ParticleList.remove(this);
            return;
        }
    }
}

