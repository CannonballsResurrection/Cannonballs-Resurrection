/*
 * Decompiled with CFR 0.152.
 */
import java.util.StringTokenizer;

public class Text
implements Global {
    Media_Object_Shader Trebuchet;
    Media_Object_Shader BlueTrebuchet;
    Media_Object_Shader GrayTrebuchet;
    int TextLines = 0;
    private static String[] badwords = new String[]{"69", "a\u00a7nal", "ass", "anal", "anus", "arse", "azhol", "azwipe", "b\u00a7atch", "b\u00a7yach", "b\u00a7yotch", "bast\u00a7rd", "beaner", "beardedclam", "befcurtain", "befcurtin", "befinjection", "bi\u00a7ach", "bi\u00a7atch", "bi\u00a7ch", "bi\u00a7tch", "biotch", "bit\u00a7ch", "biz\u00a7tch", "bizach", "bizn\u00a7ch", "bizn\u00a7tch", "bj", "blome", "blowjob", "blowme", "blubals", "bluebals", "bo\u00a7b", "bol\u00a7ock", "bol\u00a7ok", "boner", "borderbuni", "borderbuny", "borderhoper", "brea\u00a7t", "brest", "btch", "bu\u00a7t", "bucake", "buckethead", "bucknaked", "bukake", "buknaked", "bulshiet", "bulsht", "bumperlip", "bunghol", "bustanut", "butburglar", "butburgler", "buthol", "butpirate", "by\u00a7tch", "byt\u00a7ch", "bythch", "c\u00a7och", "c\u00a7otch", "cabron", "cameljockey", "cameljocky", "carpetmuncher", "carpetpilot", "cervix", "chichi", "chink", "choad", "chode", "christkiler", "ckock", "ckok", "clevelandste\u00a7mer", "clit", "co\u00a7n", "coc", "cock", "cok", "coq", "cornhole", "cotonpicker", "cotonpiker", "cox", "cum", "cunalingus", "cunelingus", "cunilingus", "cuningulus", "cunt", "cvnt", "dago", "damn", "damnit", "darkie", "darky", "diaperhead", "diaperhed", "dic", "dik", "dikhead", "dikhed", "dildo", "dildoe", "dilhol", "dirtymexican", "dirtysanchez", "dmsht", "do\u00a7che", "doche", "dogiestyle", "dogystyle", "dong", "easylay", "eatme", "ejaculat", "ere\u00a7hun", "erectil", "erection", "erectshun", "ezlay", "f\u00a7ck", "f\u00a7hq", "fag", "fagot", "fahk", "fahker", "fahq", "fajina", "fartknocker", "fck", "fcof", "felashio", "felatio", "felch", "feltch", "fhuck", "fhuk", "filthymexican", "fingerbang", "fisting", "fockit", "fok", "foreq", "fuc", "fuck", "fucker", "fudgepack", "fuhk", "fuhker", "fuhq", "fuk", "fuker", "funbag", "fuq", "furberger", "furburger", "furlicker", "fux", "fuxor", "fvck", "fvk", "fvuck", "fvuk", "g\u00a7odhead", "gangbang", "gasem", "gasim", "gasm", "gavehead", "gavemehead", "gay", "gen\u00a7tal", "getlaid", "getomonkey", "getomonky", "getomunkey", "getomunky", "ghetomonkey", "ghetomonky", "ghetomunkey", "ghetomunky", "gimeabj", "gism", "gism", "givehead", "givemehead", "gizm", "go\u00a7k", "goldenshower", "gomba", "gonad", "goyim", "h\u00a7oter", "hairpie", "hairybals", "hairybalz", "hairyclam", "hairygash", "handjob", "hardon", "hardon", "haryclam", "heeb", "hitler", "homo", "homoes", "homos", "horgasm", "hughgrection", "hughjorgan", "hum\u00a7er", "hymie", "incest", "insest", "j\u00a7rkof\u00a7", "jablome", "jablowme", "jablowmie", "jacko\u00a7f", "jako\u00a7f", "jap", "jerfof", "jesuskiler", "jew", "jig", "jigab\u00a7", "jirkof", "jism", "jiz", "junglebuni", "junglebuny", "kike", "klukluxklan", "kock", "kok", "kornhole", "kraut", "kukluxklan", "kunt", "lawnjockey", "lawnjocky", "lesbian", "lesbin", "lesbo", "lezbian", "lezbo", "licker", "lovepump", "masterbait", "masterbate", "masterbayt", "mastirbait", "mastirbate", "mastirbayt", "masturbait", "masturbate", "masturbayt", "me\u00a7t\u00a7ane", "me\u00a7tpole", "me\u00a7twhistle", "mikehunt", "milkpilow", "molest", "mthrfkr", "mu\u00a7f", "mushromhead", "mushromhed", "mushromtip", "mybals", "mybalz", "nads", "nazifirewod", "nig", "ni\u00a7ple", "nig\u00a7a", "nig\u00a7er", "nig\u00a7ir", "nig\u00a7r", "nigr", "nockers", "nutsac", "oneyedwilie", "oneyedwily", "oneyewilie", "oneyewily", "orgi", "orgy", "pe\u00a7hn", "pe\u00a7hol", "pearlnecklace", "pehole", "penis", "penus", "peslit", "phal\u00a7s", "phalic", "phalik", "phuc", "phuck", "phucue", "phuk", "phuq", "phuque", "pi\u00a7s", "pimp", "pinktaco", "pinktako", "pinktorpedo", "po\u00a7n", "po\u00a7pshot", "poarchmonkey", "poarchmonky", "poarchmonky", "poarchmunkey", "poarchmunky", "poarchmunky", "polack", "polesmoker", "polock", "pontang", "popchute", "porchmonkey", "porchmonky", "porchmonky", "porchmunkey", "porchmunky", "porchmunky", "porksword", "prick", "prik", "prostitute", "pusy", "pu\u00a7se", "pu\u00a7si", "pu\u00a7sy", "pu\u00a7zy", "pube", "pubic", "pud", "pu\u00a7sie", "puzy", "pvsy", "qu\u00a7ef", "que\u00a7r", "queaf", "quim", "qver", "raghead", "raghed", "ragheds", "ramrod", "rape", "rayp", "recktum", "rectum", "rimjob", "ruaslut", "rugmunch", "sambo", "sbals", "sbalz", "sch\u00a7long", "schlong", "screw", "scrotum", "semen", "sex", "shit", "shizl", "shizn", "shlong", "skank", "skrew", "slanteye", "slantyeye", "slopyseconds", "slut", "slvt", "smeg", "snach", "snatch", "sodom", "sp\u00a7rm", "spank", "spic", "spo\u00a7ge", "spo\u00a7gum", "suck", "suk", "suq", "tarbaby", "tits", "towelhead", "towelhed", "trousersnake", "turd", "twat", "uncletom", "upherbut", "uphisbut", "upmybut", "upyourbut", "vagina", "vajina", "vibrater", "vibrator", "vulva", "wackof", "wang", "wank", "wanker", "welfaremonkey", "wetback", "wetbak", "whackof", "whore", "wop", "yourbals", "yourbalz", "penile", "manpacker", "masturbation", "cunny", "labia", "genitalia", "beatyourmeat", "beatmymeat", "beatoff", "condom", "funbag", "testicles", "testes", "skrotum", "crap", "sux", "dyk", "balz", "rapist", "shyt", "lickmysac", "hentai", "titz", "tities", "titiez", "titties", "tittiez", "tittys", "tittyz", "dique", "bolucks", "slanthate", "slantkiler", "crackho", "fxuxcxk", "nigz", "niguz", "niguh", "zipperhead", "retard", "bestiality", "necrophilia", "nympho", "fornicate", "sixtynine", "fairy", "grangrape", "humping", "bugger", "auschwitz", "ginnywhop"};
    private static String[] validwords = new String[]{"s exa", "s exb", "s exc", "s exe", "s exd", "s exf", "s exh", "s exg", "s exi", "s exo", "s exp", "s exq", "s exr", "s ext", "s exu", "morass", "glass", "pass", "sass", "as sa", "as se", "as si", "as so", "as su", "as st", "as sy", "as sc", "as sk", "as sl", "as sm", "as sn", "as sp", "as sq", "as sv", "as sw", "as sz", "canal", "parsec", "parse", "parses", "larsen", "parsed", "parser", "darsey", "passe", "lasso", "sassy", "masse", "lasso", "lass", "bass", "banal", "annal", "annul", "birch", "bjorn", "abdomen", "salome", "bobb", "bomb", "bunt", "bust", "koko", "corn", "coin", "conn", "coen", "cohn", "coln", "coan", "scorn", "coins", "corns", "corny", "count", "conni", "conns", "connu", "corni", "coing", "conny", "scoon", "coca", "coco", "coct", "coch", "edict", "dict", "dongle", "fugue", "refuge", "hotter", "homogenize", "homogenous", "jape", "jewel", "ning", "pies", "pits", "pins", "pigs", "pips", "pics", "pims", "paprika", "grape", "drape", "crape", "frape", "sextant", "sextet", "unisex", "abs", "acs", "ads", "aes", "afs", "ags", "ahs", "ais", "ajs", "aks", "als", "ams", "aos", "aps", "ars", "aus", "avs", "aws", "ays", "amass", "mass", "brass", "class", "glass", "grass", "crass", "harass", "harrass", "bypass", "canvass", "carcass", "cutlass", "embarass", "asset", "assay", "assist", "assure", "cannonball", "ats", "thats", "but that", "and", "japan", "nigh", "snigger", "point", "accumulat", "circumvent", "circumference", "assumption", "assum", "muffin", "assembly", "uranus", "butter", "button", "pen", "pills", "bug that", "assimilate", "assign", "pimple", "assess", "raccoon", "puddle", "shitake", "analogy", "cumulative", "crevasse", "peephole", "associat"};
    private static final String[] badAscii = new String[]{"(_o_)", "(.)(.)", "(@)(@)", "(o)(o)", "8===D", "(y)"};
    private static final String[] validAscii = new String[]{"8D", ":)", ":-)", ";)", ":/", ":P", ">:)", "8)"};
    private static final char[] wspaceAscii = new char[]{' '};
    private static final char[] lCharsAscii = new char[]{' '};
    private static final char[] lSubsAscii = new char[]{' '};
    private static iFilter[] filters;
    private static final char[] filterChars;
    private static final char[] wspace;
    private static final char[] lChars;
    private static final char[] lSubs;
    private static final char[] lChars2;
    private static final char[] lSubs2;
    private static final char[] lChars3;
    private static final char[] lSubs3;
    private static final char[] lChars4;
    private static final char[] lSubs4;
    static final float[] CharacterWidthTrebuchet;

    public static void setWordLists(byte[] byArray, byte[] byArray2, byte[] byArray3, byte[] byArray4) {
        String[] stringArray = Text.getStringArray(byArray);
        String[] stringArray2 = Text.getStringArray(byArray2);
        String[] stringArray3 = Text.getStringArray(byArray3);
        String[] stringArray4 = Text.getStringArray(byArray4);
        filters = new iFilter[9];
        if (stringArray != null && stringArray2 != null) {
            Text.filters[0] = new repetitionFilter(stringArray, stringArray2, wspace, lChars, lSubs, 1);
            Text.filters[1] = new repetitionFilter(stringArray, stringArray2, wspace, lChars2, lSubs2, 1);
            Text.filters[2] = new repetitionFilter(stringArray, stringArray2, wspace, lChars3, lSubs3, 1);
            Text.filters[3] = new repetitionFilter(stringArray, stringArray2, wspace, lChars4, lSubs4, 1);
            Text.filters[4] = new repetitionFilter(stringArray, stringArray2, wspace, lChars, lSubs, 2);
            Text.filters[5] = new repetitionFilter(stringArray, stringArray2, wspace, lChars2, lSubs2, 2);
            Text.filters[6] = new repetitionFilter(stringArray, stringArray2, wspace, lChars3, lSubs3, 2);
            Text.filters[7] = new repetitionFilter(stringArray, stringArray2, wspace, lChars4, lSubs4, 2);
        }
        if (stringArray3 != null && stringArray4 != null) {
            Text.filters[8] = new repetitionFilter(stringArray3, stringArray4, wspaceAscii, lCharsAscii, lSubsAscii, 1);
        }
        System.gc();
    }

    String trimBlock(String string, int n, int n2) {
        if (n < 0) {
            n = 0;
        }
        String string2 = "";
        StringTokenizer stringTokenizer = new StringTokenizer(string, "" + '\r');
        int n3 = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String string3 = stringTokenizer.nextToken();
            if (n3 >= n) {
                string2 = string2 + string3 + '\r';
            }
            if (++n3 < n + n2) continue;
            return string2;
        }
        return string2;
    }

    public Text() {
        this.Trebuchet = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/FONT/TREBUCHET/font.png", "MEDIA/IMAGES/FONT/TREBUCHET/alpha.png", false, false), true);
        this.Trebuchet.noDepth();
        this.BlueTrebuchet = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/FONT/TREBUCHET/bluefont.png", "MEDIA/IMAGES/FONT/TREBUCHET/alpha.png", false, false), true);
        this.BlueTrebuchet.noDepth();
        this.GrayTrebuchet = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader("MEDIA/IMAGES/FONT/TREBUCHET/grayfont.png", "MEDIA/IMAGES/FONT/TREBUCHET/alpha.png", false, false), true);
        this.GrayTrebuchet.noDepth();
    }

    public static String doFilter(String string) {
        System.currentTimeMillis();
        String string2 = string;
        try {
            if (filters == null) {
                filters = new iFilter[9];
                Text.filters[0] = new repetitionFilter(badwords, validwords, wspace, lChars, lSubs, 1);
                Text.filters[1] = new repetitionFilter(badwords, validwords, wspace, lChars2, lSubs2, 1);
                Text.filters[2] = new repetitionFilter(badwords, validwords, wspace, lChars3, lSubs3, 1);
                Text.filters[3] = new repetitionFilter(badwords, validwords, wspace, lChars4, lSubs4, 1);
                Text.filters[4] = new repetitionFilter(badwords, validwords, wspace, lChars, lSubs, 2);
                Text.filters[5] = new repetitionFilter(badwords, validwords, wspace, lChars2, lSubs2, 2);
                Text.filters[6] = new repetitionFilter(badwords, validwords, wspace, lChars3, lSubs3, 2);
                Text.filters[7] = new repetitionFilter(badwords, validwords, wspace, lChars4, lSubs4, 2);
                Text.filters[8] = new repetitionFilter(badAscii, validAscii, wspaceAscii, lCharsAscii, lSubsAscii, 1);
            }
            char[] cArray = string.toCharArray();
            int n = 0;
            int n2 = 0;
            while (n2 < filters.length) {
                if (filters[n2] != null) {
                    boolean[] blArray = filters[n2].theFilter(string);
                    int n3 = 0;
                    while (n3 < blArray.length) {
                        if (blArray[n3]) {
                            cArray[n3] = filterChars[n++];
                            if (n >= filterChars.length) {
                                n = 0;
                            }
                        }
                        ++n3;
                    }
                }
                ++n2;
            }
            string2 = new String(cArray);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        System.currentTimeMillis();
        return string2;
    }

    boolean isLoaded() {
        if (!this.Trebuchet.isLoaded()) {
            return false;
        }
        if (!this.BlueTrebuchet.isLoaded()) {
            return false;
        }
        return this.GrayTrebuchet.isLoaded();
    }

    static {
        filterChars = new char[]{'@', '#', '$', '%', '&'};
        wspace = new char[]{' ', '`', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '-', '=', '[', ']', '\\', ';', '\'', ':', '\"', ',', '.', '/', '<', '>', '?'};
        lChars = new char[]{'4', '3', '+', '1', '@', '$', '5', '7', '0'};
        lSubs = new char[]{'a', 'e', 't', 'l', 'a', 's', 's', 't', 'o'};
        lChars2 = new char[]{'4', '3', '1', '5', '7', '0'};
        lSubs2 = new char[]{'a', 'e', 'l', 's', 't', 'o'};
        lChars3 = new char[]{'4', '3', '+', '1', '@', '$', '5', '7', '0'};
        lSubs3 = new char[]{'a', 'e', 't', 'i', 'a', 's', 's', 't', 'o'};
        lChars4 = new char[]{'4', '3', '1', '5', '7', '0'};
        lSubs4 = new char[]{'a', 'e', 'i', 's', 't', 'o'};
        CharacterWidthTrebuchet = new float[]{12.0f, 12.0f, 13.0f, 16.0f, 14.0f, 18.0f, 14.0f, 11.0f, 12.0f, 12.0f, 12.0f, 16.0f, 11.0f, 12.0f, 12.0f, 14.0f, 16.0f, 15.0f, 16.0f, 16.0f, 15.0f, 16.0f, 17.0f, 17.0f, 16.0f, 16.0f, 13.0f, 13.0f, 16.0f, 16.0f, 15.0f, 13.0f, 20.0f, 18.0f, 16.0f, 17.0f, 17.0f, 18.0f, 16.0f, 18.0f, 17.0f, 11.0f, 15.0f, 17.0f, 16.0f, 21.0f, 17.0f, 20.0f, 16.0f, 20.0f, 17.0f, 16.0f, 17.0f, 17.0f, 17.0f, 22.0f, 17.0f, 18.0f, 16.0f, 11.0f, 13.0f, 12.0f, 12.0f, 14.0f, 10.0f, 16.0f, 15.0f, 15.0f, 16.0f, 17.0f, 13.0f, 15.0f, 15.0f, 11.0f, 11.0f, 16.0f, 11.0f, 20.0f, 16.0f, 17.0f, 16.0f, 15.0f, 13.0f, 14.0f, 13.0f, 16.0f, 16.0f, 19.0f, 16.0f, 17.0f, 16.0f, 13.0f, 12.0f, 13.0f, 14.0f, 16.0f, 14.0f, 18.0f, 14.0f, 11.0f};
    }

    private static final String[] getStringArray(byte[] byArray) {
        if (byArray == null) {
            return null;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(new String(byArray), "\n" + '\r', false);
        String[] stringArray = new String[stringTokenizer.countTokens()];
        int n = 0;
        while (n < stringArray.length) {
            stringArray[n] = Text.removeSpacesAndMultipleLetters(stringTokenizer.nextToken().toLowerCase(), 1);
            ++n;
        }
        return stringArray;
    }

    String wordWrap(String[] stringArray, int n, float f, float f2) {
        String string = "";
        this.TextLines = 0;
        float f3 = 0.0f;
        float f4 = 0.0f;
        int n2 = 0;
        while (n2 < n) {
            f3 = 0.0f;
            f4 = 0.0f;
            if (stringArray[n2].length() > 1) {
                StringTokenizer stringTokenizer = new StringTokenizer(stringArray[n2], " ,", true);
                while (stringTokenizer.hasMoreTokens()) {
                    char c;
                    String string2 = stringTokenizer.nextToken();
                    int n3 = string2.length();
                    f4 = 0.0f;
                    int n4 = 0;
                    while (n4 < n3) {
                        c = string2.charAt(n4);
                        if ((c = (char)(c - 32)) < '\u0000') {
                            c = '\u0000';
                        }
                        if (c > '_') {
                            c = '\u0000';
                        }
                        f4 += CharacterWidthTrebuchet[c] * 0.75f * f2;
                        ++n4;
                    }
                    if (f3 + f4 < f) {
                        string = string + string2;
                        f3 += f4;
                        continue;
                    }
                    if (f4 < f) {
                        string = string + '\r' + string2;
                        f3 = f4;
                        ++this.TextLines;
                        continue;
                    }
                    n4 = 0;
                    while (n4 < n3) {
                        c = string2.charAt(n4);
                        if ((c = (char)(c - 32)) < '\u0000') {
                            c = '\u0000';
                        }
                        if (c > '_') {
                            c = '\u0000';
                        }
                        if (f3 + (f4 = CharacterWidthTrebuchet[c] * 0.75f * f2) < f) {
                            string = string + string2.charAt(n4);
                            f3 += f4;
                        } else {
                            string = string + '\r' + string2.charAt(n4);
                            f3 = f4;
                            ++this.TextLines;
                        }
                        ++n4;
                    }
                }
                if (n2 < n - 1) {
                    string = string + '\r';
                }
                ++this.TextLines;
            }
            ++n2;
        }
        return string;
    }

    String wordWrapTrim(String string, float f, float f2) {
        String string2 = "";
        this.TextLines = 0;
        float f3 = 0.0f;
        float f4 = 0.0f;
        f3 = 0.0f;
        f4 = 0.0f;
        if (string.length() > 1) {
            StringTokenizer stringTokenizer = new StringTokenizer(string, " ,", true);
            while (stringTokenizer.hasMoreTokens()) {
                char c;
                String string3 = stringTokenizer.nextToken();
                int n = string3.length();
                f4 = 0.0f;
                int n2 = 0;
                while (n2 < n) {
                    c = string3.charAt(n2);
                    if ((c = (char)(c - 32)) < '\u0000') {
                        c = '\u0000';
                    }
                    if (c > '_') {
                        c = '\u0000';
                    }
                    f4 += CharacterWidthTrebuchet[c] * 0.75f * f2;
                    ++n2;
                }
                if (f3 + f4 < f) {
                    string2 = string2 + string3;
                    f3 += f4;
                    continue;
                }
                if (f4 < f) {
                    return string2;
                }
                n2 = 0;
                while (n2 < n) {
                    c = string3.charAt(n2);
                    if ((c = (char)(c - 32)) < '\u0000') {
                        c = '\u0000';
                    }
                    if (c > '_') {
                        c = '\u0000';
                    }
                    if (f3 + (f4 = CharacterWidthTrebuchet[c] * 0.75f * f2) < f) {
                        string2 = string2 + string3.charAt(n2);
                        f3 += f4;
                    } else {
                        return string2;
                    }
                    ++n2;
                }
            }
        }
        return string2;
    }

    private static final String removeSpacesAndMultipleLetters(String string, int n) {
        return new String(Text.removeSpacesAndMultipleLetters(string.toCharArray(), n));
    }

    private static final char[] removeSpacesAndMultipleLetters(char[] cArray, int n) {
        int n2 = 0;
        int n3 = 0;
        int n4 = 23;
        int n5 = 0;
        int n6 = 0;
        while (n6 < cArray.length) {
            int n7 = cArray[n6];
            int n8 = 0;
            if (n6 > 0 && n7 == n4) {
                ++n5;
            } else if (n5 > 0) {
                int n9 = n5;
                n5 = 0;
                if (n7 == 32) {
                    n5 = n9;
                }
            }
            if (n5 >= n) {
                n8 = 2;
            } else if (n7 == 32) {
                n8 = n7;
            }
            if (n8 == 0) {
                n4 = n7;
                cArray[n3++] = cArray[n6];
            } else {
                ++n2;
            }
            ++n6;
        }
        if (n2 > 0) {
            char[] cArray2 = new char[cArray.length - n2];
            System.arraycopy(cArray, 0, cArray2, 0, cArray2.length);
            return cArray2;
        }
        return cArray;
    }
}

