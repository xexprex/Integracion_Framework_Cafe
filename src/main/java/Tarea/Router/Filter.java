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
	        if(!entradas.getFirst().isEmptyQueue()){
	            Mensaje mensaje = entradas.getFirst().dequeuePoll();
	            Document doc = mensaje.getBody();

	            try {
	                XPathFactory xPathFactory = XPathFactory.newInstance();
	                XPath xPath = xPathFactory.newXPath();
	                XPathExpression expr = xPath.compile(xpath);
	                NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

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
