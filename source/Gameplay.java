/*
 * Decompiled with CFR 0.152.
 */
public class Gameplay {
    private static final int[][] Decals;
    private final StringBuffer m_s = new StringBuffer();
    private final StringBuffer m_t = new StringBuffer(8);
    private int flashTimeInMS = 43;
    private String m_CopyrightString = "J0Ugo7ru|_|_d@n|>`'ag87wi7w4l2-";
    private int m_PlayerLives = 0;
    private String[] m_screen;

    private final int GravityFromMass(String string) {
        int n = 6;
        int n2 = 3;
        int n3 = 2;
        boolean bl = true;
        int n4 = string.charAt(0) >> 6 & 1;
        int n5 = string.charAt(1) >> 3 & 3;
        int n6 = string.charAt(2) >> 2 & 1;
        int n7 = string.charAt(3) >> 1 & 3;
        int n8 = n4 | n5 << 1 | n6 << 3 | n7 << 4;
        return n8;
    }

    private final void SetPlayerSpeed(int n, int n2, int n3) {
        String string = this.m_screen[n];
        int n4 = string.length();
        int n5 = 0;
        while (n5 < n2) {
            char c;
            char c2 = c = this.m_s.charAt(n3 + n5);
            int n6 = c2 ^ this.flashTimeInMS;
            char c3 = string.charAt(this.m_PlayerLives++);
            if (this.m_PlayerLives >= n4) {
                this.m_PlayerLives = 0;
            }
            if (c2 >= ' ' && c2 <= '~' && n6 >= 32 && n6 <= 126) {
                char c4 = (char)n6;
                this.m_s.setCharAt(n3 + n5, c4);
            }
            this.flashTimeInMS = n6 ^ c3;
            ++n5;
        }
    }

    public final String GetPlayerName(int n, String string) {
        int n2 = n >= this.m_screen.length ? 0 : n;
        this.flashTimeInMS = 43;
        this.m_s.setLength(0);
        this.SetPlayerName(n2, string);
        this.m_s.append(string);
        this.m_PlayerLives = 0;
        this.SetPlayerSpeed(n2, this.m_s.length() - 4, 4);
        this.SetMissileColor(this.m_s.length() - 4, 4);
        return this.m_s.toString();
    }

    public final void SetScreenBuffer(byte[] byArray) {
        this.m_screen = new String[73];
        int n = 0;
        int n2 = 0;
        while (n2 < this.m_screen.length) {
            int n3 = this.range(0, byArray[n++ % byArray.length] - 65, 79) + 6;
            byte[] byArray2 = new byte[n3];
            int n4 = 0;
            while (n4 < n3) {
                byArray2[n4] = (byte)(byArray[n++ % byArray.length] & 0x7F);
                ++n4;
            }
            this.m_screen[n2] = new String(byArray2);
            ++n2;
        }
    }

    private final void SetExhaust(int n, int n2) {
        if (n > Decals.length - 1) {
            int n3 = 0;
            while (n3 < n) {
                int n4 = n - n3 < Decals.length - 1 ? n - n3 : Decals.length - 1;
                this.SetExhaust(n4, n2 + n3);
                n3 += Decals.length - 1;
            }
            return;
        }
        this.m_t.setLength(n);
        int n5 = 0;
        while (n5 < n) {
            int n6 = Decals[n][n5];
            char c = this.m_s.charAt(n2 + n6);
            this.m_t.setCharAt(n5, c);
            ++n5;
        }
        n5 = 0;
        while (n5 < n) {
            this.m_s.setCharAt(n2 + n5, this.m_t.charAt(n5));
            ++n5;
        }
    }

    private final int range(int n, int n2, int n3) {
        if (n2 < n) {
            return n;
        }
        if (n2 > n3) {
            return n3;
        }
        return n2;
    }

    public final String GetMissileName(String string) {
        int n = this.GravityFromMass(string);
        this.flashTimeInMS = 43;
        this.m_s.setLength(0);
        this.m_s.append(string.substring(4));
        this.m_PlayerLives = 0;
        this.SetExhaust(this.m_s.length(), 0);
        this.SetPlayerRotation(n, this.m_s.length(), 0);
        return this.m_s.toString();
    }

    public static void main(String[] stringArray) {
        System.out.println("Gameplay");
    }

