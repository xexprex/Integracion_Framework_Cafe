package Puerto;

import org.w3c.dom.Document;

import Principal.Mensaje;
import Principal.Slot;

public abstract class Puerto {//encapsular la gestión del flujo de mensajes (Mensaje) y la extracción de su contenido XML (Document)
	
	 protected final Slot entrada;
	    protected final Slot salida;

	    protected Document doc;
	    protected Mensaje mensaje;

	    public Puerto(Slot entrada, Slot salida) {
	        this.entrada = entrada;
	        this.salida = salida;
	    }

	    public abstract void execute();

	    public Document getDocumentBySlot() {
	        if (entrada.getQueueSize() > 0) {//Comprobar si hay mensajes esperando en la cola.

	            mensaje = entrada.dequeuePoll();// Se guarda en la variable de clase 'mensaje' para mantener el contexto (headers, IDs, etc.).

	            doc = (Document) mensaje.getBody();//Se hace un casting explícito a Document (XML) y se actualiza la variable de estado 'doc'.
	            return doc;
	        }
	        return null;
	    }

	    public void setDocument(Document doc){
	        this.doc = doc;
	    }

	}
