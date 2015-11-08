package crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import activity.mychat.MainActivity;

public class Crypto {

    public static SharedPreferences mPreferences;

    private static String encryptedKey;
    private static String SHAHash;
    private static int NO_OPTIONS=0;

    public static void init(Context context) {
        mPreferences = context.getSharedPreferences("myapplab.securechat", 0);
    }

    public static void writePublicKeyToPreferences(KeyPair keyPair) {
        StringWriter publicStringWriter = new StringWriter();

        try {
            PemWriter pemWriter = new PemWriter(publicStringWriter);
            pemWriter.writeObject(new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded()));
            pemWriter.flush();
            pemWriter.close();

            MainActivity.user.edit().putString("RSA_PUBLIC_KEY", publicStringWriter.toString()).commit();

        } catch (IOException e) {
            Log.e("RSA", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writePrivateKeyToPreferences(KeyPair keyPair) {
        StringWriter privateStringWriter = new StringWriter();
        try {
            PemWriter pemWriter = new PemWriter(privateStringWriter);
            pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
            pemWriter.flush();
            pemWriter.close();

            try {
                encryptedKey = AESHelper.encrypt(MainActivity.userpassword, privateStringWriter.toString());
                privateStringWriter = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            MainActivity.user.edit().putString("RSA_PRIVATE_KEY", encryptedKey).commit();
            encryptedKey = "";

        } catch (IOException e) {
            Log.e("RSA", e.getMessage());
            e.printStackTrace();
        }
    }

    public static PublicKey getRSAPublicKeyFromString(String publicKeyPEM) throws Exception {
        publicKeyPEM = stripPublicKeyHeaders(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SC");
        byte[] publicKeyBytes = Base64.decode(publicKeyPEM.getBytes("UTF-8"));
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(x509KeySpec);
    }

    public static PrivateKey getRSAPrivateKeyFromString(String privateKeyPEM) throws Exception {
        privateKeyPEM = stripPrivateKeyHeaders(privateKeyPEM);
        KeyFactory fact = KeyFactory.getInstance("RSA", "SC");
        byte[] clear = Base64.decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        PrivateKey priv = fact.generatePrivate(keySpec);
        Arrays.fill(clear, (byte) 0);
        return priv;
    }

    public static String stripPublicKeyHeaders(String key) {
        //strip the headers from the key string
        StringBuilder strippedKey = new StringBuilder();
        String lines[] = key.split("\n");
        for (String line : lines) {
            if (!line.contains("BEGIN PUBLIC KEY") && !line.contains("END PUBLIC KEY") && !Strings.isNullOrEmpty(line.trim())) {
                strippedKey.append(line.trim());
            }
        }
        return strippedKey.toString().trim();
    }

    public static String stripPrivateKeyHeaders(String key) {
        StringBuilder strippedKey = new StringBuilder();
        String lines[] = key.split("\n");
        for (String line : lines) {
            if (!line.contains("BEGIN PRIVATE KEY") && !line.contains("END PRIVATE KEY") && !Strings.isNullOrEmpty(line.trim())) {
                strippedKey.append(line.trim());
            }
        }
        return strippedKey.toString().trim();
    }

    private static String convertToHex(byte[] data) throws java.io.IOException {


        StringBuffer sb = new StringBuffer();
        String hex=null;

        hex= android.util.Base64.encodeToString(data, 0, data.length, NO_OPTIONS);

        sb.append(hex);

        return sb.toString();
    }

    public static String computeSHAHash(String password) {
        MessageDigest mdSha1 = null;
        try
        {
            mdSha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            Log.e("MyChat", "Error initializing SHA1 message digest");
        }
        try {
            mdSha1.update(password.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] data = mdSha1.digest();
        try {
            SHAHash=convertToHex(data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return SHAHash;
    }

}
