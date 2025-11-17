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
        if (!entradas.getFirst().isEmptyQueue()) {
            Mensaje m = entradas.getFirst().dequeuePoll();

            for (Slot salida : salidas) {
            	Mensaje aux = m.clone();
                aux.getHead().setIdUnico(IdUnico.getInstance().getIdUnico());

                salida.enqueue(aux);
            }
        } else {
            //System.out.println("Replicator: No hay mensajes en la cola de entrada");
        }
    }
}
