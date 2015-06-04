/*
 * Thanks to: http://www.vogella.com/articles/AndroidSQLite/article.html
 * 
 * Copyright APUS 2013. GPL Licensed. 
 * 
 */

package ac.at.tuwien.inso.ble.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SessionDataSource {

    // Database fields
    private SQLiteDatabase database;
    private BLESQLLiteHelper dbHelper;
    private String[] allColumns = {BLESQLLiteHelper.COLUMN_ID,
            BLESQLLiteHelper.COLUMN_TIME};

    public SessionDataSource(Context context) {
        dbHelper = new BLESQLLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Session createSession(long time) {

        ContentValues values = new ContentValues();
        values.put(BLESQLLiteHelper.COLUMN_TIME, time);

        long insertId = database.insert(BLESQLLiteHelper.TABLE_SESSION, null,
                values);
        Cursor cursor = database.query(BLESQLLiteHelper.TABLE_SESSION,
                allColumns, BLESQLLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Session newSession = cursorToEvent(cursor);
        cursor.close();
        return newSession;
    }

    public void deleteSession(Session session) {
        long id = session.getId();
        database.delete(BLESQLLiteHelper.TABLE_SESSION, BLESQLLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Session> getAllSessions() {
        List<Session> sessions = new ArrayList<Session>();

        Cursor cursor = database.query(BLESQLLiteHelper.TABLE_SESSION,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Session session = cursorToEvent(cursor);
            sessions.add(session);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return sessions;
    }

    private Session cursorToEvent(Cursor cursor) {
        Session session = new Session();
        session.setId(cursor.getLong(0));
        session.setTime(cursor.getLong(1));
        return session;
    }
} 