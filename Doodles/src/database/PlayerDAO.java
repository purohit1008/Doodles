package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDAO {

    // =========================================================
    // GET OR CREATE PLAYER
    // =========================================================

    public static int getOrCreatePlayer(String playerName) {

        // First check if this name already exists
        String findSql =
                "SELECT player_id FROM players WHERE player_name = ?";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return -1;
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(findSql)
            ) {

                statement.setString(1, playerName);

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    if (resultSet.next()) {

                        int existingId =
                                resultSet.getInt("player_id");

                        System.out.println(
                                "Existing player found: "
                                        + playerName
                                        + " | ID: "
                                        + existingId
                        );

                        return existingId;
                    }
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            return -1;
        }


        // Player does not exist, so create one
        return addPlayer(playerName);
    }


    // =========================================================
    // ADD PLAYER
    // =========================================================

    public static int addPlayer(String playerName) {

        String sql =
                "INSERT INTO players (player_name) VALUES (?)";

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

                statement.setString(
                        1,
                        playerName
                );

                statement.executeUpdate();

                try (
                        ResultSet resultSet =
                                statement.getGeneratedKeys()
                ) {

                    if (resultSet.next()) {

                        int playerId =
                                resultSet.getInt(1);

                        System.out.println(
                                "Player added successfully! "
                                        + playerName
                                        + " | ID: "
                                        + playerId
                        );

                        return playerId;
                    }
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error adding player: "
                            + playerName
            );

            e.printStackTrace();
        }

        return -1;
    }


    // =========================================================
    // GET PLAYER NAME
    // =========================================================

    public static String getPlayerName(int playerId) {

        String sql =
                "SELECT player_name " +
                        "FROM players " +
                        "WHERE player_id = ?";

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
                        playerId
                );

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    if (resultSet.next()) {

                        return resultSet.getString(
                                "player_name"
                        );
                    }
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return null;
    }


    // =========================================================
    // GET ALL PLAYERS
    // =========================================================

    public static List<String> getAllPlayers() {

        List<String> players =
                new ArrayList<>();

        String sql =
                "SELECT player_id, player_name " +
                        "FROM players " +
                        "ORDER BY player_id";

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {
                return players;
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    int id =
                            resultSet.getInt(
                                    "player_id"
                            );

                    String name =
                            resultSet.getString(
                                    "player_name"
                            );

                    players.add(
                            id + " - " + name
                    );
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return players;
    }
}