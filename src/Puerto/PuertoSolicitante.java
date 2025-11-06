package Puerto;

import org.w3c.dom.Document;

import Principal.Slot;

public class PuertoSolicitante extends Puerto{


    public PuertoSolicitante(Slot entrada, Slot salida) {
        super(entrada, salida);
    }

    @Override
    public void execute() {
        if(doc != null){
            mensaje.setBody((Document) doc);
            salida.enqueue(mensaje);
        }
    }

}
