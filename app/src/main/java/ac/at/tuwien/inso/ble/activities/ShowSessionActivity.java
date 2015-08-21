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
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.database.Session;
import ac.at.tuwien.inso.ble.services.HrvParameterService;
import ac.at.tuwien.inso.ble.utils.DateHelper;
import ac.at.tuwien.inso.ble.utils.Events;

/**
 * Shows already recorded sessions
 */
public class ShowSessionActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Map<String, List<Double>> valueMap = new HashMap<String, List<Double>>();

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

        // Init Dropdown of parameters
        Spinner spinner = (Spinner) findViewById(R.id.params_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.parameters, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        // Init Map for data
        for (String param : getResources().getStringArray(R.array.parameters)) {
            valueMap.put(param, new ArrayList<Double>());
        }

        initPlot();

        // Register for HRV Data
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_HRV_DATA_AVAILABLE.toString());
        registerReceiver(broadcastReceiver, intentFilter);

        // Start HrvParameter Service
        Intent hrvParameterServiceIntent = new Intent(this, HrvParameterService.class);
        bindService(hrvParameterServiceIntent, hrvParameterServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Selects the data according to the chosen value of the Spinner
     *
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        String label = (String) parent.getItemAtPosition(pos);
        plot(label, valueMap.get(label));
    }

    /**
     * Needed for Spinner with Parameters
     *
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO?
    }

    /**
     * Shows or removes datapoints
     *
     * @param view
     */
    public void onCheckboxClicked(View view) {
        PointStyle style = ((CheckBox) view).isChecked() ? PointStyle.CIRCLE : PointStyle.POINT;
        ((XYSeriesRenderer) renderer.getSeriesRenderers()[0]).setPointStyle(style);
        chart.invalidate();
    }

    private void initPlot() {

        series = new XYSeries("");
        dataset.addSeries(series);

        XYSeriesRenderer xySeriesRenderer = new XYSeriesRenderer();
        xySeriesRenderer.setLineWidth(2);
        xySeriesRenderer.setColor(Color.RED);
        xySeriesRenderer.setDisplayBoundingPoints(true);
        xySeriesRenderer.setPointStyle(PointStyle.CIRCLE);
        xySeriesRenderer.setPointStrokeWidth(3);
        // Don't show legend as we use the same series for different stuff (chosen by Spinner)
        xySeriesRenderer.setShowLegendItem(false);

        renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer(xySeriesRenderer);
        renderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
        renderer.setShowGrid(true);

        // Calculate and set text size (https://www.google.com/design/spec/style/typography.html)
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, metrics);
        renderer.setLabelsTextSize(textSize);
        renderer.setLegendTextSize(textSize);
        renderer.setAxisTitleTextSize(textSize);
        renderer.setChartTitleTextSize(textSize);

        renderer.setMargins(new int[]{0, (int) textSize * 2, 10, 0});

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
        valueMap.get(getString(R.string.avg_heart_rate)).add(hrvParameters.getMeanHr());
        valueMap.get(getString(R.string.sdnn)).add(hrvParameters.getSdnn());
        valueMap.get(getString(R.string.rmssd)).add(hrvParameters.getRmssd());
        valueMap.get(getString(R.string.pnn50)).add(hrvParameters.getPnn50());
    }

    /**
     * Plots the heart rate
     */
    private void plot(String label, List<Double> values) {

        series.clearSeriesValues();
        for (int i = 0; i < values.size(); i++) {
            series.add(i, values.get(i));
        }
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(values.size());
        renderer.setYAxisMin(series.getMinY());
        renderer.setYAxisMax(series.getMaxY());
        renderer.setPanLimits(new double[]{0, values.size(), series.getMinY(), series.getMaxY()});
        renderer.setYTitle(label);

        chart.invalidate();
        //TODO: Datum passend ausgeben (wie?)
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
            valueMap.put(getString(R.string.heart_rate), values);
            for (Number heartRate : values) {
                final Intent intent = new Intent(Events.ACTION_DATA_AVAILABLE.toString());
                intent.putExtra(Events.HR_DATA.toString(), String.valueOf(heartRate));
                sendBroadcast(intent);
            }
            plot(getString(R.string.heart_rate), values);
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
