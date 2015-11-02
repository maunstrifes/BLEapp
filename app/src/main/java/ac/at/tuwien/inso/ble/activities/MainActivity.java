package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.utils.Baseline;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button btnSessionStart;
    private Button btnBaselineStart;
    private Button btnAllSessions;
    private TextView baselineParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSessionStart = (Button) findViewById(R.id.session_start);
        btnSessionStart.setOnClickListener(this);
        btnBaselineStart = (Button) findViewById(R.id.baseline_start);
        btnBaselineStart.setOnClickListener(this);
        btnAllSessions = (Button) findViewById(R.id.all_sessions);
        btnAllSessions.setOnClickListener(this);
        baselineParams = (TextView) findViewById(R.id.baseline_params);
        setBaselineParamsTxt();
    }

    private void setBaselineParamsTxt() {
        Baseline baseline = Baseline.getInstance(this);
        HrvParameters params = baseline.getParams();
        if (params != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            String text = getString(R.string.avg_heart_rate) + ": " + df.format(params.getMeanHr()) + " bpm\n";
            text += getString(R.string.sdnn) + ": " + df.format(params.getSdnn()) + "\n";
            text += getString(R.string.rmssd) + ": " + df.format(params.getRmssd()) + "\n";
            text += getString(R.string.pnn50) + ": " + df.format(params.getPnn50()) + " %\n";
            baselineParams.setText(text);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    /**
     * Start the session by connecting to device
     */
    @Override
    public void onClick(View view) {

        if (view.equals(btnSessionStart)) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivity(intent);
        } else if (view.equals(btnBaselineStart)) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            intent.putExtra(IntentConstants.IS_BASELINE.toString(), true);
            startActivity(intent);
        } else if (view.equals(btnAllSessions)) {
            Intent intent = new Intent(this, AllSessionsActivity.class);
            startActivity(intent);
        }
    }
}
