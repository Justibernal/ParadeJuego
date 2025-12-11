package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Snapshot del estado mutable de una partida en curso.
 * Solo datos: sin lógica de reglas.
 * Contiene:
 *  - Jugadores con su mano/recogidas.
 *  - Mazo (pila LIFO/stack usando Deque).
 *  - Carnaval (lista en mesa).
 *  - Turno actual, bandera de última ronda e iniciada.
 */
public class EstadoPartida implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Lista en orden de asiento/turno. */
    private final List<Jugador> jugadores = new ArrayList<>();
    /** Mazo como pila (push/pop en el tope). */
    private final Deque<Carta> mazo = new ArrayDeque<>();
    /** Cartas en mesa en orden de aparición. */
    private final List<Carta> carnaval = new ArrayList<>();

    private int turnoActual = 0;
    private boolean ultimaRonda = false;
    private boolean iniciada = false;

    // --- Getters/Setters (dejan mutar desde el Modelo) ---
    public List<Jugador> getJugadores() { return jugadores; }
    public Deque<Carta> getMazo() { return mazo; }
    public List<Carta> getCarnaval() { return carnaval; }

    public int getTurnoActual() { return turnoActual; }
    public void setTurnoActual(int t) { this.turnoActual = t; }

    public boolean isUltimaRonda() { return ultimaRonda; }
    public void setUltimaRonda(boolean u) { this.ultimaRonda = u; }

    public boolean isIniciada() { return iniciada; }
    public void setIniciada(boolean b) { iniciada = b; }

    /** Limpia todo el estado (útil para re-iniciar partidas). */
    public void reset() {
        jugadores.clear();
        mazo.clear();
        carnaval.clear();
        turnoActual = 0;
        ultimaRonda = false;
        iniciada = false;
    }
}



