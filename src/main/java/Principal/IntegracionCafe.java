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

//Metodo de pago
import Conector.ConectorStripePago;
import java.io.StringReader;
import org.xml.sax.InputSource;



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
        pruebaStripePago();
    }

    public static void pruebaBaseDeDatos() {

        System.out.println("\n--- Probando ConectorSQL (con Maven y AAD) ---");



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


    public static void pruebaStripePago() {
    System.out.println("\n--- Probando ConectorStripePago ---");

    // 1. Pega tu clave secreta de prueba aquí
    
    
    // 2. Configurar los Slots y el Puerto (igual que en tu prueba de DB)
    //
    Slot slotPeticionPago = new Slot();
    Slot slotRespuestaPago = new Slot();
    // Usamos PuertoSolicitante para el patrón Petición-Respuesta
    //
    PuertoSolicitante puertoPago = new PuertoSolicitante(slotPeticionPago, slotRespuestaPago);

    // 3. Crear el nuevo Conector
    ConectorStripePago conectorStripe = new ConectorStripePago(puertoPago, stripeApiKey);

    try {
        // 4. Crear el Mensaje de petición XML
        String xmlPeticion = "<peticionPago>" +
                             "<monto>2000</monto>" + // 2000 céntimos (ej: 20 EUR)
                             "<moneda>eur</moneda>" +
                             "<fuente>tok_visa</fuente>" + // Token de tarjeta de prueba de Stripe
                             "</peticionPago>";

        // Convertir String a Document (como en tu prueba de DB)
        //
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document docPeticion = builder.parse(new InputSource(new StringReader(xmlPeticion)));

        Head headPeticion = new Head();
        headPeticion.setIdUnico(Principal.IdUnico.getInstance().getIdUnico());
        Mensaje msgPeticion = new Mensaje(headPeticion, docPeticion);

        // 5. Enviar mensaje al flujo
        slotPeticionPago.enqueue(msgPeticion);
        System.out.println("Mensajes en cola PETICION PAGO: " + slotPeticionPago.getQueueSize());

        // 6. Ejecutar los componentes en orden
        conectorStripe.execute(); // Conector llama a la API externa
        puertoPago.execute();     // Puerto mueve la respuesta al slot de salida

        // 7. Verificar los resultados
        System.out.println("Mensajes en cola PETICION PAGO (debe ser 0): " + slotPeticionPago.getQueueSize());
        System.out.println("Mensajes en cola RESPUESTA PAGO (debe ser 1): " + slotRespuestaPago.getQueueSize());

        if (!slotRespuestaPago.isEmptyQueue()) {
            Mensaje msgRespuesta = slotRespuestaPago.dequeuePoll();
            Document docRespuesta = msgRespuesta.getBody();
            String nodoRaiz = docRespuesta.getDocumentElement().getTagName();
            String estado = docRespuesta.getElementsByTagName("estado").item(0).getTextContent();

            System.out.println("Respuesta de Stripe recibida. Nodo raíz: " + nodoRaiz);
            System.out.println("Estado del pago: " + estado); // Debería ser "succeeded"
        }

    } catch (Exception e) {
        System.out.println("Error en la prueba del ConectorStripe: " + e.getMessage());
        e.printStackTrace();
    }
}



}
