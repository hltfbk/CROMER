package servlet.storage;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 22/01/14
 * Time: 15.04
 */
import servlet.WebController;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DBConnection {

    private String sDriverName=null;
    private String sServerName=null;
    private String sPort=null;
    private String sDatabaseName=null;
    private String sUserName=null;
    private String sPassword=null;

    private Connection conn = null;

    public DBConnection() {
        Properties prop = new Properties();
        try {
            InputStream dbconf = Thread.currentThread().getContextClassLoader().getResourceAsStream("../dbconnection.conf");
            prop.load(dbconf);
            // Extraction the properties
            this.sDriverName = prop.getProperty("driver.name");
            this.sServerName = prop.getProperty("server.name");
            this.sPort = prop.getProperty("server.port");
            this.sDatabaseName = prop.getProperty("database.name");
            this.sUserName = prop.getProperty("user.name");
            this.sPassword = prop.getProperty("user.password");
            dbconf.close();

            conn = getDBConnection();

        } catch (Exception e) {
            WebController.logger.info(e.getMessage());
        }
    }

    public boolean checkDBConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = new DBConnection().getDBConnection();
                if (conn != null && !conn.isClosed()) {
                    WebController.logger.debug("Attempted the MYSQL connection... OK!");
                    return true;
                }
            } else {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Statement getStatement () {
        Statement stm = null;
        try {
            stm = conn.createStatement();
            //con questo catch prendo i casi in cui conn == null e in cui lo stm == null o ci sono dei problemi di connessione (vedi wait_timeout di MySQL)
        } catch (Exception e) {
            try {
                conn = new DBConnection().getDBConnection();
                stm = conn.createStatement();
            } catch (Exception e1) {
                return null;
            }
        }
        return stm;
    }

    public ResultSet executeQuery(String query, String user) {
        Statement stm = getStatement();
        if (stm != null) {
            try {
                return stm.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        WebController.logger.error("QUERY FAILED ("+user+" "+stm+"): "+query);
        return null;
    }

    public boolean executeUpdate(String query, String user) {
        Statement stm = getStatement();
        if (stm != null) {
            try {
                return stm.executeUpdate(query) == 1;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        WebController.logger.error("QUERY FAILED ("+user+ " "+stm+"): "+query);
        return false;
    }

    public Connection getDBConnection() throws Exception {
        Connection conn = null;
        try {
            // Register a driver for the MySQL database
            Class.forName(sDriverName);
            // Create a url for accessing the MySQL
            String url = "jdbc:mysql://"+sServerName+":"+sPort+"/"+sDatabaseName+"?autoReconnect=true&autoReconnectForPools=true";
            // User the DriverManager to get a Connection to the database using user and passord
            conn = DriverManager.getConnection(url, sUserName, sPassword);
        } catch (ClassNotFoundException ex) {
            WebController.logger.error("Driver could not be loaded: " + ex);
        }

        return conn;
    }

}
