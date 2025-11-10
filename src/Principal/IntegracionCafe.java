package Principal;

import java.util.List;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import org.xml.sax.InputSource;

// Importamos los componentes que vamos a usar
import Puerto.PuertoEntrada;
import Puerto.PuertoSalida;
import Tarea.Router.Distributor;

public class IntegracionCafe {

    /**
     * Helper para crear un Document XML desde un String.
     * Es útil para no tener que leer archivos en la prueba.
     */
    private static Document createDocument(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(
                        new InputSource(new StringReader(xml))
                );
    }

    public static void main(String[] args) {
        System.out.println("Iniciando prueba de IntegracionCafe...");

        try {
            // 1. Crear los "canales" (Slots) por donde viajarán los mensajes
            Slot slotEntrada = new Slot();       // Del puerto de entrada al Distributor
            Slot slotSalidaTipoA = new Slot();  // Del Distributor al puerto de salida A
            Slot slotSalidaTipoB = new Slot();  // Del Distributor al puerto de salida B

            // 2. Crear los componentes (Puertos y Tareas) y "cablearlos"
            
            // El PuertoEntrada recibe datos "de fuera" y los pone en "slotEntrada"
            PuertoEntrada pEntrada = new PuertoEntrada(slotEntrada);
            
            // El Distributor escucha en "slotEntrada" y reparte a "slotSalidaTipoA" o "slotSalidaTipoB"
            Distributor distributor = new Distributor(slotEntrada, List.of(slotSalidaTipoA, slotSalidaTipoB));
            
            // Los PuertosDeSalida escuchan en sus respectivos slots (aunque en tu código, PuertoSalida.execute() está vacío)
            PuertoSalida pSalidaA = new PuertoSalida(slotSalidaTipoA);
            PuertoSalida pSalidaB = new PuertoSalida(slotSalidaTipoB);

            // 3. Configurar el Distributor
            // Le decimos qué XPath debe leer
            distributor.setXpath("/pedido/tipo");
            // Le decimos el orden de las salidas: la salida 0 es para "A", la salida 1 es para "B"
            distributor.setElementosSegunOrden(List.of("A", "B"));

            // 4. Simular la entrada de tres mensajes
            System.out.println("Enviando mensaje Pedido A...");
            pEntrada.setDocument(createDocument("<pedido><tipo>A</tipo><valor>100</valor></pedido>"));
            pEntrada.execute(); // El mensaje 1 se coloca en slotEntrada

            System.out.println("Enviando mensaje Pedido B...");
            pEntrada.setDocument(createDocument("<pedido><tipo>B</tipo><valor>200</valor></pedido>"));
            pEntrada.execute(); // El mensaje 2 se coloca en slotEntrada
            
            System.out.println("Enviando mensaje Pedido C (será descartado)...");
            pEntrada.setDocument(createDocument("<pedido><tipo>C</tipo><valor>300</valor></pedido>"));
            pEntrada.execute(); // El mensaje 3 se coloca en slotEntrada

            System.out.println("Mensajes encolados en slotEntrada: " + slotEntrada.getQueueSize()); // Debería ser 3

            // 5. Ejecutar el flujo (simulando un "tick" del sistema)
            // En un sistema real, esto sería un bucle o hilos.
            // Aquí, llamamos a execute() por cada mensaje que sabemos que hay.
            
            System.out.println("\n--- Ejecutando Distributor (procesa Pedido A) ---");
            distributor.execute(); 
            
            System.out.println("--- Ejecutando Distributor (procesa Pedido B) ---");
            distributor.execute(); 
            
            System.out.println("--- Ejecutando Distributor (procesa Pedido C) ---");
            distributor.execute(); // Procesa Pedido C (lo descarta porque "C" no está en la lista)

            // (No llamamos a pSalidaA.execute() porque actualmente no hace nada)

            // 6. Verificar los resultados
            System.out.println("\n--- Resultados ---");
            System.out.println("Mensajes en slotEntrada (debe ser 0): " + slotEntrada.getQueueSize());
            System.out.println("Mensajes en slotSalidaTipoA (debe ser 1): " + slotSalidaTipoA.getQueueSize());
            System.out.println("Mensajes en slotSalidaTipoB (debe ser 1): " + slotSalidaTipoB.getQueueSize());
            
            // Vaciamos las colas para ver el contenido
            if (!slotSalidaTipoA.isEmptyQueue()) {
                System.out.println("Mensaje en A: El nodo raíz es <" + slotSalidaTipoA.dequeuePoll().getBody().getDocumentElement().getTagName() + ">");
            }
            if (!slotSalidaTipoB.isEmptyQueue()) {
                System.out.println("Mensaje en B: El nodo raíz es <" + slotSalidaTipoB.dequeuePoll().getBody().getDocumentElement().getTagName() + ">");
            }
            
            System.out.println("\nPrueba finalizada.");

        } catch (Exception e) {
            System.out.println("Ha ocurrido un error en el main:");
            e.printStackTrace();
        }
    }
}