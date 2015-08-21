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

package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
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
import ac.at.tuwien.inso.ble.utils.Events;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements View.OnClickListener {

    // BLE stuff
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // Breath Pacer
    private static final int PACER_DURATION = 5000; //ms
    private final static String TAG = DeviceControlActivity.class
            .getSimpleName();
    // Various UI stuff
    public static boolean currentlyVisible;
    private BluetoothLeService mBluetoothLeService;
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
            final Events action = Events.valueOf(intent.getAction());
            if (Events.ACTION_GATT_CONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if (Events.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                invalidateOptionsMenu();
                clearUI();
            } else if (Events.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
//                displayGattServices(mBluetoothLeService
//                        .getSupportedGattServices());
                // mButtonStop.setVisibility(View.VISIBLE);
            } else if (Events.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent
                        .getStringExtra(Events.HR_DATA.toString()));
            }
        }
    };
    private XYSeriesRenderer mCurrentRenderer;
    private Button pacerBtn;
    private ImageView pacerView;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Events.ACTION_GATT_CONNECTED.toString());
        intentFilter.addAction(Events.ACTION_GATT_DISCONNECTED.toString());
        intentFilter
                .addAction(Events.ACTION_GATT_SERVICES_DISCOVERED.toString());
        intentFilter.addAction(Events.ACTION_DATA_AVAILABLE.toString());
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
            mRenderer.setXLabelsAlign(Align.CENTER);
            mRenderer.setXLabelsPadding(10);
            mRenderer.setXLabelsAngle(-30.0f);
            mRenderer.setYLabelsAlign(Align.RIGHT);
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

        setContentView(R.layout.heartrate);

        // getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mDataField = (TextView) findViewById(R.id.data_value);

        pacerBtn = (Button) findViewById(R.id.pacer_start);
        pacerBtn.setOnClickListener(this);
        pacerView = (ImageView) findViewById(R.id.pacer_view);
        pacerView.setScaleX(0.5f);
        pacerView.setScaleY(0.5f);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop_session:
                mBluetoothLeService.disconnect();
                //TODO: weiter zu nächster Activity
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayData(String data) {
        try {
            if (data != null) {

                long time = (new Date()).getTime();
                int dataElement = Integer.parseInt(data);
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
        }
    }

    /**
     * Start the Breath Pacer
     *
     * @param view
     */
    @Override
    public void onClick(View view) {

        if (view.equals(pacerBtn)) {
            if (pacerBtn.getText().equals(getString(R.string.pacer_start))) {
                pacerBtn.setText(getString(R.string.pacer_stop));

                Animation scaleAnimator = new ScaleAnimation(1f, 2f, 1f, 2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleAnimator.setDuration(PACER_DURATION);
                scaleAnimator.setFillAfter(true);
                scaleAnimator.setRepeatCount(Animation.INFINITE);
                scaleAnimator.setRepeatMode(Animation.REVERSE);
                pacerView.startAnimation(scaleAnimator);
            } else {
                pacerBtn.setText(getString(R.string.pacer_start));
                pacerView.clearAnimation();
            }
        }
    }
}
