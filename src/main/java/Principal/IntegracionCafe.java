package Principal;
//package main.java;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import Conector.ConectorSolicitudDB;
import Puerto.PuertoSolicitante;

public class IntegracionCafe {

    /**
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
        
        /*if (args.length == 0) {
            System.err.println("Error: No se proporcionaron archivos XML.");
            System.err.println("Uso: java Principal.IntegracionCafe <ruta_archivo1> <ruta_archivo2> ...");
            return;
        }

        System.out.println("Iniciando PRUEBA DE TODAS LAS TAREAS...");

        try {
            // --- 1. DEFINIR TODOS LOS SLOTS (Canales) ---
            Slot slotEntradaPedidos = new Slot();
            Slot slotEntradaContexto = new Slot();
            Slot slotEntradaCorrelator = new Slot();
            
            Slot slotFiltrados = new Slot();
            Slot slotLibros = new Slot();
            Slot slotMusica = new Slot();
            Slot slotLibrosEnriquecidos = new Slot();
            Slot slotFragmentos = new Slot();
            Slot slotLibrosAgregados = new Slot(); // Salida del Agregator
            Slot slotMusicaTransformada = new Slot();
            Slot slotMusicaReplicada1 = new Slot();
            Slot slotMusicaReplicada2 = new Slot();
            Slot slotCorrelatorSalidaA = new Slot();
            Slot slotCorrelatorSalidaB = new Slot();
            Slot slotMergeSalida = new Slot();

            // Slots de salida final
            Slot slotSalidaFinalLibros = new Slot();
            Slot slotSalidaFinalMusica = new Slot();
            Slot slotSalidaFinalCorrelator = new Slot();

            // --- 2. DEFINIR PUERTOS DE ENTRADA ---
            PuertoEntrada pEntradaPedidos = new PuertoEntrada(slotEntradaPedidos);
            PuertoEntrada pEntradaContexto = new PuertoEntrada(slotEntradaContexto);
            PuertoEntrada pEntradaCorrelator = new PuertoEntrada(slotEntradaCorrelator);

            // --- 3. DEFINIR PUERTOS DE SALIDA ---
            PuertoSalida pSalidaLibros = new PuertoSalida(slotSalidaFinalLibros);
            PuertoSalida pSalidaMusica = new PuertoSalida(slotSalidaFinalMusica);
            PuertoSalida pSalidaCorrelator = new PuertoSalida(slotCorrelatorSalidaB); // Mostramos solo la B

            // --- 4. CONFIGURAR TODAS LAS TAREAS ---

            // ETAPA 1: FILTER (Filtra solo los pedidos con <valido>true</valido>)
            Filter filter = new Filter(slotEntradaPedidos, slotFiltrados);
            filter.setXpath("/pedido/valido[.='true']"); 

            // ETAPA 2: DISTRIBUTOR (Separa LIBRO de MUSICA)
            Distributor distributor = new Distributor(slotFiltrados, Arrays.asList(slotLibros, slotMusica));
            distributor.setXpath("/pedido/@tipo"); // Lee el atributo 'tipo'
            distributor.setElementosSegunOrden(Arrays.asList("LIBRO", "MUSICA"));

            // ETAPA 3 (Flujo Libros): ENRICHER
            ContextEnricher enricher = new ContextEnricher(Arrays.asList(slotLibros, slotEntradaContexto), List.of(slotLibrosEnriquecidos));
            enricher.setXPathPrincipal("/pedido/destino"); // Dónde insertar
            enricher.setXPathContexto("/contexto/dato_extra"); // Qué insertar
           

            // ETAPA 4 (Flujo Libros): SPLITTER
            Splitter splitter = new Splitter(List.of(slotLibrosEnriquecidos), List.of(slotFragmentos));
            splitter.setXPathExpression("/pedido/items_a_separar/item"); 

            // ETAPA 5 (Flujo Libros): AGREGATOR
            // ### LÍNEA CORREGIDA ###
            Agregator agregator = new Agregator(List.of(slotFragmentos), List.of(slotLibrosAgregados));

            // ETAPA 3 (Flujo Musica): TRANSLATOR
            Translator translator = new Translator(List.of(slotMusica), List.of(slotMusicaTransformada));
            translator.setRutaXSLT("archivos/transformar_musica.xslt");

            // ETAPA 4 (Flujo Musica): REPLICATOR
            Replicator replicator = new Replicator(slotMusicaTransformada, Arrays.asList(slotMusicaReplicada1, slotMusicaReplicada2));

            // ETAPA 5 (Flujo Mixto): CORRELATOR
            Correlator correlator = new Correlator(Arrays.asList(slotLibrosAgregados, slotEntradaCorrelator), Arrays.asList(slotCorrelatorSalidaA, slotCorrelatorSalidaB));
            
            // ETAPA 6 (Flujo Mixto): MERGE
            Merge merge = new Merge(Arrays.asList(slotMusicaReplicada1, slotMusicaReplicada2), slotMergeSalida);

            // ETAPA 7: Conexión a Salidas Finales
            Merge mergeLibros = new Merge(List.of(slotCorrelatorSalidaA), slotSalidaFinalLibros);
            Merge mergeMusica = new Merge(List.of(slotMergeSalida), slotSalidaFinalMusica);

            // --- 5. CARGAR LOS ARCHIVOS EN LOS PUERTOS DE ENTRADA ---
            System.out.println("\n---Cargando archivos ---");
            // Cargamos el contexto PRIMERO
            try {
                pEntradaContexto.setDocument(parseXmlFile("archivos/datos_contexto.xml"));
                pEntradaContexto.execute();
                System.out.println("... Contexto cargado en 'slotContexto'");
            } catch (Exception e) { System.err.println("Error cargando contexto: " + e.getMessage()); }

            // Cargamos la factura para el correlator PRIMERO
             try {
                pEntradaCorrelator.setDocument(parseXmlFile("archivos/datos_correlator_100.xml"));
                pEntradaCorrelator.execute();
                System.out.println("... Factura cargada en 'slotEntradaCorrelator'");
            } catch (Exception e) { System.err.println("Error cargando correlator: " + e.getMessage()); }

            // Cargamos los pedidos principales (los de los argumentos)
            for (String rutaArchivo : args) {
                try {
                    System.out.println("... Leyendo archivo: " + rutaArchivo);
                    Document doc = parseXmlFile(rutaArchivo);
                    pEntradaPedidos.setDocument(doc);
                    pEntradaPedidos.execute(); // Inyecta el documento
                } catch (Exception e) {
                    System.err.println("No se pudo parsear " + rutaArchivo + ": " + e.getMessage());
                }
            }
            System.out.println("Mensajes en 'slotEntradaPedidos': " + slotEntradaPedidos.getQueueSize());


            // --- 6. EJECUTAR EL MOTOR DE INTEGRACIÓN (Bucle) ---
            System.out.println("\n---Iniciando Motor de Tareas ---");
            
            int iteraciones = 0;
            // Lista de todos los slots "intermedios"
            List<Slot> todosLosSlots = Arrays.asList(
                slotEntradaPedidos, slotEntradaContexto, slotEntradaCorrelator, slotFiltrados,
                slotLibros, slotMusica, slotLibrosEnriquecidos, slotFragmentos, slotLibrosAgregados,
                slotMusicaTransformada, slotMusicaReplicada1, slotMusicaReplicada2,
                slotCorrelatorSalidaA, slotCorrelatorSalidaB, slotMergeSalida,
                slotSalidaFinalLibros, slotSalidaFinalMusica
            );

            // Bucle principal: se ejecuta mientras CUALQUIER slot tenga mensajes
            while (iteraciones < 100) { // Límite de seguridad
                boolean hayMensajes = false;
                for (Slot s : todosLosSlots) {
                    if (!s.isEmptyQueue()) {
                        hayMensajes = true;
                        break;
                    }
                }
                if (!hayMensajes) {
                    System.out.println("--- Motor detenido: No hay más mensajes ---");
                    break;
                }

                // Ejecutamos todas las tareas en orden
                filter.execute();
                distributor.execute();
                enricher.execute();
                splitter.execute();
                agregator.execute();
                translator.execute();
                replicator.execute();
                correlator.execute();
                merge.execute();
                
                // Conectores finales a Salidas
                mergeLibros.execute();
                mergeMusica.execute();

                // Ejecutamos los puertos de salida
                pSalidaLibros.execute();
                pSalidaMusica.execute();
                pSalidaCorrelator.execute();

                iteraciones++;
            }
            
            if (iteraciones == 100) {
                System.err.println("--- MOTOR DETENIDO: Límite de iteraciones alcanzado (posible bucle infinito) ---");
            }

            // --- 7. RESULTADOS ---
            System.out.println("\n---Flujo completado en " + iteraciones + " iteraciones ---");
            // Verificamos que los slots finales (que no sean de salida) estén vacíos
            System.out.println("Mensajes restantes en 'slotFiltrados' (debe ser 0): " + slotFiltrados.getQueueSize());
            System.out.println("Mensajes restantes en 'slotFragmentos' (debe ser 0): " + slotFragmentos.getQueueSize());

        } catch (Exception e) {
            System.out.println("\n---Ha ocurrido un error fatal en el main ---");
            e.printStackTrace();
        }*/
    System.out.println("\n--- Probando ConectorSQL (con Maven y AAD) ---");

