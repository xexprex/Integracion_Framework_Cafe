package Principal;
import org.w3c.dom.Document;

public class Mensaje {

    private Head head;
    private Document body;
    
    
    public Mensaje() {
    }

    public Mensaje(Head head, Document body) {
        this.head = head;
        this.body = body;
    }

    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public Document getBody() {
        return body;
    }

    public void setBody(Document body) {
        this.body = body;
    }

    @Override
    public Mensaje clone() {
        return new Mensaje(this.head.clone(), this.body);
    }
}
