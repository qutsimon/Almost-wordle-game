# Almost wordle game #
A game similar to the popular wordle game where a 5-letter word is guessed. This implementation
of a game simiar to wordle includes both the server-side and client-side and includes several forms of security
to ensure fair play.  The impelemented measures can be found below:


* RSA key publickey handshake
* Message hash checks


![Test](../img/speed.png?raw=true)


# Game rules #
A 5-letter word is guessed and returns a hint where the letters of the target word are filled in 
if the letters are contained in the guess. When the word is guessed correctly the game ends and the number 
of incorrect attempts were made are returned. 

If the position of the guessed letter and the target letter coincide the letter in the hint is capitilised. 


# Usage #

# Protocol #
