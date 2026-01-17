import java.net.*;
import java.io.*;
import java.util.Scanner;

//A chat client that connects to a server
public class ClientClass {


    /**
     * @method Main method that creates a connection and communicates with the server
     * @param args Command line arguments
     */
    public static void main(String[] args){

        //Server details
        String hostName = "localhost";
        int portNumber = 35754;

        try(

                //Connect to game server
                Socket conn=new Socket(hostName, portNumber);

                //sockOut: sends commands to the server
                PrintWriter sockOut=new PrintWriter(conn.getOutputStream(),true);

                //sockIn: gets command updates from the server
                BufferedReader sockIn=new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //termIn: reads players keyboard input
                Scanner termIn=new Scanner(System.in);
        ){
            System.out.println("Connected to server.");

            //Thread to handle user messages 1 at a time
            //This prevents users from making moves out of turn
            new Thread(()->{
                try{
                    String message;

                    //Listens to messages from the server until the connection breaks
                    while ((message = sockIn.readLine())!=null){

                        //Print game board and messages
                        System.out.println(message);
                    }
                }catch (IOException e){
                    System.out.println("Server Disconnected.");
                }
            }).start();

            //Handles player input
            while(true){
                String fromUser = termIn.nextLine();
                sockOut.println(fromUser);
            }
        }catch (UnknownHostException e){
            System.out.println("There is a problem with the host name.");
        }catch (IOException e){
            System.out.println("IO error for the connection.");
        }
    }
}
