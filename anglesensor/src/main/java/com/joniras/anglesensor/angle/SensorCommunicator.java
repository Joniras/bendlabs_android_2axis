package com.joniras.anglesensor.angle;

import android.annotation.SuppressLint;
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
 * Zuständig für die Kommunikation mit dem Sensor
 * Beinhaltet alle wichtigen UUID's für die Kommunikation
 */
public class SensorCommunicator extends BluetoothGattCallback {
    private final String TAG = SensorCommunicator.class.getSimpleName();
    private BluetoothGatt bluetoothGatt;
    private Context context;

    public final static String ACTION_GATT_CONNECTED =
            "aau.sensor_evaluation.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "aau.sensor_evaluation.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_ANGLE_DATA_AVAILABLE =
            "aau.sensor_evaluation.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_ANGLE_X =
            "aau.sensor_evaluation.EXTRA_ANGLE_X";
    public final static String EXTRA_ANGLE_Y =
            "aau.sensor_evaluation.EXTRA_ANGLE_Y";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "aau.sensor_evaluation.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_SENSOR_INFORMATION =
            "aau.sensor_evaluation.ACTION_SENSOR_INFORMATION";
    static final String EXTRA_SENSOR_INFORMATION =
            "aau.sensor_evaluation.EXTRA_SENSOR_INFORMATION";

    final static UUID BLSERVICE_GENERIC_ACCESS = convertFromInteger(0x1800);
    final static UUID BLCHARACTERISTIC_GA_DEVICE_NAME = convertFromInteger(0x2A00);
    final static UUID BLCHARACTERISTIC_GA_APPEARANCE = convertFromInteger(0x2A01);
    final static UUID BLCHARACTERISTIC_GA_CONN_PARAMS = convertFromInteger(0x2A04);


    final static UUID BLSERVICE_GENERIC_ATTRIBUTE_PROFILE = convertFromInteger(0x1801);
    final static UUID BLCHARACTERISTIC_GAP_ = convertFromInteger(0x2A05);


    final static UUID BLSERVICE_GENERIC_ANGLE = convertFromInteger(0x1820);
    final static UUID BLCHARACTERISTIC_A_ANGLE = convertFromInteger(0x2A70);
    final static UUID BLDESCRIPTOR_A_ANGLE = convertFromInteger(0x2902);

    // Die Batterie-Charakteristik beinhaltet keine sinnvollen Daten
    // Diese Charakteristik sendet laut Rücksprache mit Bendlabs immer 100%
    final static UUID BLSERVICE_GENERIC_BATTERY = convertFromInteger(0x180F);
    final static UUID BLCHARACTERISTIC_B_BATTERY = convertFromInteger(0x2A19);
    final static UUID BLDESCRIPTOR_B_BATTERY = convertFromInteger(0x2902);

    final static UUID BLSERVICE_GENERIC_DEVICE_INFORMATION = convertFromInteger(0x180A);
    final static UUID BLCHARACTERISTIC_GDI_REVISION = convertFromInteger(0x2A27);
    final static UUID BLCHARACTERISTIC_GDI_VERSION = convertFromInteger(0x2A26);
    final static UUID BLCHARACTERISTIC_GDI_SOFTWARE = convertFromInteger(0x2A28);
    final static UUID BLCHARACTERISTIC_GDI_MANUFACTURER = convertFromInteger(0x2A29);
    final static UUID BLCHARACTERISTIC_GDI_MODEL_NUMBER = convertFromInteger(0x2A24);

    @SuppressLint("StaticFieldLeak")
    private static SensorCommunicator instance = new SensorCommunicator();

    private static SensorInformation info = new SensorInformation();

    // Gibt an, ob die Winkeldaten direkt nach entstandener Verbdindung abonniert werden sollen
    private boolean initalAngle = true;

    public static SensorCommunicator getInstance() {
        return instance;
    }

    private SensorCommunicator() {}

    /**
     * Erstellen der Verbindung zum Gerät
     * @param device das Gerät, mit dem verbunden werden soll
     * @param context wird von BluetoothGatt zum verbinden benötigt
     */
    public void connect(BluetoothDevice device, Context context) {
        bluetoothGatt = device.connectGatt(context, false, this);
        this.context = context;
    }

    /**
     *
     * @param device das Gerät, mit dem verbunden werden soll
     * @param context wird von BluetoothGatt zum verbinden benötigt
     * @param initialAngle gibt an, ob Winkeldaten direkt nach entstandener Verbindung abonniert werden sollen (ansonsten nach erfolgreicher Verbindung mit turnOn())
     */
    public void connect(BluetoothDevice device, Context context, boolean initialAngle) {
        this.connect(device,context);
        this.initalAngle = initialAngle;
    }

    /**
     * Funktion wird aufgerufen bei Änderungen der Verbindung
     * entsprechend wird AngleSensor über einen Broadcast informiert
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "Connected to GATT server.");
            Log.d(TAG, "Success to start service discovery:" +
                    bluetoothGatt.discoverServices());
            sendBroadcast(new Intent(ACTION_GATT_CONNECTED));
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Disconnected from GATT server.");
            sendBroadcast(new Intent(ACTION_GATT_DISCONNECTED));
        }
    }

    /**
     * Helper-Funktion zum senden von Broadcasts
     * @param intent der Intent, der versendet werden soll
     */
    private void sendBroadcast(Intent intent) {
        context.sendBroadcast(intent);
    }

