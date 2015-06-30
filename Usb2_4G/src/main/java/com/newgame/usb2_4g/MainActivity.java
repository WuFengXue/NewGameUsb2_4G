package com.newgame.usb2_4g;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends Activity implements View.OnClickListener, Runnable {

    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.newgame.action.USB_PREMISSION";

    // 读缓存大小（单位：字节）
    private static final int READ_BUFFER_SIZE = 32;

    // USB control commands
    private static final int COMMAND_UP = 1;
    private static final int COMMAND_DOWN = 2;
    private static final int COMMAND_RIGHT = 4;
    private static final int COMMAND_LEFT = 8;
    private static final int COMMAND_FIRE = 16;
    private static final int COMMAND_STOP = 32;
    private static final int COMMAND_STATUS = 64;

    private EditText et0;
    private EditText et1;
    private EditText et2;
    private EditText et3;
    private TextView tv;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mInEndpoint;
    private UsbEndpoint mOutEndpoint;
    private PendingIntent mUsbPermissionIntent;

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //                        Toast.makeText(MainActivity.this, "request permission success!" , Toast.LENGTH_SHORT).show();
                        setDevice(device);
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
        registerUsbReceiver();
    }

    @Override protected void onPause() {
        super.onPause();
    }

    @Override protected void onStop() {
        super.onStop();
        try {
            this.unregisterReceiver(mUsbReceiver);
        } catch (Exception ignored) {

        }
    }

    @Override protected void onResume() {
        super.onResume();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device != null) {
            setDevice(device);
        } else {
            for (UsbDevice dev : mUsbManager.getDeviceList().values()) {
                if (dev.getVendorId() == 6421) {
                    mUsbManager.requestPermission(dev, mUsbPermissionIntent);
                    break;
                }
            }
        }
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.quanxian:
                if (mUsbManager != null && mDevice != null) {
                    mUsbManager.requestPermission(mDevice, mUsbPermissionIntent);
                } else if (mUsbManager == null) {
                    Toast.makeText(this, "device not ready: mUsbManager is null!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "device not ready: mDevice is null!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.chaxun:
                boolean hasPermission = mUsbManager.hasPermission(mDevice);
                Toast.makeText(this, "has permission " + hasPermission, Toast.LENGTH_SHORT).show();
                break;
            case R.id.dakai:
                setDevice(mDevice);
                break;
            case R.id.fasong:
                sendData();
                break;
            default:
                break;
        }
    }

    @Override public void run() {
        Log.e(TAG, "read thread start");

        final ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, mInEndpoint);

        while (true) {
            // queue a request on the interrupt endpoint
            readBuffer.clear();
            request.queue(readBuffer, READ_BUFFER_SIZE);

            // wait for status event
            if (mConnection.requestWait() == request) {
                tv.post(new Runnable() {
                    @Override public void run() {
                        String data = Arrays.toString(readBuffer.array());
                        tv.setText("接收数据：" + data);
                    }
                });
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
            } else {
                Log.e(TAG, "read requestWait failed, exiting");
                break;
            }
        }
    }

    private void findViews() {
        et0 = (EditText) findViewById(R.id.et0);
        et1 = (EditText) findViewById(R.id.et1);
        et2 = (EditText) findViewById(R.id.et2);
        et3 = (EditText) findViewById(R.id.et3);
        tv = (TextView) findViewById(R.id.tv);
    }

    private void initViews() {

    }

    private void registerUsbReceiver() {
        mUsbPermissionIntent =
            PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    private void sendCommand(int control) {
        synchronized (this) {
            if (control != COMMAND_STATUS) {
                Log.d(TAG, "sendMove " + control);
            }
            if (mConnection != null) {
                byte[] message = new byte[2];
                message[0] = (byte) control;
                // Send command via a control request on endpoint zero
                // mConnection.controlTransfer(0x21, 0x9, 0x200, 0, message, message.length, 0);

            }
        }
    }

    private void setDevice(UsbDevice device) {
        Log.d(TAG, "setDevice " + device);

        if (!mUsbManager.hasPermission(device)) {
            Toast.makeText(this, "not permission", Toast.LENGTH_SHORT).show();
            return;
        }
        if (device.getInterfaceCount() != 4) {
            Toast.makeText(this, "could not find interface " + device.getInterfaceCount(),
                Toast.LENGTH_SHORT).show();
            return;
        }
        UsbInterface intf = device.getInterface(3);
        if (intf.getEndpointCount() != 2) {
            Toast.makeText(this, "could not find endpoint" + intf.getEndpointCount(),
                Toast.LENGTH_SHORT).show();
            return;
        }
        // endpoint should be of type interrupt
        UsbEndpoint inEp = intf.getEndpoint(0);
        UsbEndpoint outEp = intf.getEndpoint(1);
        if (inEp.getType() != UsbConstants.USB_ENDPOINT_XFER_INT
            || outEp.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
            Toast.makeText(this, "endpoint is not interrupt type", Toast.LENGTH_SHORT).show();
            return;
        }
        mDevice = device;
        mInEndpoint = inEp;
        mOutEndpoint = outEp;

        UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if (connection != null && connection.claimInterface(intf, true)) {
            Toast.makeText(this, "open SUCCESS", Toast.LENGTH_SHORT).show();
            mConnection = connection;
            Thread thread = new Thread(this);
            thread.start();
        } else {
            Toast.makeText(this, "open FAIL", Toast.LENGTH_SHORT).show();
            mConnection = null;
        }
    }

    /**
     * 发送数据
     */
    private void sendData() {
        byte[] writeBytes = new byte[32];
        writeBytes[0] = (byte) 0xa0;
        writeBytes[1] = Byte.parseByte(et0.getText().toString());
        writeBytes[2] = Byte.parseByte(et1.getText().toString());
        writeBytes[3] = Byte.parseByte(et2.getText().toString());
        writeBytes[4] = Byte.parseByte(et3.getText().toString());

        synchronized (this) {
            mConnection.bulkTransfer(mOutEndpoint, writeBytes, writeBytes.length, 3000);
        }
    }

}
