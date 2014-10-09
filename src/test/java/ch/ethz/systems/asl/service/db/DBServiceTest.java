package test.java.ch.ethz.systems.asl.service.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

import main.java.ch.ethz.systems.asl.service.db.DBService;

import org.junit.Test;

public class DBServiceTest {

    @Test
    public void DBConnectionTest() {
        try {
            Connection conn = DBService.getDBService().getConnection("local");
            assertTrue(!conn.isClosed());
            DBService.getDBService().close(conn);
            assertTrue(conn.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void DBConnectionPoolTest() {
        try {
            Connection conn = DBService.getDBService().getConnection("local");
            Connection conn2 = DBService.getDBService().getConnection("test");
            assertTrue(!conn.isClosed());
            DBService.getDBService().close(conn);
            assertTrue(conn.isClosed());
            assertTrue(!conn2.isClosed());
            DBService.getDBService().close(conn2);
            assertTrue(conn2.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void DBSelectTest() {
        try {
            Connection conn = DBService.getDBService().getConnection("local");
            String sql = "select * from queue";
            ResultSet rs = DBService.getDBService().select(sql, conn);
            assertNotNull(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}
