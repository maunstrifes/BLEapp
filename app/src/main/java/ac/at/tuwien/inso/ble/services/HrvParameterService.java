package ac.at.tuwien.inso.ble.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.utils.BaseCalculator;
import ac.at.tuwien.inso.ble.utils.Events;
import ac.at.tuwien.inso.ble.utils.LimitedList;

public class HrvParameterService extends Service {

    private static final int WINDOW_SIZE = 256;
    private static final int SAMPLES_TO_CALC = 64; //TODO: 64

    private final static String TAG = HrvParameterService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private LimitedList<Double> list = new LimitedList<Double>(WINDOW_SIZE);

    /**
     * Receiver for HR data
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Events action = Events.valueOf(intent.getAction());
            if (Events.ACTION_DATA_AVAILABLE.equals(action)) {
                writeData(intent
                        .getStringExtra(Events.HR_DATA.toString()));
            }
        }
    };

    /**
     * Register for HR data broadcasts
     */
    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "started HrvParameterService");
    }

    /**
     * Unregister for HR data broadcasts
     */
    @Override
    public void onDestroy() {

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Adds received HR to the list and calculates parameter every [SAMPLES_TO_CALC] samples received.
     * When parameters were calculated they are broadcasted.
     *
     * @param heartRate
     */
    private void writeData(String heartRate) {
        list.add(Double.valueOf(heartRate));
        if (list.getItemsAdded() % SAMPLES_TO_CALC == 0) {
            List<Double> calcList = new ArrayList<Double>();
            calcList.addAll(list);
            new CalculationTask().execute(calcList);
        }
    }

    /**
     * Calculate parameters asynchronous to GUI task
     */
    private class CalculationTask extends AsyncTask<List<Double>, Void, HrvParameters> {

        @Override
        protected HrvParameters doInBackground(List<Double>... lists) {
            if (lists.length > 1) throw new RuntimeException("too many lists, why?");
            BaseCalculator calc = new BaseCalculator(lists[0]);
            return new HrvParameters(calc.getMeanHr(), calc.getSdnn(), calc.getRmssd(), calc.getPnn50());
        }

        protected void onPostExecute(HrvParameters result) {
            final Intent intent = new Intent(Events.ACTION_HRV_DATA_AVAILABLE.toString());
            intent.putExtra(Events.HRV_DATA.toString(), result);
            sendBroadcast(intent);
        }
    }

    public class LocalBinder extends Binder {
        public HrvParameterService getService() {
            return HrvParameterService.this;
        }
    }
}
