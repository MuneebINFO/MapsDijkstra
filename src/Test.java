import utility.*;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

public class Test {
    public static void main(String[] args) throws IOException {
        System.out.println("Lancement des tests.\n");

        System.out.println("Exemple d'itinéraires entre même agence de transport :");
        System.out.println("Exemple STIB\n");

        System.out.println("[TEST] Itinéraire STIB : LOUISE à SIPPELBERG à 08:00");
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");
        String[] args1 = {"MACHTENS", "LOUISE", "08:00"};
        Main.main(args1);
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");

        System.out.println("\n[TEST] Itinéraire SNCB : Bruxelles-Schuman à Anderlecht à 10:30");
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");
        String[] args2 = {"Bruxelles-Schuman", "Anderlecht", "10:30"};
        Main.main(args2);
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");

        System.out.println("\n[TEST] Itinéraire DELIJN : Overijse Sint-Martinusschool à Overijse Jezus-Eik Kerk à 08:00");
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");
        String[] args3 = {"Overijse Sint-Martinusschool", "Overijse Jezus-Eik Kerk", "09:00"};
        Main.main(args3);
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");

        System.out.println("\n[TEST] Itinéraire TEC : AUTRE-EGLISE Eglise à AUTRE-EGLISE Gare à 08:00");
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");
        String[] args4 = {"AUTRE-EGLISE Eglise", "AUTRE-EGLISE Gare", "10:45"};
        Main.main(args4);
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");

        System.out.println("\nExemple d'itinéraire entre différente agence de transport :");
        System.out.println("\n[TEST] Itinéraire STIB à SNCB : TRONE à Bruxelles-Nord à 08:00");
        System.out.println("   ————————————————————————————————————————————————————————————————————————————————————\n");
        String[] args5 = {"TRONE", "Bruxelles-Nord", "11:00"};
        Main.main(args5);

        
    }
}
