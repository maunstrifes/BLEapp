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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.MenuItem;

import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.services.BaselineService;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

/**
 * Records Baseline
 */
public class BaselineRecordActivity extends AbstractHrReceivingActivity {

    // Baseline length in ms
    private static final long BASELINE_TIME = 5 * 60 * 1000; // 5min


    protected BaselineService mBaselineService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBaselineService = ((BaselineService.LocalBinder) service)
                    .getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBaselineService = null;
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.record_baseline);

        super.onCreate(savedInstanceState);

        // start BaselineService
        Intent gattServiceIntent = new Intent(this, BaselineService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // end baseline recording after BASELINE_TIME
        new CountDownTimer(BASELINE_TIME, 1000) {

            @Override
            public void onTick(long msRemaining) {
                // TODO: verbleibende Zeit anzeigen
            }

            @Override
            public void onFinish() {
                stopBaseline();
            }
        }.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop_session:
                stopBaseline();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopBaseline() {
        final Intent intent = new Intent(this, ShowSessionActivity.class);
        intent.putExtra(IntentConstants.SESSION_ID.toString(), mBluetoothLeService.getRecordService().getSessionId());
        mBaselineService.saveBaseline();
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        startActivity(intent);
    }
}
