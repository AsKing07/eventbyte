package fr.soft_labs.eventbyte.adapters.in.web;

import java.math.BigDecimal;

/**
 * Seul endroit où l'on décide de l'unité des prix renvoyés par l'API (décision EB-28).
 * Le backend travaille en centimes ; le front affiche le nombre reçu tel quel, en euros.
 */
public final class ApiPrice {

    private ApiPrice() {
    }

    public static BigDecimal toApi(long cents) {
        return BigDecimal.valueOf(cents, 2);   // 3023 -> 30.23
    }
}
