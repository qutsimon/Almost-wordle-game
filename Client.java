import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class Client {
    

    /**
     * Checks to see if the wordhash array is equal to the line string
     * @param digest Instance to create and check hash.
     * @param line Guess that client made that needs to be checked.
     * @param wordHash The hash of the word chosen by and sent by the server
     * @return Returns true if the hashes match. False otherwise.
     */
    public static boolean wordHashCheck(MessageDigest digest, String line, byte[] wordHash, int hashNumber){
        byte[] selfHash = line.getBytes(StandardCharsets.UTF_8);
        for (int i=0; i<hashNumber; i++)
            selfHash = digest.digest(selfHash);
        return Arrays.equals(selfHash, wordHash);
    }
    
    
    /**
     * Takes input from console and checks to see if the word length is correct.
     * If incorrect it asks for new input.
     * @param in Input reader 
     * @return Returns a valid guess in all uppercase.
     * @throws IOException
     */
    public static String guess_input(BufferedReader in)throws IOException{
        final int WORD_LENGTH = 5;
        String guess = in.readLine();
        boolean good = false;

        while (!good){
            if (guess.length() == WORD_LENGTH && guess.toUpperCase().matches("[A-Z]+")){
                good = true;
            }
            else {
                System.out.println("Guess format is wrong. 5 letters, alphabet only.");
                guess = in.readLine();
            }
        }

        return guess.toUpperCase();
    }
    //need to  fix exception stuff - try catch
    public static void main(String args[]){
        //parsing arguments
        int port = 0, hashNumber = 0;
        String address = args[0];
        String line = "", s_line = "";
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        BufferedReader in = null;
        KeyPair pair = null;
        PublicKey serverKey = null;
        Cipher eCipher = null, dCipher = null, seCipher=null, privateECipher =null, sdCipher = null;
        MessageDigest digest = null;
        byte[][] messageStorage = new byte[2][];
        byte[] wordHash = null;


        //check if port number is a number
        try{
            port = Integer.parseInt(args[1]);
        } 
        catch (NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Port number is invalid");
        }

        //generate RSA keypair here, init ciphers, init messagedigest
        try{
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            pair = generator.generateKeyPair();
            eCipher = Cipher.getInstance("RSA");
            dCipher = Cipher.getInstance("RSA");
            seCipher = Cipher.getInstance("RSA");
            privateECipher = Cipher.getInstance("RSA");
            sdCipher = Cipher.getInstance("RSA");
            digest = MessageDigest.getInstance("SHA3-256");
            
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return;
        } catch (NoSuchPaddingException pad){
            pad.printStackTrace();
        }

        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        
        //Establishing connection - key handshake, setting up input/output streams, cipher init, send start message
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");

            in = new BufferedReader(new  InputStreamReader(System.in));
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            //send and get key here
            //send first
            oos.writeObject(publicKey);
            //get server key
            serverKey = (PublicKey) ois.readObject();
            
            //SETUP CIPHERS HER
            dCipher.init(Cipher.DECRYPT_MODE, privateKey); //client privatekey
            eCipher.init(Cipher.ENCRYPT_MODE, publicKey); // client publickey
            seCipher.init(Cipher.ENCRYPT_MODE, serverKey); //server publickey
            privateECipher.init(Cipher.ENCRYPT_MODE, privateKey); //client privatekey encryption mode
            sdCipher.init(Cipher.DECRYPT_MODE, serverKey); //server publickey decrypt

            //get hash number here
            messageStorage = (byte[][]) ois.readObject();
            hashNumber = Integer.parseInt(SecuFunctions.decryptCipher(messageStorage, dCipher));
            if (!SecuFunctions.hashCheck(digest, messageStorage, sdCipher, SecuFunctions.decryptCipher(messageStorage, dCipher)))
                    return;

            //get hashed word here
            messageStorage = (byte[][]) ois.readObject();
            String hashString = SecuFunctions.decryptCipher(messageStorage, dCipher);
            wordHash = hashString.getBytes(StandardCharsets.UTF_8);
            if (!SecuFunctions.hashCheck(digest, messageStorage, sdCipher, hashString))
                    return;
            
            //writing start game to the server to begin game
            SecuFunctions.sendMessage(oos, "START GAME", privateECipher, seCipher, digest);

      
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed setting up socket and/or streamreaders");
        } catch (Exception e){
            e.printStackTrace();
            return;
        }
        
        try {
            while (true){
                messageStorage = (byte[][]) ois.readObject();
                s_line = SecuFunctions.decryptCipher(messageStorage, dCipher);
                System.out.println(s_line);
                if (!SecuFunctions.hashCheck(digest, messageStorage, sdCipher, s_line))
                    break;
                //check to see if counter has been returned
                if (s_line.matches("\\d+")){
                    messageStorage = (byte[][]) ois.readObject();
                    s_line = SecuFunctions.decryptCipher(messageStorage, dCipher);
                    if (!SecuFunctions.hashCheck(digest, messageStorage, sdCipher, s_line))
                        break;
                    System.out.println(s_line);
                    break;
                }
                else{
                    if (wordHashCheck(digest, line, wordHash, hashNumber)){
                        System.out.println("Server is trying to cheat. BYE BYE.");
                        break;
                    } else {
                        line = guess_input(in);
                        SecuFunctions.sendMessage(oos, line, privateECipher, seCipher, digest);
                    }
                }
                
    
            }
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed reading or writing to input/output stream");
        } catch (Exception e){ //need to catch exceptions for decrypt as well as input handling
            e.printStackTrace();
        }
        

        try {
            oos.close();
            ois.close();
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed closing");
        }


    }
}