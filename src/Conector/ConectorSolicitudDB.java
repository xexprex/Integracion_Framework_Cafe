package Conector;

import Puerto.Puerto;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
        // Obtiene el documento XML de entrada desde el slot de entrada del puerto
        Document docEntrada = puerto.getDocumentBySlot();
        
        if (docEntrada == null) {
            // No hay nada que procesar
            return;
        }

        // Extrae la consulta SQL del XML (asumiendo <sql>...</sql>)
        String query = docEntrada.getElementsByTagName("sql").item(0).getTextContent();
        System.out.println("ConectorSQLAzure: Ejecutando query: " + query);

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Crea un nuevo documento XML para la respuesta
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDoc = builder.newDocument();

            // Construye el XML de respuesta a partir del ResultSet
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

            // Pone el documento de respuesta en el puerto
       
            puerto.setDocument(newDoc);

        } catch (Exception e) {
            System.out.println("ConectorSQLAzure: Error al ejecutar la consulta: " + e.getMessage());

        }
    }
}