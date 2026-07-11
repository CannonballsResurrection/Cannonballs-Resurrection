/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTBitmap
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import wildtangent.webdriver.WTBitmap;
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class UberImage
implements WTOnLoadEvent {
    private String path;
    private WTFile data;
    public WTBitmap bitmap;
    private final int COLORTYPE_GREYSCALE = 0;
    private final int COLORTYPE_RGB = 2;
    private final int COLORTYPE_PALETTE = 3;
    private final int COLORTYPE_GREYSCALEALPHA = 4;
    private final int COLORTYPE_RGBA = 6;
    private final int RED = 0;
    private final int GREEN = 1;
    private final int BLUE = 2;
    private final int ALPHA = 3;
    private String[] colortype_descriptions = new String[]{"Greyscale", "Unknown", "RGB", "Paletted", "Greyscale+Alpha", "Unknown", "RGB+Alpha"};
    boolean Loaded = false;
    boolean FromImagePack = false;
    boolean TintImage = false;
    int TintR = 0;
    int TintG = 0;
    int TintB = 0;
    boolean isStored = false;
    public byte[] pngBitmapData;
    public byte[] grayBitmapData;
    private boolean not_iend;
    public int img_width;
    public int img_height;
    private byte img_bitdepth;
    public byte img_colortype;
    private byte img_compressionmethod;
    private byte img_filtermethod;
    private byte img_interlacemethod;
    private char img_compressionflags;
    private char img_flagscheckbits;
    private char img_compresseddatablocks;
    private int img_checkvalue;
    private char[][] img_palette;
    private byte[] temp_byte_data;

    public void MakeStatic() {
        this.data = null;
        this.pngBitmapData = null;
        this.grayBitmapData = null;
    }

    private boolean headerMatch(char c, char c2, char c3, char c4, char[] cArray) {
        return cArray.length == 4 && c == cArray[0] && c2 == cArray[1] && c3 == cArray[2] && c4 == cArray[3];
    }

    public void onLoadComplete(WTObject wTObject) {
        if (this.data != null) {
            this.createBitmap();
            this.Loaded = true;
            return;
        }
        Main.MainRef.Wt.outDebugString("*****WHAT!!! DATA IS NULL FOR " + this.path);
    }

    public boolean isStored() {
        return this.isStored;
    }

    private byte RawVal(int n, int n2, int n3) {
        try {
            switch (this.img_colortype) {
                case 0: {
                    if (n3 == 3) {
                        return -1;
                    }
                    return this.temp_byte_data[n2 * this.img_width + n + n2 + 1];
                }
                case 4: {
                    return this.temp_byte_data[(n2 * this.img_width + n) * 2 + n3 + n2 + 1];
                }
                case 3: {
                    if (n3 == 3) {
                        return -1;
                    }
                    return (byte)this.img_palette[this.unsign(this.temp_byte_data[n2 * this.img_width + n + n2 + 1])][n3];
                }
                case 2: {
                    if (n3 == 3) {
                        return -1;
                    }
                    return this.temp_byte_data[(n2 * this.img_width + n) * 3 + n3 + n2 + 1];
                }
                case 6: {
                    return this.temp_byte_data[(n2 * this.img_width + n) * 4 + n3 + n2 + 1];
                }
            }
            return 0;
        }
        catch (Exception exception) {
            return 0;
        }
    }

    public byte GSrcVal(int n, int n2, int n3) {
        return this.grayBitmapData[this.si(n, n2, n3)];
    }

    public UberImage() {
        FileIO fileIO = new FileIO();
        fileIO = null;
    }

    public UberImage(String string, boolean bl) {
        this.path = string;
        this.FromImagePack = bl;
        if (!this.FromImagePack || !Main.MainRef.UseImagePack) {
            this.FromImagePack = false;
            this.path = Main.MainRef.MediaPath + this.path;
            this.data = Main.MainRef.Wt.readFile(this.path, Main.MainRef.CacheType);
            this.data.setOnLoadedWithChildren((WTOnLoadEvent)this);
            return;
        }
        this.createBitmap();
        this.Loaded = true;
    }

    public UberImage(String string, boolean bl, int n, int n2, int n3) {
        this.TintImage = true;
        this.TintR = n;
        this.TintG = n2;
        this.TintB = n3;
        this.path = string;
        this.FromImagePack = bl;
        if (!this.FromImagePack || !Main.MainRef.UseImagePack) {
            this.FromImagePack = false;
            this.path = Main.MainRef.MediaPath + this.path;
            this.data = Main.MainRef.Wt.readFile(this.path, Main.MainRef.CacheType);
            this.data.setOnLoadedWithChildren((WTOnLoadEvent)this);
            return;
        }
        this.createBitmap();
        this.Loaded = true;
    }

    void store() {
        this.isStored = true;
        if (this.grayBitmapData == null) {
            this.grayBitmapData = new byte[this.img_width * this.img_height * 4];
        }
        System.arraycopy(this.pngBitmapData, 0, this.grayBitmapData, 0, this.img_width * this.img_height * 4);
    }

    void restore() {
        System.arraycopy(this.grayBitmapData, 0, this.pngBitmapData, 0, this.img_width * this.img_height * 4);
    }

    private void ReadChunk(FileIO fileIO) {
        int n = fileIO.ReadInt();
        char[] cArray = fileIO.ReadBytes(4);
        if (this.headerMatch('I', 'H', 'D', 'R', cArray)) {
            this.img_width = fileIO.ReadInt();
            this.img_height = fileIO.ReadInt();
            this.img_bitdepth = fileIO.ReadByte();
            this.img_colortype = fileIO.ReadByte();
            this.img_compressionmethod = fileIO.ReadByte();
            this.img_filtermethod = fileIO.ReadByte();
            this.img_interlacemethod = fileIO.ReadByte();
        } else if (this.headerMatch('P', 'L', 'T', 'E', cArray)) {
            if (n % 3 != 0) {
                Main.MainRef.Wt.outDebugString("  palette is a corrupt width");
            } else {
                this.img_palette = new char[n / 3][3];
                int n2 = 0;
                while (n2 < n / 3) {
                    this.img_palette[n2][0] = fileIO.ReadChar();
                    this.img_palette[n2][1] = fileIO.ReadChar();
                    this.img_palette[n2][2] = fileIO.ReadChar();
                    ++n2;
                }
            }
        } else if (this.headerMatch('t', 'E', 'X', 't', cArray)) {
            String string = fileIO.ReadString();
            fileIO.ReadBytes(n - string.length() - 1);
        } else if (this.headerMatch('t', 'I', 'M', 'E', cArray)) {
            fileIO.ReadShort();
            fileIO.ReadByte();
            fileIO.ReadByte();
            fileIO.ReadByte();
            fileIO.ReadByte();
            fileIO.ReadByte();
        } else if (this.headerMatch('p', 'H', 'Y', 's', cArray)) {
            fileIO.ReadInt();
            fileIO.ReadInt();
            fileIO.ReadByte();
        } else if (this.headerMatch('I', 'E', 'N', 'D', cArray)) {
            this.not_iend = false;
        } else if (this.headerMatch('I', 'D', 'A', 'T', cArray)) {
            int n3 = 0;
            int n4 = 0;
            try {
                byte[] byArray = fileIO.ReadSignedBytes(n);
                Inflater inflater = new Inflater();
                inflater.setInput(byArray);
                switch (this.img_colortype) {
                    case 0: 
                    case 3: {
                        this.temp_byte_data = new byte[(this.img_width + 1) * this.img_height];
                        break;
                    }
                    case 4: {
                        this.temp_byte_data = new byte[this.img_width * this.img_height * 2 + this.img_height];
                        break;
                    }
                    case 2: {
                        this.temp_byte_data = new byte[this.img_width * this.img_height * 3 + this.img_height];
                        break;
                    }
                    case 6: {
                        this.temp_byte_data = new byte[this.img_width * this.img_height * 4 + this.img_height];
                        break;
                    }
                    default: {
                        return;
                    }
                }
                try {
                    inflater.inflate(this.temp_byte_data);
                }
                catch (DataFormatException dataFormatException) {
                    Main.MainRef.Wt.outDebugString(this.path + " : " + dataFormatException.toString());
                }
                this.pngBitmapData = new byte[this.img_width * this.img_height * 4];
                n4 = 0;
                while (n4 < this.img_height) {
                    int n5 = 0;
                    switch (this.img_colortype) {
                        case 0: 
                        case 3: {
                            n5 = this.temp_byte_data[n4 * this.img_width + n4];
                            break;
                        }
                        case 2: {
                            n5 = this.temp_byte_data[n4 * this.img_width * 3 + n4];
                            break;
                        }
                        case 6: {
                            n5 = this.temp_byte_data[n4 * this.img_width * 4 + n4];
                            break;
                        }
                        case 4: {
                            n5 = this.temp_byte_data[n4 * this.img_width * 2 + n4];
                        }
                    }
                    switch (n5) {
                        case 0: {
                            n3 = 0;
                            while (n3 < this.img_width) {
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)0)] = this.RawVal(n3, n4, 0);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)1)] = this.RawVal(n3, n4, 1);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)2)] = this.RawVal(n3, n4, 2);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)3)] = this.RawVal(n3, n4, 3);
                                ++n3;
                            }
                            break;
                        }
                        case 1: {
                            int n6;
                            int n7;
                            int n8;
                            int n9;
                            n3 = 0;
                            while (n3 < this.img_width) {
                                if (n3 > 0) {
                                    n9 = this.unsign(this.RawVal(n3, n4, 0)) + this.SrcVal(n3 - 1, n4, 0);
                                    n8 = this.unsign(this.RawVal(n3, n4, 1)) + this.SrcVal(n3 - 1, n4, 1);
                                    n7 = this.unsign(this.RawVal(n3, n4, 2)) + this.SrcVal(n3 - 1, n4, 2);
                                    n6 = this.unsign(this.RawVal(n3, n4, 3)) + this.SrcVal(n3 - 1, n4, 3);
                                } else {
                                    n9 = this.unsign(this.RawVal(n3, n4, 0));
                                    n8 = this.unsign(this.RawVal(n3, n4, 1));
                                    n7 = this.unsign(this.RawVal(n3, n4, 2));
                                    n6 = this.unsign(this.RawVal(n3, n4, 3));
                                }
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)0)] = (byte)n9;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)1)] = (byte)n8;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)2)] = (byte)n7;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)3)] = (byte)n6;
                                ++n3;
                            }
                            break;
                        }
                        case 2: {
                            int n6;
                            int n7;
                            int n8;
                            int n9;
                            n3 = 0;
                            while (n3 < this.img_width) {
                                n9 = this.unsign(this.SrcVal(n3, n4 - 1, 0)) + this.unsign(this.RawVal(n3, n4, 0));
                                n8 = this.unsign(this.SrcVal(n3, n4 - 1, 1)) + this.unsign(this.RawVal(n3, n4, 1));
                                n7 = this.unsign(this.SrcVal(n3, n4 - 1, 2)) + this.unsign(this.RawVal(n3, n4, 2));
                                n6 = this.unsign(this.SrcVal(n3, n4 - 1, 3)) + this.unsign(this.RawVal(n3, n4, 3));
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)0)] = (byte)n9;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)1)] = (byte)n8;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)2)] = (byte)n7;
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)3)] = (byte)n6;
                                ++n3;
                            }
                            break;
                        }
                        case 3: {
                            int n10;
                            int n11;
                            int n12;
                            int n13;
                            char c;
                            char c2;
                            char c3;
                            char c4;
                            char c5;
                            char c6;
                            char c7;
                            char c8;
                            n3 = 0;
                            while (n3 < this.img_width) {
                                if (n3 > 0) {
                                    c8 = this.unsign(this.SrcVal(n3, n4 - 1, 0));
                                    c7 = this.unsign(this.SrcVal(n3, n4 - 1, 1));
                                    c6 = this.unsign(this.SrcVal(n3, n4 - 1, 2));
                                    c5 = this.unsign(this.SrcVal(n3, n4 - 1, 3));
                                    c4 = this.unsign(this.SrcVal(n3 - 1, n4, 0));
                                    c3 = this.unsign(this.SrcVal(n3 - 1, n4, 1));
                                    c2 = this.unsign(this.SrcVal(n3 - 1, n4, 2));
                                    c = this.unsign(this.SrcVal(n3 - 1, n4, 3));
                                    n13 = (this.unsign(this.RawVal(n3, n4, 0)) + (c4 + c8) / 2) % 256;
                                    n12 = (this.unsign(this.RawVal(n3, n4, 1)) + (c3 + c7) / 2) % 256;
                                    n11 = (this.unsign(this.RawVal(n3, n4, 2)) + (c2 + c6) / 2) % 256;
                                    n10 = (this.unsign(this.RawVal(n3, n4, 3)) + (c + c5) / 2) % 256;
                                } else {
                                    c8 = this.unsign(this.SrcVal(n3, n4 - 1, 0));
                                    c7 = this.unsign(this.SrcVal(n3, n4 - 1, 1));
                                    c6 = this.unsign(this.SrcVal(n3, n4 - 1, 2));
                                    c5 = this.unsign(this.SrcVal(n3, n4 - 1, 3));
                                    n13 = c8 / 2 + this.unsign(this.RawVal(n3, n4, 0));
                                    n12 = c7 / 2 + this.unsign(this.RawVal(n3, n4, 1));
                                    n11 = c6 / 2 + this.unsign(this.RawVal(n3, n4, 2));
                                    n10 = c5 / 2 + this.unsign(this.RawVal(n3, n4, 3));
                                }
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)0)] = (byte)(n13 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)1)] = (byte)(n12 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)2)] = (byte)(n11 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)3)] = (byte)(n10 % 256);
                                ++n3;
                            }
                            break;
                        }
                        case 4: {
                            int n10;
                            int n11;
                            int n12;
                            int n13;
                            char c;
                            char c2;
                            char c3;
                            char c4;
                            char c5;
                            char c6;
                            char c7;
                            char c8;
                            int n6;
                            int n7;
                            int n8;
                            int n9;
                            n3 = 0;
                            while (n3 < this.img_width) {
                                c8 = this.unsign(this.SrcVal(n3, n4 - 1, 0));
                                c7 = this.unsign(this.SrcVal(n3, n4 - 1, 1));
                                c6 = this.unsign(this.SrcVal(n3, n4 - 1, 2));
                                c5 = this.unsign(this.SrcVal(n3, n4 - 1, 3));
                                if (n3 > 0) {
                                    c4 = this.unsign(this.SrcVal(n3 - 1, n4, 0));
                                    c3 = this.unsign(this.SrcVal(n3 - 1, n4, 1));
                                    c2 = this.unsign(this.SrcVal(n3 - 1, n4, 2));
                                    c = this.unsign(this.SrcVal(n3 - 1, n4, 3));
                                } else {
                                    c4 = '\u0000';
                                    c3 = '\u0000';
                                    c2 = '\u0000';
                                    c = '\u0000';
                                }
                                if (n3 > 0) {
                                    n13 = this.unsign(this.SrcVal(n3 - 1, n4 - 1, 0));
                                    n12 = this.unsign(this.SrcVal(n3 - 1, n4 - 1, 1));
                                    n11 = this.unsign(this.SrcVal(n3 - 1, n4 - 1, 2));
                                    n10 = this.unsign(this.SrcVal(n3 - 1, n4 - 1, 3));
                                } else {
                                    n13 = 0;
                                    n12 = 0;
                                    n11 = 0;
                                    n10 = 0;
                                }
                                n9 = this.PaethPredictor(c4, c8, n13) + this.unsign(this.RawVal(n3, n4, 0));
                                n8 = this.PaethPredictor(c3, c7, n12) + this.unsign(this.RawVal(n3, n4, 1));
                                n7 = this.PaethPredictor(c2, c6, n11) + this.unsign(this.RawVal(n3, n4, 2));
                                n6 = this.PaethPredictor(c, c5, n10) + this.unsign(this.RawVal(n3, n4, 3));
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)0)] = (byte)(n9 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)1)] = (byte)(n8 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)2)] = (byte)(n7 % 256);
                                this.pngBitmapData[this.si((int)n3, (int)n4, (int)3)] = (byte)(n6 % 256);
                                ++n3;
                            }
                            break;
                        }
                    }
                    ++n4;
                }
            }
            catch (Exception exception) {
                Main.MainRef.Wt.outDebugString(this.path + " : " + exception.toString());
            }
        } else {
            fileIO.AdvanceBytes(n);
        }
        int n14 = fileIO.ReadInt();
    }

    public void destroy() {
        this.data = null;
        this.pngBitmapData = null;
        this.grayBitmapData = null;
        this.temp_byte_data = null;
        if (this.bitmap != null) {
            this.bitmap.destroy();
        }
        this.bitmap = null;
    }

    public void blur() {
        if (this.pngBitmapData == null) {
            return;
        }
        int n = 1;
        while (n < this.img_height - 1) {
            int n2 = 1;
            while (n2 < this.img_width - 1) {
                int n3 = this.unsign(this.SrcVal(n2 + 1, n, 0)) + this.unsign(this.SrcVal(n2 - 1, n, 0)) + this.unsign(this.SrcVal(n2, n + 1, 0)) + this.unsign(this.SrcVal(n2, n - 1, 0)) + this.unsign(this.SrcVal(n2, n, 0));
                int n4 = this.unsign(this.SrcVal(n2 + 1, n, 1)) + this.unsign(this.SrcVal(n2 - 1, n, 1)) + this.unsign(this.SrcVal(n2, n + 1, 1)) + this.unsign(this.SrcVal(n2, n - 1, 1)) + this.unsign(this.SrcVal(n2, n, 1));
                int n5 = this.unsign(this.SrcVal(n2 + 1, n, 2)) + this.unsign(this.SrcVal(n2 - 1, n, 2)) + this.unsign(this.SrcVal(n2, n + 1, 2)) + this.unsign(this.SrcVal(n2, n - 1, 2)) + this.unsign(this.SrcVal(n2, n, 2));
                this.pngBitmapData[this.si((int)n2, (int)n, (int)0)] = (byte)((n3 /= 5) % 256);
                this.pngBitmapData[this.si((int)n2, (int)n, (int)1)] = (byte)((n4 /= 5) % 256);
                this.pngBitmapData[this.si((int)n2, (int)n, (int)2)] = (byte)((n5 /= 5) % 256);
                ++n2;
            }
            ++n;
        }
    }

    private int si(int n, int n2, int n3) {
        switch (n3) {
            case 0: {
                return (n + n2 * this.img_width) * 4 + 2;
            }
            case 1: {
                return (n + n2 * this.img_width) * 4 + 1;
            }
            case 2: {
                return (n + n2 * this.img_width) * 4;
            }
            case 3: {
                return (n + n2 * this.img_width) * 4 + 3;
            }
        }
        return 0;
    }

    public boolean isLoaded() {
        if (!this.Loaded && this.data != null) {
            switch (this.data.status()) {
                case 0: {
                    Main.MainRef.Wt.outDebugString(this.path + " already loaded.");
                    this.onLoadComplete(null);
                    break;
                }
                case -6: {
                    Main.MainRef.Wt.outDebugString(this.path + " No such file. The file that you are trying to open does not exist. Note that if you try to open a file over the internet using the normal http: methods, this error value will never be set. Instead the remote web server will return seemingly valid data, often with the form: \"Not Found. The requested URL /foobat.txt was not found on this server.\" You must always check the returned data for validity!");
                    break;
                }
                case -9: {
                    Main.MainRef.Wt.outDebugString(this.path + " EOF. There is nothing more to read in the file AND an attempt has been made to read beyond the end of the file.");
                    break;
                }
                case -10: {
                    Main.MainRef.Wt.outDebugString(this.path + " Forbidden. Not all files can be opened, especially on the local machine. Attempting to open these files will return this forbidden status.");
                    break;
                }
                case -11: {
                    Main.MainRef.Wt.outDebugString(this.path + " Not Opened. The WTFile object has been created with the wt.readFile method, but the file specified has not yet been read. After calling readFile, it typically takes several seconds at least before the data is ready.");
                    break;
                }
                default: {
                    Main.MainRef.Wt.outDebugString(this.path + " Unknown error : " + this.data.status());
                }
            }
        }
        return this.Loaded;
    }

    public void blendPixel(int n, int n2, float f, float f2, float f3, float f4) {
        if (this.pngBitmapData == null) {
            return;
        }
        char c = this.unsign(this.SrcVal(n, n2, 0));
        char c2 = this.unsign(this.SrcVal(n, n2, 1));
        char c3 = this.unsign(this.SrcVal(n, n2, 2));
        this.pngBitmapData[this.si((int)n, (int)n2, (int)0)] = (byte)(((float)c * (1.0f - f) + f2 * f) % 256.0f);
        this.pngBitmapData[this.si((int)n, (int)n2, (int)1)] = (byte)(((float)c2 * (1.0f - f) + f3 * f) % 256.0f);
        this.pngBitmapData[this.si((int)n, (int)n2, (int)2)] = (byte)(((float)c3 * (1.0f - f) + f4 * f) % 256.0f);
    }

    private byte PaethPredictor(int n, int n2, int n3) {
        int n4 = n + n2 - n3;
        int n5 = Math.abs(n4 - n);
        int n6 = Math.abs(n4 - n2);
        int n7 = Math.abs(n4 - n3);
        if (n5 <= n6 && n5 <= n7) {
            return (byte)n;
        }
        if (n6 <= n7) {
            return (byte)n2;
        }
        return (byte)n3;
    }

    protected void finalize() {
        this.destroy();
    }

    public int getHeight() {
        return this.img_height;
    }

    private char unsign(byte by) {
        if (by < 0) {
            return (char)(by + 256);
        }
        return (char)by;
    }

    private void createBitmap() {
        FileIO fileIO;
        char[] cArray;
        byte[] byArray = this.data.readAll();
        if (byArray.length == 0) {
            Main.MainRef.Wt.outDebugString("WTFile failed [" + this.path + "]");
            Main.MainRef.Wt.outDebugString("------------------ ERROR DESCRIPTION ---------------------");
            switch (this.data.status()) {
                case 0: {
                    Main.MainRef.Wt.outDebugString("OK. No errors.");
                    break;
                }
                case -6: {
                    Main.MainRef.Wt.outDebugString("No such file. The file that you are trying to open does not exist. Note that if you try to open a file over the internet using the normal http: methods, this error value will never be set. Instead the remote web server will return seemingly valid data, often with the form: \"Not Found. The requested URL /foobat.txt was not found on this server.\" You must always check the returned data for validity!");
                    break;
                }
                case -9: {
                    Main.MainRef.Wt.outDebugString("EOF. There is nothing more to read in the file AND an attempt has been made to read beyond the end of the file.");
                    break;
                }
                case -10: {
                    Main.MainRef.Wt.outDebugString("Forbidden. Not all files can be opened, especially on the local machine. Attempting to open these files will return this forbidden status.");
                    break;
                }
                case -11: {
                    Main.MainRef.Wt.outDebugString("Not Opened. The WTFile object has been created with the wt.readFile method, but the file specified has not yet been read. After calling readFile, it typically takes several seconds at least before the data is ready.");
                    break;
                }
                default: {
                    Main.MainRef.Wt.outDebugString("Unknown error : " + this.data.status());
                }
            }
            Main.MainRef.Wt.outDebugString("Error Number : " + this.data.getErrorNumber());
            Main.MainRef.Wt.outDebugString("---------------- END ERROR DESCRIPTION -------------------");
            this.data = null;
            return;
        }
        if (!this.FromImagePack) {
            this.data.close();
            this.data = null;
        }
        if ((cArray = (fileIO = new FileIO(byArray)).ReadBytes(8))[0] != '\u0089' || cArray[1] != 'P' || cArray[2] != 'N' || cArray[3] != 'G' || cArray[4] != '\r' || cArray[5] != '\n' || cArray[6] != '\u001a' || cArray[7] != '\n') {
            return;
        }
        this.not_iend = true;
        while (!fileIO.Eof() && this.not_iend) {
            this.ReadChunk(fileIO);
        }
        fileIO.destroy();
        fileIO = null;
        this.bitmap = Main.MainRef.Wt.createBlankBitmap(this.img_width, this.img_height);
        if (this.TintImage) {
            this.grayBitmapData = new byte[this.img_width * this.img_height * 4];
            System.arraycopy(this.pngBitmapData, 0, this.grayBitmapData, 0, this.img_width * this.img_height * 4);
            this.Tint(this.TintR, this.TintG, this.TintB);
        } else if (this.img_colortype == 6) {
            this.bitmap.copyRectFromByteArray(this.pngBitmapData, 3);
        } else {
            this.bitmap.copyRectFromByteArray(this.pngBitmapData);
        }
        this.temp_byte_data = null;
    }

    private boolean byteArrayMatch(char[] cArray, char[] cArray2) {
        if (cArray.length == cArray2.length) {
            int n = 0;
            while (n < cArray.length) {
                if (cArray[n] != cArray2[n]) {
                    return false;
                }
                ++n;
            }
            return true;
        }
        return false;
    }

    public void Tint(float f, float f2, float f3) {
        f /= 255.0f;
        f2 /= 255.0f;
        f3 /= 255.0f;
        if (this.pngBitmapData == null || this.grayBitmapData == null) {
            return;
        }
        int n = 0;
        while (n < this.img_height) {
            int n2 = 0;
            while (n2 < this.img_width) {
                int n3 = this.unsign(this.GSrcVal(n2, n, 0));
                int n4 = this.unsign(this.GSrcVal(n2, n, 1));
                int n5 = this.unsign(this.GSrcVal(n2, n, 2));
                if (n3 == n4 && n3 == n5) {
                    n3 = (int)((float)n3 * f);
                    n4 = (int)((float)n4 * f2);
                    n5 = (int)((float)n5 * f3);
                    if (n3 > 255) {
                        n3 = 255;
                    }
                    if (n4 > 255) {
                        n4 = 255;
                    }
                    if (n5 > 255) {
                        n5 = 255;
                    }
                    if (n3 < 0) {
                        n3 = 0;
                    }
                    if (n4 < 0) {
                        n4 = 0;
                    }
                    if (n5 < 0) {
                        n5 = 0;
                    }
                    this.pngBitmapData[this.si((int)n2, (int)n, (int)0)] = (byte)(n3 % 256);
                    this.pngBitmapData[this.si((int)n2, (int)n, (int)1)] = (byte)(n4 % 256);
                    this.pngBitmapData[this.si((int)n2, (int)n, (int)2)] = (byte)(n5 % 256);
                }
                ++n2;
            }
            ++n;
        }
        if (this.img_colortype == 6) {
            this.bitmap.copyRectFromByteArray(this.pngBitmapData, 3);
            return;
        }
        this.bitmap.copyRectFromByteArray(this.pngBitmapData);
    }

    public int getWidth() {
        return this.img_width;
    }

    public void putPixel(int n, int n2, float f, float f2, float f3) {
        if (this.pngBitmapData == null) {
            return;
        }
        this.pngBitmapData[this.si((int)n, (int)n2, (int)0)] = (byte)(f % 256.0f);
        this.pngBitmapData[this.si((int)n, (int)n2, (int)1)] = (byte)(f2 % 256.0f);
        this.pngBitmapData[this.si((int)n, (int)n2, (int)2)] = (byte)(f3 % 256.0f);
    }

    public void getPixel(VEC3D vEC3D, int n, int n2) {
        if (this.pngBitmapData == null) {
            return;
        }
        vEC3D.X = this.unsign(this.SrcVal(n, n2, 0));
        vEC3D.Y = this.unsign(this.SrcVal(n, n2, 1));
        vEC3D.Z = this.unsign(this.SrcVal(n, n2, 2));
    }

    public void getStoredPixel(VEC3D vEC3D, int n, int n2) {
        if (this.pngBitmapData == null) {
            return;
        }
        vEC3D.X = this.unsign(this.GSrcVal(n, n2, 0));
        vEC3D.Y = this.unsign(this.GSrcVal(n, n2, 1));
        vEC3D.Z = this.unsign(this.GSrcVal(n, n2, 2));
    }

    public void pushTexture() {
        if (this.img_colortype == 6) {
            this.bitmap.copyRectFromByteArray(this.pngBitmapData, 3);
            return;
        }
        this.bitmap.copyRectFromByteArray(this.pngBitmapData);
    }

    public byte SrcVal(int n, int n2, int n3) {
        return this.pngBitmapData[this.si(n, n2, n3)];
    }

    public class FileIO {
        int pngIndex;
        char[] pngData;
        boolean eof;

        public char ReadChar() {
            ++this.pngIndex;
            return this.pngData[this.pngIndex - 1];
        }

        public boolean Eof() {
            return this.eof;
        }

        public FileIO() {
            UberImage.this.getClass();
        }

        public FileIO(byte[] byArray) {
            UberImage.this.getClass();
            this.pngData = this.ByteArrayToCharArray(byArray);
        }

        public int ReadInt() {
            char[] cArray = this.ReadBytes(4);
            if (this.eof) {
                return 0;
            }
            return cArray[0] << 24 | cArray[1] << 16 | cArray[2] << 8 | cArray[3];
        }

        public String ReadString() {
            String string = "";
            while (!this.Eof() && this.pngData[this.pngIndex] != '\u0000') {
                string = string + "" + this.pngData[this.pngIndex];
                ++this.pngIndex;
            }
            ++this.pngIndex;
            return string;
        }

        public void destroy() {
            this.pngData = null;
        }

        public byte ReadByte() {
            ++this.pngIndex;
            return (byte)this.pngData[this.pngIndex - 1];
        }

        public float ReadFloat() {
            int n = this.ReadInt();
            if (this.eof) {
                return 0.0f;
            }
            return Float.intBitsToFloat(n);
        }

        public void AdvanceBytes(int n) {
            this.pngIndex += n;
        }

        public byte[] ReadSignedBytes(int n) {
            byte[] byArray = new byte[n];
            int n2 = 0;
            while (n2 < n && this.pngIndex < this.pngData.length) {
                byArray[n2] = (byte)this.pngData[this.pngIndex];
                ++n2;
                ++this.pngIndex;
            }
            boolean bl = this.eof = this.pngData.length <= this.pngIndex;
            if (this.eof) {
                return null;
            }
            return byArray;
        }

        public char[] ReadBytes(int n) {
            char[] cArray = new char[n];
            int n2 = 0;
            while (n2 < n && this.pngIndex < this.pngData.length) {
                cArray[n2] = this.pngData[this.pngIndex];
                ++n2;
                ++this.pngIndex;
            }
            boolean bl = this.eof = this.pngData.length <= this.pngIndex;
            if (this.eof) {
                return null;
            }
            return cArray;
        }

        char[] ByteArrayToCharArray(byte[] byArray) {
            char[] cArray = new char[byArray.length];
            int n = 0;
            while (n < byArray.length) {
                cArray[n] = byArray[n] < 0 ? (char)(byArray[n] + 256) : (char)byArray[n];
                ++n;
            }
            return cArray;
        }

        public int ReadShort() {
            char[] cArray = this.ReadBytes(2);
            if (this.eof) {
                return 0;
            }
            return cArray[0] << 8 | cArray[1];
        }
    }
}

