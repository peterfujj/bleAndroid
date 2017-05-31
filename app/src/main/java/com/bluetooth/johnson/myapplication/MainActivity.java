package com.bluetooth.johnson.myapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import johnsonBleLib.BleService;

public class MainActivity extends AppCompatActivity {

    private BleService mBleService;
    private  boolean mIsConnectedService = false;

    private final String ServiceUUid = "0000fff0-0000-1000-8000-00805f9b34fb";
    private final String ReadUUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    private final String WriteUUID = "0000fff2-0000-1000-8000-00805f9b34fb";

    private  JohnsonServiceConnection mServiceConnectionCB = new JohnsonServiceConnection();

    public class JohnsonServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBleService = ((BleService.JohnsonBinder)iBinder).getService();
            mIsConnectedService = true;
            mBleService.initialize();
            mBleService.startScan();
            Log.d("PETER","Service Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
            mIsConnectedService = false;
        }
    }
    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.CONNECTED_DEVICES);
        intentFilter.addAction(BleService.FIND_DEVICE);
        intentFilter.addAction(BleService.SCAN_FINISH);
        intentFilter.addAction(BleService.DISCONNECTED_DEVICES);
        return intentFilter;
    }
    private BroadcastReceiver mBleReciver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BleService.DISCONNECTED_DEVICES)){
                Log.d("Peter","DISCONNECTED_DEVICES");
            }else if(action.equals(BleService.CONNECTED_DEVICES)){
                Log.d("Peter","CONNECTED_DEVICES");
            }else if(action.equals(BleService.SCAN_FINISH)){
                Log.d("Peter","SCAN_FINISH");
            }else if(action.equals(BleService.FIND_DEVICE)){
                Log.d("Peter","find device");
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                mBleService.startConnect(tmpDevAddress);
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBleReciver);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this,BleService.class);
        bindService(intent,mServiceConnectionCB, Context.BIND_AUTO_CREATE);
        registerReceiver(mBleReciver,makeIntentFilter());
    }
}
