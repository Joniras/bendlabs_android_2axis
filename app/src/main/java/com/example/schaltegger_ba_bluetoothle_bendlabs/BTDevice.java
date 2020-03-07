package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.bluetooth.BluetoothDevice;

class BTDevice {
    private String name;
    private String address;
    private int type;

    BTDevice(String name, String address, int type) {
        this.name = name;
        this.address = address;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return name+"\n"+address+"\t ("+ intToType(type)+")";
    }

    private static String intToType(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "Classic";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "Dual";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "Le";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                return "N/A";
        }
        return null;
    }
}
