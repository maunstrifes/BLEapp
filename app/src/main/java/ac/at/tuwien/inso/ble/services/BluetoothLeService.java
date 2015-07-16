/*
 * Copyright (C) 2013 The Android Open Source Project
 * This software is based on Apache-licensed code from the above.
 * 
 * Copyright (C) 2013 APUS
 *
 *     This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ac.at.tuwien.inso.ble.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import ac.at.tuwien.inso.ble.activities.DeviceControlActivity;
import ac.at.tuwien.inso.ble.utils.BleAction;
import pro.apus.heartrate.R;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public static UUID UUID_HEART_RATE_MEASUREMENT = UUID
            .fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final BluetoothGattCallback gattCallback = new MyBluetoothGattCallback();
    private final IBinder binder = new LocalBinder();

    private RecordService recordService;
    private final ServiceConnection recordServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            recordService = ((RecordService.LocalBinder) service)
                    .getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            recordService = null;
        }
    };
    private HrvParameterService hrvParameterService;
    private final ServiceConnection hrvParameterServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            hrvParameterService = ((HrvParameterService.LocalBinder) service)
                    .getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            hrvParameterService = null;
        }
    };
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int notificationId = 1;
    private boolean started;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // do nothing if service already running
        if (started) {
            return Service.START_NOT_STICKY;
        }
        started = true;

        startBluetooth();
        String deviceAddress = intent.getStringExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS);
        connect(deviceAddress);

        showNotification();

        Intent recordServiceIntent = new Intent(this, RecordService.class);
        bindService(recordServiceIntent, recordServiceConnection, BIND_AUTO_CREATE);
        Intent hrvParameterServiceIntent = new Intent(this, HrvParameterService.class);
        bindService(hrvParameterServiceIntent, hrvParameterServiceConnection, BIND_AUTO_CREATE);
        return Service.START_STICKY;
    }

    /**
     * Starts Bluetooth
     */
    private void startBluetooth() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            throw new RuntimeException("Unable to initialize BluetoothManager.");
        }
        bluetoothAdapter = mBluetoothManager.getAdapter();
    }

    /**
     * Chooses the right GATT characteristic for retreiving the heart rate
     */
    private void connectToHeartRateService() {
        List<BluetoothGattService> services = bluetoothGatt.getServices();
        if (services == null) {
            return;
        }
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : services) {
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattService
                    .getCharacteristics()) {

                if (UUID_HEART_RATE_MEASUREMENT
                        .equals(gattCharacteristic.getUuid())) {
                    Log.d(TAG, "Found heart rate");
                    setCharacteristicNotification(gattCharacteristic, true);
                    return;
                }
            }
        }
        //TODO: Was tun, wenn GattService nicht verfügbar? (Testen ob man da reinkommt, wenn man zu random Bluetooth Gerät verbinden will, zB Handy)
        Log.w(TAG, "No heart rate characteristic found!");
    }

    /**
     * Creates and shows the notification for the app to signal that the service is running.
     */
    private void showNotification() {
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("BLE Logger")
                        .setContentText("testtext");
        builder.setOngoing(true);
        Intent resultIntent = new Intent(getApplicationContext(), DeviceControlActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DeviceControlActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        startForeground(notificationId, builder.build());
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(BleAction.EXTRA_DATA.toString(), String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(
                        data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(BleAction.EXTRA_DATA.toString(), new String(data) + "\n"
                        + stringBuilder.toString());
            }
        }

        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(String address) {

        // Previously connected device. Try to reconnect.
        if (bluetoothDeviceAddress != null
                && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG,
                    "Trying to use an existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        // New device
        final BluetoothDevice device = bluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * Stops the service and releases the bluetooth connections
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        stopForeground(true);
        unbindService(recordServiceConnection);
        bluetoothGatt.close();
        bluetoothGatt = null;
        stopSelf();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {

        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // Specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG);
            descriptor
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);

        }
    }


    /**
     * Callback for GATT events
     */
    private class MyBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            BleAction intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = BleAction.ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction.toString());
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:"
                        + bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = BleAction.ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction.toString());
                close(); //TODO: oder doch nur wenn ausdrücklich gewünscht und nicht passiert? (HR Brustgurt Verhalten??)
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectToHeartRateService();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(BleAction.ACTION_DATA_AVAILABLE.toString(), characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(BleAction.ACTION_DATA_AVAILABLE.toString(), characteristic);
        }
    }

    ;

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}
