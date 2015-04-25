/*
 * Thanks to: http://www.vogella.com/articles/AndroidSQLite/article.html
 * 
 * Copyright APUS 2013. GPL Licensed. 
 * 
 */

package ac.at.tuwien.inso.ble.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BLESQLLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_SESSION = "session";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TIME = "time";
    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_SESSION + "(" + COLUMN_ID
            + " integer primary key autoincrement, "
            + COLUMN_TIME + " long);";
    private static final String DATABASE_NAME = "ble_db";
    private static final int DATABASE_VERSION = 2;

    public BLESQLLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(DATABASE_CREATE);
        Log.i("WICHTIG", "table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(BLESQLLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
        onCreate(db);
    }

} 