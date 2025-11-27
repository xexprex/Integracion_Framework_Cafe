package Principal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Conector.ConectorFicheroEntrada;
import Conector.ConectorFicheroSalida;
import Conector.ConectorSolicitudDB;
import Puerto.PuertoEntrada;
import Puerto.PuertoSalida;
import Puerto.PuertoSolicitante;
import Tarea.ITarea;
import Tarea.TareaFactory;
import io.github.cdimascio.dotenv.Dotenv;

public class cafe {

    public void CafesinPago() {
        try {
            // --- 1. CONFIGURACIÓN INICIAL (Cargar .env y Conexión DB) ---
            Dotenv dotenv = Dotenv.load();
            String basededatos = dotenv.get("DB_HOST");
            String usuario = dotenv.get("DB_USER");
            String contaseña = dotenv.get("DB_PASS");

            String connectionUrl = basededatos + ";" +
                    "database=Practica_Integracion;" +
                    "user=" + usuario + ";" +
                    "password=" + contaseña + ";" +
                    "encrypt=true;" +
                    "trustServerCertificate=false;" +
                    "hostNameInCertificate=*.database.windows.net;";

            System.out.println("Conectando a la base de datos...");

            try (java.sql.Connection checkConn = java.sql.DriverManager.getConnection(connectionUrl)) {

                // Verificamos si la conexión es válida con un timeout de 5 segundos
                if (!checkConn.isValid(5)) {
                    throw new RuntimeException("La conexión se estableció pero no es válida.");
                }

            } catch (java.sql.SQLException e) {
                // Si falla (entra aquí), lanzamos una excepción que detiene el programa
                // inmediatamente
                throw new RuntimeException(
                        "ERROR FATAL: No se pudo conectar a la Base de Datos. Verifique el archivo .env y la VPN/Red.",
                        e);
            }

            // --- 2. DEFINICIÓN DE TODOS LOS SLOTS ---
            // Definimos todos los nombres de los canales en una lista simple
            List<String> nombresSlots = Arrays.asList(
                    "comandasIn", "splitterOut","correlatorOut",
                    "bebidasFrias", "bebidasCalientes",
                    // Rama Fría
                    "repFrioToTrans", "repFrioToCorr", "transFrioToPuerto",
                    "barmanFrioOut", "corrFrioToEnrichP", "corrFrioToEnrichC", "enrichFrioToMerge",
                    // Rama Caliente
                    "repCalToTrans", "repCalToCorr", "transCalToPuerto",
                    "barmanCalOut", "corrCalToEnrichP", "corrCalToEnrichC", "enrichCalToMerge",
                    // Unión y Pago
                    "mergedOut", "aggregatorOut", "finalCamarero");

            // CREACIÓN AUTOMÁTICA: Un Map guarda todos los slots por nombre
            Map<String, Slot> slots = new HashMap<>();
            for (String nombre : nombresSlots) {
                slots.put(nombre, new Slot());
            }
            List<ITarea> tareas = new ArrayList<>();
            // --- 3. "CABLEADO" DE COMPONENTES ---
            // A. PUERTO DE ENTRADA (COMANDAS)
            // Instanciamos manualmente porque son componentes de frontera (I/O)
            PuertoEntrada puertoComandas = new PuertoEntrada(slots.get("comandasIn"));
            ConectorFicheroEntrada conectorComandas = new ConectorFicheroEntrada(puertoComandas);

            // SPLITTER (1)
            Map<String, Object> cfgSplit = new HashMap<>();
            cfgSplit.put("xpath", "//drink");

            tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.SPLITTER, List.of(slots.get("comandasIn")),
                    List.of(slots.get("splitterOut")), cfgSplit));

            //ID SETTER
            tareas.add(TareaFactory.crearTarea(
                TareaFactory.TipoTarea.ID_SETTER,
                List.of(slots.get("splitterOut")),   // Entrada (viene del Splitter)
                List.of(slots.get("correlatorOut")), // Salida (va al Distributor)
                null // No requiere configuración extra
            ));

