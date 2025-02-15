package Player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.awt.Image; // For handling images
import javax.imageio.ImageIO; // For reading and writing image files
import java.io.*; // For working with file paths


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

    public ConnectFourClient(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Use BorderLayout for the frame
       
        ((JComponent) frame.getContentPane()).setDoubleBuffered(true);
        frame.setLayout(new BorderLayout());

        // Initialize the floating icon
        floatingIcon = new JLabel(icon);
        //floatingIcon.setVisible(true);
        
        opponentMouseIcon = new JLabel(opponentIcon);
        //opponentMouseIcon.setVisible(true);

        // North Panel for Floating Icon
        JPanel northPanel = new JPanel();
        northPanel.setBackground(northBg); // Optional
        northPanel.setPreferredSize(new Dimension(0,50));
        northPanel.add(floatingIcon);
        northPanel.add(opponentMouseIcon);
        frame.getContentPane().add(northPanel, BorderLayout.NORTH);
        
        // Message Label at the bottom
        messageLabel.setBackground(northBg);
        frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

        // Board Panel in the Center
        JPanel boardPanel = new JPanel();
        
        boardPanel.setBackground(bg);
        boardPanel.setLayout(new GridLayout(6, 7, 2, 2));

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
        logoPanel.setBackground(Color.WHITE); // Set background color for the logo panel
        ImageIcon logoIcon = new ImageIcon("connect4logo.png"); // Load the logo image
        JLabel logoLabel = new JLabel(logoIcon);
        logoPanel.add(logoLabel);

        // Create a vertical stack panel to hold the logo and the main content
        JPanel verticalStackPanel = new JPanel();
        verticalStackPanel.setLayout(new BoxLayout(verticalStackPanel, BoxLayout.PAGE_AXIS));

        // Add the logo panel and the main content to the vertical stack panel
        verticalStackPanel.add(logoPanel);
        verticalStackPanel.add(northPanel, BorderLayout.NORTH);
        verticalStackPanel.add(boardPanel, BorderLayout.CENTER);

        // Add the vertical stack panel to the frame
        frame.getContentPane().add(verticalStackPanel, BorderLayout.CENTER);
        
        //frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
        
        
        
        startInterpolation();
        startPlayerInterpolation();
        startMouseStillTimer(); 
    }
    
    private void sendMousePositionToServer(int x, int y) {
        // Send the mouse position to the server for the opponent to see
        out.println("MOUSE_MOVE " + x + ":" + y);
    }

    public void printBoard() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                System.out.print(board[i][j].toString() + " ");
            }
            System.out.println();
        }
    }
    
    private void startMouseStillTimer() {
        mouseStillTimer = new Timer(100, e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMouseMoveTime > MOUSE_STILL_THRESHOLD) {
                // If the mouse has been still for more than the threshold, disable interpolation
                interpolationEnabled = false;
            } else {
                // If the mouse is moving, re-enable interpolation
                interpolationEnabled = true;
            }
        });
        mouseStillTimer.start();
    }
    
    private void updateOpponentMousePosition(int x, int y) {
        targetX = x;
        targetY = y;
    }

    private void startInterpolation() {
        Timer timer = new Timer(5, e -> {
            currentX += (targetX - currentX) * INTERPOLATION_FACTOR;
            currentY += (targetY - currentY) * INTERPOLATION_FACTOR;
            opponentMouseIcon.setLocation(currentX, currentY);
            opponentMouseIcon.getParent().repaint();
        });
        timer.start();
    }
    
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
    
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                String playerColour = (mark == 'X' ? "pink" : "yellow");
                String oppColour = (mark == 'X' ? "yellow" : "pink");

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
                    messageLabel.setText("Valid move, please wait");

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
                    messageLabel.setText("Opponent moved, your turn");
                        
                   
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
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("You'll get 'em next time!");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("You tied");
                    break;
                } else if (response.startsWith("MESSAGE")) {
       
                    if(response.contains("Your move")){
                        floatingIcon.setIcon(floatingIconPlayer);
                        floatingIcon.setVisible(true);
                    }else{
                        floatingIcon.setVisible(false);
                    }
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
        } finally {
            socket.close();
        }
    }

    private int getLowestAvailableRow(int row, int col) {
        int lowestRow = board.length - 1;
        for (int i = board.length - 1; i > -1; i--) {
            if (board[i][col].toString().equals("_")) {
                return i;
            }
        }
        return lowestRow;
    }

    private void updateBoard(Square currentSquare) {
        int rowClicked = currentSquare.row;
        int colClicked = currentSquare.col;

        int lowestRow = getLowestAvailableRow(rowClicked, colClicked);

        Square sq = board[lowestRow][colClicked];
        sq.beenClicked = true;
        sq.setIcon(icon);
        sq.repaint();
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
                "Up to another round?",
                "Connect 4",
                JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

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

    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[0];
            ConnectFourClient client = new ConnectFourClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setBounds(50, 50, 600, 600);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}
