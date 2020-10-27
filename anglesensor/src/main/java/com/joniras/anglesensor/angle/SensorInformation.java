package com.joniras.anglesensor.angle;

import java.io.Serializable;

/**
 * Klasse zum erleichterten Transport und Zugriff auf Sensor-Informationen (die am Sensor selbst gespeichert sind)
 */
public class SensorInformation implements Serializable {
    private String manufacturer;
    private String model;
    private String hardwareRevision;
    private String firmwareRevision;
    private String softwareRevision;
    private String deviceName;

    public String getManufacturer() {
        return manufacturer;
    }

    void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    void setModel(String model) {
        this.model = model;
    }

    public String getHardwareRevision() {
        return hardwareRevision;
    }

    void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    void setFirmwareRevision(String firmwareRevision) {
        this.firmwareRevision = firmwareRevision;
    }

    public String getSoftwareRevision() {
        return softwareRevision;
    }

    void setSoftwareRevision(String softwareRevision) {
        this.softwareRevision = softwareRevision;
    }

    public String getDeviceName() {
        return deviceName;
    }

    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public SensorInformation() {
    }

    @Override
    public String toString() {
        return "SensorInformation{" +
                "manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", hardwareRevision='" + hardwareRevision + '\'' +
                ", firmwareRevision='" + firmwareRevision + '\'' +
                ", softwareRevision='" + softwareRevision + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
