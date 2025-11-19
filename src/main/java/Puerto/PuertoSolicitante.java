package Puerto;

import org.w3c.dom.Document;

import Principal.Slot;

public class PuertoSolicitante extends Puerto{


    public PuertoSolicitante(Slot entrada, Slot salida) {
        super(entrada, salida);
    }

    @Override
    public void execute() {
        if(doc != null && mensaje != null){ 
            // 1. Actualizamos el cuerpo del mensaje original con la respuesta recibida (ej. resultado SQL)
            mensaje.setBody((Document) doc);
            salida.enqueue(mensaje);
            

           // Reseteamos las referencias para evitar re-procesar el mismo mensaje en el siguiente ciclo
            doc = null;
            mensaje = null;

        }
    }

}
