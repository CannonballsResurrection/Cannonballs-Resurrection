/*
 * Decompiled with CFR 0.152.
 */
public class Particle_Plane_List {
    private Particle_Plane Root = null;
    private Particle_Plane Update;
    int Particle_Plane_Count = 0;

    public Particle_Plane add(Particle_Plane particle_Plane) {
        ++this.Particle_Plane_Count;
        if (this.Root != null) {
            particle_Plane.Next = this.Root;
            particle_Plane.Last = null;
            this.Root.Last = particle_Plane;
            this.Root = particle_Plane;
        } else {
            this.Root = particle_Plane;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return particle_Plane;
    }

    public void destroy() {
        Particle_Plane particle_Plane = this.Root;
        Particle_Plane particle_Plane2 = null;
        while (particle_Plane != null) {
            particle_Plane2 = particle_Plane.Next;
            particle_Plane.destroy();
            particle_Plane.Next = null;
            particle_Plane.Last = null;
            particle_Plane = null;
            particle_Plane = particle_Plane2;
        }
        this.Root = null;
        particle_Plane2 = null;
        particle_Plane = null;
        this.Particle_Plane_Count = 0;
    }

    public Particle_Plane reservePlane() {
        if (this.Update != null) {
            this.Update = this.Update.Next;
            if (this.Update == null) {
                this.Update = this.Root;
            }
        } else {
            this.Update = this.Root;
        }
        return this.Update;
    }

    public void remove(Particle_Plane particle_Plane) {
        this.Particle_Plane_Count += -1;
        try {
            if (particle_Plane == this.Root) {
                if (particle_Plane.Next != null) {
                    particle_Plane.Next.Last = null;
                }
                if (particle_Plane.Next != null) {
                    this.Root = particle_Plane.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                particle_Plane.destroy();
                particle_Plane = null;
                return;
            }
            if (particle_Plane.Last != null) {
                particle_Plane.Last.Next = particle_Plane.Next;
            }
            if (particle_Plane.Next != null) {
                particle_Plane.Next.Last = particle_Plane.Last;
            }
            particle_Plane.destroy();
            particle_Plane = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING ENTITY FROM LIST");
            return;
        }
    }
}

