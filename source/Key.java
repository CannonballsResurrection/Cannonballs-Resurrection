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

public class Key
implements WTOnLoadEvent,
Global {
    WTFile File;
    boolean Loaded = false;
    byte[] buffer;

    public void onLoadComplete(WTObject wTObject) {
        this.parseDescriptor();
    }

    public Key() {
        this.File = Main.MainRef.Wt.readFile(Main.MainRef.MediaPath + "/XORBlockEncryptData.wtxt");
        this.File.setOnLoad((WTOnLoadEvent)this);
    }

    boolean isLoaded() {
        return this.Loaded;
    }

    void parseDescriptor() {
        if (!this.File.eof()) {
            this.buffer = this.File.readAll();
        } else {
            Main.MainRef.Wt.outDebugString("INVALID KEY FILE");
        }
        this.File.close();
        this.File = null;
        this.Loaded = true;
    }
}

