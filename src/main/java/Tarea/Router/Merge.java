package Tarea.Router;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

import java.util.List;

public class Merge extends TareaBase {
     
    public Merge(List<Slot> entradas, List<Slot> salidas) {
            super(entradas, salidas);
            
            // VALIDACIÓN (Importante): Protegemos la lógica interna
            if (salidas.size() != 1) {
                throw new IllegalArgumentException("Splitter requiere exactamente 1 slot de salida");
            }
    }

    @Override
    public void execute() {
        for (Slot entrada : entradas){
            if(!entrada.isEmptyQueue()){
                Mensaje mensaje = entrada.dequeuePoll();
                salidas.getFirst().enqueue(mensaje);
                break;
            }
        }
    }
}
