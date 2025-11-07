package Test.Tareas;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.modifier.ContextEnricher;

class ContextEnricherTest {

    // Clases a probar y sus dependencias
    private ContextEnricher contextEnricher;
    private Slot slotEntradaPrincipal;
    private Slot slotEntradaContexto;
    private Slot slotSalida;

    // Documentos XML de prueba
    private Document docPrincipal;
    private Document docContexto;

    /**
     * Este método se ejecuta ANTES de CADA test (@Test).
     * Prepara un entorno limpio para cada prueba.
     */
    @BeforeEach
    void setUp() throws Exception {
        // 1. Crear los Slots (colas)
        slotEntradaPrincipal = new Slot();
        slotEntradaContexto = new Slot();
        slotSalida = new Slot();

        // 2. Crear las listas de entradas y salidas
        List<Slot> entradas = Arrays.asList(slotEntradaPrincipal, slotEntradaContexto);
        List<Slot> salidas = Arrays.asList(slotSalida);

        // 3. Instanciar el ContextEnricher
        contextEnricher = new ContextEnricher(entradas, salidas);

        // 4. Crear los mensajes y documentos XML base
        docPrincipal = createDocumentFromString("<root><destino></destino></root>");
        docContexto = createDocumentFromString("<data><info>ValorAEnriquecer</info></data>");
    }

    /**
     * Helper para crear un Document XML a partir de un String.
     * Facilita mucho la creación de datos de prueba.
     */
    private Document createDocumentFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    /**
     * Prueba el caso exitoso ("Happy Path").
     * El mensaje principal debe ser enriquecido con el nodo del contexto
     * y enviado a la cola de salida.
     */
    @Test
    void testExecute_CasoExitoso_EnriqueceMensajePrincipal() throws Exception {
        // Arrange (Preparar)
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);

        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        
        slotEntradaPrincipal.enqueue(msgPrincipal);
        slotEntradaContexto.enqueue(msgContexto);

        contextEnricher.setXPathPrincipal("/root/destino");
        contextEnricher.setXPathContexto("/data/info");

        // Act (Actuar)
        contextEnricher.execute();

        // Assert (Verificar)
        
        // 1. Las colas de entrada deben estar vacías (mensajes consumidos)
        assertTrue(slotEntradaPrincipal.isEmptyQueue(), "La cola principal debería estar vacía");
        assertTrue(slotEntradaContexto.isEmptyQueue(), "La cola de contexto debería estar vacía");
        
        // 2. La cola de salida debe tener 1 mensaje
        assertEquals(1, slotSalida.getQueueSize(), "La cola de salida debería tener un mensaje");
        
        // 3. El mensaje en la salida debe ser el principal (la misma instancia)
        Mensaje msgSalida = slotSalida.dequeuePoll();
        assertSame(msgPrincipal, msgSalida, "El mensaje de salida debe ser el mismo que el principal");

        // 4. VERIFICACIÓN CLAVE: El contenido XML debe haber cambiado
        Document docEnriquecido = msgSalida.getBody();
        XPath xPath = XPathFactory.newInstance().newXPath();
        
        // Buscamos el nodo que debió ser insertado
        Node nodoInsertado = (Node) xPath.evaluate("/root/destino/info", docEnriquecido, XPathConstants.NODE);
        
