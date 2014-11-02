package main.java.ch.ethz.systems.asl.service.middleware;

/*
 * @author: <a href="mailto:tsayk@student.ethz.ch">Kai-En Tsay(Ken)</a>
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
import main.java.ch.ethz.systems.asl.bean.ResponseCode;
import main.java.ch.ethz.systems.asl.util.DataCollector;
import main.java.ch.ethz.systems.asl.service.db.DBService;
import main.java.ch.ethz.systems.asl.util.DbUtil;
import main.java.ch.ethz.systems.asl.util.StopWatch;

public class MessageService implements Runnable {

    private static final Logger Log = Logger.getLogger(DBService.class.getName());
    
    MessageServiceMain server;
    Socket clientSocket;
    ObjectInputStream  objInputStream;
    ObjectOutputStream objOutputStream;
    String dbFarm;
    Connection conn;
    boolean isRollBack = false;
    int id;
    int rescount;
    StopWatch sw;
    
    public MessageService(Socket clientSocket, int id, MessageServiceMain server, String db) throws ClassNotFoundException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.id = id;
        this.dbFarm = db;
        //Log.info( "Connection " + id + " established with: " + clientSocket );
        sw = new StopWatch();
    }
    
    public void run() {
        sw.on();
        Message rawMsg, resultMsg = new Message();
        try {
            objInputStream = new ObjectInputStream(clientSocket.getInputStream());
            objOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            rawMsg = (Message)objInputStream.readObject();
            resultMsg = runMsgManagement(rawMsg);
            objOutputStream.writeObject(resultMsg);
            //Log.info( "Connection " + id + " closed. Execution time: " + sw.off() + "ms" );
            //System.out.println( "Connection " + id + " closed. Execution time: " + sw.off() + "ms" );
            DataCollector.putMsgMidTimeJar("msgMid", sw.off());
            clientSocket.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            resultMsg.setMsgDetail(ResponseCode.ERROR_MISSING_PSQL_JDBC_DRIVER.value());
        } catch (SQLException e) {
            e.printStackTrace();
            resultMsg.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
        }
    }
    
    public Message runMsgManagement(Message raw) throws SQLException {
        Message result = new Message();
        result = msgExecution(raw, result);
        return result;
    }
    
    private Message msgExecution(Message raw, Message result) {
        try {
            synchronized (this) {
                conn = DbUtil.getConnection(conn, dbFarm);
            }
            processMsg(raw, result);
            processMsgDtl(raw, result);
            
            switch(raw.getMsgType()) {
            case SEND:
                processSend(raw, result);
                if (result.getMsgDetail().isEmpty())
                    result.setMsgDetail(ResponseCode.OK.value());
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
            DbUtil.closeConnection(conn);
            
        } catch (SQLException | NamingException e) {
            result.setMsgDetail(ResponseCode.ERROR_DB_CONNECT_FAIL.value());
            e.printStackTrace();
        }
        return result;
    }
    
    private void processMsg(Message raw, Message result) {
        try {
            String sql = "SELECT func_insert_msg(?,?,?,?)";
            Vector<String> param = new Vector<>();
            String mid = raw.getMid();
            String sendId = raw.getSender();
            String recvId = (null != raw.getReceiver()) ? raw.getReceiver(): "";
            String queue  = (null != raw.getMsgQueue()) ? raw.getMsgQueue(): "";
            param.add(mid);
            param.add(sendId);
            param.add(recvId);
            param.add(queue);
            ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
            if (!rs.next()) {
                result.setMsgDetail(ResponseCode.ERROR_DB_MSG_TABLE_INSERT_FAIL.value());
                //Log.info("insert msg table fail");
            }
        } catch (SQLException e) {
            result.setMsgDetail(ResponseCode.ERROR_DB_CONNECT_FAIL.value());
            e.printStackTrace();
        }
    }
    
    private void processMsgDtl(Message raw, Message result) {
        try {
            String sql = "SELECT func_insert_msgdtl(?,?,?,?)";
            Vector<String> param = new Vector<>();
            String mid = raw.getMid();
            String msgType = raw.getMsgType().value();
            String msgFunc = raw.getMsgFunc().value();
            String msgDtl = raw.getMsgDetail();
            param.add(mid);
            param.add(msgType);
            param.add(msgFunc);
            param.add(msgDtl);
            ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
            if (!rs.next()) {
                result.setMsgDetail(ResponseCode.ERROR_DB_MSG_DETAIL_TABLE_INSERT_FAIL.value());
                //Log.info("insert msg_detail table fail");
            }
        } catch (SQLException e) {
            result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
            e.printStackTrace();
        }
    }
    
    private void processSend(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        case CTR_CREATEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    ResultSet rs = checkQueueExists(raw);
                    if (!rs.next()) {
                        //queue does exists, can execute queue create
                        conn = DbUtil.getConnection(conn, "local");
                        String qname = raw.getMsgQueue();
                        String sql = "SELECT func_insert_queue(?)";
                        Vector<String> param = new Vector<>();
                        param.add(qname);
                        rs = DbUtil.sqlSelect(sql, param, conn);
                        if (!rs.next()) {
                            //set error code: queue did not insert
                            isRollBack = true;
                            result.setMsgDetail(ResponseCode.ERROR_DB_QUEUE_TABLE_INSERT_FAIL.value());
                            //Log.info("insert queue table fail");
                        }
                    } else {
                        result.setMsgDetail(ResponseCode.ERROR_QUEUE_ALREADY_EXISTS.value());
                        //Log.info("queue already exists");
                    }
                } catch (SQLException | NamingException e) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
                    e.printStackTrace();
                }
            }
            break;
        case CTR_DELETEQ:
            if(null!= raw.getMsgDetail()) {
                try {
                    //remove queue data
                    String qname = raw.getMsgQueue();
                    String sql = "SELECT func_delete_queue(?);";
                    Vector<String> param = new Vector<>();
                    param.add(qname);
                    ResultSet rs =DbUtil.sqlSelect(sql, param, conn);
                    if (rs.next()) {
                        //remove message in msg, msg_detail belong to this queue
                        sql = "SELECT func_delete_msg_detail(?)";
                        param = new Vector<>();
                        param.add(qname);
                        rs =DbUtil.sqlSelect(sql, param, conn);
                        sql = "SELECT func_delete_msg(?)";
                        rs =DbUtil.sqlSelect(sql, param, conn);
                        if (!rs.next()) {
                            isRollBack = true;
                            result.setMsgDetail(ResponseCode.ERROR_DB_QUEUE_TABLE_DELETE_FAIL.value());
                            //Log.info("delete msg, msg_detail table to remove msg fail");
                        }
                    } else {
                        //set error code: queue did not delete
                        isRollBack = true;
                        result.setMsgDetail(ResponseCode.ERROR_DB_QUEUE_TABLE_DELETE_FAIL.value());
                        //Log.info("delete queue table fail");
                    }
                } catch (SQLException e) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
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
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_QUEUE_NOT_EXISTS.value());
                    //Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    updateQueueSize(raw,1);
                }
            } catch (SQLException e) {
                isRollBack = true;
                result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
                e.printStackTrace();
            }            
            break;
        default:
            break;
        }
    }

    private void updateQueueSize(Message raw, int size) throws SQLException {
        //update queue size
        String qname = raw.getMsgQueue();
        String sql = "update queue "
                + "set qsize = ((select qsize from queue where qname = ?) + " + size + ") "
                + "where qname = ? ";
        Vector<String> param = new Vector<>();
        param.add(qname);
        param.add(qname);
        rescount = DbUtil.sqlAction(sql, param, conn, false);
        if (rescount == 0) {
            isRollBack = true;
            //Log.info("update queue size fail");
        }
    }

    private ResultSet checkQueueExists(Message raw) throws SQLException {
        String qname = raw.getMsgQueue();
        String sql = "select * from queue where qname = ?";
        Vector<String> param = new Vector<>();
        param.add(qname);
        ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
        return rs;
    }
    
    private void processReceive(Message raw, Message result) {
        switch(raw.getMsgFunc()) {
        //[Definition]: Clients can read a queue by removing the topmost message 
        case READ_BYRMVMESSAGE:
            try {
                //check if queue exists, if not exist, then ROLLBACK
                ResultSet rs = checkQueueExists(raw);
                if (!rs.next()) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_QUEUE_NOT_EXISTS.value());
                    //Log.info("queue does not exists, ROLLBACK!!");  
                } else {
                    //select message, remove top 1 from msg, msg_detail, update queue size
                    String mid = "";
                    String receive = raw.getSender();
                    String qname   = raw.getMsgQueue();
                    String sql = "select msg_id, msend_id, msg_detail "
                               + "from msg, msg_detail "
                               + "where mid=msg_id and mrecv_id in (?,?) and mqueue_id=? limit 1";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    param.add("all");
                    param.add(qname);
                    rs = DbUtil.sqlSelect(sql, param, conn);
                    
                    while (rs.next()) {
                        mid = rs.getString("msg_id");
                        result.setMid(mid);
                        result.setSender(rs.getString("msend_id"));
                        result.setMsgDetail(rs.getString("msg_detail"));
                    }
                    
                    //delete from msg and msg_detail only if there is content in table
                    if ( !mid.equals("") ) {
                        sql = "SELECT func_delete_msg_byid(?)";
                        param = new Vector<>();
                        param.add(mid);
                        DbUtil.sqlSelect(sql, param, conn);
                        sql = "SELECT func_delete_msg_detail_byid(?)";
                        rs = DbUtil.sqlSelect(sql, param, conn);
                        if (rs.next()) {
                            updateQueueSize(raw, -1);
                        } else {
                            isRollBack = true;
                            result.setMsgDetail(ResponseCode.ERROR_DB_MSG_DETAIL_TABLE_DELETE_FAIL.value());
                            //Log.info("read by remove message fail");
                        }
                    } else {
                        result.setMsgDetail(ResponseCode.NO_DATA_IN_QUEUE.value());
                        //Log.info("no data in queue for client");
                    }
                }
            } catch (SQLException e) {
                isRollBack = true;
                result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
                e.printStackTrace();
            }
            break;
        case READ_BYLOOKMESSAGE:
            try {
                //check if queue exists, if not exist, then ROLLBACK
                ResultSet rs = checkQueueExists(raw);
                if (!rs.next()) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_QUEUE_NOT_EXISTS.value());
                    //Log.info("queue does not exists, ROLLBACK!!");
                } else {
                    String mid = "";
                    String receive = raw.getSender();
                    String qname   = raw.getMsgQueue();
                    String sql = "select msg_id, msend_id, msg_detail "
                               + "from msg, msg_detail "
                               + "where mid=msg_id and mrecv_id in (?,?) and mqueue_id=? limit 1;";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    param.add("all");
                    param.add(qname);
                    rs = DbUtil.sqlSelect(sql, param, conn);
                    while (rs.next()) {
                        mid = rs.getString("msg_id");
                        result.setMid(mid);
                        result.setSender(rs.getString("msend_id"));
                        result.setMsgDetail(rs.getString("msg_detail"));
                    }
                    
                    if (mid.equals("")) {
                        result.setMsgDetail(ResponseCode.NO_DATA_IN_QUEUE.value());
                        //Log.info("no data in queue for client");
                    }
                }
            } catch (SQLException e) {
                isRollBack = true;
                result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
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
                    
                    String msg_detail = "";
                    while(rs.next()) {
                        msg_detail = rs.getString("msg_detail");
                        result.setMsgDetail(msg_detail);
                    }
                    if (msg_detail.equals("")) {
                        result.setMsgDetail(ResponseCode.NO_DATA_IN_QUEUE.value());
                        //Log.info("No data in queue for client");
                    }
                } catch (SQLException e) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
                    e.printStackTrace();
                }
                break;
            case QUERY_BYQUEUE:
                try {
                    String receive = raw.getSender();
                    String sql = "select distinct mqueue_id "
                               + "from msg "
                               + "where mrecv_id in (?, ?)";
                    Vector<String> param = new Vector<>();
                    param.add(receive);
                    param.add("all");
                    ResultSet rs = DbUtil.sqlSelect(sql, param, conn);
                    String rsQueue = "";
                    while(rs.next()) {
                        rsQueue = rs.getString("mqueue_id") + "," + rsQueue;
                    }
                    if (rsQueue.equals("")) {
                        //Log.info("No queue has client data");
                        result.setMsgDetail(ResponseCode.NO_QUEUE_HAS_DATA.value());
                    } else {
                        result.setMsgDetail(rsQueue.substring(0, rsQueue.length()-1));
                    }
                } catch (SQLException e) {
                    isRollBack = true;
                    result.setMsgDetail(ResponseCode.ERROR_DB_SQL_FAIL.value());
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        
    }
}
