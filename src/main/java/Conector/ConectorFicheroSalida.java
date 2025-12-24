package Conector;


import java.io.File;
import java.time.LocalDateTime;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import Puerto.Puerto;

public class ConectorFicheroSalida extends Conector{
    private String rutaSalida;

    public ConectorFicheroSalida(Puerto puerto) {
        super(puerto);
    }

    public void setRutaSalida(String rutaSalida) {
        this.rutaSalida = rutaSalida;
    }

    public void execute(){
        LocalDateTime fecha = LocalDateTime.now();
        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String fechaCadena = fecha.format(formato);
        

        Document doc = super.puerto.getDocumentBySlot();

        if (doc == null) {
            return;
        }
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(rutaSalida+ File.separator + fechaCadena +".xml"));
            transformer.transform(source, result);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
