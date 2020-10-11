package com.joniras.anglesensor.angle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;


/**
 * Class that is responsible for communication with the Sensor
 * Also holds all the necessary UUIDs
 */
public class SensorCommunicator extends BluetoothGattCallback {
    private final String TAG = SensorCommunicator.class.getSimpleName();
    private BluetoothGatt bluetoothGatt;
    private Context context;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_ANGLE_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_BATTERY_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_BATTERY_DATA_AVAILABLE";
    public final static String EXTRA_BATTERY =
            "com.example.bluetooth.le.EXTRA_BATTERY";
    public final static String EXTRA_ANGLE_X =
            "com.example.bluetooth.le.EXTRA_ANGLE_X";
    public final static String EXTRA_ANGLE_Y =
            "com.example.bluetooth.le.EXTRA_ANGLE_Y";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    final static UUID BLSERVICE_GENERIC_ACCESS = convertFromInteger(0x1800);
    final static UUID BLCHARACTERISTIC_GA_DEVICE_NAME = convertFromInteger(0x2A00);
    final static UUID BLCHARACTERISTIC_GA_APPEARANCE = convertFromInteger(0x2A01);
    final static UUID BLCHARACTERISTIC_GA_CONN_PARAMS = convertFromInteger(0x2A04);


    final static UUID BLSERVICE_GENERIC_ATTRIBUTE_PROFILE = convertFromInteger(0x1801);
    final static UUID BLCHARACTERISTIC_GAP_ = convertFromInteger(0x2A05);


    final static UUID BLSERVICE_GENERIC_ANGLE = convertFromInteger(0x1820);
    final static UUID BLCHARACTERISTIC_A_ANGLE = convertFromInteger(0x2A70);
    final static UUID BLDESCRIPTOR_A_ANGLE = convertFromInteger(0x2902);


    final static UUID BLSERVICE_GENERIC_BATTERY = convertFromInteger(0x180F);
    final static UUID BLCHARACTERISTIC_B_BATTERY = convertFromInteger(0x2A19);
    final static UUID BLDESCRIPTOR_B_BATTERY = convertFromInteger(0x2902);


    final static UUID BLSERVICE_GENERIC_DEVICE_INFORMATION = convertFromInteger(0x180A);
    final static UUID BLCHARACTERISTIC_GDI_REVISION = convertFromInteger(0x2A27);
    final static UUID BLCHARACTERISTIC_GDI_VERSION = convertFromInteger(0x2A26);
    final static UUID BLCHARACTERISTIC_GDI_SOFTWARE = convertFromInteger(0x2A28);
    final static UUID BLCHARACTERISTIC_GDI_MANUFACTURER = convertFromInteger(0x2A29);
    final static UUID BLCHARACTERISTIC_GDI_MODEL_NUMBER = convertFromInteger(0x2A24);


    private static SensorCommunicator instance = new SensorCommunicator();

    private static SensorInformation info = new SensorInformation();

    public static SensorCommunicator getInstance() {
        return instance;
    }

    private SensorCommunicator() {

    }

