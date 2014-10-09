package main.java.ch.ethz.systems.asl.bean;

public enum MsgType {
    
    SEND("send"),
    RECEIVE("receive"),
    QUERY("query");
    
    private String type;
    
    private MsgType(String type) {
        this.type = type;
    }
    
    public String value() {
        return type;
    }
}