package ac.at.tuwien.inso.ble.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import ac.at.tuwien.inso.ble.database.Session;
import ac.at.tuwien.inso.ble.database.SessionDataSource;
import ac.at.tuwien.inso.ble.utils.BleAction;

public class RecordService extends Service {

    private final static String TAG = RecordService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BleAction action = BleAction.valueOf(intent.getAction());
            if (BleAction.ACTION_DATA_AVAILABLE.equals(action)) {
                writeData(intent
                        .getStringExtra(BleAction.EXTRA_DATA.toString()));
            }
        }
    };
    private SessionDataSource datasource;
    private BufferedWriter writer;

    public RecordService() {
    }

    @Override
    public void onCreate() {

        datasource = new SessionDataSource(this);
        datasource.open();
        Session session = datasource.createSession(new Date().getTime());
        createFile(session);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleAction.ACTION_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "started, session created");
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(broadcastReceiver);
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.w(TAG, "Problems closing writer: ", e);
        }
        datasource.close();
    }

    private void createFile(Session session) {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/" + session.getId() + ".csv");
        try {
            if (!file.exists()) {

                file.createNewFile();

            }
            writer = new BufferedWriter(new FileWriter(file,
                    true));
        } catch (IOException e) {
            Log.e(TAG, "Error while creating file. ", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void writeData(String heartRate) {

        try {
            writer.append((new Date()).toString() + "," + heartRate);
            writer.newLine();

        } catch (IOException e) {
            Log.e(TAG, "Error writing file:", e);
        }
    }

    public class LocalBinder extends Binder {
        RecordService getService() {
            return RecordService.this;
        }
    }
}
