import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.*;

public class SecuFunctions {

     /**
     *  Takes the received byte[][] and decrypts the cipher text and returns it as a string
     * @param messageStorage Contains the ciphertext and the sender's signature
     * @param decryptCipher Cipher to decrypt ciphertext - own privatekey
     * @return Returns the plaintext obtained from the ciphertext.
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String decryptCipher(byte[][] messageStorage, Cipher decryptCipher) 
        throws BadPaddingException, IllegalBlockSizeException{

        String plain;
        byte[] storage = decryptCipher.doFinal(messageStorage[0]);
        plain = new String(storage, StandardCharsets.UTF_8);
        return plain;
    }

      /**
     * Checks to see if the recieved hash signature is correct
     * @param digest Instance to create and check hash.
     * @param messageStorage Contains the ciphertext and the sender's signature
     * @param decryptCipher PublicKey cipher of the client
     * @param plain plain text that was receieved
     * @return Returns true if the signature matches. False otherwise.
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static boolean hashCheck(MessageDigest digest, byte[][] messageStorage, Cipher decryptCipher, String plain)
        throws BadPaddingException, IllegalBlockSizeException{
        
        byte[] hashStorage = decryptCipher.doFinal(messageStorage[1]);
        byte[] selfHash = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
        boolean value = Arrays.equals(hashStorage, selfHash);
        if (!value)
            System.out.println("Hashed signature is bad");
        return value;
    }

       /**
    * Writes a byte[][] to the objectoutputstream with ciphertext and encrypted hash signature
    * @param oos Outputstream to write to.
    * @param message The message to be encrypted and written.
    * @param myEncryptCipher own privatekey to encrypt the hash signature
    * @param clientCipher publickey of the otherside to encrypt the message
    * @param digest Instance to create and check hash.
    * @throws BadPaddingException
    * @throws IllegalBlockSizeException
    * @throws IOException
    */
    public static void sendMessage(ObjectOutputStream oos, String message, Cipher myEncryptCipher, 
        Cipher clientCipher, MessageDigest digest)
        throws BadPaddingException, IllegalBlockSizeException, IOException{

        byte[][] output = new byte[2][];
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        output[0] = clientCipher.doFinal(messageBytes);
        output[1] = myEncryptCipher.doFinal(digest.digest(messageBytes));
        oos.writeObject(output);
        return;
    }


    
}
