package Principal;

import java.util.List;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder; // Necesario para parsear
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File; // Necesario para leer archivos

// Importamos los componentes que vamos a usar
import Puerto.PuertoEntrada;
import Puerto.PuertoSalida;
import Tarea.Router.Distributor;
import Principal.Slot; // Importamos Slot explícitamente

public class IntegracionCafe {

    /**
     * ¡NUEVO HELPER!
     * Lee un archivo XML desde una ruta y lo convierte en un Document.
     */
    private static Document parseXmlFile(String filePath) throws Exception {
        File xmlFile = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    public static void main(String[] args) {
        
        // 1. Validar que el usuario ha pasado archivos como argumentos
        if (args.length == 0) {
            System.err.println("Error: No se proporcionaron archivos XML.");
            System.err.println("Modo de uso: java Principal.IntegracionCafe <ruta/archivo1.xml> <ruta/archivo2.xml> ...");
            return;
        }

        System.out.println("🚀 Iniciando flujo con " + args.length + " documento(s) externo(s)...");

        try {
            // 2. Crear los "canales" (Slots)
            Slot slotEntrada = new Slot();       // (pEntrada) -> (distributor)
            Slot slotSalidaTipoA = new Slot();  // (distributor) -> (pSalidaA)
            Slot slotSalidaTipoB = new Slot();  // (distributor) -> (pSalidaB)

            // 3. Crear y "cablear" los componentes
            PuertoEntrada pEntrada = new PuertoEntrada(slotEntrada);
            Distributor distributor = new Distributor(slotEntrada, List.of(slotSalidaTipoA, slotSalidaTipoB));
            PuertoSalida pSalidaA = new PuertoSalida(slotSalidaTipoA);
            PuertoSalida pSalidaB = new PuertoSalida(slotSalidaTipoB);

            // 4. Configurar el Distributor
            distributor.setXpath("/pedido/tipo");
            distributor.setElementosSegunOrden(List.of("A", "B")); // A -> salida 0, B -> salida 1

            // 5. Simular la entrada de mensajes (¡LEYENDO LOS ARCHIVOS!)
            System.out.println("\n--- 📥 Inyectando mensajes desde archivos ---");
            
            for (String rutaArchivo : args) {
                try {
                    System.out.println("... Leyendo archivo: " + rutaArchivo);
                    Document doc = parseXmlFile(rutaArchivo);
                    pEntrada.setDocument(doc);
                    pEntrada.execute(); // Inyecta el documento en slotEntrada
                } catch (Exception e) {
                    System.err.println("No se pudo parsear el archivo " + rutaArchivo + ": " + e.getMessage());
                }
            }

            System.out.println("Mensajes encolados en slotEntrada: " + slotEntrada.getQueueSize());

            // 6. Ejecutar el flujo (motor de integración)
            System.out.println("\n--- ⚙️  Ejecutando Tareas ---");
            while (!slotEntrada.isEmptyQueue() || !slotSalidaTipoA.isEmptyQueue() || !slotSalidaTipoB.isEmptyQueue()) {
                distributor.execute(); 
                pSalidaA.execute();
                pSalidaB.execute();
            }
            
            // 7. Verificar los resultados
            System.out.println("\n--- ✅ Flujo completado ---");
            System.out.println("Mensajes restantes en slotEntrada (debe ser 0): " + slotEntrada.getQueueSize());
            System.out.println("Mensajes restantes en slotSalidaTipoA (debe ser 0): " + slotSalidaTipoA.getQueueSize());
            System.out.println("Mensajes restantes en slotSalidaTipoB (debe ser 0): " + slotSalidaTipoB.getQueueSize());

        } catch (Exception e) {
            System.out.println("\n--- ❌ Ha ocurrido un error en el main ---");
            e.printStackTrace();
        }
    }
}