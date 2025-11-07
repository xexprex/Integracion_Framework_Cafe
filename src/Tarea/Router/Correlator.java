package Tarea.Router;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import Principal.Mensaje;
import Principal.Slot;
import Principal.Utilidad;
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

        // Iteramos sobre una copia de la cola maestra
        for (Mensaje mensajeControl : entradas.get(0).getQueue()) {
            
            // CAMBIO 1: Usamos el método correcto (getIdCorrelator) y el tipo (int)
            int idCorrelacion = mensajeControl.getHead().getIdCorrelator();

            // CAMBIO 2: Comparamos con -1 (valor por defecto) en lugar de null
            if (idCorrelacion == -1) {
                continue; 
            }

            List<Mensaje> mensajesCorrelacionados = new ArrayList<>();
            mensajesCorrelacionados.add(mensajeControl);

            boolean setCompleto = true;
            
            for (int i = 1; i < entradas.size(); i++) {
                // CAMBIO 3: Pasamos el 'int' al método de búsqueda
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
                // (Se mantiene sin el 'return' para procesar múltiples sets)
            }
        }
    }

    /**
     * Busca un mensaje por ID de correlación (ahora como int).
     */
    // CAMBIO 4: El parámetro ahora es 'int'
    private Mensaje findMessageByCorrelId(Slot slot, int idCorrelacion) {
        for (Mensaje mensaje : slot.getQueue()) {
            // CAMBIO 5: Comparamos 'int' con '=='
            if (idCorrelacion == mensaje.getHead().getIdCorrelator()) {
                return mensaje;
            }
        }
        return null;
    }
}