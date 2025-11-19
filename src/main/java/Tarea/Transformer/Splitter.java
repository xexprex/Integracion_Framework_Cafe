package Tarea.Transformer;

import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import Principal.Diccionario;
import Principal.Head;
import Principal.IdUnico;
import Principal.Mensaje;
import Principal.Slot;
import Principal.ValoresDiccionario;
import Tarea.TareaBase;



public class Splitter extends TareaBase {

	   private String xPathExpression;
	    private String idXML;

		public Splitter(Slot entrada, Slot salida) {
			super(List.of(entrada), List.of(salida));

			// VALIDACIÓN (Importante): Protegemos la lógica interna
			if (entradas.size() != 1) {
				throw new IllegalArgumentException("Splitter requiere exactamente 1 slot de entrada");
			}
			if (salidas.size() != 1) {
				throw new IllegalArgumentException("Splitter requiere exactamente 1 slot de salida");
			}
		}

	    @Override
	    public void execute() {
	        if (!entradas.getFirst().isEmptyQueue()) {
	            idXML = UUID.randomUUID().toString();

	            Mensaje mensaje = entradas.getFirst().dequeuePoll();
	            Document doc = mensaje.getBody();

	            try {
	                XPathFactory xPathFactory = XPathFactory.newInstance();
	                XPath xPath = xPathFactory.newXPath();
	                XPathExpression expr = xPath.compile(xPathExpression);

	                NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

	                String xp = "//"+items.item(0).getParentNode().getNodeName();

	                for (int i = 0; i < items.getLength(); i++) {
	                    Node item = items.item(i);
	                    item.getParentNode().removeChild(item);
	                }

	                ValoresDiccionario vD = new ValoresDiccionario(xp, doc);
	                Diccionario diccionario = Diccionario.getInstance();
	                diccionario.put(idXML, vD);

	                for (int i = 0; i < items.getLength(); i++) {
	                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	                    Document newDoc = docBuilder.newDocument();

	                    Node item = items.item(i);
	                    Node importedItem = newDoc.importNode(item, true);
	                    newDoc.appendChild(importedItem);

	                    Head headAux = new Head(0, idXML, (i+1), items.getLength());
	                    headAux.setIdUnico(IdUnico.getInstance().getIdUnico());
	                    Mensaje mensajeAux = new Mensaje(headAux, newDoc);

	                    salidas.getFirst().enqueue(mensajeAux);
	                }

	            } catch (Exception e) {
	                System.out.println("Error al ejecutar el splitter " + e.getMessage());
	            }
	        } else {
	            //System.out.println("Splitter: No hay mensajes en la cola de entrada");
	        }
	    }

	    public void setXPathExpression(String xPathExpression) {
	        this.xPathExpression = xPathExpression;
	    }
	}