package com.newgame.usb2_4g;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity implements View.OnClickListener, Runnable {

    private static final String TAG = "MainActivity";

    // USB control commands
    private static final int COMMAND_UP = 1;
    private static final int COMMAND_DOWN = 2;
    private static final int COMMAND_RIGHT = 4;
    private static final int COMMAND_LEFT = 8;
    private static final int COMMAND_FIRE = 16;
    private static final int COMMAND_STOP = 32;
    private static final int COMMAND_STATUS = 64;

    // constants for accelerometer orientation
    private static final int TILT_LEFT = 1;
    private static final int TILT_RIGHT = 2;
    private static final int TILT_UP = 4;
    private static final int TILT_DOWN = 8;
    private static final double THRESHOLD = 5.0;
    Iterator<UsbDevice> deviceIterator;
    private Button mFire;
    private EditText et;
    private EditText et1;
    private EditText et2;
    private EditText et3;
    private EditText et4;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbDeviceConnection mConnection1;
    private UsbEndpoint mEndpointIntr;
    private UsbEndpoint mEndpointIntr1;
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;
    private UsbDevice device1;
    private UsbDevice device;
    private int mLastValue = 0;

    SensorEventListener mGravityListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {

            // compute current tilt
            int value = 0;
            if (event.values[0] < -THRESHOLD) {
                value += TILT_LEFT;
            } else if (event.values[0] > THRESHOLD) {
                value += TILT_RIGHT;
            }
            if (event.values[1] < -THRESHOLD) {
                value += TILT_UP;
            } else if (event.values[1] > THRESHOLD) {
                value += TILT_DOWN;
            }

            if (value != mLastValue) {
                mLastValue = value;
                // send motion command if the tilt changed
                switch (value) {
                    case TILT_LEFT:
                        sendCommand(COMMAND_LEFT);
                        break;
                    case TILT_RIGHT:
                        sendCommand(COMMAND_RIGHT);
                        break;
                    case TILT_UP:
                        sendCommand(COMMAND_UP);
                        break;
                    case TILT_DOWN:
                        sendCommand(COMMAND_DOWN);
                        break;
                    default:
                        sendCommand(COMMAND_STOP);
                        break;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        deviceIterator = deviceList.values().iterator();
    }

    @Override protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mGravityListener);
    }

    @Override protected void onResume() {
        super.onResume();
        mSensorManager
            .registerListener(mGravityListener, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        while (deviceIterator.hasNext()) {
            device1 = deviceIterator.next();

            if (device1.getVendorId() == 6421) {

                device = device1;


            }
        }

        //setDevice(device);
      /*  Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();

        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
            }
        }*/
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.quanxian:
                if (device1 != null) {
                    mUsbManager.requestPermission(device1, null);
                } else {
                    Toast.makeText(this, "device not ready!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.chaxun:
                // setDevice1(device);
                byte[] buffer = new byte[65];
                int k = 0;
                int l = 0;
                for (k = 0; k < 65; k++) {
                    buffer[k] = (byte) l;
                }
                int j = mConnection.bulkTransfer(mEndpointIntr1, buffer, 2, 1000);
                Toast.makeText(MainActivity.this,
                    "j=" + j + ";data=" + buffer[0] + buffer[1] + buffer[2], Toast.LENGTH_SHORT)
                    .show();
                break;
            case R.id.dakai:
                setDevice(device);
                break;
            case R.id.fasong:
                int pa1 = Integer.parseInt(et.getText().toString());
                int pa2 = Integer.parseInt(et1.getText().toString());
                int pa3 = Integer.parseInt(et2.getText().toString());
                int pa4 = Integer.parseInt(et3.getText().toString());
                int m = 0x84;
                int n = 0x58;

                byte[] message = new byte[2];
                message[0] = (byte) m;
                message[1] = (byte) n;
                int i =
                    mConnection.controlTransfer(0x20, 0x9, 0x300, 0, message, message.length, 0);
                //Toast.makeText(MissileLauncherActivity.this,"i="+i+"0x" +pa1+","+"0x" +pa2+","+"0x" +pa3+","+"0x0,0x" +pa4+",0x"+pa4,  Toast.LENGTH_SHORT).show();
                break;
            case R.id.fire:
                sendCommand(COMMAND_FIRE);
                break;
            default:
                break;
        }
    }

    private void findViews() {
        mFire = (Button) findViewById(R.id.fire);
        et = (EditText) findViewById(R.id.et);
        et1 = (EditText) findViewById(R.id.et1);
        et2 = (EditText) findViewById(R.id.et2);
        et3 = (EditText) findViewById(R.id.et3);
        et4 = (EditText) findViewById(R.id.et4);
    }

    private void initViews() {

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

    private void setDevice1(UsbDevice device) {
        if (device.getInterfaceCount() != 1) {
            // Toast.makeText(this, "could not find interface" +device.getInterfaceCount(),  Toast.LENGTH_SHORT).show();
            //   return;
        }
        UsbInterface intf = device.getInterface(2);
        // Toast.makeText(this, "could not find endpoint"+intf.getEndpointCount() ,	Toast.LENGTH_SHORT).show();
        // device should have one endpoint
        if (intf.getEndpointCount() != 1) {
            //Toast.makeText(this, "could not find endpoint"+intf.getEndpointCount() ,  Toast.LENGTH_SHORT).show();
            //return;
        }
        // endpoint should be of type interrupt
        UsbEndpoint ep1 = intf.getEndpoint(1);
        mEndpointIntr1 = ep1;

    }

    private void setDevice(UsbDevice device) {
        if (device.getInterfaceCount() != 1) {
            // Toast.makeText(this, "could not find interface" +device.getInterfaceCount(),  Toast.LENGTH_SHORT).show();
            //   return;
        }
        UsbInterface intf = device.getInterface(3);
        // Toast.makeText(this, "could not find endpoint"+intf.getEndpointCount() ,  Toast.LENGTH_SHORT).show();
        // device should have one endpoint
        if (intf.getEndpointCount() != 1) {
            //Toast.makeText(this, "could not find endpoint"+intf.getEndpointCount() ,  Toast.LENGTH_SHORT).show();
            //return;
        }
        // endpoint should be of type interrupt
        UsbEndpoint ep = intf.getEndpoint(0);

        UsbEndpoint ep1 = intf.getEndpoint(1);



        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
            // Toast.makeText(this, "endpoint is not interrupt type" ,  Toast.LENGTH_SHORT).show();
            return;
        }
        mDevice = device;
        mEndpointIntr = ep;

        mEndpointIntr1 = ep1;
        if (device != null) {
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
    }

    @Override public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, mEndpointIntr);
        byte status = -1;
        while (true) {
            // queue a request on the interrupt endpoint
            request.queue(buffer, 1);
            // send poll status command
            sendCommand(COMMAND_STATUS);
            // wait for status event
            if (mConnection.requestWait() == request) {
                byte newStatus = buffer.get(0);
                if (newStatus != status) {
                    Log.d(TAG, "got status " + newStatus);
                    status = newStatus;
                    if ((status & COMMAND_FIRE) != 0) {
                        // stop firing
                        sendCommand(COMMAND_STOP);
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            } else {
                Log.e(TAG, "requestWait failed, exiting");
                break;
            }
        }
    }

}
