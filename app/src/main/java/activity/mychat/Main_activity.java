package activity.mychat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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
import crypto.Crypto;
import database.SQLiteHelper;
import database.chatEntryDataSource;
import database.userEntryDataSource;
import items.contactItem;
import items.contactListViewAdapter;


public class Main_activity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    public static SharedPreferences user;
    public static SharedPreferences.Editor editor;

    private boolean doubleBackToExitPressedOnce = false;
    ActionBarDrawerToggle drawerToggle;

    public static String userpassword;
    private String resp;

    public final SQLiteHelper dbHelper = new SQLiteHelper(this);
    public static SQLiteDatabase newDB;
    public static userEntryDataSource datasourceUser;
    public static chatEntryDataSource datasourceChat;

    private TextView showusername;
    private ListView chatListView;
    private EditText searchuser;

    private ArrayList<Long> userID = new ArrayList<Long>();
    private ArrayList<String> userName = new ArrayList<String>();
    private ArrayList<String> chatDate = new ArrayList<String>();
    private List<contactItem> contactItems;

    private NotificationManager mNotificationManager;

    //Broadcast um das ListView zu Aktualisieren wenn neuer User Hinzugefügt wurde
    private BroadcastReceiver receiveruser = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String resultCode = bundle.getString("RESULT");
                if (resultCode.equals("TRUE")) {
                    openAndQueryDatabase();
                    displayResultList();
                    chatListView.setClickable(true);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);
        editor = user.edit();
        userpassword = getIntent().getExtras().getString("userpassword");

        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        datasourceUser = new userEntryDataSource(this);
        datasourceUser.open();
        datasourceChat = new chatEntryDataSource(this);
        datasourceChat.open();

        showusername = (TextView)findViewById(R.id.txtshowusername);
        chatListView = (ListView)findViewById(R.id.userchatlist);
        searchuser = (EditText)findViewById(R.id.searchuser);
        searchuser.setMaxLines(1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        searchuser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                chatListView.removeAllViewsInLayout();
                userID.clear();
                userName.clear();
                chatDate.clear();
                try {

                    String[] name = new String[1];
                    name[0] = searchuser.getText().toString() + "%";

                    String selectSearch = "SELECT userlist.USER_ID,userlist.USER_NAME,chatlist.CHAT_ID,chatlist.CHAT_DATE " +
                            "FROM userlist LEFT JOIN chatlist " +
                            "ON userlist.USER_ID = chatlist.CHAT_ID " +
                            "WHERE userlist.USER_NAME LIKE ? " +
                            "ORDER BY chatlist.CHAT_DATE DESC";

                    Cursor c = newDB.rawQuery(selectSearch, name);

                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                Long tmpid = c.getLong(c.getColumnIndex("USER_ID"));
                                String tmpname = c.getString(c.getColumnIndex("USER_NAME"));
                                String tmpdate = c.getString(c.getColumnIndex("CHAT_DATE"));

                                if (tmpdate.equals("0")) {
                                    chatDate.add("No Messages");
                                } else {
                                    chatDate.add(tmpdate);
                                }

                                userID.add(tmpid);
                                userName.add(tmpname);

                            } while (c.moveToNext());
                        }
                        displayResultList();
                    }

                    c.close();
                } catch (SQLiteException se) {
                    Log.e(getClass().getSimpleName(), "Could not create or Open the database");
                }

                if (searchuser.getText().toString().equals("")) {
                    openAndQueryDatabase();
                    displayResultList();
                }
            }
        });

        chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                openChat(position);
            }
        });

    }

    private void openAndQueryDatabase(){
        try {

            chatListView.removeAllViewsInLayout();
            userID.clear();
            userName.clear();
            chatDate.clear();

            String selectQuery = "SELECT USER_ID,USER_NAME " +
                                 "FROM userlist " +
                                 "ORDER BY USER_NAME DESC";


            Cursor c = newDB.rawQuery(selectQuery, null);

            //"ON user.USER_ID = chat.CHAT_ID " +
            //"ORDER BY chat.CHAT_DATE DESC ", null);
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {

                        String[] data = new String[1];
                        data[0] = Long.toString(c.getLong(c.getColumnIndex("USER_ID")));

                        String selectchat = "SELECT CHAT_DATE,CHAT_ID " +
                                "FROM chatlist " +
                                "WHERE CHAT_ID = ? " +
                                "ORDER BY CHAT_DATE DESC LIMIT 1";

                        Cursor c2 = newDB.rawQuery(selectchat, data);
                        c2.moveToFirst();

                        Long tmpid = c.getLong(c.getColumnIndex("USER_ID"));
                        String tmpname  = c.getString(c.getColumnIndex("USER_NAME"));
                        String tmpdate = c2.getString(c2.getColumnIndex("CHAT_DATE"));

                            if(tmpdate.equals("0")){
                                chatDate.add("No Messages");
                            }else{
                                chatDate.add(tmpdate);
                            }

                        userID.add(tmpid);
                        userName.add(tmpname);


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

        contactItems = new ArrayList<contactItem>();
        contactItems.clear();

        if(userID.size() == 0){
            userID.add(0L);
            userName.add("No User available");
            chatDate.add("Please add user");
            chatListView.setClickable(false);
        }

        for (int i = 0; i < userID.size(); i++) {
            contactItem item = new contactItem(userID.get(i),userName.get(i), chatDate.get(i));
            contactItems.add(item);
        }
        contactListViewAdapter adapter1 = new contactListViewAdapter(this,R.layout.contact_item, contactItems);
        chatListView.setAdapter(adapter1);
    }

    private void searchforuser(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Search new User:");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT );
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

    private void openChat(int position){

        Long userid = userID.get(position);
        String username = userName.get(position);

        Intent intent = new Intent(this, Chat_activity.class);
        Bundle b = new Bundle();
        b.putLong("userid", userid);
        b.putString("username", username);
        intent.putExtras(b);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            //Clean Ram!!!!

            newDB.close();
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;

        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chats) {

        } else if (id == R.id.nav_new_cotact) {

            searchforuser();
        } else if (id == R.id.nav_newkey) {

            revokekey();
        } else if (id == R.id.nav_logout) {

            logout();
        }else if (id == R.id.nav_deleteacc) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

                new revokekey().execute(Crypto.hashpassword(input.getText().toString(), userpassword));
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

                editor.clear();
                editor.putBoolean("login", false);
                editor.commit();

                SQLiteHelper.cleanTable(newDB);

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
                new revokekey().execute(Crypto.hashpassword(input.getText().toString(), userpassword));
            }
        });

        builder.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor.clear();
                editor.commit();

                openlogin();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void checkifuserexists(String name){

        String[] data = new String[1];
        data[0] = name;

        String selectSearch = "SELECT userlist.USER_NAME " +
                "FROM userlist " +
                "WHERE userlist.USER_NAME = ? ";

        Cursor c = newDB.rawQuery(selectSearch, data);

        int count = c.getCount();
        if (count == 0) {

            new searchcontact().execute(name);
        }else{
            Toast.makeText(getApplicationContext(), "User already exist", Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    @Override
    protected void onResume() {

        showusername.setText(user.getString("USER_NAME", "Error loading Data"));

        if(isMyServiceRunning(Background_Service.class.getName())){
            stopService(new Intent(getBaseContext(), Background_Service.class));
        }

        Intent intent = new Intent(this, Background_Service.class);
        intent.putExtra("Time", 30000);
        startService(intent);

        new checkPublicKey().execute();

        if(!user.getBoolean("haskey",false)){
            differentkey();
        }

        mNotificationManager.cancel(0);

        registerReceiver(receiveruser, new IntentFilter(Background_Service.NOTIFICATION_USER));

        openAndQueryDatabase();
        displayResultList();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiveruser);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if(resultCode == Activity.RESULT_OK){
                String result=data.getStringExtra("result");

                if(result.equals("true")){

                    Toast.makeText(this, "New Key Saved", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "You need a Key", Toast.LENGTH_SHORT).show();
                }
            }else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }

        }
    }//onActivityResult

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
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", valueIWantToSend1));
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

                                editor.putString("RSA_PUBLIC_KEY", "");
                                editor.putString("RSA_PRIVATE_KEY", "");
                                editor.putBoolean("key", false);
                                editor.commit();
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
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/newcontact.php");

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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");
                    if (splitResult[0].equals("login_true")) {

                        if (!splitResult[1].equals("no_user")) {

                            try {
                                Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]),
                                        Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true","");

                                Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();

                            }finally {

                                chatListView.setClickable(true);
                                openAndQueryDatabase();
                                displayResultList();

                            }

                        }else{
                            searchforuser();
                            Toast.makeText(getApplicationContext(), "No User" , Toast.LENGTH_LONG).show();
                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class checkPublicKey extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub

            postData();
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData() {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/testkey.php");

            try {

                // Add your data
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

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        String publickeyphone = user.getString("RSA_PUBLIC_KEY", "");
                        String publickeyserver = splitResult[1];

                        if(publickeyserver.equals("---")){

                            createnewkey();

                        }else if(!publickeyphone.equals(publickeyserver)){
                            
                            if(publickeyserver.equals("-")){

                                differentkey();

                            }
                        }

                    }else {

                        openlogin();
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }
}
