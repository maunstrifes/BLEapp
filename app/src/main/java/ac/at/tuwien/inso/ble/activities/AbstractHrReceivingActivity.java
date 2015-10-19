package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Date;

import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.services.BluetoothLeService;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

/**
 * Abstract class for Activities that connect to the BLE sensor and receive the heart rate.
 */
public class AbstractHrReceivingActivity extends Activity {

    private final static String TAG = AbstractHrReceivingActivity.class
            .getSimpleName();
    // Various UI stuff
    public static boolean currentlyVisible;
    protected BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    // Chart stuff
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final IntentConstants action = IntentConstants.valueOf(intent.getAction());
            if (IntentConstants.ACTION_GATT_CONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if (IntentConstants.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                invalidateOptionsMenu();
                clearUI();
            } else if (IntentConstants.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                //TODO: what to do?
                // Show all the supported services and characteristics on the
                // user interface.
//                displayGattServices(mBluetoothLeService
//                        .getSupportedGattServices());
                // mButtonStop.setVisibility(View.VISIBLE);
            } else if (IntentConstants.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent
                        .getStringExtra(IntentConstants.HR_DATA.toString()));
            }
        }
    };
    private XYSeriesRenderer mCurrentRenderer;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentConstants.ACTION_GATT_CONNECTED.toString());
        intentFilter.addAction(IntentConstants.ACTION_GATT_DISCONNECTED.toString());
        intentFilter
                .addAction(IntentConstants.ACTION_GATT_SERVICES_DISCOVERED.toString());
        intentFilter.addAction(IntentConstants.ACTION_DATA_AVAILABLE.toString());
        return intentFilter;
    }

    private void initChart() {

        Log.i(TAG, "initChart");
        if (mCurrentSeries == null) {
            mCurrentSeries = new XYSeries("Heart Rate");
            mDataset.addSeries(mCurrentSeries);
            Log.i(TAG, "initChart mCurrentSeries == null");
        }

        if (mCurrentRenderer == null) {
            mCurrentRenderer = new XYSeriesRenderer();
            mCurrentRenderer.setLineWidth(4);

            mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
            mCurrentRenderer.setFillPoints(true);
            mCurrentRenderer.setColor(Color.GREEN);
            Log.i(TAG, "initChart mCurrentRenderer == null");

            mRenderer.setAxisTitleTextSize(70);
            mRenderer.setPointSize(5);
            mRenderer.setYTitle("Time");
            mRenderer.setYTitle("Heart rate");
            mRenderer.setPanEnabled(true);
            mRenderer.setLabelsTextSize(50);
            mRenderer.setLegendTextSize(50);

            mRenderer.setYAxisMin(0);
            mRenderer.setYAxisMax(120);
            mRenderer.setXAxisMin(0);
            mRenderer.setXAxisMax(100);

            mRenderer.setShowLegend(false);

            mRenderer.setApplyBackgroundColor(true);
            mRenderer.setBackgroundColor(Color.BLACK);
            mRenderer.setMarginsColor(Color.BLACK);

            mRenderer.setShowGridY(true);
            mRenderer.setShowGridX(true);
            mRenderer.setGridColor(Color.WHITE);
            // mRenderer.setShowCustomTextGrid(true);

            mRenderer.setAntialiasing(true);
            mRenderer.setPanEnabled(true, false);
            mRenderer.setZoomEnabled(true, false);
            mRenderer.setZoomButtonsVisible(false);
            mRenderer.setXLabelsColor(Color.WHITE);
            mRenderer.setYLabelsColor(0, Color.WHITE);
            mRenderer.setXLabelsAlign(Paint.Align.CENTER);
            mRenderer.setXLabelsPadding(10);
            mRenderer.setXLabelsAngle(-30.0f);
            mRenderer.setYLabelsAlign(Paint.Align.RIGHT);
            mRenderer.setPointSize(3);
            mRenderer.setInScroll(true);
            // mRenderer.setShowLegend(false);
            mRenderer.setMargins(new int[]{50, 150, 10, 50});

            mRenderer.addSeriesRenderer(mCurrentRenderer);
        }
    }

    private void clearUI() {
        // mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");

        // getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(IntentConstants.DEVICE_NAME.toString());
        mDeviceAddress = intent.getStringExtra(IntentConstants.DEVICE_ADDRESS.toString());

        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(IntentConstants.DEVICE_ADDRESS.toString(), mDeviceAddress);
        gattServiceIntent.putExtra(IntentConstants.IS_BASELINE.toString(), this instanceof BaselineRecordActivity);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (mChart == null) {
            initChart();
            mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer,
                    "hh:mm");
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        currentlyVisible = true;

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//            Log.d(TAG, "Connect request result=" + result);
//        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        currentlyVisible = false;
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroy");
        super.onDestroy();
        currentlyVisible = false;
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        return true;
    }

    private void displayData(String data) {
        try {
            if (data != null) {

                long time = (new Date()).getTime();
                double dataElement = Double.parseDouble(data);
                mCurrentSeries.add(time, dataElement);
                // Storing last 600 only - should average...
                while (mCurrentSeries.getItemCount() > 60 * 10) {
                    mCurrentSeries.remove(0);
                }

                if (currentlyVisible) {
                    mDataField.setText("Pulse: " + data);

                    mRenderer.setYAxisMin(0);
                    mRenderer.setYAxisMax(mCurrentSeries.getMaxY() + 20);

                    double minx = mCurrentSeries.getMinX();
                    double maxx = mCurrentSeries.getMaxX();

                    if ((maxx - minx) < 5 * 60 * 1000) {
                        mRenderer.setXAxisMin(minx);
                        mRenderer.setXAxisMax(minx + (5 * 60 * 1000));
                    } else {
                        mRenderer.setXAxisMin(maxx - (5 * 60 * 1000));
                        mRenderer.setXAxisMax(maxx);
                    }

                    mChart.repaint();
                    mChart.zoomReset();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing: " + data);
            e.printStackTrace();
        }
    }
}
