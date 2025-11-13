package Principal;
//package main.java;

import org.w3c.dom.Document;

public class ValoresDiccionario {

    private Document context;
    private String xPathExpression;

    public ValoresDiccionario(String xPathExpression, Document context) {
        this.xPathExpression = xPathExpression;
        this.context = context;
    }

    public Document getContext() {
        return context;
    }

    public String getxPathExpression() {
        return xPathExpression;
    }
}
