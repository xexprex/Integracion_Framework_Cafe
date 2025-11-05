package Tarea.Router;

import java.util.ArrayList;
import java.util.List;

import Principal.Head;
import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class Correlator extends TareaBase {

    public Correlator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    @Override
    public void execute() {
        for (Slot entrada : entradas) {
            if (entrada.isEmptyQueue()) {
                return;
            }
        }

        for (Mensaje mensajeControl : entradas.get(0).getQueue()) {
            String idCorrelacion = mensajeControl.getHead().getIdCorrelacion();
            if (idCorrelacion == null) {
                continue;
            }

            List<Mensaje> mensajesCorrelacionados = new ArrayList<>();
            mensajesCorrelacionados.add(mensajeControl);

            boolean setCompleto = true;
            for (int i = 1; i < entradas.size(); i++) {
                Mensaje mensajeEncontrado = findMessageByCorrelId(entradas.get(i), idCorrelacion);
                if (mensajeEncontrado != null) {
                    mensajesCorrelacionados.add(mensajeEncontrado);
                } else {
                    setCompleto = false;
                    break;
                }
            }

            if (setCompleto) {
                for (int i = 0; i < entradas.size(); i++) {
                    Mensaje msg = mensajesCorrelacionados.get(i);
                    entradas.get(i).removeByMessage(msg);
                    salidas.get(i).enqueue(msg);
                }
                return; 
            }
        }
    }

    private Mensaje findMessageByCorrelId(Slot slot, String idCorrelacion) {
        for (Mensaje mensaje : slot.getQueue()) {
            if (idCorrelacion.equals(mensaje.getHead().getIdCorrelacion())) {
                return mensaje;
            }
        }
        return null;
    }
}