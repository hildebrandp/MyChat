package activity.mychat;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import crypto.AES;
import crypto.Crypto;
import crypto.RSA;
import crypto.SignatureClass;
import database.SQLiteHelper;
import items.Message;
import items.MessagesListAdapter;

//Klasse für´s Chatten, wenn die Klasse geöffnet wird ein ein neues View Geöffnet um mit dem Kontakt zu Chatten, der vorher ausgewählt wurde.
//Diese Klasse Verschüsselt, die zu Sendende Nachricht, und sendet diese ab.
public class Chat_activity extends AppCompatActivity {

    //Array fürs Speichern der Nachrichten mit den zugehörigen Daten
    private ArrayList<String> chatMessage = new ArrayList<String>();
    private ArrayList<String> chatDate = new ArrayList<String>();
    private ArrayList<String> chatVerified = new ArrayList<String>();
    private ArrayList<String> chatSender = new ArrayList<String>();
    private ArrayList<String> chatAESKey = new ArrayList<String>();
    private ArrayList<String> chatSignature = new ArrayList<String>();

    //Message Item in dem alle Daten der Nachricht gespeichert werden
    private List<Message> messageItems;
    private MessagesListAdapter mAdapter;

    //Textfelder für das Anzeigen und Senden der Nachrichten
    private ListView chatlist;
    private EditText texttosend;
    private Button btnsend;

    private String resp;
    private Long userid;
    private String username;

    //Feld für den Notification Manager
    private NotificationManager mNotificationManager;
    //Datenbank Objekt
    public static SQLiteDatabase newDB;

    //Boolean Feld zum überprüfen wie oft man auf den Zurück Button drückt
    private boolean doubleBackToExitPressedOnce = false;

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
        setContentView(R.layout.content_chat);

        //Initialisiere Notification Manager
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        chatlist = (ListView)findViewById(R.id.chatlistView);
        texttosend = (EditText)findViewById(R.id.texttosend);
        btnsend = (Button)findViewById(R.id.btnsend);

        //Speichere die übergebenen Werte in Variablen
        userid = getIntent().getExtras().getLong("userid");
        username = getIntent().getExtras().getString("username");

        //Titel setzten mit dem Namen des Empfängers
        getSupportActionBar().setTitle("User: " + username);

