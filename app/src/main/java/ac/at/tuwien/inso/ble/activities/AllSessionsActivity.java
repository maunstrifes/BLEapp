package ac.at.tuwien.inso.ble.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.adapter.SessionAdapter;
import ac.at.tuwien.inso.ble.database.Session;
import ac.at.tuwien.inso.ble.database.SessionDataSource;
import ac.at.tuwien.inso.ble.utils.IntentConstants;

public class AllSessionsActivity extends ListActivity {

    private final static String TAG = AllSessionsActivity.class
            .getSimpleName();
    private static final int CONTEXT_MENU_DELETE_ITEM = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_sessions);

        SessionDataSource datasource = new SessionDataSource(this);
        datasource.open();
        List<Session> sessions = datasource.getAllSessions();
        datasource.close();
        setListAdapter(new SessionAdapter(this, sessions));
        registerForContextMenu(getListView());
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        Session session = (Session) getListView().getItemAtPosition(position);

        final Intent intent = new Intent(this, ShowSessionActivity.class);
        intent.putExtra(IntentConstants.SESSION_ID.toString(), session.getId());
        startActivity(intent);
    }

    /**
     * Context menu for long clicking list items
     *
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(getString(R.string.session_delete));
        menu.add(0, CONTEXT_MENU_DELETE_ITEM, 0, getString(R.string.menu_delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == CONTEXT_MENU_DELETE_ITEM) {
            Session session = (Session) getListAdapter().getItem(info.position);
            deleteSession(session);
            return true;
        }
        return false;
    }

    /**
     * Deletes the session and removes it from the list
     *
     * @param session
     */
    private void deleteSession(Session session) {
        SessionDataSource datasource = new SessionDataSource(this);
        datasource.open();
        datasource.deleteSession(session);
        datasource.close();
        SessionAdapter sessionAdapter = (SessionAdapter) getListAdapter();
        sessionAdapter.remove(session);
        sessionAdapter.notifyDataSetChanged();
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
