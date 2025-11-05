package Principal;

public class Head {

    private long idUnico;
    private int idCorrelator;
    private String idSecuencia;
    private int numSecuencia;
    private int totalSecuencia;

    public Head() {
        this.idCorrelator = -1;
        this.idSecuencia = "";
        this.numSecuencia = -1;
        this.totalSecuencia = -1;
    }

    public Head(int idCorrelator, String idSecuencia, int numSecuencia, int totalSecuencia) {
        this.idCorrelator = idCorrelator;
        this.idSecuencia = idSecuencia;
        this.numSecuencia = numSecuencia;
        this.totalSecuencia = totalSecuencia;
    }

    public int getIdCorrelator() {
        return idCorrelator;
    }

    public void setIdCorrelator(int idCorrelator) {
        this.idCorrelator = idCorrelator;
    }

    public String getIdSecuencia() {
        return idSecuencia;
    }

    public void setIdSecuencia(String idSecuencia) {
        this.idSecuencia = idSecuencia;
    }

    public int getNumSecuencia() {
        return numSecuencia;
    }

    public void setNumSecuencia(int numSecuencia) {
        this.numSecuencia = numSecuencia;
    }

    public int getTotalSecuencia() {
        return totalSecuencia;
    }

    public void setTotalSecuencia(int totalSecuencia) {
        this.totalSecuencia = totalSecuencia;
    }

    public long getIdUnico() {
        return idUnico;
    }

    public void setIdUnico(long idUnico) {
        this.idUnico = idUnico;
    }

    @Override
    public Head clone() {
        return new Head(this.idCorrelator, this.idSecuencia, this.numSecuencia, this.totalSecuencia);
    }
}
