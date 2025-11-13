package Test.Tareas;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.Router.Correlator;

class CorrelatorTest {

    private Correlator correlator;
    private Slot slotEntrada1;
    private Slot slotEntrada2;
    private Slot slotSalida1;
    private Slot slotSalida2;

    // Usaremos un contador para asegurar IDs únicos
    private long idUnicoCounter = 1L;

    /**
     * Se ejecuta ANTES de cada test.
     * Configura un escenario limpio con 2 entradas y 2 salidas.
     */
    @BeforeEach
    void setUp() {
        slotEntrada1 = new Slot();
        slotEntrada2 = new Slot();
        slotSalida1 = new Slot();
        slotSalida2 = new Slot();

        List<Slot> entradas = Arrays.asList(slotEntrada1, slotEntrada2);
        List<Slot> salidas = Arrays.asList(slotSalida1, slotSalida2);

        correlator = new Correlator(entradas, salidas);
    }

    /**
     * Helper para crear un Mensaje con los IDs necesarios.
     * Es crucial que idUnico sea diferente para cada mensaje
     * para que removeByMessage() funcione correctamente.
     */
    private Mensaje crearMensaje(int idCorrelator) {
        Head head = new Head();
        head.setIdCorrelator(idCorrelator);
        head.setIdUnico(idUnicoCounter++); // Incrementamos el ID único
        
        Mensaje msg = new Mensaje();
        msg.setHead(head);
        // El body puede ser null para este test, no se usa.
        return msg;
    }


    /**
     * Prueba el "Happy Path": un set completo está presente
     * y debe ser movido a las colas de salida.
     */
    @Test
    void testExecute_CuandoSetEstaCompleto_MueveMensajesALaSalida() {
        // Arrange
        Mensaje msgE1 = crearMensaje(100); // ID Correlación 100
        Mensaje msgE2 = crearMensaje(100); // ID Correlación 100

        slotEntrada1.enqueue(msgE1);
        slotEntrada2.enqueue(msgE2);

        // Act
        correlator.execute();

        // Assert
        // 1. Las colas de entrada deben estar vacías
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debería estar vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 debería estar vacía");

        // 2. Las colas de salida deben contener los mensajes
        assertEquals(1, slotSalida1.getQueueSize(), "La salida 1 debería tener 1 mensaje");
        assertEquals(1, slotSalida2.getQueueSize(), "La salida 2 debería tener 1 mensaje");

        // 3. Verificamos que son los mensajes correctos
        assertSame(msgE1, slotSalida1.dequeuePoll(), "El mensaje en salida 1 no es el esperado");
        assertSame(msgE2, slotSalida2.dequeuePoll(), "El mensaje en salida 2 no es el esperado");
    }

    /**
     * Prueba que si falta un mensaje en una cola,
     * el set NO se procesa y los mensajes permanecen en la entrada.
     */
    @Test
    void testExecute_CuandoSetEstaIncompleto_NoMueveMensajes() {
        // Arrange
        Mensaje msgE1 = crearMensaje(100); // ID Correlación 100
        // Falta el mensaje en slotEntrada2

        slotEntrada1.enqueue(msgE1);

        // Act
        correlator.execute();

        // Assert
        // 1. Las colas de entrada NO deben estar vacías
        assertEquals(1, slotEntrada1.getQueueSize(), "La entrada 1 aún debe tener el mensaje");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 sigue vacía");

        // 2. Las colas de salida deben estar vacías
        assertTrue(slotSalida1.isEmptyQueue(), "La salida 1 debe estar vacía");
        assertTrue(slotSalida2.isEmptyQueue(), "La salida 2 debe estar vacía");
    }

