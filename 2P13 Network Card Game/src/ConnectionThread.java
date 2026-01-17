import java.io.*;
import java.net.*;
import java.util.*;

/* COSC 2P13
 * Assignment 2
 *
 *
 * Username: ww23iu@brocku.ca
 * Student #: 7843469
 *
 * Due: March 28 @11:55pm
 * @version 5.2
 */


//Provides the game logic between the 2 players
public class ConnectionThread extends Thread{
    private final Socket client1, client2; //Players socket connections
    private char[][] gameBoard; //The game board
    private List<String> p1Deck,p2Deck; //Players deck of cards
    private int p1Score=0, p2Score=0; //Players score
    private int roundNum = 1; //Current round number
    private int currPlayer; //To track whose turn it is (0=player1, 1=player2)

    //Tracks if a player passed
    private boolean p1Passed = false;
    private boolean p2Passed = false;
    private final List<String> msgLog = new ArrayList<>(); //The message log

    //Mapping of which card beats which ("attack rules")
    private static final Map<Character, Set<Character>> atkRules =new HashMap<>();

    //Initializer for attack rules
    static{
        atkRules.put('A',Set.of('H')); //Axe beats Hammer
        atkRules.put('H',Set.of('S')); //Hammer beats Sword
        atkRules.put('S',Set.of('A')); //Sword beats Axe
        atkRules.put('R',Set.of('H','A','S')); //Arrow beats everything
    }

    //Map position letters to integer values
    private static final Map<Character,Integer> positionNum = Map.of(
            'A', 0,'B',1,'C',2,'D',3,'E',4,'F',5
    );


    /**
     *@constructor initializes a game with 2 players connected
     * @param c1 socket for player 1
     * @param c2 socket for player 2
     */
    public ConnectionThread(Socket c1, Socket c2){
        client1 = c1;
        client2 = c2;
        gameSetup();
    }


    /**
     * @method creates ASCII art for cards
     * @param weapon the character of the weapon (H, R, S, A)
     * @param row which row of the ASCII is being created (0-2)
     * @return string of a portion of the ASCII art
     */
    private String cardASCII(char weapon, int row){
        switch (weapon){
            case 'H':
                return (row==0)?"[=]":" | ";
            case 'R':
                return (row==0)?" ^ ":(row==1)?" | ":"/^\\";
            case 'S':
                return (row==0)?"  /":(row==1)?" / ":"x  ";
            case 'A':
                return (row==0)?"<7>":(row==1)?" I ":" L ";
            default:
                return "   ";
        }
    }


    /**
     * @method determines if the attack results in giving the player a point
     * @param attacker the attacking card
     * @param target the card getting attacked
     * @return true if the attack is valid and scores a point (based on given rules), else: false
     */
    private boolean givePlayerPoint(char attacker, char target){

        //Arrows don't earn points
        if(attacker=='R') return false;

        //Ensure the elimination is valid
        return atkRules.containsKey(attacker) && atkRules.get(attacker).contains(target);
    }


    /**
     * @method displays a message in MESSAGE LOG box
     * @param message the message to display
     */
    private void displayMessage(String message){
        msgLog.clear();
        msgLog.add(message);
    }


    /**
     * @method sets up the game for a new round
     */
    private void gameSetup(){

        //Each users deck of cards(3 cards for each weapon type)
        List<Character> deck = new ArrayList<>(Arrays.asList('A','A','A','H','H','H','R','R','R','S','S','S'));
        Collections.shuffle(deck);
        p1Deck = new ArrayList<>();
        p2Deck = new ArrayList<>();
        for(int i=0 ; i<6 ; i++){
            p1Deck.add(deck.get(i).toString());
            p2Deck.add(deck.get(i+6).toString());
        }

        //Initialize game board with player cards
        gameBoard = new char[2][6];
        for(int i=0 ; i<6 ; i++){
            gameBoard[0][i] = p1Deck.get(i).charAt(0);
            gameBoard[1][i] = p2Deck.get(i).charAt(0);
        }

        //Display the NEW GAME message in MESSAGE LOG box
        msgLog.clear();
        displayMessage("NEW GAME                             |");
    }


