package Test.Tareas;


import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue; // Importante para el archivo temporal
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.Transformer.Translator;

class TranslatorTest {

    private Translator translator;
    private Slot slotEntrada;
    private Slot slotSalida;
    private DocumentBuilder docBuilder;

    /**
     * @TempDir crea una carpeta temporal ANTES de cada test
     * y la borra DESPUÉS. 'tempDir' contendrá la ruta a esa carpeta.
     */
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        slotEntrada = new Slot();
        slotSalida = new Slot();
        
        translator = new Translator(List.of(slotEntrada), List.of(slotSalida));
        
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    // --- Helpers ---
    
    private Document createDocument(String xml) throws Exception {
        InputSource is = new InputSource(new StringReader(xml));
        return docBuilder.parse(is);
    }
    
    private Mensaje createMensaje(String xml) throws Exception {
        Mensaje msg = new Mensaje();
        msg.setHead(new Head());
        msg.setBody(createDocument(xml));
        return msg;
    }
    
    // Convierte un Document XML a un String para poder compararlo
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        // Limpiamos espacios/saltos de línea para una comparación más robusta
        return writer.getBuffer().toString().replaceAll("[\\n\\r\\s]", "");
    }

    /**
     * Prueba el "Happy Path": Transforma un XML de entrada a uno
     * de salida usando un XSLT temporal que creamos al vuelo.
     */
    @Test
    void testExecute_TransformacionExitosa() throws Exception {
        // --- Arrange ---
        
        // 1. Definir el XSLT de prueba (transforma <persona> en <usuario>)
        String xsltContenido = 
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
            "  <xsl:template match=\"/persona\">" +
            "    <usuario>" +
            "      <nombre><xsl:value-of select=\"nombre\"/></nombre>" +
            "    </usuario>" +
            "  </xsl:template>" +
            "</xsl:stylesheet>";

        // 2. Crear el archivo XSLT temporal en la carpeta @TempDir
        Path xsltFile = tempDir.resolve("transform.xslt");
        Files.writeString(xsltFile, xsltContenido);
        
        // 3. Configurar el Translator para que use ese archivo
        translator.setRutaXSLT(xsltFile.toString());
        
        // 4. Crear el mensaje de entrada
        Mensaje msgEntrada = createMensaje("<persona><nombre>Ana</nombre></persona>");
        slotEntrada.enqueue(msgEntrada);
        
        // --- Act ---
        translator.execute();
        
        // --- Assert ---
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debe tener 1 mensaje");

        // 6. Verificar que es el mismo objeto Mensaje
        Mensaje msgSalida = slotSalida.dequeuePoll();
        assertSame(msgEntrada, msgSalida, "Debe ser la misma instancia del mensaje");

        // 7. VERIFICACIÓN CLAVE: El contenido del body debe haber cambiado
        String xmlEsperado = "<usuario><nombre>Ana</nombre></usuario>";
        String xmlObtenido = documentToString(msgSalida.getBody());
        
        assertEquals(xmlEsperado, xmlObtenido, "El XML no se transformó correctamente");
    }

    /**
     * Prueba que si la cola de entrada está vacía, no hace nada.
     */
    @Test
    void testExecute_CuandoEntradaVacia_NoHaceNada() {
        // Arrange
        translator.setRutaXSLT("dummy.xslt");
        // (Entrada vacía)
        
        // Act
        translator.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue());
        assertTrue(slotSalida.isEmptyQueue());
    }

    /**
     * Prueba que si la ruta XSLT es nula, se captura la excepción
     * y el mensaje no pasa a la salida.
     */
    @Test
    void testExecute_CuandoRutaXSLTEsNula_CapturaError() throws Exception {
        // Arrange
        translator.setRutaXSLT(null); // Causa un NullPointerException
        Mensaje msgEntrada = createMensaje("<persona>Test</persona>");
        slotEntrada.enqueue(msgEntrada);

        // Act
        // El execute() debe capturar la excepción y no relanzarla
        assertDoesNotThrow(() -> {
            translator.execute();
        });

        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "El mensaje debió ser consumido (dequeue)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (la transformación falló)");
    }
    
    /**
     * Prueba que si la ruta XSLT no existe, se captura la excepción
     * y el mensaje no pasa a la salida.
     */
    @Test
    void testExecute_CuandoRutaXSLTNoExiste_CapturaError() throws Exception {
        // Arrange
        translator.setRutaXSLT("ruta/inexistente/archivo.xslt"); // Causa un FileNotFoundException
        Mensaje msgEntrada = createMensaje("<persona>Test</persona>");
        slotEntrada.enqueue(msgEntrada);

        // Act
        assertDoesNotThrow(() -> {
            translator.execute();
        });

        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "El mensaje debió ser consumido (dequeue)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (la transformación falló)");
    }
}