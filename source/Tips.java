/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class Tips
implements WTOnLoadEvent,
Global {
    WTFile File;
    boolean Loaded = false;
    String[] tips = new String[100];
    int tipcount = 0;

    public void onLoadComplete(WTObject wTObject) {
        this.parseDescriptor();
    }

    public Tips() {
        this.File = Main.MainRef.Wt.readFile("http://cannonballs.wildtangent.com/tips.txt");
        this.File.setOnLoad((WTOnLoadEvent)this);
    }

    boolean isLoaded() {
        return this.Loaded;
    }

    String getRandomTip() {
        int n = (int)(Main.MainRef.random.nextFloat() * (float)this.tipcount);
        if (n >= this.tipcount) {
            n = this.tipcount - 1;
        }
        if (n < 0) {
            n = 0;
        }
        return this.tips[n];
    }

    void parseDescriptor() {
        while (!this.File.eof()) {
            this.tips[this.tipcount] = this.File.readLine();
            if (this.tips[this.tipcount].length() <= 1) continue;
            ++this.tipcount;
        }
        this.File.close();
        this.File = null;
        this.Loaded = true;
    }
}

