package Tarea.Router;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

import java.util.List;

public class Merge extends TareaBase {
     public Merge(List<Slot> entradas, Slot salida) {
        super(entradas, List.of(salida));
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
