/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTGroup;

public class Chunk_Object
implements Global {
    Chunk_Object Next;
    Chunk_Object Last;
    WTGroup Group;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    float TrajectoryX = 0.0f;
    float TrajectoryY = 0.0f;
    float TrajectoryZ = 0.0f;
    float ChunkSpeed = 0.0f;
    float MaxLifeSpan = 6.0f;
    float LifeSpan = 0.0f;
    Media_Object_Actor Actor;
    float Opacity = 255.0f;

    public Chunk_Object(Media_Object_Actor media_Object_Actor, float f, float f2, float f3, float f4, float f5, float f6) {
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.addObject((WTContainer)media_Object_Actor.Model);
        this.Actor = media_Object_Actor;
        Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Group);
        this.X = f;
        this.Y = f2;
        this.Z = f3;
        this.TrajectoryX = f4;
        this.TrajectoryY = f5;
        this.TrajectoryZ = f6;
        this.Group.setPosition(this.X, this.Y, this.Z);
        this.LifeSpan = 0.0f;
        this.MaxLifeSpan = 4.0f + Main.MainRef.random.nextFloat() * 3.0f;
        this.Group.setConstantRotation(Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, (Main.MainRef.random.nextFloat() - 0.5f) * 50.0f);
    }

    public void destroy() {
        if (this.Actor != null) {
            this.Group.removeObject((WTContainer)this.Actor.Model);
            Main.MainRef.MediaList.remove(this.Actor);
            this.Actor = null;
        }
        Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Group);
        this.Group = null;
    }

    public void updateTimeSlice(float f) {
        this.X += this.TrajectoryX * f;
        this.Y += this.TrajectoryY * f;
        this.Z += this.TrajectoryZ * f;
        this.TrajectoryY += -32.0f * f;
        if (this.TrajectoryY < 0.0f) {
            this.Opacity -= f * 100.0f;
        }
        if (this.Opacity < 0.0f) {
            Main.MainRef.ChunkList.remove(this);
            return;
        }
        if (this.TrajectoryY < 0.0f) {
            this.Actor.Model.setOpacity((int)this.Opacity);
        }
        this.Group.setPosition(this.X, this.Y, this.Z);
        float f2 = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y <= f2) {
            if (f2 < 1.0f) {
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_Splash(this.X, 0.0f, this.Z, (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, 20.0f + (Main.MainRef.random.nextFloat() - 0.5f) * 15.0f, (Main.MainRef.random.nextFloat() - 0.5f) * 10.0f, 0.5f + Main.MainRef.random.nextFloat() * 0.5f));
                Main.MainRef.ParticleList.add(new Particle_Object_SplashRing(this.X, 0.11f, this.Z, 3.0f, 2.0f));
                Main.MainRef.ChunkList.remove(this);
                return;
            }
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            }
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 2.0f + 4.0f));
            Main.MainRef.ChunkList.remove(this);
            return;
        }
        this.LifeSpan += f;
        if (this.LifeSpan > this.MaxLifeSpan) {
            if (Main.MainRef.random.nextFloat() < 0.5f) {
                Main.MainRef.GlobalMedia.Sound_Puff.playDepth(false, Library_Math.camDistance3D(this.X, this.Y, this.Z));
            }
            Main.MainRef.ParticleList.add(Main.MainRef.Smoke.getNext(this.X, this.Y, this.Z, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() - 0.5f, Main.MainRef.random.nextFloat() * 2.0f + 4.0f));
            Main.MainRef.ChunkList.remove(this);
            return;
        }
    }
}

