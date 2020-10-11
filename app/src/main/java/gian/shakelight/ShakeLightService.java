package gian.shakelight;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ShakeLightService extends Service implements SensorEventListener, Runnable {
    private long lastTick;
    public static boolean isServiceRunning = false;
    static final int NOTIFICATION_ID = 543;
    private NotificationManager mNM;

    private FlashlightI flashlight;
    private Thread servicethread;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashlight = new Flashlight((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        } else {
            flashlight = new FlashlightSubM();
        }
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        servicethread = new Thread(this);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            stopSelf();
            return START_STICKY; // value unimportant, because sensorManager shouldn't be null
        }
        servicethread.start();
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //System.out.println(event.values[2]);
        if (System.currentTimeMillis() - lastTick > 1000) {
            lastTick = System.currentTimeMillis();
            flashlight.toggle();
            System.out.println("lightOn = " + flashlight.isOn());
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.notificationText);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LaunchingActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.torch)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("hcujfgvkhvb")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(R.string.notificationText, notification);
    }

    @Override
    public void run() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        }
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, sensor, 10000);
        lastTick = System.currentTimeMillis();
    }
}
//https://stackoverflow.com/questions/42126979/cannot-keep-android-service-alive-after-app-is-closed
