/*
 * Decompiled with CFR 0.152.
 */
public class Button_3DList {
    Button_3D Root = null;
    int Button_Count = 0;

    public void hideAll() {
        Button_3D button_3D = this.Root;
        while (button_3D != null) {
            if (button_3D.Visible) {
                button_3D.hide();
            }
            button_3D = button_3D.Next;
        }
    }

    public String mouseOver(int n, int n2, int n3) {
        Button_3D button_3D = this.Root;
        while (button_3D != null) {
            String string = button_3D.checkBounds(n, n2, n3);
            if (string != null) {
                return string;
            }
            button_3D = button_3D.Next;
        }
        return null;
    }

    public Button_3D add(Button_3D button_3D) {
        ++this.Button_Count;
        if (this.Root != null) {
            button_3D.Next = this.Root;
            button_3D.Last = null;
            this.Root.Last = button_3D;
            this.Root = button_3D;
        } else {
            this.Root = button_3D;
            this.Root.Next = null;
            this.Root.Last = null;
        }
        return button_3D;
    }

    public void destroy() {
        Button_3D button_3D = this.Root;
        Button_3D button_3D2 = null;
        while (button_3D != null) {
            button_3D2 = button_3D.Next;
            button_3D.destroy();
            button_3D.Next = null;
            button_3D.Last = null;
            button_3D = null;
            button_3D = button_3D2;
        }
        this.Root = null;
        button_3D2 = null;
        button_3D = null;
        this.Button_Count = 0;
    }

    public String update(int n, int n2, int n3) {
        Button_3D button_3D = this.Root;
        while (button_3D != null) {
            String string = button_3D.checkBounds(n, n2, n3);
            if (string != null) {
                return string;
            }
            button_3D = button_3D.Next;
        }
        return null;
    }

    public void showAll() {
        Button_3D button_3D = this.Root;
        while (button_3D != null) {
            button_3D.show();
            button_3D = button_3D.Next;
        }
    }

    public void updateTime(float f) {
        Button_3D button_3D = this.Root;
        while (button_3D != null) {
            button_3D = button_3D.Next;
        }
    }

    public void remove(Button_3D button_3D) {
        this.Button_Count += -1;
        try {
            if (button_3D == this.Root) {
                if (button_3D.Next != null) {
                    button_3D.Next.Last = null;
                }
                if (button_3D.Next != null) {
                    this.Root = button_3D.Next;
                    this.Root.Last = null;
                } else {
                    this.Root = null;
                }
                button_3D.Next = null;
                button_3D.Last = null;
                button_3D.destroy();
                button_3D = null;
                return;
            }
            if (button_3D.Last != null) {
                button_3D.Last.Next = button_3D.Next;
            }
            if (button_3D.Next != null) {
                button_3D.Next.Last = button_3D.Last;
            }
            button_3D.Next = null;
            button_3D.Last = null;
            button_3D.destroy();
            button_3D = null;
            return;
        }
        catch (Exception exception) {
            Main.MainRef.showAlert("ERROR DELETING BUTTON FROM LIST");
            return;
        }
    }
}

