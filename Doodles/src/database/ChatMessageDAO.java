package database;

import java.sql.PreparedStatement;

public class ChatMessageDAO {

    /** Queues the message for saving on the background writer; never blocks the game thread. */
    public static void saveMessage(
            int gameId,
            int playerId,
            String message
    ) {

        if (gameId <= 0 || playerId <= 0) {
            return;                                   // no database session for this game
        }

        DbWriter.submit(connection -> {

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    "INSERT INTO chat_messages " +
                                            "(game_id, player_id, message) " +
                                            "VALUES (?, ?, ?)"
                            )
            ) {

                statement.setInt(1, gameId);
                statement.setInt(2, playerId);
                statement.setString(3, message);
                statement.executeUpdate();
            }
        });
    }
}
