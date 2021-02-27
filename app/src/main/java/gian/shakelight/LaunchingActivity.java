package gian.shakelight;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.CAMERA;
import static gian.shakelight.ServiceButtonState.OFFLINE;
import static gian.shakelight.ServiceButtonState.ONLINE;
import static gian.shakelight.ServiceButtonState.WAITING;

public class LaunchingActivity extends ComponentActivity {

    public static final String channelID = "FlashLightChannelID";
    private ImageButton setServiceBtn;
    private ImageView shakeTut;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        setContentView(R.layout.activity_main);
        TextView infoField = findViewById(R.id.infoField);
        setServiceBtn = findViewById(R.id.setServiceBtn);
        shakeTut = findViewById(R.id.shakeTut);

        if (!hasCamera()) {
            infoField.setText(R.string.infotextAppWontWork);
            setServiceBtn.setEnabled(false);
            return;
        }

        setServiceBtnImgTo(isShakeLightServiceRunning());

        setServiceBtn.setOnClickListener(view -> updateServiceStateAndUI(!isShakeLightServiceRunning()));

        new Thread(() -> {
            int animationSpeed = 200;
            final int[] frame = {0};
            int[] animation = {
                    R.drawable.shake_tut1,
                    R.drawable.shake_tut2,
                    R.drawable.shake_tut1,
                    R.drawable.shake_tut2,
                    R.drawable.shake_tut0,
                    R.drawable.shake_tut0,
                    R.drawable.shake_tut0,
                    R.drawable.shake_tut0,
                    R.drawable.shake_tut0,
            };
            while (true) {
                if (frame[0] == animation.length) {
                    frame[0] = 0;
                }
                runOnUiThread(() -> shakeTut.setImageResource(animation[frame[0]++]));
                try {
                    Thread.sleep(animationSpeed);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (!isShakeLightServiceRunning()) {
                updateServiceStateAndUI(true);
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                showInfoWhyPermissionIsNeeded();
            }
            this.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    updateServiceStateAndUI(true);
                }
            }).launch(CAMERA);
        }
    }

    private void updateServiceStateAndUI(boolean on) {
        boolean shakeLightServiceRunning = isShakeLightServiceRunning();
        if (shakeLightServiceRunning == on) {
            setServiceBtnImgTo(on);
            return;
        }
        updatePwrBtnUIOnChange(!on);
        if (on) {
            setShakeLightService(this);
        } else {
            unsetShakeLightService();
        }
    }

    private void updatePwrBtnUIOnChange(boolean oldStateService) {
        new Thread(() -> {
            setServiceBtnImgTo(WAITING);
            while (true) {
                if (isShakeLightServiceRunning() != oldStateService) {
                    boolean on = !oldStateService;
                    setServiceBtnImgTo(on);
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
    }

    private void setServiceBtnImgTo(boolean on) {
        setServiceBtnImgTo(on ? ONLINE : OFFLINE);
    }

    private void setServiceBtnImgTo(ServiceButtonState state) {
        switch (state) {
            case ONLINE:
                runOnUiThread(() -> setServiceBtn.setImageResource(R.drawable.button_online));
                return;
            case OFFLINE:
                runOnUiThread(() -> setServiceBtn.setImageResource(R.drawable.button_offline));
                return;
            case WAITING:
                runOnUiThread(() -> setServiceBtn.setImageResource(R.drawable.button_waiting));
        }
    }

    private void showInfoWhyPermissionIsNeeded() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Permission Info");
        alertDialog.setMessage("This App needs Permission to use the Camera, because it needs access to the Flashlight");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static void setShakeLightService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, ShakeLightService.class));
        } else {
            context.startService(new Intent(context, ShakeLightService.class));
        }
    }

    public void unsetShakeLightService() {
        stopService(new Intent(this, ShakeLightService.class));
    }


    /**
     * for API 26+ create notification channels
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(channelID,
                getString(R.string.channel_name),  //name of the channel
                NotificationManager.IMPORTANCE_LOW);   //importance level
        mChannel.setDescription(getString(R.string.channel_description));
        mChannel.setShowBadge(true);
        nm.createNotificationChannel(mChannel);
    }

    private boolean isShakeLightServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo service : manager.getRunningAppProcesses()) {
            if ("gian.shakelight:shakeLightServiceProcess".equals(service.processName)) {
                return true;
            }
        }
        String manufacturer = Build.MANUFACTURER;
        return false;
    }

    public void testMethod(View view) {
        new Thread(() -> isShakeLightServiceRunning()).start();
    }
}
//backlog:

//significant motion?

//https://developer.android.com/about/versions/oreo/background#services