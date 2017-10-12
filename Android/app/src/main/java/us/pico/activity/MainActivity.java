package us.pico.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import us.pico.R;
import us.pico.helper.GC;
import us.pico.service.BTConnect;

public class MainActivity extends Activity {


    Button btnConnect, btnNavigate, btnMusic, btnEndJourney;
    RelativeLayout viewConnected;
    TextView textInfo;
    ProgressDialog dialog;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean lastUpdate = true;

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
                    updateUI(intent.getBooleanExtra(GC.EXTRA_BT_CONNECTED, false));
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
        setContentView(R.layout.activity_main);

        btnConnect = (Button) findViewById(R.id.btnConn);
        btnNavigate = (Button) findViewById(R.id.btn_navigate);
        btnMusic = (Button) findViewById(R.id.btn_music);
        btnEndJourney = (Button) findViewById(R.id.btn_end_journey);
        viewConnected = (RelativeLayout) findViewById(R.id.view_connected);
        textInfo = (TextView) findViewById(R.id.btResult);

        dialog = new ProgressDialog(this);

        btnNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Search.class));
            }
        });

        btnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Coming soon! ;)", Toast.LENGTH_SHORT).show();
            }
        });

        btnEndJourney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btService.performAction("xxDisconnxx");
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setMessage("Building connection");
                dialog.show();
                btService.performAction("xxConnxx");
            }
        });

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        updateUI(false);

    }

    private void updateUI(boolean var) {

        if (lastUpdate != var) {
            if (var) {
                btnConnect.setVisibility(View.GONE);
                viewConnected.setVisibility(View.VISIBLE);
                btnEndJourney.setVisibility(View.VISIBLE);
                textInfo.setText("Let's Begin!");
            } else {
                btnConnect.setVisibility(View.VISIBLE);
                viewConnected.setVisibility(View.GONE);
                btnEndJourney.setVisibility(View.GONE);
                textInfo.setText("Ready for a new journey");
            }
        }

        lastUpdate = var;
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBTReceiver);
        if (isServiceBound) {
            // Detach our existing connection.
            unbindService(btConnection);
            isServiceBound = false;
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
        if (dialog.isShowing())
            dialog.dismiss();
    }

}
