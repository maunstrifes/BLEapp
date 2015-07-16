package ac.at.tuwien.inso.ble.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import ac.at.tuwien.inso.ble.utils.BaseCalculator;
import ac.at.tuwien.inso.ble.utils.BleAction;
import ac.at.tuwien.inso.ble.utils.LimitedList;

public class HrvParameterService extends Service {

    private static final int WINDOW_SIZE = 256;
    private static final int SAMPLES_TO_CALC = 64;

    private final static String TAG = HrvParameterService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private LimitedList<Double> list = new LimitedList<Double>(WINDOW_SIZE);
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

    public HrvParameterService() {
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleAction.ACTION_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "started, session created");
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void writeData(String heartRate) {
        list.add(Double.valueOf(heartRate));
        if (list.getItemsAdded() % SAMPLES_TO_CALC == 0) {
            BaseCalculator calc = new BaseCalculator(list);
            //TODO: call all calculators
        }
    }

    public class LocalBinder extends Binder {
        HrvParameterService getService() {
            return HrvParameterService.this;
        }
    }
}
