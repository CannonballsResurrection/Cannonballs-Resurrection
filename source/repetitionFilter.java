/*
 * Decompiled with CFR 0.152.
 */
public class repetitionFilter
extends subFilter {
    private int NAR;
    private final char validChar = (char)97;
    private final char repeatedChar = (char)98;

    protected char[] removeWhitespace(char[] cArray) {
        int n = 0;
        int n2 = 0;
        char c = '\u0017';
        int n3 = 0;
        this.whiteSpaces = new char[cArray.length];
        int n4 = 0;
        while (n4 < cArray.length) {
            int n5;
            char c2 = cArray[n4];
            this.whiteSpaces[n4] = 97;
            if (n4 > 0 && c2 == c) {
                ++n3;
            } else if (n3 > 0) {
                n5 = n3;
                n3 = 0;
                int n6 = 0;
                while (n6 < this.wspace.length) {
                    if (c2 == this.wspace[n6]) {
                        n3 = n5;
                        break;
                    }
                    ++n6;
                }
            }
            if (n3 >= this.NAR) {
                this.whiteSpaces[n4] = 98;
            } else {
                n5 = 0;
                while (n5 < this.wspace.length) {
                    if (c2 == this.wspace[n5]) {
                        this.whiteSpaces[n4] = c2;
                        break;
                    }
                    ++n5;
                }
            }
            if (this.whiteSpaces[n4] == 'a') {
                c = c2;
                cArray[n2++] = cArray[n4];
            } else {
                ++n;
            }
            ++n4;
        }
        if (n > 0) {
            char[] cArray2 = new char[cArray.length - n];
            System.arraycopy(cArray, 0, cArray2, 0, cArray2.length);
            return cArray2;
        }
        return cArray;
    }

    protected repetitionFilter(String[] stringArray, String[] stringArray2, char[] cArray, char[] cArray2, char[] cArray3, int n) {
        super(stringArray, stringArray2, cArray, cArray2, cArray3);
        this.NAR = n;
    }

    protected char[] restoreWhitespace(char[] cArray) {
        if (this.whiteSpaces == null) {
            return null;
        }
        int n = 0;
        int n2 = 0;
        while (n2 < this.whiteSpaces.length) {
            switch (this.whiteSpaces[n2]) {
                case 'a': {
                    this.whiteSpaces[n2] = cArray[n++];
                    break;
                }
                case 'b': {
                    this.whiteSpaces[n2] = cArray[n - 1];
                }
            }
            ++n2;
        }
        return this.whiteSpaces;
    }
}

