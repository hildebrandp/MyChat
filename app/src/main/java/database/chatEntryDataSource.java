package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;


public class chatEntryDataSource {

    // Database fields
    private SQLiteDatabase chatdatabase;
    private SQLiteHelper chatdbHelper;

    private String[] allColumnsChat = { SQLiteHelper.COLUMN_CHAT_ID,
            SQLiteHelper.COLUMN_CHAT_SENDER_ID, SQLiteHelper.COLUMN_CHAT_RECIEVER_ID, SQLiteHelper.COLUMN_CHAT_MESSAGE,
            SQLiteHelper.COLUMN_CHAT_READ, SQLiteHelper.COLUMN_CHAT_DATE, SQLiteHelper.COLUMN_CHAT_ISSEND};

    public chatEntryDataSource(Context context) {
        chatdbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        chatdatabase = chatdbHelper.getWritableDatabase();
    }

    public void close() {
        chatdbHelper.close();
    }

    public chatDbEntry createChatEntry(String CHAT_SENDER_ID,String CHAT_RECIEVER_ID,String CHAT_MESSAGE, String CHAT_READ, String CHAT_DATE, String CHAT_ISSEND) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_CHAT_SENDER_ID, CHAT_SENDER_ID);
        values.put(SQLiteHelper.COLUMN_CHAT_RECIEVER_ID, CHAT_RECIEVER_ID);
        values.put(SQLiteHelper.COLUMN_CHAT_MESSAGE, CHAT_MESSAGE);
        values.put(SQLiteHelper.COLUMN_CHAT_READ, CHAT_READ);
        values.put(SQLiteHelper.COLUMN_CHAT_DATE, CHAT_DATE);
        values.put(SQLiteHelper.COLUMN_CHAT_ISSEND, CHAT_ISSEND);

        long insertId = chatdatabase.insert(SQLiteHelper.TABLE_CHAT, null, values);
        Cursor cursor = chatdatabase.query(SQLiteHelper.TABLE_CHAT, allColumnsChat,SQLiteHelper.COLUMN_CHAT_ID + " = " + insertId, null, null, null, null, null);
        cursor.moveToFirst();
        chatDbEntry newDbEntry = cursorToEntry(cursor);
        cursor.close();
        return newDbEntry;
    }

    public void deleteEntry(chatDbEntry id) {
        chatdatabase.delete(SQLiteHelper.TABLE_CHAT, SQLiteHelper.COLUMN_CHAT_ID
                + " = " + id, null);
    }

    public List<chatDbEntry> getAllChatEntries() {
        List<chatDbEntry> entries = new ArrayList<chatDbEntry>();

        Cursor cursor = chatdatabase.query(SQLiteHelper.TABLE_CHAT,
                allColumnsChat, null, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            chatDbEntry entry = cursorToEntry(cursor);
            entries.add(entry);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return entries;
    }

    private chatDbEntry cursorToEntry(Cursor cursor) {
        chatDbEntry entry = new chatDbEntry();
        entry.setId(cursor.getLong(0));
        entry.setCHAT_ID(cursor.getLong(1));
        entry.setCHAT_SENDER_ID(cursor.getString(2));
        entry.setCHAT_RECIEVER_ID(cursor.getString(3));
        entry.setCHAT_MESSAGE(cursor.getString(4));
        entry.setCHAT_READ(cursor.getString(5));
        entry.setCHAT_DATE(cursor.getString(6));
        entry.setCHAT_ISSEND(cursor.getString(7));
        return entry;
    }
}