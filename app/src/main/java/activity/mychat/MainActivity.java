package activity.mychat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.WindowManager;
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


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    public static SharedPreferences user;
    public static SharedPreferences.Editor editor;

    private boolean doubleBackToExitPressedOnce = false;
    ActionBarDrawerToggle drawerToggle;

    public static String userpassword;
    public static String userpasswordhash;
    private String resp;

    public final SQLiteHelper dbHelper = new SQLiteHelper(this);
    public SQLiteDatabase newDB;
    private String usertableName = SQLiteHelper.TABLE_USER;
    public userEntryDataSource datasourceUser;
    private String chattableName = SQLiteHelper.TABLE_CHAT;
    public chatEntryDataSource datasourceChat;

    private TextView showusername;
    private ListView chatListView;
    private EditText searchuser;

    private ArrayList<Long> userID = new ArrayList<Long>();
    private ArrayList<String> userName = new ArrayList<String>();
    private ArrayList<String> chatDate = new ArrayList<String>();
    private List<contactItem> contactItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);
        editor = user.edit();
        userpassword = getIntent().getExtras().getString("userpassword");
        userpasswordhash = getIntent().getExtras().getString("userpasswordhash");

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

        //Fehler finden warum datenbank nicht existiert
        //datasourceUser.createUserEntry("1", "Name", "KEY");
        //datasourceChat.createChatEntry(user.getString("USER_ID", "0"), "1", "Hallo", "true", "No Messages","true");


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

                    String name = searchuser.getText().toString() + "%";

                    String selectSearch = "SELECT userlist.USER_ID,userlist.USER_NAME,chatlist.CHAT_ID,chatlist.CHAT_DATE " +
                            "FROM userlist LEFT JOIN chatlist " +
                            "ON userlist.USER_ID = chatlist.CHAT_ID " +
                            "WHERE userlist.USER_NAME = " + name +
                            "ORDER BY chatlist.CHAT_DATE DESC";

                    Cursor c = newDB.rawQuery(selectSearch, null);

                    if (c != null ) {
                        if  (c.moveToFirst()) {
                            do {
                                Long ID = c.getLong(c.getColumnIndex("USER_ID"));
                                String Name  = c.getString(c.getColumnIndex("USER_NAME"));
                                String Date = c.getString(c.getColumnIndex("CHAT_DATE"));
                                userID.add(ID);
                                userName.add(Name);
                                chatDate.add(Date);

                            }while (c.moveToNext());
                        }
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

        onAppStart();

    }

    private void openAndQueryDatabase(){
        try {

            chatListView.removeAllViewsInLayout();
            userID.clear();
            userName.clear();
            chatDate.clear();

            String selectQuery = "SELECT userlist.USER_ID, userlist.USER_NAME, chatlist.CHAT_ID, chatlist.CHAT_DATE " +
                                 "FROM userlist LEFT JOIN chatlist " +
                                 "ON USER_ID = CHAT_ID " +
                                 "ORDER BY chat.CHAT_DATE DESC";

            Cursor c = newDB.rawQuery(selectQuery, null);

            //"ON user.USER_ID = chat.CHAT_ID " +
            //"ORDER BY chat.CHAT_DATE DESC ", null);
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {
                        Long ID = c.getLong(c.getColumnIndex("USER_ID"));
                        String Name  = c.getString(c.getColumnIndex("USER_NAME"));
                        String Date = c.getString(c.getColumnIndex("CHAT_DATE"));
                        userID.add(ID);
                        userName.add(Name);
                        chatDate.add(Date);

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

    private void onAppStart(){

        showusername.setText(user.getString("USER_NAME", "Error loading Data"));
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

                editor.clear();
                editor.commit();
                editor.putBoolean("login", false);
                SQLiteHelper.cleanUserTable(newDB);
                SQLiteHelper.cleanChatTable(newDB);

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

        Intent i = new Intent(this, login.class);
        startActivity(i);
        finish();
    }

    private void createnewkey(){
        Intent i = new Intent(this, newKey.class);
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
                editor.clear();
                editor.commit();

                openlogin();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void checkifuserexists(String name){

        String query = "SELECT USER_NAME " +
                        "FROM userlist " +
                        "WHERE USER_NAME = " + name;
        Cursor c = newDB.rawQuery(query, null);

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

        if(!user.getBoolean("key",false)){
            createnewkey();
        }else{
            //new checkPublicKey().execute();
        }

        openAndQueryDatabase();
        displayResultList();

        super.onResume();
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
                nameValuePairs.add(new BasicNameValuePair("userpassword", userpasswordhash));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(valueIWantToSend1, userpassword)));
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
                nameValuePairs.add(new BasicNameValuePair("userpassword", userpasswordhash));
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
                                datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                datasourceChat.createChatEntry(user.getString("USER_ID", "0"), splitResult[1], "", "true", "No Messages","true");
                                Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();

                            }finally {

                            }

                        }else{
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

                String key = user.getString("RSA_PUBLIC_KEY", "");
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", userpasswordhash));
                nameValuePairs.add(new BasicNameValuePair("userkey", key));
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

                        if(splitResult[1].equals("key_false")) {

                            differentkey();

                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }
}
