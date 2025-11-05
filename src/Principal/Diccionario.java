package Principal;

import java.util.HashMap;
import java.util.Map;

public class Diccionario {

    private static final Diccionario INSTANCE = new Diccionario();
    private final Map<String, ValoresDiccionario> diccionarioMap;

    public Diccionario() {
        diccionarioMap = new HashMap<>();
    }

    public static Diccionario getInstance() {
        return INSTANCE;
    }

    public void put(String key, ValoresDiccionario value) {
        diccionarioMap.put(key, value);
    }

    public ValoresDiccionario get(String key) {
        return diccionarioMap.get(key);
    }

}
