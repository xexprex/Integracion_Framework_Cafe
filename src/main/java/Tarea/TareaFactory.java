package Tarea;

import java.util.List;
import java.util.Map;
import Principal.Slot;
import Tarea.Router.*;
import Tarea.Transformer.*;
import Tarea.modifier.*;

public class TareaFactory {

    // Definimos el Enum público para que puedas usar TareaFactory.TipoTarea.SPLITTER
    public enum TipoTarea {
        SPLITTER, DISTRIBUTOR, AGREGATOR, MERGE, FILTER, REPLICATOR, TRANSLATOR, CORRELATOR, ENRICHER
    }

    public static ITarea crearTarea(TipoTarea tipo, List<Slot> entradas, List<Slot> salidas, Map<String, Object> config) {
        
        TareaBase tarea = null;

        switch (tipo) {
            case SPLITTER:
                // Tu Splitter actual pide (Slot, Slot), así que sacamos el primero de cada lista
                tarea = new Splitter(entradas.get(0), salidas.get(0));
                if (config != null && config.containsKey("xpath")) {
                    ((Splitter) tarea).setXPathExpression((String) config.get("xpath"));
                }
                break;

            case DISTRIBUTOR:
                // Tu Distributor pide (Slot, List)
                tarea = new Distributor(entradas, salidas);
                if (config != null) {
                    if(config.containsKey("xpath")) ((Distributor) tarea).setXpath((String) config.get("xpath"));
                    if(config.containsKey("orden")) ((Distributor) tarea).setElementosSegunOrden((List<String>) config.get("orden"));
                }
                break;

            case REPLICATOR:
                // Tu Replicator pide (Slot, List)
                tarea = new Replicator(entradas.get(0), salidas);
                break;

            case TRANSLATOR:
                // Tu Translator pide (List, List)
                tarea = new Translator(entradas, salidas);
                if (config != null && config.containsKey("xslt")) {
                    ((Translator) tarea).setRutaXSLT((String) config.get("xslt"));
                }
                break;

            case CORRELATOR:
                 // Tu Correlator pide (List, List)
                tarea = new Correlator(entradas, salidas);
                break;

            case ENRICHER:
                // Tu ContextEnricher pide (List, List)
                tarea = new ContextEnricher(entradas, salidas);
                if (config != null) {
                    if(config.containsKey("xpath-p")) ((ContextEnricher) tarea).setXPathPrincipal((String) config.get("xpath-p"));
                    if(config.containsKey("xpath-c")) ((ContextEnricher) tarea).setXPathContexto((String) config.get("xpath-c"));
                }
                break;
            
            case MERGE:
                // Tu Merge pide (List, Slot)
                tarea = new Merge(entradas, salidas);
                break;

            case AGREGATOR:
                 // Tu Agregator pide (List, List)
                tarea = new Agregator(entradas, salidas);
                break;
            
            case FILTER:
                 // Tu Filter pide (Slot, Slot)
                 tarea = new Filter(entradas.get(0), salidas.get(0));
                 if (config != null && config.containsKey("xpath")) {
                     ((Filter) tarea).setXpath((String) config.get("xpath"));
                 }
                 break;

            default:
                throw new IllegalArgumentException("Tipo de tarea no soportado: " + tipo);
        }

        return tarea;
    }
}