    /**
     * @method adjusts variables to start a new round
     */
    private void newRound(){
        gameSetup();
        roundNum++;
        p1Passed = false;
        p2Passed = false;
        displayMessage("NEW ROUND STARTED                    |");
    }


    /**
     * @method clears any queued input from user to avoid players making moves out of turn
     * @param in1 BufferedReader for player 1
     * @param in2 BufferedReader for player 2
     * @throws IOException error if there is an error reading in the streams
     */
    private void clearQueue(BufferedReader in1, BufferedReader in2) throws IOException{
        while(in1.ready())in1.readLine();
        while(in2.ready())in2.readLine();
    }


    /**
     * @method main game execution
     */
    public void run(){
        try(
                //I/O streams for players 1 and 2
                PrintWriter outP1 = new PrintWriter(client1.getOutputStream(),true);
                PrintWriter outP2 = new PrintWriter(client2.getOutputStream(),true);
                BufferedReader inP1 = new BufferedReader(new InputStreamReader(client1.getInputStream()));
                BufferedReader inP2 = new BufferedReader(new InputStreamReader(client2.getInputStream()));
        ){

            //Initial game state
            visualizeGame(outP1,true);
            visualizeGame(outP2,false);
            boolean gameOver = false;
            String pMove; //Player move (2 characters)

            while (!gameOver){

                //Current players' streams
                PrintWriter currOut = (currPlayer==0)?outP1:outP2;
                BufferedReader currIn = (currPlayer==0)?inP1:inP2;

                //Clear any queued input
                clearQueue(inP1,inP2);

                //Get player move in all caps
                pMove = null;
                while (pMove==null && !gameOver){
                    if(currIn.ready()){
                        pMove=currIn.readLine();
                        if(pMove!=null){
                            pMove = pMove.toUpperCase().trim();
                            break;
                        }
                    }
                }
                if(gameOver)break;

                //If the current player has no more valid moves, automatically pass their turn
                if(!hasValidMove(currPlayer)){
                    displayMessage("PLAYER "+(currPlayer+1)+" HAS NO MOVES: AUTO-PASS  |");
                    currPlayer = 1-currPlayer; //switch turn
                    visualizeGame(outP1,true);
                    visualizeGame(outP2,false);
                    continue;
                }

                //If someone passes, pass their turn
                if(pMove.equalsIgnoreCase("PS")){
                    displayMessage("PLAYER " + (currPlayer + 1) + " PASSED                      |");
                    if(currPlayer==0)p1Passed=true;
                    else p2Passed=true;

                    //Check if both players passed back-to-back
                    if(p1Passed && p2Passed){
                        if(roundNum>=4) {
                            displayMessage("BOTH PLAYERS PASSED: GAME OVER        |");
                            gameOver = true;
                        }else{
                            newRound();
                        }
                    }

                    //Switch turn
                    currPlayer =1-currPlayer;
                    visualizeGame(outP1,true);
                    visualizeGame(outP2,false);
                    continue;
                }

                //Check if round limit reached
                if(roundNum>4)gameOver=true;

                //Process player attack (2 character string input)
                if(pMove.length()==2 && positionNum.containsKey(pMove.charAt(0)) && positionNum.containsKey(pMove.charAt(1))){
                    int atkPos = positionNum.get(pMove.charAt(0));
                    int tarPos = positionNum.get(pMove.charAt(1));
                    char attacker = gameBoard[currPlayer][atkPos];
                    char target = gameBoard[1-currPlayer][tarPos];

                    //Ensure attack is valid
                    if(validAttack(attacker,target)){

                        //Give player point if not arrow
                        if(givePlayerPoint(attacker,target)){
                            if(currPlayer==0)p1Score++;
                            else p2Score++;
                        }

                        //Remove targeted card
                        removeCard(tarPos,1-currPlayer);

                        //Display attack message
                        String atkMessage;
                        switch ("" + attacker + target) {
                            case "HS": atkMessage = "HAMMER TAKES SWORD                   |";break;
                            case "AH": atkMessage = "AXE TAKES HAMMER                     |"; break;
                            case "SA": atkMessage = "SWORD TAKES AXE                      |"; break;
                            case "RR": atkMessage = "ARROW TAKES ARROW                    |"; break;
                            case "RH": atkMessage = "ARROW TAKES HAMMER                   |"; break;
                            case "HR": atkMessage = "HAMMER TAKES ARROW                   |"; break;
                            case "RA": atkMessage = "ARROW TAKES AXE                      |"; break;
                            case "AR": atkMessage = "AXE TAKES ARROW                      |"; break;
                            case "RS": atkMessage = "ARROW TAKES SWORD                    |"; break;
                            case "SR": atkMessage = "SWORD TAKES ARROW                    |"; break;
                            default: atkMessage = "UNKNOWN MOVE                         |";
                        }
                        displayMessage(atkMessage);

                        //Reset pass indicators
                        if(currPlayer==0)p1Passed=false;
                        else p2Passed=false;

                        //Check if round is over
                        if(roundNum<4 && (noValidMoves() || oneCardLeft())){
                            newRound();
                            currPlayer=1-currPlayer;
                            visualizeGame(outP1,true);
                            visualizeGame(outP2,false);
                            continue;
                        }

                        //Check if game is over
                        if(oneCardLeft()||p1Score>=9||p2Score>=9||noValidMoves()){
                            endMessage(attacker,target);
                            gameOver=true;
                        }

                        //Switch player turn
                        currPlayer=1-currPlayer;
                    }else{
                        displayMessage("INVALID MOVE                         |");
                    }
                }else{
                    displayMessage("SYNTAX ERROR                         |");
                }

                //Update game board for both clients
                visualizeGame(outP1,true);
                visualizeGame(outP2,false);

                //Game end check
                if(roundNum>=4 && (p1Score>=9||p2Score>=9||oneCardLeft()||noValidMoves()||bothPassed())){
                    displayMessage("GAME OVER                             |");
                    gameOver=true;
                }
            }
        }catch (IOException e){
            System.out.println("CONNECTION LOST                      |");
        }
    }


