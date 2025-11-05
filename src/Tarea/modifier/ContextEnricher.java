package Tarea.modifier;

import java.util.List;

import javax.xml.xpath.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class ContextEnricher extends TareaBase {

    private String xPathPrincipal;
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
        if (entradas.get(0).isEmptyQueue() || entradas.get(1).isEmptyQueue()) {
            return;
        }

        if (xPathPrincipal == null || xPathContexto == null) {
            System.out.println("ContextEnricher: XPath no configurado.");
            return;
        }

        try {
            Mensaje mensajePrincipal = entradas.get(0).dequeuePoll();
            Mensaje mensajeContexto = entradas.get(1).dequeuePoll();

            Document docPrincipal = mensajePrincipal.getBody();
            Document docContexto = mensajeContexto.getBody();

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            XPathExpression exprContexto = xPath.compile(xPathContexto);
            Node nodoContexto = (Node) exprContexto.evaluate(docContexto, XPathConstants.NODE);

            XPathExpression exprPrincipal = xPath.compile(xPathPrincipal);
            Node nodoPrincipal = (Node) exprPrincipal.evaluate(docPrincipal, XPathConstants.NODE);

            if (nodoContexto != null && nodoPrincipal != null) {
                Node importedNode = docPrincipal.importNode(nodoContexto, true);
                nodoPrincipal.appendChild(importedNode);
            }

            salidas.get(0).enqueue(mensajePrincipal);

        } catch (XPathExpressionException e) {
            System.out.println("Error en ContextEnricher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}