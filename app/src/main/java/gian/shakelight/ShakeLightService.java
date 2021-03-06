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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;

public class ShakeLightService extends Service implements SensorEventListener {
    private static final int NOTIFICATION_ID = 345;
    private final String TAG = "FlashlightService";

    private boolean lastRightRound;
    private FlashlightI flashlight;
    private ArrayList<ShakeMarker> shakeQueue = new ArrayList<ShakeMarker>();
    private SensorManager sensorManager;

    // Settings
    private float zAngularForceThreshold = 5;
    private int ShakeToggleTriggerCounter = 5;
    private long ShakeTimeSpanMs = 500;
    private Vibrator vibrator;
    private static VibrationEffect vibrationEffect = createVibrationEffect();
    private static VibrationEffect vibrationEffectReverse = vibrationEffect; //todo make reverse effect

    private static VibrationEffect createVibrationEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            float effectLengthMs = 500;
            int resolution = 3;
            long[] timings = new long[resolution];
            int[] amps = new int[resolution];
            for (int i = 0; i < resolution; i++) {
                timings[i] = (long) (effectLengthMs / resolution);
                amps[i] = (int) (((float) i / resolution) * 255);
            }
            return VibrationEffect.createWaveform(timings, amps, -1);
        }
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate(): ThreadId=" + Thread.currentThread());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        startForeground(NOTIFICATION_ID, getVersionCompatibleNotification());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flashlight = new Flashlight((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        } else {
            flashlight = new FlashlightSubM();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy(): ThreadId=" + Thread.currentThread());
        flashlight.setLight(false);
        unregisterSensorListener();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]" + "ThreadId=" + Thread.currentThread());
        registerSensorListener();

        if (getSystemService(Context.SENSOR_SERVICE) == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

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
                .setTicker(text)  // the status text
                .setWhen(currentTimeMillis())  // the time stamp
                .setContentTitle(getString(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NotNull
    private Notification getNotificationSuperO(CharSequence text, PendingIntent contentIntent) {
        return new Notification.Builder(this)
                .setTicker(text)  // the status text
                .setWhen(currentTimeMillis())  // the time stamp
                .setChannelId(LaunchingActivity.channelID)
                .setContentTitle(getString(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float zAngularForce = event.values[2];
        long sTime = currentTimeMillis();
        if (zAngularForceThreshold < abs(zAngularForce)) {
            boolean rightRound = zAngularForce > zAngularForceThreshold;
            if (this.lastRightRound != rightRound) {
                shakeQueue.add(new ShakeMarker(currentTimeMillis()));
                Log.d("Profiler", "isInToggle: " + (currentTimeMillis() - sTime));
                if (isInToggleState()) {
                    Log.d("Profiler", "flashlight.toggle: " + (currentTimeMillis() - sTime));
                    flashlight.toggle();
                    Log.d("Profiler", "vibrate(flashlight.isOn()): " + (currentTimeMillis() - sTime));
                    vibrate(flashlight.isOn());
                    Log.d("Profiler", "shakeQueue.clear(): " + (currentTimeMillis() - sTime));
                    shakeQueue.clear();
                }
                Log.d(TAG, "ShakeQueue: size:" + shakeQueue.size() + " " + Arrays.toString(shakeQueue.toArray()));
            }
            this.lastRightRound = rightRound;
        }
    }

    private void vibrate(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (on)
                vibrator.vibrate(vibrationEffect);
            else
                vibrator.vibrate(vibrationEffectReverse);
        } else {
            if (on)
                vibrator.vibrate(500);
            else
                vibrator.vibrate(100);
        }
    }

    private boolean isInToggleState() {
        checkTrimQueue();
        return shakeQueue.size() > ShakeToggleTriggerCounter;
    }

    private void checkTrimQueue() {
        long currentTime = currentTimeMillis();
        for (int i = 0; i < shakeQueue.size(); i++) {
            if (shakeQueue.get(i).timeStamp > currentTime - ShakeTimeSpanMs) {
                trimQueue(i);
                return;
            }
        }
    }

    private void trimQueue(int till) {
        if (till > 0) {
            shakeQueue.subList(0, till).clear();
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
        if (sensorManager == null) {
            return true;
        }
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, sensor, 10000);
        return false;
    }

    private void unregisterSensorListener() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }

    private class ShakeMarker {
        final long timeStamp;

        public ShakeMarker(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        @Override
        public String toString() {
            return "ms in past: " + (currentTimeMillis() - timeStamp);
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
