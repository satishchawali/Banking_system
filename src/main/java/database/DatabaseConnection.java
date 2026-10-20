package database;
import java.sql.*;
public final class DatabaseConnection {
 private DatabaseConnection(){}
 public static Connection getConnection() throws SQLException {String url=System.getenv().getOrDefault("DB_URL","jdbc:mysql://localhost:3306/banking_db?serverTimezone=Asia/Kolkata");return DriverManager.getConnection(url,System.getenv().getOrDefault("DB_USER","root"),System.getenv().getOrDefault("DB_PASSWORD",""));}
}
