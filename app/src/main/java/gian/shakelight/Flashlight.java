package gian.shakelight;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Flashlight {

    private final CameraManager cameraManager;
    private boolean flashLightStatus;

    public Flashlight(CameraManager cameraManager1) {
        cameraManager = cameraManager1;
    }

    public void setLight(boolean on) {
        if (on) {
            turnOn();
        } else {
            turnOff();
        }
    }

    private void turnOn() {

        try {
            String cameraId = null;
            cameraId = cameraManager.getCameraIdList()[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, true);
            }
            flashLightStatus = true;
        } catch (CameraAccessException e) {
        }
    }

    private void turnOff() {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
            flashLightStatus = false;
        } catch (CameraAccessException e) {
        }
    }

    public boolean isOn() {
        return flashLightStatus;
    }

    public void toggle() {
        setLight(flashLightStatus);
        flashLightStatus = !flashLightStatus;
    }
}
