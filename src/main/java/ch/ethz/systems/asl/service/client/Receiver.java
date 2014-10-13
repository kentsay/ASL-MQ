package main.java.ch.ethz.systems.asl.service.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import main.java.ch.ethz.systems.asl.bean.Message;
import main.java.ch.ethz.systems.asl.bean.MsgFunc;
import main.java.ch.ethz.systems.asl.bean.MsgType;
import main.java.ch.ethz.systems.asl.service.middleware.IService;
import main.java.ch.ethz.systems.asl.util.CommonUtil;

public class Receiver implements IService {

    
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 1999;

        Socket clientSocket = null;  
        DataOutputStream os = null;
        BufferedReader is = null;
        
        try {
            clientSocket = new Socket(hostname, port);
            os = new DataOutputStream(clientSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + hostname);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + hostname);
        }
        
        if (clientSocket == null || os == null || is == null) {
            System.err.println( "Something is wrong. One variable is null." );
            return;
        }

        try {
            while ( true ) {
            System.out.print( "Enter an integer (0 to stop connection, -1 to stop server): " );
            ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            Message msg = new Message();
            msg.setMsgType(MsgType.RECEIVE);
//            msg.setMsgFunc(MsgFunc.READ_BYLOOKMESSAGE);
            msg.setMsgFunc(MsgFunc.READ_BYRMVMESSAGE);
            msg.setSender("recv1");
            msg.setMsgQueue("queue4");
            msg.setMid(CommonUtil.genUUID());
            
            objOut.writeObject(msg);
            
            Message r = (Message)in.readObject();
            //String responseLine = is.readLine();
            ArrayList<Message> data = r.getResponMessage();
            for (Message result: data) {
                System.out.println("Server returns its square as: ");
                System.out.println("Sender: " + result.getSender());
                System.out.println("Message Detail: " + result.getMsgDetail());
            }
            break;
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
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
