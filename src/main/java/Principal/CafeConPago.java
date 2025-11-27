package Principal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Conector.ConectorFicheroEntrada;
import Conector.ConectorFicheroSalida;
import Conector.ConectorSolicitudDB;
import Conector.ConectorStripePago;
import Puerto.PuertoEntrada;
import Puerto.PuertoSalida;
import Puerto.PuertoSolicitante;
import Tarea.ITarea;
import Tarea.TareaFactory;
import io.github.cdimascio.dotenv.Dotenv;

public class CafeConPago {

        public void CafePago() {
                System.out.println("Iniciando simulación (Ticket con Stock Numérico y Total)...");

                try {
                        // 1. CONFIGURACIÓN
                        Dotenv dotenv = Dotenv.load();
                        String connectionUrl = dotenv.get("DB_HOST") + ";" +
                                        "database=Practica_Integracion;" +
                                        "user=" + dotenv.get("DB_USER") + ";" +
                                        "password=" + dotenv.get("DB_PASS") + ";" +
                                        "encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;";

                        try (java.sql.Connection checkConn = java.sql.DriverManager.getConnection(connectionUrl)) {
                                if (!checkConn.isValid(5))
                                        throw new RuntimeException("Conexión inválida");
                        } catch (Exception e) {
                                throw new RuntimeException("ERROR FATAL BD: " + e.getMessage());
                        }

                        // 2. DEFINICIÓN DE SLOTS
                        List<String> nombresSlots = Arrays.asList(
                                        "comandasIn", "splitterOut", "correlatorOut","bebidasFrias", "bebidasCalientes",
                                        "repFrioToTrans", "repFrioToCorr", "transFrioToPuerto", "barmanFrioOut",
                                        "corrFrioToEnrichP", "corrFrioToEnrichC", "enrichFrioToMerge",
                                        "repCalToTrans", "repCalToCorr", "transCalToPuerto", "barmanCalOut",
                                        "corrCalToEnrichP", "corrCalToEnrichC", "enrichCalToMerge",
                                        "mergedOut", "aggregatorOut", "finalCamarero",
                                        "slotStripe", "slotFichero");

                        Map<String, Slot> slots = new HashMap<>();
                        for (String nombre : nombresSlots)
                                slots.put(nombre, new Slot());
                        List<ITarea> tareas = new ArrayList<>();

                        // 3. CONSTRUCCIÓN DEL FLUJO

                        // A. Entrada
                        PuertoEntrada puertoComandas = new PuertoEntrada(slots.get("comandasIn"));
                        ConectorFicheroEntrada conectorComandas = new ConectorFicheroEntrada(puertoComandas);

                        // 1. Splitter
                        Map<String, Object> cfgSplit = new HashMap<>();
                        cfgSplit.put("xpath", "//drink");
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.SPLITTER,
                                        List.of(slots.get("comandasIn")),
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

                        // --- Rama Fría ---
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.REPLICATOR,
                                        List.of(slots.get("bebidasFrias")),
                                        Arrays.asList(slots.get("repFrioToTrans"), slots.get("repFrioToCorr")), null));

