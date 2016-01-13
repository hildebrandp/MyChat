package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

//Klasse um ein Datenbank Element für die Tabelle User zu erstellen, auszulesen
//oder um eins zu löschen
public class userEntryDataSource {

    //Datenbank Feld
    private SQLiteDatabase userdatabase;
    private SQLiteHelper userdbHelper;

    //String in dem alle Spaltennamen enthalten sind
    private String[] allColumns = {SQLiteHelper.COLUMN_USER_ID, SQLiteHelper.COLUMN_USER_NAME, SQLiteHelper.COLUMN_USER_PUBLICKEY};

    //Methode um Datenbank Feld zu initialisieren
    public userEntryDataSource(Context context) {
        userdbHelper = new SQLiteHelper(context);
    }

    //Methode um Datenbank mit schreibzugriff zu öffenen
    public void open() throws SQLException {
        userdatabase = userdbHelper.getWritableDatabase();
    }

    //Methode um Datenbank zu schließen
    public void close() {
        userdbHelper.close();
    }

    //Methode um einen neuen Eintrag in die Tabelle User zu machen
    public userDbEntry createUserEntry(String USER_ID, String USER_NAME,String USER_PUBLICKEY) {
        //Erstelle ein Element mit den Übergebenen Daten
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_USER_ID, USER_ID);
        values.put(SQLiteHelper.COLUMN_USER_NAME, USER_NAME);
        values.put(SQLiteHelper.COLUMN_USER_PUBLICKEY, USER_PUBLICKEY);

        //Füge die Daten in die Tabelle Chat ein
        userdatabase.insert(SQLiteHelper.TABLE_USER, null, values);
        Cursor cursor = userdatabase.query(SQLiteHelper.TABLE_USER, allColumns, null, null, null, null, null);
        cursor.moveToFirst();
        userDbEntry newDbEntry = cursorToEntry(cursor);
        cursor.close();
        return newDbEntry;
    }

    //Methode um einen Eintrag mit der übergebenen ID zu löschen
    public void deleteEntry(String id) {
        userdatabase.delete(SQLiteHelper.TABLE_USER, SQLiteHelper.COLUMN_USER_ID + " = " + id, null);
    }

    //Methode um alle einträge in der Tabelle User zu löschen
    public void deleteAllEntries(){
        userdatabase.execSQL("DELETE FROM userlist");
    }

    //Methode um ein Element aus der Datenbank zu öffnen auf dem der Cursor steht, der übergeben wird
    private userDbEntry cursorToEntry(Cursor cursor) {
        userDbEntry entry = new userDbEntry();
        entry.setUSER_ID(cursor.getString(0));
        entry.setUSER_NAME(cursor.getString(1));
        entry.setUSER_PUBLICKEY(cursor.getString(2));
        return entry;
    }
}
