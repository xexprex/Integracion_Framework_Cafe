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

    }
}