        assertNotNull(nodoInsertado, "El nodo <info> debería existir en el documento principal");
        assertEquals("ValorAEnriquecer", nodoInsertado.getTextContent(), "El texto del nodo insertado no es correcto");
    }

    @Test
    void testExecute_CuandoEntradaPrincipalVacia_NoHaceNada() {
        // Arrange
        // (La cola principal está vacía por defecto)
        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        slotEntradaContexto.enqueue(msgContexto); // Ponemos un mensaje en la cola de contexto

        contextEnricher.setXPathPrincipal("/root/destino");
        contextEnricher.setXPathContexto("/data/info");

        // Act
        contextEnricher.execute();

        // Assert
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía si la entrada principal lo está");
        assertFalse(slotEntradaContexto.isEmptyQueue(), "No debe consumir el mensaje de contexto si el principal falta");
    }

    @Test
    void testExecute_CuandoEntradaContextoVacia_NoHaceNada() {
        // Arrange
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);
        slotEntradaPrincipal.enqueue(msgPrincipal); // Ponemos un mensaje en la cola principal
        // (La cola de contexto está vacía por defecto)

        contextEnricher.setXPathPrincipal("/root/destino");
        contextEnricher.setXPathContexto("/data/info");

        // Act
        contextEnricher.execute();

        // Assert
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía si la entrada de contexto lo está");
        assertFalse(slotEntradaPrincipal.isEmptyQueue(), "No debe consumir el mensaje principal si el de contexto falta");
    }

    @Test
    void testExecute_CuandoXPathPrincipalEsNulo_NoHaceNada() {
        // Arrange
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);
        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        
        slotEntradaPrincipal.enqueue(msgPrincipal);
        slotEntradaContexto.enqueue(msgContexto);

        // contextEnricher.setXPathPrincipal(null); // (Ya es null por defecto)
        contextEnricher.setXPathContexto("/data/info");

        // Act
        contextEnricher.execute();

        // Assert
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía si el XPath principal es nulo");
        // Verifica que los mensajes NO fueron consumidos
        assertEquals(1, slotEntradaPrincipal.getQueueSize());
        assertEquals(1, slotEntradaContexto.getQueueSize());
    }

    @Test
    void testExecute_CuandoXPathContextoEsNulo_NoHaceNada() {
        // Arrange
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);
        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        
        slotEntradaPrincipal.enqueue(msgPrincipal);
        slotEntradaContexto.enqueue(msgContexto);

        contextEnricher.setXPathPrincipal("/root/destino");
        // contextEnricher.setXPathContexto(null); // (Ya es null por defecto)

        // Act
        contextEnricher.execute();

        // Assert
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía si el XPath de contexto es nulo");
        // Verifica que los mensajes NO fueron consumidos
        assertEquals(1, slotEntradaPrincipal.getQueueSize());
        assertEquals(1, slotEntradaContexto.getQueueSize());
    }

    @Test
    void testExecute_CuandoXPathEsInvalido_ManejaExcepcion() {
        // Arrange
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);
        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        
        slotEntradaPrincipal.enqueue(msgPrincipal);
        slotEntradaContexto.enqueue(msgContexto);

        // XPath sintácticamente incorrecto
        contextEnricher.setXPathPrincipal("///---invalid-xpath[");
        contextEnricher.setXPathContexto("/data/info");

        // Act
        // El método execute() debe capturar la XPathExpressionException internamente
        // y no relanzarla.
        assertDoesNotThrow(() -> {
            contextEnricher.execute();
        });

        // Assert
        // Los mensajes fueron consumidos, pero fallaron en el procesamiento,
        // por lo que la salida debe estar vacía.
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía si el XPath es inválido");
        assertTrue(slotEntradaPrincipal.isEmptyQueue(), "El mensaje principal debió ser consumido (aunque fallara)");
        assertTrue(slotEntradaContexto.isEmptyQueue(), "El mensaje de contexto debió ser consumido (aunque fallara)");
    }
    
    @Test
    void testExecute_CuandoNodoPrincipalNoEncontrado_PasaMensajeSinEnriquecer() {
        // Arrange
        Mensaje msgPrincipal = new Mensaje();
        msgPrincipal.setBody(docPrincipal);
        Mensaje msgContexto = new Mensaje();
        msgContexto.setBody(docContexto);
        
        slotEntradaPrincipal.enqueue(msgPrincipal);
        slotEntradaContexto.enqueue(msgContexto);

        // XPath que no encontrará nada en el doc principal
        contextEnricher.setXPathPrincipal("/root/destino_inexistente"); 
        contextEnricher.setXPathContexto("/data/info");

        // Act
        contextEnricher.execute();

        // Assert
        // El mensaje debe pasar a la salida, pero sin modificar.
        assertEquals(1, slotSalida.getQueueSize(), "El mensaje debe pasar a la salida");
        Mensaje msgSalida = slotSalida.dequeuePoll();
        assertSame(msgPrincipal, msgSalida);
        
        // Verificamos que el documento NO fue modificado
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node nodoInsertado = (Node) xPath.evaluate("/root/destino/info", msgSalida.getBody(), XPathConstants.NODE);
            assertNull(nodoInsertado, "El documento no debió ser enriquecido");
        } catch (Exception e) {
            fail("La verificación de XPath falló", e);
        }
    }
}
