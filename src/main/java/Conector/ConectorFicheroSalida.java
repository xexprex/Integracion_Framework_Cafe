package Conector;


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
        /*LocalDateTime fechaHoraActual = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd_MM_yyyy HH_mm_ss_nnnnnnnnn");
        String nombreFichero = fechaHoraActual.format(formato);

        Document doc = port.getDocumentBySlot();

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult
                    (new File(this.filePath + File.separator + nombreFichero +".xml"));
            transformer.transform(source, result);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }*/
    }
}
