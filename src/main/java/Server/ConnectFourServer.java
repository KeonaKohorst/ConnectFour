package Server;

import java.io.*;
import java.net.*;

public class ConnectFourServer {

    /**
     * Main method which runs the program and accepts clients as players
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Connect Four Server is Running");
        try {
            while (true) {
                Game game = new Game();
                Game.Player player1 = game.new Player(listener.accept(), 'P');
                Game.Player player2 = game.new Player(listener.accept(), 'Y');
                player1.setOpponent(player2);
                player2.setOpponent(player1);
                game.currentPlayer = player1;
                player1.start();
                player2.start();
            }
        } finally {
            listener.close();
        }
    }
}

/**
 * Class which represents the two player connect 4 game
 */
class Game {
    private Player[][] board = new Player[6][7];
    Player currentPlayer;
    
    /**
     * Checks if anyone has won the game after each player movement
     * @return boolean for whether a player has won
     */
    public boolean hasWinner() {
        // Check horizontal
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) { 
                if (board[row][col] != null &&
                    board[row][col] == board[row][col+1] &&
                    board[row][col] == board[row][col+2] &&
                    board[row][col] == board[row][col+3]) {
                    return true;
                }
            }
        }

        // Check vertical
        for (int row = 0; row < 3; row++) { 
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
     * Check if the move is being done by the player whose turn it is and if the spot
     * on the board isn't already taken up.
     * If legal, notify opponent and update board.
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
    
    /**
     * Used for debugging, prints the internal board to the console to show state of board on server
     */
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
    
    /**
     * If the board has no more empty squares this returns true, else false
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
     * Gets the lowest available row (highest index) for a chip.
     * This is the "gravity" of connect 4
     * @param row
     * @param col
     * @return the index of the row 
     */
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
     * Class representing a player. It extends Thread to allow multi-threading for the game.
     */
    class Player extends Thread {
        BufferedReader input;
        PrintWriter output;
        char mark;
        Player opponent;
        Socket socket;

        /**
         * Gets input and output streams for the player socket
         */
        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Hold on while we find you an opponent!");
            } catch (IOException e) {
                System.out.println("Player exited application: " + e);
            }
        }

        /**
         * Sets who the players opponent is
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * When we are told the other player moved, we tell the opponent player where
         * Also tell if anyone won after this move
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
         * Holds the logic which executes when the thread runs
         */
        public void run() {
            try {
                //wait for all players to be connected
                output.println("MESSAGE All Players Ready for Battle");

                if (mark == 'P') {
                    output.println("MESSAGE Your move");
                }
                
                // while the game is running, communicate with the client
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
                            output.println("MESSAGE Wait Your Turn!");
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
                System.out.println("Player disconnected: " + e);
                
                //send message to opponent that player disconnted
                opponent.output.println("DISCONNECT Your opponent disconnected.");
                try {socket.close();} catch (IOException ioe) {}
                
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