    // --- 1. Configuración de la Base de Datos ---
    
    // Esta es la URL mágica. 
    // "ActiveDirectoryDefault" buscará tu credencial de Azure (de 'az login')
    String connectionUrl = "jdbc:sqlserver://integracion.database.windows.net:1433;" +
                         "database=bebidas;" +
                         "encrypt=true;" +
                         "trustServerCertificate=true;" +
                         "authentication=ActiveDirectoryDefault;"; 

    // --- 2. Cableado de Componentes ---
    Slot slotPeticion = new Slot();
    Slot slotRespuesta = new Slot();
    PuertoSolicitante puertoSQL = new PuertoSolicitante(slotPeticion, slotRespuesta);

    // ¡Llama al constructor SIN contraseña!
    ConectorSolicitudDB conectorDB = new ConectorSolicitudDB(puertoSQL, connectionUrl);

    // --- 3. Simulación de Petición (Request) ---
    try {
        String xmlPeticion = "<peticion><sql>SELECT * FROM bebidas</sql></peticion>"; 
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document docPeticion = builder.parse(new InputSource(new StringReader(xmlPeticion)));

        Head headPeticion = new Head();
        headPeticion.setIdUnico(Principal.IdUnico.getInstance().getIdUnico());
        Mensaje msgPeticion = new Mensaje(headPeticion, docPeticion);

        slotPeticion.enqueue(msgPeticion);
        System.out.println("Mensajes en la cola de PETICIÓN: " + slotPeticion.getQueueSize());

        // --- 4. Simulación del Procesamiento ---
        conectorDB.execute(); 
        puertoSQL.execute();  

        // --- 5. Verificación de Respuesta ---
        System.out.println("Mensajes en la cola de PETICIÓN (debe ser 0): " + slotPeticion.getQueueSize());
        System.out.println("Mensajes en la cola de RESPUESTA (debe ser 1): " + slotRespuesta.getQueueSize());

        if (!slotRespuesta.isEmptyQueue()) {
            Mensaje msgRespuesta = slotRespuesta.dequeuePoll();
            Document docRespuesta = msgRespuesta.getBody();
            String nodoRaiz = docRespuesta.getDocumentElement().getTagName();
            System.out.println("Respuesta recibida. Nodo raíz: " + nodoRaiz);
        }

    } catch (Exception e) {
        System.out.println("Error en la prueba del ConectorSQL: " + e.getMessage());
        e.printStackTrace();
    }
    }
}