package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class SQLiteHelper extends SQLiteOpenHelper {

    //String Textfelder mit dem Tabellennamen und den Namen der Spalter für die Tabelle Chat
    public static final String TABLE_CHAT = "chatlist";
    public static final String COLUMN_CHAT_UNIQUE_ID = "CHAT_UNIQUE_ID";
    public static final String COLUMN_CHAT_ID = "CHAT_ID";
    public static final String COLUMN_CHAT_SENDER_ID = "CHAT_SENDER_ID";
    public static final String COLUMN_CHAT_RECIEVER_ID = "CHAT_RECIEVER_ID";
    public static final String COLUMN_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String COLUMN_CHAT_READ = "CHAT_READ";
    public static final String COLUMN_CHAT_DATE = "CHAT_DATE";
    public static final String COLUMN_CHAT_ISSEND = "CHAT_ISSEND";
    public static final String COLUMN_CHAT_AESKEY = "CHAT_AESKEY";
    public static final String COLUMN_CHAT_SIGNATURE = "CHAT_SIGNATURE";

    //String Textfelder mit dem Tabellennamen und den Namen der Spalter für die Tabelle User
    public static final String TABLE_USER = "userlist";
    public static final String COLUMN_USER_ID = "USER_ID";
    public static final String COLUMN_USER_NAME = "USER_NAME";
    public static final String COLUMN_USER_PUBLICKEY = "USER_PUBLICKEY";

    //Datenbankname und Versionsnummer
    public static final String DATABASE_NAME = "securechat.db";
    private static final int DATABASE_VERSION = 2;

    //String mit Befehl um Tabelle Chat zu erstellen
    private static final String DATABASE_CREATE_CHAT = "create table "
            + TABLE_CHAT + " ( " +  COLUMN_CHAT_UNIQUE_ID
            + " integer primary key autoincrement, " + COLUMN_CHAT_ID
            + " integer      , " + COLUMN_CHAT_SENDER_ID
            + " text not null, " + COLUMN_CHAT_RECIEVER_ID
            + " text not null, " + COLUMN_CHAT_MESSAGE
            + " VARCHAR(4096), " + COLUMN_CHAT_SIGNATURE
            + " VARCHAR(4096), " + COLUMN_CHAT_READ
            + " text not null, " + COLUMN_CHAT_DATE
            + " text not null, " + COLUMN_CHAT_AESKEY
            + " VARCHAR(4096),  " + COLUMN_CHAT_ISSEND
            + " text not null);";

    //String mit Befehl um Tabelle User zu erstellen
    private static final String DATABASE_CREATE_USER = "create table "
            + TABLE_USER + " ( " + COLUMN_USER_ID
            + " text primary key, " + COLUMN_USER_NAME
            + " VARCHAR(40), " + COLUMN_USER_PUBLICKEY
            + " VARCHAR(2048));";


    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Methode um Datenbank zu erstellen
    private static void create(SQLiteDatabase database){
        new SQLiteHelper(null).onCreate(database);
    }

    //OnCreate Methode die die beiden Tabellen die vorher über die Strings definiert wurden
    //zu erstellen
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_CHAT);
        database.execSQL(DATABASE_CREATE_USER);
    }

    //Methode um Tabelle User zu löschen und neu zu erstellen
    public static void cleanTableUser(SQLiteDatabase database){
        database.execSQL("DROP TABLE " + TABLE_USER);
        database.execSQL(DATABASE_CREATE_USER);
    }

    //Methode um Tabelle Chat zu löschen und neu zu erstellen
    public static void cleanTableChat(SQLiteDatabase database){
        database.execSQL("DROP TABLE " + TABLE_CHAT);
        database.execSQL(DATABASE_CREATE_CHAT);
    }

    //Upgrade Methode um die Datenbank Version zu updaten
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }
}