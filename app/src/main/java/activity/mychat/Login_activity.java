package activity.mychat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

//Klasse die als erstes aufgerufen wird um sich bei der App einzuloggen
//Die Klasse überprüft die eingegebenen Daten mit denen auf dem Server
//um sicherzugehen das es sich um die richtige Person handelt
public class Login_activity extends AppCompatActivity {

    //Testfelder und Button für die eingabe der Login Daten
    private EditText username;
    private EditText password;
    private Button btnlogin;
    private TextView createlogout;

    //String für die Antwort von Server
    private String resp;

    //Felder für die Shared Preferences
    //Feld "user" für das auslesen der Daten
    //Feld "editor" für das Editieren der Daten
    public static SharedPreferences user;
    public static SharedPreferences.Editor editor;

    //Boolean Feld zum überprüfen wie oft man auf den Zurück Button drückt
    private boolean doubleBackToExitPressedOnce = false;

    //Datenbank Feld mit dem man zugriff auf die eignene Datenbank bekommt
    public static SQLiteDatabase newDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Zugriff auf die Shared Preferences holen, "MODE_PRIVATE" damit nur diese App zugriff auf die Daten bekommt
        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);
        editor = user.edit();

        //Datenbank öffnen
        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        //Textfelder initialisieren
        username = (EditText)findViewById(R.id.loginusername);
        password = (EditText)findViewById(R.id.loginpassword);
        btnlogin = (Button)findViewById(R.id.btnlogin);
        createlogout = (TextView)findViewById(R.id.txtlogout);

        //Falls jemand die App schon benutzt hat: Zeige den Nutzernamen an und setzte den Focus auf
        //das Feld fürs Passwort und öffne die Tastatur
        if(user.getBoolean("login",false)) {
            password.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            username.setText(user.getString("USER_NAME","Error loading Data"));
        }

        //OnClickListener für den Login Button
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Überprüfen ob die Felder leer sind sonst starte Asynchonen Task, der die Login Daten überprüft
                if(checkinput()){
                    try {
                        //Starte Asynchonen Task und übergebe den Inhalt der Felder. Das Passwort wird vorher noch gehashed
                        new acclogin().execute(username.getText().toString(), Crypto.hashpassword(password.getText().toString(), username.getText().toString()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //OnCLickListener für den Button um sich einen neuen Account zu erstellen
        createlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Öffne Methode um neuen Account zu erstellen
                createnewaccount();
            }
        });

    }

    //Methode die überprüft ob in den beiden Feldern etwas drin steht oder ob diese leer sind
    //Wenn in beiden Felder inhalt steht gin true zurück sonst false
    private boolean checkinput(){

        if(username.equals("")){
            //Feld für den Nutzernamen leer, zeige Toast an
            Toast.makeText(getApplicationContext(), "Empty User Name", Toast.LENGTH_LONG).show();
            return false;
        }else if(password.equals("")){
            //Feld für den Passwort leer, zeige Toast an
            Toast.makeText(getApplicationContext(), "Empty Password", Toast.LENGTH_LONG).show();
            return false;
        }else{
            //In beiden Fleder steht etwas drin, also gib true zurück
            return true;
        }
    }

    //Methode die die Activity startet um sich einen neuen Account anzulegen
    private void createnewaccount(){

        Intent i = new Intent(this, NewAccount_activity.class);
        startActivityForResult(i, 1);
    }

    //Methode die nach erfolgreicher überprüfung der Login Daten die Haupt Activity öffnet
    //und das eingegebene Passwort übergibt und sich selbst dann schließt
    private void mainactivity(String pass){

        Intent mIntent = new Intent(this, Main_activity.class);
        mIntent.putExtra("userpassword", pass);
        startActivity(mIntent);

        finish();
    }

    //Methode die aufgerufen wird wenn die Klasse für die Account erstellung geschlossen wird
    //es wird überprüft ob die erstellung erfolgreich war oder nicht
    //Wenn erfolgreich starte MainActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Überprüfe ob die Methode von der Klasse aufgerufen wurde mit dem RequestCode = 1
        if (requestCode == 1) {

            //Überprufe den ResultCode
            if(resultCode == Activity.RESULT_OK){

                //Überprüfe ob Result der NewAccount Activity true ist
                if(data.getStringExtra("result").equals("true")){

                    mainactivity(data.getStringExtra("pass"));
                }

            }

        }

    }

    //Methode die aufgerufen wird wenn die Zurücktaste gedrückt wurde
    //Methode muss zweimal innerhalb von 2 Sekunden aufgerufen werden, damit Activity geschlossen wird
    @Override
    public void onBackPressed() {
        //Überprüfe ob Methode schonmal aufgerufen wurde, wenn ja dann schließe die Activity
        if (doubleBackToExitPressedOnce) {

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

    //Asynchroner Task der die eingegebenen Login Daten an den Server sendet, der diese dann überprüft
    private class acclogin extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {

            postData(params[0],params[1]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1, String valueIWantToSend2 ) {


            //Erstelle Http Client mit passender URL
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/login.php");

            try {
                //Füge die zusendenden Daten zu dem Http CLient hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("userpassword", valueIWantToSend2));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe Http Post aus und empfange die Daten vom Server
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

                    //Überprüfe die Antwort vom Server
                    if(splitResult[0].equals("login_false")) {

                        //Login Daten sind falsch
                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();

                    }else if(splitResult[0].equals("login_true")){

                        //Login Daten sind richtig
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_LONG).show();

                        //Überprüfe ob der eingeloggte Benutzer mit dem letzten angemeldeten Nutzer übereinstimmt
                        //wenn neuer Nutzer lösche die Daten des alten Nutzers
                        if(!user.getString("USER_ID", "").equals(splitResult[1])){

                            editor.clear();
                            editor.commit();

                            Main_activity.datasourceChat.deleteAllEntries();
                            Main_activity.datasourceUser.deleteAllEntries();

                        }

                        //Speichere die eingegebenen Daten in den Shared Preferences,
                        //ebenso wie die Nutzer ID, die vom Server empfangen wird
                        editor.putString("USER_ID", splitResult[1]);
                        editor.putString("USER_NAME", splitResult[2]);
                        editor.putString("USER_PASSWORD",
                                Crypto.hashpassword(password.getText().toString(), username.getText().toString()));
                        editor.putBoolean("login", true);
                        editor.commit();

                        //Öffne Methode um die Main Activity zu starten, da die eingegebenen Daten korrekt sind
                        mainactivity(password.getText().toString());
                    }else {

                        //Server nicht erreichbar
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

}
