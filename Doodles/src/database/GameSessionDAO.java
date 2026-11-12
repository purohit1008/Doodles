package database;

import java.sql.*;

public class GameSessionDAO {

    public static int createGame() {

        String sql =
                "INSERT INTO game_sessions (status) " +
                        "VALUES ('ACTIVE')";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return -1;
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql,
                                    Statement.RETURN_GENERATED_KEYS
                            )
            ) {

                statement.executeUpdate();

                try (
                        ResultSet resultSet =
                                statement.getGeneratedKeys()
                ) {

                    if (resultSet.next()) {

                        int gameId =
                                resultSet.getInt(1);

                        System.out.println(
                                "Game created successfully! ID: "
                                        + gameId
                        );

                        return gameId;
                    }
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error creating game."
            );

            e.printStackTrace();
        }

        return -1;
    }


    public static String getGameStatus(
            int gameId
    ) {

        String sql =
                "SELECT status " +
                        "FROM game_sessions " +
                        "WHERE game_id = ?";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return null;
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

                    if (resultSet.next()) {

                        return resultSet.getString(
                                "status"
                        );
                    }
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return null;
    }


    public static void endGame(
            int gameId
    ) {

        if (gameId <= 0) {
            return;                                   // this game was never recorded
        }

        String sql =
                "UPDATE game_sessions " +
                        "SET status = 'ENDED' " +
                        "WHERE game_id = ?";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return;
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setInt(
                        1,
                        gameId
                );

                statement.executeUpdate();

                System.out.println(
                        "Game " +
                                gameId +
                                " ended successfully!"
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }
}