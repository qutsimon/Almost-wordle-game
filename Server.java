import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.crypto.Cipher;
import java.security.*;


public class Server {
    
    
    /**
     * Gets a word from the target.txt and returns a random word
     * @return A random word from the target.txt file
     */
    public static String getWord(){
        String line = null;
        try {
            File file = new File("target.txt");
            RandomAccessFile f = new RandomAccessFile(file, "r");
            long random = (long) (Math.random() * f.length());
            f.seek(random);
            f.readLine();
            line = f.readLine();
            f.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        
        return line;
    }

    /**
     * Takes the guess.txt file and returns an ArrayList<String> object.
     * @return ArrayList<String> with all lines in guess.txt
     * @throws IOException
     */
    public static ArrayList<String> returnTargetArray() throws IOException{
        BufferedReader file_buf = new BufferedReader(new FileReader("guess.txt"));
        ArrayList<String> lines = new ArrayList<String>();
        String l;
        while ((l = file_buf.readLine()) != null){
                lines.add(l);
        }

        file_buf.close();
        return lines;
    }

    public static void main(String args[]){
        int port = 0;
        ServerSocket serverSocket = null;
        Socket socket = null;
        ArrayList<String> target = null;
        String word;
        KeyPair pair = null;
        

        //check if port number is a number
        try{
            port = Integer.parseInt(args[0]);
        } 
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Port number is invalid");
            return;
        }

        //Start server at port if it is a valid number
        try{
            serverSocket = new ServerSocket(port);
            target = returnTargetArray();
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        //Generate RSA key pair here
        try{
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            pair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return;
        }

        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        while(true){
            try{
                socket = serverSocket.accept();
            } catch(IOException io){
                io.printStackTrace();
            }
            word = getWord();
            ServerThread thread = new ServerThread(socket, target, word, publicKey, privateKey);
            thread.start();
        }
        


    }
    
}


/**
 * Server threads for multiple concurrent clients
 */
class ServerThread extends Thread{
    final int HASH_NO = 100;
    final int WORD_LENGTH = 5;
    protected Socket socket;
    ArrayList<String> target;
    String word;
    PublicKey puKey;
    PrivateKey prKey;
    

    public ServerThread(Socket clientSocket, ArrayList<String> clientTarget, String clientWord,
        PublicKey serverPuKey, PrivateKey  serverPrKey){

        this.socket = clientSocket;
        this.target = clientTarget;
        this.word = clientWord;
        this.puKey = serverPuKey;
        this.prKey = serverPrKey;

    }

    /**
     * Checks to see if the guess is contained with the valid word list
     * @param line The line sent by the clinet to compare
     * @param guess_list the ArrayList containing all the accepted guesses
     * @return True if the ArrayList cotains the line. False otherwise.
     */
    public boolean isValid(String line, ArrayList<String> guess_list){
        boolean in = false;
        if(guess_list.contains(line) && line.length() == WORD_LENGTH){
            in = true;
        }

        return in; 
    }
    

    /**
     * Compares the client's guess to the word and returns the hint
     * @param guess The guess that the client made.
     * @param word The client's chosen word.
     * @return Returns a string of the new hint based on the client's guess.
     * @throws IndexOutOfBoundsException
     */
    public String CompareGuess(String guess, String word) throws IndexOutOfBoundsException {
        char[] hint = new char[WORD_LENGTH];
        guess = guess.toUpperCase();
        for (int i=0; i<WORD_LENGTH; i++)
            hint[i] = '_';
        for (int i=0; i < WORD_LENGTH; i++){
            for (int j=0; j<WORD_LENGTH; j++){
                if (guess.charAt(i) == word.charAt(j)){
                    if (hint[j] == '_'){
                        if (i==j){
                            hint[j] = word.charAt(j);
                        } else {
                            hint[j] = Character.toLowerCase(word.charAt(j));
                        }
                    }
                }
            }
        }
        return String.valueOf(hint);
    }

    



