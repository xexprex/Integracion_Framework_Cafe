package Test.Tareas;

import java.io.StringReader;

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
import Tarea.Router.Filter;

class FilterTest {

    private Filter filter;
    private Slot slotEntrada;
    private Slot slotSalida;

    /**
     * Se ejecuta ANTES de cada test.
     * Configura un escenario limpio con 1 entrada y 1 salida.
     */
    @BeforeEach
    void setUp() {
        slotEntrada = new Slot();
        slotSalida = new Slot();
        
        // Creamos el Filter
        filter = new Filter(slotEntrada, slotSalida);
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
        return msg;
    }

    /**
     * Prueba el "Happy Path": El XPath encuentra un nodo con contenido,
     * por lo que el mensaje debe pasar a la salida.
     */
    @Test
    void testExecute_CuandoMensajeCumpleFiltro_PasaALaSalida() throws Exception {
        // Arrange
        filter.setXpath("/pedido/cliente"); // Buscamos el nodo /pedido/cliente
        Mensaje msg = crearMensaje("<pedido><cliente>Cliente123</cliente></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act
        filter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debe tener 1 mensaje");
        assertSame(msg, slotSalida.dequeuePoll(), "El mensaje en la salida es el incorrecto");
    }

    /**
     * Prueba el descarte: El XPath busca un nodo que no existe.
     * El mensaje debe ser consumido (descartado).
     */
    @Test
    void testExecute_CuandoXPathNoEncuentraNodo_MensajeEsFiltrado() throws Exception {
        // Arrange
        filter.setXpath("/pedido/producto"); // Buscamos un nodo que no existe
        Mensaje msg = crearMensaje("<pedido><cliente>Cliente123</cliente></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act
        filter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (mensaje filtrado)");
    }
    
    /**
     * Prueba el descarte: El XPath encuentra un nodo, pero su contenido
     * está vacío (ej. <cliente></cliente>).
     * El mensaje debe ser consumido (descartado).
     */
    @Test
    void testExecute_CuandoNodoEncontradoEstaVacio_MensajeEsFiltrado() throws Exception {
        // Arrange
        filter.setXpath("/pedido/cliente"); // Buscamos el nodo cliente
        Mensaje msg = crearMensaje("<pedido><cliente></cliente></pedido>"); // Nodo vacío
        slotEntrada.enqueue(msg);
        
        // Act
        filter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (mensaje filtrado)");
    }
    
    /**
     * Prueba el descarte: El XPath encuentra un nodo, pero es auto-cerrado
     * (ej. <cliente/>), lo que también cuenta como contenido vacío.
     */
    @Test
    void testExecute_CuandoNodoEncontradoEsAutoCerrado_MensajeEsFiltrado() throws Exception {
        // Arrange
        filter.setXpath("/pedido/cliente"); // Buscamos el nodo cliente
        Mensaje msg = crearMensaje("<pedido><cliente/></pedido>"); // Nodo auto-cerrado
        slotEntrada.enqueue(msg);
        
        // Act
        filter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía (mensaje filtrado)");
    }

    /**
     * Prueba que si la cola de entrada está vacía, no ocurre nada.
     */
    @Test
    void testExecute_CuandoEntradaEstaVacia_NoHaceNada() {
        // Arrange
        filter.setXpath("/pedido/cliente");
        // No se añaden mensajes a slotEntrada
        
        // Act
        filter.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada sigue vacía");
        assertTrue(slotSalida.isEmptyQueue(), "La salida sigue vacía");
    }

    /**
     * Prueba que un XPath sintácticamente inválido lanza RuntimeException.
     */
    @Test
    void testExecute_CuandoXPathEsInvalido_LanzaRuntimeException() throws Exception {
        // Arrange
        filter.setXpath("///---invalid-xpath["); // XPath sintácticamente incorrecto
        Mensaje msg = crearMensaje("<pedido><cliente>Cliente123</cliente></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            filter.execute();
        });
        
        // El mensaje fue consumido (dequeuePoll) antes del 'try'
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía");
    }

    /**
     * Prueba que si el XPath es 'null', lanza RuntimeException.
     */
    @Test
    void testExecute_CuandoXPathEsNulo_LanzaRuntimeException() throws Exception {
        // Arrange
        filter.setXpath(null); // XPath es null
        Mensaje msg = crearMensaje("<pedido><cliente>Cliente123</cliente></pedido>");
        slotEntrada.enqueue(msg);
        
        // Act & Assert
        // Fallará en xPath.compile(xpath)
        assertThrows(RuntimeException.class, () -> {
            filter.execute();
        });
        
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía (mensaje consumido)");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe estar vacía");
    }
}
