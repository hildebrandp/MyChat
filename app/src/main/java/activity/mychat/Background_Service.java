package activity.mychat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import database.SQLiteHelper;
import database.chatEntryDataSource;
import database.userEntryDataSource;

/**
 * Klasse die nach dem Start der App im Hintergrund weiter läuft und in 10 Sekunden Intervall
 * abfragt ob neue Nachrichten vorhanden sind
 */
public class Background_Service extends Service {


    //Variable für die Shared Preferences
    public static SharedPreferences user;

    //Variablen für den Timer der im Hintergrund läuft
    private Timer mTimer1 = null;
    private Handler mHandler = new Handler();
    private String resp;

    //Variablen für den Datenbank zugriff
    public final SQLiteHelper dbHelper = new SQLiteHelper(this);
    public static SQLiteDatabase newDB;
    public static chatEntryDataSource datasourceChat;
    public static userEntryDataSource datasourceUser;

    //Variablen für Notifications
    public static final String NOTIFICATION_CHAT = "NEW_MESSAGE";
    public static final String NOTIFICATION_USER = "NEW_USER";
    private final IBinder mBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Die Shared Preferences öffnen
        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);

        //Die Datenbank mit Schreibzugriff öffnen
        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();
        datasourceChat = new chatEntryDataSource(this);
        datasourceChat.open();
        datasourceUser = new userEntryDataSource(this);
        datasourceUser.open();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Start des Timers mit dem Intervall von 10 Sekunden
        startTimer(10000);

        return START_STICKY;
    }

    //Timer Methode die falls der Timer bereits läuft in Stopt und dann wieder startet
    public void startTimer(int time){
        //Timer initialize
        if(mTimer1 != null) {

            mTimer1.cancel();
            mTimer1 = new Timer();
        } else {

            mTimer1 = new Timer();
        }

        mTimer1.scheduleAtFixedRate(new readmessages(), 1000, time);
    }

    //Timer Klasse die im Intervall von 10 Sekunden aufgerufen wierd
    class readmessages extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {

                    //Starte Asynchronen Task der den Server abfragt ob neue Nachrichten vorhanden sind
                    new recieveMessage().execute();
                }

            });
        }
    }

    //Binder Service, damit man sich mit diesem Service verbinden kann
    public class MyBinder extends Binder {
        Background_Service getService() {
            return Background_Service.this;
        }
    }

    //Zeige eine Notification falls eine neue Nachricht eintrifft
    protected void displayNotification() {
        Intent intent = new Intent(getApplicationContext(), Login_activity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationManager mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Secure Chat")
                .setContentTitle("Secure Chat")
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("New Message"))
                .setContentIntent(pendingIntent);

        mNotificationManager.notify(0, mBuilder.build());

    }

    //Sende einen Broadcast an die MainActivity und ChatActivity
    private void publishResults(String result) {
        Intent intent = new Intent(NOTIFICATION_CHAT);
        intent.putExtra("RESULT", result);
        sendBroadcast(intent);
    }

    //Sende einen Broadcast, wenn eine Nachricht von einem Unbekannten Empfänger kommt
    private void publishNewUser() {
        Intent intent = new Intent(NOTIFICATION_USER);
        intent.putExtra("RESULT", "TRUE");
        sendBroadcast(intent);
    }

    //Stope den Timer, wenn der Service gestoppt wird
    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer1.cancel();
    }

    //Asynchroner Task fürs Abrufen der neuen Nachrichten
    private class recieveMessage extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub

            postData();
            return null;
        }

        protected void onPostExecute(Double result){

        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData() {

            //Erstelle einen Httpclient mit der URL des Servers
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/readmessages.php");

            try {

                //Füge die zu Sendenden Daten hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe den Httppost aus
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append((line));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                resp = sb.toString();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }

                    //Trenne den Empfangenen String bei >>
                    String[] splitResult = String.valueOf(resp).split(">>");

                    if (splitResult[0].equals("login_false")) {

                        //Login Fehlgeschlafen
                    } else if (splitResult[0].equals("nomessages")) {

                        //Keine Nachrichten vorhanden
                    } else if(splitResult[0].equals("newmessage")){

                        //Neue Nachricht empfangen
                        for(int i=1;i < splitResult.length ;i++){

                            displayNotification();

                            //Trenne die Nachrichten bei ::
                            String[] splitResult2 = String.valueOf(splitResult[i]).split("::");

                            Long sender = Long.parseLong(splitResult2[0]);
                            String reciever = user.getString("USER_ID", "");

                                String[] data = new String[1];
                                data[0] = splitResult2[0];

                                //Überprüfe ob der Sender in der eigenen Datenbank vorhanden ist
                                String selectSearch = "SELECT userlist.USER_ID " +
                                        "FROM userlist " +
                                        "WHERE userlist.USER_ID = ? ";

                                Cursor c = newDB.rawQuery(selectSearch, data);

                                //Falls der Sender nicht in der Datenbank ist lade die Daten des Absenders vom Server herunter
                                int count = c.getCount();
                                if (count == 0) {

                                    new addcontact().execute(data[0]);
                                }

                            //Speichere die empfangene Nachricht in der Tabelle für Nachrichten
                            datasourceChat.createChatEntry(sender, splitResult2[0], reciever, splitResult2[2] , "false", splitResult2[1], "true", splitResult2[3], splitResult2[4]);

                            //Schließe den Datenbank Cursor
                            c.close();
                            //Sende Broadcast
                            publishResults(splitResult2[0]);

                        }
                    }
        }
    }

    //Asynchroner Task für das Empfangen der Daten des Absenders
    private class addcontact extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1) {

            //Erstelle Http Client mit URL
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/addcontact.php");

            try {
                //Füge Daten zu dem Http Client hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME","")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("usercontact", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Führe Http post aus
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append((line));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                resp = sb.toString();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }

                    //Trenne den Empfangenen String bei ::
                    String[] splitResult = String.valueOf(resp).split("::");
                    if (splitResult[0].equals("login_true")) {

                        try {

                            //Speichere Kontakt Daten in der Datenbank für die KOntakte
                            Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);

                        } finally {

                            //Sende Broadcast da neuer Nutzer in der Liste ist
                            publishNewUser();
                        }
                    }
        }

    }
}
