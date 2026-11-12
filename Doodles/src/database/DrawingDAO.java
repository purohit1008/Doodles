package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DrawingDAO {

    /** Queues the stroke for saving on the background writer; never blocks the game thread. */
    public static void saveDrawing(
            int gameId,
            int playerId,
            String drawingData
    ) {

        if (gameId <= 0 || playerId <= 0) {
            return;                                   // no database session for this game
        }

        DbWriter.submit(connection -> {

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    "INSERT INTO drawings " +
                                            "(game_id, player_id, drawing_data) " +
                                            "VALUES (?, ?, ?)"
                            )
            ) {

                statement.setInt(1, gameId);
                statement.setInt(2, playerId);
                statement.setString(3, drawingData);
                statement.executeUpdate();
            }
        });
    }


    public static List<String> loadDrawings(
            int gameId
    ) {

        List<String> drawings =
                new ArrayList<>();

        String sql =
                "SELECT drawing_data " +
                        "FROM drawings " +
                        "WHERE game_id = ? " +
                        "ORDER BY drawing_id";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return drawings;
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setInt(
                        1,
                        gameId
                );

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    while (resultSet.next()) {

                        drawings.add(
                                resultSet.getString(
                                        "drawing_data"
                                )
                        );
                    }
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return drawings;
    }
}