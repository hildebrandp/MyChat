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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
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


public class Main_activity extends AppCompatActivity {

    //Felder für die SharedPreferences
    public static SharedPreferences user;
    public static SharedPreferences.Editor editor;

    //Boolean Feld zum überprüfen wie oft man auf den Zurück Button drückt
    private boolean doubleBackToExitPressedOnce = false;

    //String für das User passwort
    public static String userpassword;

    //String für die Antwort vom Server
    private String resp;

    //Felder für den Datenbank zugriff
    public static SQLiteDatabase newDB;
    public static userEntryDataSource datasourceUser;
    public static chatEntryDataSource datasourceChat;

    //Textfelder
    private TextView showusername;
    private ListView chatListView;
    private EditText searchuser;

    //Arrays um die Daten der Kontakte zu Speichern
    private ArrayList<Long> userID = new ArrayList<Long>();
    private ArrayList<String> userName = new ArrayList<String>();
    private ArrayList<String> chatMessages = new ArrayList<String>();
    private List<contactItem> contactItems;

    //Feld für Notifications
    private NotificationManager mNotificationManager;

    //Broadcast Receiver um das ListView zu Aktualisieren wenn neuer User Hinzugefügt wurde
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

    //Broadcast Receiver um das ListView zu Aktualisieren wenn Nachricht
    //an diesen Emfänger empfangen wurde
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {

                    mNotificationManager.cancel(0);
                    openAndQueryDatabase();
                    displayResultList();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        //Zeige Activity ohne Tastatur
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Initialisiere Notification Manager
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        //Öffne die Shared Preferences
        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);
        editor = user.edit();

        //Titel setzten mit eignem Nutzernamen
        getSupportActionBar().setTitle(user.getString("USER_NAME", "My Chat"));

        //Speichere das Nutzer Passwort ab
        userpassword = getIntent().getExtras().getString("userpassword");

        //Öffnen der Datenbank
        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        //Öffnen der Tabellen aus der Datenbank
        datasourceUser = new userEntryDataSource(this);
        datasourceUser.open();
        datasourceChat = new chatEntryDataSource(this);
        datasourceChat.open();

        //Textfelder initialisieren
        chatListView = (ListView)findViewById(R.id.userchatlist);
        searchuser = (EditText)findViewById(R.id.searchuser);
        searchuser.setMaxLines(1);

        //Dem Textfeld einen TextChangedListener zuweisen, der direkt wenn dort etwas eingegeben
        //wurde in der Datenbank für die Kontakte sucht ob ein Kontakt mit dem eigegebenen Namen existiert
        searchuser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //Arrays leeren
                chatListView.removeAllViewsInLayout();
                userID.clear();
                userName.clear();
                chatMessages.clear();

                try {

                    //In der Datenbank nach dem eingegebenen Namen suchen
                    String[] name = new String[1];
                    name[0] = searchuser.getText().toString() + "%";

                    String selectSearch = "SELECT userlist.USER_ID,userlist.USER_NAME,chatlist.CHAT_ID,chatlist.CHAT_DATE " +
                            "FROM userlist LEFT JOIN chatlist " +
                            "ON userlist.USER_ID = chatlist.CHAT_ID " +
                            "WHERE userlist.USER_NAME LIKE ? " +
                            "ORDER BY chatlist.CHAT_DATE DESC";

                    //Datenbank Befehl ausführen und in Cursor Element Speichern
                    Cursor c = newDB.rawQuery(selectSearch, name);

                    //Überprüfen ob Cursor Daten enthält
                    if (c != null) {
                        if (c.moveToFirst()) {
                            //Solange Elemente vorhanden sind speichere die gefundenen Daten in den Arrays
                            do {
                                Long tmpid = c.getLong(c.getColumnIndex("USER_ID"));
                                String tmpname = c.getString(c.getColumnIndex("USER_NAME"));

                                //Überprüfe ob ungelesene Nachrichten von dem Nutzer existieren nachdem gesucht wurde
                                String[] id = new String[1];
                                id[0] = Long.toString(tmpid);

                                String unreadmessages = "SELECT COUNT(*) " +
                                        "FROM chatlist " +
                                        "WHERE chatlist.CHAT_ID = ? AND chatlist.CHAT_READ = 'false'";

                                Cursor unreadmess = newDB.rawQuery(unreadmessages, id);
                                unreadmess.moveToFirst();
                                int count = unreadmess.getInt(0);
                                unreadmess.close();

                                if (!userName.contains(tmpname)) {
                                    chatMessages.add("" + count);
                                    userID.add(tmpid);
                                    userName.add(tmpname);
                                }

                            } while (c.moveToNext());
                        }
                        //Zeige die gefundenen Einträge an
                        displayResultList();
                    }

                    //Schließe den Cursor
                    c.close();
                } catch (SQLiteException se) {
                    Log.e(getClass().getSimpleName(), "Could not create or Open the database");
                }

