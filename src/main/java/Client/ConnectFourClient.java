package Client;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.net.Socket;
import java.awt.Image; 
import javax.imageio.ImageIO; 
import javax.swing.*;

public class ConnectFourClient {

    private JFrame frame = new JFrame("Connect 4");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;
    private ImageIcon opponentIcon;
    private JLabel floatingIcon;
    private JLabel opponentMouseIcon;
    ImageIcon floatingIconOpponent;
    ImageIcon floatingIconPlayer;
    private Square[][] board = new Square[6][7];
    private Square currentSquare;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Color bg = new Color(48, 99, 142);
    private Color northBg = new Color(0, 61, 91);
    
    //Stuff for tracking opponent mouse position
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 50; // Update every 50ms
    private int targetX = 0;
    private int targetY = 0;
    private int currentX = 0;
    private int currentY = 0;
    private static final double INTERPOLATION_FACTOR = 0.2; // Adjust for smoother/faster interpolation
    
    //Stuff for tracking player mouse position
    private int playerTargetX = 0;
    private int playerCurrentX = 0;
    private static final double PLAYER_INTERPOLATION_FACTOR = 0.2; // Adjust for smoother/faster interpolation
    private long lastMouseMoveTime = System.currentTimeMillis(); // Track the last time the mouse moved
    private static final long MOUSE_STILL_THRESHOLD = 500; // 0.5 second threshold
    private Timer mouseStillTimer; // Timer to check for mouse stillness
    private boolean interpolationEnabled = true;
    
    //Stuff for play again or quit 
    private final JButton playAgainButton = new JButton("Play Again");
    private final JButton quitButton = new JButton("Quit");
    private boolean playAgain = false;
    private boolean decisionMade = false;
    JPanel messagePanel = new JPanel(new BorderLayout());
    private final Object lock = new Object(); //shared object to synchronize main method and client 

