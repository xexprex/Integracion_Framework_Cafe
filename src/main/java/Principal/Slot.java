package Principal;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Slot {

    private Queue<Mensaje> mensajes;

    public Slot(){
        mensajes = new LinkedList<>();
    }

    public Slot(Queue<Mensaje> mensajes) {
        this.mensajes = mensajes;
    }

    public void enqueue(Mensaje mensaje) {
        mensajes.offer(mensaje);
    }

    public Mensaje dequeuePoll() {
        return mensajes.poll();
    }

    public List<Mensaje> getQueue() {
        return new LinkedList<>(mensajes);
    }

    public int getQueueSize() {
        return mensajes.size();
    }

    public boolean isEmptyQueue() {
        return mensajes.isEmpty();
    }

    public void removeByIndex(int index) {
        List<Mensaje> list = new LinkedList<>(mensajes);
        list.remove(index);
        mensajes = new LinkedList<>(list);
    }

    public void removeByMessage(Mensaje message){
        List<Mensaje> list = new LinkedList<>(mensajes);
        boolean borrado = false;
        int i = 0;

        while(!borrado && i < list.size()){
            if(list.get(i).getHead().getIdUnico() == message.getHead().getIdUnico()){
                removeByIndex(i);
                borrado = true; // <-- ESTA ES LA LÍNEA QUE FALTA
            }
            i++;
        }
    }

}

