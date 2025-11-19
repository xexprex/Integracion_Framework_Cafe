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
    	if(entradas.getFirst().isEmptyQueue()) {
    		return;
    	}
    	try {
    	    Mensaje mensaje = entradas.getFirst().dequeuePoll();							//* Recupera el mensaje de la cola y obtenemos su contenido XML actual
    	    Transformer transformer = TransformerFactory.newInstance()						//* Creamos una instancia para cargar el archivo .XSLT configurado,
			.newTransformer(new StreamSource(new File(rutaXSLT)));							//* ese archivo contiene las reglas de como convertir el XML de uno a otro.

		
    	    Document output = DocumentBuilderFactory.newInstance()							//* Creamos un documento en blanco donde el traslador 
			.newDocumentBuilder().newDocument();											//* escribira el resultado.

    	    transformer.transform(new DOMSource(mensaje.getBody()), new DOMResult(output));	//* DOMSource: Es el XML original (El de entrada).
																							//* DOMResult: Es el documento vacio (El de salida), ahí va los resultados.
			
			mensaje.setBody(output);														//* Remplaza el cuerpo del mensaje original con el nuevo documento.
    	    salidas.getFirst().enqueue(mensaje);											//* Por ultimo colocamos el mensaje transformador en la cola de salida.

    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    }
}

