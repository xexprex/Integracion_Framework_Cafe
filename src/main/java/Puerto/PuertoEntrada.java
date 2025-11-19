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
        // Verificamos si el Conector ya ha inyectado un documento XML en este puerto
        if (doc != null) {
            try {
                // Generamos las cabeceras necesarias (ID único para trazabilidad)
                Head head = new Head();
                head.setIdUnico(IdUnico.getInstance().getIdUnico());
                // Encapsulamos el XML (body) y las cabeceras (head) en un objeto Mensaje
                Mensaje mensaje = new Mensaje(head, doc);

                salida.enqueue(mensaje);

            } catch (Exception e) {
                System.out.println("Error al leer el fichero: " + e.getMessage());
            }
        }
    }
}
