package Principal;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import java.util.LinkedList;
import java.util.List;

public class Utilidad {

    private Slot slot;

    public Utilidad(Slot slot){
        this.slot = slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public Mensaje getMessagesByXPathAndResult(String xpath, String resultadoEsperado){
        List<Mensaje> mensajes = slot.getQueue();

        for(Mensaje mensaje :  mensajes){
            try {
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();
                XPathExpression expr = xPath.compile(xpath);
                NodeList items = (NodeList) expr.evaluate(mensaje.getBody(), XPathConstants.NODESET);

                if(items.item(0).getTextContent().equals(resultadoEsperado)){
                    return mensaje;
                }
            }catch (Exception e){
                System.out.println("Error en utilidad: "+e.getMessage());
            }
        }
        return null;
    }

    public List<Mensaje> getMessagesByIdSecuencia(String idSecuencia){
        List<Mensaje> mensajes = slot.getQueue();
        List<Mensaje> aux = new LinkedList<>();

        for(Mensaje mensaje :  mensajes){
            Head head = mensaje.getHead();
            if(head.getIdSecuencia().equals(idSecuencia)){
                aux.add(mensaje);
            }
        }

        return aux;
    }
}