    // Callback-Funktion bei Finden von Services beim Sensor
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // sobald die Services gefunden wurden, können Operationen (READ/WRITE/NOTIFY) ausgeführt werden
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered success");
            sendBroadcast(new Intent(ACTION_GATT_SERVICES_DISCOVERED));
            if(initalAngle){
                turnOnNotifications();
            }
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
            Log.d(TAG, "Attempting to start service discovery:" +
                    bluetoothGatt.discoverServices());
        }
    }

    /**
     * Helper-Funktion zum lesen von Charakteristiken
     * @param service die UUID des Services
     * @param characteristic die UUID der Charakteristik
     */
    private void readChara(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic chara = bluetoothGatt.getService(service)
                .getCharacteristic(characteristic);
        Log.d(TAG, "Read characteristic successfull: " + bluetoothGatt.readCharacteristic(chara));
    }


    /**
     * Initiiert das Lesen der Sensor-Information
     * Sobald die erste Information ankommt, wird die nächste gefragt, usw
     */
    void readSensorInformation(){
        readChara(BLSERVICE_GENERIC_ACCESS, BLCHARACTERISTIC_GA_DEVICE_NAME);
    }

    /**
     * Schaltet die Benachrichtigungen ein für die Winkel-Charakteristik
     */
    void turnOnNotifications() {
        changeNotifications(true);
    }

    /**
     * Schaltet die Benachrichtigungen aus für die Winkel-Charakteristik
     */
    void turnOffNotifications() {
        changeNotifications(false);
    }

    /**
     * Helper-Funktion zum ein/ausschalten der Benachrichtigungen
     * @param turnOn steuert, ob Benachrichtigungen ein oder ausgeschaltet werden
     */
    void changeNotifications(boolean turnOn){
        BluetoothGattCharacteristic chara =
                bluetoothGatt.getService(SensorCommunicator.BLSERVICE_GENERIC_ANGLE)
                        .getCharacteristic(SensorCommunicator.BLCHARACTERISTIC_A_ANGLE);
        bluetoothGatt.setCharacteristicNotification(chara, turnOn);

        BluetoothGattDescriptor desc = chara.getDescriptor(SensorCommunicator.BLDESCRIPTOR_A_ANGLE);
        if(turnOn){
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }else{
            desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        Log.v(TAG,"Turn Notifications "+(turnOn?"on":"off")+":"+bluetoothGatt.writeDescriptor(desc));
    }

    /**
     * Die Sample-Rate steuert die Schnelligkeit, in der der Sensor neue Winkel-Daten schickt
     * @param rate Ein Wert zwischen 1 und 500 (1 is schnell, 500 ist sehr langsam)
     */
    public void writeSampleRate(int rate) {
        final byte[] data = ByteBuffer.allocate(2).putShort((short) rate).array();
        boolean successfull = writeAngleData(data);
        Log.d(TAG, "rate write : " + successfull);
    }

    /**
     * Kalibriert den Sensor auf den jetzigen Biege-Zustand
     */
    public void calibrate() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(0).byteValue()).array();
        Log.d(TAG, "calibration write : " + writeAngleData(data));
    }

    /**
     * Setzt die Kalibrierung des Sensor zurück auf den Originalzustand bei Auslieferung
     */
    public void resetSensor() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(3).byteValue()).array();
        Log.d(TAG, "reset write : " + writeAngleData(data));
    }


    /**
     * Setzt die Software des Sensors zurück auf den Auslieferungszustand (bricht Verbdindung zum Sensor ab, da der Sensor neu startet)
     */
    public void softwareResetSensor() {
        final byte[] data = ByteBuffer.allocate(1).put(Integer.valueOf(7).byteValue()).array();
        Log.d(TAG, "software reset write : " + writeAngleData(data));
    }

    /**
     * Helper-Funktion zum Schreiben der Daten auf die Winkel-Charakteristik
     * Werte zum Schreiben wurden dem Datenblatt entnommen
     * @param data Daten, die auf den Sensor geschrieben werden sollen
     * @return gibt zurück, ob erfolgreich
     */
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

    /**
     * Callback-Funktion die aufgerufen wird, wenn eine angeforderte Charakteristik als Wert zurück kommt
     */
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
     * Callback-Funktion die aufgerufen wird, wenn sich eine Charakteristik, bei der die Benachrichtigung aktiviert wurde, ändert
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(BLCHARACTERISTIC_A_ANGLE)) {
            byte[] data = characteristic.getValue();
            // Nach dem Beschreiben der Winkel-Charakteristik (calibratem resetm etc) kommt ein ungültiger Winkel-Wert mit Länge 4
            if (data.length > 7) {
                // laut Datenblatt sind die ersten 4 Byte der X-Wert und die folgenden 4 Byte der Y-Wert
                float angle_x = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                float angle_y = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                Intent intent = new Intent(ACTION_ANGLE_DATA_AVAILABLE);
                intent.putExtra(EXTRA_ANGLE_X, angle_x);
                intent.putExtra(EXTRA_ANGLE_Y, angle_y);
                sendBroadcast(intent);
            }
        } else {
            UUID uuid = characteristic.getUuid();
            System.out.println(uuid);
        }
    }

    /**
     * Callback-Funktion nach dem Schreiben einer Charakteristik (reset, calibrate, etc)
     * Versucht, die gleiche Operation nochmal auszuführen
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Failed write, retrying");
            gatt.writeCharacteristic(characteristic);
        }
    }

    // function to generate a SIG compatible UUID from a Number

    /**
     * Generiert SIG-kompatible UUID's anhand einer Nummer
     */
    private static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | ((long) i << 32), LSB);
    }

    /**
     * Trennt die Verbindung zum Sensor falls verbdunen
     */
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }
}
