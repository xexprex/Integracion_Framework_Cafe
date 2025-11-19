package Tarea;

import java.util.List;
import Principal.Slot;

//Tarea base es nuestro constructor universal
public abstract class TareaBase implements ITarea {

    protected final List<Slot> entradas, salidas;

    public TareaBase(List<Slot> entradas, List<Slot> salidas) {
        this.entradas = entradas;
        this.salidas = salidas;
    }
}