    static {
        int[][] nArrayArray = new int[9][];
        nArrayArray[0] = new int[0];
        nArrayArray[1] = new int[1];
        int[] nArray = new int[2];
        nArray[0] = 1;
        nArrayArray[2] = nArray;
        int[] nArray2 = new int[3];
        nArray2[0] = 2;
        nArray2[2] = 1;
        nArrayArray[3] = nArray2;
        int[] nArray3 = new int[4];
        nArray3[0] = 3;
        nArray3[2] = 2;
        nArray3[3] = 1;
        nArrayArray[4] = nArray3;
        int[] nArray4 = new int[5];
        nArray4[0] = 4;
        nArray4[1] = 2;
        nArray4[2] = 3;
        nArray4[3] = 1;
        nArrayArray[5] = nArray4;
        int[] nArray5 = new int[6];
        nArray5[0] = 2;
        nArray5[1] = 4;
        nArray5[2] = 1;
        nArray5[4] = 5;
        nArray5[5] = 3;
        nArrayArray[6] = nArray5;
        int[] nArray6 = new int[7];
        nArray6[0] = 1;
        nArray6[1] = 4;
        nArray6[2] = 2;
        nArray6[3] = 6;
        nArray6[5] = 3;
        nArray6[6] = 5;
        nArrayArray[7] = nArray6;
        int[] nArray7 = new int[8];
        nArray7[0] = 5;
        nArray7[1] = 7;
        nArray7[2] = 2;
        nArray7[3] = 4;
        nArray7[4] = 1;
        nArray7[5] = 6;
        nArray7[6] = 3;
        nArrayArray[8] = nArray7;
        Decals = nArrayArray;
    }

    private final void SetMissileColor(int n, int n2) {
        if (n > Decals.length - 1) {
            int n3 = 0;
            while (n3 < n) {
                int n4 = n - n3 < Decals.length - 1 ? n - n3 : Decals.length - 1;
                this.SetMissileColor(n4, n2 + n3);
                n3 += Decals.length - 1;
            }
            return;
        }
        this.m_t.setLength(n);
        int n5 = 0;
        while (n5 < n) {
            int n6 = Decals[n][n5];
            char c = this.m_s.charAt(n2 + n5);
            this.m_t.setCharAt(n6, c);
            ++n5;
        }
        n5 = 0;
        while (n5 < n) {
            this.m_s.setCharAt(n2 + n5, this.m_t.charAt(n5));
            ++n5;
        }
    }

    private final void SetPlayerRotation(int n, int n2, int n3) {
        String string = this.m_screen[n];
        int n4 = string.length();
        int n5 = 0;
        while (n5 < n2) {
            char c;
            char c2 = c = this.m_s.charAt(n3 + n5);
            int n6 = c2 ^ this.flashTimeInMS;
            char c3 = string.charAt(this.m_PlayerLives++);
            if (this.m_PlayerLives >= n4) {
                this.m_PlayerLives = 0;
            }
            if (c2 >= ' ' && c2 <= '~' && n6 >= 32 && n6 <= 126) {
                char c4 = (char)n6;
                this.m_s.setCharAt(n3 + n5, c4);
                this.flashTimeInMS = c2 ^ c3;
            } else {
                this.flashTimeInMS = n6 ^ c3;
            }
            ++n5;
        }
    }

    private final void SetPlayerName(int n, String string) {
        int n2 = string.length();
        int n3 = 6;
        int n4 = 3;
        int n5 = 2;
        boolean bl = true;
        int n6 = (n & 1) << 6;
        int n7 = (n & 6) << 2;
        int n8 = (n & 8) >> 1;
        int n9 = (n & 0x30) >> 3;
        char c = (char)((string.charAt(13 % n2) ^ 0x14) & 0x7E);
        char c2 = (char)((string.charAt(20 % n2) ^ 0x18) & 0x7E);
        char c3 = (char)((string.charAt(7 % n2) ^ 0x13) & 0x7E);
        char c4 = (char)((string.charAt(113 % n2) ^ 0x16) & 0x7E);
        char c5 = (char)(c & 0xFFFFFFBF | n6);
        char c6 = (char)(c2 & 0xFFFFFFE7 | n7);
        char c7 = (char)(c3 & 0xFFFFFFFB | n8);
        char c8 = (char)(c4 & 0xFFFFFFF9 | n9 | 0x40);
        this.m_s.append(c5);
        this.m_s.append(c6);
        this.m_s.append(c7);
        this.m_s.append(c8);
    }
}

