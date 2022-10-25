# Almost wordle game #
A game similar to the popular wordle game where a 5-letter word is guessed. This implementation
of the game is  similar to wordle includes both the server-side and client-side and includes several forms of security
to ensure fair play.  The impelemented measures can be found below:


* RSA key publickey handshake
* Message hash checks
* Multiple hashes 

<br>


Non-security additonall implementations include:
* Multi-threaded server to host multiple concurrent clients

<br>

* Server running Example
<br>

![Server example image](../img/serverexample.png?raw=true)

<br>

* Client running example
<br>

![Client example image](../img/clientexample.png?raw=true)


# Game rules #
A 5-letter word is guessed and returns a hint where the letters of the target word are filled in 
if the letters are contained in the guess. When the word is guessed correctly the game ends and the number 
of attempts were made are returned. 

If the position of the guessed letter and the target letter coincide the letter in the hint is capitilised. 


# Usage #

* Starting the server
```
javac Server.java
java Server PORT_NUMBER
```
* Starting the client
```
javac Client.java
java Client HOSTNAME PORT_NUMBER
```


# Protocol #

* Initial connection protocol

| Server | Client |
| :--- | ---: |
|  | Connects to server
| | Client publickey |
| | <--------------|
| Server publickey | |
| Number of hashes for chosen word | |
| Chosen word hash | |
| First hint of the game | |
| -------------------> | |
| | Start game keyword |
| | <-------------- |

<br>

All message except for the initial key exchange will also be in the form of a multi-dimensional byte array[][] with two rows. The first row (array[0][]) will contain the ciphertext of the message encrypted using the public key of the receiver and the second row of the array (array[1][]) will contain the plaintext hashed (using SHA3-256) then encrypted using the sender’s private key to create a signature. The server will check every message’s signature to see if it is valid. If the signature is not valid the server will terminate the connection.  

<br>

The server will also send a hash of the word that is chosen for the client to guess. This is hashed a number of times equal to what it is set on the server side. The number of times this hash has been repeated is sent to the client before the chosen word hash is sent. The chosen word is the only hash that is done this number of times. The signature hashes are only hashed once. 

<br>
  
* Word guessing protocol

| Server | Client |
| :--- | ---: |
| | Client's guess|
| | <-------------- |
| Hint based on the client’s guess | |
| -------------------> | |
| **Continued until correct answer** | |  

<br>

* Correct guess is made

| Server | Client |
| :--- | ---: |
| | Correct guess made by client |
| | <-------------- |
| Counter of number of guesses made | |
| Game over message | |
| -------------------> | |
| Close connection | Close connection |
