package activity.mychat;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import crypto.AESHelper;
import crypto.Crypto;
import crypto.RSA;
import crypto.SignatureUtils;
import database.SQLiteHelper;
import items.Message;
import items.MessagesListAdapter;


public class Chat_activity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    ActionBarDrawerToggle drawerToggle;

    private ArrayList<String> chatMessage = new ArrayList<String>();
    private ArrayList<String> chatDate = new ArrayList<String>();
    private ArrayList<String> chatVerified = new ArrayList<String>();
    private ArrayList<String> chatSender = new ArrayList<String>();
    private ArrayList<String> chatAESKey = new ArrayList<String>();
    private ArrayList<String> chatSignature = new ArrayList<String>();

    private List<Message> messageItems;
    private MessagesListAdapter mAdapter;

    private ListView chatlist;
    private EditText texttosend;
    private Button btnsend;
    private ListView chatListView;

    private String resp;
    private Long userid;
    private String username;

    private NotificationManager mNotificationManager;
    private static char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();

    public static SQLiteDatabase newDB;

    //Broadcast um das ListView zu Aktualisieren wenn Nachricht an diesen Emfänger empfangen wurde
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String resultCode = bundle.getString("RESULT");
                if (resultCode.equals(Long.toString(userid))) {
                    mNotificationManager.cancel(0);
                    openAndQueryDatabase();
                    displayResultList();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        chatlist = (ListView)findViewById(R.id.chatlistView);
        texttosend = (EditText)findViewById(R.id.texttosend);
        btnsend = (Button)findViewById(R.id.btnsend);
        chatListView = (ListView)findViewById(R.id.chatlistView);

        userid = getIntent().getExtras().getLong("userid");
        username = getIntent().getExtras().getString("username");

        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar1);
        getSupportActionBar().setTitle(username);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar1, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!texttosend.getText().toString().equals("")) {
                    encryptMessage(texttosend.getText().toString());
                }
            }
        });


        Bundle b = getIntent().getExtras();

            if(b != null){
                userid = b.getLong("userid");
            }else{
                userid = 0L;
            }

        if(isMyServiceRunning(Background_Service.class.getName())){

            stopService(new Intent(getBaseContext(), Background_Service.class));
        }

        Intent intent = new Intent(this, Background_Service.class);
        intent.putExtra("Time", 10000);
        startService(intent);

        openAndQueryDatabase();
        displayResultList();
    }

    private boolean isMyServiceRunning(String className) {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void openAndQueryDatabase(){
        try {

            chatListView.removeAllViewsInLayout();
            chatMessage.clear();
            chatVerified.clear();
            chatDate.clear();
            chatSender.clear();
            chatAESKey.clear();
            chatSignature.clear();

            String[] data = new String[1];
            data[0] = Long.toString(userid);

            String selectSearch = "SELECT * " +
                    "FROM chatlist " +
                    "WHERE CHAT_ID = ? " +
                    "ORDER BY CHAT_DATE ASC";

            Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

            //"ON user.USER_ID = chat.CHAT_ID " +
            //"ORDER BY chat.CHAT_DATE DESC ", null);
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {
                        String tmpchatMessage = c.getString(c.getColumnIndex("CHAT_MESSAGE"));

                            if(tmpchatMessage.length() > 0){

                                String tmpchatDate  = c.getString(c.getColumnIndex("CHAT_DATE"));
                                String tmpchatsender = c.getString(c.getColumnIndex("CHAT_SENDER_ID"));
                                String tmpchatAESKey = c.getString(c.getColumnIndex("CHAT_AESKEY"));
                                String tmpSignature = c.getString(c.getColumnIndex("CHAT_SIGNATURE"));

                                chatDate.add(tmpchatDate);
                                chatMessage.add(tmpchatMessage);
                                chatVerified.add("false");
                                chatSender.add(tmpchatsender);
                                chatAESKey.add(tmpchatAESKey);
                                chatSignature.add(tmpSignature);
                            }

                    }while (c.moveToNext());
                }
            }

            c.close();
        } catch (SQLiteException se ) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } finally {

            try {
                chatListView.removeAllViewsInLayout();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private void displayResultList() {

        messageItems = new ArrayList<Message>();
        messageItems.clear();
        String tmpmessage;
        String tmpSignature="false";

        for (int i = 0; i < chatMessage.size(); i++) {

            if(i == 0){

                tmpmessage = "Chat Created";
            }else {

                tmpmessage = decryptMessage(chatMessage.get(i), i);
                tmpSignature = ""+checkSignature(chatSignature.get(i), tmpmessage, chatSender.get(i));

                if(tmpmessage.length() == 0){

                    tmpmessage = "System:Can´t Decrypt";
                }
            }

            Message item = new Message(chatDate.get(i), tmpmessage, tmpSignature, chatSender.get(i));
            messageItems.add(item);
        }

        mAdapter = new MessagesListAdapter(this, messageItems);
        chatListView.setAdapter(mAdapter);
    }

    private Boolean checkSignature(String signature, String message, String id){

        boolean isValid;
        String pubkey = Main_activity.user.getString("RSA_PUBLIC_KEY", "");

            if(id != pubkey){

                pubkey = getPublicKey();
            }

        isValid = SignatureUtils.checkSignature(signature, message, pubkey);

        return isValid;
    }

    private String decryptMessage(String message,int pos){
        String decryptedMessage = "";

            String decryptedKey = RSA.decryptWithStoredKey(chatAESKey.get(pos));

            try {
                decryptedMessage = AESHelper.decrypt(decryptedKey, message);

            }catch (Exception e){
                e.printStackTrace();
            }

        return decryptedMessage;
    }

    private void encryptMessage(String message){

            String key = getPublicKey();
            String rand = random();

            String tmpmessage=null;
            String encryptedkey;
            String privateencrypt;
            String encryptedmessage=null;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateandTime = sdf.format(Calendar.getInstance().getTime());

                try {

                    encryptedmessage = AESHelper.encrypt(rand, message);
                }catch (Exception e) {
                    e.printStackTrace();
                }

            encryptedkey = RSA.encryptWithKey(key, rand);
            privateencrypt = RSA.encryptWithStoredKey(rand);

            String signature = SignatureUtils.genSignature(message);



            new sendMessage().execute(encryptedmessage, currentDateandTime, encryptedkey, signature);

            Main_activity.datasourceChat.createChatEntry(userid, Main_activity.user.getString("USER_ID", "0"), Long.toString(userid), tmpmessage, "true", currentDateandTime, "true", privateencrypt, signature);

            texttosend.setText("");

            openAndQueryDatabase();
            displayResultList();

    }

    private String getPublicKey(){

        String[] data = new String[1];
        data[0] = Long.toString(userid);

        String selectKey = "SELECT userlist.USER_PUBLICKEY " +
                "FROM userlist " +
                "WHERE userlist.USER_ID = ? ";

        Cursor c = Main_activity.newDB.rawQuery(selectKey, data);
        String tmpkey = "---";

        if (c != null ) {
            if (c.moveToFirst()) {
                tmpkey = c.getString(c.getColumnIndex("USER_PUBLICKEY"));

            }
        }

        if(tmpkey.length() < 4){
            noKey();
            return "---";
        }else {
            return tmpkey;
        }
    }

    private void noKey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("User has no Key\nCan´t send Message");

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        builder.setCancelable(false);
        builder.show();
    }

    public static String random() {
        SecureRandom srand = new SecureRandom();
        Random rand = new Random();
        char[] buff = new char[128];

        for (int i = 0; i < 128; ++i) {
            // reseed rand once you've used up all available entropy bits
            if ((i % 10) == 0) {
                rand.setSeed(srand.nextLong()); // 64 bits of random!
            }
            buff[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(buff);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chats) {

            finish();
        } else if (id == R.id.nav_new_cotact) {

            searchforuser();
        } else if (id == R.id.nav_newkey) {

            revokekey();
        } else if (id == R.id.nav_logout) {

            logout();
        }else if (id == R.id.nav_deleteacc) {

            deleteAccount();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void revokekey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Enter your Revoke Key:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new revokekey().execute(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void logout(){

        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);

        msgBox.setTitle("Secure Chat");
        msgBox.setMessage("Logout will Delete all your Data");
        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Main_activity.editor.clear();
                Main_activity.editor.commit();

                SQLiteHelper.cleanTableChat(newDB);
                SQLiteHelper.cleanTableUser(newDB);

                openlogin();
            }
        });

        msgBox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        msgBox.setCancelable(false);
        // create alert dialog
        AlertDialog alertDialog = msgBox.create();
        // show it
        alertDialog.show();
    }

    private void openlogin(){

        Intent i = new Intent(this, Login_activity.class);
        startActivity(i);
        finish();
    }

    private void searchforuser(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Search new User:");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().equals("")) {
                    checkifuserexists(input.getText().toString());

                } else {
                    Toast.makeText(getApplicationContext(), "No empty Username", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void checkifuserexists(String name){

        String[] data = new String[1];
        data[0] = name;

        String selectSearch = "SELECT userlist.USER_NAME " +
                "FROM userlist " +
                "WHERE userlist.USER_NAME = ? ";

        Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

        int count = c.getCount();
        if (count == 0) {

            new searchcontact().execute(name,"false");
        }else{
            Toast.makeText(getApplicationContext(), "User already exist", Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    private void createnewkey(){
        Intent i = new Intent(this, NewKey_activity.class);
        startActivityForResult(i, 1);
    }

    private void differentkey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Your Key is Wrong!\nPlease enter Revoke Key:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new revokekey().execute(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main_activity.editor.clear();
                Main_activity.editor.commit();

                SQLiteHelper.cleanTableChat(newDB);
                SQLiteHelper.cleanTableUser(newDB);

                openlogin();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void deleteAccount(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Delete your Account?\nPlease enter Revoke Key:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new deleteAccount().execute(Crypto.hashpassword(input.getText().toString(), Main_activity.userpassword));
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new searchcontact().execute(username, "true");

        registerReceiver(receiver, new IntentFilter(Background_Service.NOTIFICATION_CHAT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private class revokekey extends AsyncTask<String, Integer, Double> {

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
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/revokekey.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(valueIWantToSend1, Main_activity.userpassword)));
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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();
                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("revokekey_true")) {

                            Toast.makeText(getApplicationContext(), "Revoke Key correct", Toast.LENGTH_LONG).show();

                            if(splitResult[2].equals("delete_true")) {

                                Main_activity.editor.putString("RSA_PUBLIC_KEY", "");
                                Main_activity.editor.putString("RSA_PRIVATE_KEY", "");
                                Main_activity.editor.putBoolean("key", false);
                                Main_activity.editor.commit();
                                createnewkey();
                            }else {
                                Toast.makeText(getApplicationContext(), "Error Please try again", Toast.LENGTH_LONG).show();
                            }

                        }else{

                            Toast.makeText(getApplicationContext(), "Revoke Key false", Toast.LENGTH_LONG).show();
                            differentkey();
                        }


                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class searchcontact extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0], params[1]);
            return null;
        }

        protected void onPostExecute(Double result) {
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        public void postData(String valueIWantToSend1, final String checkkey) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/newcontact.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");
                    if (splitResult[0].equals("login_true")) {

                        if (!splitResult[1].equals("no_user")) {

                            if (checkkey.equals("false")) {
                                try {

                                    Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                    Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]),Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true", "","");

                                } finally {

                                    Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();
                                }

                            } else if (checkkey.equals("true")){

                                String[] data = new String[1];
                                data[0] = Long.toString(userid);

                                String selectKey = "SELECT userlist.USER_PUBLICKEY " +
                                        "FROM userlist " +
                                        "WHERE userlist.USER_ID = ? ";

                                Cursor c = Main_activity.newDB.rawQuery(selectKey, data);
                                String tmpkey = "---";

                                if (c != null ) {
                                    if (c.moveToFirst()) {
                                        tmpkey = c.getString(c.getColumnIndex("USER_PUBLICKEY"));

                                    }
                                }

                                if(!tmpkey.equals(splitResult[3])){
                                    Main_activity.datasourceChat.deleteEntry(splitResult[1]);

                                    Main_activity.datasourceUser.deleteEntry(splitResult[1]);

                                    Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                    Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]), Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true", "","");

                                    Toast.makeText(getApplicationContext(), "User has changed his Public Key", Toast.LENGTH_LONG).show();
                                }else if(splitResult[3].equals("-") || splitResult[3].equals("---")){

                                    Toast.makeText(getApplicationContext(), "User has no Public Key\nPlease try again later!", Toast.LENGTH_LONG).show();
                                    btnsend.setClickable(false);
                                    texttosend.setText("Can´t send Message!");
                                    texttosend.setClickable(false);

                                }

                            }

                        } else {
                            searchforuser();
                            Toast.makeText(getApplicationContext(), "No User", Toast.LENGTH_LONG).show();
                        }

                    } else {

                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class sendMessage extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub

            postData(params[0],params[1],params[2],params[3]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(final String message,String date,String key, String signature) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/chatmessage.php");

            try {

                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("message", message));
                nameValuePairs.add(new BasicNameValuePair("encryptedkey", key));
                nameValuePairs.add(new BasicNameValuePair("signature", signature));
                nameValuePairs.add(new BasicNameValuePair("date", date));
                nameValuePairs.add(new BasicNameValuePair("receiverid", Long.toString(userid)));
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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        openlogin();
                        finish();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("message_send")){

                            //Toast.makeText(getApplicationContext(), "Message Send" , Toast.LENGTH_LONG).show();
                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class deleteAccount extends AsyncTask<String, Integer, Double> {

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

        public void postData(String revokekey) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/deleteAccount.php");

            try {

                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userid", Main_activity.user.getString("USER_ID", "")));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", revokekey));
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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        Toast.makeText(getApplicationContext(), "Delete Error" , Toast.LENGTH_LONG).show();

                    }else if(splitResult[0].equals("login_true")){

                        Main_activity.editor.clear();
                        Main_activity.editor.commit();

                        SQLiteHelper.cleanTableChat(newDB);
                        SQLiteHelper.cleanTableUser(newDB);

                        openlogin();

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }
}