    public void connect(BluetoothDevice device, Context context) {
        bluetoothGatt = device.connectGatt(context, false, this);
        this.context = context;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Connected to GATT server.");
            Log.i(TAG, "Attempting to start service discovery:" +
                    bluetoothGatt.discoverServices());

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected from GATT server.");
            sendBroadcast(new Intent(ACTION_GATT_DISCONNECTED));
        }
    }

    private void sendBroadcast(Intent intent) {
        context.sendBroadcast(intent);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "onServicesDiscovered success");
            sendBroadcast(new Intent(ACTION_GATT_CONNECTED));
            sendBroadcast(new Intent(ACTION_GATT_SERVICES_DISCOVERED));
            //writeSampleRate(gatt, BLSERVICE_GENERIC_ANGLE, BLCHARACTERISTIC_A_ANGLE, 1);
            //readChara(gatt, BLSERVICE_GENERIC_BATTERY, BLCHARACTERISTIC_B_BATTERY);
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
            Log.i(TAG, "Attempting to start service discovery:" +
                    bluetoothGatt.discoverServices());
        }
    }

    private void readChara(BluetoothGatt gatt, UUID service, UUID characteristic) {
        BluetoothGattCharacteristic chara = gatt.getService(service)
                .getCharacteristic(characteristic);
        boolean successfull = gatt.readCharacteristic(chara);
        Log.i(TAG, "Read characteristic successfull: " + successfull);
    }

    void turnOnNotifications() {
        BluetoothGattCharacteristic chara =
                bluetoothGatt.getService(SensorCommunicator.BLSERVICE_GENERIC_ANGLE)
                        .getCharacteristic(SensorCommunicator.BLCHARACTERISTIC_A_ANGLE);
        bluetoothGatt.setCharacteristicNotification(chara, true);

        BluetoothGattDescriptor desc = chara.getDescriptor(SensorCommunicator.BLDESCRIPTOR_A_ANGLE);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(desc);
    }


    void turnOffNotifications() {
        BluetoothGattCharacteristic chara =
                bluetoothGatt.getService(SensorCommunicator.BLSERVICE_GENERIC_ANGLE)
                        .getCharacteristic(SensorCommunicator.BLCHARACTERISTIC_A_ANGLE);
        bluetoothGatt.setCharacteristicNotification(chara, false);

        BluetoothGattDescriptor desc = chara.getDescriptor(SensorCommunicator.BLDESCRIPTOR_A_ANGLE);
        desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(desc);
    }

    private void turnOffNotifications(BluetoothGatt gatt, UUID service, UUID characteristic, UUID descriptor) {
        BluetoothGattCharacteristic chara =
                gatt.getService(service)
                        .getCharacteristic(characteristic);
        gatt.setCharacteristicNotification(chara, false);

        BluetoothGattDescriptor desc = chara.getDescriptor(descriptor);
        desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(desc);
    }

    /**
     * @param rate A Value between 0 and 16384 (0 is fast and 16484 is very slow)
     */
    public void writeSampleRate(int rate) {
        final byte[] data = ByteBuffer.allocate(2).putShort((short) rate).array();
        boolean successfull = writeAngleData(data);
        Log.i(TAG, "rate write : " + successfull);
    }

    /**
     *
     */
    public void calibrate() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(0).byteValue()).array();
        boolean successfull = writeAngleData(data);
        Log.i(TAG, "calibration write : " + successfull);
    }

    /**
     *
     */
    public void resetSensor() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(3).byteValue()).array();
        boolean successfull = writeAngleData(data);
        Log.i(TAG, "reset write : " + successfull);
    }


    /**
     *
     */
    public void softwareResetSensor() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(7).byteValue()).array();
        boolean successfull = writeAngleData(data);
        Log.i(TAG, "software reset write : " + successfull);
    }

    public boolean writeAngleData(byte[] data){
        if (bluetoothGatt != null) {
            BluetoothGattCharacteristic chara = bluetoothGatt.getService(BLSERVICE_GENERIC_ANGLE)
                    .getCharacteristic(BLCHARACTERISTIC_A_ANGLE);
            chara.setValue(data);
            return bluetoothGatt.writeCharacteristic(chara);
        } else {
            Log.e(TAG, "Device not yet connected");
            return false;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (BLCHARACTERISTIC_B_BATTERY.equals(characteristic.getUuid())) {
                //turnOnNotifications(gatt, BLSERVICE_GENERIC_ANGLE, BLCHARACTERISTIC_A_ANGLE, BLDESCRIPTOR_A_ANGLE);
                final Intent intent = new Intent(ACTION_BATTERY_DATA_AVAILABLE);
                final byte[] data = characteristic.getValue();
                // Log.i(TAG,"Received: "+ Arrays.toString(data));
                if (data.length > 0) {
                    intent.putExtra(EXTRA_BATTERY, ((data[0] & 0xFF)));
                }
                sendBroadcast(intent);
            } else{
                Log.i(TAG, "UNKNOWN Read Characteristic");
            }
        }
    }

    /**
     * Function gets called when a characteristic that has turned on notification changes
     *
     * @param gatt           the gatt service
     * @param characteristic the characteristic that changed
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(BLCHARACTERISTIC_A_ANGLE)) {
            byte[] data = characteristic.getValue();
            if (data.length > 7) {
                float a0 = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                float a1 = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                Intent intent = new Intent(ACTION_ANGLE_DATA_AVAILABLE);
                intent.putExtra(EXTRA_ANGLE_X, a0);
                intent.putExtra(EXTRA_ANGLE_Y, a1);
                sendBroadcast(intent);
            } else {
                Log.i(TAG, "Incoming data: " + Arrays.toString(data) + " with length: " + data.length);
            }
        } else {
            UUID uuid = characteristic.getUuid();
            System.out.println(uuid);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d("onCharacteristicWrite", "Failed write, retrying");
            gatt.writeCharacteristic(characteristic);
        }
    }

    // function to generate a SIG compatible UUID from a Number
    private static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | ((long) i << 32), LSB);
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }
}
