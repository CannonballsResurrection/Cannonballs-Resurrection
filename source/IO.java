/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTEvent
 *  wildtangent.webdriver.WTEventCallback
 *  wildtangent.webdriver.WTKeyboardPollInfo
 */
import com.wildtangent.dmmp.shared.log.Log;
import com.wildtangent.dmmp.shared.log.LogFactory;
import wildtangent.webdriver.WTEvent;
import wildtangent.webdriver.WTEventCallback;
import wildtangent.webdriver.WTKeyboardPollInfo;

public class IO
implements WTEventCallback,
Global {
    WTKeyboardPollInfo KeyInfo;
    float MouseAxisX = 0.0f;
    float MouseAxisY = 0.0f;
    boolean[] MouseButton = new boolean[this.MaxButtons + 1];
    int[] MouseButtonMap = new int[this.MaxButtons + 1];
    int MaxButtons = 12;
    float FocusTimeOut = 5.0f;
    private static Log outlog = LogFactory.getLog(class$IO != null ? class$IO : (class$IO = IO.class$("IO")));
    boolean HasDied = false;
    private static /* synthetic */ Class class$IO;

    public void onExceptionEvent(WTEvent wTEvent) {
    }

    public IO() {
        this.clearMouse();
        this.defaultMouseMap();
    }

    public void clearMouse() {
        int n = 0;
        while (n < this.MaxButtons + 1) {
            this.MouseButton[n] = false;
            ++n;
        }
        this.MouseAxisX = 0.0f;
        this.MouseAxisY = 0.0f;
    }

    public void updateGameMouse(int n, int n2, int n3) {
        this.MouseAxisX = ((float)n - (float)Main.MainRef.wt_stage.ScreenWidth / 2.0f) / ((float)Main.MainRef.wt_stage.ScreenWidth / 2.0f);
        this.MouseAxisY = ((float)n2 - (float)Main.MainRef.wt_stage.ScreenHeight / 2.0f) / ((float)Main.MainRef.wt_stage.ScreenHeight / 2.0f);
        this.updateMouseButtonGame(n3);
    }

    public void updateMouseButton(int n) {
        int n2 = 1;
        int n3 = 1;
        n2 = 1;
        n3 = 1;
        n3 = 1;
        while (n3 < this.MaxButtons) {
            if ((n & n2) > 0) {
                if (!this.MouseButton[n3]) {
                    this.MouseButton[n3] = true;
                }
            } else if (this.MouseButton[n3]) {
                this.MouseButton[n3] = false;
            }
            n2 *= 2;
            ++n3;
        }
    }

    public void defaultMouseMap() {
        this.clearMouseMap();
        this.MouseButtonMap[1] = 8;
        this.MouseButtonMap[2] = 31;
        this.MouseButtonMap[3] = 9;
    }

    public void clearMouseMap() {
        int n = 0;
        while (n < this.MaxButtons + 1) {
            this.MouseButtonMap[n] = -1;
            ++n;
        }
    }

    int shiftKey(int n, boolean bl) {
        switch (n) {
            case 32: {
                return 32;
            }
            case 65: {
                if (bl) {
                    return 65;
                }
                return 97;
            }
            case 66: {
                if (bl) {
                    return 66;
                }
                return 98;
            }
            case 67: {
                if (bl) {
                    return 67;
                }
                return 99;
            }
            case 68: {
                if (bl) {
                    return 68;
                }
                return 100;
            }
            case 69: {
                if (bl) {
                    return 69;
                }
                return 101;
            }
            case 70: {
                if (bl) {
                    return 70;
                }
                return 102;
            }
            case 71: {
                if (bl) {
                    return 71;
                }
                return 103;
            }
            case 72: {
                if (bl) {
                    return 72;
                }
                return 104;
            }
            case 73: {
                if (bl) {
                    return 73;
                }
                return 105;
            }
            case 74: {
                if (bl) {
                    return 74;
                }
                return 106;
            }
            case 75: {
                if (bl) {
                    return 75;
                }
                return 107;
            }
            case 76: {
                if (bl) {
                    return 76;
                }
                return 108;
            }
            case 77: {
                if (bl) {
                    return 77;
                }
                return 109;
            }
            case 78: {
                if (bl) {
                    return 78;
                }
                return 110;
            }
            case 79: {
                if (bl) {
                    return 79;
                }
                return 111;
            }
            case 80: {
                if (bl) {
                    return 80;
                }
                return 112;
            }
            case 81: {
                if (bl) {
                    return 81;
                }
                return 113;
            }
            case 82: {
                if (bl) {
                    return 82;
                }
                return 114;
            }
            case 83: {
                if (bl) {
                    return 83;
                }
                return 115;
            }
            case 84: {
                if (bl) {
                    return 84;
                }
                return 116;
            }
            case 85: {
                if (bl) {
                    return 85;
                }
                return 117;
            }
            case 86: {
                if (bl) {
                    return 86;
                }
                return 118;
            }
            case 87: {
                if (bl) {
                    return 87;
                }
                return 119;
            }
            case 88: {
                if (bl) {
                    return 88;
                }
                return 120;
            }
            case 89: {
                if (bl) {
                    return 89;
                }
                return 121;
            }
            case 90: {
                if (bl) {
                    return 90;
                }
                return 122;
            }
            case 48: 
            case 96: {
                if (bl) {
                    return 41;
                }
                return 48;
            }
            case 49: 
            case 97: {
                if (bl) {
                    return 33;
                }
                return 49;
            }
            case 50: 
            case 98: {
                if (bl) {
                    return 64;
                }
                return 50;
            }
            case 51: 
            case 99: {
                if (bl) {
                    return 35;
                }
                return 51;
            }
            case 52: 
            case 100: {
                if (bl) {
                    return 36;
                }
                return 52;
            }
            case 53: 
            case 101: {
                if (bl) {
                    return 37;
                }
                return 53;
            }
            case 54: 
            case 102: {
                if (bl) {
                    return 94;
                }
                return 54;
            }
            case 55: 
            case 103: {
                if (bl) {
                    return 38;
                }
                return 55;
            }
            case 56: 
            case 104: {
                if (bl) {
                    return 42;
                }
                return 56;
            }
            case 57: 
            case 105: {
                if (bl) {
                    return 40;
                }
                return 57;
            }
            case 19: {
                return 42;
            }
            case 187: {
                if (bl) {
                    return 43;
                }
                return 61;
            }
            case 189: {
                if (bl) {
                    return 95;
                }
                return 45;
            }
            case 192: {
                if (bl) {
                    return 126;
                }
                return 126;
            }
            case 219: {
                if (bl) {
                    return 123;
                }
                return 91;
            }
            case 221: {
                if (bl) {
                    return 125;
                }
                return 93;
            }
            case 220: {
                if (bl) {
                    return 124;
                }
                return 92;
            }
            case 186: {
                if (bl) {
                    return 58;
                }
                return 59;
            }
            case 222: {
                if (bl) {
                    return 34;
                }
                return 39;
            }
            case 188: {
                if (bl) {
                    return 60;
                }
                return 44;
            }
            case 190: {
                if (bl) {
                    return 62;
                }
                return 46;
            }
            case 191: {
                if (bl) {
                    return 63;
                }
                return 47;
            }
            case 110: {
                return 46;
            }
            case 107: {
                return 43;
            }
            case 109: {
                return 45;
            }
            case 106: {
                return 42;
            }
            case 111: {
                return 47;
            }
        }
        return n;
    }

    public void onKeyboardEvent(WTEvent wTEvent) {
        int n = wTEvent.getKey();
        boolean bl = false;
        this.KeyInfo = Main.MainRef.Wt.pollKeyboard();
        if (this.KeyInfo.isKeyDown(16) != 0) {
            bl = true;
        }
        if (this.KeyInfo.isKeyDown(20) != 0) {
            bl = !bl;
        }
        n = this.shiftKey(n, bl);
        switch (Main.MainRef.GameState) {
            case 10: {
                if (wTEvent.getKeyState() == 1) {
                    Main.MainRef.MenuManager.KeyDown(n);
                    return;
                }
                Main.MainRef.MenuManager.KeyUp(n);
                return;
            }
            case 3: 
            case 12: {
                if (wTEvent.getKeyState() == 1) {
                    Main.MainRef.hud.keyDownGameActive(n);
                    return;
                }
                Main.MainRef.hud.keyUpGameActive(n);
                return;
            }
            case 11: {
                if (wTEvent.getKeyState() == 1) {
                    Main.MainRef.hud.keyDownSpectator(n);
                    return;
                }
                Main.MainRef.hud.keyUpSpectator(n);
                return;
            }
        }
    }

    private static /* synthetic */ Class class$(String string) {
        try {
            return Class.forName(string);
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(classNotFoundException.getMessage());
        }
    }

    public void updateMouseButtonGame(int n) {
        int n2 = 1;
        int n3 = 1;
        n2 = 1;
        n3 = 1;
        n3 = 1;
        while (n3 < this.MaxButtons) {
            if ((n & n2) > 0) {
                if (!this.MouseButton[n3]) {
                    this.MouseButton[n3] = true;
                }
            } else if (this.MouseButton[n3]) {
                this.MouseButton[n3] = false;
            }
            n2 *= 2;
            ++n3;
        }
    }

    /*
     * Exception decompiling
     */
    public void onRenderEvent(WTEvent var1_1) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Extractable last case doesn't follow previous, and can't clone.
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer.examineSwitchContiguity(SwitchReplacer.java:611)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer.replaceRawSwitches(SwitchReplacer.java:94)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:517)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public void onMouseEvent(WTEvent wTEvent) {
        int n = wTEvent.getWTX();
        int n2 = wTEvent.getWTY();
        switch (Main.MainRef.GameState) {
            case 10: {
                Main.MainRef.MenuManager.processMouseMenu(n, n2, wTEvent.getButtonState());
                Main.MainRef.camera.updateMouse(n, n2);
                return;
            }
            case 3: 
            case 11: {
                Main.MainRef.hud.updateGameMouse(n, n2, wTEvent.getButtonState());
                Main.MainRef.camera.updateMouse(n, n2);
                return;
            }
        }
        Main.MainRef.camera.updateMouse(n, n2);
    }
}

