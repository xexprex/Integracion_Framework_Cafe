package Tarea.Transformer;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class Translator extends TareaBase {

	private String rutaXSLT;
	
	/**
     * Constructor del Translator.
     * @param entradas 	Lista de slots de entrada (típicamente 1).
     * @param salidas 	Lista de slots de salida (típicamente 1).
     */
    public Translator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }
	 /**
     * Configura el archivo de hoja de estilos que se usará para la transformación.
     * @param rutaXSLT 	Ruta relativa o absoluta al archivo .xslt
     */
    public void setRutaXSLT(String rutaXSLT) {
        this.rutaXSLT = rutaXSLT;
    }

     @Override
    public void execute() {
        // Verificación de guardia: Si no hay mensajes pendientes, salimos para ahorrar procesamiento.
        if(entradas.getFirst().isEmptyQueue()) {
            // System.out.println("Translator: No hay mensaje en la cola de entrada.");
            return;
        }
        
        try {
            // Recuperamos el mensaje de la cola y obtenemos su contenido XML actual.
            Mensaje mensaje = entradas.getFirst().dequeuePoll();

            // Creamos una instancia de Transformer cargando el archivo .xslt configurado.
            // Este objeto contiene las "reglas" de cómo convertir el XML A en XML B.
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(new StreamSource(new File(rutaXSLT)));

            // Necesitamos un documento en blanco donde el Transformer escribirá el resultado.
            Document output = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();

            // - DOMSource: Es el XML original (Input)
            // - DOMResult: Es el documento vacío (Output) donde se volcará el resultado
            transformer.transform(new DOMSource(mensaje.getBody()), new DOMResult(output));

            // Reemplazamos el cuerpo del mensaje original con el nuevo documento transformado.
            // Nota: Mantenemos el mismo objeto 'Mensaje' (y sus cabeceras/IDs), solo cambiamos el Body.
            mensaje.setBody(output);
            
            // Colocamos el mensaje transformado en la cola de salida.
            salidas.getFirst().enqueue(mensaje);

        } catch (Exception e) {
            System.err.println("Error crítico en Translator (" + rutaXSLT + "):");
            e.printStackTrace();
        }
    }
}