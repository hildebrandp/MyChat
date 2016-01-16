package crypto;


import android.util.Base64;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import activity.mychat.Main_activity;

//Klasse um eine Signatur von einer Nachricht zu erstellen
public class SignatureClass {

    //Signatur Instanz erstellen "SHA512withRSA" Algorithmus
    private static Signature getInstance() {
        try {
            Signature s = Signature.getInstance("SHA512withRSA");
            return s;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //Methode um Signatur von übergebenen Text zu erstellen und gib dann die Signatur zurück
    public static String genSignature(String text) {
        try {
            //Hole Instanz des Algorithmus
            Signature s = getInstance();

            //Lade Private Key aus Shared Preferences und entschlüssel diesen
            //übergebe Private Key an Signatur Element
            //
            s.initSign(Crypto.getRSAPrivateKeyFromString(AES.decrypt(
                    Main_activity.userpassword, Main_activity.user.getString("RSA_PRIVATE_KEY", null))));

            //Update Signatur Element
            s.update(text.getBytes());
            //Erstelle Signatur und gib diese zurück
            return RSA.stringify(Base64.encode(s.sign(), Base64.DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Methode um die Signatur "signatur" mit dem "input" zu vergleichen
    public static boolean checkSignature(String signature, String input, String publickey) {
        try {
            //Hole Instanz des Algorithmus
            Signature s = getInstance();

            //Übergebe den Public Key an das Signatur Element
            s.initVerify(Crypto.getRSAPublicKeyFromString(publickey));
            //Übergebe den "input" an das "signatur" Element
            s.update(input.getBytes());

            //Überprüfe die beiden Signaturen und gib true zurück falls sie übereinstimmen
            return s.verify(Base64.decode(signature.getBytes(), Base64.DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