    /**
     * @method displays the game end message based on the attack
     * @param attacker the attacking card
     * @param target the card being attacked
     */
    private void endMessage(char attacker, char target){
        String messageEnd;
        switch ("" + attacker + target) {
            case "HS": messageEnd = "HAMMER TAKES SWORD: GAME OVER        |"; break;
            case "AH": messageEnd = "AXE TAKES HAMMER: GAME OVER          |"; break;
            case "SA": messageEnd = "SWORD TAKES AXE: GAME OVER           |"; break;
            case "RR": messageEnd = "ARROW TAKES ARROW: GAME OVER         |"; break;
            case "RH": messageEnd = "ARROW TAKES HAMMER: GAME OVER        |"; break;
            case "HR": messageEnd = "HAMMER TAKES ARROW: GAME OVER        |"; break;
            case "RA": messageEnd = "ARROW TAKES AXE: GAME OVER           |"; break;
            case "AR": messageEnd = "AXE TAKES ARROW: GAME OVER           |"; break;
            case "RS": messageEnd = "ARROW TAKES SWORD: GAME OVER         |"; break;
            case "SR": messageEnd = "SWORD TAKES ARROW: GAME OVER         |"; break;
            default: messageEnd = "UNKNOWN MOVE                         |";
        }
        displayMessage(messageEnd);
    }


    /**
     * @method checks if either player has no valid moves left
     * @return true if neither player has a valid move left, else: false
     */
    private boolean noValidMoves(){
        return !hasValidMove(0) || !hasValidMove(1);
    }


