/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  wildtangent.webdriver.WTCamera
 *  wildtangent.webdriver.WTContainer
 *  wildtangent.webdriver.WTDrop
 *  wildtangent.webdriver.WTGroup
 *  wildtangent.webdriver.WTObject
 *  wildtangent.webdriver.WTStage
 *  wildtangent.webdriver.WTVector3D
 */
import wildtangent.webdriver.WTCamera;
import wildtangent.webdriver.WTContainer;
import wildtangent.webdriver.WTDrop;
import wildtangent.webdriver.WTGroup;
import wildtangent.webdriver.WTObject;
import wildtangent.webdriver.WTStage;
import wildtangent.webdriver.WTVector3D;

public class Camera
implements Global {
    WTGroup Camera;
    WTCamera CameraView;
    WTVector3D TempVector2;
    WTVector3D TempVector;
    VEC3D Temp = new VEC3D();
    VEC3D Temp2 = new VEC3D();
    VEC3D Forward = new VEC3D();
    VEC3D Right = new VEC3D();
    VEC3D Up = new VEC3D();
    WTDrop Pointer;
    boolean MouseVisible = false;
    int MouseX = 0;
    int MouseY = 0;
    WTGroup Environment;
    boolean EnvironmentVisible = false;
    float X = 0.0f;
    float Y = 0.0f;
    float Z = 0.0f;
    float ChangeX = 0.0f;
    float ChangeY = 0.0f;
    float ChangeZ = 0.0f;
    float WorldTargetX = 0.0f;
    float WorldTargetY = 0.0f;
    float WorldTargetZ = 0.0f;
    float BallTrackTime = 0.0f;
    float RecoilTimer = 0.0f;
    float TempY = 0.0f;
    VEC3D TargetLocation = new VEC3D();
    int CameraViews = 5;
    int CurrentCameraView = 1;
    int CameraEffects = 0;
    float CameraEffectsTimer = 0.0f;
    float CameraEffectsAmplitude = 0.0f;
    float CameraFlash = 0.0f;
    boolean GlareVisible = false;
    boolean FadeVisible = false;
    boolean Suspended = false;
    boolean CameraRecentered = false;
    boolean ShotCamActive = false;
    float ShotAccel = 0.1f;
    boolean ToolTipVisible = false;
    float SuccessAngle = 0.0f;
    float FOVFactor = 1.0f;

    void updateMouse(int n, int n2) {
        this.MouseX = n;
        this.MouseY = n2;
        if (this.MouseVisible) {
            this.Pointer.setPosition(n, n2);
        }
    }

    void showEnvironment() {
        if (!this.EnvironmentVisible) {
            Main.MainRef.wt_stage.Stage.setFogColor(Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][0], Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][1], Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][2]);
            Main.MainRef.wt_stage.Stage.setBGColor(Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][0], Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][1], Main.MainRef.MapTracker.MapColor[Main.MainRef.ActiveMap][2]);
            Main.MainRef.wt_stage.Directional.setColor(Main.MainRef.MapTracker.MapSunColor[Main.MainRef.ActiveMap][0], Main.MainRef.MapTracker.MapSunColor[Main.MainRef.ActiveMap][1], Main.MainRef.MapTracker.MapSunColor[Main.MainRef.ActiveMap][2]);
            Main.MainRef.wt_stage.Stage.setFogStartDistance(200.0f);
            Main.MainRef.wt_stage.Stage.setFogEndDistance(1000.0f);
            Main.MainRef.wt_stage.Stage.setFogEnabled(true);
            Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Environment);
            this.EnvironmentVisible = true;
        }
    }

    void suspend() {
        if (!this.Suspended) {
            this.CameraView.suspend();
            this.Suspended = true;
        }
    }

    void applyCameraView(float f) {
        switch (this.CurrentCameraView) {
            case 6: {
                this.SuccessAngle = 0.0f;
                break;
            }
            case 0: {
                this.setCannonCamera(f);
                break;
            }
            case 1: {
                this.setShotCamera(f);
                break;
            }
            case 2: {
                this.setMediumCamera(f);
                break;
            }
            case 3: {
                this.setHighCamera(f);
                break;
            }
            case 4: {
                this.setBarrelCamera(f);
            }
        }
        this.updateCamera(0.001f, 0);
    }

    void setFOV(float f) {
        this.FOVFactor = f / 50.0f;
        this.CameraView.setFieldOfView(f);
    }

    void addCameraToGroup(WTGroup wTGroup) {
        this.removeCameraFromParent();
        wTGroup.addObject((WTContainer)this.Camera);
        this.assembleCamera();
    }

    void cameraShock(float f, float f2, float f3, float f4) {
        float f5 = Library_Math.camDistance3D(f, f2, f3);
        if (f5 < f4) {
            this.CameraEffects = 1;
            this.CameraEffectsAmplitude = (f4 - f5) * (100.0f / f4) / 1000.0f;
            this.CameraEffectsTimer = 600.0f;
        }
    }

    void setBarrelCamera(float f) {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].BarrelTilt.addObject((WTContainer)this.Camera);
        this.assembleCamera();
        this.Camera.setPosition(0.0f, 0.0f, 6.0f);
        this.Camera.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
        Main.MainRef.hud.addMessage("Activating Barrel Camera", 1);
        Main.MainRef.hud.showReticle();
    }

    void disableBarrelCamera() {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].BarrelTilt.removeObject((WTContainer)this.Camera);
        this.addCameraToStage();
        Main.MainRef.hud.hideReticle();
    }

    void updateCannonCamera(int n, float f, int n2) {
        this.Temp.fill(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetFinal.X - this.X, Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetFinal.Y - this.Y, Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetFinal.Z - this.Z);
        float f2 = this.Temp.length();
        this.Temp.multiply(9.5f * f);
        float f3 = this.Temp.length();
        if (f3 > f2) {
            this.Temp.normalize();
            this.Temp.multiply(f2);
        }
        this.X += this.Temp.X;
        this.Y += this.Temp.Y;
        this.Z += this.Temp.Z;
        this.TempY = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y < this.TempY + 4.0f) {
            this.Y = this.TempY + 4.0f;
        }
        if (this.Y < 8.0f) {
            this.Y = 8.0f;
        }
        this.Forward.setEqual(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].BarrelTargetFinal);
        this.Forward.subtract(this.X, this.Y, this.Z);
        this.Forward.normalize();
        this.Up.fill(0.0f, 1.0f, 0.0f);
        this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
        this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
        this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
        this.Right.normalize();
        this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
        this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
        this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
        this.Forward.Normalize();
        this.Up.Normalize();
        this.Right.Normalize();
        if (n2 == 0) {
            this.Camera.setPosition(this.X, this.Y, this.Z);
            this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
        }
    }

    void addCameraToStage() {
        this.removeCameraFromParent();
        Main.MainRef.wt_stage.StageGroup.addObject((WTContainer)this.Camera);
        this.assembleCamera();
    }

    void hideMouse() {
        if (this.MouseVisible) {
            this.CameraView.removeDrop(this.Pointer);
            this.MouseVisible = false;
        }
    }

    void setViewRect() {
    }

    public Camera(int n, int n2, int n3) {
        this.CameraView = Main.MainRef.wt_stage.Stage.createCamera();
        this.CameraView.setViewRect(0, 0, n, n2);
        this.CameraView.setClipping(6000.0f, 1.0f);
        this.Camera = Main.MainRef.Wt.createGroup();
        this.addCameraToStage();
        this.Environment = Main.MainRef.Wt.createGroup();
    }

    void assembleCamera() {
        this.Camera.addObject((WTContainer)this.CameraView);
        this.CameraView.setPosition(0.0f, 0.0f, 0.0f);
        this.CameraView.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
    }

    void removeCameraFromParent() {
        WTObject wTObject = this.Camera.getOwner();
        if (wTObject == null) {
            return;
        }
        int n = wTObject.getObjectType();
        if (n == 8194) {
            ((WTStage)this.Camera.getOwner()).removeObject((WTContainer)this.Camera);
            return;
        }
        if (n == 54) {
            ((WTGroup)this.Camera.getOwner()).removeObject((WTContainer)this.Camera);
        }
    }

    void hideEnvironment() {
        if (this.EnvironmentVisible) {
            Main.MainRef.wt_stage.Stage.setBGColor(0, 0, 0);
            Main.MainRef.wt_stage.Stage.setFogEnabled(false);
            Main.MainRef.wt_stage.Directional.setColor(255, 255, 255);
            Main.MainRef.wt_stage.Directional.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
            Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Environment);
            this.EnvironmentVisible = false;
        }
    }

    void cycleCamera(float f) {
        if (this.CurrentCameraView == 4) {
            this.disableBarrelCamera();
        }
        ++this.CurrentCameraView;
        if (this.CurrentCameraView >= this.CameraViews) {
            this.CurrentCameraView = 0;
        }
        this.applyCameraView(f);
    }

    void setCannonCamera(float f) {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetVector.fill(0.0f, 6.0f, -20.0f);
        Main.MainRef.hud.addMessage("Activating Cannon Camera", 1);
    }

    void positionForMenu() {
        this.Camera.setPosition(0.0f, 0.0f, 0.0f);
        this.Camera.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
        this.CameraView.setPosition(0.0f, 0.0f, 0.0f);
        this.CameraView.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
    }

    void setSpectatorCamera() {
        Main.MainRef.hud.addMessage("Activating Spectator Camera", 1);
        Main.MainRef.hud.hideBar();
        if (this.CurrentCameraView == 4) {
            this.disableBarrelCamera();
        }
        this.CurrentCameraView = 99;
        this.refreshSpectatorCamera();
        if (Main.MainRef.hud.SpectatorMessage == null) {
            Main.MainRef.hud.SpectatorMessage = new Message_3D("Spectator Mode", 1, 1.0f, 30);
            Main.MainRef.hud.SpectatorMessage.show(400.0f, 550.0f);
        }
    }

    void updateSuccessCamera(int n, float f, int n2) {
        this.Temp.fill(0.0f, 10.0f, -60.0f);
        this.Temp.rotateY(Library_Math.degreesToRadians(this.SuccessAngle));
        this.SuccessAngle += f * 10.0f;
        int n3 = 0;
        int n4 = 0;
        while (n4 < Main.MainRef.CannonCount) {
            if (Main.MainRef.cannon[n4].Active) {
                n3 = n4;
                break;
            }
            ++n4;
        }
        this.Temp.add(Main.MainRef.cannon[n3].Position);
        this.Temp.subtract(this.X, this.Y, this.Z);
        float f2 = this.Temp.length();
        this.Temp.multiply(9.5f * f);
        float f3 = this.Temp.length();
        if (f3 > f2) {
            this.Temp.normalize();
            this.Temp.multiply(f2);
        }
        this.X += this.Temp.X;
        this.Y += this.Temp.Y;
        this.Z += this.Temp.Z;
        this.TempY = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
        if (this.Y < this.TempY + 4.0f) {
            this.Y = this.TempY + 4.0f;
        }
        if (this.Y < 8.0f) {
            this.Y = 8.0f;
        }
        this.Forward.setEqual(Main.MainRef.cannon[n3].Position);
        this.Forward.subtract(this.X, this.Y, this.Z);
        this.Forward.normalize();
        this.Up.fill(0.0f, 1.0f, 0.0f);
        this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
        this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
        this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
        this.Right.normalize();
        this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
        this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
        this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
        this.Forward.Normalize();
        this.Up.Normalize();
        this.Right.Normalize();
        if (n2 == 0) {
            this.Camera.setPosition(this.X, this.Y, this.Z);
            this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
        }
    }

    void destroy() {
        Main.MainRef.wt_stage.StageGroup.removeObject((WTContainer)this.Environment);
        this.Environment = null;
        this.Camera.removeObject((WTContainer)this.CameraView);
        this.removeCameraFromParent();
        this.Camera = null;
        this.CameraView = null;
    }

    void resume() {
        if (this.Suspended) {
            this.CameraView.resume();
            this.Suspended = false;
        }
    }

    void showMouse() {
        if (!this.MouseVisible) {
            this.Pointer = this.CameraView.addDrop(Main.MainRef.GlobalMedia.MousePointer.Image, true);
            this.Pointer.make3d();
            this.Pointer.setPosition(this.MouseX, this.MouseY);
            this.MouseVisible = true;
        }
    }

    void positionCamera() {
        this.Camera.setPosition(this.X, this.Y, this.Z);
    }

    void updateCamera(float f, int n) {
        switch (this.CameraEffects) {
            case 1: {
                this.rattleCamera(f);
            }
        }
        if (this.CurrentCameraView == 99) {
            this.updateShotCamera(Main.MainRef.network.CurrentPlayer, f, n);
        }
        if (this.CurrentCameraView == 6) {
            this.updateSuccessCamera(Main.MainRef.network.CurrentPlayer, f, n);
            return;
        }
        if (this.CurrentCameraView == 0 || this.CurrentCameraView == 2 || this.CurrentCameraView == 3) {
            this.updateCannonCamera(Main.MainRef.network.PlayerNumber, f, n);
            return;
        }
        if (this.CurrentCameraView == 4) {
            this.Forward.setEqual(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].BarrelTargetVector);
            this.Forward.rotateX(Library_Math.degreesToRadians(-Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].ActiveTilt));
            this.Forward.rotateY(Library_Math.degreesToRadians(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].SpinAngle));
            this.Temp.setEqual(this.Forward);
            this.Temp.normalize();
            this.Temp.multiply(12.0f);
            this.Temp.add(Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].Position);
            this.X = this.Temp.X;
            this.Y = this.Temp.Y;
            this.Z = this.Temp.Z;
            this.Up.fill(0.0f, 1.0f, 0.0f);
            this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
            this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
            this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
            this.Right.normalize();
            this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
            this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
            this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
            this.Forward.Normalize();
            this.Up.Normalize();
            this.Right.Normalize();
            if (n == 0) {
                this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
                return;
            }
        } else if (this.CurrentCameraView == 1) {
            this.updateShotCamera(Main.MainRef.network.PlayerNumber, f, n);
        }
    }

    void setMediumCamera(float f) {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetVector.fill(0.0f, 50.0f, -50.0f);
        Main.MainRef.hud.addMessage("Activating Medium Camera", 1);
    }

    void refreshSpectatorCamera() {
        this.ShotCamActive = false;
        if (Main.MainRef.hud.SpectatorName != null) {
            Main.MainRef.hud.SpectatorName.destroy();
        }
        Main.MainRef.hud.SpectatorName = null;
        Main.MainRef.hud.SpectatorName = new Message_3D(Main.MainRef.cannon[Main.MainRef.network.CurrentPlayer].Name, 1, 1.0f, 30, 1);
        Main.MainRef.hud.SpectatorName.show(400.0f, 570.0f);
    }

    void updateShotCamera(int n, float f, int n2) {
        if (this.ShotCamActive) {
            if (!Main.MainRef.cannon[n].weapon.Active) {
                this.Temp.fill(Main.MainRef.cannon[n].weapon.X, Main.MainRef.cannon[n].weapon.Y, Main.MainRef.cannon[n].weapon.Z);
                this.Temp.subtract(this.X, this.Y, this.Z);
                this.Temp.normalize();
                this.Temp.multiply(-80.0f);
                this.Temp.add(Main.MainRef.cannon[n].weapon.X, Main.MainRef.cannon[n].weapon.Y, Main.MainRef.cannon[n].weapon.Z);
                this.Temp.subtract(this.X, this.Y, this.Z);
                float f2 = this.Temp.length();
                this.Temp.multiply(6.0f * f);
                float f3 = this.Temp.length();
                if (f3 > f2) {
                    this.Temp.normalize();
                    this.Temp.multiply(f2);
                }
                this.X += this.Temp.X;
                this.Y += this.Temp.Y;
                this.Z += this.Temp.Z;
                this.TempY = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
                if (this.Y < this.TempY + 4.0f) {
                    this.Y = this.TempY + 4.0f;
                }
                this.Camera.setPosition(this.X, this.Y, this.Z);
                this.BallTrackTime += f;
                if (this.BallTrackTime > 4.0f) {
                    this.ShotCamActive = false;
                    return;
                }
            } else {
                this.BallTrackTime = 0.0f;
                this.ShotAccel += f;
                if (this.ShotAccel > 7.0f) {
                    this.ShotAccel = 7.0f;
                }
                if (this.RecoilTimer < 1.0f) {
                    this.RecoilTimer += f;
                    this.Temp.fill(Main.MainRef.cannon[n].CameraTargetFinal.X - this.X, Main.MainRef.cannon[n].CameraTargetFinal.Y - this.Y, Main.MainRef.cannon[n].CameraTargetFinal.Z - this.Z);
                    float f4 = this.Temp.length();
                    this.Temp.multiply(this.ShotAccel * f);
                    float f5 = this.Temp.length();
                    if (f5 > f4) {
                        this.Temp.normalize();
                        this.Temp.multiply(f4);
                    }
                } else {
                    this.Temp.fill(Main.MainRef.cannon[n].weapon.X, Main.MainRef.cannon[n].weapon.Y, Main.MainRef.cannon[n].weapon.Z);
                    this.Temp2.fill(Main.MainRef.cannon[n].weapon.TrajectoryX, Main.MainRef.cannon[n].weapon.TrajectoryY, Main.MainRef.cannon[n].weapon.TrajectoryZ);
                    this.Temp2.normalize();
                    this.Temp2.multiply(10.0f);
                    this.Temp.subtract(this.Temp2);
                    this.Temp.subtract(this.X, this.Y, this.Z);
                    float f6 = this.Temp.length();
                    this.Temp.multiply(this.ShotAccel * f);
                    float f7 = this.Temp.length();
                    if (f7 > f6) {
                        this.Temp.normalize();
                        this.Temp.multiply(f6);
                    }
                }
                this.X += this.Temp.X;
                this.Y += this.Temp.Y;
                this.Z += this.Temp.Z;
                this.TempY = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
                if (this.Y < this.TempY + 4.0f) {
                    this.Y = this.TempY + 4.0f;
                }
                this.Forward.fill(Main.MainRef.cannon[n].weapon.X, Main.MainRef.cannon[n].weapon.Y, Main.MainRef.cannon[n].weapon.Z);
                this.Forward.subtract(this.X, this.Y, this.Z);
                this.Forward.normalize();
                this.Up.fill(0.0f, 1.0f, 0.0f);
                this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
                this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
                this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
                this.Right.normalize();
                this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
                this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
                this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
                this.Forward.Normalize();
                this.Up.Normalize();
                this.Right.Normalize();
                if (n2 == 0) {
                    this.Camera.setPosition(this.X, this.Y, this.Z);
                    this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
                    return;
                }
            }
        } else if (Main.MainRef.cannon[n].weapon.Active) {
            this.ShotCamActive = true;
            this.ShotAccel = 0.1f;
            this.RecoilTimer = 0.0f;
            Main.MainRef.cannon[n].CameraTargetVector.fill(0.0f, 6.0f, -30.0f);
            this.Forward.fill(Main.MainRef.cannon[n].weapon.X, Main.MainRef.cannon[n].weapon.Y, Main.MainRef.cannon[n].weapon.Z);
            this.Forward.subtract(this.X, this.Y, this.Z);
            this.Forward.normalize();
            this.Up.fill(0.0f, 1.0f, 0.0f);
            this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
            this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
            this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
            this.Right.normalize();
            this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
            this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
            this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
            this.Forward.Normalize();
            this.Up.Normalize();
            this.Right.Normalize();
            if (n2 == 0) {
                this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
                return;
            }
        } else {
            this.Temp.fill(Main.MainRef.cannon[n].CameraTargetFinal.X - this.X, Main.MainRef.cannon[n].CameraTargetFinal.Y - this.Y, Main.MainRef.cannon[n].CameraTargetFinal.Z - this.Z);
            float f8 = this.Temp.length();
            this.Temp.multiply(9.5f * f);
            float f9 = this.Temp.length();
            if (f9 > f8) {
                this.Temp.normalize();
                this.Temp.multiply(f8);
            }
            this.X += this.Temp.X;
            this.Y += this.Temp.Y;
            this.Z += this.Temp.Z;
            this.TempY = Main.MainRef.island.getTerrainHeight(this.X, this.Z);
            if (this.Y < this.TempY + 2.0f) {
                this.Y = this.TempY + 2.0f;
            }
            this.Forward.setEqual(Main.MainRef.cannon[n].BarrelTargetFinal);
            this.Forward.subtract(this.X, this.Y, this.Z);
            this.Forward.normalize();
            this.Up.fill(0.0f, 1.0f, 0.0f);
            this.Right.X = this.Up.Y * this.Forward.Z - this.Up.Z * this.Forward.Y;
            this.Right.Y = this.Up.Z * this.Forward.X - this.Up.X * this.Forward.Z;
            this.Right.Z = this.Up.X * this.Forward.Y - this.Up.Y * this.Forward.X;
            this.Right.normalize();
            this.Up.X = this.Forward.Y * this.Right.Z - this.Forward.Z * this.Right.Y;
            this.Up.Y = this.Forward.Z * this.Right.X - this.Forward.X * this.Right.Z;
            this.Up.Z = this.Forward.X * this.Right.Y - this.Forward.Y * this.Right.X;
            this.Forward.Normalize();
            this.Up.Normalize();
            this.Right.Normalize();
            if (n2 == 0) {
                this.Camera.setPosition(this.X, this.Y, this.Z);
                this.Camera.setAbsoluteOrientationVector(this.Forward.X, this.Forward.Y, this.Forward.Z, this.Up.X, this.Up.Y, this.Up.Z);
            }
        }
    }

    void updateObjects_Environment() {
        if (this.EnvironmentVisible) {
            this.Environment.setPosition(this.X, this.Y, this.Z);
        }
    }

    void setCamera(int n, float f) {
        if (this.CurrentCameraView == 4) {
            this.disableBarrelCamera();
        }
        this.CurrentCameraView = n;
        this.applyCameraView(f);
    }

    void setShotCamera(float f) {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetVector.fill(0.0f, 6.5f, -22.0f);
        this.ShotCamActive = false;
        Main.MainRef.hud.addMessage("Activating Shot Camera", 1);
    }

    void setHighCamera(float f) {
        Main.MainRef.cannon[Main.MainRef.network.PlayerNumber].CameraTargetVector.fill(0.0f, 100.0f, -100.0f);
        Main.MainRef.hud.addMessage("Activating High Camera", 1);
    }

    void rattleCamera(float f) {
        float f2 = Main.MainRef.random.nextFloat() - 0.5f;
        float f3 = Main.MainRef.random.nextFloat() - 0.5f;
        Main.MainRef.hud.HUDGroup.setPosition(f2 * this.CameraEffectsAmplitude / 400.0f, f3 * this.CameraEffectsAmplitude / 400.0f, 0.010015f);
        this.CameraEffectsTimer -= f;
        this.CameraEffectsAmplitude *= 0.9f;
        if (this.CameraEffectsTimer <= 0.0f) {
            this.CameraView.setOrientation(0.0f, 1.0f, 0.0f, 0.0f);
            Main.MainRef.hud.HUDGroup.setPosition(0.0f, 0.0f, 1.0001f);
            this.CameraEffects = 0;
        }
    }
}

