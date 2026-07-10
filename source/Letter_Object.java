/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTGroup
 */
import wildtangent.webdriver.WTGroup;

public class Letter_Object
implements Global {
    Letter_Object Next2;
    Letter_Object Last2;
    WTGroup Group;

    public Letter_Object() {
        this.Group = Main.MainRef.Wt.createGroup();
        this.Group.attachSurfaceShader(Main.MainRef.TextManager.Trebuchet.Shader, 1.0f, 1.0f, 128, 128);
    }

    public void destroy() {
        this.Group.detach();
        this.Group = null;
    }
}

