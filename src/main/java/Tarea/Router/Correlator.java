package Tarea.Router;

import java.util.ArrayList;
import java.util.List;

import Principal.Mensaje;
import Principal.Slot;
import Tarea.TareaBase;

public class Correlator extends TareaBase {

    public Correlator(List<Slot> entradas, List<Slot> salidas) {
        super(entradas, salidas);
    }

    @Override
    public void execute() {
        // 1. Validación previa: Si alguna cola de entrada está vacía, no podemos formar grupos completos, así que salimos.
        for (Slot entrada : entradas) {
            if (entrada.isEmptyQueue()) {
                return;
            }
        }

        // 2. Bucle principal: Iteramos sobre la primera cola (maestra) para usar sus mensajes como referencia de búsqueda.
        // Iteramos sobre una copia de la cola maestra
        for (Mensaje mensajeControl : entradas.get(0).getQueue()) {
            
            //  Obtenemos el ID para buscar coincidencias 
            int idCorrelacion = mensajeControl.getHead().getIdCorrelator();

            // Saltamos mensajes que no tengan un ID de correlación válido
            if (idCorrelacion == -1) {
                continue; 
            }

            // Lista temporal para guardar el grupo de mensajes coincidentes
            List<Mensaje> mensajesCorrelacionados = new ArrayList<>();
            mensajesCorrelacionados.add(mensajeControl);

            boolean setCompleto = true;
            // 3. Buscamos el mensaje correspondiente en el resto de las colas de entrada
            for (int i = 1; i < entradas.size(); i++) {
                // Pasamos el 'int' al método de búsqueda
                Mensaje mensajeEncontrado = findMessageByCorrelId(entradas.get(i), idCorrelacion);
                if (mensajeEncontrado != null) {
                    mensajesCorrelacionados.add(mensajeEncontrado);
                } else {
                    setCompleto = false;
                    break; 
                }
            }

            // 4. Procesamiento: Si encontramos el mensaje en TODAS las colas (set completo)
            if (setCompleto) {
                for (int i = 0; i < entradas.size(); i++) {
                    Mensaje msg = mensajesCorrelacionados.get(i);
                    // Retiramos el mensaje de la cola de entrada y lo pasamos a la salida correspondiente
                    entradas.get(i).removeByMessage(msg); 
                    salidas.get(i).enqueue(msg);
                }
                // (Se mantiene sin el 'return' para procesar múltiples sets)
            }
        }
    }

    /**
     * Busca un mensaje por ID de correlación (ahora como int).
     */

    private Mensaje findMessageByCorrelId(Slot slot, int idCorrelacion) {
        for (Mensaje mensaje : slot.getQueue()) {

            if (idCorrelacion == mensaje.getHead().getIdCorrelator()) {
                return mensaje;
            }
        }
        return null;
    }
}