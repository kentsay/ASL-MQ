package main.java.ch.ethz.systems.asl.service.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

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
        
        try {
            clientSocket = new Socket(hostname, port);
            os = new DataOutputStream(clientSocket.getOutputStream());

            while ( true ) {
            System.out.println("Receiver is action:");
            ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            Message msg = new Message();
            /* setup message type */
            //msg.setMsgType(MsgType.RECEIVE);
            msg.setMsgType(MsgType.QUERY);

            /* setup function type */
            //msg.setMsgFunc(MsgFunc.READ_BYLOOKMESSAGE);
            //msg.setMsgFunc(MsgFunc.READ_BYRMVMESSAGE);
            //msg.setMsgFunc(MsgFunc.QUERY_BYQUEUE);
            msg.setMsgFunc(MsgFunc.QUERY_BYSENDER);
            
            /* message detail information */
            msg.setSender("recv2");
            //msg.setMsgQueue("queue4");
            msg.setMsgDetail("send1");
            msg.setMid(CommonUtil.genUUID());
            
            objOut.writeObject(msg);
            
            Message r = (Message)in.readObject();
            //ArrayList<Message> data = r.getResponMessage();
            //for (Message result: data) {
                System.out.println("Server returns its square as: ");
                //System.out.println("Sender: " + result.getSender());
                //System.out.println("Message Detail: " + result.getMsgDetail());
                //System.out.println("Message Queue: " + r.getMsgQueue());
                System.out.println("TOp 1 msg from send1: " + r.getMsgDetail());
            break;
            }
            
            os.close();
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
