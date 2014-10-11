package main.java.ch.ethz.systems.asl.service.middleware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Logger;

import javax.naming.NamingException;

import main.java.ch.ethz.systems.asl.bean.Message;
import main.java.ch.ethz.systems.asl.service.db.DBService;
import main.java.ch.ethz.systems.asl.util.DbUtil;

public class MessageService implements Runnable {

    private static final Logger Log = Logger.getLogger(DBService.class.getName());
    
    MessageServiceMain server;
    Socket clientSocket;
    ObjectInputStream  objInputStream;
    ObjectOutputStream objOutputStream;
    Connection conn;
    int id;
    int rescount;
    
    public MessageService(Socket clientSocket, int id, MessageServiceMain server) throws ClassNotFoundException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.id = id;
        System.out.println( "Connection " + id + " established with: " + clientSocket );
    }
    
    //TODO: Establish Thread Pool to reduce system loading
    public void run() {
        Message rawMsg;
        Message resultMsg;
        try {
            objInputStream = new ObjectInputStream(clientSocket.getInputStream());
            objOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            rawMsg = (Message)objInputStream.readObject();
            Log.info("Received message from client " + id);
            
            resultMsg = runMsgManagement(rawMsg);
            objOutputStream.writeObject(resultMsg);
            
            Log.info( "Connection " + id + " closed." );
            clientSocket.close();
            
        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public Message runMsgManagement(Message raw) {
        Message result = new Message();
        msgValidation(raw);
        result = msgExecution(raw, result);
        return result;
    }
    
    private void msgValidation(Message raw) {
        /* TODO
         * validate: message type, message function type
         */
    }
    
    private Message msgExecution(Message raw, Message result) {
        switch(raw.getMsgType()) {
        case SEND:
            processSend(raw, result);
            break;
        case RECEIVE:
            processReceive(raw, result);
            break;
        case QUERY:
            processQuery(raw, result);
            break;
        default:
            break;
        }
        return result;
    }
    
    private void processSend(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        case CTR_CREATEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    conn = DbUtil.getConnection("local");
                    String qname = raw.getMsgDetail();
                    String sql = "insert into queue (qname) values (?)";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        conn.commit();
                        Log.info("insert success");
                    } else {
                        //set error code: queue did not insert
                        Log.info("insert fail");
                    }
                } catch (SQLException | NamingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
        case CTR_DELETEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    conn = DbUtil.getConnection("local");
                    String qname = raw.getMsgDetail();
                    String sql = "delete from queue where qname = ?";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        conn.commit();
                        Log.info("delete success");
                    } else {
                        //set error code: queue did not delete
                        Log.info("delete fail");
                    }
                } catch (SQLException | NamingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
        case SEND_TOUSER:
            //TODO
            break;
        case SEND_TOALL:
            //TODO
            break;
        default:
            break;
        }
    }
    
    private void processReceive(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        case READ_BYRMVMESSAGE:
            //TODO
            break;
        case READ_BYLOOKMESSAGE:
            //TODO
            break;
        default:
            break;
        }
    }
    
    private void processQuery(Message raw, Message result) {
        switch (raw.getMsgFunc()) {
            case QUERY_BYSENDER:
                //TODO
                break;
            case QUERY_BYQUEUE:
                //TODO
                break;
            default:
                break;
        }
        
    }
}
