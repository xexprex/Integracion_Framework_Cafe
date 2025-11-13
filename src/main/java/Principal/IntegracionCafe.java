package Principal;
//package main.java;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import Conector.ConectorSolicitudDB;
import Puerto.PuertoSolicitante;

public class IntegracionCafe {

    /**
     * Lee un archivo XML desde una ruta y lo convierte en un Document.
     */
    private static Document parseXmlFile(String filePath) throws Exception {
        File xmlFile = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    public static void main(String[] args) {
        pruebaBaseDeDatos();
    }

    public static void pruebaBaseDeDatos() {

        System.out.println("\n--- Probando ConectorSQL (con Maven y AAD) ---");

        /*
         * String connectionUrl =
         * "jdbc:sqlserver://integracion.database.windows.net:1433;" +
         * "database=Practica_Integracion;" +
         * "encrypt=true;" +
         * "trustServerCertificate=true;" +
         * "authentication=ActiveDirectoryInteractive;";
         */

        String connectionUrl = "jdbc:sqlserver://integracion.database.windows.net:1433;" + // TODO
                "database=Practica_Integracion;" +
                "user=PruebaUsuario;" +
                "password=,PruebaContraseña;" +
                "encrypt=true;" +
                "trustServerCertificate=false;" +
                "hostNameInCertificate=*.database.windows.net;";

        Slot slotPeticion = new Slot();
        Slot slotRespuesta = new Slot();
        PuertoSolicitante puertoSQL = new PuertoSolicitante(slotPeticion, slotRespuesta);

        ConectorSolicitudDB conectorDB = new ConectorSolicitudDB(puertoSQL, connectionUrl);

        try {
            String xmlPeticion = "<peticion><sql>SELECT * FROM bebidas</sql></peticion>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document docPeticion = builder.parse(new InputSource(new StringReader(xmlPeticion)));

            Head headPeticion = new Head();
            headPeticion.setIdUnico(Principal.IdUnico.getInstance().getIdUnico());
            Mensaje msgPeticion = new Mensaje(headPeticion, docPeticion);

            slotPeticion.enqueue(msgPeticion);
            System.out.println("Mensajes en la cola de PETICIÓN: " + slotPeticion.getQueueSize());

            conectorDB.execute();
            puertoSQL.execute();

            System.out.println("Mensajes en la cola de PETICIÓN (debe ser 0): " + slotPeticion.getQueueSize());
            System.out.println("Mensajes en la cola de RESPUESTA (debe ser 1): " + slotRespuesta.getQueueSize());

            if (!slotRespuesta.isEmptyQueue()) {
                Mensaje msgRespuesta = slotRespuesta.dequeuePoll();
                Document docRespuesta = msgRespuesta.getBody();
                String nodoRaiz = docRespuesta.getDocumentElement().getTagName();
                System.out.println(
                        "Respuesta recibida. Nodo raíz: " + nodoRaiz + " " + msgRespuesta.getHead().toString());
            }

        } catch (Exception e) {
            System.out.println("Error en la prueba del ConectorSQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