                //Wenn das Textfeld leer ist zeige wieder alle Elemente aus der Datenbank an
                if (searchuser.getText().toString().equals("")) {
                    openAndQueryDatabase();
                    displayResultList();
                }
            }
        });

        //OnclickListener für Listview, wenn Element angeklickt wird starte Chat Activity von dem jeweiligen Nutzer
        chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                openChat(position);
            }
        });

        //OnLongClickLinstener für Listview, wenn Element lange angeklickt wird zeige Fenster mit Option
        //diesen Nutzer zu löschen
        chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                deleteEntry(position);

                return false;
            }
        });

    }

    //Methode um Ausgewählten Kontakt zu löschen
    private void deleteEntry(final int pos){

        //Zeige einen AlertDialog mit dem Namen des zu Löschenden Kontakt
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Delete User: " + userName.get(pos));

        //Erstelle OK Button zum löschen
        builder.setPositiveButton("Contact", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Main_activity.datasourceChat.deleteEntry("" + userID.get(pos));
                Main_activity.datasourceUser.deleteEntry("" + userID.get(pos));

                openAndQueryDatabase();
                displayResultList();
            }
        });
        //Erstelle Button Cancel um das löschen abzubrechen
        builder.setNegativeButton("Messages", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main_activity.datasourceChat.deleteEntry("" + userID.get(pos));
                Main_activity.datasourceChat.createChatEntry(userID.get(pos),
                        Main_activity.user.getString("USER_ID", "0"), ""+userID.get(pos), "Add User", "true", "0", "true", "", "");
            }
        });

        builder.show();

    }

    //Methode um alle Kontake aus der Datenbank zu lesen und in den Array zu Speichern
    private void openAndQueryDatabase(){
        try {
            //Lösche den Inhalt der Arrays
            chatListView.removeAllViewsInLayout();
            userID.clear();
            userName.clear();
            chatMessages.clear();

            //Suche nach allen Konatkten
            String selectQuery = "SELECT USER_ID,USER_NAME " +
                                 "FROM userlist " +
                                 "ORDER BY USER_NAME DESC";

            Cursor c = newDB.rawQuery(selectQuery, null);

            //Überprüfe ob Elemente vorhanden sind
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {

                        //Speichere Daten der Kontakte in Tempoären Variablen
                        Long tmpid = c.getLong(c.getColumnIndex("USER_ID"));
                        String tmpname  = c.getString(c.getColumnIndex("USER_NAME"));

                        //Überprüfe ob ungelesene Nachrichten von dem Kontakt "tmpname" vorhanden sind
                        String[] id = new String[2];
                        id[0] = Long.toString(tmpid);
                        id[1] = "false";

                        String unreadmessages = "SELECT CHAT_READ,CHAT_ID " +
                                "FROM chatlist " +
                                "WHERE CHAT_ID = ? AND CHAT_READ = ? ";

                        Cursor unreadmess = newDB.rawQuery(unreadmessages, id);

                        //Zähle die ungelesenen Nachrichten
                        int count= unreadmess.getCount();
                        unreadmess.close();

                        //Speichere die Tempoären Variablen in den dafür vorgesehenen Arrays
                        chatMessages.add(""+count);
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

    //Methode um die von "openandQueryDatabase" ausgelesenen Daten anzuzeigen
    private void displayResultList() {

        //Erstelle "contactItem" Array
        contactItems = new ArrayList<contactItem>();
        contactItems.clear();

        //Falls keine Kontakte vorhanden sind erstelle Element das anzeigt das keine vorhanden sind
        if(userID.size() == 0){
            userID.add(0L);
            userName.add("No User available");
            chatMessages.add("Please add user");
            chatListView.setClickable(false);
        }

        //Speichere den Inhalt der Arrays in einem contactItem und Speichere dieses dann in dem contactItem Array
        for (int i = 0; i < userID.size(); i++) {
            contactItem item = new contactItem(userID.get(i),userName.get(i), chatMessages.get(i));
            contactItems.add(item);
        }

        //Füge das contactItem Array zu dem Listview hinzu und zeige dieses an
        contactListViewAdapter adapter1 = new contactListViewAdapter(this,R.layout.contact_item, contactItems);
        chatListView.setAdapter(adapter1);
    }

    //Methode die ein Alert Dialog anzeigt wo man nach einem Nutzer suchen kann, ob dieser auf dem
    //Server vorhanden ist
    private void searchforuser(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Search new User:");

        //Erstelle Text Eingabe Feld
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);

        //Erstelle Buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().equals("")) {
                    //Öffne Methode die nach dem Nutzer sucht
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

    //Methode die Aufgerufen wird, wenn man auf einen Kontakt klickt
    private void openChat(int position){

        //Erstelle Intent Objekt und übergabe die Daten des Nutzers
        //auf den geklickt wurde und starte die Chat activity
        Intent intent = new Intent(this, Chat_activity.class);
        Bundle b = new Bundle();
        b.putLong("userid", userID.get(position));
        b.putString("username", userName.get(position));
        intent.putExtras(b);
        startActivity(intent);
    }

    //Methode die aufgerufen wird wenn die Zurücktaste gedrückt wurde
    //Methode muss zweimal innerhalb von 2 Sekunden aufgerufen werden, damit Activity geschlossen wird
    @Override
    public void onBackPressed() {
        //Überprüfe ob Methode schonmal aufgerufen wurde, wenn ja dann schließe die Activity
        if (doubleBackToExitPressedOnce) {

            //App wird geschlossen
            //Datenbank schließen und passwort überschreiben
            userpassword = null;
            newDB.close();
            super.onBackPressed();
            return;
        }

        //Setze Feld auf true und zeige Toast an, dass man die Taste nochmal drücken muss um die Activity zu schließen
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        //Starte Timer, der nach 2 Sekunden das Feld wieder auf false setzt
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    //Methode um seinen Key zu ändern, aber damit dieser geändert werden kann muss der
    //Revoke Key eingegeben werden
    private void revokekey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Enter your Revoke Key:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Starte Asynchronen Task der überprüft ob der Revoke Key richtig ist
                //vorher wird dieser noch gehashed
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

    //Methode um sich auszuloggen und alle Daten auf dem Telefon zu Löschen
    private void logout(){

        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);

        msgBox.setTitle("Secure Chat");
        msgBox.setMessage("Logout will Delete all your Data");

        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Lösche alle daten vom Telefon
                editor.clear();
                editor.commit();

                Main_activity.datasourceChat.deleteAllEntries();
                Main_activity.datasourceUser.deleteAllEntries();

                //Öffne Login Activity
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
        AlertDialog alertDialog = msgBox.create();
        alertDialog.show();
    }

    //Methode um Login Fenster zu öffnen und diese Activity zu schließen
    private void openlogin(){

        Intent i = new Intent(this, Login_activity.class);
        startActivity(i);

        userpassword = null;
        finish();
    }

    //Methode um Activity zu öffnen mit der man einen neuen Key erzeugen kann
    private void createnewkey(){
        Intent i = new Intent(this, NewKey_activity.class);
        startActivityForResult(i, 1);
    }

    //Methode die geöffnet wird wenn der Public Key nicht mit dem auf dem Server übereinstimmt
    //Nutzer muss wie bei RevokeKey Methode Revoke Key eingeben bevor er sich einen neuen
    //Schlüssel generieren kann
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

    //Methode um den Account zu löschen, wenn der eingegebene Revoke Key korrekt ist
    //wird der Account gelöscht und alle Daten von ihm auch
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
                new deleteAccount().execute(Crypto.hashpassword(input.getText().toString(), userpassword));
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

    //Methode die überprüft ob der nutzer nicht schon in der Datenbank vorhanden ist
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

        //Zeige den Nutzernamen im Drawer Layout an
        //showusername.setText(user.getString("USER_NAME", "Error loading Data"));

        //Überprüfe ob der Background Service läuft, wenn ja stoppe ihn
        if(isMyServiceRunning(Background_Service.class.getName())){
            stopService(new Intent(getBaseContext(), Background_Service.class));
        }

        //Starte den Background Servide
        Intent intent = new Intent(this, Background_Service.class);
        startService(intent);

        //Starte den Asynchronen Task der den Public Key mit dem auf dem Server vergleicht
        new checkPublicKey().execute();

        //Lösche alle Notifications mit der ID=0
        mNotificationManager.cancel(0);

        //Starte die Broadcast Reciever
        registerReceiver(receiveruser, new IntentFilter(Background_Service.NOTIFICATION_USER));
        registerReceiver(receiver, new IntentFilter(Background_Service.NOTIFICATION_CHAT));

        //Öffne und zeige Inhalte der Datenbank
        openAndQueryDatabase();
        displayResultList();

        super.onResume();
    }

    @Override
    protected void onPause() {

        //Stoppe die Broadcast Reciever
        unregisterReceiver(receiveruser);
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        userpassword = "0000000000000000000000000000000000";
    }

    //Methode die Prüft ob der Background Service läuft
    private boolean isMyServiceRunning(String className) {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //Wenn neuer Key erzeugt wurde zeige nachricht ob dies erfolgreich war oder nicht
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater men = getMenuInflater();
        men.inflate(R.menu.settings_menu, menu);

        MenuItem menuItemChat = menu.findItem(R.id.nav_chats);

        //Logineintrag passend setzten
        menuItemChat.setEnabled(false).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_chats) {

            finish();
        } else if (id == R.id.nav_new_contact) {

            searchforuser();
        } else if (id == R.id.nav_newkey) {

            revokekey();
        } else if (id == R.id.nav_logout) {

            logout();
        }else if (id == R.id.nav_deleteacc) {

            deleteAccount();
        }

        return true;
    }

    //Asynchroner Task um den Public Key zurückzuziehen
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


            //Erstelle Http Client mit passender URL
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/revokekey.php");

            try {
                //Füge die zu sendenden Daten hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(valueIWantToSend1, userpassword)));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe den Http Post aus
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

                        //Login nicht erfolgreich daher wird das Login Fenster geöffnet
                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();
                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("revokekey_true")) {

                            Toast.makeText(getApplicationContext(), "Revoke Key correct", Toast.LENGTH_LONG).show();

                            if(splitResult[2].equals("delete_true")) {

                                //Login erfolgreich, Daten werden im Mobiltelefon gelöscht und Fenster um neuen Key zu erstellen wird geöffnet
                                editor.putString("RSA_PUBLIC_KEY", "");
                                editor.putString("RSA_PRIVATE_KEY", "");
                                editor.putBoolean("key", false);
                                editor.commit();

                                Main_activity.datasourceChat.deleteAllEntries();

                                createnewkey();
                            }else {
                                Toast.makeText(getApplicationContext(), "Error Please try again", Toast.LENGTH_LONG).show();
                            }

                        }else{

                            //Revoke Key falsch
                            Toast.makeText(getApplicationContext(), "Revoke Key false", Toast.LENGTH_LONG).show();
                            differentkey();
                        }


                    }else {
                        //Es konnte keine Verbindung zu dem Server aufgebaut werden
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                        openlogin();
                        finish();
                    }
                }
            });


        }

    }

    //Asynchroner Task um Online zu Prüfen ob der Nutzer vorhanden ist nachdem gesucht wurde
    private class searchcontact extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result) {
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        public void postData(String valueIWantToSend1) {


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

                    //Trenne den empfangenen String
                    String[] splitResult = String.valueOf(resp).split("::");

                    //Überprüfe ob der Login erfolgreich war
                    if (splitResult[0].equals("login_true")) {

                        //Überprüfe ob der Nutzer auch vorhanden ist
                        if (!splitResult[1].equals("no_user")) {

                            //Nutzer vorhanden trage die Daten in der eigenen Datenbank ein
                            try {
                                Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]),
                                        Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true","","");

                                Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();

                            }finally {

                                //Neuer Butzer eingetragen. Akualisiere die Views
                                chatListView.setClickable(true);
                                openAndQueryDatabase();
                                displayResultList();

                            }

                        }else{

                            //Nutzer nicht vorhanden
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

    //Asynchroner Task zum überprüfen ob der eigene Public Key mit dem auf dem Server zusammen passt
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


            //Erstelle Http CLient
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/testkey.php");

            try {

                //Füge die zu sendende Daten hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe den Http Post aus
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

                    //Trenne den empfangenen String
                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        //Login am Server Fehlgeschlagen, daher aus App ausloggen
                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        //Empfangenen Public Key mit dem eigenen überprüfen
                        String publickeyphone = user.getString("RSA_PUBLIC_KEY", "");
                        String publickeyserver = splitResult[1];

                        if(publickeyserver.equals("---")){

                            //Online kein Public Key eingetragen, daher einen neuen erstellen
                            createnewkey();

                        }else if(!publickeyphone.equals(publickeyserver)){
                            
                            if(publickeyserver.equals("-")){

                                //Public Key wurde geändert, daher abfrage nach Revoke Key bevor ein neuer Public Key generiert werden kann
                                differentkey();

                            }
                        }

                    }else {

                        //Error keine Internet Verbindung, daher ausloggen
                        openlogin();
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    //Asynchroner Taskzum löschen des Accounts
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
                nameValuePairs.add(new BasicNameValuePair("username", user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userid", user.getString("USER_ID", "")));
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

                        //Login Daten sind Falsch
                        Toast.makeText(getApplicationContext(), "Login not Successful" , Toast.LENGTH_LONG).show();
                        openlogin();
                        finish();

                    }else if(splitResult[0].equals("login_true")){

                        //Löschen Erfolgreich. Daten auf dem Mobiltelefon werden gelöscht
                        editor.clear();
                        editor.commit();

                        SQLiteHelper.cleanTableChat(newDB);
                        SQLiteHelper.cleanTableUser(newDB);

                        openlogin();

                    }else {
                        //Es konnte keine Verbindung zu dem Server aufgebaut werden
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                        openlogin();
                        finish();
                    }
                }
            });


        }

    }
}
