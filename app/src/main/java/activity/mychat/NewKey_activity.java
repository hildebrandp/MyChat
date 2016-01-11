package activity.mychat;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import crypto.Crypto;
import crypto.RSA;

//Activity um neuen Public und Private Key zu erzeugen
public class NewKey_activity extends AppCompatActivity {

    //String für die Antwort des Servers
    private String resp;
    //String für Result Code der zurückgegeben wird
    private String result = "false";
    //String für Random erzeugten Key
    private String key;
    //Boolean Feld zum überprüfen wie oft man auf den Zurück Button drückt
    private boolean doubleBackToExitPressedOnce = false;
    //Char Array mit den Buchstaben aus denen der Random String erzeugt wird
    private static char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newkey);

        //Starte Asynchronen Task für Key erzeugung
        new createKey().execute();
    }

    //Methode um einen Secure Random Key zu erzeugen
    public static String random() {
        SecureRandom srand = new SecureRandom();
        Random rand = new Random();
        char[] buff = new char[16];

        for (int i = 0; i < 16; ++i) {

            if ((i % 10) == 0) {
                rand.setSeed(srand.nextLong());
            }
            buff[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(buff);
    }

    //Methode die einem den Revoke Key anzeigt, den man braucht um den Account zu Löschen
    //oder um einen neuen Key zu erzeugen
    private void revokekeywindow(){

        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);

        msgBox.setTitle("Secure Chat");
        msgBox.setMessage("If you want to Revoke your Public-Key you need this Key: " + key);
        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                result = "true";
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", result);
                setResult(RESULT_OK, returnIntent);
                NewKey_activity.this.finish();
            }
        });

        msgBox.setCancelable(false);
        // create alert dialog
        AlertDialog alertDialog = msgBox.create();
        // show it
        alertDialog.show();
    }

    //Methode um Login Fenster zu öffnen und diese Activity zu schließen
    private void openlogin(){

        Intent i = new Intent(this, Login_activity.class);
        startActivity(i);

        Main_activity.userpassword = null;
        finish();
    }

    //Methode die aufgerufen wird wenn die Zurücktaste gedrückt wurde
    //Methode muss zweimal innerhalb von 2 Sekunden aufgerufen werden, damit Activity geschlossen wird
    @Override
    public void onBackPressed() {
        //Überprüfe ob Methode schonmal aufgerufen wurde, wenn ja dann schließe die Activity
        if (doubleBackToExitPressedOnce) {
            result = "false";
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", result);
            setResult(RESULT_OK, returnIntent);

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

    //Methode die den Result Code false zurück gibt wenn die Activity vorzeitig beendet wird
    @Override
    protected void onDestroy() {
        result = "false";
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", result);
        setResult(RESULT_OK, returnIntent);
        super.onDestroy();
    }

    //Asynchroner Task, der die Schlüssel erzeugt und diese dann abspeichert
    private class createKey extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {

            //Erzeuge 2048 bit RSA-Schlüsselpaar
            final KeyPair keyPair = RSA.generate();

            //Speichere RSA-Schlüssel in den Shared Preferences
            Crypto.writePrivateKeyToPreferences(keyPair);
            Crypto.writePublicKeyToPreferences(keyPair);

            //Erzeuge Revoke Key
            key = random();

            postData(key);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String revokekey) {


            //Erzeuge Http Client
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/updateKey.php");

            try {
                //Füge die Daten zum Client
                String publickey = Main_activity.user.getString("RSA_PUBLIC_KEY", "");
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userpublickey", publickey));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(revokekey, Main_activity.userpassword)));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                //Führe HTTP Post aus und warte auf Antwort vom Server
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

                        //Login nicht erfolgreich
                        openlogin();
                    }else if(splitResult[0].equals("login_true")){

                        //Login erfolgreich
                        if(splitResult[1].equals("update_true")){

                            //Public Key erfolgreich auf Server Gespeichert
                            Toast.makeText(getApplicationContext(), "Key update successful", Toast.LENGTH_LONG).show();

                            //Shared Preferences Aktualisieren
                            Main_activity.editor.putBoolean("key", true);
                            Main_activity.editor.commit();

                            //Zeige Fenster mit dem Revoke Key
                            revokekeywindow();

                        }else{

                            //Key Speichern fehlgeschlagen
                            Toast.makeText(getApplicationContext(), "Key update fail" , Toast.LENGTH_LONG).show();
                        }

                    }else {

                        //Error bei hochladen
                        openlogin();
                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }


}