    public void run(){
        String guess = "_____", message="";
        int counter = 1;
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        boolean fin = false;
        PublicKey clientKey = null;
        byte[][] messageStorage = new byte[2][]; //used to send cipher as well as hashed signature
        Cipher eCipher = null, dCipher = null, ceCipher=null, privateECipher = null, cdCipher = null;
        MessageDigest digest = null; //not thread safe so it is here
        
        
        System.out.println("Client Connected");

        try{
            ois = new ObjectInputStream(this.socket.getInputStream());
            oos = new ObjectOutputStream(this.socket.getOutputStream());

            
            //send server public key and get client public key here - need to send size first
            //get client key
            clientKey = (PublicKey) ois.readObject();
            //write key
            oos.writeObject(this.puKey);

            //cipher setup
            eCipher = Cipher.getInstance("RSA");
            dCipher = Cipher.getInstance("RSA");
            ceCipher = Cipher.getInstance("RSA");
            privateECipher = Cipher.getInstance("RSA");
            cdCipher = Cipher.getInstance("RSA");
            dCipher.init(Cipher.DECRYPT_MODE, this.prKey); //server private key
            eCipher.init(Cipher.ENCRYPT_MODE, this.puKey); //server public key
            privateECipher.init(Cipher.ENCRYPT_MODE, this.prKey); // server privatekey encrypt
            ceCipher.init(Cipher.ENCRYPT_MODE, clientKey); //client public key
            cdCipher.init(Cipher.DECRYPT_MODE, clientKey); //client public key decrypt for hash 

            //hashing instance init
            digest = MessageDigest.getInstance("SHA3-256");

            //send the number of times the word is hashed here
            SecuFunctions.sendMessage(oos, Integer.toString(HASH_NO), privateECipher, ceCipher, digest);

            //send the hash of the chosen word here
            byte[] wordHashStorage= word.getBytes(StandardCharsets.UTF_8);
            for (int i=0; i < HASH_NO; i++)
                wordHashStorage = digest.digest(wordHashStorage);
            String wordHash = new String(wordHashStorage);
            SecuFunctions.sendMessage(oos, wordHash, privateECipher, ceCipher, digest);
 
            //send the first hint of the game
            SecuFunctions.sendMessage(oos, guess, privateECipher, ceCipher, digest);
            System.out.println("The word is: "+ word);

            //get the start game message here
            messageStorage = (byte[][]) ois.readObject();
            message = SecuFunctions.decryptCipher(messageStorage, dCipher);

        }  catch (Exception e){
            e.printStackTrace();
        } 

        
        
        if (message.equals("START GAME")){
            while(!fin){
                try {
                    messageStorage = (byte[][]) ois.readObject();
                    message = SecuFunctions.decryptCipher(messageStorage, dCipher);
                    //signature check
                    if (!SecuFunctions.hashCheck(digest, messageStorage, cdCipher, message))
                        break;
                    if (isValid(message, target)){
                        if (message.equals(word)){
                            SecuFunctions.sendMessage(oos, Integer.toString(counter), privateECipher, ceCipher, digest);
                            SecuFunctions.sendMessage(oos, "GAME OVER", privateECipher, ceCipher, digest);
                            fin = true;
                        } else{
                            guess = CompareGuess(message, word);
                            SecuFunctions.sendMessage(oos, guess, privateECipher, ceCipher, digest);
                            counter++;
                        }
                    } else {
                        SecuFunctions.sendMessage(oos, "INVALID GUESS", privateECipher, ceCipher, digest);
                    }
                } catch (IOException e) { 
                    e.printStackTrace();
                    System.out.println("Failed to read/write to input/output stream");
                    fin = true;
                } catch (IndexOutOfBoundsException e){
                    e.printStackTrace();
                    System.out.println("Failed to compare guess");
                } catch (Exception e){
                    e.printStackTrace();
                } 
            }
        }

        //Closing socket, input and outputstream
        try{
            socket.close();
            oos.close();
            ois.close();
        } catch (IOException io){
            System.out.println("Failed close");
            io.printStackTrace();
        }
        

    }
}
