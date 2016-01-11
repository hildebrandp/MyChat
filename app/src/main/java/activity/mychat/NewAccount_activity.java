package activity.mychat;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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


//Klasse um einen neuen Account zu erstellen
public class NewAccount_activity extends AppCompatActivity{

    //Textfelder für Nutzernamen und Passwort, Button um den Account zu erstellen
    private EditText newUsername;
    private EditText newPassword1;
    private EditText newPassword2;
    private Button createAccount;

    //Result Code der an die Login Activity übergeben wird, wenn diese Activity geschlossen wird
    private String result = "false";

    //String für die Antwort vom Server
    private String resp;

    //Boolean Feld zum überprüfen wie oft man auf den Zurück Button drückt
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newaccount);

        //Felder initialisieren
        newUsername = (EditText)findViewById(R.id.newUsername);
        newPassword1 = (EditText)findViewById(R.id.newPassword1);
        newPassword2 = (EditText)findViewById(R.id.newPassword2);
        createAccount = (Button)findViewById(R.id.btncreateaccount);

        //OnClickListener für Button
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Überprüfen ob Felder leer oder Passwörter ungleich
                if (checkinput()) {

                    //Button Clickable auf false setzten solange geprüft wird ob Account erstellt werden kann
                    createAccount.setClickable(false);
                    new createnewaccount().execute(newUsername.getText().toString(), Crypto.hashpassword(newPassword1.getText().toString(), newUsername.getText().toString() ));
                }
            }
        });

    }

    //Methode die prüft ob die eingabe Felder leer sind oder ob die Passwörter ungleich sind
    private boolean checkinput(){

        if(newUsername.equals("")){
            Toast.makeText(getApplicationContext(), "Empty User Name", Toast.LENGTH_LONG).show();
            return false;
        }else if(newPassword1.equals("")){
            Toast.makeText(getApplicationContext(), "Empty Password 1", Toast.LENGTH_LONG).show();
            return false;
        }else if(newPassword2.equals("")){
            Toast.makeText(getApplicationContext(), "Empty Password 2", Toast.LENGTH_LONG).show();
            return false;
        }else if(!newPassword1.getText().toString().equals(newPassword2.getText().toString())){
            Toast.makeText(getApplicationContext(), "Passwords not equal!", Toast.LENGTH_LONG).show();
            return false;
        }else{
            return true;
        }
    }

    //Wenn erstellung erfolgreich war wird diese Activity geschlossen und der Result code true
    //an die Login Activity übergeben
    private void startMain(){

        result = "true";
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", result);
        returnIntent.putExtra("pass", newPassword1.getText().toString());
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    //Methode die aufgerufen wird wenn die Zurücktaste gedrückt wurde
    //Methode muss zweimal innerhalb von 2 Sekunden aufgerufen werden, damit Activity geschlossen wird
    @Override
    public void onBackPressed() {
        //Überprüfe ob Methode schonmal aufgerufen wurde, wenn ja dann schließe die Activity
        if (doubleBackToExitPressedOnce) {

            //Activity schließen und Result Code false zurückgeben
            result = "false";
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", result);
            setResult(RESULT_OK, returnIntent);
            finish();

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

    //Asynchroner Task der prüft ob der Nutzername verfügbar ist, wenn dieser frei ist wird
    //der Account erstellt
    private class createnewaccount extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0],params[1]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1, String valueIWantToSend2) {


            //Erstelle Http Client
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/newaccount.php");

            try {
                //Füge die Daten zu dem Http Client hinzu
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("userpassword", valueIWantToSend2));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe Http Post aus
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
                    if(splitResult[0].equals("user_name_used")) {

                        //Nutzername bereits vergeben, Textfeld vom Nutzernamen leeren und Button wieder freigeben
                        Toast.makeText(getApplicationContext(), "User Name is already used", Toast.LENGTH_LONG).show();
                        newUsername.setText("");
                        createAccount.setClickable(true);

                    }else if(splitResult[0].equals("insert_success")){

                        //Erstellung des Accounts erfolgreich
                        //Daten in den Shared Preferences Speichern
                        Login_activity.editor.putString("USER_ID", splitResult[1]);
                        Login_activity.editor.putString("USER_NAME", newUsername.getText().toString());
                        Login_activity.editor.putString("USER_PASSWORD", Crypto.hashpassword(newPassword1.getText().toString(), newUsername.getText().toString()));
                        Login_activity.editor.putString("RSA_PUBLIC_KEY", "");
                        Login_activity.editor.putString("RSA_PRIVATE_KEY", "");
                        Login_activity.editor.putBoolean("login", true);
                        Login_activity.editor.putBoolean("key", false);
                        Login_activity.editor.commit();

                        //Activity schließen
                        startMain();

                    }else {

                        //Server nicht erreichbar
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                        createAccount.setClickable(true);
                    }
                }
            });


        }

    }

}