    /**
     * @method checks if 1 player has any valid moves
     * @param player the player that is being checked (0 for player 1, 1 for player 2)
     * @return true of player has at least 1 valid move, else: false
     */
    private boolean hasValidMove(int player){

        //Check all possible attacker-target combinations on the board
        for(int i=0 ; i<6 ; i++){
            char attacker = gameBoard[player][i];
            if (attacker==' ')continue; //Skip empty cards
            for(int j=0 ; j<6 ; j++){
                char target = gameBoard[1-player][j];
                if(target == ' ')continue;
                if(validAttack(attacker, target)){
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * @method checks if one player has only 1 card left, and the other has 0
     * @return true if player has only 1 card left and the other has 0, else: false
     */
    private boolean oneCardLeft(){
        int p1Cards = numCardsLeft(0);
        int p2Cards = numCardsLeft(1);
        return (p1Cards==1 && p2Cards==0) || (p1Cards==0 && p2Cards==1);
    }


    /**
     * @method counts the number of cards a player has left
     * @param player the player whose cards are being counted (0 for player 1, 1 for player 2)
     * @return number of cards the player has left
     */
    private int numCardsLeft(int player){
        int cardCount = 0;
        for(int i=0 ; i<6 ; i++){
            if(gameBoard[player][i]!=' '){
                cardCount++;
            }
        }
        return cardCount;
    }


    /**
     * @method checks if both players passed
     * @return true if both players passed, else: false
     */
    private boolean bothPassed(){
        return p2Passed&&p1Passed;
    }


    /**
     * @method removes a card from the game board
     * @param cardPos position of the card to remove (0-5)
     * @param pCard player whose card is being removed (0 for player 1, 1 for player 2)
     */
    private void removeCard(int cardPos,int pCard){
        gameBoard[pCard][cardPos] = ' ';
    }


    /**
     * @method ensures an attack is valid according to the game rules
     * @param attacker the card attacking
     * @param target the card being attacked
     * @return true if attack is valid, else: false
     */
    private boolean validAttack(char attacker, char target){
        if(target=='R')return true;
        if(attacker=='R')return true;
        return atkRules.containsKey(attacker)&&atkRules.get(attacker).contains(target);
    }


    /**
     * @method displays the gameboard to user in ASCII format
     * @param out the players output stream
     * @param isPlayerOne player 1:true, player2:false
     */
    private void visualizeGame(PrintWriter out, boolean isPlayerOne){
        StringBuilder s = new StringBuilder();
        s.append("/--------------------------------------\\\n");
        s.append("   A    B    C    D    E    F   \n");

        //Check if the game has ended
        boolean gameOver = roundNum>4||p1Score>=9||p2Score>=9||(roundNum==4&&bothPassed());

        s.append(" /---\\/---\\/---\\/---\\/---\\/---\\       ");
        s.append(gameOver?"-\n": ((isPlayerOne&&currPlayer==0)||(!isPlayerOne&&currPlayer==1)?"-\n":"^\n"));

        //Opponents card ASCII art
        for(int i=0 ; i<3; i++){
            s.append(" |");
            for(int j=0 ; j<6 ; j++){
                s.append(cardASCII(isPlayerOne?gameBoard[1][j]:gameBoard[0][j], i));
                s.append(j < 5 ? "||" : "|");
            }
            if(i == 1){
                s.append("       ").append(gameOver?"-":((isPlayerOne && currPlayer == 0) || (!isPlayerOne && currPlayer == 1)? "v" : "-"));
            }
            if(i == 0)s.append("       |");
            s.append("\n");
        }

        //Middle portion (constant except scores and round number)
        s.append(" \\---/\\---/\\---/\\---/\\---/\\---/       \n");
        s.append("                                     [").append(isPlayerOne?p2Score:p1Score).append("]\n");
        s.append("<==================================> R").append(roundNum).append("\n");
        s.append("                                     [").append(isPlayerOne?p1Score:p2Score).append("]\n");
        s.append(" /---\\/---\\/---\\/---\\/---\\/---\\   \n");

        //Players card ascii art
        for(int i=0 ; i<3 ; i++){
            s.append(" |");
            for(int j=0 ; j<6 ; j++){
                s.append(cardASCII(isPlayerOne?gameBoard[0][j] : gameBoard[1][j], i));
                s.append(j<5? "||" : "|");
            }
            if(i==1){
                s.append("       ").append(gameOver?"-" : ((isPlayerOne && currPlayer == 0) || (!isPlayerOne && currPlayer == 1)? "-" : "^"));
            }
            if(i==2)s.append("       |");
            s.append("\n");
        }


        s.append(" \\---/\\---/\\---/\\---/\\---/\\---/       ");
        s.append(gameOver?"-\n" : ((isPlayerOne && currPlayer == 0) || (!isPlayerOne && currPlayer == 1)? "v\n" : "-\n"));
        s.append("   A    B    C    D    E    F   \n");

        //MESSAGE LOG
        s.append("|--------------------------------------|\n");
        if(!msgLog.isEmpty()){
            s.append("| ").append(msgLog.get(0)).append("\n");
        }
        s.append("\\--------------------------------------/\n");

        //Send game board to players client
        out.println(s.toString());
    }
}
