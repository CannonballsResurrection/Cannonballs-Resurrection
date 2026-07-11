/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 */
import wildtangent.webdriver.WTConstants;

public class Particle_Object_SmokeColumn
extends Particle_Object
implements Global,
WTConstants {
    float LifeTime = 0.0f;
    boolean Fire;
    int Type = 0;
    boolean OnGround = false;
    boolean placed = false;

    public Particle_Object_SmokeColumn(int n, float f, float f2, float f3, boolean bl, float f4) {
        this.Type = n;
        this.Fire = bl;
        this.LifeTime = f4;
        this.X = f;
        this.Y = f2;
        this.Z = f3;
    }

    public void destroy() {
    }

    public void updateTimeSlice(float f) {
        if (this.OnGround && (Main.MainRef.island.TerrainChanged || !this.placed)) {
            if (!this.placed) {
                this.placed = true;
            }
            this.Y = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            if (this.Y <= 0.0f) {
                Main.MainRef.ParticleList.remove(this);
                return;
            }
        }
        if (this.LifeTime != 999.0f) {
            this.LifeTime -= f;
            if (this.LifeTime <= 0.0f) {
                Main.MainRef.ParticleList.remove(this);
                return;
            }
        }
        if (this.isOnscreen(this.X, this.Y, this.Z)) {
            if (Main.MainRef.random.nextFloat() < 0.7f) {
                if (this.Type == 0) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, Main.MainRef.random.nextFloat() * 3.0f + 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, Main.MainRef.random.nextFloat() * 0.8f + 0.6f));
                } else {
                    Main.MainRef.ParticleList.add(Main.MainRef.SmokeBlack.getNext(this.X, this.Y, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, Main.MainRef.random.nextFloat() * 3.0f + 10.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 3.0f, Main.MainRef.random.nextFloat() * 0.8f + 0.6f));
                }
            }
            if (this.Fire) {
                if (Main.MainRef.random.nextFloat() < 0.1f) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Explosions.getNext(this.X, this.Y, this.Z, 0.0f, 4.0f + Main.MainRef.random.nextFloat() * 5.0f, 0.0f, 4.0f + Main.MainRef.random.nextFloat() * 2.0f));
                }
                if (Main.MainRef.random.nextFloat() < 0.1f) {
                    Main.MainRef.ParticleList.add(Main.MainRef.Sparkles.getNext(this.X + (Main.MainRef.random.nextFloat() - 0.5f) * 2.0f, this.Y, this.Z + (Main.MainRef.random.nextFloat() - 0.5f) * 2.0f, Main.MainRef.random.nextFloat() - 0.5f, 2.0f + Main.MainRef.random.nextFloat() * 5.0f, Main.MainRef.random.nextFloat() - 0.5f, 1.0f));
                }
            }
        }
    }
}

