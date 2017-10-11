package us.pico.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import us.pico.R;
import us.pico.helper.GC;
import us.pico.service.BTConnect;

public class MainActivity extends Activity {


    Button btnRemote,testBtn,connector , btnNavigate;
    EditText inputMessage;
    TextView myLabel;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean lastUpdate = false;
    private Handler handler;

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
                case GC.ACTION_UPDATE_UI:
                    updateUI(intent.getBooleanExtra(GC.EXTRA_ACTION_UPDATE_UI,false));
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

        handler = new Handler();

        testBtn = (Button) findViewById(R.id.testBtn);
        connector = (Button) findViewById(R.id.btnConn);
        btnNavigate = (Button) findViewById(R.id.btn_navigate);
        myLabel = (TextView) findViewById(R.id.btResult);
        inputMessage = (EditText) findViewById(R.id.input_message);
        btnRemote = (Button) findViewById(R.id.btn_remote);
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                testBtn.setEnabled(s.length()>0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        btnNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,Search.class));
            }
        });


        // start send data handler

        testBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                String msg = inputMessage.getText().toString();
                inputMessage.setText("");
                testBtn.setEnabled(false);

                btService.performAction(msg);

            }
        });

        connector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btService.performAction("xxConnxx");
            }
        });

        btnRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btService.performAction("xxDisconnxx");

            }
        });



        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }


    }

    private void updateUI(boolean var) {

        if (lastUpdate != var && var){
            handler.post(new Runnable() {
                public void run() {
                    connector.setText("Disconnect");
                    inputMessage.setEnabled(true);
                    btnRemote.setEnabled(true);
                    myLabel.setText("Connected to PICO");
                }
            });
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
        filter.addAction(GC.ACTION_UPDATE_UI);
        registerReceiver(mBTReceiver, filter);

    }

}
