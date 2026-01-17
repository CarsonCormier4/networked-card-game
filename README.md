# CoExistence — Multiplayer Networked Card Game

This project is a Java-based multiplayer card game featuring a server and GUI client. It was developed as part of the COSC 2P13 assignment at Brock University. Players compete by attacking each other's units according to specific rules, and the first player to reach 9 points wins.

## Getting Started

To run the game, you need Java 11 installed on your system. The game consists of three main components:

- **Server** — Handles game logic and coordinates player turns.
- **GUI Client** — Allows a player to interact with the game via a simple Java Swing interface.
- **Terminal Client** — A text-based interface (you can also use `netcat`) that communicates with the server.

### Prerequisites

Make sure you have:

- Java 11 installed
- IntelliJ IDEA or another Java IDE (optional, but recommended)

## Running the Game

1. **Start the Server**  
   Run `ServerClass.java`. This will start the game server and wait for two clients to connect.

2. **Start the Clients**  
   Run `ClientClass.java` **twice** (or once on two different computers) to connect two players to the server.

3. **Gameplay Instructions**  
   - Each player has a deck of 6 cards drawn from a standard 12-card deck (Axe, Hammer, Sword, Arrow).  
   - Take turns attacking your opponent’s cards. Only valid attacks are allowed.  
   - Players can **pass** their turn by typing `PS`. A player must also pass if no valid attacks are available.  
   - Rounds end when both players pass consecutively. The game ends when one player reaches **9 points**, or the round limit of **5 rounds** is reached.  

## Server-to-Client Communication

- The server sends a fixed-size, human-readable message frame to each client.  
- Each client sees the board from their perspective, with the **current turn indicator** (`v`) and the **opponent indicator** (`^`).  
- Arrows (`-`) indicate the game is over.  

## Client-to-Server Commands

- **Move**: Two-character command indicating attacker and target columns (e.g., `CE`)  
- **Pass**: `PS`  
- Commands are case-insensitive. Invalid moves or syntax errors are reported back in the message log.  

## Project Structure
ServerClass.java // Starts the game server and accepts client connections
ConnectionThread.java // Handles game logic and individual game threads
ClientClass.java // GUI or terminal client for a player


## Notes

- The GUI client parses the server message frame and displays the game visually using ASCII art.  
- Players should only attempt moves when it's their turn; the client disables input otherwise.  
- The terminal client can be used with standard terminal programs (or `netcat`).  

## Learn More

- Java Networking: [Oracle Java Networking Docs](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/package-summary.html)  
- Java Swing GUI: [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)  
- IntelliJ IDEA: [IntelliJ Official Documentation](https://www.jetbrains.com/idea/documentation/)

## Author

- **Carson Cormier** — Brock University, COSC 2P13

