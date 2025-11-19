package Conector;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import Puerto.Puerto;

public class ConectorFicheroEntrada extends Conector {

    private String rutaEntrada;

    public ConectorFicheroEntrada(Puerto puertoEntrada) {
        super(puertoEntrada);
    }

    public void setRuta(String rutaEntrada){
        this.rutaEntrada = rutaEntrada;
    }

    public void execute(){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(rutaEntrada);

            super.puerto.setDocument(doc);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
