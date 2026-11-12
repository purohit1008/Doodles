package database;

import java.sql.Connection;

public class DatabaseManager {

    public static void main(
            String[] args
    ) {

        System.out.println(
                "================================="
        );

        System.out.println(
                "      DOODLES DATABASE MANAGER"
        );

        System.out.println(
                "================================="
        );

        try (
                Connection connection =
                        DataBaseConnection.connect()
        ) {

            if (connection == null) {

                System.out.println(
                        "Database connection failed."
                );

                return;
            }

            System.out.println(
                    "Database connection successful!"
            );

            System.out.println(
                    "Database ready for Doodles."
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}