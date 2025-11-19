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
		//Verifica si no hay mensajes.
		if (!entradas.getFirst().isEmptyQueue()) {
			idXML = UUID.randomUUID().toString();												//* Genera un ID unico para la sesion, este ID permite al agregator saber
																								//* que mensajes pertenecen al mismo grupo.
			Mensaje mensaje = entradas.getFirst().dequeuePoll();
			Document doc = mensaje.getBody();

			try {
				XPathFactory xPathFactory = XPathFactory.newInstance();							//* Expresion XPAHT para localizar los nodos.
				XPath xPath = xPathFactory.newXPath();											//* 
				XPathExpression expr = xPath.compile(xPathExpression);							//*

				NodeList items = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);			//* Obetenemos la lista de nodos para separarlo 
				String xp = "//" + items.item(0).getParentNode().getNodeName(); 			//* Calculamos el Xpahy del padre para saber donde
																								//* reinstalar los datos en el futuro 

				for (int i = 0; i < items.getLength(); i++) {									//* Eliminamos los items hijos del documento original
					Node item = items.item(i);													//* Lo que queda es el "Esqueleto"
					item.getParentNode().removeChild(item);										//* 
				}

				ValoresDiccionario vD = new ValoresDiccionario(xp, doc);						//* Guardamos ese esqueleto en el diccionario grobal asociado a su ID
				Diccionario diccionario = Diccionario.getInstance();							//* El agregator usara ese ID para recuperar dicho esqueleto.
				diccionario.put(idXML, vD);														//* 

				for (int i = 0; i < items.getLength(); i++) {									//* Se iteran todos los nodos extraidos para crear los
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();	//* mensajes independientes. Creamos un documento XML limpio
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();				//* para el fragmento.
					Document newDoc = docBuilder.newDocument();									//* 
																								//* 
					Node item = items.item(i);													//* Importamos el nodo nuevo.
					Node importedItem = newDoc.importNode(item, true);					//* 
					newDoc.appendChild(importedItem);											//* 
																								//* 
					Head headAux = new Head(0, idXML, (i + 1), items.getLength());//* Configuramos la cabecera para la agregacion.
					headAux.setIdUnico(IdUnico.getInstance().getIdUnico());						//* Asignamos un nuevo ID para saber donde esta el mensaje.
					Mensaje mensajeAux = new Mensaje(headAux, newDoc);							//* 
																								//* 
					salidas.getFirst().enqueue(mensajeAux);										//* Por ultimo enviamos el fragmento de mensaje al siguiente paso.
				}

			} catch (Exception e) {
				System.out.println("Error al ejecutar el splitter " + e.getMessage());
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