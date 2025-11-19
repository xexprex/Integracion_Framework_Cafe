package Tarea.Router;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class Filter extends TareaBase{
	
	   private String xpath;

	    public Filter(Slot entrada, Slot salida) {
	        super(List.of(entrada), List.of(salida));
	    }

	    public void setXpath(String xpath) {
	        this.xpath = xpath;
	    }

	    @Override
	    public void execute() {
			// 1. Verificación inicial: Procesamos solo si hay mensajes pendientes en la entrada.
	        if(!entradas.getFirst().isEmptyQueue()){
				// 2. Recuperación: Extraemos el mensaje y su contenido XML para analizarlo.
	            Mensaje mensaje = entradas.getFirst().dequeuePoll();
	            Document doc = mensaje.getBody();

	            try {
					// 3. Preparación XPath: Configuramos el motor para evaluar la expresión de filtro.
	                XPathFactory xPathFactory = XPathFactory.newInstance();
	                XPath xPath = xPathFactory.newXPath();
	                XPathExpression expr = xPath.compile(xpath);
	                NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

					// 4. Lógica de Filtrado: 
                	// El mensaje pasa solo si el nodo buscado existe (no es null) y tiene contenido (no está vacío).
	                if(items.item(0) != null && !items.item(0).getTextContent().isEmpty()){
	                    salidas.getFirst().enqueue(mensaje);
	                }

	            } catch (Exception e) {
	                throw new RuntimeException(e);
	            }

	        } else {
	            //System.out.println("Filter: no hay mensajes en la entrada");
	        }

	    }
	}
