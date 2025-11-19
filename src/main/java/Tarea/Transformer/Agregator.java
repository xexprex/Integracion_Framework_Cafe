package Tarea.Transformer;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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

/**
 * Implementación Agregador.
 * 
 * Este componente es el opuesto al Splitter. Su función es recibir una serie de mensajes
 * individuales (fragmentos) que pertenecen a un mismo grupo lógico y combinarlos en un único mensaje compuesto.
 * 
 * Lógica de funcionamiento:
 * 1. Espera a recibir mensajes en su cola de entrada.
 * 2. Cuando llega un mensaje, verifica si el grupo al que pertenece está completo.
 * 3. Si está completo, recupera el "esqueleto" original desde el Diccionario.
 * 4. Inyecta todos los fragmentos recibidos dentro del esqueleto.
 * 5. Envía el mensaje reconstruido a la salida y elimina los fragmentos de la cola.
 */
public class Agregator extends TareaBase {
    /**
     * Constructor del Agregator.
     * @param entradas  Lista de slots de entrada (unicamente 1).
     * @param salidas   Lista de slots de salida (unicamente 1).
     */
    public Agregator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    @Override
    public void execute() {
        // Verifica Si no hay nada que procesar.
        if (entradas.getFirst().isEmptyQueue()) {
            return;
        }

        List<Mensaje> mensajes = entradas.getFirst().getQueue();         //* Obtenemos una copia de la cola para iterar sin modificar 
        boolean terminado = false;                                       
        int indexGlobal = 0;

        while (!terminado && indexGlobal < mensajes.size()) {                               //* Iteramos sobre los mensajes pendientes. 
            Mensaje mensaje = mensajes.get(indexGlobal);                                    
            Head head = mensaje.getHead();  

            Utilidad utilidad = new Utilidad(entradas.getFirst());                          //* Usamos la clase utilidad para buscar en la cola todos los mensajes
            List<Mensaje> aux = utilidad.getMessagesByIdSecuencia(head.getIdSecuencia());   //* que tengan el mismo ID de secuencia y agrupa los fragmentos.

            if (aux.size() == head.getTotalSecuencia()) {                                   //* Verifica si la cantridad de mensajes encontrados coincide con el total.
                    Diccionario diccionario = Diccionario.getInstance();                    //* Si encuentra todos los mensajes quiere decir que el grupo esta completo
                    ValoresDiccionario vd = diccionario.get(head.getIdSecuencia());         //* por tanto recuperamos el esqueleto XML original que el splitter guardo.
                    Document context = (Document) vd.getContext();                          //*

                    try {
                        XPathFactory xPathFactory = XPathFactory.newInstance();             //* Preparamos XPATH para encontrar un punto de entrada en el esqueleto. 
                        XPath xPath = xPathFactory.newXPath();
                        XPathExpression expr = xPath.compile(vd.getxPathExpression());
                        NodeList items = (NodeList) expr.evaluate(context, XPathConstants.NODESET); //* Localizamos el nodo padre donde se va a pegar los hijos.

                        for (Mensaje m : aux) {                                             //* Iteramos sobre cada fragmento recuperado.
                            Document docAux = m.getBody();                                  //* Lo pegamos en el esqueleto.
                            Node node = docAux.getDocumentElement();                        //* y eliminamos el fragmento individual de la cola de entrada porque
                            Node importedNode = context.importNode(node, true);       //* este ya ha sido procesado.
                            items.item(0).appendChild(importedNode);
                            entradas.getFirst().removeByMessage(m);
                        }

                        terminado = true;                                                   //* Marcamos como terminado para salir del bucle principal.

                        mensaje.setBody(context);                                           //* Actualizamos el cuerpo del mensaje con elodocumento completo reconstruido
                        mensaje.getHead().setNumSecuencia(0);                  //* reseteamos el contador de secuencia.
                        salidas.getFirst().enqueue(mensaje);                                //* y enviamos el mensaje final unificado.

                    } catch (XPathExpressionException e) {
                        System.out.println("Error al ejecutar Aggregator: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                indexGlobal++;
            }
    }
}

