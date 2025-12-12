# Connect Four - Networked Two-Player Game (Java)

This repository contains a fully networked, two-player Connect Four game implemented in Java.
The project includes both a server and a graphical client, allowing two remote players to compete in real time.

The system is built with a multithreaded server, a lightweight client communication protocol, and a Java Swing–based UI.

Key Components:
- **ConnectFourServer** — Manages client connections, game pairing, game state, and move validation.
- **ConnectFourClient** — Renders the UI, sends user input to the server, and updates the board based on server events.
- **Game / Game.Player** — Per-match logic plus one thread per client.
- **Board** — Internal 6×7 game model with win detection.
- **Protocol** — Message definitions shared by client and server.

---

## Features

### Game Mechanics
- Classic 6×7 Connect Four rules  
- Gravity-based piece placement  
- Win, loss, and tie detection  
- Strict turn enforcement  

### Networking
- Server pairs two players into a match  
- Dedicated thread for each client  
- Real-time move + hover sharing  
- Disconnect detection  
- Clean communication protocol  

### User Interface
- Java Swing rendering  
- Column hover indicators  
- Turn and status display  
- Error + move feedback  

---

# Pictures
## Game Loads:
![image](https://github.com/user-attachments/assets/8cdea0e3-3ef1-4cea-95b1-bee492893a52)

## Players begin playing:
![image](https://github.com/user-attachments/assets/7e8daa5b-acee-45d2-862a-cd8b7405820b)

## Player wins:
![image](https://github.com/user-attachments/assets/a0b725ab-923f-4455-b41e-3677beeb3294)

---

## Communication Protocol

### Client → Server

| Command            | Description                                   |
|--------------------|-----------------------------------------------|
| MOVE(col)              | Attempts to place a piece in the column        |
| MOUSE_MOVE(col)        | Sends hover position to the opponent           |
| QUIT              | Client exits the game                          |

### Server → Client

| Message                    | Meaning                                    |
|---------------------------|---------------------------------------------|
| VALID_MOVE                | Move accepted                               |
| INVALID_MOVE              | Illegal move or wrong turn                  |
| VICTORY                   | Player has won                              |
| DEFEAT                    | Opponent has won                            |
| TIE                       | Board is full                               |
| OPPONENT_MOVED(col)            | Opponent placed a piece                     |
| OPPONENT_CURSOR(col)           | Opponent hovered over a column              |
| OPPONENT_LEFT             | Opponent disconnected                       |





