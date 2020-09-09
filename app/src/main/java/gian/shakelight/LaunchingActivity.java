package gian.shakelight;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class LaunchingActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void setWorker(View v) {
        WorkRequest shakeLightWorkerRequest =
                new OneTimeWorkRequest.Builder(ShakeLightWorker.class)
                        .build();

        WorkManager
                .getInstance(v.getContext())
                .enqueue(shakeLightWorkerRequest);

    }
}
