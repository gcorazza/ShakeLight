package gian.shakelight;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class Flashlight implements FlashlightI {

    private final CameraManager cameraManager;
    private boolean flashLightStatus;

    public Flashlight(CameraManager cameraManager1) {
        cameraManager = cameraManager1;
    }

    public void setLight(boolean on) {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, on);
            flashLightStatus = on;
        } catch (CameraAccessException e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean isOn() {
        return flashLightStatus;
    }

    public void toggle() {
        setLight(!flashLightStatus);
    }
}