        //Öffne Datenbank
        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        //OnClickListener für den Senden Button
        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!texttosend.getText().toString().equals("")) {
                    //Wenn das Textfeld mit dem zusendendem Text nicht leer ist wird die
                    //Nachricht an die Methode weitergeleitet, die diese dann Verschlüsselt
                    encryptMessage(texttosend.getText().toString());
                }
            }
        });

            //hole die übergebenen Daten aus dem Bundle
            Bundle b = getIntent().getExtras();
            if(b != null){
                userid = b.getLong("userid");
            }else{
                userid = 0L;
            }

        //Falls der Service läuft beende diesen und starte ihn neu
        if(isMyServiceRunning(Background_Service.class.getName())){

            stopService(new Intent(getBaseContext(), Background_Service.class));
        }

        Intent intent = new Intent(this, Background_Service.class);
        startService(intent);

        //Datenbank öffnen und Anzeigen
        openAndQueryDatabase();
        displayResultList();
    }

    //Methode die aufgerufen wird wenn die Zurücktaste gedrückt wurde
    //Methode muss zweimal innerhalb von 2 Sekunden aufgerufen werden, damit Activity geschlossen wird
    @Override
    public void onBackPressed() {
        //Überprüfe ob Methode schonmal aufgerufen wurde, wenn ja dann schließe die Activity
        if (doubleBackToExitPressedOnce) {
            //App wird geschlossen
            //Datenbank schließen
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

    //Methode die prüft ob der Service am laufen ist
    private boolean isMyServiceRunning(String className) {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //Methode zum öffnen der Datenbank. Es werden alle Nachrichten im ListView gespeichert
    private void openAndQueryDatabase(){
        try {

            //Alle Daten in den Array löschen
            chatlist.removeAllViewsInLayout();
            chatMessage.clear();
            chatVerified.clear();
            chatDate.clear();
            chatSender.clear();
            chatAESKey.clear();
            chatSignature.clear();

            String[] data = new String[1];
            data[0] = Long.toString(userid);

            //Suche nach allen Nachrichten von dem User dessen Chat hier geöffnet wurde
            String selectSearch = "SELECT * " +
                    "FROM chatlist " +
                    "WHERE CHAT_ID = ? " +
                    "ORDER BY CHAT_UNIQUE_ID ASC";

            Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

            //Makiere die emfangenen Nachrichten als gelesen
            ContentValues newval = new ContentValues();
            newval.put("CHAT_READ", "true");
            String[] args={Long.toString(userid)};

            Main_activity.newDB.update("chatlist", newval, "CHAT_ID = ?", args);


            //Speichere alle ausgelesenen Daten in den Arrays
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {

                        String tmpchatMessage = c.getString(c.getColumnIndex("CHAT_MESSAGE"));
                        String tmpchatDate  = c.getString(c.getColumnIndex("CHAT_DATE"));
                        String tmpchatsender = c.getString(c.getColumnIndex("CHAT_SENDER_ID"));
                        String tmpchatAESKey = c.getString(c.getColumnIndex("CHAT_AESKEY"));
                        String tmpSignature = c.getString(c.getColumnIndex("CHAT_SIGNATURE"));

                        chatDate.add(tmpchatDate);
                        chatMessage.add(tmpchatMessage);
                        chatSender.add(tmpchatsender);
                        chatAESKey.add(tmpchatAESKey);
                        chatSignature.add(tmpSignature);


                    }while (c.moveToNext());
                }
            }

            c.close();
        } catch (SQLiteException se ) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } finally {

            try {
                chatlist.removeAllViewsInLayout();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    //Methode zum Anzeigen der aus der Datenbak ausgelesenen Nachrichten
    private void displayResultList() {

        messageItems = new ArrayList<Message>();
        messageItems.clear();
        String tmpmessage;
        String tmpSignature="false";

        for (int i = 0; i < chatMessage.size(); i++) {

            if(i == 0){

                //Falls es die erste Nachricht ist zeige "Chat Created" an
                tmpmessage = "Chat Created";
            }else {

                //Entschlüssele die Nachricht
                tmpmessage = decryptMessage(chatMessage.get(i), i);

                if(chatSignature.get(i).length() > 0){

                    //Überprüfe die Signatur der Nachricht
                    tmpSignature = ""+checkSignature(chatSignature.get(i), tmpmessage, chatMessage.get(i), chatSender.get(i));

                }else{

                    //Keine Signatur vorhanden
                    tmpSignature = "false";
                }

                if(tmpmessage.length() == 0){

                    //Nachricht konnte nicht Entschlüsselt werden
                    tmpmessage = "System:Can´t Decrypt";
                }
            }

            //Speichere die Entschlüsselte Nachricht in einem Message Item
            Message item = new Message(chatDate.get(i), tmpmessage, tmpSignature, chatSender.get(i));
            messageItems.add(item);
        }

        //Speichere die Message Items in einem ListView
        mAdapter = new MessagesListAdapter(this, messageItems);
        chatlist.setAdapter(mAdapter);
    }

    //Methode zum Überprüfen der Signatur der Nachricht
    private Boolean checkSignature(String signature, String decmessage, String encmessage, String sender){


        //Wenn die entschlüsselte und die verschlüsselte Nachricht ungleich null ist überprüfe sie Signatur
        if(decmessage != null && encmessage != null){

            String pubkey;

                if(sender.equals(Main_activity.user.getString("USER_ID",""))){

                    pubkey = Main_activity.user.getString("RSA_PUBLIC_KEY","");
                }else{

                    pubkey = getPublicKey();
                }

            //Überprüfe ob die Signatur der Nachricht Korrekt ist
            return SignatureClass.checkSignature(signature, encmessage, pubkey);
        }else{

            return false;
        }
    }

    //Methode zum Entschlüsseln der Nachrichten
    private String decryptMessage(String message,int pos){
        String decryptedMessage = "";

            //Entschlüssel den AES Schlüssel mit dem Privaten RSA Schlüssel
            String decryptedKey = RSA.decryptWithStoredKey(chatAESKey.get(pos));

            try {
                //Entschlüssel die Nachricht mit dem AES Schlüssel
                decryptedMessage = AES.decrypt(decryptedKey, message);

            }catch (Exception e){
                e.printStackTrace();
            }

        //Gib die Entschlüsselte Nachricht zurück
        return decryptedMessage;
    }

    //Methode zum Verschlüsseln der Nachrichten
    private void encryptMessage(String message){

            //Hole den Public Key des Empfängers
            String key = getPublicKey();
            //Erstelle Seed für AES Verschlüsselung
            String rand = Crypto.random();

            String encryptedkey;
            String privateencrypt;
            String encryptedmessage=null;

            //Erstelle Date Format und hole Aktuelle System Zeit
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateandTime = sdf.format(Calendar.getInstance().getTime());

                try {
                    //Verschlüssele die Nachricht mit AES und der vorher erstellten Seed
                    encryptedmessage = AES.encrypt(rand, message);
                }catch (Exception e) {
                    e.printStackTrace();
                }

            //Verschlüssel den AES Schlüssel mit dem Public Key des Empfängers
            encryptedkey = RSA.encryptWithKey(key, rand);
            //Verschlüssel den AES Schlüssel mit dem eigenen Public Key für die eigene Datenbank
            privateencrypt = RSA.encryptWithStoredKey(rand);

            //Erstelle eine Signatur der Nachricht mit dem eignen RSA Schlüssel
            String signature = SignatureClass.genSignature(encryptedmessage);

            //Sende Nachricht an den Server
            new sendMessage().execute(encryptedmessage, currentDateandTime, encryptedkey, signature);

            //Speichere die Nachricht in der Datennak mit "privatencrypt",
            //damit man die gesendeten Nachrichten auch wieder entschlüsseln kann
            Main_activity.datasourceChat.createChatEntry(userid, Main_activity.user.getString("USER_ID", "0"),
                    Long.toString(userid), encryptedmessage, "true", currentDateandTime, "true", privateencrypt, signature);

            texttosend.setText("");
            rand = "";

            //Aktualisiere die Datenbank und das ListView
            openAndQueryDatabase();
            displayResultList();
    }

    //Methode um den Public Key des Empfängers aus der Datenbank zu holen
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

    //Methode falls der Empfänger keinen Public Key hat zeige Alert Dialog
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater men = getMenuInflater();
        men.inflate(R.menu.settings_menu, menu);

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

    //Wenn der Nutzer seinen Key zurückziehen will muss er seinen Revoke Key eingeben. Dafür Wird ein Alert Dialog angezeigt
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

    //Wenn der Nutzer sich ausloggt werden alle seine Daten Gelöscht
    private void logout(){

        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);

        msgBox.setTitle("Secure Chat");
        msgBox.setMessage("Logout will Delete all your Data");
        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                revokekey();

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

    //Methode die das Login Fenster öffnet
    private void openlogin(){

        Intent i = new Intent(this, Login_activity.class);
        startActivity(i);
        finish();
    }

    //Alert Dialog wo man einen Namen eingeben kann nach dem man suchen will
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

    //Überprüfe ob der Nutzer schon in der eigenen Datenbank vorhanden ist
    private void checkifuserexists(String name){

        String[] data = new String[1];
        data[0] = name;

        String selectSearch = "SELECT userlist.USER_NAME " +
                "FROM userlist " +
                "WHERE userlist.USER_NAME = ? ";

        Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

        int count = c.getCount();
        if (count == 0) {

            //Name nicht in der Datenbank vorhanden, daher wird auf dem Server nach dem Nutzer gesucht
            new searchcontact().execute(name,"false");
        }else{
            Toast.makeText(getApplicationContext(), "User already exist", Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    //Öffne Fenster um neuen Key zu erstellen
    private void createnewkey(){
        Intent i = new Intent(this, NewKey_activity.class);
        startActivityForResult(i, 1);
    }

    //Methode falls der eigene Public Key nicht mit dem Public Key auf dem Server übereinstimmt
    //Nutzer muss Revoke Key eingeben oder er wird Ausgeloggt
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

    //Methode um dem Account zu löschen
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

    //OnResume Methode die überprüft ob die Daten des Nutzers mit denen auf dem Server überprüft
    //und den Broadcast Reciever startet, der die Daten von neuen Nachrichten Empfängt
    @Override
    protected void onResume() {
        super.onResume();

        new searchcontact().execute(username, "true");

        registerReceiver(receiver, new IntentFilter(Background_Service.NOTIFICATION_CHAT));
    }

    //OnPause Methode die den Broadcast Reciever Stopt
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
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

                        //Login nicht erfolgreich daher wird das Login Fenster geöffnet
                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();
                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("revokekey_true")) {

                            Toast.makeText(getApplicationContext(), "Revoke Key correct", Toast.LENGTH_LONG).show();

                            if(splitResult[2].equals("delete_true")) {

                                //Login erfolgreich, Daten werden im Mobiltelefon gelöscht und Fenster um neuen Key zu erstellen wird geöffnet
                                Main_activity.editor.putString("RSA_PUBLIC_KEY", "");
                                Main_activity.editor.putString("RSA_PRIVATE_KEY", "");
                                Main_activity.editor.commit();

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

                                    //Erstelle für den Nutzer einen Eintrag in in der Nutzer und Nachrichten Datenbank
                                    Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                    Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]),Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true", "","");

                                } finally {

                                    Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();
                                }

                            } else if (checkkey.equals("true")){

                                //Hole Public Key aus der Datenbank
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
                                    //Wenn der Public Key von Server sich von dem Public Key aus der Eigenen Datenbank unterscheidet, akualisiere den Publick Key in der eigenen Datenbank und lösche
                                    //alle bisherigen Nachrichten mit diesem Nutzer

                                    Main_activity.datasourceChat.deleteEntry(splitResult[1]);

                                    Main_activity.datasourceUser.deleteEntry(splitResult[1]);

                                    Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                    Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]), Main_activity.user.getString("USER_ID", "0"), splitResult[1], "Add User", "true", "0", "true", "","");

                                    Toast.makeText(getApplicationContext(), "User has changed his Public Key", Toast.LENGTH_LONG).show();
                                }else if(splitResult[3].equals("-") || splitResult[3].equals("---")){

                                    //Wenn Nutzer keinen Public Key auf dem Server hat wird das eingabe Feld zum Senden vomn NAchrichten gesperrt
                                    Toast.makeText(getApplicationContext(), "User has no Public Key\nPlease try again later!", Toast.LENGTH_LONG).show();
                                    btnsend.setClickable(false);
                                    texttosend.setText("Can´t send Message!");
                                    texttosend.setClickable(false);

                                }

                            }

                        } else {
                            //Nutzer nicht vorhanden
                            searchforuser();
                            Toast.makeText(getApplicationContext(), "No User", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        //Es konnte keine Verbindung zu dem Server aufgebaut werden
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                        openlogin();
                        finish();
                    }
                }
            });


        }

    }

    //Asynchroner Task zum Senden von Nachrichten
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
                nameValuePairs.add(new BasicNameValuePair("recieverid", Long.toString(userid)));
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
                    //Login Daten Passen nicht, daher wird das Login Fenster geöffnet
                        openlogin();
                        finish();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("message_send")){
                            //Nachricht wurde erfolgreich gesendet
                            //Toast.makeText(getApplicationContext(), "Message Send" , Toast.LENGTH_LONG).show();
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

                        //Login Daten sind Falsch
                        Toast.makeText(getApplicationContext(), "Login not Successful" , Toast.LENGTH_LONG).show();
                        openlogin();
                        finish();

                    }else if(splitResult[0].equals("login_true")){

                        //Löschen Erfolgreich. Daten auf dem Mobiltelefon werden gelöscht
                        Main_activity.editor.clear();
                        Main_activity.editor.commit();

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
