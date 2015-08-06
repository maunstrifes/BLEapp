package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.database.Session;
import ac.at.tuwien.inso.ble.services.HrvParameterService;
import ac.at.tuwien.inso.ble.utils.DateHelper;
import ac.at.tuwien.inso.ble.utils.Events;

/**
 * Shows already recorded sessions
 */
public class ShowSessionActivity extends Activity {

    /**
     * Receiver for HRV data
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Events action = Events.valueOf(intent.getAction());
            if (Events.ACTION_HRV_DATA_AVAILABLE.equals(action)) {
                showHrvParameters((HrvParameters) intent.getSerializableExtra(Events.HRV_DATA.toString()));
            } else {
                throw new RuntimeException("not implemented");
            }
        }
    };
    private Session session;
    private XYPlot plot;
    // HR values
    private List<Number> values;
    /**
     * Service for calculating the HRV parameters
     */
    private HrvParameterService hrvParameterService;
    private final ServiceConnection hrvParameterServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            hrvParameterService = ((HrvParameterService.LocalBinder) service)
                    .getService();
            // send HR data when service is started and connected
            sendHrToHrvParameterService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            hrvParameterService = null;
        }
    };

    /**
     * Reads the HR from the Session, plots it and sends it to the HrvParameterService to calculate the HRV parameters.
     * Also registers for HRV data and plots that upon receiving.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_session);
        final Intent intent = getIntent();
        session = (Session) intent.getSerializableExtra(AllSessionsActivity.EXTRAS_SESSION);
        plot = (XYPlot) findViewById(R.id.hrPlot);

        // Read data from session
        Pair<List<Date>, List<Number>> result = readFile();
        values = result.second;
        plotHr();

        // Register for HRV Data
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_HRV_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);

        // Start HrvParameter Service
        Intent hrvParameterServiceIntent = new Intent(this, HrvParameterService.class);
        bindService(hrvParameterServiceIntent, hrvParameterServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unbindService(hrvParameterServiceConnection);
    }

    /**
     * Broadcasts HR read from file before (called after HrvParameterService was started)
     */
    private void sendHrToHrvParameterService() {

        for (Number heartRate : values) {
            final Intent intent = new Intent(Events.ACTION_DATA_AVAILABLE.toString());
            intent.putExtra(Events.HR_DATA.toString(), String.valueOf(heartRate));
            sendBroadcast(intent);
        }
    }

    /**
     * Shows the received HRV data
     */
    private void showHrvParameters(HrvParameters hrvParameters) {
        System.out.println(hrvParameters.getMeanHr());
        System.out.println(hrvParameters.getSdnn());
        System.out.println(hrvParameters.getRmssd());
        System.out.println(hrvParameters.getPnn50());
        // TODO in gui ausgeben!
    }

    /**
     * Plots the heart rate
     */
    private void plotHr() {

        Pair<Number, Number> minmax = getBoundaries(values);
        XYSeries hrSeries = new SimpleXYSeries(
                values, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Heart Rate");
        LineAndPointFormatter seriesFormat = new LineAndPointFormatter();
        seriesFormat.configure(getApplicationContext(),
                R.xml.hrformatter);
        plot.addSeries(hrSeries, seriesFormat);
        plot.setRangeBoundaries(minmax.first, minmax.second, BoundaryMode.FIXED);
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
        plot.setMarkupEnabled(false);
        //TODO: Datum passend ausgeben (wie?)
    }

    /**
     * Gives the Boundaries according to min/max of all values rounded to the next multiple of 10.
     * Used for scaling the plot.
     *
     * @param values
     * @return
     */
    private Pair<Number, Number> getBoundaries(List<Number> values) {
        Number min = Integer.MAX_VALUE;
        Number max = Integer.MIN_VALUE;
        for (Number number : values) {
            if (number.intValue() < min.intValue()) {
                min = number;
            }
            if (number.intValue() > max.intValue()) {
                max = number;
            }
        }
        return new Pair<Number, Number>(Math.floor(min.doubleValue() / 10) * 10, Math.ceil(max.doubleValue() / 10) * 10);
    }

    /**
     * Returns the values of the session-file (heart rates and timestamps)
     *
     * @return
     */
    private Pair<List<Date>, List<Number>> readFile() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/" + session.getId() + ".csv");
        List<Number> valueList = new ArrayList<Number>(100);
        List<Date> dateList = new ArrayList<Date>(100);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String s;
            while ((s = reader.readLine()) != null) {
                String[] tmp = s.split(",");
                dateList.add(DateHelper.parse(tmp[0]));
                valueList.add(Integer.valueOf(tmp[1]));
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Session data not found!", Toast.LENGTH_LONG).show();
            //TODO: error handling?
        } catch (IOException e) {
            Toast.makeText(this, "Error reading session data!", Toast.LENGTH_LONG).show();
            //TODO: error handling?
        }
        return new Pair<List<Date>, List<Number>>(dateList, valueList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
