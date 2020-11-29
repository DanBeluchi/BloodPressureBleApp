/**
 * Original source: https://github.com/rustemazimov/Hasher/blob/master/app/src/main/java/com/example/rustem/hasher/Function.java
 *
 * @author Rustem Azimov
 */
package com.example.BloodPressureBleApp.UserManagement;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordManager {

    private final int HASH_BYTES = 24;
    private final int PBKDF2_ITERATIONS = 1000;

    /**
     * Takes the entered password and hashes it. Returns a string in the format (iterations:salt:hash)
     *
     * @param password the password.
     *
     *@return formatted string (iterations:salt:hash)
     */
    public String hashPassword(String password) {
        return convertToPBKDF2(password.toCharArray());
    }

    /**
     * Takes the entered password and hashes it. After that the generated hash value is compared
     * the hashed password from the database.
     *
     * @param enteredPassword the password that was entered in plain text.
     * @param userPasswordFromDB hashed password from the database.
     *
     *@return true if the has value of the entered password equals the hashed password from the database.
     */
    public Boolean isPasswordValid(String enteredPassword, String userPasswordFromDB) {

        //split the string to get the salt that was used when the password was saved (iterations:salt:hash)
        String[] splittedPW = userPasswordFromDB.split(":");

        //salt was saved as hex on the second position
        byte[] salt = hexStringToByteArray(splittedPW[1]);

        String enteredPasswordHashed = convertToPBKDF2(enteredPassword.toCharArray(), salt);

        return userPasswordFromDB.equals(enteredPasswordHashed);


    }

    /**
     * Takes a password and PBKDF2 hashes it with an given salt.
     * After the password is hashed a string(format: (iterations:salt:hash)) is returned.
     *
     * @param password the password hash.
     * @param salt a given salt.
     *
     *@return formatted string (iterations:salt:hash)
     */
    private String convertToPBKDF2(char[] password, byte[] salt) {
        try {
            // Hash the password with the given salt
            byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTES);

            // format iterations:salt:hash
            return PBKDF2_ITERATIONS + ":" + convertByteToHex(salt) + ":" + convertByteToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
        }
        return null;
    }

    /**
     * Takes a password and PBKDF2 hashes it with a random generated salt.
     * After the password is hashed a string(format: (iterations:salt:hash)) is returned.
     *
     * @param password the password to hash.
     *
     *@return formatted string (iterations:salt:hash)
     */
    private String convertToPBKDF2(char[] password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            int SALT_BYTES = 24;
            byte[] salt = new byte[SALT_BYTES];
            random.nextBytes(salt);

            // Hash the password
            byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTES);

            // format iterations:salt:hash
            return PBKDF2_ITERATIONS + ":" + convertByteToHex(salt) + ":" + convertByteToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            //If the jvp is here  Your program is already broken :)))
        }
        return null;
    }

    /**
     * Takes a password, salt, iteration count, and
     * to-be-derived key length for generating PBEKey of variable-key-size
     * PBE ciphers.
     *
     * @param password the password.
     * @param salt the salt.
     * @param iterations the iteration count.
     * @param keyLength the to-be-derived key length.
     *
     *@return the encoded key, or null if the key does not support
     * encoding.
     *
     * @exception NoSuchAlgorithmException if no Provider supports a
     * SecretKeyFactorySpi implementation for the
     * specified algorithm.
     * @exception InvalidKeySpecException if the given key specification
     *  is inappropriate for this secret-key factory to produce a secret key.
     */
    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
        String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Converts a byte array to hex
     *
     * @param data byte array.
     *
     *@return string with the hex value of the byte array
     */
    private String convertByteToHex(byte[] data) {
        StringBuilder hexData = new StringBuilder();
        for (byte datum : data) {
            hexData.append(Integer.toString((datum & 0xff) + 0x100, 16).substring(1));
        }
        return hexData.toString();
    }

    /**
     * Converts a hex value to byte array
     *
     * @param hexString value as string
     *
     *@return converted hex value as a byte array
     */
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}

