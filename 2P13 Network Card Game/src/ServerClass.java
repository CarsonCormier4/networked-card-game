import java.net.*;
import java.io.*;

//Game server that allows clients to connect
//Almost all this code was provided to us via the 'socket programming' tab
public class ServerClass {

    /**
     * @method Main method that creates a server
     * @param args Command line arguments
     */
    public static void main(String[] args){
        int portNumber = 35754;
        try(ServerSocket serverSocket = new ServerSocket(portNumber)){
            while (true){
                System.out.println("Waiting for players...");
                Socket client1 = serverSocket.accept();
                System.out.println("Player 1 connected.");
                Socket client2 = serverSocket.accept();
                System.out.println("Player 2 connected.");

                new ConnectionThread(client1,client2).start();
            }
        }catch (IOException e){
            System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}
