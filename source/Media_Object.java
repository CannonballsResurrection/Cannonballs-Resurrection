/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class Media_Object
implements WTOnLoadEvent {
    Media_Object Next = null;
    Media_Object Last = null;
    String Name = "";
    String Path = "";
    int Type = -1;
    boolean Loaded = false;
    boolean Permanent = false;

    public void onLoadComplete(WTObject wTObject) {
    }

    public void destroy() {
    }

    public boolean isLoaded() {
        return this.Loaded;
    }
}

