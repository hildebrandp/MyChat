package activity.mychat;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
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
import recivekey.Bluetooth;
import recivekey.NFC;
import recivekey.enterkey;

public class NewKey_activity extends AppCompatActivity implements View.OnClickListener{

    private Button createkey;
    private Button enterKey;
    private Button recievebluetooth;
    private Button recievenfc;

    private String resp;
    private String result = "false";
    private String key;


    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newkey);

        createkey = (Button)findViewById(R.id.btncreatenewkey);
        enterKey = (Button)findViewById(R.id.btnrevieveenter);
        recievebluetooth = (Button)findViewById(R.id.btnrevievebluetooth);
        recievenfc = (Button)findViewById(R.id.btnrevievenfc);

        createkey.setOnClickListener(this);
        enterKey.setOnClickListener(this);
        recievebluetooth.setOnClickListener(this);
        recievenfc.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if(resultCode == Activity.RESULT_OK){
                String result=data.getStringExtra("result");

                if(result.equals("true")){

                    Main_activity.editor.putBoolean("key", true);
                    result = "true";
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", result);
                    setResult(RESULT_OK, returnIntent);
                    finish();

                }else{

                }

            }else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }

        }
    }//onActivityResult


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.btncreatenewkey:

                    createkey.setClickable(false);
                    enterKey.setClickable(false);
                    recievebluetooth.setClickable(false);
                    recievenfc.setClickable(false);

                    new createKey().execute();

                break;
            case R.id.btnrevieveenter:

                    Intent intent1 = new Intent(this, enterkey.class);
                    startActivityForResult(intent1, 1);
                break;
            case R.id.btnrevievebluetooth:

                    Intent intent2 = new Intent(this, Bluetooth.class);
                    startActivityForResult(intent2, 1);
                break;
            case R.id.btnrevievenfc:

                    Intent intent3 = new Intent(this, NFC.class);
                    startActivityForResult(intent3, 1);
                break;
        }
    }

    public static String random() {

        SecureRandom sr = new SecureRandom();
        byte[] output = new byte[16];
        sr.nextBytes(output);

        return sr.toString();
    }

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

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            result = "false";
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", result);
            setResult(RESULT_OK, returnIntent);

            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private class createKey extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {

            final KeyPair keyPair = RSA.generate();

            Crypto.writePrivateKeyToPreferences(keyPair);
            Crypto.writePublicKeyToPreferences(keyPair);

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


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/updateKey.php");

            try {
                // Add your data
                String publickey = Main_activity.user.getString("RSA_PUBLIC_KEY", "");
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.user.getString("USER_PASSWORD", "")));
                nameValuePairs.add(new BasicNameValuePair("userpublickey", publickey));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(revokekey, Main_activity.userpassword)));
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

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("update_true")){

                            Toast.makeText(getApplicationContext(), "Key update successful", Toast.LENGTH_LONG).show();

                            Main_activity.editor.putBoolean("key", true).commit();
                            Main_activity.editor.putBoolean("haskey", true).commit();

                            revokekeywindow();
                        }else{

                            Toast.makeText(getApplicationContext(), "Key update fail" , Toast.LENGTH_LONG).show();

                            createkey.setClickable(true);
                            enterKey.setClickable(true);
                            recievebluetooth.setClickable(true);
                            recievenfc.setClickable(true);
                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();

                        createkey.setClickable(true);
                        enterKey.setClickable(true);
                        recievebluetooth.setClickable(true);
                        recievenfc.setClickable(true);
                    }
                }
            });


        }

    }


}
