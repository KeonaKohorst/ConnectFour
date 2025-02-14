package Player;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


/**
 * TTTP (Tic Tac Toe Protocol)
 * which is entirely text based.  Here are the strings that are sent:
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
 */
public class ConnectFourClient {

    private JFrame frame = new JFrame("Connect Four");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;
    private ImageIcon opponentIcon;

    private Square[][] board = new Square[6][7];
    private Square currentSquare;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public ConnectFourClient(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Layout GUI
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(6, 7, 2, 2));
        
        System.out.println("initializing board");
        for (int i = 0; i < board.length; i++) {
            //System.out.println("i is " + i);
            final int z = i;
            for(int k = 0; k < board[i].length; k++){
                //System.out.println("k is " + k);
                final int m = k;
                board[i][k] = new Square(i,k);
                board[i][k].addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        
                        
                        currentSquare = board[z][m];
                        
                        
                        
                       
                        //when this button is pressed this message is sent to the server
                        out.println("MOVE " + z + ":" + m);}});
                        
                boardPanel.add(board[i][k]);
            }
        }
        System.out.println("Done initializing board");
        frame.getContentPane().add(boardPanel, "Center");
    }
    
    public void printBoard(){
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                System.out.print(board[i][j].toString() + " ");
            }
            System.out.println();
        }
    }

    /**
     * The main thread of the client will listen for messages
     * from the server.  The first message will be a "WELCOME"
     * message in which we receive our mark.  Then we go into a
     * loop listening for "VALID_MOVE", "OPPONENT_MOVED", "VICTORY",
     * "DEFEAT", "TIE", "OPPONENT_QUIT or "MESSAGE" messages,
     * and handling each message appropriately.  The "VICTORY",
     * "DEFEAT" and "TIE" ask the user whether or not to play
     * another game.  If the answer is no, the loop is exited and
     * the server is sent a "QUIT" message.  If an OPPONENT_QUIT
     * message is received then the loop will exit and the server
     * will be sent a "QUIT" message also.
     */
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                icon = new ImageIcon(mark == 'X' ? "x.gif" : "o.gif");
                opponentIcon  = new ImageIcon(mark == 'X' ? "o.gif" : "x.gif");
                frame.setTitle("Connect Four - Player " + mark);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Valid move, please wait");
                    
                    updateBoard(currentSquare);
                    printBoard();
                    
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    System.out.println("My opponent moved");
                    String coords = response.substring(15);
                    System.out.println("Coords is " + coords);
                    int row = Integer.parseInt(coords.split(":")[0]);
                    int col = Integer.parseInt(coords.split(":")[1]);
                    System.out.println("opponent Row is " + row);
                    System.out.println("opponent Col is " + col);
                    board[row][col].setIcon(opponentIcon);
                    board[row][col].repaint();
                    messageLabel.setText("Opponent moved, your turn");
                    
                    //updateBoard()
                    printBoard();
                    
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("You win");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("You lose");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("You tied");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
        }
        finally {
            socket.close();
        }
    }
    
    private int getLowestAvailableRow(int row, int col){
        int lowestRow = board.length-1;
      
        for(int i = board.length-1; i > -1; i--){
            System.out.println("if board[" + i + "][" + col + "].toString (result: " + board[i][col].toString() + ") equals _ ? " + board[i][col].toString().equals('_'));
            if(board[i][col].toString().equals("_")){
                //lowestRow = i;
                System.out.println("Returning early from loop");
                return i;
            }
        }
        return lowestRow; //i think this is the problem and the above isnt working
    }
    
    private void updateBoard(Square currentSquare){
        int rowClicked = currentSquare.row;
        int colClicked = currentSquare.col;

        int lowestRow = getLowestAvailableRow(rowClicked, colClicked);
        
        System.out.println("Updating board at " + lowestRow + " " + colClicked);
        Square sq = board[lowestRow][colClicked];
        sq.setIcon(icon);
        sq.repaint();
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
            "Want to play again?",
            "Connect Four is Fun Fun Fun",
            JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Graphical square in the client window.  Each square is
     * a white panel containing.  A client calls setIcon() to fill
     * it with an Icon, presumably an X or O.
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);
        int row;
        int col;

        public Square(int row, int col) {
            this.row = row;
            this.col = col;
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
        
        @Override 
        public String toString(){
            Icon ic = label.getIcon();
            
            return (ic != null) ? "P" : "_";
        }
    }

    /**
     * Runs the client as an application.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[0];
            ConnectFourClient client = new ConnectFourClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setBounds(400, 400, 600, 500);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}