    public ConnectFourClient(String serverAddress) throws Exception {

        // creating a socket on the specified port with the address of the server
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // creating the GUI
        ((JComponent) frame.getContentPane()).setDoubleBuffered(true);
        frame.setLayout(new BorderLayout());

        // Initialize the floating icons which track player mouse movement
        floatingIcon = new JLabel(icon);
        opponentMouseIcon = new JLabel(opponentIcon);

        // North Panel for Floating Icons
        JPanel northPanel = new JPanel();
        northPanel.setBackground(northBg); // Optional
        northPanel.setPreferredSize(new Dimension(0,50));
        northPanel.add(floatingIcon);
        northPanel.add(opponentMouseIcon);
        frame.getContentPane().add(northPanel, BorderLayout.NORTH);
        
        // Message label between the logo and north panel
        messageLabel.setBackground(northBg);
        messageLabel.setOpaque(true);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //messageLabel.setPreferredSize(new Dimension(frame.getWidth(), 50)); // Adjust height if needed
        messageLabel.setFont(new Font("BerlinSansFB", Font.BOLD, 20));
        
        //section for the play again button when the game ends
        playAgainButton.setVisible(false); //only show when the game ends
        playAgainButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                synchronized(lock){
                    playAgain = true;
                    decisionMade = true;
                    lock.notify();
                    frame.dispose();
                    System.out.println("Play again button pressed, playAgain bool is now " + playAgain);
                }
            }
        });
        
        quitButton.setVisible(false);
        quitButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                synchronized(lock){
                    playAgain = false;
                    decisionMade = true;
                    lock.notify();
                    frame.dispose();
                    System.out.println("Player is quitting");
                }
            }
        });
        
        //JPanel for the buttons
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(northBg);

        // Style the Quit Button
        quitButton.setText("Quit");
        quitButton.setForeground(Color.WHITE);
        quitButton.setFont(new Font("Arial", Font.BOLD, 16));
        quitButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding
        quitButton.setFocusPainted(false); 
        quitButton.setContentAreaFilled(false);
        quitButton.setOpaque(true); 
        quitButton.setBackground(Color.decode("#ef476f"));
        //add hover effect
        quitButton.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){
                quitButton.setBackground(Color.decode("#bf3253"));
            }
            
            public void mouseExited(MouseEvent e){
                quitButton.setBackground(Color.decode("#ef476f"));
            }
        });

        // Style the Play Again Button
        playAgainButton.setText("Play Again");
        playAgainButton.setForeground(Color.WHITE);
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 16));
        playAgainButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding
        playAgainButton.setFocusPainted(false); 
        playAgainButton.setContentAreaFilled(false);
        playAgainButton.setOpaque(true); 
        playAgainButton.setBackground(Color.decode("#06d6a0"));
        //add hover effect
        playAgainButton.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){
                playAgainButton.setBackground(Color.decode("#038a67"));
            }
            
            public void mouseExited(MouseEvent e){
                playAgainButton.setBackground(Color.decode("#06d6a0"));
            }
        });

        // Create constraints for the quit button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; // 1st column
        gbc.gridy = 0; // 1st row
        gbc.weightx = 0.5; // to the left
        gbc.anchor = GridBagConstraints.CENTER; // align within cell
        buttonPanel.add(quitButton, gbc);

        // Create constraints for the play again button
        gbc.gridx = 1; // 2nd column (same row)
        gbc.weightx = 0.5; // to the right
        gbc.anchor = GridBagConstraints.CENTER; // align within cell
        buttonPanel.add(playAgainButton, gbc);

        
        //need JPanel so whole width of background behind label is blue
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.SOUTH);
        messagePanel.setBackground(northBg);
        messagePanel.setPreferredSize(new Dimension(frame.getWidth(), 50));

        // board Panel in the Center
        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(bg);
        boardPanel.setLayout(new GridLayout(6, 7, 2, 2));

        //track mouse movements by adding a motion listener to the board panel
        boardPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (floatingIcon.isVisible()) {
                    playerTargetX = e.getX(); // Update the target position
                    lastMouseMoveTime = System.currentTimeMillis(); // Reset the stillness timer
                    interpolationEnabled = true; // Re-enable interpolation
                    sendMousePositionToServer(playerTargetX, 0); // Send the target position to the server
                }
            }
        });

        //initialize the board GUI
        System.out.println("initializing board");
        for (int i = 0; i < board.length; i++) {
            final int z = i;
            for (int k = 0; k < board[i].length; k++) {
                final int m = k;
                board[i][k] = new Square(i, k);
                board[i][k].addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        currentSquare = board[z][m];
                        //currentSquare.beenClicked = true;
                        out.println("MOVE " + z + ":" + m);
                        floatingIcon.setVisible(false);
                    }
                });

                boardPanel.add(board[i][k]);
            }
        }
        System.out.println("Done initializing board");
        
        // Create a panel for the logo
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(northBg); // background color for the logo panel
        ImageIcon logoIcon = new ImageIcon("connect4logo.png"); 
        JLabel logoLabel = new JLabel(logoIcon);
        logoPanel.add(logoLabel);

        // vertical stack panel to hold the logo and the main content
        JPanel verticalStackPanel = new JPanel();
        verticalStackPanel.setLayout(new BoxLayout(verticalStackPanel, BoxLayout.PAGE_AXIS));

        // add logo panel and the main content to the vertical stack panel
        verticalStackPanel.add(logoPanel);
        verticalStackPanel.add(messagePanel);
        verticalStackPanel.add(northPanel, BorderLayout.NORTH);
        verticalStackPanel.add(boardPanel, BorderLayout.CENTER);

        // add the vertical stack panel to the frame
        frame.getContentPane().add(verticalStackPanel, BorderLayout.CENTER);
        
        startInterpolation();
        startPlayerInterpolation();
        startMouseStillTimer(); 
    }
    
    /**
     * Tells the main method whether or not the user has decided if they are gonna quit or play again.
     * Synchronized so main method blocks.
     * @return boolean
     */
    public synchronized boolean hasDecided(){
        return decisionMade;
    }
    
    /**
     * Tells the main method whether or not the user is going to play again.
     * Synchronized so main method blocks.
     * @return boolean
     */
    public synchronized boolean wantsToPlayAgain(){
        return playAgain;
    }
    
    private void sendMousePositionToServer(int x, int y) {
        // Send the mouse position to the server for the opponent to see
        // This will allow the opponent to see the floating icon moving when the player moves their mouse
        out.println("MOUSE_MOVE " + x + ":" + y);
    }

    public void printBoard() {
        //for debugging, printing board to console to see internal structure
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                System.out.print(board[i][j].toString() + " ");
            }
            System.out.println();
        }
    }
    
    private void startMouseStillTimer() {
        //timer to check how long a players mouse has been still, the idea is that interpolation should stop
        //so we dont send too much to the server
        mouseStillTimer = new Timer(100, e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMouseMoveTime > MOUSE_STILL_THRESHOLD) {
                // if the mouse has been still for more than the threshold, disable interpolation
                interpolationEnabled = false;
            } else {
                // if the mouse is moving, re-enable interpolation
                interpolationEnabled = true;
            }
        });
        mouseStillTimer.start();
    }
    
    private void updateOpponentMousePosition(int x, int y) {
        targetX = x;
        targetY = y;
    }

    /**
     * Helps with smoother animation and ensuring we don't send too much info to the server regarding mouse
     * position
     */
    private void startInterpolation() {
        Timer timer = new Timer(5, e -> {
            currentX += (targetX - currentX) * INTERPOLATION_FACTOR;
            currentY += (targetY - currentY) * INTERPOLATION_FACTOR;
            opponentMouseIcon.setLocation(currentX, currentY);
            opponentMouseIcon.getParent().repaint();
        });
        timer.start();
    }
    
    /**
     * For the other player
     */
    private void startPlayerInterpolation() {
        Timer playerTimer = new Timer(5, e -> {
            if (interpolationEnabled) {
                // Interpolate the player's icon position
                playerCurrentX += (playerTargetX - playerCurrentX) * PLAYER_INTERPOLATION_FACTOR;
            } else {
                // Snap to the target position when interpolation is disabled
                playerCurrentX = playerTargetX;
            }
            floatingIcon.setLocation(playerCurrentX, 0);
            floatingIcon.getParent().repaint(); // Repaint only the necessary area
        });
        playerTimer.start();
    }
    
    /**
     * Contains the logic which is used while the game is running
     * Handles messages from/to the server
    */
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                String playerColour = (mark == 'P' ? "pink" : "yellow");
                String oppColour = (mark == 'P' ? "yellow" : "pink");

                icon = new ImageIcon(playerColour + ".jpg");
                opponentIcon = new ImageIcon(oppColour +".jpg");
                floatingIconPlayer = new ImageIcon(playerColour + "DarkBG.jpg");
                floatingIconOpponent = new ImageIcon(oppColour +"DarkBG.jpg");

                floatingIcon.setIcon(floatingIconPlayer);
                opponentMouseIcon.setIcon(floatingIconOpponent);
        
                // Initially hide both icons
                floatingIcon.setVisible(false);
                opponentMouseIcon.setVisible(false);
                
                frame.setTitle("Connect 4 - Player " + playerColour);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Opponent's Turn");

                    floatingIcon.setVisible(false);
                    opponentMouseIcon.setIcon(floatingIconOpponent);
                    opponentMouseIcon.setVisible(true);
                    updateBoard(currentSquare);
                    printBoard();

                } else if (response.startsWith("OPPONENT_MOVED")) {
                    String coords = response.substring(15);
                    int row = Integer.parseInt(coords.split(":")[0]);
                    int col = Integer.parseInt(coords.split(":")[1]);
                    board[row][col].setIcon(opponentIcon);
                    board[row][col].beenClicked = true;
                    board[row][col].repaint();
                    messageLabel.setText("Your Turn");
                        
                   
                    opponentMouseIcon.setVisible(false);
                    floatingIcon.setIcon(floatingIconPlayer);
                    floatingIcon.setVisible(true);

                    printBoard();

                } else if (response.startsWith("OPPONENT_MOUSE")){
                    // Extract the opponent's mouse coordinates
                    String coords = response.substring(response.lastIndexOf(" ")+1);
                    String[] parts = coords.split(":");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);

                    // Update the opponent's mouse position
                    opponentMouseIcon.setIcon(floatingIconOpponent);
                    opponentMouseIcon.setVisible(true);
                    updateOpponentMousePosition(x, y);
                    System.out.println("Opponent mouse position is: " + coords);
                    
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("You win!");
                    showPlayAgainPanel();
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("You'll get 'em next time!");
                    showPlayAgainPanel();
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("You tied!");
                    showPlayAgainPanel();
                    break;
                } else if (response.startsWith("MESSAGE")) {
       
                    if(response.contains("Your move")){
                        floatingIcon.setIcon(floatingIconPlayer);
                        floatingIcon.setVisible(true);
                    }else{
                        floatingIcon.setVisible(false);
                    }
                    messageLabel.setText(response.substring(8));
                } else if (response.startsWith("DISCONNECT")){
                    messageLabel.setText(response.substring(11));
                    showPlayAgainPanel();
                    break;
                }
            }
            out.println("QUIT");
        } finally {
            socket.close();
        }
    }
    
    /**
     * When game ends show the button which allows a user to play again
     */
    private void showPlayAgainPanel(){
        //hide the floating icons
        floatingIcon.setVisible(false);
        opponentMouseIcon.setVisible(false);
        
        //show the buttons to quit or play again
        playAgainButton.setVisible(true);
        quitButton.setVisible(true);
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    /**
     * Finds the lowest row in the board which a chip can be placed in, 
     * this helps with the "gravity" logic of connect 4
     * @param row
     * @param col
     * @return index of the lowest row (highest index without a chip already in it) 
     */
    private int getLowestAvailableRow(int row, int col) {
        int lowestRow = board.length - 1;
        for (int i = board.length - 1; i > -1; i--) {
            if (board[i][col].toString().equals("_")) {
                return i;
            }
        }
        return lowestRow;
    }

    /**
     * When a player makes a move, this updates the board with the new move
     * @param currentSquare 
     */
    private void updateBoard(Square currentSquare) {
        int rowClicked = currentSquare.row;
        int colClicked = currentSquare.col;

        int lowestRow = getLowestAvailableRow(rowClicked, colClicked);

        Square sq = board[lowestRow][colClicked];
        sq.beenClicked = true;
        sq.setIcon(icon);
        sq.repaint();
    }

    //this class represents a square on the board
    static class Square extends JPanel {
        Image empty;
        Color bg = new Color(48, 99, 142);
        JLabel label = new JLabel((Icon) null);
        int row;
        int col;
        boolean beenClicked = false;

        public Square(int row, int col) {
            this.row = row;
            this.col = col;
            setBackground(bg);
            add(label);
            
            try{
                empty = ImageIO.read(new File("empty.jpg"));
            }catch(IOException f){
                System.out.println(f.getMessage());
            }
            label.setIcon(new ImageIcon(empty));
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }

        @Override
        public String toString() {
            //Icon ic = label.getIcon();
            return (beenClicked) ? "P" : "_";
        }
    }

    //runs the program
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[0];
            ConnectFourClient client = new ConnectFourClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setBounds(50, 50, 600, 660);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
//            if (!client.wantsToPlayAgain()) {
//                break;
//            }

            synchronized(client.lock){
                while(!client.hasDecided()){
                    client.lock.wait();
                    System.out.println("Client has decided whether to play again? " + client.hasDecided());
                } 
            }
            
            if(!client.wantsToPlayAgain()){
                System.out.println("Client wants to play again? " + client.wantsToPlayAgain());
                break;
            }
            
            //reset flags for next game
            client.playAgain = false;
            client.decisionMade = false;
        }
        System.out.println("The game session has ended by choice of player.");
        System.exit(0);
    }
}
