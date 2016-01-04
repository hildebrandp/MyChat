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

public class Login_activity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button btnlogin;
    private TextView createlogout;

    private String resp;
    private String result = "false";
    private String SHAHash;
    private static int NO_OPTIONS=0;

    public static SharedPreferences user;
    public static SharedPreferences.Editor editor;
    private boolean doubleBackToExitPressedOnce = false;
    public static SQLiteDatabase newDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        user = getSharedPreferences("myapplab.securechat", MODE_PRIVATE);
        editor = user.edit();

        SQLiteHelper dbHelper = new SQLiteHelper(this);
        newDB = dbHelper.getWritableDatabase();

        username = (EditText)findViewById(R.id.loginusername);
        password = (EditText)findViewById(R.id.loginpassword);
        btnlogin = (Button)findViewById(R.id.btnlogin);
        createlogout = (TextView)findViewById(R.id.txtlogout);

        if(user.getBoolean("login",false)) {
            password.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        if(user.getBoolean("login", false)) {

            username.setText(user.getString("USER_NAME","Error loading Data"));
        }

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkinput()){
                    try {
                        new acclogin().execute(username.getText().toString(), Crypto.hashpassword(password.getText().toString(), username.getText().toString()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        createlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                createnewaccount();
            }
        });

    }

    private boolean checkinput(){

        if(username.equals("")){
            Toast.makeText(getApplicationContext(), "Empty User Name", Toast.LENGTH_LONG).show();
            return false;
        }else if(password.equals("")){
            Toast.makeText(getApplicationContext(), "Empty Password 1", Toast.LENGTH_LONG).show();
            return false;
        }else{
            return true;
        }
    }

    private void createnewaccount(){

        Intent i = new Intent(this, NewAccount_activity.class);
        startActivityForResult(i, 1);
    }

    private void mainactivity(String pass){

        Intent mIntent = new Intent(this, Main_activity.class);
        mIntent.putExtra("userpassword", pass);
        startActivity(mIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if(resultCode == Activity.RESULT_OK){
                String result=data.getStringExtra("result");

                if(result.equals("true")){

                    mainactivity(data.getStringExtra("pass"));
                }

            }else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }

        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            //Clean Ram!!!!

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

    private class acclogin extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {

            if(!params[0].equals(user.getString("USER_NAME", ""))){

                editor.clear();
                editor.commit();
            }

            postData(params[0],params[1]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1, String valueIWantToSend2 ) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/login.php");

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

                    if(splitResult[0].equals("login_false")) {

                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();

                    }else if(splitResult[0].equals("login_true")){

                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_LONG).show();

                        if(!user.getString("USER_ID", "Ad").equals(splitResult[1])){
                            editor.clear();
                            editor.commit();

                            SQLiteHelper.cleanTableChat(newDB);
                            SQLiteHelper.cleanTableUser(newDB);
                        }

                        editor.putString("USER_ID", splitResult[1]);
                        editor.putString("USER_NAME", splitResult[2]);
                        editor.putString("USER_PASSWORD", Crypto.hashpassword(password.getText().toString(), username.getText().toString()));
                        editor.putBoolean("login", true);
                        editor.commit();

                        mainactivity(password.getText().toString());
                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

}
