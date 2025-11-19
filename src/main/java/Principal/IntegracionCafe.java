package Principal;

import java.util.Scanner;

public class IntegracionCafe {

    public static void main(String[] args) {
     
     
     
     
        Scanner scanner = new Scanner(System.in);
        System.out.println("=========================================");
        System.out.println("      INTEGRACIÓN CAFÉ - SIMULADOR       ");
        System.out.println("=========================================");
        System.out.println("Seleccione el modo de ejecución:");
        System.out.println("1. Café CON Pago (Stripe + Ticket)");
        System.out.println("2. Café SIN Pago (Solo Ticket)");
        System.out.println("=========================================");
        System.out.print("Opción: ");

        String opcion = scanner.nextLine();

        if (opcion.equals("1")) {
            CafeConPago cafepago = new CafeConPago();
            cafepago.CafePago();
        } else if (opcion.equals("2")) {
            cafe cafesinpago = new cafe();
            cafesinpago.CafesinPago();
        } else {
            System.out.println("Opción no válida.");
        }  
        scanner.close(); 
    }
}

