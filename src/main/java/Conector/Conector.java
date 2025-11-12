package Conector;

import Puerto.Puerto;

public abstract class Conector {
    protected Puerto puerto;

    public Conector(Puerto puerto) {
        this.puerto = puerto;
    }

    public abstract void execute();
}
