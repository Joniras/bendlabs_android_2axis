package com.joniras.anglesensor.angle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.joniras.anglesensor.angle.interfaces.IAngleDataObserver;
import com.joniras.anglesensor.angle.interfaces.ISensorDataObserver;

import java.util.ArrayList;

import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_ANGLE_DATA_AVAILABLE;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_CONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_DISCONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_SERVICES_DISCOVERED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_SENSOR_INFORMATION;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_X;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_Y;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_SENSOR_INFORMATION;
import static com.joniras.anglesensor.angle.BluetoothService.ACTION_DISCOVERY_TIMEOUT;

public class AngleSensor {
    // wird für das Logging benötigt
    private static final String TAG = "AngleSensor";

    // Singleton-Instanz
    private static final AngleSensor instance = new AngleSensor();

    // Der Service für die Bluetooth-Schnittstelle
    private static BluetoothService service;

    // unterschiedliche Observer für unterschiedliche Daten (siehe Interfaces)
    private static ArrayList<ISensorDataObserver> angleSensorObservers = new ArrayList<>();
    private static ArrayList<IAngleDataObserver> angleObservers = new ArrayList<>();

    // gibt an, ob der Sensor aktuell verbunden ist
    private boolean connected = false;

    // Der Sensor der AAU mit der Inventarnummer "ISYS-2019-122"
    private String IDOFSensor = "FA:0E:BA:83:09:8A";

    /**
     * @param id Die Bluetooth-Mac-Adresse des Sensor, zu dem verbunden werden soll (standardmässig der Sensor der AAU)
     */
    public void setIDOfSensor(String id){
        IDOFSensor = id;
    }

