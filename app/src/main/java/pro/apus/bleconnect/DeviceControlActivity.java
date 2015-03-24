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

package pro.apus.bleconnect;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import pro.apus.heartrate.R;

//import android.widget.ExpandableListView;
//import android.widget.SimpleExpandableListAdapter;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
	// BLE stuff
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TAG = DeviceControlActivity.class
            .getSimpleName();
    // Various UI stuff
    public static boolean currentlyVisible;
    private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
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
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                mConnected = false;
                updateConnectionState(false);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(mBluetoothLeService
                        .getSupportedGattServices());
                // mButtonStop.setVisibility(View.VISIBLE);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent
                        .getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // Database
    private EventsDataSource datasource;
    private boolean logging = false;
	private TextView mDataField;
	private String mDeviceName;
	private String mDeviceAddress;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private ImageButton mButtonStart;
	private ImageButton mButtonStop;
	// Chart stuff
	private GraphicalView mChart;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	private XYSeries mCurrentSeries;
	private XYSeriesRenderer mCurrentRenderer;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
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

        // Set up database connection
        datasource = new EventsDataSource(this);
        datasource.open();

        mDataField = (TextView) findViewById(R.id.data_value);

        mButtonStart = (ImageButton) findViewById(R.id.btnStart);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startLogging();
            }
        });

        mButtonStop = (ImageButton) findViewById(R.id.btnStop);
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopLogging();
            }
        });

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        // TODO: Lars added this
        this.startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        if (mChart == null) {
            initChart();
            mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer,
                    "hh:mm");
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }

    }

	@Override
    protected void onResume() {
        super.onResume();
        currentlyVisible = true;

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
	}

    // this is called when the screen rotates.
    // (onCreate is no longer called when screen rotates due to manifest, see:
    // android:configChanges)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // setContentView(R.layout.heartrate);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "ORIENTATION_LANDSCAPE");
        } else {
            Log.i(TAG, "ORIENTATION_PORTRAIT");
        }
    }

	@Override
    protected void onPause() {
        super.onPause();
        currentlyVisible = false;
        // unregisterReceiver(mGattUpdateReceiver);
    }

	@Override
    protected void onDestroy() {
        super.onDestroy();
        currentlyVisible = false;
        unbindService(mServiceConnection);
        // mBluetoothLeService = null;
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            if (logging) {
                menu.findItem(R.id.menu_start_logging).setVisible(false);
                menu.findItem(R.id.menu_stop_logging).setVisible(true);
                mButtonStart.setVisibility(View.GONE);
                mButtonStop.setVisibility(View.VISIBLE);
            } else {
                menu.findItem(R.id.menu_start_logging).setVisible(true);
                menu.findItem(R.id.menu_stop_logging).setVisible(false);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonStop.setVisibility(View.GONE);
            }
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_start_logging).setVisible(false);
            menu.findItem(R.id.menu_stop_logging).setVisible(false);
            mButtonStart.setVisibility(View.GONE);
            mButtonStop.setVisibility(View.GONE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case R.id.menu_start_logging:
                startLogging();
                return true;
            case R.id.menu_stop_logging:
                stopLogging();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	private void updateConnectionState(final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    mButtonStart.setVisibility(View.VISIBLE);
                    mButtonStop.setVisibility(View.GONE);
                } else {
                    mButtonStart.setVisibility(View.GONE);
                    mButtonStop.setVisibility(View.GONE);
                }
			}
        });
    }

	private void displayData(String data) {
        try {
            if (data != null) {

                long time = (new Date()).getTime();
                int dataElement = Integer.parseInt(data);
                mCurrentSeries.add(time, dataElement);
                appendLog((new Date()).toString() + "," + data);
                //datasource.createEvent(1, time, dataElement);
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

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid;
        String unknownServiceString = getResources().getString(
                R.string.unknown_service);
        String unknownCharaString = getResources().getString(
                R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME,
                    SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                if (UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
                        .equals(gattCharacteristic.getUuid())) {
                    Log.d(TAG, "Found heart rate");
                    mNotifyCharacteristic = gattCharacteristic;
                }

                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME,
                        SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

	}

	public void appendLog(String text) {
		File logFile = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/hrmlog.csv");
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "Error while creating file. ", e);
				e.printStackTrace();
			}
		}
		try {
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
					true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startLogging() {
		mButtonStop.setVisibility(View.VISIBLE);
		mButtonStart.setVisibility(View.GONE);
		mBluetoothLeService.setCharacteristicNotification(
				mNotifyCharacteristic, true);
		invalidateOptionsMenu();
		logging = true;
	}

	private void stopLogging() {
		mButtonStop.setVisibility(View.GONE);
		mButtonStart.setVisibility(View.VISIBLE);
		mBluetoothLeService.setCharacteristicNotification(
				mNotifyCharacteristic, false);
		invalidateOptionsMenu();
		logging = false;
	}
}
