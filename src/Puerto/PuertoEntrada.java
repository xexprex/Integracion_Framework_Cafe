package Puerto;

import Principal.Head;
import Principal.IdUnico;
import Principal.Mensaje;
import Principal.Slot;


public class PuertoEntrada extends Puerto {

    public PuertoEntrada(Slot salida) {
        super(null, salida);
    }

    @Override
    public void execute() {
        if (doc != null) {
            try {
                Head head = new Head();
                head.setIdUnico(IdUnico.getInstance().getIdUnico());

                Mensaje mensaje = new Mensaje(head, doc);

                salida.enqueue(mensaje);

            } catch (Exception e) {
                System.out.println("Error al leer el fichero: " + e.getMessage());
            }
        }
    }
}
