package fpuna.ebye.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Bd {

    private static final String url = "jdbc:postgresql://localhost:5432/ebye";
    private static final String user = "postgres";
    private static final String password = "emmote";

    /**
     * @return objeto Connection
     */
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

}
