package Tarea.Transformer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import Principal.Diccionario;
import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Principal.Utilidad;
import Principal.ValoresDiccionario;
import Tarea.TareaBase;

import javax.xml.xpath.*;
import java.util.List;


public class Agregator extends TareaBase {

    public Agregator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    @Override
    public void execute() {
        if (entradas.getFirst().isEmptyQueue()) {
            System.out.println("Aggregator: No hay mensajes en la cola de entrada.");
            return;
        }

        List<Mensaje> mensajes = entradas.getFirst().getQueue();
        boolean terminado = false;
        int indexGlobal = 0;

        while (!terminado && indexGlobal < mensajes.size()) {
            Mensaje mensaje = mensajes.get(indexGlobal);
            Head head = mensaje.getHead();

            Utilidad utilidad = new Utilidad(entradas.getFirst());
            List<Mensaje> aux = utilidad.getMessagesByIdSecuencia(head.getIdSecuencia());

            // Verifica si ya llegaron todos los fragmentos
            if (aux.size() == head.getTotalSecuencia()) {
                Diccionario diccionario = Diccionario.getInstance();
                ValoresDiccionario vd = diccionario.get(head.getIdSecuencia());
                Document context = (Document) vd.getContext();

                try {
                    XPathFactory xPathFactory = XPathFactory.newInstance();
                    XPath xPath = xPathFactory.newXPath();
                    XPathExpression expr = xPath.compile(vd.getxPathExpression());
                    NodeList items = (NodeList) expr.evaluate(context, XPathConstants.NODESET);

                    for (Mensaje m : aux) {
                        Document docAux = m.getBody();
                        Node node = docAux.getDocumentElement();
                        Node importedNode = context.importNode(node, true);
                        items.item(0).appendChild(importedNode);
                        entradas.getFirst().removeByMessage(m);
                    }

                    terminado = true;

                    mensaje.setBody(context);
                    mensaje.getHead().setNumSecuencia(0);
                    salidas.getFirst().enqueue(mensaje);

                } catch (XPathExpressionException e) {
                    System.out.println("Error al ejecutar Aggregator: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            indexGlobal++;
        }
    }
}

