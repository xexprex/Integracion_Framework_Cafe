package Puerto;

import org.w3c.dom.Document;
import Principal.Head;
import Principal.IdUnico;
import Principal.Mensaje;
import Principal.Slot;

// Importa las clases de XPath para leer el XML
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;

public class PuertoEntrada extends Puerto {

    public PuertoEntrada(Slot salida) {
        super(null, salida);
    }

    @Override
    public void execute() {
        // 'doc' se establece desde el main
        if (doc != null) {
            try {
                // 1. Crear el Head base
                Head head = new Head();
                head.setIdUnico(IdUnico.getInstance().getIdUnico());

                // --- NUEVA LÓGICA PARA LEER EL ID_CORRELATOR ---
                XPath xPath = XPathFactory.newInstance().newXPath();
                
                // 2. Buscar el nodo raíz (cualquier nombre: /*)
                Node rootNode = (Node) xPath.evaluate("/*", doc, XPathConstants.NODE);
                
                if (rootNode != null && rootNode.getAttributes() != null) {
                    // 3. Buscar el atributo 'idCorrelator' en el nodo raíz
                    Node idNode = rootNode.getAttributes().getNamedItem("idCorrelator");
                    if (idNode != null) {
                        // 4. Si existe, convertirlo a int y ponerlo en el Head
                        int idCorrelator = Integer.parseInt(idNode.getNodeValue());
                        head.setIdCorrelator(idCorrelator);
                        System.out.println("... ID Correlator detectado: " + idCorrelator);
                    }
                }
                // --- FIN DE LA NUEVA LÓGICA ---

                // 5. Crear el mensaje con el Head poblado
                Mensaje mensaje = new Mensaje(head, (Document) doc);
                salida.enqueue(mensaje);

            } catch (Exception e) {
                System.err.println("Error al leer el fichero en PuertoEntrada: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
