package com.zhanbrenet.twelvemonthsv2app.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private static final String URL = "jdbc:postgresql://10.0.2.2:5432/twelvemonthsapp"; // adresse spéciale
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";

    public static List<String> getCities() {
        List<String> cities = new ArrayList<>();

        try {
            // Chargement du driver PostgreSQL
            Class.forName("org.postgresql.Driver");

            // Connexion à la base de données
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement statement = connection.createStatement();

            // Requête pour récupérer les villes
            String query = "SELECT name, country FROM cities";
            ResultSet resultSet = statement.executeQuery(query);

            // Lecture des résultats
            while (resultSet.next()) {
                String city = resultSet.getString("name") + ", " + resultSet.getString("country");
                cities.add(city);
            }

            // Fermeture des connexions
            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cities;
    }
}
