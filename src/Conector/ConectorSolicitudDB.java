package Conector;

import Puerto.Puerto;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConectorSolicitudDB extends Conector {

    
    private Connection con;

    public ConectorSolicitudDB(Puerto puerto, String urlDB) {
        super(puerto);

        try {
            con = DriverManager.getConnection(urlDB);
            System.out.println("Conexión establecida con la base de datos.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void execute() {
        
    }
}