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
import Principal.IdUnico;
import Principal.Mensaje;
import Principal.Slot;
import Principal.ValoresDiccionario;
import Tarea.TareaBase;

/**
 * Implementación de Splitter.
 * 
 * Su funcion es recibir un mensaje con multiples elementos y 
 * dividirlo en una secuencia de mensajes individuales
 * 
 * Lógica clave:
 * 1. Identifica las partes usando XPath.
 * 2. Separa las partes del documento original, dejando un contexto común.
 * 3. Guardar ese contexto en un Diccionario global para que un Agregator futuro pueda reconstruirlo.
 * 4. Envía cada parte como un nuevo mensaje individual.
 */
public class Splitter extends TareaBase {

	private String xPathExpression;
	private String idXML;

    /**
     * Constructor del Splitter.
     * @param entrada 	Slot único de donde se leerán los mensajes compuestos.
     * @param salida 	Slot único donde se depositarán las multiples partes del mensaje
     */
	public Splitter(Slot entrada, Slot salida) {
		super(List.of(entrada), List.of(salida));
	}

	 @Override
    public void execute() {
        // Verificación de guardia: Si no hay mensajes, no gastamos recursos.
        if (!entradas.getFirst().isEmptyQueue()) {
            
            // Generamos un ID único para esta "sesión" de división.
            // Este ID permitirá al Agregator saber qué mensajes pertenecen al mismo grupo original.
            idXML = UUID.randomUUID().toString();

            Mensaje mensaje = entradas.getFirst().dequeuePoll();
            Document doc = mensaje.getBody();

            try {
                // Compilamos la expresión para localizar los nodos repetitivos (ej: <item>, <drink>)
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();
                XPathExpression expr = xPath.compile(xPathExpression);

                // Obtenemos la lista de nodos que vamos a separar
                NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                // Calculamos el XPath del padre para saber dónde re-insertar en el futuro
                String xp = "//" + items.item(0).getParentNode().getNodeName();

                // Eliminamos los items hijos del documento original.
                // Lo que queda es el "esqueleto" (cabeceras, IDs de pedido, totales, etc.).
                for (int i = 0; i < items.getLength(); i++) {
                    Node item = items.item(i);
                    item.getParentNode().removeChild(item);
                }

                // Guardamos el documento "esqueleto" en el Diccionario global asociado al ID de secuencia (idXML).
                // El Agregator usará ese idXML para recuperar el esqueleto y rellenarlo de nuevo.
                ValoresDiccionario vD = new ValoresDiccionario(xp, doc);
                Diccionario diccionario = Diccionario.getInstance();
                diccionario.put(idXML, vD);
                
                // Iteramos sobre los nodos extraídos para crear mensajes independientes.
                for (int i = 0; i < items.getLength(); i++) {
                    
                    // Crear un nuevo Documento XML limpio para el fragmento
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document newDoc = docBuilder.newDocument();

                    // Importar el nodo al nuevo documento
                    Node item = items.item(i);
                    Node importedItem = newDoc.importNode(item, true);
                    newDoc.appendChild(importedItem);

                    // CONFIGURACIÓN DE LA CABECERA (HEAD
                    Head headAux = new Head(0, idXML, (i + 1), items.getLength());
                    
                    // Asignamos un nuevo ID único de trazabilidad al mensaje
                    headAux.setIdUnico(IdUnico.getInstance().getIdUnico());
                    
                    Mensaje mensajeAux = new Mensaje(headAux, newDoc);

                    // Enviamos el fragmento al siguiente paso
                    salidas.getFirst().enqueue(mensajeAux);
                }

            } catch (Exception e) {
                System.out.println("Error al ejecutar el splitter: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Define qué nodos se separarán.
     * @param xPathExpression Expresión XPath (ej: "/Pedido/Items/Item")
     */
    public void setXPathExpression(String xPathExpression) {
        this.xPathExpression = xPathExpression;
    }
}