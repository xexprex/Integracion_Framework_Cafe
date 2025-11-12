package Test.Tareas;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.Router.Distributor;

class DistributorTest {

    private Distributor distributor;
    private Slot slotEntrada;
    private Slot slotSalidaA;
    private Slot slotSalidaB;
    private Slot slotSalidaC;

    /**
     * Se ejecuta ANTES de cada test.
     * Configura un escenario limpio con 1 entrada y 3 salidas.
     */
    @BeforeEach
    void setUp() {
        slotEntrada = new Slot();
        slotSalidaA = new Slot();
        slotSalidaB = new Slot();
        slotSalidaC = new Slot();

        List<Slot> salidas = Arrays.asList(slotSalidaA, slotSalidaB, slotSalidaC);
        
        // Creamos el Distributor
        distributor = new Distributor(slotEntrada, salidas);

        // --- Configuración base del Distributor ---
        
        // 1. Definimos el XPath que leerá el tipo
        distributor.setXpath("/pedido/tipo");
        
        // 2. Definimos el orden de las salidas
        // salida 0 (slotSalidaA) recibirá mensajes "TIPO_A"
        // salida 1 (slotSalidaB) recibirá mensajes "TIPO_B"
        // salida 2 (slotSalidaC) recibirá mensajes "TIPO_C"
        distributor.setElementosSegunOrden(Arrays.asList("TIPO_A", "TIPO_B", "TIPO_C"));
    }

    /**
     * Helper para crear un Document XML a partir de un String.
     */
    private Document createDocumentFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }
    
    /**
     * Helper para crear un Mensaje listo para el test.
     */
    private Mensaje crearMensaje(String xml) throws Exception {
        Mensaje msg = new Mensaje();
        msg.setBody(createDocumentFromString(xml));
        // Head y otros atributos no son necesarios para este test
        return msg;
    }

    @Test
    void testExecute_CuandoMensajeEsTipoA_RutaASalidaA() throws Exception {
        // Arrange
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_A</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act
        distributor.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía");
        assertEquals(1, slotSalidaA.getQueueSize(), "La salida A debe tener 1 mensaje");
        assertEquals(0, slotSalidaB.getQueueSize(), "La salida B debe estar vacía");
        assertEquals(0, slotSalidaC.getQueueSize(), "La salida C debe estar vacía");
        assertSame(msg, slotSalidaA.dequeuePoll(), "El mensaje en Salida A es el incorrecto");
    }
    
    @Test
    void testExecute_CuandoMensajeEsTipoB_RutaASalidaB() throws Exception {
        // Arrange
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_B</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act
        distributor.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía");
        assertEquals(0, slotSalidaA.getQueueSize(), "La salida A debe estar vacía");
        assertEquals(1, slotSalidaB.getQueueSize(), "La salida B debe tener 1 mensaje");
        assertEquals(0, slotSalidaC.getQueueSize(), "La salida C debe estar vacía");
        assertSame(msg, slotSalidaB.dequeuePoll(), "El mensaje en Salida B es el incorrecto");
    }

    @Test
    void testExecute_CuandoMensajeNoCoincide_EsDescartado() throws Exception {
        // Arrange
        // "TIPO_D" no está en la lista de elementosSegunOrden
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_D</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act
        distributor.execute();
        
        // Assert
        // El mensaje fue consumido de la entrada, pero no enrutado
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalidaA.isEmptyQueue(), "La salida A debe estar vacía");
        assertTrue(slotSalidaB.isEmptyQueue(), "La salida B debe estar vacía");
        assertTrue(slotSalidaC.isEmptyQueue(), "La salida C debe estar vacía");
    }

    @Test
    void testExecute_CuandoEntradaEstaVacia_NoHaceNada() {
        // Arrange
        // No se añaden mensajes a slotEntrada
        
        // Act
        distributor.execute();
        
        // Assert
        // Todas las colas deben seguir vacías
        assertTrue(slotEntrada.isEmptyQueue());
        assertTrue(slotSalidaA.isEmptyQueue());
        assertTrue(slotSalidaB.isEmptyQueue());
        assertTrue(slotSalidaC.isEmptyQueue());
    }

    @Test
    void testExecute_CuandoXPathEsInvalido_LanzaRuntimeException() throws Exception {
        // Arrange
        distributor.setXpath("///---invalid-xpath["); // XPath sintácticamente incorrecto
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_A</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        // Verificamos que se lanza la excepción que definiste (RuntimeException)
        assertThrows(RuntimeException.class, () -> {
            distributor.execute();
        });
        
        // Importante: El mensaje fue consumido (dequeuePoll) antes del 'try'
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        // Pero ninguna salida lo recibió
        assertTrue(slotSalidaA.isEmptyQueue(), "Las salidas deben estar vacías");
    }

    @Test
    void testExecute_CuandoXPathNoEncuentraNodo_LanzaRuntimeException() throws Exception {
        // Arrange
        // El XPath busca un nodo que no existe
        distributor.setXpath("/pedido/tipo_inexistente"); 
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_A</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        // Esto causará un NullPointerException en 'items.item(0).getTextContent()'
        // que es capturado y relanzado como RuntimeException
        assertThrows(RuntimeException.class, () -> {
            distributor.execute();
        });
        
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalidaA.isEmptyQueue(), "Las salidas deben estar vacías");
    }
    
    @Test
    void testExecute_CuandoXPathEsNulo_LanzaRuntimeException() throws Exception {
        // Arrange
        distributor.setXpath(null); 
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_A</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        // Esto causará un NullPointerException en 'xPath.compile(xpath)'
        assertThrows(RuntimeException.class, () -> {
            distributor.execute();
        });
        
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
    }
    
    @Test
    void testExecute_CuandoListaElementosEsNula_LanzaRuntimeException() throws Exception {
        // Arrange
        distributor.setElementosSegunOrden(null); // Lista de ruteo es nula
        Mensaje msg = crearMensaje("<pedido><tipo>TIPO_A</tipo></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        // Esto causará un NullPointerException en el bucle 'for'
        assertThrows(RuntimeException.class, () -> {
            distributor.execute();
        });
        
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
    }
}