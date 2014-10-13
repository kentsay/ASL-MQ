package main.java.ch.ethz.systems.asl.service.middleware;

/*
 * @author: <a href="mailto:tsayk@student.ethz.ch">Kai-En Tsay(Ken)</a>
 * @version: 1.0
 * 
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
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
    boolean isRollBack = false;
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
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Message runMsgManagement(Message raw) throws SQLException {
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
    
    private Message msgExecution(Message raw, Message result) throws SQLException {
        //store message data into msg, msg_detail table
        processMsg(raw);
        processMsgDtl(raw);
        
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
        if (!isRollBack) conn.commit();
        return result;
    }
    
    private void processMsg(Message raw) {
        try {
            conn = DbUtil.getConnection(conn, "local");
            
            String sql_msg = "insert into msg (mid, msend_id, mrecv_id, mqueue_id) values (?,?,?,?)";
            Vector<String> param = new Vector<>();
            String mid = raw.getMid();
            String sendId = raw.getSender();
            String recvId = (null != raw.getReceiver()) ? raw.getReceiver(): "";
            String queue  = (null != raw.getMsgQueue()) ? raw.getMsgQueue(): "";
            param.add(mid);
            param.add(sendId);
            param.add(recvId);
            param.add(queue);
            rescount = DbUtil.sqlAction(sql_msg, param, conn, false);
            if (rescount > 0) {
                Log.info("insert msg table success");
            } else {
                //set error code: queue did not insert
                Log.info("insert msg table fail");
            }
        } catch (SQLException | NamingException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
    
    private void processMsgDtl(Message raw) {
        try {
            conn = DbUtil.getConnection(conn, "local");
            
            String sql_msg = "insert into msg_detail "
                    + "(msg_id, msg_type, msg_func, msg_detail) values "
                    + "(?,CAST(? as msgtype_enum), CAST(? as msgfunc_enum),?)";
            Vector<String> param = new Vector<>();
            String mid = raw.getMid();
            String msgType = raw.getMsgType().value();
            String msgFunc = raw.getMsgFunc().value();
            String msgDtl = raw.getMsgDetail();
            param.add(mid);
            param.add(msgType);
            param.add(msgFunc);
            param.add(msgDtl);
            rescount = DbUtil.sqlAction(sql_msg, param, conn, false);
            if (rescount > 0) {
                Log.info("insert msg_detail table success");
            } else {
                //set error code: queue did not insert
                Log.info("insert msg_detail table fail");
            }
        } catch (SQLException | NamingException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
    
    private void processSend(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        case CTR_CREATEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    conn = DbUtil.getConnection(conn, "local");
                    String qname = raw.getMsgQueue();
                    String sql = "insert into queue (qname) values (?)";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        Log.info("insert queue table success");
                    } else {
                        //set error code: queue did not insert
                        Log.info("insert queue table fail");
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
                    conn = DbUtil.getConnection(conn, "local");
                    String qname = raw.getMsgQueue();
                    String sql = "update queue set qstatus = CAST('delete' as qstatus_enum) where qname = ?";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        Log.info("delete queue table success");
                    } else {
                        //set error code: queue did not delete
                        Log.info("delete queue table fail");
                    }
                } catch (SQLException | NamingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
        case SEND_TOUSER:
        case SEND_TOALL:
            
            try {
                //check if queue exists
                conn = DbUtil.getConnection(conn, "local");
                String qname = raw.getMsgQueue();
                String sql = "select * from queue where qname = ?";
                Vector<String> param = new Vector<>();
                param.add(qname);
                ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
                
                if (rs.getRow() == 0) {
                    //TODO: set error code: queue does not exist, then ROLLBACK
                    isRollBack = true;
                    Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    //update queue size
                    conn = DbUtil.getConnection(conn, "local");
                    qname = raw.getMsgQueue();
                    sql = "update queue set qsize = ((select qsize from queue where qname = ?) + 1) ";
                    param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        Log.info("update queue size success");
                    } else {
                        //TODO: set error code: queue did not delete
                        Log.info("update queue size fail");
                    }
                }
            } catch (SQLException | NamingException e) {
                isRollBack = true;
                e.printStackTrace();
            }            
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
