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

import com.joniras.anglesensor.angle.interfaces.IAngleDataReceiver;
import com.joniras.anglesensor.angle.interfaces.IAngleDataObserver;
import com.joniras.anglesensor.angle.interfaces.ISensorDataObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_ANGLE_DATA_AVAILABLE;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_CONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_DISCONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_SERVICES_DISCOVERED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_SENSOR_INFORMATION;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_X;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_Y;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_SENSOR_INFORMATION;
import static com.joniras.anglesensor.angle.BluetoothService.ACTION_DISCOVERY_TIMEOUT;

/**
 * Schnittstelle für die Kommunikation mit einem 2-Achsen Biege-Sensor von Bendlabs
 */
public class AngleSensor {
    // wird für das Logging benötigt
    private static final String TAG = "AngleSensor";

    // Singleton-Instanz
    private static final AngleSensor instance = new AngleSensor();

    // Der Service für die Bluetooth-Schnittstelle
    private static BluetoothService service;

    public static final int permissionRequestCode = new Random().nextInt();

    private AngleSensor() {

    }

    // unterschiedliche Observer für unterschiedliche Daten (siehe Interfaces)
    private ArrayList<ISensorDataObserver> angleSensorObservers = new ArrayList<>();
    private ArrayList<IAngleDataObserver> angleObservers = new ArrayList<>();
    private HashMap<Long, IAngleDataReceiver> angleReceiver = new HashMap<>();

    // gibt an, ob der Sensor aktuell verbunden ist
    private boolean connected = false;

    //zur Überprüfung, ob die Funktion "start" aufgerufen wurde
    private boolean started = false;

    // Der Sensor der AAU mit der Inventarnummer "ISYS-2019-122"
    private String IDOFSensor = "FA:0E:BA:83:09:8A";

    /**
     * @param id Die Bluetooth-Mac-Adresse des Sensor, zu dem verbunden werden soll (standardmässig der Sensor der AAU)
     */
    public void setIDOfSensor(String id) {
        IDOFSensor = id;
    }


    /**
     * Initialisiert den Empfänger der Broadcasts und überprüft die Berechtigungen
     *
     * @param context für den Service (bentötigt zum Starten des Services)
     */
    public void start(Activity context) {
        // Service starten und an den Context binden  (Service stirbt mit Activity)
        Intent intent = new Intent(context, BluetoothService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // falls nicht bereits geschehen
        if(!started){
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
        }
        started = true;

        // Berechtigungen prüfen
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, permissionRequestCode);
    }

    /**
     * Startet die Suche nach dem Sensor und verbindet sich mit diesem falls erreichbar
     *
     * @param initialAngle Bei true werden die Winkeldaten sofort abonniert, ansonsten erst mit turnOn()
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor bereits verbunden
     */
    public void discover(boolean initialAngle) throws IllegalStateException {
        if (validState(false)) {
            service.discover(initialAngle);
        }
    }

    /**
     * Startet die Suche nach dem Sensor und verbindet sich mit diesem falls erreichbar
     * Winkeldaten werden sofort abonniert
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor bereits verbunden
     */
    public void discover() throws IllegalStateException {
        discover(true);
    }

    /**
     * @param rate Ein Wert zwischen 1 und 500 (1 ist schnell, 500 ist sehr langsam)
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void setRate(int rate) throws IllegalStateException {
        if (validState(true)) {
            if (rate < 1 || rate > 500) {
                throw new IllegalArgumentException("Rate must be between 1 and 500");
            } else {
                service.setRate(rate);
            }
        }
    }

    /**
     * Kalibriert den Sensor auf den aktuellen Winkel in dem er gebogen ist
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void calibrate() throws IllegalStateException {

        if (validState(true)) {
            service.calibrate();
        }
    }

    /**
     * Setzt die Kalibrierung des Sensor zurück auf den Originalzustand bei Auslieferung
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void resetCalibration() throws IllegalStateException {
        if (validState(true)) {
            service.resetCalibration();
        }
    }

    /**
     * Setzt die Software des Sensors zurück auf den Auslieferungszustand (bricht Verbdindung zum Sensor ab, da der Sensor neu startet)
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void resetSensorSoftware() throws IllegalStateException {
        if (validState(true)) {
            service.resetSoftware();
        }
    }

    /**
     * Trennt die Verbindung zum Sensor
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void disconnect() throws IllegalStateException {
        // If service is null probably something with permissions is wrong
        if (validState(true)) {
            service.disconnect();
        }
    }

    /**
     * Schaltet die Benachrichtigungen über neue Winkelwerte ein
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void turnOn() throws IllegalStateException {
        // If service is null probably something with permissions is wrong
        if (validState(true)) {
            service.turnOn();
        }
    }

    /**
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor verbunden oder nicht (siehe Parameter needsConnected)
     */
    private boolean validState(boolean needsConnected) throws IllegalStateException {
        if (started) {
            if (service != null) {
                if (connected == needsConnected) {
                    return true;
                } else {
                    if (needsConnected) {
                        throw new IllegalStateException("Device not connected, try discover first");
                    } else {
                        throw new IllegalStateException("Device already connected, try disconnect first");
                    }
                }
            } else {
                throw new IllegalStateException("Service not ready");
            }
        } else {
            throw new IllegalStateException("Please call function 'start' first, to provide a context for the service");
        }

    }

