package eu.domibus.util;

import eu.domibus.api.util.EncryptionUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class EncryptionUtilImpl implements EncryptionUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EncryptionUtilImpl.class);

    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    public SecretKey getSecretKey(byte[] secretKey) {
        return new SecretKeySpec(secretKey, ALGORITHM);
    }

    public IvParameterSpec getSecretKeySpec(byte[] IV) {
        return new IvParameterSpec(IV);
    }


    public SecretKey generateSecretKey() {
        LOG.debug("Generating secret key");

        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new DomibusEncryptionException(String.format("No algorithm found for [%s]", ALGORITHM), e);
        }
        keyGenerator.init(256);
        final SecretKey secretKey = keyGenerator.generateKey();

        LOG.debug("Finished Generating secret key");
        return secretKey;
    }

    public byte[] generateIV() {
        LOG.debug("Generating initialization vector");

        byte[] IV = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);

        LOG.debug("Finished generating initialization vector");
        return IV;
    }

    public Cipher getEncryptCipher(SecretKey key, IvParameterSpec ivSpec) {
        //Get Cipher Instance
        Cipher cipher = getCipher();

        //Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), ALGORITHM);

        //Initialize Cipher for ENCRYPT_MODE
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new DomibusEncryptionException("Error initializing cipher for encrypt mode", e);
        }

        return cipher;
    }


    public Cipher getDecryptCipher(SecretKey key, IvParameterSpec ivSpec) {
        //Get Cipher Instance
        Cipher cipher = getCipher();

        //Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), ALGORITHM);

        //Initialize Cipher for DECRYPT_MODE
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new DomibusEncryptionException("Error initializing cipher for decrypt mode", e);
        }

        return cipher;
    }

    public byte[] encrypt(byte[] content, SecretKey key, IvParameterSpec ivSpec) {
        //Get Cipher Instance
        Cipher cipher = getEncryptCipher(key, ivSpec);

        //Perform Encryption
        byte[] cipherText = new byte[0];
        try {
            cipherText = cipher.doFinal(content);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new DomibusEncryptionException("Error while encrypting", e);
        }

        return cipherText;
    }

    public String decrypt(byte[] cipherText, SecretKey key, IvParameterSpec ivSpec) {
        //Get Cipher Instance
        Cipher cipher = getDecryptCipher(key, ivSpec);

        //Perform Decryption
        byte[] decryptedText = new byte[0];
        try {
            decryptedText = cipher.doFinal(cipherText);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new DomibusEncryptionException("Error while decrypting", e);
        }

        return new String(decryptedText);
    }

    protected Cipher getCipher() {
        try {
            return Cipher.getInstance(CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new DomibusEncryptionException(String.format("Error getting cipher [%s]", CIPHER), e);
        }
    }
}
