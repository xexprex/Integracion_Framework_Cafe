package Conector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Puerto.Puerto;

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
        Document docEntrada = puerto.getDocumentBySlot();
        
        if (docEntrada == null) {
            return;
        }

        String query = docEntrada.getElementsByTagName("sql").item(0).getTextContent();
        System.out.println("Conector: Ejecutando query: " + query);

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDoc = builder.newDocument();

            Element raiz = newDoc.createElement("resultadoSQL");
            newDoc.appendChild(raiz);

            while (rs.next()) {
                Element fila = newDoc.createElement("fila");
                raiz.appendChild(fila);

                int numColumnas = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= numColumnas; i++) {
                    String nombreColumna = rs.getMetaData().getColumnName(i);
                    String valorColumna = rs.getString(i);

                    Element elementoCol = newDoc.createElement(nombreColumna);
                    elementoCol.setTextContent(valorColumna);
                    fila.appendChild(elementoCol);
                }
            }
            
            puerto.setDocument(newDoc);

        } catch (Exception e) {
            System.out.println("Conector: Error al ejecutar la consulta: " + e.getMessage());
        }
    }
}