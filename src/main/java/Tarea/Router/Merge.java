package Tarea.Router;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

import java.util.List;


/**
 * Implementación Merge
 * 
 * Su función es recibir mensajes de múltiples canales de entrada y canalizarlos
 * hacia una única salida común.  Distributor.
 * 
 * Este componente implementa una fusión con prioridad basada en el orden de conexión.
 * Siempre revisa las entradas en el orden en que están en la lista (índice 0, 1, 2...).
 * Si el primer canal siempre tiene tráfico, los canales posteriores tendrán que esperar,
 * actuando el primero como un canal de "Alta Prioridad".
 */
public class Merge extends TareaBase {
    /**
     * Constructor del Merge.
     * @param entradas  Lista de slots de entrada. El orden de la lista define la prioridad de atención.
     * @param salidas   Lista que debe contener exactamente 1 slot de salida.
     */
    public Merge(List<Slot> entradas, List<Slot> salidas) {
            super(entradas, salidas);
    }

    @Override
    public void execute() {
        for (Slot entrada : entradas){                      //* Iteramos secuencialmente sobre las entradas para garantizar que la
            if(!entrada.isEmptyQueue()){                    //* entrada de la posicion 0 siempre se verifique primero.
                Mensaje mensaje = entrada.dequeuePoll();    //* extraemos el mensaje de la cola de entrada
                salidas.getFirst().enqueue(mensaje);        //* lo encolamos en el canal de salida
                break;                                      //* Por ultimo al hacer el break terminamos la ejecucion para 
            }                                               //* que la siguiente llamada vuelva a empezar desde 0.
        }
    }
}
