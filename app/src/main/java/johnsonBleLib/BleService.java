package johnsonBleLib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BleService extends Service {
    public static  final  String TAG = "JohnsonBle";
    public static  final  String FIND_DEVICE  = "FIND_DEVICE";
    public static final  String SCAN_FINISH = "SCAN_FINISH";
    public static  final String CONNECTED_DEVICES = "CONNECTED_DEVICES";
    public static final String DISCONNECTED_DEVICES = "DISCONNECTED_DEVICES";
    public static final int DEVICE_CONNECTING = 0;
    public static final int DEVICE_CONNECTED = 1;
    public static final int DEVICE_DISCONNECTING = 2;
    public static final int DEVICE_DISCONNECTED = 3;

    private  JohnsonBinder mJohnsonBinder = new JohnsonBinder();
    private static  BleService mBleService  = null;

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private String mBluetoothDeviceAddress;
    private final int DEFAULT_PERIOD = 10*1000;
    private  boolean mIsScaning = false;
    private int mConnectState = DEVICE_DISCONNECTED ;
    private  int mScaningPeriod;

    private List<BluetoothDevice> mDeviceList = new ArrayList<>();


    public class JohnsonBinder extends Binder{
        public BleService getService(){
            return BleService.this;
        }
    }

    public boolean initialize(){
        boolean ret = false;
        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null)
                return false;
        }
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if(mBluetoothAdapter == null)
                return false;
        }
        return true;
    }

    public boolean startScan(){
        return startScan(DEFAULT_PERIOD);
    }

    public boolean stopScan(){
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mIsScaning = false;

        return true;
    }

    public boolean startScan(int scanPeriod){
        if(mIsScaning)
            return false;
        mDeviceList.clear();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mIsScaning = false;
                broadcastUpdate(SCAN_FINISH);
            }
        },scanPeriod);
        mScaningPeriod = scanPeriod;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mIsScaning = true;
        return true;
    }
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

            broadcastUpdate(FIND_DEVICE,bluetoothDevice);
        }
    };
    public boolean startConnect(final String address){
        boolean ret = false;
        if(mIsScaning) {
            stopScan();
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectState = DEVICE_CONNECTING;
        return true;

    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(BluetoothProfile.STATE_CONNECTED == newState ){
                mConnectState = DEVICE_CONNECTED;
                Log.d("Peter","DEVICE_CONNECTED");
                gatt.discoverServices();
            }else if(BluetoothProfile.STATE_DISCONNECTING == newState){
                Log.d("Peter","DEVICE_DISCONNECTING");
                mConnectState = DEVICE_DISCONNECTING;
            }else if(BluetoothProfile.STATE_DISCONNECTED == newState){
                Log.d("Peter","STATE_DISCONNECTED");
                mConnectState = DEVICE_DISCONNECTED;
            }else if(BluetoothProfile.STATE_CONNECTING == newState){
                Log.d("Peter","STATE_CONNECTING");
                mConnectState = DEVICE_CONNECTING;
            }
        }
        private List<BluetoothGattService> gattServiceList;
        private List<String> serviceList;
        private List<String[]> characteristicList;
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                gattServiceList = gatt.getServices();
                serviceList.clear();
            }
        }
    };

    private boolean containsDevices(List<BluetoothDevice> list,BluetoothDevice device){
        for(int i=0;i<list.size();i++){
            BluetoothDevice listDevice = list.get(i);
            if(listDevice.getAddress().equals(device.getAddress())){
                return true;
            }
        }
        return false;
    }

    private void broadcastUpdate(final String action, BluetoothDevice device) {
        final Intent intent = new Intent(action);
        String deviceName = device.getName();

        if(deviceName.contains("IndoorCycle")||deviceName.contains("Indoor Cycle")
            ||deviceName.contains("Indoorcycle")||deviceName.contains("indoorcycle")) {
            if(!containsDevices(mDeviceList,device))
                mDeviceList.add(device);
            intent.putExtra("name", device.getName());
            intent.putExtra("address", device.getAddress());
            Log.d("Peter",device.getAddress());
            sendBroadcast(intent);
        }
    }
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);

        sendBroadcast(intent);
    }

    public BleService() {
        mBleService = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mJohnsonBinder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
