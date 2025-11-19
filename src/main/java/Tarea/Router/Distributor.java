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

// Recibe un mensaje y decide a qué canal de salida enviarlo,
// basándose en un valor específico dentro del XML

public class Distributor extends TareaBase {
	
    // Lista de las palabras clave
    // El orden debe coincidir con el orden de los Slots en la lista 'salidas'.
    // Ejemplo: Si index 0 es "cold", el Slot en salidas.get(0) debe ser el canal de bebidas frías.
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
            // Extraemos el mensaje de la cola para procesarlo
            Mensaje mensaje = entradas.getFirst().dequeuePoll();
            Document doc = mensaje.getBody();

            try {
                
                // Configuramos el motor XPath para buscar dentro del XML del mensaje
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();
                // Compilamos la ruta ej: "/drink/type"
                XPathExpression expr = xPath.compile(xpath);
                // Hacemos la busqueda en el documento
                NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                
                // Obtenemos el valor del texto (ej: "cold" o "hot")
                // Asumimos que el XML es valido y el nodo existe (item(0))
                String tipo = items.item(0).getTextContent();

                // Recorremos nuestra lista de criterios para ver donde encaja este mensaje
                for (int i = 0; i < elementosSegunOrden.size(); i++) {
                    // Si el tipo encontrado en el XML coincide con el criterio actual,
                    // enviamos el mensaje al Slot de salida correspondiente al mismo índice.

                    if (elementosSegunOrden.get(i).equals(tipo)) {
                        salidas.get(i).enqueue(mensaje);
                        // Rompemos el bucle una vez enrutado.
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
