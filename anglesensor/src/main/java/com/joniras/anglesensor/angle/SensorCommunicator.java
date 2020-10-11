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
    public final static String EXTRA_BATTERY =
            "com.example.bluetooth.le.EXTRA_BATTERY";
    public final static String EXTRA_ANGLE_X =
            "com.example.bluetooth.le.EXTRA_ANGLE_X";
    public final static String EXTRA_ANGLE_Y =
            "com.example.bluetooth.le.EXTRA_ANGLE_Y";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_SENSOR_INFORMATION =
            "com.example.bluetooth.le.ACTION_SENSOR_INFORMATION";
    static final String EXTRA_SENSOR_INFORMATION =
            "com.example.bluetooth.le.EXTRA_SENSOR_INFORMATION";

    final static UUID BLSERVICE_GENERIC_ACCESS = convertFromInteger(0x1800);
    final static UUID BLCHARACTERISTIC_GA_DEVICE_NAME = convertFromInteger(0x2A00);
    final static UUID BLCHARACTERISTIC_GA_APPEARANCE = convertFromInteger(0x2A01);
    final static UUID BLCHARACTERISTIC_GA_CONN_PARAMS = convertFromInteger(0x2A04);


    final static UUID BLSERVICE_GENERIC_ATTRIBUTE_PROFILE = convertFromInteger(0x1801);
    final static UUID BLCHARACTERISTIC_GAP_ = convertFromInteger(0x2A05);


    final static UUID BLSERVICE_GENERIC_ANGLE = convertFromInteger(0x1820);
    final static UUID BLCHARACTERISTIC_A_ANGLE = convertFromInteger(0x2A70);
    final static UUID BLDESCRIPTOR_A_ANGLE = convertFromInteger(0x2902);

    // Battery characteristics do not contain real data, only 100% all the time
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
    private boolean initalAngle = true;

    public static SensorCommunicator getInstance() {
        return instance;
    }

    private SensorCommunicator() {

    }

    public void connect(BluetoothDevice device, Context context) {
        bluetoothGatt = device.connectGatt(context, false, this);
        this.context = context;
    }


    public void connect(BluetoothDevice device, Context context, boolean initialAngle) {
        this.connect(device,context);
        this.initalAngle = initialAngle;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Connected to GATT server.");
            Log.i(TAG, "Attempting to start service discovery:" +
                    bluetoothGatt.discoverServices());
            sendBroadcast(new Intent(ACTION_GATT_CONNECTED));

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
            sendBroadcast(new Intent(ACTION_GATT_SERVICES_DISCOVERED));
            if(initalAngle){
                turnOnNotifications();
            }
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
            Log.i(TAG, "Attempting to start service discovery:" +
                    bluetoothGatt.discoverServices());
        }
    }


    private void readChara(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic chara = bluetoothGatt.getService(service)
                .getCharacteristic(characteristic);
        boolean successfull = bluetoothGatt.readCharacteristic(chara);
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

    void readSensorInformation(){
        readChara(BLSERVICE_GENERIC_ACCESS, BLCHARACTERISTIC_GA_DEVICE_NAME);
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
            if (BLCHARACTERISTIC_GA_DEVICE_NAME.equals(characteristic.getUuid())) {
                        info.setDeviceName(characteristic.getStringValue(0));
                        readChara(BLSERVICE_GENERIC_DEVICE_INFORMATION,BLCHARACTERISTIC_GDI_MODEL_NUMBER);
            } else if(BLCHARACTERISTIC_GDI_MODEL_NUMBER.equals(characteristic.getUuid())){
                info.setModel(characteristic.getStringValue(0));
                readChara(BLSERVICE_GENERIC_DEVICE_INFORMATION,BLCHARACTERISTIC_GDI_MANUFACTURER);
            }else if(BLCHARACTERISTIC_GDI_MANUFACTURER.equals(characteristic.getUuid())){
                info.setManufacturer(characteristic.getStringValue(0));
                readChara(BLSERVICE_GENERIC_DEVICE_INFORMATION,BLCHARACTERISTIC_GDI_REVISION);
            }else if(BLCHARACTERISTIC_GDI_REVISION.equals(characteristic.getUuid())){
                info.setFirmwareRevision(characteristic.getStringValue(0));
                readChara(BLSERVICE_GENERIC_DEVICE_INFORMATION,BLCHARACTERISTIC_GDI_SOFTWARE);
            }else if(BLCHARACTERISTIC_GDI_SOFTWARE.equals(characteristic.getUuid())){
                info.setSoftwareRevision(characteristic.getStringValue(0));
                readChara(BLSERVICE_GENERIC_DEVICE_INFORMATION,BLCHARACTERISTIC_GDI_VERSION);
            }else if(BLCHARACTERISTIC_GDI_VERSION.equals(characteristic.getUuid())){
                info.setHardwareRevision(characteristic.getStringValue(0));
                Intent a = new Intent();
                a.setAction(ACTION_SENSOR_INFORMATION);
                a.putExtra(EXTRA_SENSOR_INFORMATION, info);
                sendBroadcast(a);
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
