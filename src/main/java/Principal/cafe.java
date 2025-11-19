package Principal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import Conector.ConectorFicheroEntrada;
import Conector.ConectorFicheroSalida;
import Conector.ConectorSolicitudDB;
import Puerto.PuertoEntrada;
import Puerto.PuertoSalida;
import Puerto.PuertoSolicitante;
import Tarea.Router.Correlator;
import Tarea.Router.Distributor;
import Tarea.Router.Merge;
import Tarea.Router.Replicator;
import Tarea.Transformer.Agregator;
import Tarea.Transformer.Splitter;
import Tarea.Transformer.Translator;
import Tarea.modifier.ContextEnricher;
import io.github.cdimascio.dotenv.Dotenv;

public class cafe {
    public static void main(String[] args) {
        System.out.println("Iniciando la simulación de IntegracionCafe...");

        try {       
        
        	 // --- 1. CONFIGURACIÓN INICIAL (Cargar .env y Conexión DB) ---
            Dotenv dotenv = Dotenv.load();
            String basededatos = dotenv.get("DB_HOST");
            String usuario = dotenv.get("DB_USER");
            String contaseña = dotenv.get("DB_PASS"); // Clave de Stripe y Pass de DB
            String stripeApiKey = dotenv.get("STRIPE_KEY"); // --- NUEVO ---

            String connectionUrl = basededatos + ";" +
                    "database=Practica_Integracion;" +
                    "user=" + usuario + ";" +
                    "password=" + contaseña + ";" +
                    "encrypt=true;" +
                    "trustServerCertificate=false;" +
                    "hostNameInCertificate=*.database.windows.net;";
        	
        	System.out.println("Conectando a la base de datos...");

            // --- 2. DEFINICIÓN DE TODOS LOS SLOTS (CANALES) ---
            
            // a. Entrada
            Slot slotComandasIn = new Slot();
            
            // b. Splitter -> Distributor
            Slot slotSplitterOut = new Slot();
            
            // c. Distributor -> Replicators
            Slot slotBebidasFrias = new Slot();
            Slot slotBebidasCalientes = new Slot();
            
            // d. Flujo Frío
            Slot slotRepFrio1_ToTrans = new Slot();
            Slot slotRepFrio2_ToCorr = new Slot();
            Slot slotTransFrio_ToPuerto = new Slot();
            Slot slotBarmanFrio_FromPuerto = new Slot();
            Slot slotCorrFrio1_ToEnrich = new Slot();
            Slot slotCorrFrio2_ToEnrich = new Slot();
            Slot slotEnrichFrio_ToMerge = new Slot();

            // f. Flujo Caliente
            Slot slotRepCal1_ToTrans = new Slot();
            Slot slotRepCal2_ToCorr = new Slot();
            Slot slotTransCal_ToPuerto = new Slot();
            Slot slotBarmanCal_FromPuerto = new Slot();
            Slot slotCorrCal1_ToEnrich = new Slot();
            Slot slotCorrCal2_ToEnrich = new Slot();
            Slot slotEnrichCal_ToMerge = new Slot();

            // h. Merger -> Aggregator
            Slot slotMergedOut = new Slot();
            
            // i. Aggregator -> Flujo de Pago
            Slot slotAggregatorOut = new Slot();

            // --- NUEVOS SLOTS PARA EL FLUJO DE PAGO ---
            /*Slot slotAgg_To_TransPago = new Slot();
            Slot slotAgg_To_EnrichPago = new Slot();
            Slot slotPeticionPago = new Slot();
            Slot slotRespuestaPago = new Slot();
            Slot slotFinalParaCamarero = new Slot(); */// --- NUEVO ---


            // Lista de todos los slots para el helper 'isSistemaEstable'
            // --- MODIFICADO --- (Añadidos los nuevos slots)
            Slot[] todosLosSlots = {
                slotComandasIn, slotSplitterOut, slotBebidasFrias, slotBebidasCalientes,
                slotRepFrio1_ToTrans, slotRepFrio2_ToCorr, slotTransFrio_ToPuerto, slotBarmanFrio_FromPuerto,
                slotCorrFrio1_ToEnrich, slotCorrFrio2_ToEnrich, slotEnrichFrio_ToMerge,
                slotRepCal1_ToTrans, slotRepCal2_ToCorr, slotTransCal_ToPuerto, slotBarmanCal_FromPuerto,
                slotCorrCal1_ToEnrich, slotCorrCal2_ToEnrich, slotEnrichCal_ToMerge,
                slotMergedOut, slotAggregatorOut,
                // Slots de pago
                //slotAgg_To_TransPago, slotAgg_To_EnrichPago, slotPeticionPago, slotRespuestaPago, slotFinalParaCamarero
            };


            // --- 3. "CABLEADO" DE COMPONENTES SEGÚN PDF ---

            // a. Puerto de Entrada Comandas
            PuertoEntrada puertoComandas = new PuertoEntrada(slotComandasIn);
            ConectorFicheroEntrada conectorComandas = new ConectorFicheroEntrada(puertoComandas);

            // b. Splitter (1)
            Splitter splitter = new Splitter(slotComandasIn, slotSplitterOut);
            splitter.setXPathExpression("//drink"); // Basado en order1.xml

            // c. Distributor (2)
            Distributor distributor = new Distributor(slotSplitterOut, Arrays.asList(slotBebidasFrias, slotBebidasCalientes));
            distributor.setXpath("/drink/type"); // Basado en order1.xml
            distributor.setElementosSegunOrden(Arrays.asList("cold", "hot")); 

            // --- Flujo Frío (3, 4, 5, 6) ---
            Replicator replicatorFrio = new Replicator(slotBebidasFrias, Arrays.asList(slotRepFrio1_ToTrans, slotRepFrio2_ToCorr));
            Translator translatorFrio = new Translator(List.of(slotRepFrio1_ToTrans), List.of(slotTransFrio_ToPuerto));
            translatorFrio.setRutaXSLT("src/main/resources/transformar_bebida_fria.xslt"); 
            PuertoSolicitante puertoBarmanFrio = new PuertoSolicitante(slotTransFrio_ToPuerto, slotBarmanFrio_FromPuerto);
            ConectorSolicitudDB conectorBarmanFrio = new ConectorSolicitudDB(puertoBarmanFrio, connectionUrl);
            Correlator correlatorFrio = new Correlator(
                Arrays.asList(slotRepFrio2_ToCorr, slotBarmanFrio_FromPuerto), 
                Arrays.asList(slotCorrFrio1_ToEnrich, slotCorrFrio2_ToEnrich)
            );
            ContextEnricher enricherFrio = new ContextEnricher(
                Arrays.asList(slotCorrFrio1_ToEnrich, slotCorrFrio2_ToEnrich), 
                List.of(slotEnrichFrio_ToMerge)
            );
            enricherFrio.setXPathPrincipal("/drink"); 
            enricherFrio.setXPathContexto("/resultadoSQL/fila"); 

            // --- Flujo Caliente (7, 8, 9, 10) --- 
            Replicator replicatorCaliente = new Replicator(slotBebidasCalientes, Arrays.asList(slotRepCal1_ToTrans, slotRepCal2_ToCorr));
            Translator translatorCaliente = new Translator(List.of(slotRepCal1_ToTrans), List.of(slotTransCal_ToPuerto));
            translatorCaliente.setRutaXSLT("src/main/resources/transformar_bebida_caliente.xslt");
            PuertoSolicitante puertoBarmanCaliente = new PuertoSolicitante(slotTransCal_ToPuerto, slotBarmanCal_FromPuerto);
            ConectorSolicitudDB conectorBarmanCaliente = new ConectorSolicitudDB(puertoBarmanCaliente, connectionUrl);
            Correlator correlatorCaliente = new Correlator(
                Arrays.asList(slotRepCal2_ToCorr, slotBarmanCal_FromPuerto),
                Arrays.asList(slotCorrCal1_ToEnrich, slotCorrCal2_ToEnrich)
            );
            ContextEnricher enricherCaliente = new ContextEnricher(
                Arrays.asList(slotCorrCal1_ToEnrich, slotCorrCal2_ToEnrich),
                List.of(slotEnrichCal_ToMerge)
            );
            enricherCaliente.setXPathPrincipal("/drink");
            enricherCaliente.setXPathContexto("/resultadoSQL/fila");

            // --- Flujo Común (11, 12) ---
            // h. Merger (11)
            Merge merger = new Merge(Arrays.asList(slotEnrichFrio_ToMerge, slotEnrichCal_ToMerge), slotMergedOut);
            // i. Aggregator (12)
            Agregator agregator = new Agregator(List.of(slotMergedOut), List.of(slotAggregatorOut));

            
            // --- INICIO: NUEVO FLUJO DE PAGO (Pasos 13, 14, 15, 16) ---

            // j. Replicator (13): Duplica la comanda agregada
           /* Replicator replicatorPago = new Replicator(slotAggregatorOut, Arrays.asList(slotAgg_To_TransPago, slotAgg_To_EnrichPago));

            // k. Translator (14): Transforma la comanda en petición de pago
            Translator translatorPago = new Translator(List.of(slotAgg_To_TransPago), List.of(slotPeticionPago));
            translatorPago.setRutaXSLT("src/main/resources/transformar_a_pago.xslt");

            // l. Conector Stripe (15): Llama a la API de Stripe
            PuertoSolicitante puertoPago = new PuertoSolicitante(slotPeticionPago, slotRespuestaPago);
            ConectorStripePago conectorStripe = new ConectorStripePago(puertoPago, stripeApiKey);

            // m. Enricher (16): Añade la respuesta de pago a la comanda final
            ContextEnricher enricherPago = new ContextEnricher(
                Arrays.asList(slotAgg_To_EnrichPago, slotRespuestaPago), // Entradas: Comanda original y Respuesta de Stripe
                List.of(slotFinalParaCamarero) // Salida: Comanda final con datos de pago
            );
            enricherPago.setXPathPrincipal("/cafe_order"); // Añade la info en la raíz de la comanda
            enricherPago.setXPathContexto("/resultadoPago"); // Coge el XML de respuesta de Stripe
            */
            // --- FIN: NUEVO FLUJO DE PAGO ---


            // n. Puerto de Salida Camarero (Final)
            // --- MODIFICADO ---: Ahora lee del slot 'slotFinalParaCamarero'
            PuertoSalida puertoCamarero = new PuertoSalida(slotAggregatorOut);
            ConectorFicheroSalida conectorCamarero = new ConectorFicheroSalida(puertoCamarero);
            
            File entregasDir = new File("./cafe/entregas");
            if (!entregasDir.exists()) {
                entregasDir.mkdirs();
            }
            conectorCamarero.setRutaSalida(entregasDir.getPath()); 


            // --- 4. EJECUCIÓN DEL FLUJO ---

            System.out.println("Introduce los números de comanda a procesar (ej: 1,3,8):");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine(); // Lee la línea "1,3,8"

            // 2. Procesar la entrada
            List<String> comandas = new ArrayList<>();
            String[] numeros = input.split(","); // Divide el string en ["1", "3", "8"]

            for (String num : numeros) {
                String numLimpio = num.trim(); // Quita espacios (por si escriben "1, 3")
                if (!numLimpio.isEmpty()) { // Evita procesar si escriben "1,,3"
                    comandas.add("src/main/resources/order" + numLimpio + ".xml");
                }
            }

            System.out.println("Procesando " + comandas.size() + " comandas...");

            for (String rutaComanda : comandas) {
                System.out.println("\n--- Cargando Comanda: " + rutaComanda + " ---");
                
                File f = new File(rutaComanda);
                if(!f.exists()) { 
                    System.out.println("Advertencia: No se encontró el archivo " + rutaComanda + ". Saltando...");
                    continue; // Salta al siguiente bucle si el orderX.xml no existe
                }

                // a. Cargar la comanda en el primer slot
                conectorComandas.setRuta(rutaComanda);
                conectorComandas.execute();
                puertoComandas.execute();

                // b. Iniciar el "motor"
                int ciclos = 0;
                
                while (!isSistemaEstable(todosLosSlots)) {
                    
                    // Ejecutar todos los componentes en orden lógico
                    splitter.execute();
                    distributor.execute();
                    
                    replicatorFrio.execute();
                    replicatorCaliente.execute();
                    
                    translatorFrio.execute();
                    translatorCaliente.execute();
                    
                    puertoBarmanFrio.execute();
                    puertoBarmanCaliente.execute();
                    
                    conectorBarmanFrio.execute();
                    conectorBarmanCaliente.execute();
                    
                    puertoBarmanFrio.execute();
                    puertoBarmanCaliente.execute();
                    
                    correlatorFrio.execute();
                    correlatorCaliente.execute();
                    
                    enricherFrio.execute();
                    enricherCaliente.execute();
                    
                    merger.execute();
                    agregator.execute();
                    
                    // --- N1UEVO ---: Ejecutar componentes de pago
                    /*replicatorPago.execute();
                    translatorPago.execute();
                    puertoPago.execute();
                    
                    if(!slotPeticionPago.isEmptyQueue()) {
                    conectorStripe.execute(); // Segundo execute para mover la respuesta
                    } 
                    puertoPago.execute();
                    enricherPago.execute();*/
                    
                    // --- MODIFICADO ---: Salida a Fichero (Camarero)
                    puertoCamarero.execute(); 
                    conectorCamarero.execute(); 

                    ciclos++;
                    if (ciclos > 100) { 
                        System.err.println("Simulación detenida: Posible bucle infinito o atasco.");
                        // Imprimir estado de las colas para depurar
                        System.err.println("--- ESTADO DE SLOTS (ATASCO) ---");
                        for (Slot s : todosLosSlots) {
                            if (!s.isEmptyQueue()) {
                                System.err.println("Slot " + s.toString() + " tiene " + s.getQueueSize() + " mensajes.");
                            }
                        }
                        // --- FIN DEBUG ---
                        break;
                    }
                }
                System.out.println("--- Comanda procesada en " + ciclos + " ciclos ---");
            }

            System.out.println("\nSimulación completada. Revisa el directorio './cafe/entregas'.");

        } catch (Exception e) {
            System.err.println("Ha ocurrido un error fatal en la simulación:");
            e.printStackTrace();
        }
    }

    
    private static boolean isSistemaEstable(Slot... slots) {
        // Usamos la API Stream para comprobar si "alguno" de los slots NO está vacío
        return !Stream.of(slots).anyMatch(slot -> !slot.isEmptyQueue());
    }
}
