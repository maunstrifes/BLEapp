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
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;

import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends AbstractHrReceivingActivity implements View.OnClickListener {

    // Breath Pacer
    private static final int PACER_DURATION = 5000; //ms
    private Button pacerBtn;
    private ImageView pacerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.heartrate);
        pacerBtn = (Button) findViewById(R.id.pacer_start);
        pacerBtn.setOnClickListener(this);
        pacerView = (ImageView) findViewById(R.id.pacer_view);
        pacerView.setScaleX(0.5f);
        pacerView.setScaleY(0.5f);

        super.onCreate(savedInstanceState);
    }

    /**
     * Starts the Breath Pacer
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop_session:
                final Intent intent = new Intent(this, ShowSessionActivity.class);
                intent.putExtra(IntentConstants.SESSION_ID.toString(), mBluetoothLeService.getRecordService().getSessionId());
                mBluetoothLeService.disconnect();
                startActivity(intent);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
