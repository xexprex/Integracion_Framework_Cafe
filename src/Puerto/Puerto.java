package Puerto;

import org.w3c.dom.Document;

import Principal.Mensaje;
import Principal.Slot;

public abstract class Puerto {
	
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
	        if (entrada.getQueueSize() > 0) {
	            mensaje = entrada.dequeuePoll();
	            doc = (Document) mensaje.getBody();
	            return doc;
	        }
	        return null;
	    }

	    public void setDocument(Document doc){
	        this.doc = doc;
	    }

	}
