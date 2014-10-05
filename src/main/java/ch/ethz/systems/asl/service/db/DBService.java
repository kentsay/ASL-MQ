package main.java.ch.ethz.systems.asl.service.db;
/*
 * @author: <a href="mailto:tsayk@student.ethz.ch">Kai-En Tsay(Ken)</a>
 * @version: 1.0
 * 
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.postgresql.ds.PGPoolingDataSource;

public final class DBService {

    private static final Logger Log = Logger.getLogger(DBService.class.getName());

	private static volatile int usedConnectionCount = 0;
	private static volatile int getConnectionCount = 0;

	public static final String CONFIG_PROPERTIES_FILENAME = "config" + File.separator + "dbservice.properties";
	public static final String CONN_ROLENAMES        = "dbservice.connection.rolenames";
	public static final String CONN_SERVER           = "dbservice.connection.server";
	public static final String CONN_DBNAME           = "dbservice.connection.dbname";
	public static final String CONN_DRIVER_CLASS     = "dbservice.connection.driverClass";
	public static final String CONN_USERNAME         = "dbservice.connection.username";
	public static final String CONN_URL              = "dbservice.connection.url";
	public static final String CONN_PASSWORD         = "dbservice.connection.password";
	public static final String CONN_MAXCONN          = "dbservice.connection.maxconnection";
	public static final String PROPERTY_RETRY_COUNT  = "dbservice.connection.retryCount";
	public static final String PROPERTY_RETRY_PERIOD = "dbservice.connection.retryPeriod";

	private Properties props = null;

	int retryCount = 0;
	int retryPeriod = 10; //ms

	// The data source.
	private Hashtable<String, DataSource> nameToDsMap = null;
	private int executeUpdateTime = 10;
	private int executeQueryTime = 20;
	private int borrowTime = 40;

	// Make DBService Singleton
	private static final DBService INSTANCE = new DBService();

    /**
     * Creates a new DBService object.
	 */
	private DBService() {
	}

	/**
	 * Gets an instance of the DBService, or null if the DBService failed to initialize itself.
	 * 
	 * @return the DBService instance.
	 */
	public static DBService getDBService() {
	    return INSTANCE;
	}

	/**
	 * Load Props from file.
	 * 
	 * @param propPath
	 *            the properties path.
	 * @return the Props.
	 */
	private Properties getProps(String propPath) {
	    InputStream is = null;
	    Properties properties = null;
	    try {
	        try {
	            is = new FileInputStream(propPath);
	        } catch (FileNotFoundException e) {
	            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(propPath);
	        }
	        properties = new Properties();
	        properties.load(is);
	    } catch (Exception e) {
	        Log.log(Level.SEVERE, "DBService init() loading properties error." + e);
	        return null;
	    } finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (Exception e) {
	                Log.log(Level.SEVERE, "Close properties file error." + e);
	            }
	        }
	    }
	    return properties;
	}

	/**
	 * Load config data from properties.
	 * 
	 * @throws NamingException 
	 */
	private void init() throws NamingException {
	    if (nameToDsMap == null) {
	        synchronized (this) {
	            if (nameToDsMap == null) {
	                this.props = getProps(CONFIG_PROPERTIES_FILENAME);
	                String userNames = this.props.getProperty(CONN_ROLENAMES);
	                StringTokenizer st = new StringTokenizer(userNames, ",");
	                
	                String name;
	                DataSource ds;
	                nameToDsMap = new Hashtable<String, DataSource>();

	                while (st.hasMoreTokens()) {
	                    name = st.nextToken();
	                    ds = setupDs(this.props, name);
	                    nameToDsMap.put(name, ds);
	                }

	                String rc = this.props.getProperty(PROPERTY_RETRY_COUNT);
	                String rp = this.props.getProperty(PROPERTY_RETRY_PERIOD);

	                if (rc != null) this.retryCount = Integer.parseInt(rc);
	                if (rp != null) this.retryPeriod = Integer.parseInt(rp);
	            }
	        }
	    }
	}

	private DataSource setupDs(Properties props, String name) {
 
		PGPoolingDataSource source = new PGPoolingDataSource();
		source.setDataSourceName(name);
		source.setServerName(props.getProperty(CONN_SERVER + "." + name));
		source.setDatabaseName(props.getProperty(CONN_DBNAME + "." + name));
		source.setUser(props.getProperty(CONN_USERNAME + "." + name));
		source.setPassword(props.getProperty(CONN_PASSWORD + "." + name));
		source.setMaxConnections(Integer.parseInt(props.getProperty(CONN_MAXCONN + "." + name)));
		
		return source;
	}
	
	public Connection getConnection(String roleName) throws SQLException, NamingException {
        return this.getConnection(roleName, retryCount);
    }
	/**
	 * Get a database connection. The auto-commit mode is off.
	 * 
	 * @param roleName
	 * 
	 * @param retry
	 *            Retry counter.
	 * 
	 * @return A Connection object.
	 * 
	 * @throws SQLException
	 *             if access db error.
	 * @throws NamingException 
	 */
	public Connection getConnection(String roleName, int retry) throws SQLException, NamingException {
		this.init();

		Connection conn = null;
		long before = System.currentTimeMillis();
		long diffTime = 0;
		try {

			getConnectionCount++;
			Log.info("getConnectionCount = " + String.valueOf(getConnectionCount));

			before = System.currentTimeMillis();
			DataSource source = (DataSource) nameToDsMap.get(roleName);
			conn = source.getConnection();
			diffTime = System.currentTimeMillis() - before;
			if (diffTime >= this.borrowTime) {
				Log.info("DBCP borrow connection time: " + diffTime + " ms.");
			}

			usedConnectionCount++;
			Log.info("Used connections = " + String.valueOf(usedConnectionCount));

		} catch (SQLException e) {
			diffTime = System.currentTimeMillis() - before;
			Log.info("Get connection error:" + diffTime + " ms.");
			if (retry > 0) {
				try {
					Thread.sleep(retryPeriod);
				} catch (Exception ex) {
				}

				final int remainRetryTimes = retry - 1;
				conn = this.getConnection(roleName, remainRetryTimes);
			} else {
				throw e;
			}
		}

		conn.setAutoCommit(false);
		return conn;
	}

	/**
	 * Return a connection to the pool.
	 * 
	 * @param conn
	 *            The Database Connection object to return.
	 * 
	 * @return <i>true</i> if the command is successful; <i>false</i> otherwise.
	 */
	public boolean close(Connection conn) {
		boolean b = true;
		
		if (conn == null) return b;
		try {
			if (!conn.isClosed()) {
				conn.close();
				usedConnectionCount--;
				getConnectionCount--;
				Log.info("Used connections = "   + String.valueOf(usedConnectionCount));
				Log.info("getConnectionCount = " + String.valueOf(getConnectionCount));
			}
		} catch (SQLException e) {
			Log.info("Close connection error.");
			b = false;
		}
		return b;
	}
	
	/**
     * Execute a SQL SELECT command.
     * 
     * @param command
     *            The SQL SELECT command.
     * @param conn
     *            A database connection. <i>CANNOT</i> be <i>null</i>.
     * 
     * @return the SqlResult object.
     * 
     * @throws SQLException
     *             if access db error.
     */
    public ResultSet select(String command, Connection conn) throws SQLException {
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();

            long before = System.currentTimeMillis();
            rs = stmt.executeQuery(command);
            long diffTime = System.currentTimeMillis() - before;

            if (diffTime >= this.executeQueryTime) {
                String s = "SQL stmt query time: " + diffTime + " ms[" + command + "]";
                Log.info(s);
            }
        } catch (SQLException e) {
            throw new SQLException("sql:" + command + "\n" + e.getMessage(), e.getSQLState(), e.getErrorCode());
        }

        return rs;
    }
    
    /**
     * Execute a SQL SELECT command in the form of a prepared statement.
     * 
     * @param command
     *            The SELECT statement.
     * @param params
     *            Parameters vector.
     * @param conn
     *            The DB connection.
     * 
     * @return the SqlResult object.
     * 
     * @throws SQLException
     *             if access db error.
     */
    public ResultSet select(String command, Vector<?> params, Connection conn) throws SQLException {
        String pSql = makeSql(command, params);

        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(command);

            int paramsSize = params.size();

            for (int i = 0; i < paramsSize; i++) {
             // use set Object to process non string field
                stmt.setObject(i + 1, params.get(i));
            }

            long before = System.currentTimeMillis();
            rs = stmt.executeQuery();

            long diffTime = System.currentTimeMillis() - before;

            if (diffTime >= this.executeQueryTime) {
                String s = "SQL pstmt query time: " + diffTime + " ms[" + pSql + "]";
                Log.warning(s);
            }

        } catch (SQLException e) {
            throw new SQLException("sql:" + pSql + "\n" + e.getMessage(), e.getSQLState(), e.getErrorCode());
        } finally {
            this.close(rs);
            this.close(stmt);
        }
        return rs;
    }
    
    /**
     * Execute a SQL INSERT/UPDATE/DELETE command, end with commit
     * 
     * @param command
     *            The INSERT/UPDATE/DELETE statement.
     * @param conn
     *            The DB connection.
     * @param bRollback
     * @return integer
     *            Row number been effect
     * 
     * @throws SQLException
     *             if access db error.
     */
    public int sqlAction(String command, Connection conn, boolean isRollback) throws SQLException {

        int rescount = 0;
        Statement stmt = null;

        try {
            stmt = conn.createStatement();
            long before = System.currentTimeMillis();
            rescount = stmt.executeUpdate(command);

            long diffTime = System.currentTimeMillis() - before;

            if (diffTime >= this.executeUpdateTime) {
                String s = "SQL stmt update time: " + diffTime + " ms[" + command + "]";
                Log.warning(s);
            }
            
        } catch (SQLException e) {
            if (isRollback) {
                conn.rollback(); // remove dbcp rollback and add here
            }
            throw new SQLException("sql:" + command + "\n" + e.getMessage(), e.getSQLState(), e.getErrorCode());
        } finally {
            this.close(stmt);
        }

        return rescount;
    }

    /**
     * Execute a SQL INSERT/UPDATE/DELETE command in the form of a prepared
     * statement.
     * 
     * @param command
     *            The INSERT/UPDATE/DELETE statement.
     * @param params
     *            Parameters vector.
     * @param conn
     *            The DB connection.
     * @param bRollback
     * @return integer
     *            Row number been effect
     * 
     * @throws SQLException
     *             if access db error.
     */
    public int sqlAction(String command, Vector<?> params, Connection conn,
            boolean bRollback) throws SQLException {
        String sqlStatement = makeSql(command, params);

        int rescount = 0;
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(command);

            int paramsSize = params.size();

            for (int i = 0; i < paramsSize; i++) {
                // use set Object to process non string field
                stmt.setObject(i + 1, params.get(i));
            }

            long before = System.currentTimeMillis();
            rescount = stmt.executeUpdate();
            long diffTime = System.currentTimeMillis() - before;

            if (diffTime >= this.executeUpdateTime) {
                String s = "SQL pstmt update time: " + diffTime + " ms[" + sqlStatement + "]";
                Log.warning(s);
            }

        } catch (SQLException e) {
            if (bRollback) {
                conn.rollback(); // remove dbcp rollback and add here
            }
            throw new SQLException("sql:" + sqlStatement + "\n" + e.getMessage(), e.getSQLState(), e.getErrorCode());
        } finally {
            this.close(stmt);
        }
        return rescount;
    }
    
    /**
     * Transfer prepare statement to sql command String.
     * 
     * @param command
     *            the prepare statement.
     * @param params
     *            the prepare statement parameters.
     * 
     * @return the transfered sql command String.
     */
    private String makeSql(String command, Vector<?> params) {
        StringBuffer cmd = new StringBuffer(command);

        int offset = 0;
        int i = 0;
        String value;

        while (true) {
            offset = cmd.indexOf("?", offset);
            if (offset == -1) break;

            String paramVal = (params.get(i) != null) ? params.get(i).toString() : null;
            value = (paramVal == null) ? "null" : ("'" + paramVal + "'");
            cmd.replace(offset, offset + 1, value);
            offset += value.length();
            i++;
        }

        return cmd.toString();
    }
    
    /**
     * A convenient method for closing a result set without lousy code.
     * 
     * @param rs
     *            the result set object to be closed.
     */
    private void close(ResultSet rs) {
        if (rs == null) {
            return;
        }

        try {
            rs.close();
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Close result set error.", e);
        }
    }

    /**
     * A convenient method for closing a statement without lousy code.
     * 
     * @param stmt
     *            the statement to be closed.
     */
    private void close(Statement stmt) {
        if (stmt == null) {
            return;
        }

        try {
            stmt.close();
        } catch (SQLException e) {
            Log.log(Level.SEVERE, "Close statement  error.", e);
        }
    }

}
