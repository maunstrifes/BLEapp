package ac.at.tuwien.inso.ble.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import pro.apus.heartrate.R;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button btnSessionStart;
    private Button btnAllSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSessionStart = (Button) findViewById(R.id.session_start);
        btnSessionStart.setOnClickListener(this);
        btnAllSessions = (Button) findViewById(R.id.all_sessions);
        btnAllSessions.setOnClickListener(this);
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
     *
     * @param view
     */
    @Override
    public void onClick(View view) {

        if(view.equals(btnSessionStart)) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivity(intent);
        }
        if(view.equals(btnAllSessions)){
            Intent intent = new Intent(this, AllSessionsActivity.class);
            startActivity(intent);
        }
    }
}
