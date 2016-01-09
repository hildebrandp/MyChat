package crypto;


import android.util.Base64;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import activity.mychat.Main_activity;


public class SignatureUtils {

    private static Signature getInstance() {
        try {
            Signature s = Signature.getInstance("SHA512withRSA", new BouncyCastleProvider());

            return s;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String genSignature(String text) {
        try {
            Signature s = getInstance();

            s.initSign(Crypto.getRSAPrivateKeyFromString(AESHelper.decrypt(
                    Main_activity.userpassword, Main_activity.user.getString("RSA_PRIVATE_KEY", null))));

            s.update(text.getBytes());
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

    public static boolean checkSignature(String signature, String input, String publickey) {
        try {
            Signature s = getInstance();
            s.initVerify(Crypto.getRSAPublicKeyFromString(publickey));
            s.update(input.getBytes());

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

