package main.java.ch.ethz.systems.asl.service.client;

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

public class Sender implements IService{
    
    public static void main(String[] args) {
        String hostname = "localhost";
        Socket clientSocket = null;  
        int port = 1999;

        try {
            clientSocket = new Socket(hostname, port);

            while ( true ) {
                ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                Message msg = new Message();
                /* setup message type */
                msg.setMsgType(MsgType.SEND);
                
                /* setup function type */
                //msg.setMsgFunc(MsgFunc.CTR_CREATEQ);
                //msg.setMsgFunc(MsgFunc.CTR_DELETEQ);
                //msg.setMsgFunc(MsgFunc.SEND_TOUSER);
                msg.setMsgFunc(MsgFunc.SEND_TOALL);
                
                /* message detail information */
                msg.setSender("send2");
                msg.setReceiver("all");
                //msg.setReceiver("recv2");
                msg.setMsgQueue("queue2");
                msg.setMid(CommonUtil.genUUID());
                msg.setMsgDetail("msg_q2_all is for you");
                objOut.writeObject(msg);
            
                Message r = (Message)in.readObject();
                System.out.println("Server returns its square as: " + r.getMsgDetail());
                break;
            }
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
