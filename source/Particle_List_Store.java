/*
 * Decompiled with CFR 0.152.
 */
public class Particle_List_Store {
    public Particle_Object Root = null;
    private Particle_Object Update;
    int Particle_Count = 0;

    public Particle_Object add(Particle_Object particle_Object) {
        ++this.Particle_Count;
        if (this.Root != null) {
            particle_Object.Next2 = this.Root;
            particle_Object.Last2 = null;
            this.Root.Last2 = particle_Object;
            this.Root = particle_Object;
        } else {
            this.Root = particle_Object;
            this.Root.Next2 = null;
            this.Root.Last2 = null;
        }
        return particle_Object;
    }

    Particle_Object getNext(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        if (this.Update == null) {
            this.Update = this.Root;
        } else {
            this.Update = this.Update.Next2;
            if (this.Update == null) {
                this.Update = this.Root;
            }
        }
        if (this.Update.Active) {
            Main.MainRef.ParticleList.removeFromList(this.Update);
        }
        this.Update.activate(f, f2, f3, f4, f5, f6, f7);
        return this.Update;
    }

    public void destroy() {
        Particle_Object particle_Object = this.Root;
        Particle_Object particle_Object2 = null;
        while (particle_Object != null) {
            particle_Object2 = particle_Object.Next2;
            particle_Object.destroy();
            particle_Object.Next2 = null;
            particle_Object.Last2 = null;
            particle_Object = null;
            particle_Object = particle_Object2;
        }
        this.Root = null;
        particle_Object2 = null;
        particle_Object = null;
        this.Particle_Count = 0;
    }

    Particle_Object_Chunk getNextChunk(int n, boolean bl, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, boolean bl2) {
        if (this.Update == null) {
            this.Update = this.Root;
        } else {
            this.Update = this.Update.Next2;
            if (this.Update == null) {
                this.Update = this.Root;
            }
        }
        if (this.Update.Active) {
            Main.MainRef.ParticleList.removeFromList(this.Update);
        }
        ((Particle_Object_Chunk)this.Update).activate(n, bl, f, f2, f3, f4, f5, f6, f7, f8, bl2);
        return (Particle_Object_Chunk)this.Update;
    }

    public void remove(Particle_Object particle_Object) {
        this.Particle_Count += -1;
        try {
            if (particle_Object == this.Root) {
                if (particle_Object.Next2 != null) {
                    particle_Object.Next2.Last2 = null;
                }
                if (particle_Object.Next2 != null) {
                    this.Root = particle_Object.Next2;
                    this.Root.Last2 = null;
                } else {
                    this.Root = null;
                }
                particle_Object.destroy();
                particle_Object = null;
                return;
            }
            if (particle_Object.Last2 != null) {
                particle_Object.Last2.Next2 = particle_Object.Next2;
            }
            if (particle_Object.Next2 != null) {
                particle_Object.Next2.Last2 = particle_Object.Last2;
            }
            particle_Object.destroy();
            particle_Object = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING PARTICLE FROM LIST");
            return;
        }
    }
}

