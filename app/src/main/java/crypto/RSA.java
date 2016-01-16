package crypto;


import android.util.Base64;
import android.util.Log;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import javax.crypto.Cipher;
import activity.mychat.Main_activity;

//Klasse um RSA schlüsselpaar zu erstellen
public class RSA {

    //String für den entschlüsselten Key
    private static String decryptedKey;

    //String für Tag namen
    private static final String TAG = RSA.class.getSimpleName();

    //Länge des RSA Schlüssel
    private static final int KEY_SIZE = 2048;

    //Spongycastle Provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    //Methode um KeyPair zu erstellen
    //RSA Schlüssel erstellen mit einer Länge von 2048 bit und dem "SC" Provider
    public static KeyPair generate() {
        try {
                SecureRandom random = new SecureRandom();
                RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(KEY_SIZE, RSAKeyGenParameterSpec.F4);
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(spec, random);

            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Methode um byte Text zu Verschlüsseln
    public static byte[] encrypt(Key publicKey, byte[] toBeCiphred) {
        try {
                //Verschlüssel die byte´s mit "RSA/ECB/OAEPWithSHA1AndMGF1Padding" Algorithmus
                Cipher rsaCipher = Cipher.getInstance("RSA");
                rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return rsaCipher.doFinal(toBeCiphred);
        } catch (Exception e) {
            //Text konnte nicht verschlüsselt werden
            Log.e(TAG, "Error while encrypting data: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Verschlüsselungs Funktion
    public static String encryptToBase64(Key publicKey, String toBeCiphred) {

            byte[] cyphredText = RSA.encrypt(publicKey, toBeCiphred.getBytes());

        return Base64.encodeToString(cyphredText, Base64.DEFAULT);
    }

    //Methode um byte text zu entschlüsseln
    public static byte[] decrypt(Key privateKey, byte[] encryptedText) {
        try {
                //Entschlüssel die byte´s mit "RSA/ECB/OAEPWithSHA1AndMGF1Padding" Algorithmus
                Cipher rsaCipher = Cipher.getInstance("RSA");
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);

            return rsaCipher.doFinal(encryptedText);
        } catch (Exception e) {
            //Text konnte nicht entschlüsselt werden
            Log.e(TAG, "Error while decrypting data: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //Entschlüsselungs Funktion
    public static String decryptFromBase64(Key key, String cyphredText) {
            byte[] afterDecrypting = RSA.decrypt(key, Base64.decode(cyphredText, Base64.DEFAULT));

        return stringify(afterDecrypting);
    }

    //Verschlüsseln mit übergebenen Public Key
    public static String encryptWithKey(String key, String text) {
        try {
                //Übergebenen String mit Public Key zu PublicKey Element konvertieren
                PublicKey apiPublicKey = Crypto.getRSAPublicKeyFromString(key);

            //Text Verschlüsseln
            return encryptToBase64(apiPublicKey, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Text mit eigenem Public Key Verschlüsseln
    public static String encryptWithStoredKey(String text) {

            //Key aus Shared Preferences holen und dann Text Verschlüsseln
            String strippedKey = Main_activity.user.getString("RSA_PUBLIC_KEY", null);

        return encryptWithKey(strippedKey, text);
    }

    //Text mit Private Key entschlüsseln
    public static String decryptWithStoredKey(String text) {
        try {

            try {
                //Private Key aus Shared Preferences laden und mit AES entschlüsseln
                decryptedKey = AES.decrypt(Main_activity.userpassword, Main_activity.user.getString("RSA_PRIVATE_KEY", null));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Aus String ein Private Key Element erzeugen
            PrivateKey privateKey = Crypto.getRSAPrivateKeyFromString(decryptedKey);

            //Text entschlüsseln
            return decryptFromBase64(privateKey, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            decryptedKey = Crypto.random();
        }
    }

    //Bytes in String Konvertieren
    public static String stringify(byte[] bytes) {
        //Funktion aufrufen die Text Konvertiert
        return stringify(new String(bytes));
    }

    //Funktion die aus Byte einen String macht
    private static String stringify(String str) {
        String aux = "";
        for (int i = 0; i < str.length(); i++) {
            aux += str.charAt(i);
        }
        return aux;
    }

}