                        Map<String, Object> cfgTransFrio = new HashMap<>();
                        cfgTransFrio.put("xslt", "src/main/resources/transformar_bebida_fria.xslt");
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.TRANSLATOR,
                                        List.of(slots.get("repFrioToTrans")),
                                        List.of(slots.get("transFrioToPuerto")), cfgTransFrio));

                        PuertoSolicitante puertoBarmanFrio = new PuertoSolicitante(slots.get("transFrioToPuerto"),
                                        slots.get("barmanFrioOut"));
                        ConectorSolicitudDB conectorBarmanFrio = new ConectorSolicitudDB(puertoBarmanFrio,
                                        connectionUrl);

                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.CORRELATOR,
                                        Arrays.asList(slots.get("repFrioToCorr"), slots.get("barmanFrioOut")),
                                        Arrays.asList(slots.get("corrFrioToEnrichP"), slots.get("corrFrioToEnrichC")),
                                        null));

                        Map<String, Object> cfgEnrichFrio = new HashMap<>();
                        cfgEnrichFrio.put("xpath-p", "/drink");
                        cfgEnrichFrio.put("xpath-c", "/resultadoSQL/fila");
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.ENRICHER,
                                        Arrays.asList(slots.get("corrFrioToEnrichP"), slots.get("corrFrioToEnrichC")),
                                        List.of(slots.get("enrichFrioToMerge")), cfgEnrichFrio));

                        // --- Rama Caliente ---
                        tareas.add(
                                        TareaFactory.crearTarea(TareaFactory.TipoTarea.REPLICATOR,
                                                        List.of(slots.get("bebidasCalientes")),
                                                        Arrays.asList(slots.get("repCalToTrans"),
                                                                        slots.get("repCalToCorr")),
                                                        null));

                        Map<String, Object> cfgTransCal = new HashMap<>();
                        cfgTransCal.put("xslt", "src/main/resources/transformar_bebida_caliente.xslt");
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.TRANSLATOR,
                                        List.of(slots.get("repCalToTrans")),
                                        List.of(slots.get("transCalToPuerto")), cfgTransCal));

                        PuertoSolicitante puertoBarmanCaliente = new PuertoSolicitante(slots.get("transCalToPuerto"),
                                        slots.get("barmanCalOut"));
                        ConectorSolicitudDB conectorBarmanCaliente = new ConectorSolicitudDB(puertoBarmanCaliente,
                                        connectionUrl);

                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.CORRELATOR,
                                        Arrays.asList(slots.get("repCalToCorr"), slots.get("barmanCalOut")),
                                        Arrays.asList(slots.get("corrCalToEnrichP"), slots.get("corrCalToEnrichC")),
                                        null));

                        Map<String, Object> cfgEnrichCal = new HashMap<>();
                        cfgEnrichCal.put("xpath-p", "/drink");
                        cfgEnrichCal.put("xpath-c", "/resultadoSQL/fila");
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.ENRICHER,
                                        Arrays.asList(slots.get("corrCalToEnrichP"), slots.get("corrCalToEnrichC")),
                                        List.of(slots.get("enrichCalToMerge")), cfgEnrichCal));

                        // --- Unión y Agregación ---
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.MERGE,
                                        Arrays.asList(slots.get("enrichFrioToMerge"), slots.get("enrichCalToMerge")),
                                        List.of(slots.get("mergedOut")), null));
                        tareas.add(TareaFactory.crearTarea(TareaFactory.TipoTarea.AGREGATOR,
                                        List.of(slots.get("mergedOut")),
                                        List.of(slots.get("aggregatorOut")), null));

                        // --- 13. TRANSLATOR FINAL (Genera el Ticket Limpio) ---
                        // Toma la salida del agregador y la limpia, calculando el total
                        Map<String, Object> cfgTransFinal = new HashMap<>();
                        cfgTransFinal.put("xslt", "src/main/resources/transformar_a_pago.xslt");

                        // --- REPLICATOR PARA BIFURCAR (Stripe + Fichero) ---
                        tareas.add(TareaFactory.crearTarea(
                                        TareaFactory.TipoTarea.TRANSLATOR,
                                        List.of(slots.get("aggregatorOut")), // Leemos del agregador
                                        List.of(slots.get("finalCamarero")), // Escribimos al slot final
                                        cfgTransFinal));

                        // Tomamos el mensaje de 'finalCamarero' y lo duplicamos
                        tareas.add(TareaFactory.crearTarea(
                                        TareaFactory.TipoTarea.REPLICATOR,
                                        List.of(slots.get("finalCamarero")), // Entrada
                                        Arrays.asList(slots.get("slotStripe"), slots.get("slotFichero")), // Salidas
                                        null));

                        // SALIDA FICHERO
                        PuertoSalida puertoCamarero = new PuertoSalida(slots.get("slotFichero"));
                        ConectorFicheroSalida conectorCamarero = new ConectorFicheroSalida(puertoCamarero);
                        File entregasDir = new File("./cafe/entregas");
                        if (!entregasDir.exists())
                                entregasDir.mkdirs();
                        conectorCamarero.setRutaSalida(entregasDir.getPath());

                        // --- RAMA B: CONECTOR STRIPE (Pago API) ---
                        // Definimos el puerto y el conector para Stripe 
                        PuertoSalida puertoStripe = new PuertoSalida(slots.get("slotStripe"));
                        String stripeKey = dotenv.get("STRIPE_KEY");
                        ConectorStripePago conectorStripe = new ConectorStripePago(puertoStripe, stripeKey);

                        // 4. EJECUCIÓN
                        Scanner scanner = new Scanner(System.in);
                        System.out.println("Introduce el número de comanda (ej: 1,3):");
                        int numero = scanner.nextInt();

                        String ruta = "src/main/resources/order" + numero + ".xml";
                        if (!new File(ruta).exists())
                                throw new FileNotFoundException("El archivo no existe");

                        System.out.println("\n--- Procesando: " + ruta + " ---");
                        conectorComandas.setRuta(ruta);
                        conectorComandas.execute();
                        puertoComandas.execute();

                        int ciclos = 0;
                        while (!isSistemaEstable(slots)) {
                                // Automáticas
                                for (ITarea t : tareas)
                                        t.execute();

                                // Manuales (BD y Salida)
                                puertoBarmanFrio.execute();
                                conectorBarmanFrio.execute();
                                puertoBarmanFrio.execute();

                                puertoBarmanCaliente.execute();
                                conectorBarmanCaliente.execute();
                                puertoBarmanCaliente.execute();

                                puertoCamarero.execute();
                                conectorCamarero.execute();

                                puertoStripe.execute(); 
                                conectorStripe.execute();

                                ciclos++;
                                if (ciclos > 200)
                                        break;
                        }
                        System.out.println("--- Fin (" + ciclos + " ciclos) ---");

                        scanner.close();

                } catch (Exception e) {
                        System.out.println("Error en el sistema.");
                }
        }

        // --- MÉTODOS AUXILIARES ---

        private static boolean isSistemaEstable(Map<String, Slot> slots) {
                for (Slot slot : slots.values()) {
                        if (!slot.isEmptyQueue())
                                return false;
                }
                return true;
        }
}