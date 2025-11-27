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
        // Verificación rápida: Si no hay nada que procesar, salimos.
        if (entradas.getFirst().isEmptyQueue()) {
            return;
        }

        // Obtenemos una copia de la cola para iterar sin modificarla concurrentemente (aunque aquí es single-thread)
        List<Mensaje> mensajes = entradas.getFirst().getQueue();
        boolean terminado = false;
        int indexGlobal = 0;

        // Iteramos sobre los mensajes pendientes para ver si alguno completa una secuencia
        while (!terminado && indexGlobal < mensajes.size()) {
            Mensaje mensaje = mensajes.get(indexGlobal);
            Head head = mensaje.getHead();

            // Usamos una utilidad para buscar en la cola TODOS los mensajes que tengan el mismo ID de Secuencia.
            // Esto agrupa los fragmentos dispersos que pueden haber llegado desordenados.
            Utilidad utilidad = new Utilidad(entradas.getFirst());
            List<Mensaje> aux = utilidad.getMessagesByIdSecuencia(head.getIdSecuencia());

            // Verificamos si la cantidad de mensajes encontrados coincide con el 'totalSecuencia' esperado.
            // Si faltan mensajes, ignoramos este grupo por ahora y seguimos esperando.
            if (aux.size() == head.getTotalSecuencia()) {
                
                // El grupo está completo. Recuperamos el "esqueleto" XML original que el Splitter guardó.
                Diccionario diccionario = Diccionario.getInstance();
                ValoresDiccionario vd = diccionario.get(head.getIdSecuencia());
                Document context = (Document) vd.getContext();

                try {
                    // Preparamos XPath para encontrar el punto de inserción en el esqueleto
                    XPathFactory xPathFactory = XPathFactory.newInstance();
                    XPath xPath = xPathFactory.newXPath();
                    XPathExpression expr = xPath.compile(vd.getxPathExpression());
                    
                    // Localizamos el nodo padre donde se deben pegar los hijos (ej: <Pedido>)
                    NodeList items = (NodeList) expr.evaluate(context, XPathConstants.NODESET);

                    // Iteramos sobre cada fragmento recuperado.
                    for (Mensaje m : aux) {
                        Document docAux = m.getBody();
                        Node node = docAux.getDocumentElement(); // La raíz del fragmento (ej: <item>)
                        
                        // Importamos el nodo al documento contexto (necesario porque vienen de Docs distintos)
                        Node importedNode = context.importNode(node, true);
                        
                        // Lo pegamos en el esqueleto
                        items.item(0).appendChild(importedNode);
                        
                        // Eliminamos el fragmento individual de la cola de entrada, ya que ha sido procesado.
                        entradas.getFirst().removeByMessage(m);
                    }

                    // Marcamos como terminado para salir del bucle principal y no procesar el mismo grupo dos veces
                    terminado = true;
                    
                    // Actualizamos el cuerpo del mensaje con el documento completo reconstruido.
                    mensaje.setBody(context);
                    
                    // Reseteamos el contador de secuencia indicando que ya no es un fragmento.
                    mensaje.getHead().setNumSecuencia(0);
                    
                    // Enviamos el mensaje final unificado.
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
