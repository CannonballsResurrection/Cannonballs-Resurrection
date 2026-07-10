/*
 * Decompiled with CFR 0.152.
 */
public class Chunk_List {
    Chunk_Object Root = null;
    int Chunk_Count = 0;

    public Chunk_Object add(Chunk_Object chunk_Object) {
        ++this.Chunk_Count;
        if (this.Root != null) {
            chunk_Object.Next = this.Root;
            chunk_Object.Last = null;
            this.Root.Last = chunk_Object;
            this.Root = chunk_Object;
        } else {
            this.Root = chunk_Object;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return chunk_Object;
    }

    public void destroy() {
        Chunk_Object chunk_Object = this.Root;
        Chunk_Object chunk_Object2 = null;
        while (chunk_Object != null) {
            chunk_Object2 = chunk_Object.Next;
            chunk_Object.destroy();
            chunk_Object.Next = null;
            chunk_Object.Last = null;
            chunk_Object = null;
            chunk_Object = chunk_Object2;
        }
        this.Root = null;
        chunk_Object2 = null;
        chunk_Object = null;
        this.Chunk_Count = 0;
    }

    public void update(float f) {
        Chunk_Object chunk_Object = this.Root;
        while (chunk_Object != null) {
            chunk_Object.updateTimeSlice(f);
            chunk_Object = chunk_Object.Next;
        }
    }

    public void offset(float f, float f2, float f3) {
        Chunk_Object chunk_Object = this.Root;
        while (chunk_Object != null) {
            chunk_Object.X -= f;
            chunk_Object.Y -= f2;
            chunk_Object.Z -= f3;
            chunk_Object = chunk_Object.Next;
        }
    }

    public void remove(Chunk_Object chunk_Object) {
        this.Chunk_Count += -1;
        try {
            if (chunk_Object == this.Root) {
                if (chunk_Object.Next != null) {
                    chunk_Object.Next.Last = null;
                }
                if (chunk_Object.Next != null) {
                    this.Root = chunk_Object.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                chunk_Object.destroy();
                chunk_Object = null;
                return;
            }
            if (chunk_Object.Last != null) {
                chunk_Object.Last.Next = chunk_Object.Next;
            }
            if (chunk_Object.Next != null) {
                chunk_Object.Next.Last = chunk_Object.Last;
            }
            chunk_Object.destroy();
            chunk_Object = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING CHUNK FROM LIST");
            return;
        }
    }
}