            // DISTRIBUTOR (2)
            Map<String, Object> cfgDist = new HashMap<>();
            cfgDist.put("xpath", "/drink/type");
            cfgDist.put("orden", Arrays.asList("cold", "hot"));

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.DISTRIBUTOR,
                    List.of(slots.get("correlatorOut")),
                    Arrays.asList(slots.get("bebidasFrias"), slots.get("bebidasCalientes")),
                    cfgDist));

            // Flujo frio
            // Replicator Frío
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.REPLICATOR,
                    List.of(slots.get("bebidasFrias")),
                    Arrays.asList(slots.get("repFrioToTrans"), slots.get("repFrioToCorr")),
                    null // Sin config extra
            ));

            // Translator Frío
            Map<String, Object> cfgTransFrio = new HashMap<>();
            cfgTransFrio.put("xslt", "src/main/resources/transformar_bebida_fria.xslt");

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.TRANSLATOR,
                    List.of(slots.get("repFrioToTrans")),
                    List.of(slots.get("transFrioToPuerto")),
                    cfgTransFrio));

            // Puerto y Conector Frío
            PuertoSolicitante puertoBarmanFrio = new PuertoSolicitante(
                    slots.get("transFrioToPuerto"),
                    slots.get("barmanFrioOut"));
            ConectorSolicitudDB conectorBarmanFrio = new ConectorSolicitudDB(puertoBarmanFrio, connectionUrl);

            // Correlator Frío
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.CORRELATOR,
                    Arrays.asList(slots.get("repFrioToCorr"), slots.get("barmanFrioOut")),
                    Arrays.asList(slots.get("corrFrioToEnrichP"), slots.get("corrFrioToEnrichC")),
                    null));

            // Enricher Frío
            Map<String, Object> cfgEnrichFrio = new HashMap<>();
            cfgEnrichFrio.put("xpath-p", "/drink"); // Principal
            cfgEnrichFrio.put("xpath-c", "/resultadoSQL/fila"); // Contexto

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.ENRICHER,
                    Arrays.asList(slots.get("corrFrioToEnrichP"), slots.get("corrFrioToEnrichC")),
                    List.of(slots.get("enrichFrioToMerge")), cfgEnrichFrio));

            // Flujo Caliente
            // Replicator Caliente
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.REPLICATOR,
                    List.of(slots.get("bebidasCalientes")),
                    Arrays.asList(slots.get("repCalToTrans"), slots.get("repCalToCorr")),
                    null));

            // Translator Caliente
            Map<String, Object> cfgTransCal = new HashMap<>();
            cfgTransCal.put("xslt", "src/main/resources/transformar_bebida_caliente.xslt");

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.TRANSLATOR,
                    List.of(slots.get("repCalToTrans")),
                    List.of(slots.get("transCalToPuerto")),
                    cfgTransCal));

            // Puerto y Conector Caliente
            PuertoSolicitante puertoBarmanCaliente = new PuertoSolicitante(
                    slots.get("transCalToPuerto"),
                    slots.get("barmanCalOut"));
            ConectorSolicitudDB conectorBarmanCaliente = new ConectorSolicitudDB(puertoBarmanCaliente, connectionUrl);

            // Correlator Caliente
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.CORRELATOR,
                    Arrays.asList(slots.get("repCalToCorr"), slots.get("barmanCalOut")),
                    Arrays.asList(slots.get("corrCalToEnrichP"), slots.get("corrCalToEnrichC")),
                    null));

            // Enricher Caliente
            Map<String, Object> cfgEnrichCal = new HashMap<>();
            cfgEnrichCal.put("xpath-p", "/drink");
            cfgEnrichCal.put("xpath-c", "/resultadoSQL/fila");

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.ENRICHER,
                    Arrays.asList(slots.get("corrCalToEnrichP"), slots.get("corrCalToEnrichC")),
                    List.of(slots.get("enrichCalToMerge")),
                    cfgEnrichCal));

            // Flujo Comun
            // Merger
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.MERGE,
                    Arrays.asList(slots.get("enrichFrioToMerge"), slots.get("enrichCalToMerge")),
                    List.of(slots.get("mergedOut")),
                    null));

            // Agregator
            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.AGREGATOR,
                    List.of(slots.get("mergedOut")),
                    List.of(slots.get("aggregatorOut")),
                    null));

            // TRANSLATOR FINAL (LIMPIEZA DE TICKET) ---
            Map<String, Object> cfgTransFinal = new HashMap<>();
            cfgTransFinal.put("xslt", "src/main/resources/transformar_ticket_final.xslt");

            tareas.add(TareaFactory.crearTarea(
                    TareaFactory.TipoTarea.TRANSLATOR,
                    List.of(slots.get("aggregatorOut")), // ENTRADA: Lo que sale del Agregator
                    List.of(slots.get("finalCamarero")), // SALIDA: El XML limpio para el fichero
                    cfgTransFinal));
            
            // SALIDA (CAMARERO)
            PuertoSalida puertoCamarero = new PuertoSalida(slots.get("finalCamarero"));
            ConectorFicheroSalida conectorCamarero = new ConectorFicheroSalida(puertoCamarero);

            File entregasDir = new File("./cafe/entregas");
            if (!entregasDir.exists()) {
                entregasDir.mkdirs();
            }
            conectorCamarero.setRutaSalida(entregasDir.getPath());

            // --- 4. EJECUCIÓN DEL FLUJO (INTERACTIVO Y AUTOMÁTICO) ---
            System.out.println("Introduce el número de comanda a procesar (ej: 1,3):");
            Scanner scanner = new Scanner(System.in);
            int numero = scanner.nextInt();

            // Preparar lista de archivos a procesar
            List<String> rutasComandas = new ArrayList<>();
            rutasComandas.add("src/main/resources/order" + numero + ".xml");

            // Bucle principal: Procesar cada comanda una por una
            for (String rutaComanda : rutasComandas) {
                System.out.println("\n--- Cargando Comanda: " + rutaComanda + " ---");

                File f = new File(rutaComanda);
                if (!f.exists()) {
                    System.out.println("Advertencia: No se encontró el archivo " + rutaComanda + ". Saltando...");
                    continue;
                }

                // A. INYECCIÓN INICIAL
                // Configuramos el conector de entrada con la ruta actual y cargamos el mensaje
                conectorComandas.setRuta(rutaComanda);
                conectorComandas.execute(); // Lee fichero -> Puerto
                puertoComandas.execute(); // Puerto -> Slot 'comandasIn'

                // B. MOTOR DE EJECUCIÓN (BUCLE WHILE)
                int ciclos = 0;

                // El sistema sigue rodando mientras haya mensajes en CUALQUIER slot
                while (!isSistemaEstable(slots)) {

                    // 1. Ejs (Las que creamos con la Factory)ecutar Tareas Automática
                    // Esto cubre: Splitter, Distributor, Replicators, Translators,
                    // Correlators, Enrichers, Merge, Agregator
                    for (ITarea tarea : tareas) {
                        tarea.execute();
                    }

                    // 2. Ejecutar Componentes Manuales (Conectores y Puertos de frontera)
                    // Estos no están en la lista 'tareas' porque requieren gestión de recursos
                    // externos

                    // --- Rama Fría (Base de Datos) ---
                    puertoBarmanFrio.execute(); // Mueve petición al conector (si hay)
                    conectorBarmanFrio.execute(); // Ejecuta SQL
                    puertoBarmanFrio.execute(); // Mueve respuesta al slot de salida

                    // --- Rama Caliente (Base de Datos) ---
                    puertoBarmanCaliente.execute();
                    conectorBarmanCaliente.execute();
                    puertoBarmanCaliente.execute();

                    // --- Salida Final (Fichero) ---
                    puertoCamarero.execute(); // Slot -> Conector
                    conectorCamarero.execute(); // Escribe fichero

                    // 3. Control de Atascos (Safety Check)
                    ciclos++;
                    if (ciclos > 200) { // Aumentado un poco por si hay muchos pasos
                        System.err.println("!!! ATASCO DETECTADO O BUCLE INFINITO !!!");
                        mostrarEstadoSlots(slots); // Método helper para ver dónde se quedó
                        break;
                    }
                }
                System.out.println("--- Comanda procesada en " + ciclos + " ciclos ---");
            }

            System.out.println("\nSimulación completada. Revisa el directorio './cafe/entregas'.");
            scanner.close();

        } catch (Exception e) {
            System.err.println("Ha ocurrido un error fatal en la simulación:");
            e.printStackTrace();
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Verifica si el sistema está "quieto".
     * Retorna TRUE si TODOS los slots del mapa están vacíos.
     */
    private static boolean isSistemaEstable(java.util.Map<String, Slot> slots) {
        for (Slot slot : slots.values()) {
            if (!slot.isEmptyQueue()) {
                return false; // Si encontramos uno con mensajes, el sistema sigue trabajando
            }
        }
        return true;
    }

    /**
     * Método de depuración.
     * Imprime por consola qué slots tienen mensajes atrapados.
     */
    private static void mostrarEstadoSlots(java.util.Map<String, Slot> slots) {
        System.err.println("--- ESTADO DE LOS SLOTS (DEBUG) ---");
        for (java.util.Map.Entry<String, Slot> entrada : slots.entrySet()) {
            Slot s = entrada.getValue();
            if (!s.isEmptyQueue()) {
                System.err.println(
                        " -> Slot ['" + entrada.getKey() + "'] tiene " + s.getQueueSize() + " mensajes pendientes.");
            }
        }
    }

}
