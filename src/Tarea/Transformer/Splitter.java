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

import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;


public class Splitter extends TareaBase {

    private String xPathExpression;
    private String idXML;

    public Splitter(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    @Override
    public void execute() {
        if (entradas.getFirst().isEmptyQueue()) {
            System.out.println("Splitter: No hay mensajes en la cola de entrada.");
            return;
        }

        try {
            idXML = UUID.randomUUID().toString();

            Mensaje mensaje = entradas.getFirst().dequeuePoll();
            Document doc = mensaje.getBody();

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expr = xPath.compile(xPathExpression);

            NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (items == null || items.getLength() == 0) {
                System.out.println("Splitter: No se encontraron nodos con la expresión XPath.");
                return;
            }

            // Guarda el documento original sin los nodos extraídos
            String xp = "//" + items.item(0).getParentNode().getNodeName();
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                item.getParentNode().removeChild(item);
            }

            //ValoresDiccionario vD = new ValoresDiccionario(xp, doc);
            //Diccionario diccionario = Diccionario.getInstance();
            //diccionario.put(idXML, vD);

            // Crea un nuevo mensaje por cada nodo separado
            for (int i = 0; i < items.getLength(); i++) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document newDoc = docBuilder.newDocument();

                Node item = items.item(i);
                Node importedItem = newDoc.importNode(item, true);
                newDoc.appendChild(importedItem);

                //Head headAux = new Head(0, idXML, i + 1, items.getLength());
                //headAux.setIdUnico(IdUnico.getInstance().getIdUnico());
                //Mensaje mensajeAux = new Mensaje(headAux, newDoc);

                salidas.getFirst().enqueue(mensajeAux);
            }

        } catch (Exception e) {
            System.out.println("Error al ejecutar Splitter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setXPathExpression(String xPathExpression) {
        this.xPathExpression = xPathExpression;
    }
}