package Securite;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CryptoUtils {

    // Clé AES partagée (128 bits = 16 octets)
    // Dans un vrai système, cette clé devrait être échangée dynamiquement (RSA, Diffie-Hellman, TLS, etc.).
    private static final byte[] SHARED_KEY_BYTES = "MySecretKey12345".getBytes(StandardCharsets.UTF_8);

    private static SecretKeySpec getSharedKey() {
        return new SecretKeySpec(SHARED_KEY_BYTES, "AES");
    }

    public static byte[] encryptAES(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getSharedKey());
        return cipher.doFinal(data);
    }

    public static byte[] decryptAES(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, getSharedKey());
        return cipher.doFinal(encryptedData);
    }

    public static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

