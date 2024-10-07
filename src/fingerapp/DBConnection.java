/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fingerapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/absensi_kece"; // Ganti dengan URL database Anda
    private static final String USER = "root"; // Ganti dengan username MySQL Anda
    private static final String PASS = ""; // Ganti dengan password MySQL Anda

    private static Connection connection = null;

    // Method untuk mendapatkan koneksi
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Buat koneksi ke database
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

}