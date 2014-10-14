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
import java.util.ArrayList;
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
                        isRollBack = true;
                        Log.info("insert queue table fail");
                    }
                } catch (SQLException | NamingException e) {
                    isRollBack = true;
                    e.printStackTrace();
                }
            }
            break;
        case CTR_DELETEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    //remove queue data
                    conn = DbUtil.getConnection(conn, "local");
                    String qname = raw.getMsgQueue();
                    String sql = "delete from queue where qname = ?";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    rescount = DbUtil.sqlAction(sql, param, conn, false);
                    if (rescount > 0) {
                        Log.info("delete queue table success");
                        //change all msg status in this queue to remove
                        sql = "update msg_detail "
                                + "set status = CAST('remove' as status_enum) "
                                + "where msg_id in ("
                                    + "select msg_id from msg_detail, msg "
                                    + "where msg_id = mid and mqueue_id = ?"
                                + ")";
                        rescount = DbUtil.sqlAction(sql, param, conn, false);
                        if (rescount > 0) {
                            Log.info("update msg_detail table to remove msg success");
                        } else {
                            //set error code: queue did not delete
                            isRollBack = true;
                            Log.info("update msg_detail table to remove msg fail");
                        }
                    } else {
                        //set error code: queue did not delete
                        isRollBack = true;
                        Log.info("delete queue table fail");
                    }
                    
                } catch (SQLException | NamingException e) {
                    isRollBack = true;
                    e.printStackTrace();
                }
            }
            break;
        case SEND_TOUSER:
        case SEND_TOALL:
            try {
                //check if queue exists
                ResultSet rs = checkQueueExists(raw);
                if (!rs.next()) {
                    //queue does not exist, then ROLLBACK
                    //TODO: set error code: 
                    isRollBack = true;
                    Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    updateQueueSize(raw,1);
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

    private void updateQueueSize(Message raw, int size) throws SQLException,
            NamingException {
        //update queue size
        conn = DbUtil.getConnection(conn, "local");
        String qname = raw.getMsgQueue();
        String sql = "update queue "
                + "set qsize = ((select qsize from queue where qname = ?) + " + size + ") "
                + "where qname = ? ";
        Vector<String> param = new Vector<>();
        param.add(qname);
        param.add(qname);
        rescount = DbUtil.sqlAction(sql, param, conn, false);
        if (rescount > 0) {
            Log.info("update queue size success");
        } else {
            isRollBack = true;
            Log.info("update queue size fail");
        }
    }

    private ResultSet checkQueueExists(Message raw) throws SQLException,
            NamingException {
        conn = DbUtil.getConnection(conn, "local");
        String qname = raw.getMsgQueue();
        String sql = "select * from queue where qname = ? and qstatus = 'exists'";
        Vector<String> param = new Vector<>();
        param.add(qname);
        ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
        return rs;
    }
    
    private void processReceive(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        case READ_BYRMVMESSAGE:
            try {
                //check if queue exists, if not exist, then ROLLBACK
                ResultSet rs = checkQueueExists(raw);
                if (!rs.next()) {
                    //TODO: set error code: 
                    isRollBack = true;
                    Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    //select message, update msg_status, update queue size
                    conn = DbUtil.getConnection(conn, "local");
                    String receive = raw.getSender();
                    String qname   = raw.getMsgQueue();
                    String sql = "select msg_id, msend_id, msg_detail "
                            + "from msg, msg_detail "
                            + "where mid=msg_id and mrecv_id in (?,?) and mqueue_id=? and status='exists';";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    param.add("all");
                    param.add(qname);
                    rs = DbUtil.sqlSelect(sql, param, conn);
                    String mid = "";
                    ArrayList<Message> respMsg = new ArrayList<>();
                    while(rs.next()) {
                        Message data = new Message();
                        mid = "'" + rs.getString("msg_id") + "'" + "," + mid;
                        data.setMid(rs.getString("msg_id"));
                        data.setMid(rs.getString("msend_id"));
                        data.setMsgDetail(rs.getString("msg_detail"));
                        respMsg.add(data);
                    }
                    result.setResponMessage(respMsg);
                    
                    sql = "update msg_detail "
                            + "set status = CAST('remove' as status_enum) "
                            + "where msg_id in (" + mid.substring(0, mid.length()-1) + ")";
                    rescount = DbUtil.sqlAction(sql, conn);
                    if (rescount > 0) {
                        updateQueueSize(raw, -rescount);
                        Log.info("read by remove message success");
                    } else {
                        //TODO: set error code: queue did not delete
                        isRollBack = true;
                        Log.info("read by remove message fail");
                    }
                }
            } catch (SQLException | NamingException e) {
                isRollBack = true;
                e.printStackTrace();
            }
            break;
        case READ_BYLOOKMESSAGE:
            try {
                //check if queue exists, if not exist, then ROLLBACK
                ResultSet rs = checkQueueExists(raw);
                if (!rs.next()) {
                    //TODO: set error code: 
                    isRollBack = true;
                    Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    conn = DbUtil.getConnection(conn, "local");
                    String receive = raw.getSender();
                    String qname   = raw.getMsgQueue();
                    String sql = "select msg_id, msend_id, msg_detail "
                            + "from msg, msg_detail "
                            + "where mid=msg_id and mrecv_id=? and mqueue_id=? and status='exists';";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    param.add(qname);
                    rs = DbUtil.sqlSelect(sql, param, conn);
                    ArrayList<Message> respMsg = new ArrayList<>();
                    while(rs.next()) {
                        Message data = new Message();
                        data.setMid(rs.getString("msg_id"));
                        data.setMid(rs.getString("msend_id"));
                        data.setMsgDetail(rs.getString("msg_detail"));
                        respMsg.add(data);
                    }
                    result.setResponMessage(respMsg);
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
    
    private void processQuery(Message raw, Message result) {
        switch (raw.getMsgFunc()) {
            case QUERY_BYSENDER:
                try {
                    conn = DbUtil.getConnection(conn, "local");
                    String receive = raw.getSender();
                    String sender = raw.getMsgDetail();
                    String sql = "select distinct mid, msg_detail "
                            + "from msg, msg_detail "
                            + "where mid=msg_id and msend_id = ? and mrecv_id in (?,?) limit 1";
                    Vector<String> param = new Vector<>();
                    param.add(sender);
                    param.add(receive);
                    param.add("all");
                    ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
                    
                    while(rs.next()) {
                        result.setMsgDetail(rs.getString("msg_detail"));
                    }
                } catch (SQLException | NamingException e) {
                    isRollBack = true;
                    e.printStackTrace();
                }
                break;
            case QUERY_BYQUEUE:
                try {
                    conn = DbUtil.getConnection(conn, "local");
                    String receive = raw.getSender();
                    String sql = "select distinct mqueue_id "
                            + "from msg, msg_detail "
                            + "where mrecv_id=? and status=CAST('exists' as status_enum)";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
                    String rsQueue = "";
                    while(rs.next()) {
                        rsQueue = rs.getString("mqueue_id") + "," + rsQueue;
                    }
                    System.out.println("return Queue: " + rsQueue);
                    result.setMsgDetail(rsQueue.substring(0, rsQueue.length()-1));
                } catch (SQLException | NamingException e) {
                    isRollBack = true;
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        
    }
}
