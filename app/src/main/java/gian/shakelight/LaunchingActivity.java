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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.CAMERA;

public class LaunchingActivity extends ComponentActivity {

    public static final String channelID = "FlashLightChannelID";
    private ImageButton setServiceBtn;
    private ImageView shakeTut;
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            this.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setShakeLightService();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createchannel();
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

        setServiceBtn.setOnClickListener(view -> toggleServiceState());

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
//            setShakeLightService(null);
            //todo understand
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
            showInfoWhyPermissionIsNeeded();
        } else {
            requestCameraPermissionLauncher.launch(CAMERA);
        }
    }

    private void toggleServiceState() {
        boolean shakeLightServiceRunning = isShakeLightServiceRunning();
        updatePwrBtnUIOnChange(shakeLightServiceRunning);
        if (shakeLightServiceRunning) {
            unsetShakeLightService();
        } else {
            setShakeLightService();
        }
    }

    private void updatePwrBtnUIOnChange(boolean oldStateService) {
        new Thread(() -> {
            setServiceBtn.setImageResource(R.drawable.button_waiting);
            while (true) {
                if (isShakeLightServiceRunning() != oldStateService) {
                    setServiceBtnImgTo(!oldStateService);
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

    private void setServiceBtnImgTo(boolean online) {
        if (online) {
            runOnUiThread(() -> setServiceBtn.setImageResource(R.drawable.button_online));
        } else {
            runOnUiThread(() -> setServiceBtn.setImageResource(R.drawable.button_offline));
        }
    }

    private void showInfoWhyPermissionIsNeeded() {
        AlertDialog alertDialog = new AlertDialog.Builder(getApplicationContext()).create();
        alertDialog.setTitle("Permission Info");
        alertDialog.setMessage("This App needs Permission to use the Camera, because it needs access to the Flashlight");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void setShakeLightService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ShakeLightService.class));
        } else {
            startService(new Intent(this, ShakeLightService.class));
        }
    }

    public void unsetShakeLightService() {
        stopService(new Intent(this, ShakeLightService.class));
    }


    /**
     * for API 26+ create notification channels
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createchannel() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(channelID,
                getString(R.string.channel_name),  //name of the channel
                NotificationManager.IMPORTANCE_LOW);   //importance level
        //important level: default is is high on the phone.  high is urgent on the phone.  low is medium, so none is low?
        // Configure the notification channel.
        mChannel.setDescription(getString(R.string.channel_description));
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this channel, if the device supports this feature.
        mChannel.setShowBadge(true);
        nm.createNotificationChannel(mChannel);
    }

    private boolean isShakeLightServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ShakeLightService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
//backlog:

//significant motion?

//https://developer.android.com/about/versions/oreo/background#services