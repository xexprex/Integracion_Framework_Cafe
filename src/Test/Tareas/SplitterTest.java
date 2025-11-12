package Test.Tareas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// Importamos tus clases reales
import Principal.Diccionario;
import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Principal.ValoresDiccionario;
import Tarea.Transformer.Splitter;

class SplitterTest {

    private Splitter splitter;
    private Slot slotEntrada;
    private Slot slotSalida;
    private DocumentBuilder docBuilder;

    @BeforeEach
    void setUp() throws Exception {
        slotEntrada = new Slot();
        slotSalida = new Slot();
        
        // El Splitter usa la primera entrada y primera salida de las listas
        splitter = new Splitter((Slot) List.of(slotEntrada), (Slot) List.of(slotSalida));
        
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    // --- Helpers para crear XML ---
    
    private Document createDocument(String xml) throws Exception {
        InputSource is = new InputSource(new StringReader(xml));
        return docBuilder.parse(is);
    }
    
    private Mensaje createMensaje(String xml) throws Exception {
        Mensaje msg = new Mensaje();
        msg.setHead(new Head()); // Un head por defecto
        msg.setBody(createDocument(xml));
        return msg;
    }
    
    // --- Helper para convertir Document XML a String (para asserts) ---
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // Omitimos la declaración <?xml ...?> para facilitar la comparación
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Prueba el "Happy Path": Un XML con 3 items se divide en 3 mensajes.
     */
    @Test
    void testExecute_CuandoMensajeCompleto_DivideEnFragmentos() throws Exception {
        // --- Arrange ---
        // 1. Configurar el XPath que buscará los items
        splitter.setXPathExpression("/Pedido/item");
        
        // 2. Crear el mensaje de entrada
        String xmlOriginal = "<Pedido>" +
                                "<item><id>A</id></item>" +
                                "<item><id>B</id></item>" +
                                "<item><id>C</id></item>" +
                             "</Pedido>";
        Mensaje msgEntrada = createMensaje(xmlOriginal);
        slotEntrada.enqueue(msgEntrada);
        
        // --- Act ---
        splitter.execute();
        
        // --- Assert (Salidas) ---
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertEquals(3, slotSalida.getQueueSize(), "La salida debe tener 3 mensajes");
        
        // 3. Revisar los 3 mensajes fragmentados
        List<Mensaje> mensajesSalida = slotSalida.getQueue();
        Mensaje msgOut1 = mensajesSalida.get(0);
        Mensaje msgOut2 = mensajesSalida.get(1);
        Mensaje msgOut3 = mensajesSalida.get(2);
        
        // 4. Verificar el contenido XML
        assertEquals("<item><id>A</id></item>", documentToString(msgOut1.getBody()));
        assertEquals("<item><id>B</id></item>", documentToString(msgOut2.getBody()));
        assertEquals("<item><id>C</id></item>", documentToString(msgOut3.getBody()));
        
        // 5. Verificar cabeceras de secuencia (Head)
        String idSecuencia = msgOut1.getHead().getIdSecuencia();
        assertNotNull(idSecuencia, "El IdSecuencia (UUID) no debe ser nulo");
        
        assertEquals(idSecuencia, msgOut2.getHead().getIdSecuencia(), "El IdSecuencia debe ser el mismo para todos");
        assertEquals(idSecuencia, msgOut3.getHead().getIdSecuencia(), "El IdSecuencia debe ser el mismo para todos");
        
        assertEquals(1, msgOut1.getHead().getNumSecuencia(), "NumSecuencia 1");
        assertEquals(3, msgOut1.getHead().getTotalSecuencia(), "TotalSecuencia 3");
        
        assertEquals(2, msgOut2.getHead().getNumSecuencia(), "NumSecuencia 2");
        assertEquals(3, msgOut2.getHead().getTotalSecuencia(), "TotalSecuencia 3");
        
        assertEquals(3, msgOut3.getHead().getNumSecuencia(), "NumSecuencia 3");
        assertEquals(3, msgOut3.getHead().getTotalSecuencia(), "TotalSecuencia 3");

        // 6. Verificar el Diccionario (el "esqueleto" guardado)
        ValoresDiccionario vd = Diccionario.getInstance().get(idSecuencia);
        assertNotNull(vd, "No se guardó el esqueleto en el Diccionario");
        
        // 7. Verificar el XPath padre guardado
        assertEquals("//Pedido", vd.getxPathExpression());
        
        // 8. Verificar que el XML guardado es el esqueleto (sin los 'item')
        Document docEsqueleto = (Document) vd.getContext();
        assertEquals("<Pedido/>", documentToString(docEsqueleto)); // O "<Pedido></Pedido>"
    }

    /**
     * Prueba que si la cola de entrada está vacía, no hace nada.
     */
    @Test
    void testExecute_CuandoEntradaVacia_NoHaceNada() {
        // Arrange
        splitter.setXPathExpression("/Pedido/item");
        // (Entrada vacía por defecto)
        
        // Act
        splitter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue());
        assertTrue(slotSalida.isEmptyQueue());
    }
    
    /**
     * Prueba que si el XPath no encuentra nodos, consume el mensaje
     * pero no produce salidas.
     */
    @Test
    void testExecute_CuandoXPathNoEncuentraNodos_ConsumeMensaje() throws Exception {
        // Arrange
        splitter.setXPathExpression("/Pedido/producto"); // XPath que no encontrará nada
        Mensaje msgEntrada = createMensaje("<Pedido><item>A</item></Pedido>");
        slotEntrada.enqueue(msgEntrada);
        
        // Act
        splitter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (no se encontraron nodos)");
    }
    
    /**
     * Prueba que si el XPath es inválido, consume el mensaje
     * y captura la excepción (no rompe).
     */
    @Test
    void testExecute_CuandoXPathEsInvalido_ConsumeMensajeYNoLanzaExcepcion() throws Exception {
        // Arrange
        splitter.setXPathExpression("///---invalid["); // XPath inválido
        Mensaje msgEntrada = createMensaje("<Pedido><item>A</item></Pedido>");
        slotEntrada.enqueue(msgEntrada);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            splitter.execute();
        }, "El método execute() debe capturar la excepción XPath");
        
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (error en procesamiento)");
    }
}