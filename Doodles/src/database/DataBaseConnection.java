package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the MySQL database.
 *
 * Settings can be overridden without touching the code:
 *   -Ddoodles.db.url=...  -Ddoodles.db.user=...  -Ddoodles.db.password=...
 * (or the DOODLES_DB_URL / DOODLES_DB_USER / DOODLES_DB_PASSWORD environment variables).
 */
public class DataBaseConnection {

    private static final String URL = setting("doodles.db.url", "DOODLES_DB_URL",
            "jdbc:mysql://localhost:3306/doodles_db?connectTimeout=3000&allowPublicKeyRetrieval=true");

    private static final String USER = setting("doodles.db.user", "DOODLES_DB_USER", "root");

    private static final String PASSWORD = setting("doodles.db.password", "DOODLES_DB_PASSWORD", "Doodles@123");

    private static final String[] TABLES = {"players", "game_sessions", "drawings", "chat_messages"};

    /** After a failed attempt, don't hammer (and wait on) a database that is known to be down. */
    private static final long RETRY_AFTER_MS = 10_000;

    private static volatile long retryAt = 0;
    private static volatile boolean announced = false;

    private static String setting(String property, String env, String fallback) {
        String v = System.getProperty(property);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return v == null || v.isEmpty() ? fallback : v;
    }

    /** Returns an open connection, or null if the database cannot be reached right now. */
    public static Connection connect() {

        if (System.currentTimeMillis() < retryAt) {
            return null;
        }

        try {

            Connection connection =
                    DriverManager.getConnection(
                            URL,
                            USER,
                            PASSWORD
                    );

            retryAt = 0;

            if (!announced) {
                announced = true;
                System.out.println(
                        "Database connected successfully!"
                );
            }

            return connection;

        } catch (SQLException e) {

            retryAt = System.currentTimeMillis() + RETRY_AFTER_MS;
            announced = false;

            System.out.println(
                    "Database connection failed: " + explain(e)
            );

            return null;
        }
    }

    /**
     * Checks everything the game needs from MySQL and prints a clear report.
     * Called once when the server starts. Returns true when the database is fully usable.
     */
    public static boolean selfTest() {

        System.out.println("Checking database: " + URL.replaceAll("\\?.*", "") + " (user '" + USER + "')");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("  [FAIL] MySQL driver (mysql-connector-j .jar) is not on the classpath.");
            System.out.println("         IntelliJ: File > Project Structure > Libraries > + > Java > pick the .jar,");
            System.out.println("         then tick the module. Or drop the .jar into the lib/ folder.");
            return false;
        }

        Connection connection = connect();
        if (connection == null) {
            return false;
        }

        try (Connection c = connection; Statement st = c.createStatement()) {

            boolean ok = true;

            for (String table : TABLES) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    System.out.println("  [ OK ] table " + table + " (" + rs.getInt(1) + " rows)");
                } catch (SQLException e) {
                    ok = false;
                    System.out.println("  [FAIL] table " + table + " is missing -> run database/doodles_database.sql");
                }
            }

            return ok;

        } catch (SQLException e) {

            System.out.println("  [FAIL] " + explain(e));
            return false;
        }
    }

    /** Turns the usual MySQL / JDBC errors into something a human can act on. */
    private static String explain(SQLException e) {

        String msg = String.valueOf(e.getMessage());
        String state = String.valueOf(e.getSQLState());

        if (msg.contains("No suitable driver")) {
            return "the MySQL driver .jar is not on the classpath (add mysql-connector-j to the project libraries / lib folder).";
        }
        if (e.getErrorCode() == 1045 || state.equals("28000")) {
            return "wrong MySQL user or password (see DataBaseConnection.java). " + msg;
        }
        if (e.getErrorCode() == 1049) {
            return "database 'doodles_db' does not exist -> run database/doodles_database.sql in MySQL.";
        }
        if (state.startsWith("08") || msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "cannot reach MySQL on localhost:3306 - is the MySQL server running?";
        }
        return msg;
    }
}
