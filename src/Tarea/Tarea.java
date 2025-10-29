package Tarea;

public abstract class Tarea {

	protected final List<Slot> entrada, salida;
	
	public Tarea (List<Slot>entradas,List<Slot>salidas) {
		
		this.entrada = entradas;
		this.salida = salidas;
	}

	public abstract void execute();

}


