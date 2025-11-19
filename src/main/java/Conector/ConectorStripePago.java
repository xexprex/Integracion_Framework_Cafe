package Conector;

import org.w3c.dom.Document;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

import Puerto.Puerto;

public class ConectorStripePago extends Conector {

    public ConectorStripePago(Puerto puerto, String apiKey) {
        super(puerto);
        Stripe.apiKey = apiKey;
    }

    @Override
    public void execute() {
        Document docPeticion = puerto.getDocumentBySlot(); 
        
        // --- CAMBIO: Si es null, simplemente salimos SIN imprimir nada por pantalla ---
        if (docPeticion == null) {
            // System.out.println("ConectorStripe: No hay petición en el puerto."); // COMENTADO
            return;
        }

        try {
            String monto = docPeticion.getElementsByTagName("monto").item(0).getTextContent();
            String moneda = docPeticion.getElementsByTagName("moneda").item(0).getTextContent();
            String fuente = docPeticion.getElementsByTagName("fuente").item(0).getTextContent();

            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(Long.parseLong(monto))
                    .setCurrency(moneda)
                    .setSource(fuente)
                    .setDescription("Cobro de prueba desde IntegracionCafe")
                    .build();

            Charge.create(params);
            /* Ahora el documento lo generamos por el puerto solicitante
            Document docRespuesta = crearDocumentoRespuesta(charge);
            puerto.setDocument(docRespuesta);*/

        } catch (Exception e) {
            System.out.println("Error en ConectorStripePago: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*Ahora el documento lo generamos por el puerto solicitante

    private Document crearDocumentoRespuesta(Charge charge) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.newDocument();

        Element raiz = newDoc.createElement("resultadoPago");
        newDoc.appendChild(raiz);

        Element id = newDoc.createElement("id");
        id.setTextContent(charge.getId());
        raiz.appendChild(id);

        Element estado = newDoc.createElement("estado");
        estado.setTextContent(charge.getStatus());
        raiz.appendChild(estado);

        Element monto = newDoc.createElement("montoCobrado");
        monto.setTextContent(String.valueOf(charge.getAmount()));
        raiz.appendChild(monto);

        return newDoc;
    } */
}