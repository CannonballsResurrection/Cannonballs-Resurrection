/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTActor
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import wildtangent.webdriver.WTActor;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class Media_Object_Actor
extends Media_Object
implements Global {
    WTActor Model;
    float Width = 0.0f;
    float Height = 0.0f;
    float finalscalex = -1.0f;
    float finalscaley = -1.0f;
    float finalscalez = -1.0f;
    float Quality = 0.0f;
    int R = -1;
    int G = -1;
    int B = -1;

    public void onLoadComplete(WTObject wTObject) {
        this.Loaded = true;
        if (this.finalscalex != -1.0f) {
            this.Model.setAbsoluteScale(this.finalscalex, this.finalscaley, this.finalscalez);
        }
    }

    public Media_Object_Actor(String string) {
        this.Path = string;
        this.Type = 2;
        this.Model = this.Path.endsWith(".wsad") || this.Path.endsWith(".wsdf") ? Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + this.Path, Main.MainRef.CacheType) : Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + this.Path + "/actor.wsad", Main.MainRef.CacheType);
        this.Model.setOnLoadedWithChildren((WTOnLoadEvent)this);
    }

    public Media_Object_Actor(String string, float f, float f2, float f3) {
        this.finalscalex = f;
        this.finalscaley = f2;
        this.finalscalez = f3;
        this.Path = string;
        this.Type = 2;
        this.Model = this.Path.endsWith(".wsad") || this.Path.endsWith(".wsdf") ? Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + this.Path, Main.MainRef.CacheType) : Main.MainRef.Wt.createActor(Main.MainRef.MediaPath + this.Path + "/actor.wsad", Main.MainRef.CacheType);
        this.Model.setOnLoadedWithChildren((WTOnLoadEvent)this);
    }

    public void destroy() {
        if (!this.Loaded) {
            return;
        }
        this.Model = null;
    }

    public boolean isLoaded() {
        if (!this.Loaded && this.Model.isLoadedWithChildren()) {
            this.Loaded = true;
        }
        return this.Loaded;
    }
}

