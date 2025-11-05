package Tarea.Transformer;

import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import Principal.Diccionario;
import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Principal.ValoresDiccionario;
import Tarea.TareaBase;


public class Splitter extends TareaBase {

    // Expresión XPath usada para seleccionar los nodos del XML que se van a separar
    private String xPathExpression;

    // Identificador único del XML original (para poder rastrear los fragmentos)
    private String idXML;

    // Constructor: recibe las colas de entrada y salida y las pasa a la clase base
    public Splitter(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    // Método principal que ejecuta la tarea del Splitter
    @Override
    public void execute() {

        //Verifica si hay mensajes disponibles en la cola de entrada
        if (entradas.getFirst().isEmptyQueue()) {
            System.out.println("Splitter: No hay mensajes en la cola de entrada.");
            return; // Sale si no hay nada que procesar
        }

        try {
            // Genera un identificador único (UUID) para asociar a todos los fragmentos que salgan de este XML
            idXML = UUID.randomUUID().toString();

            //Extrae el mensaje de la cola de entrada
            Mensaje mensaje = entradas.getFirst().dequeuePoll();

            //Obtiene el cuerpo del mensaje (un objeto Document XML)
            Document doc = mensaje.getBody();

            // Prepara el motor XPath para buscar nodos dentro del XML
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // Compila la expresión XPath que se haya configurado antes (por setXPathExpression)
            XPathExpression expr = xPath.compile(xPathExpression);

            //  Evalúa la expresión XPath sobre el documento XML, obteniendo un conjunto de nodos
            NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            // Si no hay nodos que coincidan, se informa y se termina
            if (items == null || items.getLength() == 0) {
                System.out.println("Splitter: No se encontraron nodos con la expresión XPath.");
                return;
            }

            // Obtiene el nombre del nodo padre del primer elemento encontrado
            // Esto puede usarse más adelante para reconstruir el XML si es necesario
            String xp = "//" + items.item(0).getParentNode().getNodeName();

            // Elimina los nodos encontrados del documento original
            // (esto deja una copia del documento sin los nodos seleccionados)
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                item.getParentNode().removeChild(item);
            }

            //(Comentado) — Podría guardarse el documento original en un diccionario global
            // para reconstruirlo más tarde o mantener una referencia
	          
	            ValoresDiccionario vD = new ValoresDiccionario(xp, doc);
	            Diccionario diccionario = Diccionario.getInstance();
	            diccionario.put(idXML, vD);
            

            //Por cada nodo encontrado, se crea un nuevo mensaje independiente
            for (int i = 0; i < items.getLength(); i++) {

                // Crea un nuevo documento XML vacío
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document newDoc = docBuilder.newDocument();

                // Toma el nodo actual del listado
                Node item = items.item(i);

                // Importa ese nodo al nuevo documento (deep = true copia todo su contenido)
                Node importedItem = newDoc.importNode(item, true);
                newDoc.appendChild(importedItem);

                // ⓭ (Comentado) — Creación de un encabezado (Head) con metadatos del mensaje
                // Contendría información como posición del fragmento, total, ID único, etc.
                /*
                Head headAux = new Head(0, idXML, i + 1, items.getLength());
                headAux.setIdUnico(IdUnico.getInstance().getIdUnico());
                Mensaje mensajeAux = new Mensaje(headAux, newDoc);
                */

                // Encola el nuevo mensaje XML (fragmento) en la cola de salida
                // Nota: aquí se asume que 'mensajeAux' ya ha sido creado correctamente
                salidas.getFirst().enqueue(mensajeAux);
            }

        } catch (Exception e) {
            //Si ocurre algún error, se muestra un mensaje en consola y la traza de error
            System.out.println("Error al ejecutar Splitter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método setter para configurar desde fuera la expresión XPath
    public void setXPathExpression(String xPathExpression) {
        this.xPathExpression = xPathExpression;
    }
}