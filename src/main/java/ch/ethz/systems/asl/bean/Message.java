package main.java.ch.ethz.systems.asl.bean;

public class Message {
    
    private String mid           = "";
    private String sender        = "";
    private String receiver      = "";
    private String msgQueue      = "";
    private String msgArriveTime = "";
    private String msgType       = "";
    private String msgFunc       = "";
    private String msgDetail     = "";
    
    public String getMid() {
        return mid;
    }
    public void setMid(String mid) {
        this.mid = mid;
    }
    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    public String getMsgQueue() {
        return msgQueue;
    }
    public void setMsgQueue(String msgQueue) {
        this.msgQueue = msgQueue;
    }
    public String getMsgArriveTime() {
        return msgArriveTime;
    }
    public void setMsgArriveTime(String msgArriveTime) {
        this.msgArriveTime = msgArriveTime;
    }
    public String getMsgType() {
        return msgType;
    }
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
    public String getMsgFunc() {
        return msgFunc;
    }
    public void setMsgFunc(String msgFunc) {
        this.msgFunc = msgFunc;
    }
    public String getMsgDetail() {
        return msgDetail;
    }
    public void setMsgDetail(String msgDetail) {
        this.msgDetail = msgDetail;
    }
    
}
