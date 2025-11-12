package Puerto;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import Principal.Slot;

public class PuertoSalida extends Puerto{

    public PuertoSalida(Slot entrada) {
        super(entrada, null);
    }

    @Override
    public void execute() {
        // Revisa si el slot de 'entrada' (definido en la clase padre) tiene mensajes
        if (entrada != null && !entrada.isEmptyQueue()) {
            
            // Saca el mensaje de la cola
            mensaje = entrada.dequeuePoll();
            doc = (Document) mensaje.getBody();

            // ¡Simula el "envío al exterior" imprimiéndolo!
            System.out.println("\n---PUERTO DE SALIDA HA RECIBIDO ---");
            System.out.println("  ID Único: " + mensaje.getHead().getIdUnico());
            try {
                // Imprime el contenido XML
                System.out.println("  Contenido: " + documentToString(doc));
            } catch (Exception e) {
                System.out.println("  Contenido: (Error al serializar XML: " + e.getMessage() + ")");
            }
            System.out.println("------------------------------------");
        }
    }
    
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // Omitir la declaración <?xml ...?> para una salida más limpia
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}

