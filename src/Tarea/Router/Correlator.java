package Tarea.Router;

import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import Principal.Mensaje;
import Principal.Slot;
import Principal.Utilidad;
import Tarea.TareaBase;

public class Correlator extends TareaBase {

	private List<String> xPaths;

    public Correlator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    public void setxPaths(List<String> xPaths) {
        this.xPaths = xPaths;
    }

    @Override
    public void execute() {

        int index = 0;
        boolean founded = false;
        List<Mensaje> messages = entradas.getFirst().getQueue();

        while (!founded && index < messages.size()) {

        	Mensaje msg = messages.get(index);

            try {
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();
                XPathExpression expr = xPath.compile(xPaths.getFirst());
                NodeList items = (NodeList) expr.evaluate(msg.getBody(), XPathConstants.NODESET);

                List<Mensaje> aux = new LinkedList<>();
                aux.add(msg);

                String rE = items.item(0).getTextContent();

                for (int i = 1; i < entradas.size(); i++) {
                    Utilidad utilidad = new Utilidad(entradas.get(i));
                    Mensaje messageFor = utilidad.getMessagesByXPathAndResult(xPaths.get(i), rE);

                    if(messageFor != null)
                        aux.add(messageFor);
                    else
                        break;
                }

                if(aux.size() == entradas.size()){
                    founded=true;

                    for(int i=0; i< aux.size(); i++){
                        entradas.get(i).removeByMessage(aux.get(i));
                        salidas.get(i).enqueue(aux.get(i));
                    }
                }

                index++;

            } catch (Exception e) {
                System.out.println("Error al ejecutar el correlator " + e.getMessage());
            }
        }
    }

}