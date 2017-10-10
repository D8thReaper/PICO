package us.pico.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by ayush on 10/10/17.
 */

public class BTConnect extends Service {

    private static final String TAG = "BTConnect";
    public static boolean SERVICE_CONNECTED = false;

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BTConnect getService() {
            return BTConnect.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_CONNECTED = true;
        Log.d(TAG, "onCreate: Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SERVICE_CONNECTED = false;
    }
}
