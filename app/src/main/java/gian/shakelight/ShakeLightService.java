package gian.shakelight;

import android.app.Notification;
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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;

public class ShakeLightService extends Service implements SensorEventListener, Runnable {
    private long lastTick;
    static final int NOTIFICATION_ID = 345;

    private FlashlightI flashlight;
    private Thread serviceThread;
    private final String DEBUGLOGTAG = "FlashlightService";

    @Override
    public void onCreate() {
        Log.d(DEBUGLOGTAG, "onCreate");
        serviceThread = new Thread(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashlight = new Flashlight((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        } else {
            flashlight = new FlashlightSubM();
        }
        Notification notification = getVersionCompatibleNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUGLOGTAG, "onDestroy() called");
        serviceThread.interrupt();
    }

    private void unregisterSensorListener() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DEBUGLOGTAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]" + "ThreadId=" + Thread.currentThread());
        if (getSystemService(Context.SENSOR_SERVICE) == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (serviceThread.isAlive() || serviceThread.isInterrupted()) {
            Log.d(DEBUGLOGTAG, "Servicethread already started");
            return START_STICKY;
        }
        serviceThread.start();

//        stopService(new Intent(getApplicationContext(), ShakeLightService.class));

        return START_STICKY;
    }

    @NotNull
    private Notification getVersionCompatibleNotification() {
        CharSequence text = getText(R.string.notificationText);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LaunchingActivity.class), 0);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = getNotificationSuperO(text, contentIntent);
        } else {
            notification = getNotificationSubO(text, contentIntent);
        }
        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;
        return notification;
    }

    private Notification getNotificationSubO(CharSequence text, PendingIntent contentIntent) {
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.torch)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Shake-Light")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NotNull
    private Notification getNotificationSuperO(CharSequence text, PendingIntent contentIntent) {
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.torch)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setChannelId(LaunchingActivity.channelID)
                .setContentTitle("hcujfgvkhvb")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //System.out.println(event.values[2]);
        if (System.currentTimeMillis() - lastTick > 1000) {
            lastTick = System.currentTimeMillis();
//            flashlight.toggle();
//            System.out.println("lightOn = " + flashlight.isOn());
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

    private boolean registerSensorListener() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            return true;
        }
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, sensor, 10000);
        return false;
    }

    @Override
    public void run() {
        registerSensorListener();
        boolean running = true;
        while (running) {
            flashlight.toggle();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Thread ended");
                running = false;
            }
        }
    }
}
//https://stackoverflow.com/questions/42126979/cannot-keep-android-service-alive-after-app-is-closed

/*

startForeground
        Added in API level 5
public final void startForeground (int id,
        Notification notification)
        If your service is started (running through Context#startService(Intent)), then also make this service run in the foreground, supplying the ongoing notification to be shown to the user while in this state. By default started services are background, meaning that their process won't be given foreground CPU scheduling (unless something else in that process is foreground) and, if the system needs to kill them to reclaim more memory (such as to display a large page in a web browser), they can be killed without too much harm. You use startForeground(int, Notification) if killing your service would be disruptive to the user, such as if your service is performing background music playback, so the user would notice if their music stopped playing.

        Note that calling this method does not put the service in the started state itself, even though the name sounds like it. You must always call ContextWrapper.startService(android.content.Intent) first to tell the system it should keep the service running, and then use this method to tell it to keep it running harder.

        Apps targeting API Build.VERSION_CODES.P or later must request the permission Manifest.permission.FOREGROUND_SERVICE in order to use this API.

        Apps built with SDK version Build.VERSION_CODES.Q or later can specify the foreground service types using attribute R.attr.foregroundServiceType in service element of manifest file. The value of attribute R.attr.foregroundServiceType can be multiple flags ORed together.*/
