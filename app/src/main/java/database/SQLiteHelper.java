package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class SQLiteHelper extends SQLiteOpenHelper {


    public static final String TABLE_CHAT = "chatlist";
    public static final String COLUMN_CHAT_ID = "CHAT_ID";
    public static final String COLUMN_CHAT_SENDER_ID = "CHAT_SENDER_ID";
    public static final String COLUMN_CHAT_RECIEVER_ID = "CHAT_RECIEVER_ID";
    public static final String COLUMN_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String COLUMN_CHAT_READ = "CHAT_READ";
    public static final String COLUMN_CHAT_DATE = "CHAT_DATE";
    public static final String COLUMN_CHAT_ISSEND = "CHAT_ISSEND";

    public static final String TABLE_USER = "userlist";
    public static final String COLUMN_USER_ID = "USERID";
    public static final String COLUMN_USERID = "USER_ID";
    public static final String COLUMN_USER_NAME = "USER_NAME";
    public static final String COLUMN_USER_PUBLICKEY = "USER_PUBLICKEY";


    public static final String DATABASE_NAME = "securechat.db";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE_CHAT = "create table "
            + TABLE_CHAT + " ( " +  COLUMN_CHAT_ID
            + " integer primary key, " + COLUMN_CHAT_SENDER_ID
            + " integer not null, " + COLUMN_CHAT_RECIEVER_ID
            + " integer not null, " + COLUMN_CHAT_MESSAGE
            + " text not null, " + COLUMN_CHAT_READ
            + " text not null, " + COLUMN_CHAT_DATE
            + " text not null, " + COLUMN_CHAT_ISSEND
            + " text not null);";

    private static final String DATABASE_CREATE_USER = "create table "
            + TABLE_USER + " ( " + COLUMN_USER_ID
            + " integer primary key autoincrement, " + COLUMN_USERID
            + " integer not null, " + COLUMN_USER_NAME
            + " text not null, " + COLUMN_USER_PUBLICKEY
            + " text not null);";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static void create(SQLiteDatabase database){
        new SQLiteHelper(null).onCreate(database);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_CHAT);
        database.execSQL(DATABASE_CREATE_USER);
    }

    public static void cleanChatTable(SQLiteDatabase database){
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        create(database);
    }

    public static void cleanUserTable(SQLiteDatabase database){
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        create(database);
    }

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