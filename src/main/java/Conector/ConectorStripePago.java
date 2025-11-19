package Conector;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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


            // El ticket generado tiene <cobrado> dentro de <total>
            NodeList listaCobrado = docPeticion.getElementsByTagName("cobrado");
            
            // Verificamos si existe para evitar el NullPointerException
            if (listaCobrado.getLength() == 0) {
                System.out.println("ConectorStripe: No se encontró el monto a cobrar en el XML.");
                return;
            }

            String montoTexto = listaCobrado.item(0).getTextContent();
            
            // Convertimos "5.4" -> 5.4 -> 540
            double montoDouble = Double.parseDouble(montoTexto);
            long montoCentavos = (long) (montoDouble * 100); 

            // 3. VALORES POR DEFECTO: Como el ticket no trae moneda ni tarjeta, los ponemos fijos
            String moneda = "eur"; 
            String fuente = "tok_visa"; // Token de prueba de Stripe siempre válido


            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(montoCentavos) // Usamos el long calculado
                    .setCurrency(moneda)
                    .setSource(fuente)
                    .setDescription("Cobro automático ticket café")
                    .build();

            Charge.create(params);

        } catch (Exception e) {
            System.out.println("Error en ConectorStripePago: " + e.getMessage());
            e.printStackTrace();
        }
    }

}