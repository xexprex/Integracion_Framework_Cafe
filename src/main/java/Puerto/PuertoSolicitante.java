package Puerto;

import org.w3c.dom.Document;

import Principal.Slot;

public class PuertoSolicitante extends Puerto{


    public PuertoSolicitante(Slot entrada, Slot salida) {
        super(entrada, salida);
    }

    @Override
    public void execute() {
        if(doc != null && mensaje != null){ // Añadimos un check extra
            mensaje.setBody((Document) doc);
            salida.enqueue(mensaje);
            
            // --- INICIO DE LA CORRECCIÓN ---
            // Limpiamos el estado para no procesar el mismo mensaje 101 veces
            doc = null;
            mensaje = null;
            // --- FIN DE LA CORRECCIÓN ---
        }
    }

}
