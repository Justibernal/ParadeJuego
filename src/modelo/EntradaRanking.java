package modelo;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entrada del ranking para Parade.
 * Puntaje menor es mejor. Se guarda el instante de registro para ordenar empates.
 */
public class EntradaRanking implements Serializable, Comparable<EntradaRanking> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final int puntaje;              // menor es mejor
    private final long instanteRegistroMs;  // timestamp para desempate

    /** Crea una entrada con marca de tiempo explícita. */
    public EntradaRanking(String nombre, int puntaje, long instanteRegistroMs) {
        this.nombre = nombre;
        this.puntaje = puntaje;
        this.instanteRegistroMs = instanteRegistroMs;
    }

    public String getNombre() { return nombre; }
    public int getPuntaje() { return puntaje; }

    @SuppressWarnings("unused")
    public long getInstanteRegistroMs() { return instanteRegistroMs; }

    /** Orden natural: primero puntaje (asc), luego instante (asc). */
    @Override
    public int compareTo(EntradaRanking o) {
        int cmp = Integer.compare(this.puntaje, o.puntaje);
        return (cmp != 0) ? cmp : Long.compare(this.instanteRegistroMs, o.instanteRegistroMs);
    }

    @Override
    public String toString() { return nombre + " - " + puntaje; }
}



