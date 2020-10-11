package gian.shakelight;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

public class FlashlightSubM implements FlashlightI {
    private boolean lightOn = false;
    private Camera camera;


    public void turnOn() {
        try {
            if (camera == null) {
                camera = Camera.open();
            }
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(getFlashOnParameter());
            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            camera.autoFocus((success, camera1) -> {
            });
        } catch (Exception e) {
            // We are expecting this to happen on devices that don't support autofocus.
        }
    }

    public void turnOff() {
        if (camera == null) {
            return;
        }
        try {
            camera.stopPreview();
            camera.release();
            camera = null;
        } catch (Exception e) {
            // This will happen if the camera fails to turn on.
        }
    }

    private String getFlashOnParameter() {
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();

        if (flashModes.contains(FLASH_MODE_TORCH)) {
            return FLASH_MODE_TORCH;
        } else if (flashModes.contains(FLASH_MODE_ON)) {
            return FLASH_MODE_ON;
        } else if (flashModes.contains(FLASH_MODE_AUTO)) {
            return FLASH_MODE_AUTO;
        }
        throw new RuntimeException();
    }

    public void setLight(boolean on) {
        if (on) {
            turnOn();
        } else {
            turnOff();
        }
        lightOn = on;
    }

    @Override
    public void toggle() {
        setLight(!lightOn);
    }

    @Override
    public boolean isOn() {
        return lightOn;
    }

}

