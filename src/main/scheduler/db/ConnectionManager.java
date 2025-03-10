package scheduler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private final String driverName = "org.sqlite.JDBC";
    private final String connectionUrl = "jdbc:sqlite:" + System.getenv("DBPath");

    private Connection con = null;

    public ConnectionManager() {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    public Connection createConnection() {
        try {
            con = DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    public void closeConnection() {
        try {
            this.con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
