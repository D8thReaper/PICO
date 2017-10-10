package us.pico.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import us.pico.R;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    Button btnRemote,testBtn,connector;
    EditText inputMessage;
    TextView myLabel;

    int lastPos;
    private boolean lastUpdate = false;
    private Handler handler;

    private boolean btConnect() {

        Boolean cool = true;

        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            if (mmSocket== null)
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            } else {
                mmSocket.close();
                handler.post(new Runnable() {
                    public void run() {
                        myLabel.setText("Disconnected from PICO");
                        inputMessage.setEnabled(false);
                        inputMessage.setText("");
                        btnRemote.setEnabled(false);
                        connector.setText("Connect");
                    }
                });
                cool = false;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return cool;
    }

    public void sendBtMsg(String msg2send) {
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg2send.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        testBtn = (Button) findViewById(R.id.testBtn);
        connector = (Button) findViewById(R.id.btnConn);
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

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run() {
                boolean connected = true;
                if (btMsg.equals("xxConnxx"))
                    connected = btConnect();
                else
                    sendBtMsg(btMsg);

                while (!Thread.currentThread().isInterrupted() && connected) {
                    int bytesAvailable;

                    try {


                        if (mmSocket.isConnected()) {
                            updateUI(true);

                            final InputStream mmInputStream;
                            mmInputStream = mmSocket.getInputStream();
                            bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > lastPos) {

                                byte[] packetBytes = new byte[bytesAvailable-lastPos];
                                Log.d(TAG, "new bytes available");
                                final String data = new String(packetBytes, "US-ASCII");
                                Log.d(TAG, "run: " + data);
                                lastPos = bytesAvailable;

                                if (data.equals("xxDisconnxx")) {
                                    mmSocket.close();
                                    handler.post(new Runnable() {
                                        public void run() {
                                            myLabel.setText("Disconnected from PICO");
                                            inputMessage.setEnabled(false);
                                            inputMessage.setText("");
                                            btnRemote.setEnabled(false);
                                            connector.setText("Connect");
                                        }
                                    });
                                    break;
                                } else {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            myLabel.setText("Recieved by PICO");
                                        }
                                    });
                                }

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                if (!connected) {

                    //The variable data now contains our full command
                    handler.post(new Runnable() {
                        public void run() {
                            myLabel.setText("Disconnected");
                        }
                    });
                }
            }
        }
        ;


        // start send data handler

        testBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                String msg = inputMessage.getText().toString();
                inputMessage.setText("");
                testBtn.setEnabled(false);
                (new Thread(new workerThread(msg))).start();

            }
        });

        connector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                (new Thread(new workerThread("xxConnxx"))).start();
            }
        });

        btnRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                (new Thread(new workerThread("xxDisconnxx"))).start();

            }
        });


        //end send data handler

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("PICO")) //Note, you will need to change this to match the name of your device
                {
                    Log.e("PICO", device.getName());
                    mmDevice = device;
                    break;
                }
            }
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

}
