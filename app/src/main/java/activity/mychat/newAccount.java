package activity.mychat;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import crypto.Crypto;


public class newAccount extends AppCompatActivity{

    private EditText newUsername;
    private EditText newPassword1;
    private EditText newPassword2;
    private Button createAccount;

    private String result = "false";
    private String resp;

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newaccount);

        newUsername = (EditText)findViewById(R.id.newUsername);
        newPassword1 = (EditText)findViewById(R.id.newPassword1);
        newPassword2 = (EditText)findViewById(R.id.newPassword2);
        createAccount = (Button)findViewById(R.id.btncreateaccount);


        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkinput()) {

                    new createnewaccount().execute(newUsername.getText().toString(), Crypto.computeSHAHash(newPassword1.getText().toString()));
                }
            }
        });

    }

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

    private void startMain(){

        result = "true";
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", result);
        returnIntent.putExtra("pass", newPassword1.getText().toString());
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {

            result = "false";
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", result);
            setResult(RESULT_OK, returnIntent);
            finish();

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


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/newaccount.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("userpassword", valueIWantToSend2));
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

                    if(splitResult[0].equals("user_name_used")) {

                        Toast.makeText(getApplicationContext(), "User Name is already used", Toast.LENGTH_LONG).show();
                        newUsername.setText("");

                    }else if(splitResult[0].equals("insert_success")){

                        login.editor.putString("USER_ID", splitResult[1]);
                        login.editor.putString("USER_NAME", newUsername.getText().toString());
                        login.editor.putString("RSA_PUBLIC_KEY", "");
                        login.editor.putString("RSA_PRIVATE_KEY", "");
                        login.editor.putBoolean("login", true);
                        login.editor.putBoolean("key", false);
                        login.editor.commit();

                        startMain();

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

}
