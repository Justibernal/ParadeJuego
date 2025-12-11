package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entidad de dominio que representa a un jugador de Parade.
 * Mantiene su mano actual y las cartas recolectadas durante la partida.
 *
 * Invariantes:
 * - {@code id} es único en la partida.
 * - {@code nombre} no es null ni vacío.
 * - Las listas {@code mano} y {@code recolectadas} no son null.
 */
public class Jugador implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombre;
    private final List<Carta> mano = new ArrayList<>();
    private final List<Carta> recolectadas = new ArrayList<>();

    public Jugador(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // --- Identidad / datos básicos ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }

    // --- Estado de juego ---
    /** Mano actual del jugador (lista viva; el modelo expone copias al exterior). */
    public List<Carta> getMano() { return mano; }

    /** Pila de cartas recolectadas por el jugador. */
    public List<Carta> getRecolectadas() { return recolectadas; }

    // --- Alias para compatibilidad histórica (preferir getRecolectadas) ---
    /** @deprecated Usar {@link #getRecolectadas()}. Se mantiene por compatibilidad. */
    @Deprecated
    public List<Carta> getRecogidas() { return recolectadas; }

    // --- Mutadores seguros del estado interno ---
    public void agregarRecogidas(Collection<Carta> cartas) {
        if (cartas != null && !cartas.isEmpty()) {
            recolectadas.addAll(cartas);
        }
    }

    public void agregarRecogida(Carta c) {
        if (c != null) {
            recolectadas.add(c);
        }
    }

    @Override
    public String toString() {
        return "Jugador{" + "id=" + id + ", nombre='" + nombre + '\'' + '}';
    }

    // equals/hashCode por id para identidad dentro de una misma partida
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jugador j)) return false;
        return id == j.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}


