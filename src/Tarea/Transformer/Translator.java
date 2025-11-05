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
	
    public Translator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }
    public void setRutaXSLT(String rutaXSLT) {
        this.rutaXSLT = rutaXSLT;
    }

    @Override
    public void execute() {
    	if(entradas.getFirst().isEmptyQueue()) {
    		System.out.println("Translator: No hay mensaje en la cola de entrada.");
    		return;
    	}
    	
    	
    	try {
    	    Mensaje mensaje = entradas.getFirst().dequeuePoll();

    	    Transformer transformer = TransformerFactory.newInstance()
    	            .newTransformer(new StreamSource(new File(rutaXSLT)));

    	    Document output = DocumentBuilderFactory.newInstance()
    	            .newDocumentBuilder().newDocument();

    	    transformer.transform(new DOMSource(mensaje.getBody()), new DOMResult(output));

    	    mensaje.setBody(output);
    	    salidas.getFirst().enqueue(mensaje);

    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    }
}

