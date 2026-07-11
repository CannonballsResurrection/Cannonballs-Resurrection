/*
 * Decompiled with CFR 0.152.
 */
import java.util.Vector;

public class subFilter
implements iFilter {
    private char[][] badwords;
    private char[][] validwords;
    protected char[] wspace;
    private char[] lChars;
    private char[] lSubs;
    private char[] leetChars;
    private char[] replacedChars;
    private static final char filterChar = '\u00b6';
    private static final char mc = '\u00b7';
    private Vector removedValid;
    private char[] validArray;
    protected char[] whiteSpaces;
    private static final char wildChar = '\u00a7';

    protected char[] removeWhitespace(char[] cArray) {
        this.whiteSpaces = new char[cArray.length];
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        while (n3 < cArray.length) {
            char c = cArray[n3];
            this.whiteSpaces[n3] = 97;
            int n4 = 0;
            while (n4 < this.wspace.length) {
                if (c == this.wspace[n4]) {
                    this.whiteSpaces[n3] = c;
                    break;
                }
                ++n4;
            }
            if (this.whiteSpaces[n3] == 'a') {
                cArray[n2++] = cArray[n3];
            } else {
                ++n;
            }
            ++n3;
        }
        if (n > 0) {
            char[] cArray2 = new char[cArray.length - n];
            System.arraycopy(cArray, 0, cArray2, 0, cArray2.length);
            return cArray2;
        }
        return cArray;
    }

    private final void replaceLeet(char[] cArray, char c, char c2) {
        int n = 0;
        while (n < cArray.length) {
            if (cArray[n] == c) {
                this.leetChars[n] = c;
                this.replacedChars[n] = c2;
                cArray[n] = c2;
            }
            ++n;
        }
    }

    private final void replaceLeet(char[] cArray) {
        this.leetChars = new char[cArray.length];
        this.replacedChars = new char[this.leetChars.length];
        int n = 0;
        while (n < this.leetChars.length) {
            this.leetChars[n] = 97;
            ++n;
        }
        n = 0;
        while (n < this.lChars.length) {
            this.replaceLeet(cArray, this.lChars[n], this.lSubs[n]);
            ++n;
        }
    }

    protected subFilter(String[] stringArray, String[] stringArray2, char[] cArray, char[] cArray2, char[] cArray3) {
        this.badwords = new char[stringArray.length][];
        int n = 0;
        while (n < stringArray.length) {
            this.badwords[n] = stringArray[n].toCharArray();
            ++n;
        }
        this.validwords = new char[stringArray2.length][];
        n = 0;
        while (n < stringArray2.length) {
            this.validwords[n] = stringArray2[n].toCharArray();
            ++n;
        }
        this.wspace = cArray;
        this.lChars = cArray2;
        this.lSubs = cArray3;
    }

    protected char[] restoreWhitespace(char[] cArray) {
        if (this.whiteSpaces == null) {
            return null;
        }
        int n = 0;
        int n2 = 0;
        while (n2 < this.whiteSpaces.length) {
            if (this.whiteSpaces[n2] == 'a') {
                this.whiteSpaces[n2] = cArray[n++];
            }
            ++n2;
        }
        return this.whiteSpaces;
    }

    private final int indexOf(char[] cArray, char[] cArray2) {
        int n = cArray2.length;
        int n2 = 0;
        try {
            char c = cArray2[n2];
            int n3 = 0;
            while (n3 < cArray.length) {
                if (cArray[n3] == c || c == '\u00a7') {
                    if (++n2 == n) {
                        return n3 - n + 1;
                    }
                    c = cArray2[n2];
                } else if (n2 != 0) {
                    n2 = 0;
                    c = cArray2[0];
                }
                ++n3;
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("find: " + cArray2 + ", find.length: " + cArray2.length);
        }
        return -1;
    }

    private void replaceBadwords(char[] cArray) {
        char[] cArray2 = new char[cArray.length];
        int n = -32;
        int n2 = 0;
        while (n2 < cArray2.length) {
            cArray2[n2] = cArray[n2] >= 'A' && cArray[n2] <= 'Z' ? (char)(cArray[n2] - n) : cArray[n2];
            ++n2;
        }
        n2 = 0;
        int n3 = 0;
        while (n3 < this.badwords.length) {
            int n4 = this.indexOf(cArray2, this.badwords[n3]);
            if (n4 > -1) {
                int n5 = this.badwords[n3].length;
                int n6 = n4;
                while (n6 < n4 + n5) {
                    cArray2[n6] = 182;
                    cArray[n6] = 182;
                    ++n6;
                }
                n2 = 1;
                --n3;
            }
            ++n3;
        }
    }

    private final int indexOf(char[] cArray, char c) {
        int n = 0;
        while (n < cArray.length) {
            if (cArray[n] == c) {
                return n;
            }
            ++n;
        }
        return -1;
    }

    private void restoreLeet(char[] cArray) {
        if (this.leetChars == null) {
            return;
        }
        int n = 0;
        while (n < this.leetChars.length) {
            if (this.leetChars[n] != 'a' && this.replacedChars[n] == cArray[n]) {
                cArray[n] = this.leetChars[n];
            }
            ++n;
        }
    }

    private char[] addValidMarkers(char[] cArray) {
        this.validArray = cArray;
        char[] cArray2 = new char[cArray.length];
        int n = -32;
        int n2 = 0;
        while (n2 < cArray2.length) {
            cArray2[n2] = cArray[n2] >= 'A' && cArray[n2] <= 'Z' ? (char)(cArray[n2] - n) : cArray[n2];
            ++n2;
        }
        if (this.removedValid == null) {
            this.removedValid = new Vector();
        } else {
            this.removedValid.removeAllElements();
        }
        n2 = 0;
        int n3 = 0;
        while (n3 < this.validwords.length) {
            int n4 = this.indexOf(cArray2, this.validwords[n3]);
            if (n4 > -1) {
                char[] cArray3 = this.subWord(cArray, n4, n4 + this.validwords[n3].length);
                this.removedValid.addElement(cArray3);
                int n5 = cArray.length - (n4 + this.validwords[n3].length);
                if (n5 > 0) {
                    System.arraycopy(cArray, n4 + this.validwords[n3].length, cArray, n4 + 1, n5);
                    System.arraycopy(cArray2, n4 + this.validwords[n3].length, cArray2, n4 + 1, n5);
                }
                cArray2[n4] = 183;
                cArray[n4] = 183;
                n2 += this.validwords[n3].length - 1;
                --n3;
            }
            ++n3;
        }
        if (n2 > 0) {
            char[] cArray4 = new char[cArray.length - n2];
            System.arraycopy(cArray, 0, cArray4, 0, cArray4.length);
            return cArray4;
        }
        return cArray;
    }

    private char[] restoreValidMarkers(char[] cArray) {
        if (this.removedValid == null || this.validArray == null || this.indexOf(cArray, '\u00b7') < 0) {
            return cArray;
        }
        int n = 0;
        int n2 = 0;
        while (n2 < cArray.length) {
            if (cArray[n2] == '\u00b7') {
                if (this.removedValid.isEmpty()) break;
                char[] cArray2 = (char[])this.removedValid.elementAt(0);
                this.removedValid.removeElementAt(0);
                int n3 = 0;
                while (n3 < cArray2.length) {
                    this.validArray[n++] = cArray2[n3];
                    ++n3;
                }
            } else {
                this.validArray[n++] = cArray[n2];
            }
            ++n2;
        }
        return this.validArray;
    }

    public boolean[] theFilter(String string) {
        this.leetChars = null;
        this.replacedChars = null;
        this.whiteSpaces = null;
        this.removedValid = null;
        char[] cArray = string.toCharArray();
        this.replaceLeet(cArray);
        cArray = this.addValidMarkers(cArray);
        cArray = this.removeWhitespace(cArray);
        this.replaceBadwords(cArray);
        cArray = this.restoreWhitespace(cArray);
        cArray = this.restoreValidMarkers(cArray);
        this.restoreLeet(cArray);
        boolean[] blArray = new boolean[cArray.length];
        int n = 0;
        while (n < cArray.length) {
            blArray[n] = cArray[n] == '\u00b6';
            ++n;
        }
        return blArray;
    }

    private char[] subWord(char[] cArray, int n, int n2) {
        if (n2 <= n) {
            return null;
        }
        char[] cArray2 = new char[n2 - n];
        int n3 = 0;
        while (n3 < cArray2.length) {
            cArray2[n3] = cArray[n + n3];
            ++n3;
        }
        return cArray2;
    }
}

