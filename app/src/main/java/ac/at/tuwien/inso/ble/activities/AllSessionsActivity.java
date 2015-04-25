package ac.at.tuwien.inso.ble.activities;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import ac.at.tuwien.inso.ble.adapter.SessionAdapter;
import ac.at.tuwien.inso.ble.database.Session;
import ac.at.tuwien.inso.ble.database.SessionDataSource;
import pro.apus.heartrate.R;

public class AllSessionsActivity extends ListActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_sessions);

        SessionDataSource datasource = new SessionDataSource(this);
        datasource.open();
        List<Session> sessions = datasource.getAllSessions();
        datasource.close();
        setListAdapter(new SessionAdapter(this, sessions));
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        Session selectedItem = (Session) getListView().getItemAtPosition(position);
        Toast.makeText(this, "You clicked Session " + selectedItem.getId() + " at position " + position, Toast.LENGTH_LONG);
        //TODO. open session
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all_sessions, menu);
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
}
