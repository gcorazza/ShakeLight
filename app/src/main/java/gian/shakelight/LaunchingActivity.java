package gian.shakelight;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
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

    private FlashlightI flashlight;
    public static final String channelID = "FlashLightChannelID";


    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            this.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setShakeLightService(null);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashlight = new Flashlight((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        } else {
            flashlight = new FlashlightSubM();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createchannel();
        }

        setContentView(R.layout.activity_main);
        TextView infoField = findViewById(R.id.infoField);
        Button setServiceBtn = findViewById(R.id.setServiceBtn);
        Switch litghSwitch = findViewById(R.id.lightSwitch);

        if (!hasCamera()) {
            infoField.setText(R.string.infotextAppWontWork);
            setServiceBtn.setEnabled(false);
            litghSwitch.setEnabled(false);
            return;
        }

        litghSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            flashlight.setLight(isChecked);
        });

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            //setShakeLightService(null);
            //todo understand
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
            showInfoWhyPermissionIsNeeded();
        } else {
            requestCameraPermissionLauncher.launch(CAMERA);
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

    public void setShakeLightService(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ShakeLightService.class));
        } else {
            startService(new Intent(this, ShakeLightService.class));
        }
    }

    public void unsetShakeLightService(View v) {
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

}
//backlog:

//significant motion?

//https://developer.android.com/about/versions/oreo/background#services