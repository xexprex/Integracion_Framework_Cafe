package Tarea.modifier;

import java.util.List;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;
public class CorrelatorIdSetter extends TareaBase {

    private int correlatorId;

    // Constructor estandarizado para la Factory (List, List)
    public CorrelatorIdSetter(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
        this.correlatorId = 0;
    }

    @Override
    public void execute() {
        if (!entradas.getFirst().isEmptyQueue()) {
            Mensaje mensaje = entradas.getFirst().dequeuePoll();
            
            // Incrementamos ID y lo asignamos al Head
            mensaje.getHead().setIdCorrelator(++correlatorId);
            
            salidas.getFirst().enqueue(mensaje);
        } 
    }
}
