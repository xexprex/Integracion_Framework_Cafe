package Principal;
//package main.java;

public class IdUnico {

    private static final IdUnico INSTANCE = new IdUnico();
    private long idUnico;

    private IdUnico() {
        idUnico = 0;
    }

    public static IdUnico getInstance() {
        return INSTANCE;
    }

    public long getIdUnico() {
        return ++idUnico;
    }

}