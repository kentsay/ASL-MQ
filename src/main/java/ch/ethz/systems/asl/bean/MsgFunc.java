package main.java.ch.ethz.systems.asl.bean;

public enum MsgFunc {
    
    CTR_CREATEQ("ctr_createQ"),
    CTR_DELETEQ("ctr_deleteQ"),
    SEND_TOUSER("send_toUser"),
    SEND_TOALL("send_toAll"),
    READ_BYRMVMESSAGE("read_byRmvMsg"),
    READ_BYLOOKMESSAGE("read_byLookMsg"),
    QUERY_BYSENDER("query_bySender"),
    QUERY_BYQUEUE("query_byQueue");
    
    private String type;
    
    private MsgFunc(String type) {
        this.type = type;
    }
    
    public String value() {
        return type;
    }
    
}
