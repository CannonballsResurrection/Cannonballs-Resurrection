/*
 * Decompiled with CFR 0.152.
 */
public class Button_ListFlat {
    Button_Flat Root = null;
    int Button_Count = 0;

    public Button_Flat add(Button_Flat button_Flat) {
        ++this.Button_Count;
        if (this.Root != null) {
            button_Flat.Next = this.Root;
            button_Flat.Last = null;
            this.Root.Last = button_Flat;
            this.Root = button_Flat;
        } else {
            this.Root = button_Flat;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return button_Flat;
    }

    public void destroy() {
        Button_Flat button_Flat = this.Root;
        Button_Flat button_Flat2 = null;
        while (button_Flat != null) {
            button_Flat2 = button_Flat.Next;
            button_Flat.destroy();
            button_Flat.Next = null;
            button_Flat.Last = null;
            button_Flat = null;
            button_Flat = button_Flat2;
        }
        this.Root = null;
        button_Flat2 = null;
        button_Flat = null;
        this.Button_Count = 0;
    }

    public String update(int n, int n2, int n3) {
        Button_Flat button_Flat = this.Root;
        while (button_Flat != null) {
            String string = button_Flat.checkBounds(n, n2, n3);
            if (string != null) {
                return string;
            }
            button_Flat = button_Flat.Next;
        }
        return null;
    }

    public void remove(Button_Flat button_Flat) {
        this.Button_Count += -1;
        try {
            if (button_Flat == this.Root) {
                if (button_Flat.Next != null) {
                    button_Flat.Next.Last = null;
                }
                if (button_Flat.Next != null) {
                    this.Root = button_Flat.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                button_Flat.Next = null;
                button_Flat.Last = null;
                button_Flat.destroy();
                button_Flat = null;
                return;
            }
            if (button_Flat.Last != null) {
                button_Flat.Last.Next = button_Flat.Next;
            }
            if (button_Flat.Next != null) {
                button_Flat.Next.Last = button_Flat.Last;
            }
            button_Flat.Next = null;
            button_Flat.Last = null;
            button_Flat.destroy();
            button_Flat = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING BUTTON FROM LIST");
            return;
        }
    }
}

