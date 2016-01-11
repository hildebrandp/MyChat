package crypto;

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
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import activity.mychat.Main_activity;

public class Crypto {

    //String für den Verschlüsselten Key
    private static String encryptedKey;

    //Anzahl der Iterationen für die Hashfunktion
    public static int PW_HASH_ITERATION_COUNT = 2500;
    //Feld für den Message Digest
    private static MessageDigest md;
    //Länge für die Seed, 192 Zeichen
    private static int randomlength = 192;

    //Zeichen die erlaubt sind für die Random Funktion
    private static char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();

    //Schreibe den Public Key in die Shared Preferences
    public static void writePublicKeyToPreferences(KeyPair keyPair) {
        StringWriter publicStringWriter = new StringWriter();

        try {
            //Extrahiere den Public Key aus dem KeyPair
            PemWriter pemWriter = new PemWriter(publicStringWriter);
            pemWriter.writeObject(new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded()));
            pemWriter.flush();
            pemWriter.close();

            //Entferne den Header des Public Key´s und Speicher diesen in den Shared Preferences
            Main_activity.user.edit().putString("RSA_PUBLIC_KEY", stripPublicKeyHeaders(publicStringWriter.toString())).commit();

        } catch (IOException e) {
            Log.e("RSA", e.getMessage());
            e.printStackTrace();
        }
    }

    //Schreibe den Private Key in die Shared Preferences
    public static void writePrivateKeyToPreferences(KeyPair keyPair) {
        StringWriter privateStringWriter = new StringWriter();
        try {
            //Extrahiere den Private Key aus dem KeyPair
            PemWriter pemWriter = new PemWriter(privateStringWriter);
            pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
            pemWriter.flush();
            pemWriter.close();

            try {
                //Entferne den Header des Private Key´s
                //und der Key wird mit dem AES Algorithmus verschlüsselt
                encryptedKey = AESHelper.encrypt(Main_activity.userpassword, stripPrivateKeyHeaders(privateStringWriter.toString()));
                privateStringWriter = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Der Verschlüsselte Key wird in den Shared Preferences gespeichert
            Main_activity.user.edit().putString("RSA_PRIVATE_KEY", encryptedKey).commit();
            encryptedKey = "";

        } catch (IOException e) {
            Log.e("RSA", e.getMessage());
            e.printStackTrace();
        }
    }

    //Methode mit der aus dem String der Public Key erstellt wird und als Key Element zurückgegeben wird
    public static PublicKey getRSAPublicKeyFromString(String publicKeyPEM) throws Exception {
            //Erstelle KeyFactory mit der "RSA" Instanz und dem "SC" Provider
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SC");
            byte[] publicKeyBytes = Base64.decode(publicKeyPEM.getBytes("UTF-8"));
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(x509KeySpec);
    }

    //Methode mit der aus dem String der Private Key erstellt wird und als Key Element zurückgegeben wird
    public static PrivateKey getRSAPrivateKeyFromString(String privateKeyPEM) throws Exception {
            //Erstelle KeyFactory mit der "RSA" Instanz und dem "SC" Provider
            KeyFactory fact = KeyFactory.getInstance("RSA", "SC");
            byte[] clear = Base64.decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
            PrivateKey priv = fact.generatePrivate(keySpec);
            Arrays.fill(clear, (byte) 0);
        return priv;
    }

    //Entferne den Header des Public Key´s
    public static String stripPublicKeyHeaders(String key) {
        StringBuilder strippedKey = new StringBuilder();
        String lines[] = key.split("\n");
        //Durchsuche alle Zeilen des Keys
        for (String line : lines) {
            //Entferne die Zeilen in denen "BEGIN PUBLIC KEY" und "END PUBLIC KEY" vorkommt
            if (!line.contains("BEGIN PUBLIC KEY") && !line.contains("END PUBLIC KEY") && !Strings.isNullOrEmpty(line.trim())) {
                strippedKey.append(line.trim());
            }
        }
        return strippedKey.toString().trim();
    }

    //Entferne den Header des Private Key´s
    public static String stripPrivateKeyHeaders(String key) {
        StringBuilder strippedKey = new StringBuilder();
        String lines[] = key.split("\n");
        //Durchsuche alle Zeilen des Keys
        for (String line : lines) {
            //Entferne die Zeilen in denen "BEGIN PUBLIC KEY" und "END PUBLIC KEY" vorkommt
            if (!line.contains("BEGIN PRIVATE KEY") && !line.contains("END PRIVATE KEY") && !Strings.isNullOrEmpty(line.trim())) {
                strippedKey.append(line.trim());
            }
        }
        return strippedKey.toString().trim();
    }

    //Methode um einen String mit dem Seed zu hashen
    //Als Hash funktion wird ein "SHA-512" Algorithmus verwendet
    public static String hashpassword(String password,String seed) {

            try {
                //Erzeuge MessageDigest mit Instanz von "SHA-512" Algorithmus
                md = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new RuntimeException("No Such Algorithm");
            }

            String result = hashPw(password, seed);

            return result;
    }

    //Funktion um das Passwort zu hashen
    private static String hashPw(String pw, String salt) {
        byte[] bSalt;
        byte[] bPw;

        //Konvertiere String zu Byte
        try {
            bSalt = salt.getBytes("UTF-8");
            bPw = pw.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported Encoding", e);
        }

        //Iteriere Passwort
        byte[] digest = run(bPw, bSalt);
        for (int i = 0; i < PW_HASH_ITERATION_COUNT - 1; i++) {
            digest = run(digest, bSalt);
        }

        return Base64.toBase64String(digest);
    }

    //Hash Funktion
    private static byte[] run(byte[] input, byte[] salt) {
        md.update(input);
        return md.digest(salt);
    }

    //Methode zum erstellen eines Zufälligen Strings für die AES Verschlüsselung
    public static String random() {
        SecureRandom srand = new SecureRandom();
        Random rand = new Random();
        char[] buff = new char[randomlength];

        for (int i = 0; i < randomlength; ++i) {

            if ((i % 10) == 0) {
                rand.setSeed(srand.nextLong());
            }
            buff[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(buff);
    }

}
