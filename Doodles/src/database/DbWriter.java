package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Runs "save this" database writes on one background thread with ONE reused connection.
 *
 * Why: the game used to open a brand-new MySQL connection for every single brush movement,
 * on the same thread that forwards the drawing to the other players, which made the canvas
 * lag badly for everybody. Now the game thread only queues the write and carries on.
 */
final class DbWriter {

    interface Job {
        void run(Connection connection) throws SQLException;
    }

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10_000),
            runnable -> {
                Thread t = new Thread(runnable, "Doodles-DbWriter");
                t.setDaemon(true);
                return t;
            });

    /** Only ever touched by the writer thread. */
    private static Connection shared;

    private DbWriter() {}

    static void submit(Job job) {
        try {
            EXECUTOR.execute(() -> {
                try {
                    if (shared == null || shared.isClosed()) {
                        shared = DataBaseConnection.connect();
                    }
                    if (shared == null) {
                        return;                       // database is down: skip the save, the game goes on
                    }
                    job.run(shared);
                } catch (SQLException e) {
                    System.out.println("Database write failed: " + e.getMessage());
                    closeShared();
                }
            });
        } catch (RejectedExecutionException ignored) {
            // queue full (database far too slow): drop this save rather than block the game
        }
    }

    private static void closeShared() {
        try {
            if (shared != null) shared.close();
        } catch (SQLException ignored) {
        }
        shared = null;
    }
}
