/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTDrop
 */
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTDrop;

public class Button_Flat
implements Global {
    Button_Flat Next;
    Button_Flat Last;
    WTBitmap Bitmap;
    WTDrop Drop;
    String Keyword;
    int X = 0;
    int Y = 0;
    int TopLeftX = 0;
    int TopLeftY = 0;
    int BottomRightX = 0;
    int BottomRightY = 0;
    boolean Over = false;
    boolean OverSet = true;

    void setState() {
        if (this.Over && !this.OverSet) {
            this.OverSet = true;
            this.Drop = Main.MainRef.camera.CameraView.addDrop(this.Bitmap, true);
            this.Drop.setPosition(this.X, this.Y);
            Main.MainRef.camera.hideMouse();
            Main.MainRef.camera.showMouse();
            return;
        }
        if (!this.Over && this.OverSet) {
            this.OverSet = false;
            Main.MainRef.camera.CameraView.removeDrop(this.Drop);
        }
    }

    public Button_Flat(WTBitmap wTBitmap, int n, int n2, int n3, int n4, int n5, int n6, String string) {
        this.Bitmap = Main.MainRef.Wt.createBlankBitmap(n3 - n, n4 - n2);
        this.Bitmap.copyRect(wTBitmap, 0, n, n2, n3 - n, n4 - n2, 0, 0, n3 - n, n4 - n2);
        this.Keyword = string;
        this.X = n5;
        this.Y = n6;
        this.TopLeftX = n5;
        this.TopLeftY = n6;
        this.BottomRightX = n5 + (n3 - n);
        this.BottomRightY = n6 + (n4 - n2);
        this.setState();
    }

    public Button_Flat(WTBitmap wTBitmap, int n, int n2, int n3, int n4, int n5, int n6, String string, int n7, int n8, int n9) {
        this.Bitmap = Main.MainRef.Wt.createBlankBitmap(n3 - n, n4 - n2);
        this.Bitmap.copyRect(wTBitmap, 0, n, n2, n3 - n, n4 - n2, 0, 0, n3 - n, n4 - n2);
        this.Bitmap.setColorKey(n7, n8, n9);
        this.Keyword = string;
        this.X = n5;
        this.Y = n6;
        this.TopLeftX = n5;
        this.TopLeftY = n6;
        this.BottomRightX = n5 + (n3 - n);
        this.BottomRightY = n6 + (n4 - n2);
        this.setState();
    }

    public void destroy() {
        this.Over = false;
        this.setState();
        this.Bitmap.destroy();
        this.Bitmap = null;
        this.Drop = null;
    }

    String checkBounds(int n, int n2, int n3) {
        if (n > this.TopLeftX && n < this.BottomRightX && n2 > this.TopLeftY && n2 < this.BottomRightY) {
            this.Over = true;
            this.setState();
            if ((n3 & 1) == 1) {
                return this.Keyword;
            }
        } else {
            this.Over = false;
            this.setState();
        }
        return null;
    }

    void update(float f) {
    }
}

