package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;


public class userEntryDataSource {

    // Database fields
    private SQLiteDatabase userdatabase;
    private SQLiteHelper userdbHelper;

    private String[] allColumns = { SQLiteHelper.COLUMN_USER_ID, SQLiteHelper.COLUMN_USERID,
            SQLiteHelper.COLUMN_USER_NAME, SQLiteHelper.COLUMN_USER_PUBLICKEY};

    public userEntryDataSource(Context context) {
        userdbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        userdatabase = userdbHelper.getWritableDatabase();
    }

    public void close() {
        userdbHelper.close();
    }

    public userDbEntry createUserEntry(String USER_ID, String USER_NAME,String USER_PUBLICKEY) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_USERID, USER_ID);
        values.put(SQLiteHelper.COLUMN_USER_NAME, USER_NAME);
        values.put(SQLiteHelper.COLUMN_USER_PUBLICKEY, USER_PUBLICKEY);

        long insertId = userdatabase.insert(SQLiteHelper.TABLE_USER, null, values);
        Cursor cursor = userdatabase.query(SQLiteHelper.TABLE_USER,
                allColumns, SQLiteHelper.COLUMN_USER_ID + " = " + insertId, null,
                null, null, null, null);
        cursor.moveToFirst();
        userDbEntry newDbEntry = cursorToEntry(cursor);
        cursor.close();
        return newDbEntry;
    }

    public void deleteEntry(userDbEntry id) {
        userdatabase.delete(SQLiteHelper.TABLE_USER, SQLiteHelper.COLUMN_USER_ID
                + " = " + id, null);
    }

    public List<userDbEntry> getAllUserEntries() {
        List<userDbEntry> entries = new ArrayList<userDbEntry>();

        Cursor cursor = userdatabase.query(SQLiteHelper.TABLE_USER,
                allColumns, null, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            userDbEntry entry = cursorToEntry(cursor);
            entries.add(entry);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return entries;
    }

    private userDbEntry cursorToEntry(Cursor cursor) {
        userDbEntry entry = new userDbEntry();
        entry.setId(cursor.getLong(0));
        entry.setUSER_ID(cursor.getString(1));
        entry.setUSER_NAME(cursor.getString(2));
        entry.setUSER_PUBLICKEY(cursor.getString(3));
        return entry;
    }
}
