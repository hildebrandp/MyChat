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
 * Created by Pascal on 12.11.2015.
 */
public class Background_Service extends Service {

    public static SharedPreferences user;

    private Timer mTimer1 = null;
    private Handler mHandler = new Handler();
    private String resp;

    public final SQLiteHelper dbHelper = new SQLiteHelper(this);
    public static SQLiteDatabase newDB;
    public static chatEntryDataSource datasourceChat;
    public static userEntryDataSource datasourceUser;

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

        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);

        //Open Database
        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();
        datasourceChat = new chatEntryDataSource(this);
        datasourceChat.open();
        datasourceUser = new userEntryDataSource(this);
        datasourceUser.open();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Start Timer with Period time 10 Seconds
        startTimer(10000);

        return START_STICKY;
    }

    //Start Timer and if it runs restart it
    public void startTimer(int time){
        //Timer initialize
        if(mTimer1 != null) {
            mTimer1.cancel();
            mTimer1 = new Timer();
        } else {
            // recreate new
            mTimer1 = new Timer();
        }

        //Start Timer in 1 sec and with a period time 10 seconds
        mTimer1.scheduleAtFixedRate(new readmessages(), 1000, time);
    }

    //Timer Class which starts the Async Task
    class readmessages extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {

                    new recieveMessage().execute();
                }

            });
        }
    }

    //Binder Service
    public class MyBinder extends Binder {
        Background_Service getService() {
            return Background_Service.this;
        }
    }

    //Display a Notification when a new Message arrives
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

    //Send Broadcast to Main Activity and Chat Activity when a new Message recieved
    private void publishResults(String result) {
        Intent intent = new Intent(NOTIFICATION_CHAT);
        intent.putExtra("RESULT", result);
        sendBroadcast(intent);
    }

    //Send Broadcast to Main Activity and Chat Activity when a Message from a new User recived
    private void publishNewUser() {
        Intent intent = new Intent(NOTIFICATION_USER);
        intent.putExtra("RESULT", "TRUE");
        sendBroadcast(intent);
    }

    //Stop timer when Activity will be destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer1.cancel();
    }

    //Asynchrone Task for recieving Messages
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


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/readmessages.php");

            try {

                // Add Data which will be send to the Server
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
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

                    //Separate the message at ">>"
                    String[] splitResult = String.valueOf(resp).split(">>");

                    if (splitResult[0].equals("login_false")) {

                        //No Login
                    } else if (splitResult[0].equals("nomessages")) {

                        //No Messages
                    } else if(splitResult[0].equals("newmessage")){

                        for(int i=1;i < splitResult.length ;i++){

                            displayNotification();
                            String[] splitResult2 = String.valueOf(splitResult[i]).split("::");

                            Long sender = Long.parseLong(splitResult2[0]);
                            String reciever = user.getString("USER_ID", "");

                                String[] data = new String[1];
                                data[0] = splitResult2[0];

                                String selectSearch = "SELECT userlist.USER_ID " +
                                        "FROM userlist " +
                                        "WHERE userlist.USER_ID = ? ";

                                Cursor c = newDB.rawQuery(selectSearch, data);

                                int count = c.getCount();
                                if (count == 0) {

                                    new addcontact().execute(data[0]);
                                }

                            datasourceChat.createChatEntry(sender, splitResult2[0], reciever, splitResult2[2] , "false", splitResult2[1], "true", splitResult2[3], splitResult2[4]);


                            c.close();
                            publishResults(splitResult2[0]);

                        }

                    }



        }

    }

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


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/addcontact.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME","")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("usercontact", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
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

                    String[] splitResult = String.valueOf(resp).split("::");
                    if (splitResult[0].equals("login_true")) {

                            try {

                                Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]), Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true", "","");

                            }finally {

                                publishNewUser();
                            }



                    }


        }

    }



}
