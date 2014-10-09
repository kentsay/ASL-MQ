package main.java.ch.ethz.systems.asl.service.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import main.java.ch.ethz.systems.asl.service.middleware.IService;

public class Sender implements IService{
    
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 1999;

        // declaration section:
        // clientSocket: our client socket
        // os: output stream
        // is: input stream
        
            Socket clientSocket = null;  
            DataOutputStream os = null;
            BufferedReader is = null;
        
        // Initialization section:
        // Try to open a socket on the given port
        // Try to open input and output streams
        
            try {
                clientSocket = new Socket(hostname, port);
                os = new DataOutputStream(clientSocket.getOutputStream());
                is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host: " + hostname);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to: " + hostname);
            }
        
        // If everything has been initialized then we want to write some data
        // to the socket we have opened a connection to on the given port
        
        if (clientSocket == null || os == null || is == null) {
            System.err.println( "Something is wrong. One variable is null." );
            return;
        }

        try {
            while ( true ) {
            System.out.print( "Enter an integer (0 to stop connection, -1 to stop server): " );
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String keyboardInput = br.readLine();
            os.writeBytes( keyboardInput + "\n" );

            int n = Integer.parseInt( keyboardInput );
            if ( n == 0 || n == -1 ) {
                break;
            }
            
            String responseLine = is.readLine();
            System.out.println("Server returns its square as: " + responseLine);
            }
            
            // clean up:
            // close the output stream
            // close the input stream
            // close the socket
            
            os.close();
            is.close();
            clientSocket.close();   
        } catch (UnknownHostException e) {
            System.err.println("Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }

    @Override
    public int getServiceState() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setServiceState(int state) {
        // TODO Auto-generated method stub
        
    }

}
