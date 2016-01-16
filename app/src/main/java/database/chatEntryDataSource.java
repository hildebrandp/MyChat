package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;


//Klasse um ein Datenbank Element für die Tabelle Chat zu erstellen, auszulesen
//oder um eins zu löschen
public class chatEntryDataSource {

    //Datenbank Feld
    private SQLiteDatabase chatdatabase;
    private SQLiteHelper chatdbHelper;

    //String in dem alle Felder der Tabelle Chat gespeichert sind
    private String[] allColumnsChat = {SQLiteHelper.COLUMN_CHAT_UNIQUE_ID,SQLiteHelper.COLUMN_CHAT_ID,
            SQLiteHelper.COLUMN_CHAT_SENDER_ID, SQLiteHelper.COLUMN_CHAT_RECIEVER_ID, SQLiteHelper.COLUMN_CHAT_MESSAGE,
            SQLiteHelper.COLUMN_CHAT_READ, SQLiteHelper.COLUMN_CHAT_DATE, SQLiteHelper.COLUMN_CHAT_ISSEND, SQLiteHelper.COLUMN_CHAT_AESKEY, SQLiteHelper.COLUMN_CHAT_SIGNATURE};

    //Methode um Datenbank Feld zu initialisieren
    public chatEntryDataSource(Context context) {
        chatdbHelper = new SQLiteHelper(context);
    }

    //Methode um die datenbank mit schreibzugriff zu öffnen
    public void open() throws SQLException {
        chatdatabase = chatdbHelper.getWritableDatabase();
    }

    //Methode um die datenbank zu schließen
    public void close() {
        chatdbHelper.close();
    }

    //Methode um einen neuen Eintrag in die Tabelle Chat zu machen
    public chatDbEntry createChatEntry(Long CHAT_ID,String CHAT_SENDER_ID,String CHAT_RECIEVER_ID,String CHAT_MESSAGE, String CHAT_READ, String CHAT_DATE, String CHAT_ISSEND, String CHAT_AESKEY, String CHAT_SIGNATURE) {
        //Erstelle ein Element mit den Übergebenen Daten
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_CHAT_ID, CHAT_ID);
        values.put(SQLiteHelper.COLUMN_CHAT_SENDER_ID, CHAT_SENDER_ID);
        values.put(SQLiteHelper.COLUMN_CHAT_RECIEVER_ID, CHAT_RECIEVER_ID);
        values.put(SQLiteHelper.COLUMN_CHAT_MESSAGE, CHAT_MESSAGE);
        values.put(SQLiteHelper.COLUMN_CHAT_READ, CHAT_READ);
        values.put(SQLiteHelper.COLUMN_CHAT_DATE, CHAT_DATE);
        values.put(SQLiteHelper.COLUMN_CHAT_ISSEND, CHAT_ISSEND);
        values.put(SQLiteHelper.COLUMN_CHAT_AESKEY, CHAT_AESKEY);
        values.put(SQLiteHelper.COLUMN_CHAT_SIGNATURE, CHAT_SIGNATURE);

        //Füge die Daten in die Tabelle Chat ein
        long insertId = chatdatabase.insert(SQLiteHelper.TABLE_CHAT, null, values);
        Cursor cursor = chatdatabase.query(SQLiteHelper.TABLE_CHAT, allColumnsChat,
                SQLiteHelper.COLUMN_CHAT_UNIQUE_ID + " = " + insertId, null, null, null, null, null);
        cursor.moveToFirst();
        chatDbEntry newDbEntry = cursorToEntry(cursor);
        cursor.close();
        return newDbEntry;
    }

    //Methode um alle Tabellen Einträge mit der übergebenen ID zu löschen
    public void deleteEntry(String id) {
        chatdatabase.delete(SQLiteHelper.TABLE_CHAT, SQLiteHelper.COLUMN_CHAT_ID + " = " + id, null);
    }

    //Methode um alle Eintrage aus der Tabelle Chat zu löschen
    public void deleteAllEntries(){
        chatdatabase.execSQL("DELETE FROM chatlist");
    }

    //Methode um ein Element aus der Datenbank zu öffnen auf dem der Cursor steht, der übergeben wird
    private chatDbEntry cursorToEntry(Cursor cursor) {
        chatDbEntry entry = new chatDbEntry();
        entry.setuniqueID(cursor.getLong(0));
        entry.setId(cursor.getLong(1));
        entry.setCHAT_SENDER_ID(cursor.getString(2));
        entry.setCHAT_RECIEVER_ID(cursor.getString(3));
        entry.setCHAT_MESSAGE(cursor.getString(4));
        entry.setCHAT_READ(cursor.getString(5));
        entry.setCHAT_DATE(cursor.getString(6));
        entry.setCHAT_ISSEND(cursor.getString(7));
        entry.setCHAT_AESKEY(cursor.getString(8));
        entry.setCHAT_SIGNATURE(cursor.getString(9));
        return entry;
    }
}