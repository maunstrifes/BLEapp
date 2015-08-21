package ac.at.tuwien.inso.ble.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.utils.Events;
import ac.at.tuwien.inso.ble.utils.LimitedList;

public class StressAssessmentService extends Service {

    private static final int PARAM_HISTORY_SIZE = 100;
    private final static String TAG = StressAssessmentService.class.getSimpleName();
    private final IBinder binder = new LocalBinder();
    private List<Double> meanHrList = new LimitedList<Double>(PARAM_HISTORY_SIZE);
    private List<Double> sdnnList = new LimitedList<Double>(PARAM_HISTORY_SIZE);
    private List<Double> rmssdList = new LimitedList<Double>(PARAM_HISTORY_SIZE);
    private List<Double> pnn50List = new LimitedList<Double>(PARAM_HISTORY_SIZE);

    /**
     * Receiver for HR data
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Events action = Events.valueOf(intent.getAction());
            if (Events.ACTION_HRV_DATA_AVAILABLE.equals(action)) {
                dataReceived((HrvParameters) intent
                        .getSerializableExtra(Events.HRV_DATA.toString()));
            }
        }
    };

    private void dataReceived(HrvParameters params) {

        meanHrList.add(params.getMeanHr());
        sdnnList.add(params.getSdnn());
        rmssdList.add(params.getRmssd());
        pnn50List.add(params.getPnn50());
        //TODO
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_HRV_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "started StressAssesmentService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public StressAssessmentService getService() {
            return StressAssessmentService.this;
        }
    }
}
