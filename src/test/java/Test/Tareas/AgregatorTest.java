package Test.Tareas;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach; // Asegurándonos que usamos el Document correcto
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import Principal.Diccionario;
import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Principal.ValoresDiccionario;
import Tarea.Transformer.Agregator;

class AgregatorTest {

    private Agregator agregator;
    private Slot slotEntrada;
    private Slot slotSalida;

    private DocumentBuilder docBuilder;

    @BeforeEach
    void setUp() throws Exception {
        slotEntrada = new Slot();
        slotSalida = new Slot();
        agregator = new Agregator(List.of(slotEntrada), List.of(slotSalida));
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        // Limpiamos el Diccionario Singleton entre pruebas
        // (Esto es importante si el Diccionario guarda estado)
        // Nota: Si tu 'Diccionario' no tiene un método 'clear', 
        // esta prueba podría fallar si se ejecuta junto con otras.
        // Diccionario.getInstance().clear(); 
    }

    // --- Helpers para crear datos de prueba ---
    
    private Document createDocument(String xml) throws Exception {
        InputSource is = new InputSource(new StringReader(xml));
        return docBuilder.parse(is);
    }

    private Mensaje createMensaje(String idSec, int numSec, int totalSec, String xmlBody) throws Exception {
        Head head = new Head();
        head.setIdSecuencia(idSec);
        head.setNumSecuencia(numSec);
        head.setTotalSecuencia(totalSec);
        head.setIdUnico(System.nanoTime()); 

        Mensaje msg = new Mensaje();
        msg.setHead(head);
        msg.setBody(createDocument(xmlBody));
        return msg;
    }

    @Test
    void testExecute_CuandoSecuenciaEstaCompleta_AgregaYEnvia() throws Exception {
        // --- Arrange ---
        String idSecuencia = "seq-abc";
        
        Mensaje msg1 = createMensaje(idSecuencia, 1, 2, "<item>FragmentoA</item>");
        Mensaje msg2 = createMensaje(idSecuencia, 2, 2, "<item>FragmentoB</item>");
        
        slotEntrada.enqueue(msg1);
        slotEntrada.enqueue(msg2);

        Document contextDoc = createDocument("<Pedido></Pedido>");
        String xpathAgregacion = "/Pedido";
        
        // --- ACTUALIZACIÓN CLAVE ---
        // Usamos el orden correcto del constructor: (String, Document)
        ValoresDiccionario vd = new ValoresDiccionario(xpathAgregacion, contextDoc);

        // "Cebar" el Singleton Diccionario
        Diccionario.getInstance().put(idSecuencia, vd);

        // --- Act ---
        agregator.execute();

        // --- Assert ---
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debería estar vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debería tener 1 mensaje");

        Mensaje msgSalida = slotSalida.dequeuePoll();
        assertSame(msg1, msgSalida, "El mensaje de salida debe ser la instancia del primer fragmento");
        assertEquals(0, msgSalida.getHead().getNumSecuencia(), "El NumSecuencia debe resetearse a 0");

        // Verificar el contenido XML agregado
        Document docAgregado = msgSalida.getBody();
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList items = (NodeList) xpath.evaluate("/Pedido/item", docAgregado, XPathConstants.NODESET);
        
        assertEquals(2, items.getLength(), "Debería haber 2 nodos <item> agregados");
        assertEquals("FragmentoA", items.item(0).getTextContent());
        assertEquals("FragmentoB", items.item(1).getTextContent());
    }

    @Test
    void testExecute_CuandoSecuenciaEstaIncompleta_NoHaceNada() throws Exception {
        // --- Arrange ---
        String idSecuencia = "seq-xyz";
        
        // Solo llega 1 de 2 mensajes
        Mensaje msg1 = createMensaje(idSecuencia, 1, 2, "<item>FragmentoA</item>");
        slotEntrada.enqueue(msg1);
        
        // (Utilidad encontrará 1, pero el Head dice total 2. El 'if' fallará)
        
        // --- Act ---
        agregator.execute();

        // --- Assert ---
        assertEquals(1, slotEntrada.getQueueSize(), "La entrada NO debe vaciarse");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía");
    }

    @Test
    void testExecute_CuandoEntradaVacia_NoHaceNada() {
        // --- Arrange --- (Entrada vacía)

        // --- Act ---
        agregator.execute();

        // --- Assert ---
        assertTrue(slotEntrada.isEmptyQueue());
        assertTrue(slotSalida.isEmptyQueue());
    }
}
