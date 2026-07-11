/*
 * Decompiled with CFR 0.152.
 */
public class Particle_List {
    public Particle_Object Root = null;
    private Particle_Object Update;
    int Particle_Count = 0;

    public void hideAll() {
        Particle_Object particle_Object = this.Root;
        Particle_Object particle_Object2 = null;
        while (particle_Object != null) {
            particle_Object2 = particle_Object.Next;
            particle_Object.hide();
            particle_Object.Next = null;
            particle_Object.Last = null;
            particle_Object = null;
            particle_Object = particle_Object2;
        }
        this.Root = null;
        particle_Object2 = null;
        particle_Object = null;
        this.Particle_Count = 0;
    }

    public Particle_Object add(Particle_Object particle_Object) {
        if (particle_Object == null) {
            return null;
        }
        ++this.Particle_Count;
        if (this.Root != null) {
            particle_Object.Next = this.Root;
            particle_Object.Last = null;
            this.Root.Last = particle_Object;
            this.Root = particle_Object;
        } else {
            this.Root = particle_Object;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return particle_Object;
    }

    public void destroy() {
        Particle_Object particle_Object = this.Root;
        Particle_Object particle_Object2 = null;
        while (particle_Object != null) {
            particle_Object2 = particle_Object.Next;
            particle_Object.destroy();
            particle_Object.Next = null;
            particle_Object.Last = null;
            particle_Object = null;
            particle_Object = particle_Object2;
        }
        this.Root = null;
        particle_Object2 = null;
        particle_Object = null;
        this.Particle_Count = 0;
    }

    public void removeFromList(Particle_Object particle_Object) {
        this.Particle_Count += -1;
        try {
            if (particle_Object == this.Root) {
                if (particle_Object.Next != null) {
                    particle_Object.Next.Last = null;
                }
                if (particle_Object.Next != null) {
                    this.Root = particle_Object.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                if (particle_Object.FromStore) {
                    particle_Object.hide();
                    particle_Object.Active = false;
                } else {
                    particle_Object.destroy();
                }
                particle_Object = null;
                return;
            }
            if (particle_Object.Last != null) {
                particle_Object.Last.Next = particle_Object.Next;
            }
            if (particle_Object.Next != null) {
                particle_Object.Next.Last = particle_Object.Last;
            }
            if (particle_Object.FromStore) {
                particle_Object.hide();
                particle_Object.Active = false;
            } else {
                particle_Object.destroy();
            }
            particle_Object = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING PARTICLE FROM LIST");
            return;
        }
    }

    public void update(float f) {
        Particle_Object particle_Object = this.Root;
        Particle_Object particle_Object2 = null;
        while (particle_Object != null) {
            particle_Object2 = particle_Object.Next;
            particle_Object.updateTimeSlice(f);
            particle_Object = particle_Object2;
        }
    }

    public void removePermanent() {
        Particle_Object particle_Object = this.Root;
        Particle_Object particle_Object2 = null;
        while (particle_Object != null) {
            particle_Object2 = particle_Object.Next;
            if (particle_Object.FromStore) {
                this.removeFromList(particle_Object);
                particle_Object = null;
            }
            particle_Object = particle_Object2;
        }
        particle_Object2 = null;
        particle_Object = null;
        this.Particle_Count = 0;
    }

    public void remove(Particle_Object particle_Object) {
        this.Particle_Count += -1;
        try {
            if (particle_Object == this.Root) {
                if (particle_Object.Next != null) {
                    particle_Object.Next.Last = null;
                }
                if (particle_Object.Next != null) {
                    this.Root = particle_Object.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                particle_Object.destroy();
                particle_Object = null;
                return;
            }
            if (particle_Object.Last != null) {
                particle_Object.Last.Next = particle_Object.Next;
            }
            if (particle_Object.Next != null) {
                particle_Object.Next.Last = particle_Object.Last;
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

