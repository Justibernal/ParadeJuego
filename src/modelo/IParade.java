package modelo;

import ar.edu.unlu.rmimvc.observer.IObservableRemoto;

import java.rmi.RemoteException;
import java.util.List;

/**
 * (RMI) del juego Parade.
 * Expone operaciones de orquestación y consultas de lectura.
 * Todas las operaciones pueden lanzar {@link RemoteException} por ser remotas.
 */
public interface IParade extends IObservableRemoto {

    // -------- Conexión / setup --------

    /**
     * Registra un jugador en el lobby (solo antes de iniciar).
     * @return id asignado al jugador (posición en la lista interna)
     * @throws RemoteException en errores de red o si el nombre ya existe / partida iniciada
     */
    int unirJugador(String nombre) throws RemoteException;

    /**
     * Inicia la partida: mezcla mazo, reparte, prepara carnaval y fija turno.
     */
    void iniciarPartida(int cartasInicialMesa, int cartasPorJugador) throws RemoteException;

    /** @return true si la partida ya fue iniciada. */
    boolean partidaIniciada() throws RemoteException;

    // -------- Juego --------

    /**
     * Juega una carta por índice en la mano del jugador actual, aplica reglas,
     * roba si corresponde, puede disparar última ronda y avanza turno.
     */
    void jugarCarta(int idJugador, int indiceEnMano) throws RemoteException;

    // -------- Consultas de estado --------

    List<Carta> obtenerMano(int idJugador) throws RemoteException;
    List<Carta> obtenerCarnaval() throws RemoteException;
    List<Carta> obtenerRecolectadas(int idJugador) throws RemoteException;
    List<List<Carta>> obtenerRecolectadasTodos() throws RemoteException;
    String obtenerTurnoNombre() throws RemoteException;
    boolean esUltimaRonda() throws RemoteException;
    boolean esFinDePartida() throws RemoteException;

    /**
     * Puntajes calculados según reglas (2 jugadores: mayoría por diferencia ≥ 2).
     * @return lista paralela a {@link #nombresJugadores()}.
     */
    List<Integer> puntajesPorJugador() throws RemoteException;

    /** @return nombres en el orden de registro/turnos. */
    List<String> nombresJugadores() throws RemoteException;

    // -------- Persistencia de partida --------

    /** Guarda un snapshot serializado del {@code EstadoPartida}. */
    boolean guardarPartida(String archivo) throws RemoteException;

    /** Restaura un snapshot serializado del {@code EstadoPartida}. */
    boolean cargarPartida(String archivo) throws RemoteException;

    // -------- Ranking --------

    /** Registra el resultado actual en el ranking persistente (Top-5). */
    void registrarResultadoEnRanking() throws RemoteException;

    /** Devuelve el Top-5 ordenado (puntaje asc, y desempate por instante). */
    List<EntradaRanking> top5() throws RemoteException;
}


