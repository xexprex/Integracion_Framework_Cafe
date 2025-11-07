package Test.Tareas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

// Importaciones necesarias de tu proyecto
import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.Router.Replicator;

class ReplicatorTest {

    private Replicator replicator;
    private Slot slotEntrada;
    private Slot slotSalida1;
    private Slot slotSalida2;

    private static final long ID_ORIGINAL = 999L;

    /**
     * Se ejecuta ANTES de cada test.
     * Configura un escenario limpio con 1 entrada y 2 salidas.
     */
    @BeforeEach
    void setUp() {
        slotEntrada = new Slot();
        slotSalida1 = new Slot();
        slotSalida2 = new Slot();

        List<Slot> salidas = Arrays.asList(slotSalida1, slotSalida2);
        
        replicator = new Replicator(slotEntrada, salidas);
    }

    /**
     * Helper para crear un Mensaje con un Head y un IdUnico conocidos.
     */
    private Mensaje crearMensajeOriginal() {
        Head head = new Head();
        head.setIdUnico(ID_ORIGINAL); // ID que esperamos sea reemplazado
        head.setIdCorrelator(123); // Dato de prueba para verificar clonación
        
        Mensaje msg = new Mensaje();
        msg.setHead(head);
        return msg;
    }

    /**
     * Prueba el "Happy Path": Un mensaje en la entrada
     * debe ser clonado y enviado a ambas salidas.
     */
    @Test
    void testExecute_CuandoMensajeExiste_ReplicaEnTodasSalidas() {
        // Arrange
        Mensaje msgOriginal = crearMensajeOriginal();
        slotEntrada.enqueue(msgOriginal);

        // Act
        replicator.execute();

        // Assert
        // 1. La entrada debe estar vacía (mensaje consumido)
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe estar vacía");

        // 2. Ambas salidas deben tener un mensaje
        assertEquals(1, slotSalida1.getQueueSize(), "La salida 1 debe tener 1 mensaje");
        assertEquals(1, slotSalida2.getQueueSize(), "La salida 2 debe tener 1 mensaje");

        // 3. Obtenemos los mensajes de salida
        Mensaje msgOut1 = slotSalida1.dequeuePoll();
        Mensaje msgOut2 = slotSalida2.dequeuePoll();

        // 4. VERIFICACIÓN CLAVE: Deben ser CLONES, no el original
        assertNotSame(msgOriginal, msgOut1, "El mensaje 1 no debe ser la misma instancia que el original");
        assertNotSame(msgOriginal, msgOut2, "El mensaje 2 no debe ser la misma instancia que el original");
        assertNotSame(msgOut1, msgOut2, "Los mensajes de salida deben ser instancias diferentes entre sí");

        // 5. VERIFICACIÓN CLAVE: El IdUnico debe haber sido REEMPLAZADO
        assertNotEquals(ID_ORIGINAL, msgOut1.getHead().getIdUnico(), "El ID único de la salida 1 debe ser nuevo");
        assertNotEquals(ID_ORIGINAL, msgOut2.getHead().getIdUnico(), "El ID único de la salida 2 debe ser nuevo");
        
        // 6. Verificamos que los otros datos del Head se clonaron correctamente
        assertEquals(123, msgOut1.getHead().getIdCorrelator(), "Los datos del Head (correlator) deben clonarse");
        assertEquals(123, msgOut2.getHead().getIdCorrelator(), "Los datos del Head (correlator) deben clonarse");
    }

    /**
     * Prueba que si la cola de entrada está vacía, no ocurre nada.
     */
    @Test
    void testExecute_CuandoEntradaVacia_NoHaceNada() {
        // Arrange
        // La cola de entrada está vacía por defecto
        
        // Act
        replicator.execute();
        
        // Assert
        assertTrue(slotEntrada.isEmptyQueue(), "La entrada debe seguir vacía");
        assertTrue(slotSalida1.isEmptyQueue(), "La salida 1 debe estar vacía");
        assertTrue(slotSalida2.isEmptyQueue(), "La salida 2 debe estar vacía");
    }
}
