package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

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
    private GraphicalView chart;
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    private XYSeries series;
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
        initPlot();

        // Register for HRV Data
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_HRV_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);

        // Start HrvParameter Service
        Intent hrvParameterServiceIntent = new Intent(this, HrvParameterService.class);
        bindService(hrvParameterServiceIntent, hrvParameterServiceConnection, BIND_AUTO_CREATE);
    }

    private void initPlot() {

        series = new XYSeries("HR");
        dataset.addSeries(series);

        XYSeriesRenderer xySeriesRenderer = new XYSeriesRenderer();
        xySeriesRenderer.setLineWidth(2);
        xySeriesRenderer.setColor(Color.RED);
        // Include low and max value
        xySeriesRenderer.setDisplayBoundingPoints(true);
        // we add point markers
        xySeriesRenderer.setPointStyle(PointStyle.CIRCLE);
        xySeriesRenderer.setPointStrokeWidth(3);

        renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(xySeriesRenderer);
        // We want to avoid black border
        renderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
        // Disable Pan on two axis
//        renderer.setPanEnabled(false, false);
        renderer.setShowGrid(true);
        chart = ChartFactory.getLineChartView(this, dataset, renderer);
        ((LinearLayout) findViewById(R.id.chart)).addView(chart, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unbindService(hrvParameterServiceConnection);
    }

    /**
     * Reads and broadcasts HR (called after HrvParameterService was started)
     */
    private void sendHrToHrvParameterService() {
        final Intent intent = getIntent();
        Session session = (Session) intent.getSerializableExtra(AllSessionsActivity.EXTRAS_SESSION);
        new SessionFileReaderTask().execute(session);
    }

    /**
     * Shows the received HRV data
     */
    private void showHrvParameters(HrvParameters hrvParameters) {
        System.out.println(hrvParameters.getMeanHr());
//        System.out.println(hrvParameters.getSdnn());
//        System.out.println(hrvParameters.getRmssd());
//        System.out.println(hrvParameters.getPnn50());
        // TODO in gui ausgeben!
    }

    /**
     * Plots the heart rate
     */
    private void plotHr(List<Double> values) {

        Pair<Double, Double> minmax = getBoundaries(values);
        for (int i = 0; i < values.size(); i++) {
            series.add(i, values.get(i));
        }

//        XYSeries hrSeries = new SimpleXYSeries(
//                values, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Heart Rate");
//        LineAndPointFormatter seriesFormat = new LineAndPointFormatter();
//        seriesFormat.configure(getApplicationContext(),
//                R.xml.hrformatter);
//        plot.addSeries(hrSeries, seriesFormat);
//        plot.setRangeBoundaries(minmax.first, minmax.second, BoundaryMode.FIXED);
//        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 10);
//        plot.setMarkupEnabled(false);
//        plot.invalidate();


        renderer.setYAxisMin(minmax.first);
        renderer.setYAxisMax(minmax.second);

        chart.invalidate();
        //TODO: Datum passend ausgeben (wie?)
    }

    /**
     * Gives the Boundaries according to min/max of all values rounded to the next multiple of 10.
     * Used for scaling the plot.
     *
     * @param values
     * @return
     */
    private Pair<Double, Double> getBoundaries(List<Double> values) {
        Double min = Double.MAX_VALUE;
        Double max = Double.MIN_VALUE;
        for (Double number : values) {
            if (number.intValue() < min.intValue()) {
                min = number;
            }
            if (number.intValue() > max.intValue()) {
                max = number;
            }
        }
        return new Pair<Double, Double>(Math.floor(min.doubleValue() / 10) * 10, Math.ceil(max.doubleValue() / 10) * 10);
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

    /**
     * Reads the Session file async and then broadcasts and plots the HR data
     */
    private class SessionFileReaderTask extends AsyncTask<Session, Void, List<Double>> {

        ProgressDialog asyncDialog = new ProgressDialog(ShowSessionActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setMessage(getString(R.string.loading));
            asyncDialog.show();
        }

        @Override
        protected List<Double> doInBackground(Session... sessions) {
            if (sessions.length > 1) throw new RuntimeException("too many sessions, why?");
            // Read data from session
            Pair<List<Date>, List<Double>> result = readFile(sessions[0]);
            return result.second;
        }

        /**
         * Broadcasts HR values and plots HR
         *
         * @param values
         */
        protected void onPostExecute(List<Double> values) {
            for (Number heartRate : values) {
                final Intent intent = new Intent(Events.ACTION_DATA_AVAILABLE.toString());
                intent.putExtra(Events.HR_DATA.toString(), String.valueOf(heartRate));
                sendBroadcast(intent);
            }
            plotHr(values);
            asyncDialog.dismiss();
        }

        /**
         * Returns the values of the session-file (heart rates and timestamps)
         *
         * @return
         */
        private Pair<List<Date>, List<Double>> readFile(Session session) {
            File file = new File(Environment.getExternalStorageDirectory()
                    .getPath() + "/" + session.getId() + ".csv");
            List<Double> valueList = new ArrayList<Double>(100);
            List<Date> dateList = new ArrayList<Date>(100);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String s;
                while ((s = reader.readLine()) != null) {
                    String[] tmp = s.split(",");
                    dateList.add(DateHelper.parse(tmp[0]));
                    valueList.add(Double.valueOf(tmp[1]));
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(ShowSessionActivity.this, "Session data not found!", Toast.LENGTH_LONG).show();
                //TODO: error handling?
            } catch (IOException e) {
                Toast.makeText(ShowSessionActivity.this, "Error reading session data!", Toast.LENGTH_LONG).show();
                //TODO: error handling?
            }
            return new Pair<List<Date>, List<Double>>(dateList, valueList);
        }
    }
}
