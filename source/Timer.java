/*
 * Decompiled with CFR 0.152.
 */
public class Timer
implements Global {
    Message_3D Seconds;
    boolean Visible = false;
    boolean Active = false;
    int msecs = 0;
    int secs = 0;
    int LastSecs = -1;

    void hide() {
        if (this.Visible) {
            this.Visible = false;
            this.Seconds.hide();
            Main.MainRef.MenuManager.hideLoading();
        }
    }

    void deactivate() {
        this.Active = false;
    }

    void setTime(int n, int n2) {
        this.secs = n;
        this.msecs = n2;
        this.update(0);
    }

    void activate() {
        this.Active = true;
    }

    void update(int n) {
        Main.MainRef.MenuManager.updateLoading(n);
        this.msecs -= n;
        if (this.msecs <= 0) {
            if (this.secs > 0) {
                this.msecs += 1000;
                this.secs += -1;
                if (this.secs <= 10) {
                    Main.MainRef.GlobalMedia.Sound_Timer.play(false, 128);
                }
            } else {
                this.msecs = 0;
                this.secs = 0;
                if (!Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].weapon.Active && Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].WaitingTimer == 0.0f && !Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].BarActive) {
                    Packet packet = new Packet();
                    packet.Code = (short)4;
                    packet.Id = (short)Main.MainRef.network.PlayerNumber;
                    Main.MainRef.network.sendPacket(packet);
                    Main.MainRef.hud.addMessage("Time Up!", 0);
                    Main.MainRef.GameLoop.switchPlayers();
                    Main.MainRef.GlobalMedia.Sound_TimeUp.play(false, 127);
                    return;
                }
            }
        }
        if (this.secs != this.LastSecs) {
            if (this.Seconds != null) {
                this.Seconds.destroy();
            }
            this.Seconds = null;
            this.Seconds = new Message_3D("" + this.secs, 1, 1.0f, 40);
            this.Seconds.show(495.0f, 525.0f);
        }
        this.LastSecs = this.secs;
    }

    void show() {
        if (!this.Visible) {
            this.Visible = true;
            if (this.Seconds == null) {
                this.Seconds = new Message_3D("0", 1, 1.0f, 40);
            }
            this.Seconds.show(495.0f, 525.0f);
            Main.MainRef.MenuManager.showLoading(0.49248f, -0.98496f, 4.0f);
        }
    }
}

