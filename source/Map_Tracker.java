/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTConstants
 *  wildtangent.webdriver.WTFile
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import java.util.StringTokenizer;
import wildtangent.webdriver.WTConstants;
import wildtangent.webdriver.WTFile;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class Map_Tracker
implements WTConstants,
WTOnLoadEvent,
Global {
    WTFile File;
    boolean Loaded = false;
    String Path = "";
    int Maps = 0;
    String[] MapNames = new String[50];
    String[] MapPaths = new String[50];
    String[] MapEnvironments = new String[50];
    String[] MapMusic = new String[50];
    VEC3D[] MapSun = new VEC3D[50];
    VEC3D[] MapSunVec = new VEC3D[50];
    boolean[] HasSun = new boolean[50];
    Media_Object_Shader[] MapThumbnail = new Media_Object_Shader[50];
    float[] MapScale = new float[50];
    int[][] MapColor = new int[50][3];
    int[][] MapSunColor = new int[50][3];

    public void onLoadComplete(WTObject wTObject) {
        this.parseDescriptor();
    }

    public Map_Tracker(String string) {
        this.Path = string;
        this.File = Main.MainRef.Wt.readFile(Main.MainRef.MediaPath + this.Path + "/maplist.dat", 1);
        this.File.setOnLoad((WTOnLoadEvent)this);
    }

    boolean isLoaded() {
        return this.Loaded;
    }

    void parseDescriptor() {
        String string = null;
        String string2 = null;
        int n = 1;
        while (!this.File.eof()) {
            string = this.File.readLine();
            StringTokenizer stringTokenizer = new StringTokenizer(string, ":,");
            if (stringTokenizer.hasMoreTokens() && (string2 = stringTokenizer.nextToken()).equalsIgnoreCase("<MAP>")) {
                if (stringTokenizer.countTokens() >= 14) {
                    this.MapNames[this.Maps] = stringTokenizer.nextToken();
                    this.MapPaths[this.Maps] = stringTokenizer.nextToken();
                    this.MapEnvironments[this.Maps] = stringTokenizer.nextToken();
                    this.MapThumbnail[this.Maps] = (Media_Object_Shader)Main.MainRef.MediaList.add(new Media_Object_Shader(stringTokenizer.nextToken() + "/image.png", false, false), true);
                    this.MapMusic[this.Maps] = stringTokenizer.nextToken();
                    this.MapScale[this.Maps] = Float.valueOf(stringTokenizer.nextToken()).floatValue();
                    this.MapSun[this.Maps] = new VEC3D(Float.valueOf(stringTokenizer.nextToken()).floatValue(), Float.valueOf(stringTokenizer.nextToken()).floatValue(), Float.valueOf(stringTokenizer.nextToken()).floatValue());
                    this.MapSunVec[this.Maps] = new VEC3D();
                    this.MapSunVec[this.Maps].setEqual(this.MapSun[this.Maps]);
                    this.MapSunVec[this.Maps].Y *= 3.0f;
                    this.MapSunVec[this.Maps].normalize();
                    this.MapSunVec[this.Maps].multiply(-1.0f);
                    this.MapColor[this.Maps][0] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.MapColor[this.Maps][1] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.MapColor[this.Maps][2] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.MapSunColor[this.Maps][0] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.MapSunColor[this.Maps][1] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.MapSunColor[this.Maps][2] = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    int n2 = Float.valueOf(stringTokenizer.nextToken()).intValue();
                    this.HasSun[this.Maps] = n2 == 1;
                    ++this.Maps;
                } else {
                    Main.MainRef.showAlert("ERROR IN MAP LIST " + this.Path + "/maplist.dat" + " - LINE " + n);
                }
            }
            ++n;
        }
        this.File.close();
        this.File = null;
        this.Loaded = true;
    }
}

