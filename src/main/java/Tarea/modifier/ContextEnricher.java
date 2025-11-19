package Tarea.modifier;

import java.util.List;

import javax.xml.xpath.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class ContextEnricher extends TareaBase {

    // Mensaje principal
    private String xPathPrincipal;

    // Información a extraer del mensaje de contexto
    private String xPathContexto;

    public ContextEnricher(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    public void setXPathPrincipal(String xPathPrincipal) {
        this.xPathPrincipal = xPathPrincipal;
    }

    public void setXPathContexto(String xPathContexto) {
        this.xPathContexto = xPathContexto;
    }

    @Override
    public void execute() {
        // 1. VALIDACIÓN DE PRECONDICIONES
        // Si el Correlator aún no ha puesto ambos mensajes en las colas, no podemos hacer nada
        if (entradas.get(0).isEmptyQueue() || entradas.get(1).isEmptyQueue()) {
            return;
        }

        if (xPathPrincipal == null || xPathContexto == null) {
            System.out.println("ContextEnricher: XPath no configurado.");
            return;
        }

        try {
            // 2. RECUPERACIÓN DE MENSAJES
            // Extraemos los mensajes de las colas y desaparecen del slot
            Mensaje mensajePrincipal = entradas.get(0).dequeuePoll();
            Mensaje mensajeContexto = entradas.get(1).dequeuePoll();

            // Obtenemos los cuerpos XML de ambos mensajes
            Document docPrincipal = mensajePrincipal.getBody();
            Document docContexto = mensajeContexto.getBody();

            // 3. PREPARACIÓN DEL MOTOR XPATH
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 4. EXTRACCIÓN DEL NODO DE CONTEXTO
            // Buscamos en el XML del Barman el nodo que nos interesa (ej. la <fila> con el precio).
            XPathExpression exprContexto = xPath.compile(xPathContexto);
            Node nodoContexto = (Node) exprContexto.evaluate(docContexto, XPathConstants.NODE);

            // 5. LOCALIZACIÓN DEL NODO PRINCIPAL
            // Buscamos en el XML de la bebida el lugar donde vamos a poner la información nueva.
            XPathExpression exprPrincipal = xPath.compile(xPathPrincipal);
            Node nodoPrincipal = (Node) exprPrincipal.evaluate(docPrincipal, XPathConstants.NODE);

            // 6. ENRIQUECIMIENTO
            if (nodoContexto != null && nodoPrincipal != null) {
                // No se puede pegar un nodo de 'docContexto' directamente en 'docPrincipal'
                // Así que lo importamos al documento destino y con deep: true compiamos el nodo y los hijos.
                
                Node importedNode = docPrincipal.importNode(nodoContexto, true);
                
                nodoPrincipal.appendChild(importedNode);
            }
            // 7. ENVÍO A LA SALIDA
            // Por ultimo colocamos el mensaje principal con los datos inyectados en la salida.
            salidas.get(0).enqueue(mensajePrincipal);

        } catch (XPathExpressionException e) {
            System.out.println("Error en ContextEnricher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}