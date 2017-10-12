package us.pico.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import us.pico.helper.GC;

/**
 * Created by ayush on 10/10/17.
 */

public class BTConnect extends Service {

    Intent intent;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    private static final String TAG = "BTConnect";
    int lastPos;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static boolean SERVICE_CONNECTED = false;

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();

    public void IsBoundable() {

    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private boolean btConnect() {

        Boolean cool = true;

        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            if (mmSocket == null)
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            } else {
                mmSocket.close();
                notifyBTConnectionChange(false);
                cool = false;
            }

        } catch (IOException e) {
            notifyBTConnectionChange(false);
            e.printStackTrace();
        }

        return cool;
    }

    private class workerThread implements Runnable {

        private String btMsg;

        workerThread(String msg) {
            btMsg = msg;
        }

        public void run() {
            boolean connected = true;
            switch (btMsg) {
                case "xxConnxx":
                    connected = btConnect();
                    break;
                case "xxDisconnxx":
                    sendBtMsg(btMsg);
                    connected = false;
                    break;
                default:
                    sendBtMsg(btMsg);
                    break;
            }

            try {
                while (!Thread.currentThread().isInterrupted() && connected) {
                    int bytesAvailable;


                    if (mmSocket!= null && mmSocket.isConnected()) {

                        notifyBTConnectionChange(true);

                        InputStream mmInputStream = mmSocket.getInputStream();

                        bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > lastPos) {

                            byte[] packetBytes = new byte[bytesAvailable - lastPos];
                            int read = mmInputStream.read(packetBytes);

                            if (read < 0) {
                                notifyBTConnectionChange(false);
                                mmSocket.close();
                                break;
                            } else {

                                String data = new String(packetBytes);
                                Log.d(TAG, "run: " + data);
                                lastPos = bytesAvailable;

                                if (data.equals("xxDisconnxx")) {
                                    notifyBTConnectionChange(false);
                                    mmSocket.close();
                                    break;
                                } else {
                                    Intent sendIntent = new Intent();
                                    sendIntent.putExtra(GC.EXTRA_BT_DATA, "Recieved by PICO " + data);
                                    sendIntent.setAction(GC.ACTION_BT_DATA_RECEIVED);
                                    sendBroadcast(sendIntent);
                                }

                            }

                        }

                    }
                }

                if (!connected) {

                    //Disconnected
                    mmSocket = null;
                    notifyBTConnectionChange(false);
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                notifyBTConnectionChange(false);
            }
        }
    }

    private void notifyBTConnectionChange(boolean isConnected) {

        Intent sendIntent = new Intent();
        sendIntent.setAction(GC.ACTION_BT_STATE_CHANGED);
        sendIntent.putExtra(GC.EXTRA_BT_CONNECTED, isConnected);
        sendBroadcast(sendIntent);
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

    public void performAction(String message) {

        (new Thread(new workerThread(message))).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SERVICE_CONNECTED = false;
    }
}
