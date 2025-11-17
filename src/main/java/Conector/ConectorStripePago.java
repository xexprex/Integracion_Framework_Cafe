package Conector;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

import Puerto.Puerto;

public class ConectorStripePago extends Conector {

    /**
     * Constructor que inicializa la API de Stripe con tu clave secreta.
     */
    public ConectorStripePago(Puerto puerto, String apiKey) {
        super(puerto);
        // Configura la clave API global de Stripe al crear el conector
        Stripe.apiKey = apiKey;
    }

    /**
     * Define el XML que este conector espera recibir.
     * <peticionPago>
     * <monto>2000</monto> * <moneda>eur</moneda>
     * <fuente>tok_visa</fuente> * </peticionPago>
     */
    @Override
    public void execute() {
        // 1. Obtener el Document XML de petición desde el puerto
        //
       Document docPeticion = puerto.getDocumentBySlot(); 
        if (docPeticion == null) {
            System.out.println("ConectorStripe: No hay petición en el puerto.");
            return;
        }

        try {
            // 2. Parsear el XML de entrada
            String monto = docPeticion.getElementsByTagName("monto").item(0).getTextContent();
            String moneda = docPeticion.getElementsByTagName("moneda").item(0).getTextContent();
            String fuente = docPeticion.getElementsByTagName("fuente").item(0).getTextContent();

            // 3. Construir la petición a la API de Stripe
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(Long.parseLong(monto))
                    .setCurrency(moneda)
                    .setSource(fuente) // El token de prueba, ej: "tok_visa"
                    .setDescription("Cobro de prueba desde IntegracionCafe")
                    .build();

            // 4. Ejecutar la llamada a la API externa
            Charge charge = Charge.create(params);

            // 5. Transformar la RESPUESTA de Stripe a un Document XML
            Document docRespuesta = crearDocumentoRespuesta(charge);

            // 6. Poner el Document de respuesta en el puerto para el siguiente
            // componente del flujo
            puerto.setDocument(docRespuesta);

        } catch (Exception e) {
            System.out.println("Error en ConectorStripePago: " + e.getMessage());
            // Opcional: crear un XML de error y ponerlo en el puerto
            e.printStackTrace();
        }
    }

    /**
     * Helper para crear un Document XML a partir de la respuesta de Stripe.
     * Sigue el mismo patrón que tu ConectorSolicitudDB.
     * * Genera:
     * <resultadoPago>
     * <id>ch_...</id>
     * <estado>succeeded</estado>
     * <montoCobrado>2000</montoCobrado>
     * </resultadoPago>
     */
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
    }
}