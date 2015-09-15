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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

/**
 * Records Baseline
 */
public class BaselineRecordActivity extends AbstractHrReceivingActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.record_baseline);

        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop_session:
                final Intent intent = new Intent(this, ShowSessionActivity.class); //TODO: richtige activity, die die baseline speichert
                intent.putExtra(IntentConstants.SESSION_ID.toString(), mBluetoothLeService.getRecordService().getSessionId());
//                mBluetoothLeService.getRecordService().writeBaseline();
                mBluetoothLeService.disconnect();
                startActivity(intent);
                //TODO: weiter zur korrekten Activity
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
