/*
 * Decompiled with CFR 0.152.
 */
public class Media_List
implements Global {
    public Media_Object Root = null;
    int Media_Count = 0;

    public int countLoaded() {
        int n = 0;
        Media_Object media_Object = this.Root;
        while (media_Object != null) {
            if (media_Object.Loaded) {
                ++n;
            }
            media_Object = media_Object.Next;
        }
        return n;
    }

    public Media_Object add(Media_Object media_Object, boolean bl) {
        media_Object.Permanent = bl;
        ++this.Media_Count;
        if (this.Root != null) {
            media_Object.Next = this.Root;
            media_Object.Last = null;
            this.Root.Last = media_Object;
            this.Root = media_Object;
        } else {
            this.Root = media_Object;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return media_Object;
    }

    public void destroy() {
        Media_Object media_Object = this.Root;
        Media_Object media_Object2 = null;
        while (media_Object != null) {
            media_Object2 = media_Object.Next;
            media_Object.destroy();
            media_Object.Next = null;
            media_Object.Last = null;
            media_Object = null;
            media_Object = media_Object2;
        }
        this.Root = null;
        media_Object2 = null;
        media_Object = null;
        this.Media_Count = 0;
    }

    public void dumpMedia() {
        Media_Object media_Object = this.Root;
        while (media_Object != null) {
            Main.MainRef.Wt.outDebugString("MEDIA ITEM " + media_Object.Path);
            media_Object = media_Object.Next;
        }
    }

    public void destroyImpermanent() {
        Media_Object media_Object = this.Root;
        Media_Object media_Object2 = null;
        while (media_Object != null) {
            media_Object2 = media_Object.Next;
            if (!media_Object.Permanent) {
                this.remove(media_Object);
            }
            media_Object = media_Object2;
        }
    }

    public boolean allLoaded() {
        Media_Object media_Object = this.Root;
        while (media_Object != null) {
            if (!media_Object.isLoaded()) {
                return false;
            }
            media_Object = media_Object.Next;
        }
        return true;
    }

    public void remove(Media_Object media_Object) {
        this.Media_Count += -1;
        if (media_Object == this.Root) {
            if (media_Object.Next != null) {
                this.Root = media_Object.Next;
                this.Root.Last = null;
            } else {
                this.Root = null;
            }
            media_Object.Next = null;
            media_Object.destroy();
            media_Object = null;
            return;
        }
        if (media_Object.Last != null) {
            media_Object.Last.Next = media_Object.Next;
        }
        if (media_Object.Next != null) {
            media_Object.Next.Last = media_Object.Last;
        }
        media_Object.Last = null;
        media_Object.Next = null;
        media_Object.destroy();
        media_Object = null;
    }
}

