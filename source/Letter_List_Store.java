/*
 * Decompiled with CFR 0.152.
 */
public class Letter_List_Store {
    public Letter_Object Root = null;
    private Letter_Object Update;
    int Letter_Count = 0;

    public Letter_Object add(Letter_Object letter_Object) {
        ++this.Letter_Count;
        if (this.Root != null) {
            letter_Object.Next2 = this.Root;
            letter_Object.Last2 = null;
            this.Root.Last2 = letter_Object;
            this.Root = letter_Object;
        } else {
            this.Root = letter_Object;
            this.Root.Next2 = null;
            this.Root.Last2 = null;
        }
        return letter_Object;
    }

    Letter_Object getNext() {
        if (this.Update == null) {
            this.Update = this.Root;
        }
        Letter_Object letter_Object = this.Update;
        this.Update = this.Update.Next2;
        this.removeFromList(letter_Object);
        return letter_Object;
    }

    public void destroy() {
        Letter_Object letter_Object = this.Root;
        Letter_Object letter_Object2 = null;
        while (letter_Object != null) {
            letter_Object2 = letter_Object.Next2;
            letter_Object.destroy();
            letter_Object.Next2 = null;
            letter_Object.Last2 = null;
            letter_Object = null;
            letter_Object = letter_Object2;
        }
        this.Root = null;
        letter_Object2 = null;
        letter_Object = null;
        this.Letter_Count = 0;
    }

    public void removeFromList(Letter_Object letter_Object) {
        this.Letter_Count += -1;
        try {
            if (letter_Object == this.Root) {
                if (letter_Object.Next2 != null) {
                    letter_Object.Next2.Last2 = null;
                }
                if (letter_Object.Next2 != null) {
                    this.Root = letter_Object.Next2;
                    this.Root.Last2 = null;
                } else {
                    this.Root = null;
                }
                letter_Object = null;
                return;
            }
            if (letter_Object.Last2 != null) {
                letter_Object.Last2.Next2 = letter_Object.Next2;
            }
            if (letter_Object.Next2 != null) {
                letter_Object.Next2.Last2 = letter_Object.Last2;
            }
            letter_Object = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING Letter FROM LIST");
            return;
        }
    }

    public void remove(Letter_Object letter_Object) {
        this.Letter_Count += -1;
        try {
            if (letter_Object == this.Root) {
                if (letter_Object.Next2 != null) {
                    letter_Object.Next2.Last2 = null;
                }
                if (letter_Object.Next2 != null) {
                    this.Root = letter_Object.Next2;
                    this.Root.Last2 = null;
                } else {
                    this.Root = null;
                }
                letter_Object.destroy();
                letter_Object = null;
                return;
            }
            if (letter_Object.Last2 != null) {
                letter_Object.Last2.Next2 = letter_Object.Next2;
            }
            if (letter_Object.Next2 != null) {
                letter_Object.Next2.Last2 = letter_Object.Last2;
            }
            letter_Object.destroy();
            letter_Object = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING Letter FROM LIST");
            return;
        }
    }
}

