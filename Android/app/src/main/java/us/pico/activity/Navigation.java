package us.pico.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import us.pico.R;
import us.pico.fragment.MapFragmentView;
import us.pico.helper.GC;
import us.pico.service.BTConnect;

public class Navigation extends AppCompatActivity {

    private static final String TAG = "Navigation";
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private MapFragmentView m_mapFragmentView;
    public double destLat, destLong;
    public double startLat, startLong;
    private ProgressDialog dialog;

    // Variables for handling socket services
    private BTConnect btService;
    private boolean isServiceBound;

    /*
     * Notifications from SocketService will be received here.
     */
    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case GC.ACTION_BT_STATE_CHANGED:
                    hideDialog();
                    break;
            }
        }
    };

    private ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btService = ((BTConnect.LocalBinder) service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        requestPermissions();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            destLat = extras.getDouble("destLat");
            destLong = extras.getDouble("destLong");
            startLat = extras.getDouble("startLat");
            startLong = extras.getDouble("startLong");

            Log.d(TAG, "onCreate: " + destLat);
            Log.d(TAG, "onCreate: " + destLong);
            Log.d(TAG, "onCreate: " + startLat);
            Log.d(TAG, "onCreate: " + startLong);
        }
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private void requestPermissions() {

        final List<String> requiredSDKPermissions = new ArrayList<String>();
        requiredSDKPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredSDKPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requiredSDKPermissions.add(Manifest.permission.INTERNET);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

        ActivityCompat.requestPermissions(this,
                requiredSDKPermissions.toArray(new String[requiredSDKPermissions.size()]),
                REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permissions[index])) {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                /**
                 * All permission requests are being handled.Create map fragment view.Please note
                 * the HERE SDK requires all permissions defined above to operate properly.
                 */
                m_mapFragmentView = new MapFragmentView(this);
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBTServiceFilters(); // Start listening notifications from UsbService
        startBTService(BTConnect.class, btConnection); // Start SocketService(if it was not started before) and Bind it
    }

    private void startBTService(Class<?> service, ServiceConnection serviceConnection) {
        if (!BTConnect.SERVICE_CONNECTED) {
            startService(new Intent(this, service));
        }

        bindService(new Intent(this, service), serviceConnection, Context.BIND_AUTO_CREATE);
        isServiceBound = true;
        if (btService != null) {
            btService.IsBoundable();
        }
    }

    private void setBTServiceFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GC.ACTION_BT_STATE_CHANGED);
        registerReceiver(mBTReceiver, filter);

    }

    private void hideDialog() {

    }

    @Override
    public void onDestroy() {
        m_mapFragmentView.onDestroy();
        super.onDestroy();
    }

    public void sendNavData(String packet) {
        btService.performAction(packet);
    }
}
