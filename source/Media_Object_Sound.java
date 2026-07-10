/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTAudioClip
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTOnLoadEvent
 */
import wildtangent.webdriver.WTAudioClip;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTOnLoadEvent;

public class Media_Object_Sound
extends Media_Object
implements Global {
    WTAudioClip Sound;
    boolean SoundCreated = false;
    boolean Looping = false;
    float FallOff = 800.0f;
    int LastPan = 0;
    int LastVolume = 0;

    public void stop() {
        if (!this.SoundCreated && Main.MainRef.SoundsEnabled) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
            return;
        }
        if (this.Sound != null) {
            this.Sound.stop();
        }
    }

    public void onLoadComplete(WTObject wTObject) {
        this.Loaded = true;
    }

    public void setDepth(float f) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        if (f > this.FallOff) {
            f = this.FallOff;
        }
        f /= this.FallOff;
        f = 1.0f - f;
        if ((f = (float)((int)(127.0f * f))) < 1.0f) {
            f = 1.0f;
        }
        if (f > 127.0f) {
            f = 127.0f;
        }
        if ((int)f != this.LastVolume) {
            this.Sound.setVolume((int)f);
        }
        this.LastVolume = (int)f;
    }

    public Media_Object_Sound(String string) {
        this.Path = string;
        this.Type = 3;
        if (Main.MainRef.SoundsEnabled) {
            this.SoundCreated = true;
            this.Sound = this.Path.endsWith("wwv") ? Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path, Main.MainRef.CacheType) : Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
            return;
        }
        this.Loaded = true;
    }

    public void destroy() {
        if (!this.Loaded || !this.SoundCreated) {
            return;
        }
        if (this.Sound.getIsPlaying()) {
            this.Sound.stop();
        }
        this.Sound = null;
    }

    public void setVolume(int n) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        if (n > 127) {
            n = 127;
        }
        if (n != this.LastVolume) {
            this.Sound.setVolume(n);
        }
        this.LastVolume = n;
    }

    public void play(boolean bl, int n) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        this.Sound.setVolume(n);
        this.Sound.start(bl);
        this.Looping = bl;
    }

    public void setPan(int n) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        if (n != this.LastPan) {
            this.Sound.setPan(n);
        }
        this.LastPan = n;
    }

    public boolean isLoaded() {
        if (!this.Loaded && this.Sound.getIsLoaded()) {
            this.Loaded = true;
        }
        return this.Loaded;
    }

    public void playDepth(boolean bl, float f) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv", Main.MainRef.CacheType);
            this.Sound.setName(this.Path);
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        this.Looping = bl;
        if (f > this.FallOff) {
            return;
        }
        f /= this.FallOff;
        f = 1.0f - f;
        if ((f = (float)((int)(127.0f * f))) < 1.0f) {
            f = 1.0f;
        }
        if (f > 127.0f) {
            f = 127.0f;
        }
        this.Sound.start(bl);
        this.Sound.setVolume((int)f);
        this.LastVolume = (int)f;
    }

    public boolean isPlaying() {
        return this.getIsPlaying();
    }

    public void setFrequency(float f) {
        if (!Main.MainRef.SoundsEnabled) {
            return;
        }
        if (!this.SoundCreated) {
            this.SoundCreated = true;
            this.Sound = Main.MainRef.Wt.createAudioClip(Main.MainRef.MediaPath + this.Path + "/sound.wwv");
            this.Sound.setOnLoad((WTOnLoadEvent)this);
        }
        if (f < 1000.0f) {
            f = 1000.0f;
        }
        this.Sound.setFrequency((int)f);
    }

    public boolean getIsPlaying() {
        if (!Main.MainRef.SoundsEnabled) {
            return false;
        }
        return this.Sound.getIsPlaying();
    }
}