    /**
     * Initializiert den Empfänger der Broadcasts und überprüft die Berechtigungen
     * @param context für den Service (bentötigt zum Starten des Services)
     */
    public void start(Activity context) {
        // Service starten und an den Context binden  (Service stirbt mit Activity)
        Intent intent = new Intent(context, BluetoothService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // Empfang der Ereignisse einrichten
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(ACTION_SENSOR_INFORMATION);
        filter.addAction(ACTION_DISCOVERY_TIMEOUT);
        filter.addAction(ACTION_ANGLE_DATA_AVAILABLE);
        context.registerReceiver(blEReceiver, filter);

        // Berechtigungen prüfen
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    /**
     * Startet die Suche nach dem Sensor und verbindet sich mit diesem falls erreichbar
     * @param initialAngle Bei true werden die Winkeldaten sofort abonniert, ansonsten erst mit turnOn()
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor bereits verbunden
     */
    public void discover(boolean initialAngle) throws IllegalStateException {
        if(validState(false)){
            service.discover(initialAngle);
        }
    }

    /**
     * Startet die Suche nach dem Sensor und verbindet sich mit diesem falls erreichbar
     * Winkeldaten werden sofort abonniert
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor bereits verbunden
     */
    public void discover() throws IllegalStateException{
        discover(true);
    }

    /**
     * @param rate Ein Wert zwischen 1 und 500 (1 ist schnell, 500 ist sehr langsam)
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void setRate(int rate) throws IllegalStateException {
        if(validState(true)){
            if(rate < 1 || rate > 500){
                throw new IllegalArgumentException("Rate must be between 1 and 500");
            }else{
                service.sendToThread("rate", "rate", rate);
            }
        }
    }

    /**
     * Kalibriert den Sensor auf den aktuellen Winkel in dem er gebogen ist
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void calibrate() throws IllegalStateException {

        if(validState(true)){
            service.sendToThread("calibrate");
        }
    }

    /**
     * Setzt die Kalibrierung des Sensor zurück auf den Originalzustand bei Auslieferung
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void resetSensor() throws IllegalStateException {
        if(validState(true)){
            service.sendToThread("reset");
        }
    }

    /**
     * Setzt die Software des Sensors zurück auf den Auslieferungszustand (bricht Verbdindung zum Sensor ab, da der Sensor neu startet)
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void resetSensorSoftware() throws IllegalStateException {
        if(validState(true)){
            service.sendToThread("softwarereset");
        }
    }

    /**
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void disconnect() throws IllegalStateException {
        // If service is null probably something with permissions is wrong
        if(validState(true)){
            service.sendToThread("disconnect");
        }
    }

    /**
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void turnOn() throws IllegalStateException {
        // If service is null probably something with permissions is wrong
        if(validState(true)){
            service.sendToThread("turnOn");
        }
    }

    /**
     * Wirft eine Ausnahme, wenn Service nicht verbunden ist oder
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor verbunden oder nicht (siehe Parameter needsConnected)
     */
    private boolean validState(boolean needsConnected) throws IllegalStateException{
        if(service != null){
            if(connected == needsConnected) {
                return true;
            }else{
                if (needsConnected) {
                    throw new IllegalStateException("Device not connected, try discover first");
                } else {
                    throw new IllegalStateException("Device already connected, try disconnect first");
                }
            }
        }else{
            throw new IllegalStateException("Service not ready");
        }
    }

    /**
     * Schaltet die Benachrichtigungen über neue Winkelwerte aus (am Sensor)
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void turnOff() throws IllegalStateException {
        if(validState(true)){
            service.sendToThread("turnOff");
        }
    }

    /**
     * Liest die Sensor-Informationen (Software, Name, Model, etc)
     * Informationen kommen verzögert mit onSensorInformation() zurück
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void readSensorInformation() throws IllegalStateException {
        if(validState(true)){
            service.sendToThread("readSensorInformation");
        }
    }

    /**
     * Mit dieser Funktion registrieren wenn erweiterte Informationen gefordert sind(Sensorinformationen, Statusupdates, etc)
     * @param observer ISensorDataObserver
     */
    public void registerObserver(ISensorDataObserver observer) {
        if(!angleSensorObservers.contains(observer)) {
            angleSensorObservers.add(observer);
        }
    }


    /**
     * Entfernt den Observer von der Liste
     * @param observer ISensorDataObserver
     */
    public void removeObserver(ISensorDataObserver observer) {
        angleSensorObservers.remove(observer);
    }

    /**
     * Mit dieser Funktion registrieren, wenn nur Winkeldaten gewünscht sind
     * @param observer IAngleDataObserver
     */
    public void registerObserver(IAngleDataObserver observer) {
        if(!angleObservers.contains(observer)) {
            angleObservers.add(observer);
        }
    }


    /**
     * Entfernt den Observer von der Liste
     * @param observer IAngleDataObserver
     */
    public void removeObserver(IAngleDataObserver observer) {
        angleObservers.remove(observer);
    }

    /**
     * Empfängt die Broadcasts vom BluetoothService, SensorCommunicator sowie BluetoothAdapter
     * Leitet die Informationen weiter an die jeweiligen Empfänger
     */
   private final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            notifyBluetoothStateChanged(false);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            notifyBluetoothStateChanged(true);
                            break;
                    }
                    break;
                case ACTION_GATT_CONNECTED:
                    connected = true;
                    notifyDeviceConnected();
                    break;
                case ACTION_GATT_DISCONNECTED:
                    connected = false;
                    notifyDeviceDisconnected();
                    break;
                case ACTION_SENSOR_INFORMATION:
                    notifySensorInformation((SensorInformation)intent.getSerializableExtra(EXTRA_SENSOR_INFORMATION));
                    break;
                case ACTION_DISCOVERY_TIMEOUT:
                    notifySensorNotFound();
                    break;
                case ACTION_ANGLE_DATA_AVAILABLE:
                    notifyAngleDataChanged(new AnglePair(intent.getFloatExtra(EXTRA_ANGLE_X, 0), intent.getFloatExtra(EXTRA_ANGLE_Y, 0)));
                    break;
            }
        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG,"Service connected");
            // Bind Service to Activity
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            AngleSensor.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG,"Service disconncted");
            AngleSensor.service = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG,"Service binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.d(TAG,"Service NULL binding");
        }

    };

    /**
     * Observer benachrichtigen, wenn der Sensor verbunden wurde
     */
    private void notifyDeviceConnected(){
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onDeviceConnected();
        }
    }

    /**
     * Observer benachrichtigen, wenn die Verbindung zum Sensor abgebrochen wurde
     */
    private void notifyDeviceDisconnected(){
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onDeviceDisconnected();
        }
    }

    /**
     * Observer benachrichtigen, dass sich der Bluetooth-Status des Handys verändert hat
     * @param isOn gibt an, ob Bluetooth an oder aus ist
     */
    private void notifyBluetoothStateChanged(boolean isOn){
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onBluetoothStateChanged(isOn);
        }
    }

    /**
     * Observer benachrichtigen, dass die Geräteinformationen angekommen sind
     * @param info Sensor information
     */
    private void notifySensorInformation(SensorInformation info){
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onSensorInformation(info);
        }
    }

    /**
     * Observer benachrichtigen, dass neue Winkel-Daten verfügbar sind
     * @param anglePair Daten des Sensors
     */
    void notifyAngleDataChanged(AnglePair anglePair){
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onAngleDataChanged(anglePair);
        }
        for (IAngleDataObserver observer: angleObservers) {
            observer.onAngleDataChanged(anglePair);
        }
    }

    /**
     * Observer benachrichtigen, dass der Sensor nicht gefunden werden konnte
     */
    private void notifySensorNotFound() {
        for (ISensorDataObserver observer: angleSensorObservers) {
            observer.onDeviceNotFound();
        }
    }

    public static AngleSensor getInstance() {
        return instance;
    }
    public String getIDOFSensor() {
        return IDOFSensor;
    }

}
