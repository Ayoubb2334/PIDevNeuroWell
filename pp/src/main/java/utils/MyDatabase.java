package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    // ⚠️ Ajout des options Unicode + timezone pour MariaDB / MySQL
    private final String URL =
            "jdbc:mysql://localhost:3306/projet1?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";

    private final String USER = "root";
    private final String PASSWORD = "";

    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base 'projet1' établie avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur de connexion à la base de données : " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
