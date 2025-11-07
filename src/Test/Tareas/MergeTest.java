package Test.Tareas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.Router.Merge;

class MergeTest {

    private Merge merge;
    private Slot slotEntrada1;
    private Slot slotEntrada2;
    private Slot slotSalida;

    /**
     * Se ejecuta ANTES de cada test.
     * Configura un escenario limpio con 2 entradas y 1 salida.
     */
    @BeforeEach
    void setUp() {
        slotEntrada1 = new Slot();
        slotEntrada2 = new Slot();
        slotSalida = new Slot();

        // El orden en esta lista es crucial, define la prioridad
        List<Slot> entradas = Arrays.asList(slotEntrada1, slotEntrada2);
        
        merge = new Merge(entradas, slotSalida);
    }

    /**
     * Helper simple para crear un Mensaje.
     * No necesitamos contenido para este test, solo un objeto.
     */
    private Mensaje crearMensaje() {
        return new Mensaje();
    }

    /**
     * Prueba el caso donde solo la entrada 1 (prioridad alta) tiene un mensaje.
     */
    @Test
    void testExecute_CuandoSoloEntrada1TieneMensaje_MueveMensajeDeEntrada1() {
        // Arrange
        Mensaje msg1 = crearMensaje();
        slotEntrada1.enqueue(msg1);
        
        // Act
        merge.execute();
        
        // Assert
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debe estar vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 debe seguir vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debe tener 1 mensaje");
        assertSame(msg1, slotSalida.dequeuePoll(), "El mensaje debe ser el de la entrada 1");
    }

    /**
     * Prueba el caso donde la entrada 1 está vacía,
     * pero la entrada 2 (prioridad baja) tiene un mensaje.
     */
    @Test
    void testExecute_CuandoEntrada1VaciaYEntrada2TieneMensaje_MueveMensajeDeEntrada2() {
        // Arrange
        Mensaje msg2 = crearMensaje();
        slotEntrada2.enqueue(msg2);
        
        // Act
        merge.execute();
        
        // Assert
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debe seguir vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 debe estar vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debe tener 1 mensaje");
        assertSame(msg2, slotSalida.dequeuePoll(), "El mensaje debe ser el de la entrada 2");
    }

    /**
     * PRUEBA CLAVE: Prueba la lógica de prioridad.
     * Si ambas entradas tienen mensajes, solo debe mover el de la entrada 1
     * y el de la entrada 2 debe permanecer allí.
     */
    @Test
    void testExecute_CuandoAmbasEntradasTienenMensajes_MueveSoloMensajeDeEntrada1() {
        // Arrange
        Mensaje msg1 = crearMensaje();
        Mensaje msg2 = crearMensaje();
        
        slotEntrada1.enqueue(msg1); // Prioridad alta
        slotEntrada2.enqueue(msg2); // Prioridad baja
        
        // Act
        merge.execute();
        
        // Assert
        // El msg1 se movió
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debe estar vacía");
        assertEquals(1, slotSalida.getQueueSize(), "La salida debe tener 1 mensaje");
        assertSame(msg1, slotSalida.dequeuePoll(), "El mensaje debe ser el de la entrada 1");

        // El msg2 NO se movió (debido al 'break')
        assertEquals(1, slotEntrada2.getQueueSize(), "La entrada 2 NO debe estar vacía");
        assertSame(msg2, slotEntrada2.dequeuePoll(), "El mensaje 2 debe permanecer en la entrada 2");
    }

    /**
     * Prueba que si todas las colas de entrada están vacías, no pasa nada.
     */
    @Test
    void testExecute_CuandoTodasEntradasVacias_NoHaceNada() {
        // Arrange
        // Las colas están vacías por defecto
        
        // Act
        merge.execute();
        
        // Assert
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debe seguir vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 debe seguir vacía");
        assertTrue(slotSalida.isEmptyQueue(), "La salida debe seguir vacía");
    }

    /**
     * Prueba que múltiples llamadas a execute() drenan las colas
     * en el orden de prioridad correcto.
     */
    @Test
    void testExecute_MultiplesLlamadasDrenanColasEnOrdenDePrioridad() {
        // Arrange
        Mensaje msg1 = crearMensaje(); // Prioridad alta
        Mensaje msg2 = crearMensaje(); // Prioridad baja
        
        slotEntrada1.enqueue(msg1);
        slotEntrada2.enqueue(msg2);
        
        // Act (Llamada 1)
        merge.execute();
        
        // Assert (Llamada 1)
        assertEquals(1, slotSalida.getQueueSize(), "Salida debe tener 1 mensaje (msg1)");
        assertTrue(slotEntrada1.isEmptyQueue(), "Entrada 1 debe estar vacía");
        assertEquals(1, slotEntrada2.getQueueSize(), "Entrada 2 debe tener msg2");
        
        // Act (Llamada 2)
        merge.execute();

        // Assert (Llamada 2)
        assertEquals(2, slotSalida.getQueueSize(), "Salida debe tener 2 mensajes");
        assertTrue(slotEntrada1.isEmptyQueue(), "Entrada 1 sigue vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "Entrada 2 ahora debe estar vacía");
        
        // Verificamos el orden en la salida
        Mensaje out1 = slotSalida.dequeuePoll();
        Mensaje out2 = slotSalida.dequeuePoll();
        
        assertSame(msg1, out1, "El primer mensaje en salir debe ser msg1");
        assertSame(msg2, out2, "El segundo mensaje en salir debe ser msg2");
    }
}
