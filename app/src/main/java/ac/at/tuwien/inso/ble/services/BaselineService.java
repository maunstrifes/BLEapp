package ac.at.tuwien.inso.ble.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.utils.Baseline;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

public class BaselineService extends Service {

    private final static String TAG = BaselineService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private List<HrvParameters> list = new ArrayList<HrvParameters>();

    /**
     * Receiver for HR data
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final IntentConstants action = IntentConstants.valueOf(intent.getAction());
            if (IntentConstants.ACTION_HRV_DATA_AVAILABLE.equals(action)) {
                list.add((HrvParameters) intent
                        .getSerializableExtra(IntentConstants.HRV_DATA.toString()));
            }
        }
    };

    /**
     * Register for HR data broadcasts
     */
    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentConstants.ACTION_HRV_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "started BaselineService");
    }

    /**
     * Calculates the mean of the baseline parameters received during the baseline session.
     */
    public void saveBaseline() {
        double meanHr = 0;
        double sdnn = 0;
        double rmssd = 0;
        double pnn50 = 0;
        for (HrvParameters param : list) {
            meanHr += param.getMeanHr();
            sdnn += param.getSdnn();
            rmssd += param.getRmssd();
            pnn50 += param.getPnn50();
        }
        meanHr = meanHr / list.size();
        sdnn = sdnn / list.size();
        rmssd = rmssd / list.size();
        pnn50 = pnn50 / list.size();

        Log.i(TAG, "--- BASELINE ---");
        Log.i(TAG, "MeanHR: " + meanHr);
        Log.i(TAG, "SDNN: " + sdnn);
        Log.i(TAG, "RMSSD: " + rmssd);
        Log.i(TAG, "pNN50: " + pnn50);

        Baseline.getInstance().setBaseline(new HrvParameters(meanHr, sdnn, rmssd, pnn50));
        Baseline.getInstance().saveBaseline();
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

    public class LocalBinder extends Binder {
        public BaselineService getService() {
            return BaselineService.this;
        }
    }
}
