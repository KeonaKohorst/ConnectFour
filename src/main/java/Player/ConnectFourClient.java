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

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ConnectFourClient {

    private JFrame frame = new JFrame("Connect Four");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;
    private ImageIcon opponentIcon;
    private JLabel floatingIcon;

    private Square[][] board = new Square[6][7];
    private Square currentSquare;
    
    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ConnectFourClient(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Use BorderLayout for the frame
        frame.setLayout(new BorderLayout());

        // Initialize the floating icon
        floatingIcon = new JLabel(icon);
        floatingIcon.setVisible(true);

        // North Panel for Floating Icon
        JPanel northPanel = new JPanel();
        northPanel.setBackground(Color.WHITE); // Optional
        northPanel.setPreferredSize(new Dimension(0,50));
        northPanel.add(floatingIcon);
        frame.getContentPane().add(northPanel, BorderLayout.NORTH);

        // Message Label at the bottom
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

        // Board Panel in the Center
        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(6, 7, 2, 2));

        boardPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (floatingIcon.isVisible()) {
                    floatingIcon.setLocation(e.getX(), 0);
                    northPanel.repaint();
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
                        out.println("MOVE " + z + ":" + m);
                        floatingIcon.setVisible(false);
                    }
                });

                boardPanel.add(board[i][k]);
            }
        }
        System.out.println("Done initializing board");
        frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
    }

    public void printBoard() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                System.out.print(board[i][j].toString() + " ");
            }
            System.out.println();
        }
    }

    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                icon = new ImageIcon(mark == 'X' ? "x.gif" : "o.gif");
                opponentIcon = new ImageIcon(mark == 'X' ? "o.gif" : "x.gif");
                frame.setTitle("Connect Four - Player " + mark);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Valid move, please wait");

                    floatingIcon.setVisible(false);
                    updateBoard(currentSquare);
                    printBoard();

                } else if (response.startsWith("OPPONENT_MOVED")) {
                    String coords = response.substring(15);
                    int row = Integer.parseInt(coords.split(":")[0]);
                    int col = Integer.parseInt(coords.split(":")[1]);
                    board[row][col].setIcon(opponentIcon);
                    board[row][col].repaint();
                    messageLabel.setText("Opponent moved, your turn");

                    floatingIcon.setIcon(icon);
                    floatingIcon.setVisible(true);

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

    static class Square extends JPanel {
        JLabel label = new JLabel((Icon) null);
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
        public String toString() {
            Icon ic = label.getIcon();
            return (ic != null) ? "P" : "_";
        }
    }

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