    /**
     * Schaltet die Benachrichtigungen über neue Winkelwerte aus (am Sensor)
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void turnOff() throws IllegalStateException {
        if (validState(true)) {
            service.turnOff();
        }
    }

    /**
     * Liest die Sensor-Informationen (Software, Name, Model, etc)
     * Informationen kommen verzögert mit onSensorInformation() zurück
     *
     * @throws IllegalStateException Wenn Service nicht bereit (vermutlich ein Problem mit Berechtigungen) oder Sensor noch nicht verbunden
     */
    public void readSensorInformation() throws IllegalStateException {
        if (validState(true)) {
            service.readSensorInformation();
        }
    }

    /**
     * Mit dieser Funktion registrieren wenn erweiterte Informationen gefordert sind(Sensorinformationen, Statusupdates, etc)
     *
     * @param observer ISensorDataObserver
     */
    public void registerObserver(ISensorDataObserver observer) {
        if (!angleSensorObservers.contains(observer)) {
            angleSensorObservers.add(observer);
        }
    }


    /**
     * Entfernt den Observer von der Liste
     *
     * @param observer ISensorDataObserver
     */
    public void removeObserver(ISensorDataObserver observer) {
        angleSensorObservers.remove(observer);
    }

    /**
     * Mit dieser Funktion registrieren, wenn nur Winkeldaten gewünscht sind
     *
     * @param observer IAngleDataObserver
     */
    public void registerObserver(IAngleDataObserver observer) {
        if (!angleObservers.contains(observer)) {
            angleObservers.add(observer);
        }
    }


    /**
     * Entfernt den Observer von der Liste
     *
     * @param observer IAngleDataObserver
     */
    public void removeObserver(IAngleDataObserver observer) {
        angleObservers.remove(observer);
    }

    /**
     * @return Singleton Instanz
     */
    public static AngleSensor getInstance() {
        return instance;
    }

    /**
     * @return Bluetooth-Adresse des Sensors, zu dem automatisch verbunden wird, wenn er gefunden wird
     */
    public String getIDOFSensor() {
        return IDOFSensor;
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
                    notifySensorInformation((SensorInformation) intent.getSerializableExtra(EXTRA_SENSOR_INFORMATION));
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

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "Service connected");
            // Bind Service to Activity
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            AngleSensor.service = binder.getService();
            AngleSensor.getInstance().notifyServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "Service disconncted");
            AngleSensor.service = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "Service binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.d(TAG, "Service NULL binding");
        }

    };

    private void notifyServiceReady() {
        for (Map.Entry<Long, IAngleDataReceiver> entry : angleReceiver.entrySet()) {
            service.registerReceiver(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Observer benachrichtigen, wenn der Sensor verbunden wurde
     */
    private void notifyDeviceConnected() {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onDeviceConnected();
        }
    }

    /**
     * Observer benachrichtigen, wenn die Verbindung zum Sensor abgebrochen wurde
     */
    private void notifyDeviceDisconnected() {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onDeviceDisconnected();
        }
    }

    /**
     * Observer benachrichtigen, dass sich der Bluetooth-Status des Handys verändert hat
     *
     * @param isOn gibt an, ob Bluetooth an oder aus ist
     */
    private void notifyBluetoothStateChanged(boolean isOn) {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onBluetoothStateChanged(isOn);
        }
    }

    /**
     * Observer benachrichtigen, dass die Geräteinformationen angekommen sind
     *
     * @param info Sensor information
     */
    private void notifySensorInformation(SensorInformation info) {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onSensorInformation(info);
        }
    }

    /**
     * Observer benachrichtigen, dass der Sensor nicht gefunden werden konnte
     */
    private void notifySensorNotFound() {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onDeviceNotFound();
        }
    }

    /**
     * Observer benachrichtigen, dass neue Winkel-Daten verfügbar sind
     *
     * @param anglePair Daten des Sensors
     */
    private void notifyAngleDataChanged(AnglePair anglePair) {
        for (ISensorDataObserver observer : angleSensorObservers) {
            observer.onAngleDataChanged(anglePair);
        }
        for (IAngleDataObserver observer : angleObservers) {
            observer.onAngleDataChanged(anglePair);
        }
    }

    /**
     * Sobald ein Gerät verbunden wurde, kann über diese Funktion ein Update angefordert werden im Abstand update_every
     * Das update nach den angegebenen Millisekunden kann nicht garantiert werden wenn die SampleRate zu hoch gesetzt wurde
     *
     * @param update_every  Der Abstand in Milliskeunden, nach denen der angleReceiver über neue Winkeldaten benachrichtigt wird
     * @param angleReceiver bekommt Beanchrichtungen über Winkelwerte
     */
    public void registerReceiver(long update_every, IAngleDataReceiver angleReceiver) {
        if (!this.angleReceiver.containsValue(angleReceiver)) {
            this.angleReceiver.put(update_every, angleReceiver);
        }
        if (service != null) {
            service.registerReceiver(update_every, angleReceiver);
        } else {
            throw new IllegalStateException("Service not ready, wait for onDeviceConnected callback");
        }
    }

    /**
     * Löschen des Empfängers um Fehler zu vermeiden (in onDestroy einbauen)
     *
     * @param angleReceiver Objekt, das registriert wurde
     */
    public void unregisterReceiver(IAngleDataReceiver angleReceiver) {
        if (service != null) {
            service.unregisterReceiver(angleReceiver);
        } else {
            throw new IllegalStateException("Service not ready, wait for onDeviceConnected callback");
        }
    }


}

