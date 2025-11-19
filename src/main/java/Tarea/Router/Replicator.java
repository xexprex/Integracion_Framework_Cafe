package Tarea.Router;

import java.util.List;

import Principal.IdUnico;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class Replicator extends TareaBase{

    public Replicator(Slot entrada, List<Slot> salidas) {
        super(List.of(entrada), salidas);
    }

    @Override
    public void execute() {
        // 1. Verificación inicial: Comprobamos si hay mensajes pendientes en la cola de entrada.
        if (!entradas.getFirst().isEmptyQueue()) {
            // 2. Extraemos el mensaje original de la entrada para procesarlo.
            Mensaje m = entradas.getFirst().dequeuePoll();

            // 3. Difusión: Recorremos todas las colas de salida conectadas
            for (Slot salida : salidas) {
                // 4. Creamos una copia independiente del mensaje para que los cambios en una rama no afecten a las otras.
            	Mensaje aux = m.clone();
                // 5.  Asignamos un nuevo ID único a la copia para poder rastrearla individualmente.
                aux.getHead().setIdUnico(IdUnico.getInstance().getIdUnico());

                salida.enqueue(aux);
            }
        } else {
            //System.out.println("Replicator: No hay mensajes en la cola de entrada");
        }
    }
}
