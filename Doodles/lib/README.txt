Put the MySQL JDBC driver here:  mysql-connector-j-<version>.jar

(download from https://dev.mysql.com/downloads/connector/j/  ->  "Platform Independent" -> unzip -> copy the .jar)

IntelliJ and the run scripts (run-server / run-client) pick up every .jar in this folder automatically.
Without the jar the game still runs, but nothing is saved to MySQL (the server console says so at startup).
