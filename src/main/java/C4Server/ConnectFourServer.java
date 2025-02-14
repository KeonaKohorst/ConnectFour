package C4Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The strings that are sent in TTTP (Tic Tac Toe Protocol are:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 *
 * It allows an unlimited number of pairs of players to play.
 */

public class ConnectFourServer {

    /**
     * Runs the application. Pairs up clients that connect.
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Connect Four Server is Running");
        try {
            while (true) {
                Game game = new Game();
                Game.Player playerX = game.new Player(listener.accept(), 'X');
                Game.Player playerO = game.new Player(listener.accept(), 'O');
                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX;
                playerX.start();
                playerO.start();
            }
        } finally {
            listener.close();
        }
    }
}

/**
 * A two-player game.
 */
class Game {

    /**
     * A board has nine squares.  Each square is either unowned or
     * it is owned by a player.  So we use a simple array of player
     * references.  If null, the corresponding square is unowned,
     * otherwise the array cell stores a reference to the player that
     * owns it.
     */
    private Player[][] board = new Player[6][7];

    /**
     * The current player.
     */
    Player currentPlayer;

    /**
     * Returns whether the current state of the board is such that one
     * of the players is a winner.
     */
    public boolean hasWinner() {
  
        // Check horizontal
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) { // Only need to check up to col 3
                if (board[row][col] != null &&
                    board[row][col] == board[row][col+1] &&
                    board[row][col] == board[row][col+2] &&
                    board[row][col] == board[row][col+3]) {
                    return true;
                }
            }
        }

        // Check vertical
        for (int row = 0; row < 3; row++) { // Only need to check up to row 2
            for (int col = 0; col < 7; col++) {
                if (board[row][col] != null &&
                    board[row][col] == board[row+1][col] &&
                    board[row][col] == board[row+2][col] &&
                    board[row][col] == board[row+3][col]) {
                    return true;
                }
            }
        }

        // Check diagonal (bottom-left to top-right)
        for (int row = 3; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] != null &&
                    board[row][col] == board[row-1][col+1] &&
                    board[row][col] == board[row-2][col+2] &&
                    board[row][col] == board[row-3][col+3]) {
                    return true;
                }
            }
        }

        // Check diagonal (top-left to bottom-right)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] != null &&
                    board[row][col] == board[row+1][col+1] &&
                    board[row][col] == board[row+2][col+2] &&
                    board[row][col] == board[row+3][col+3]) {
                    return true;
                }
            }
        }

        return false;
        

    }

    /**
     * Returns whether there are no more empty squares.
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[i].length; j++){
                if (board[i][j] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Called by the player threads when a player tries to make a
     * move.  This method checks to see if the move is legal: that
     * is, the player requesting the move must be the current player
     * and the square in which she is trying to move must not already
     * be occupied.  If the move is legal the game state is updated
     * (the square is set and the next player becomes current) and
     * the other player is notified of the move so it can update its
     * client.
     */
    public synchronized boolean legalMove(int[] location, Player player) {
        int row = location[0];
        int col = location[1];
        
        boolean columnHasAnEmpty = false;
        for(int i = 0; i < board.length; i++){
            if(board[i][col] == null){
                columnHasAnEmpty = true;
                break;
            } 
        }
        
        if (player == currentPlayer && columnHasAnEmpty) {
            int lowestRow = getLowestAvailableRow(row, col);
            board[lowestRow][col] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            printBoard();
            System.out.println("Setting other player moved: " + lowestRow + " " + col);
            currentPlayer.otherPlayerMoved(new int[]{lowestRow, col});
            return true;
        }
        return false;
    }
    
    private void printBoard(){
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                if(board[i][j] != null){
                    System.out.print(board[i][j].toString() + " ");
                }else{
                    System.out.print("_ ");
                }
            }
            System.out.println();
        }
    }
    
    private int getLowestAvailableRow(int row, int col){
        int lowestRow = board.length-1;
      
        for(int i = board.length-1; i > -1; i--){
            if(board[i][col] == null){
                //lowestRow = i;
                return i;
            }
        }
        return lowestRow;
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either 'X' or 'O'.  For communication with the
     * client the player has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */
    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        /**
         * Constructs a handler thread for a given socket and mark
         * initializes the stream fields, displays the first two
         * welcoming messages.
         */
        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        /**
         * Accepts notification of who the opponent is.
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Handles the otherPlayerMoved message.
         */
        public void otherPlayerMoved(int[] location) {
            String loc = location[0] + ":" + location[1];
            System.out.println("Telling Client that The other player moved to " + location[0] + " " + location[1]);
            output.println("OPPONENT_MOVED " + loc);
            output.println(
                hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }
        
        /**
         * Tells the client where the opponents mouse is
         * @param x
         * @param y 
         */
        public void sendOpponentMousePosition(int x, int y) {
            // Send the opponent's mouse position to the other player
            opponent.output.println("OPPONENT_MOUSE " + x + ":" + y);
        }

        /**
         * The run method of this thread.
         */
        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE All players connected");

                // Tell the first player that it is her turn.
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }
                
               

                // Repeatedly get commands from the client and process them.
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        
                        String strCoords = command.substring(5);
                        int row = Integer.parseInt(strCoords.split(":")[0]);
                        int col = Integer.parseInt(strCoords.split(":")[1]);
                        int coords[] = {row, col};
                        
                        
                        
                        if (legalMove(coords, this)) {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY"
                                         : boardFilledUp() ? "TIE"
                                         : "");
                        } else {
                            output.println("MESSAGE ?");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }else if(command.startsWith("MOUSE_MOVE")){
                        String coords = command.substring(command.lastIndexOf(" ")+1);
                        System.out.println("coords of opponent mouse are " + coords);
                        String[] parts = coords.split(":");
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);

                        // Send the mouse position to the other player
                        sendOpponentMousePosition(x, y);
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
        
        @Override
        public String toString(){
            return Character.toString(this.mark);
        }
    }
}