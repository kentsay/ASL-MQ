package main.java.ch.ethz.systems.asl.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.naming.NamingException;

import main.java.ch.ethz.systems.asl.service.db.DBService;

public class DbUtil {
    private static DBService dbService = DBService.getDBService();
    
    public static final Connection getConnection(Connection conn, String profile) throws SQLException, NamingException {
        if (null != conn)
            return conn;
        else 
            return dbService.getConnection(profile);
    }
    
    public static final int sqlAction(String sqlCmd, Vector<?> params, 
            Connection conn, boolean isRollback) throws SQLException {
        return dbService.sqlAction(sqlCmd, params, conn, isRollback);
    }
        
    public static final int sqlAction(String sqlCmd, Connection conn,
            boolean isRollback) throws SQLException {
        return dbService.sqlAction(sqlCmd, conn, isRollback);
    }

    public static final int sqlAction(String sqlCmd, Connection conn) 
            throws SQLException {
        return dbService.sqlAction(sqlCmd, conn, false);
    }
    
    public static final ResultSet sqlSelect(String sqlCmd, Vector<?> params,
            Connection conn) throws SQLException {
        return dbService.select(sqlCmd, params, conn);
    }

    
    
}