    /**
     * Prueba que el Correlator procesa TODOS los sets completos
     * en una sola ejecución.
     */
    @Test
    void testExecute_MultiplesSetsCompletos_MueveTodosLosSets() {
        // Arrange
        Mensaje msgA_E1 = crearMensaje(100); // Set A
        Mensaje msgA_E2 = crearMensaje(100); // Set A

        Mensaje msgB_E1 = crearMensaje(200); // Set B
        Mensaje msgB_E2 = crearMensaje(200); // Set B

        // Añadimos en orden A, B
        slotEntrada1.enqueue(msgA_E1);
        slotEntrada1.enqueue(msgB_E1);
        
        // Añadimos en orden B, A (para probar que el orden no importa)
        slotEntrada2.enqueue(msgB_E2);
        slotEntrada2.enqueue(msgA_E2);
        
        // Act
        correlator.execute();

        // Assert
        // 1. Las colas de entrada deben estar vacías
        assertTrue(slotEntrada1.isEmptyQueue(), "La entrada 1 debería estar vacía");
        assertTrue(slotEntrada2.isEmptyQueue(), "La entrada 2 debería estar vacía");

        // 2. Las colas de salida deben tener 2 mensajes cada una
        assertEquals(2, slotSalida1.getQueueSize(), "La salida 1 debería tener 2 mensajes");
        assertEquals(2, slotSalida2.getQueueSize(), "La salida 2 debería tener 2 mensajes");
        
        // Opcional: Verificar que los mensajes correctos están en la salida
        List<Mensaje> salida1 = slotSalida1.getQueue();
        List<Mensaje> salida2 = slotSalida2.getQueue();
        
        assertTrue(salida1.contains(msgA_E1) && salida1.contains(msgB_E1));
        assertTrue(salida2.contains(msgA_E2) && salida2.contains(msgB_E2));
    }

    /**
     * Prueba que un mensaje en la cola de control (entrada 1)
     * con ID -1 es ignorado y no causa problemas.
     */
    @Test
    void testExecute_MensajeControlConIdNegativo_EsIgnorado() {
        // Arrange
        Mensaje msgE1_Invalido = crearMensaje(-1); // ID -1 (ignorable)
        Mensaje msgE2_Valido = crearMensaje(100);

        slotEntrada1.enqueue(msgE1_Invalido);
        slotEntrada2.enqueue(msgE2_Valido);

        // Act
        correlator.execute();

        // Assert
        // No se debe formar ningún set, nada se mueve
        assertEquals(1, slotEntrada1.getQueueSize(), "Entrada 1 debe mantener su mensaje");
        assertEquals(1, slotEntrada2.getQueueSize(), "Entrada 2 debe mantener su mensaje");
        assertTrue(slotSalida1.isEmptyQueue(), "Salida 1 debe estar vacía");
        assertTrue(slotSalida2.isEmptyQueue(), "Salida 2 debe estar vacía");
    }

    /**
     * Prueba la cláusula de guarda al inicio del execute().
     * Si cualquier cola está vacía, debe salir inmediatamente.
     */
    @Test
    void testExecute_CuandoUnaEntradaEstaVacia_NoHaceNada() {
        // Arrange
        Mensaje msgE1 = crearMensaje(100);
        slotEntrada1.enqueue(msgE1);
        // slotEntrada2 se deja vacía

        // Act
        correlator.execute();

        // Assert
        // Absolutamente nada debe moverse
        assertEquals(1, slotEntrada1.getQueueSize(), "Entrada 1 mantiene su mensaje");
        assertTrue(slotEntrada2.isEmptyQueue(), "Entrada 2 sigue vacía");
        assertTrue(slotSalida1.isEmptyQueue(), "Salida 1 debe estar vacía");
        assertTrue(slotSalida2.isEmptyQueue(), "Salida 2 debe estar vacía");
    }

    /**
     * Prueba un caso mixto: un set completo y uno incompleto.
     * Solo el set completo debe moverse.
     */
    @Test
    void testExecute_SetCompletoYSetIncompleto_MueveSoloElCompleto() {
        // Arrange
        Mensaje msgA_E1 = crearMensaje(100); // Set A (Completo)
        Mensaje msgA_E2 = crearMensaje(100); // Set A (Completo)

        Mensaje msgB_E1 = crearMensaje(200); // Set B (Incompleto)
        // Falta el msgB_E2

        slotEntrada1.enqueue(msgA_E1);
        slotEntrada1.enqueue(msgB_E1);
        slotEntrada2.enqueue(msgA_E2);
        
        // Act
        correlator.execute();

        // Assert
        // 1. Las colas de entrada solo deben tener el set incompleto
        assertEquals(1, slotEntrada1.getQueueSize(), "Entrada 1 solo debe tener el msg B");
        assertTrue(slotEntrada2.isEmptyQueue(), "Entrada 2 debe estar vacía");
        assertSame(msgB_E1, slotEntrada1.dequeuePoll(), "El mensaje restante debe ser el B1");

        // 2. Las colas de salida solo deben tener el set completo A
        assertEquals(1, slotSalida1.getQueueSize(), "Salida 1 debe tener el msg A");
        assertEquals(1, slotSalida2.getQueueSize(), "Salida 2 debe tener el msg A");
        assertSame(msgA_E1, slotSalida1.dequeuePoll());
        assertSame(msgA_E2, slotSalida2.dequeuePoll());
    }
}
