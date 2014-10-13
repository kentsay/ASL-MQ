package main.java.ch.ethz.systems.asl.bean;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    
    private static final long serialVersionUID = 6572077450962477506L;
    
    private String mid           = "";
    private String sender        = "";
    private String receiver      = "";
    private String msgQueue      = "";
    private String msgArriveTime = "";
    private MsgType msgType;
    private MsgFunc msgFunc;
    private String msgDetail     = "";
    private ArrayList<Message> responMessage;
    
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
    public MsgType getMsgType() {
        return msgType;
    }
    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }
    public MsgFunc getMsgFunc() {
        return msgFunc;
    }
    public void setMsgFunc(MsgFunc msgFunc) {
        this.msgFunc = msgFunc;
    }
    public String getMsgDetail() {
        return msgDetail;
    }
    public void setMsgDetail(String msgDetail) {
        this.msgDetail = msgDetail;
    }
    public ArrayList<Message> getResponMessage() {
        return responMessage;
    }
    public void setResponMessage(ArrayList<Message> responMessage) {
        this.responMessage = responMessage;
    }
    
}
