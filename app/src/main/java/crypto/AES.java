package crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//Klasse um Text mit AES zu Verschlüsseön oder zu Entschlüsseln
public class AES {

    //Hex Zeichen zum Konvertieren
    private final static String HEX = "0123456789ABCDEF";

    //Methode um den Text "cleartext" mit mit dem Seed "seed" zu verschlüssel
    public static String encrypt(String seed, String cleartext) throws Exception {
            byte[] rawKey = getRawKey(seed.getBytes());
            byte[] result = encrypt(rawKey, cleartext.getBytes());
        return toHex(result);
    }

    //Methode um den Text "encrypted" mit mit dem Seed "seed" zu entschlüsseln
    public static String decrypt(String seed, String encrypted) throws Exception {
            byte[] rawKey = getRawKey(seed.getBytes());
            byte[] enc = toByte(encrypted);
            byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    //Methode um mit dem Seed einen 256 bit langen AES-Schlüssel
    private static byte[] getRawKey(byte[] seed) throws Exception {
            //Erstelle Key Generator mit Instanz von AES
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG","Crypto");
            sr.setSeed(seed);
            kgen.init(256, sr);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
        return raw;
    }

    //Verschlüssel den Text mit dem AES Algorithmus
    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    //Entschlüssel den Text mit dem AES Algorithmus
    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    //Konvertiere den String zu Byte
    public static byte[] toByte(String hexString) {
            int len = hexString.length()/2;
            byte[] result = new byte[len];
            for (int i = 0; i < len; i++){
                result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
            }
        return result;
    }

    //Konvertiere Byte zu Hex
    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2*buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    //Konvertiere Byte zu einem String
    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
    }

}
