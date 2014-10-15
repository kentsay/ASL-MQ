package main.java.ch.ethz.systems.asl.bean;

public enum ResponseCode {
    
    OK("OK"),
    ERROR_MISSING_PSQL_JDBC_DRIVER("Missing PSQL JDBC Driver"),
    ERROR_SERVER_FAIL("Message Service on start fail"),
    ERROR_SERVER_ACCEPT_FAIL("Message Service accept message fail"),
    ERROR_DB_CONNECT_FAIL("DB Error"),
    ERROR_DB_SQL_FAIL("DB SQL Command Error"),
    ERROR_DB_MSG_TABLE_INSERT_FAIL("DB INSERT msg table fail"),
    ERROR_DB_MSG_DETAIL_TABLE_INSERT_FAIL("DB INSERT msg_detail table fail"),
    ERROR_DB_MSG_DETAIL_TABLE_DELETE_FAIL("DB Delete msg_detail table fail"),
    ERROR_DB_QUEUE_TABLE_INSERT_FAIL("DB INSERT queue table fail"),
    ERROR_DB_QUEUE_TABLE_DELETE_FAIL("DB DELETE queue table fail"),
    ERROR_QUEUE_NOT_EXISTS("Queue doest not exists");
    
    private String type;
    
    private ResponseCode(String type) {
        this.type = type;
    }
    
    public String value() {
        return type;
    }
}
