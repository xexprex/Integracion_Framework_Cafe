package Principal;
import org.w3c.dom.Document;

public class Mensaje {

    //private Cabecera ;
    //private Documento body;

    public Mensaje() {
    }

    public Mensaje(Cabecera head, Documento body) {
        this.head = head;
        this.body = body;
    }

    public Cabecera getCabecera() {
        return Cabecera;
    }

    public void setCabecera(Cabecera head) {
        this.Cabecera = head;
    }

    public Documento getBody() {
        return body;
    }

    public void setBody(Documento body) {
        this.body = body;
    }

    @Override
    public Mensaje clone() {
        return new Mensaje(this.Cabecera.clone(), this.body);
    }
}
