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

public class Distributor extends TareaBase {
	
    private List<String> elementosSegunOrden;
    private String xpath;

     public Distributor(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);

			// VALIDACIÓN (Importante): Protegemos la lógica interna
			if (entradas.size() != 1) {
				throw new IllegalArgumentException("Distributor requiere exactamente 1 slot de entrada");
			}

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

                String tipo = items.item(0).getTextContent();

                for (int i = 0; i < elementosSegunOrden.size(); i++) {
                    if (elementosSegunOrden.get(i).equals(tipo)) {
                        salidas.get(i).enqueue(mensaje);
                        break;
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            //System.out.println("Distributor: No hay mensajes en la cola de entrada");
        }
    }

    public void setElementosSegunOrden(List<String> elementosSegunOrden) {
        this.elementosSegunOrden = elementosSegunOrden;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }
}
