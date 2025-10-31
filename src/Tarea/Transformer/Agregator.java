package Tarea.Transformer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.List;


public class Aggregator extends Tarea {

	public Aggregator(Slot entrada, Slot salida) {
        super(List.of(entrada), List.of(salida));
    }

    @Override
    public void execute() {
        Slot input = entradas.getFirst();
        Slot output = salidas.getFirst();

        if (input.isEmptyQueue()) {
            System.out.println("Aggregator: No hay mensajes en la cola de entrada");
            return;
        }

        List<Message> mensajes = input.getQueue();
        Utilidad util = new Utilidad(input);

        for (Message mensaje : new ArrayList<>(mensajes)) {
            Head head = mensaje.getHead();
            List<Message> secuencia = util.getMessagesByIdSecuencia(head.getIdSecuencia());

            if (secuencia.size() != head.getTotalSecuencia()) continue;

            try {
                Message agregado = combinarMensajes(secuencia, head);
                secuencia.forEach(input::removeByMessage);
                output.enqueue(agregado);
                break; // solo procesa una secuencia por ejecución
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Message combinarMensajes(List<Message> mensajes, Head head) throws XPathExpressionException {
        Diccionario diccionario = Diccionario.getInstance();
        ValoresDiccionario vd = diccionario.get(head.getIdSecuencia());
        Document context = vd.getContext();

        XPathExpression expr = XPathFactory.newInstance()
                                           .newXPath()
                                           .compile(vd.getxPathExpression());
        NodeList items = (NodeList) expr.evaluate(context, XPathConstants.NODESET);

        for (Message m : mensajes) {
            Node node = m.getBody().getDocumentElement();
            Node imported = context.importNode(node, true);
            items.item(0).appendChild(imported);
        }

        Message resultado = mensajes.getFirst();
        resultado.setBody(context);
        resultado.getHead().setNumSecuencia(0);
        return resultado;
    }
}

}

