package modelo;

import ar.edu.unlu.rmimvc.observer.ObservableRemoto;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.Serial;

/**
 * Capa de aplicación (Observable/RMI): coordina la partida, delega reglas en el dominio
 * y emite eventos para que las vistas se actualicen.
 * Responsabilidades:
 * - Crear/mezclar mazo, repartir e iniciar partida.
 * - Encadenar jugadas y transición a última ronda / fin de partida.
 * - Persistir/leer ranking y guardar/cargar estado de partida en disco.
 * - Exponer consultas de lectura (manos, carnaval, recogidas, puntajes, ranking).
 */
public class ModeloParade extends ObservableRemoto implements IParade, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ModeloParade.class.getName());

    /** Archivo donde se persiste el Top-5 entre ejecuciones. */
    private static final String RANK_FILE = "ranking.dat";

    // ---- Estado de dominio (partida en curso) ----
    private final EstadoPartida estado = new EstadoPartida();
    private final Random rng = new Random();

    // ---- Ranking persistente ----
    private final TablaClasificacion tablaRanking = new TablaClasificacion();

    public ModeloParade() throws RemoteException {
        // Carga “best effort”: si falla/ no existe, queda vacío sin romper el server.
        tablaRanking.cargarDe(RANK_FILE);
    }

    // ---------------- Helpers internos ----------------

    /** Construye y mezcla el mazo de Parade. */
    private List<Carta> generarMazo() {
        List<Carta> mazo = new ArrayList<>();
        for (ColorCarta c : ColorCarta.values()) {
            for (int v = 0; v <= 10; v++) {
                mazo.add(new Carta(c, v));
            }
        }
        Collections.shuffle(mazo, rng);
        return mazo;
    }

    /** ¿El jugador recolectó al menos una carta de los seis colores? */
    private boolean jugadorTieneSeisColores(Jugador j) {
        EnumSet<ColorCarta> colores = EnumSet.noneOf(ColorCarta.class);
        for (Carta c : j.getRecolectadas()) colores.add(c.getColor());
        return colores.size() == ColorCarta.values().length;
    }

    /** Toma un snapshot de puntajes actuales y los mapea a nombres. */
    public Map<String, Integer> calcularPuntajesFinales() {
        Map<String, Integer> pts = new LinkedHashMap<>();
        List<String> nombres = nombresJugadores();
        List<Integer> valores = puntajesPorJugador();
        for (int i = 0; i < nombres.size(); i++) pts.put(nombres.get(i), valores.get(i));
        return pts;
    }

    /**
     * Verifica condiciones de cierre; guarda ranking y notifica a vistas.
     * Emite: PUNTAJES_FINALES y RANKING_ACTUALIZADO.
     */
    public void finalizarPartidaSiCorresponde() throws RemoteException {
        boolean sinMazo = estado.getMazo().isEmpty();
        boolean manosVacias = estado.getJugadores().stream().allMatch(j -> j.getMano().isEmpty());
        if (sinMazo || manosVacias || esFinDePartida()) {
            Map<String, Integer> puntajes = calcularPuntajesFinales();
            tablaRanking.registrarResultado(puntajes);
            tablaRanking.guardarEn(RANK_FILE); // persistencia best-effort

            notificarObservadores(Evento.PUNTAJES_FINALES);
            notificarObservadores(Evento.RANKING_ACTUALIZADO);
        }
    }

    // ---------------- Implementación IParade ----------------

    /** Alta de jugador. No se permite si la partida ya inició. */
    @Override
    public int unirJugador(String nombre) throws RemoteException {
        if (estado.isIniciada()) {
            throw new RemoteException("La partida ya está iniciada. No se pueden unir jugadores nuevos.");
        }
        for (Jugador j : estado.getJugadores()) {
            if (j.getNombre().equalsIgnoreCase(nombre)) {
                throw new RemoteException("Ya existe un jugador con ese nombre.");
            }
        }
        int id = estado.getJugadores().size();
        estado.getJugadores().add(new Jugador(id, nombre));

        notificarObservadores(Evento.JUGADOR_UNIDO); // UI: refrescar lobby
        return id;
    }

    /** Inicializa mazo/mesa/manos y marca inicio de partida. */
    @Override
    public void iniciarPartida(int cartasInicialMesa, int cartasPorJugador) throws RemoteException {
        estado.getMazo().clear();
        estado.getMazo().addAll(generarMazo());
        estado.getCarnaval().clear();
        estado.setUltimaRonda(false);
        estado.setTurnoActual(0);

        for (Jugador j : estado.getJugadores()) {
            j.getMano().clear();
            j.getRecolectadas().clear();
            for (int k = 0; k < cartasPorJugador && !estado.getMazo().isEmpty(); k++) {
                j.getMano().add(estado.getMazo().pop());
            }
        }
        for (int i = 0; i < cartasInicialMesa && !estado.getMazo().isEmpty(); i++) {
            estado.getCarnaval().add(estado.getMazo().pop());
        }

        estado.setIniciada(true);
        notificarObservadores(Evento.INICIO_PARTIDA); // UI: render manos/carnaval/turno
    }

    @Override public boolean partidaIniciada() { return estado.isIniciada(); }

    /**
     * Orquesta la jugada: valida con simulación, aplica cambios reales,
     * roba carta si corresponde, puede disparar última ronda y avanza turno.
     * Emite: CARTA_JUGADA, ULTIMA_RONDA (si aplica) y FIN_PARTIDA (si cierra).
     */
    @Override
    public void jugarCarta(int idJugador, int indiceEnMano) throws RemoteException {
        if (estado.getJugadores().isEmpty()) return;
        if (estado.getTurnoActual() != idJugador) return;

        Jugador j = estado.getJugadores().get(idJugador);
        if (indiceEnMano < 0 || indiceEnMano >= j.getMano().size()) return;

        // --- Validación por simulación (no muta estado real) ---
        List<Carta> carnavalAntes = new ArrayList<>(estado.getCarnaval());
        Carta jugada = j.getMano().get(indiceEnMano);

        List<Carta> carnavalSim = new ArrayList<>(carnavalAntes);
        carnavalSim.add(jugada);

        List<Carta> retiradasSim = calcularRetiradas(carnavalSim, jugada);
        List<Carta> carnavalDespuesSim = new ArrayList<>(carnavalSim);
        carnavalDespuesSim.removeAll(retiradasSim);

        assertJugadaValida(carnavalAntes, jugada, retiradasSim, carnavalDespuesSim);

        // --- Commit real ---
        j.getMano().remove(indiceEnMano);
        recoger(j, jugada);

        // Robo sólo si NO estamos en última ronda y queda mazo
        if (!estado.isUltimaRonda() && !estado.getMazo().isEmpty()) {
            j.getMano().add(estado.getMazo().pop());
        }

        // Disparo “última ronda” en el momento exacto en que el jugador toca 6 colores
        // o cuando el mazo se agota.
        if (!estado.isUltimaRonda() && (jugadorTieneSeisColores(j) || estado.getMazo().isEmpty())) {
            estado.setUltimaRonda(true);
            notificarObservadores(Evento.ULTIMA_RONDA);
        }

        // Avanza turno y notifica jugada
        estado.setTurnoActual((estado.getTurnoActual() + 1) % estado.getJugadores().size());
        notificarObservadores(Evento.CARTA_JUGADA);

        // Cierre de partida
        if (esFinDePartida()) {
            finalizarPartidaSiCorresponde();
            notificarObservadores(Evento.FIN_PARTIDA);
        }
    }

    // ---------------- Reglas internas (dominio) ----------------
    /** Calcula las cartas a retirar según reglas de Parade para una jugada dada. */
    private List<Carta> calcularRetiradas(List<Carta> carnavalConJugada, Carta jugada) {
        final int prev = carnavalConJugada.size() - 1; // cantidad previa a la jugada
        final int v = jugada.getValor();

        if (prev <= v) return List.of(); // no se retira nada

        final int evaluadas = prev - v; // se evalúan SOLO las primeras (prev - v)
        List<Carta> retiradas = new ArrayList<>(evaluadas);
        for (int i = 0; i < evaluadas; i++) {
            Carta c = carnavalConJugada.get(i);
            if (c.getColor() == jugada.getColor() || c.getValor() <= v) {
                retiradas.add(c);
            }
        }
        return retiradas;
    }


    /** Aplica la jugada real: agrega la carta al carnaval y mueve retiradas al jugador. */
    private void recoger(Jugador jugador, Carta jugada) {
        estado.getCarnaval().add(jugada);
        List<Carta> carnavalSim = new ArrayList<>(estado.getCarnaval());
        List<Carta> retiradas = calcularRetiradas(carnavalSim, jugada);
        aplicarRetiradas(estado.getCarnaval(), jugador, retiradas);
    }

    private void aplicarRetiradas(List<Carta> carnavalReal, Jugador jugador, List<Carta> retiradas) {
        if (retiradas == null || retiradas.isEmpty()) return;
        carnavalReal.removeAll(retiradas);
        jugador.agregarRecogidas(retiradas);
    }

    /** Invariantes de seguridad para la jugada. */
    private void assertJugadaValida(List<Carta> carnavalAntes,
                                    Carta jugada,
                                    List<Carta> retiradas,
                                    List<Carta> carnavalDespues) {
        int expectedSize = carnavalAntes.size() + 1 - retiradas.size();
        if (carnavalDespues.size() != expectedSize) {
            throw new IllegalStateException("Cardinalidad inválida tras la jugada.");
        }
        for (Carta c : retiradas) {
            if (!carnavalAntes.contains(c)) {
                throw new IllegalStateException("Se retira una carta que no estaba en el carnaval.");
            }
        }
        List<Carta> recomputeBase = new ArrayList<>(carnavalAntes);
        recomputeBase.add(jugada);
        List<Carta> check = calcularRetiradas(recomputeBase, jugada);
        if (check.size() != retiradas.size() || !new HashSet<>(check).equals(new HashSet<>(retiradas))) {
            throw new IllegalStateException("Retiradas no coinciden con el cálculo de reglas.");
        }
    }

    // ---------------- Consultas / utilidades ----------------

    @Override public List<Carta> obtenerMano(int idJugador) { return List.copyOf(estado.getJugadores().get(idJugador).getMano()); }
    @Override public List<Carta> obtenerCarnaval() { return List.copyOf(estado.getCarnaval()); }
    @Override public String obtenerTurnoNombre() { return estado.getJugadores().get(estado.getTurnoActual()).getNombre(); }
    @Override public List<Carta> obtenerRecolectadas(int idJugador) { return List.copyOf(estado.getJugadores().get(idJugador).getRecolectadas()); }
    @Override public List<List<Carta>> obtenerRecolectadasTodos() {
        List<List<Carta>> res = new ArrayList<>();
        for (Jugador j : estado.getJugadores()) res.add(List.copyOf(j.getRecolectadas()));
        return res;
    }
    @Override public boolean esUltimaRonda() { return estado.isUltimaRonda(); }

    /** Fin de partida: última ronda activada y todas las manos en 4. */
    @Override
    public boolean esFinDePartida() {
        boolean manosEn4 = estado.getJugadores().stream().allMatch(j -> j.getMano().size() == 4);
        return estado.isUltimaRonda() && manosEn4;
    }

    /** Cálculo de puntajes siguiendo reglas de 2 jugadores. */
    @Override
    public List<Integer> puntajesPorJugador() {
        Map<ColorCarta, List<Integer>> conteo = new EnumMap<>(ColorCarta.class);
        for (ColorCarta c : ColorCarta.values()) {
            conteo.put(c, new ArrayList<>(Collections.nCopies(estado.getJugadores().size(), 0)));
        }
        List<Integer> sumaValores = new ArrayList<>(Collections.nCopies(estado.getJugadores().size(), 0));

        for (int i = 0; i < estado.getJugadores().size(); i++) {
            for (Carta k : estado.getJugadores().get(i).getRecolectadas()) {
                conteo.get(k.getColor()).set(i, conteo.get(k.getColor()).get(i) + 1);
                sumaValores.set(i, sumaValores.get(i) + k.getValor());
            }
        }

        List<Integer> puntos = new ArrayList<>(Collections.nCopies(estado.getJugadores().size(), 0));
        for (ColorCarta c : ColorCarta.values()) {
            List<Integer> xs = conteo.get(c);
            int max = xs.stream().mapToInt(Integer::intValue).max().orElse(0);

            if (estado.getJugadores().size() == 2) { // regla especial 2p
                int a = xs.get(0), b = xs.get(1);
                if (a >= b + 2) puntos.set(0, puntos.get(0) + a);
                else if (b >= a + 2) puntos.set(1, puntos.get(1) + b);
            } else { // (no usamos, pero queda correcto para >2)
                for (int i = 0; i < xs.size(); i++) if (xs.get(i) == max && max > 0) {
                    puntos.set(i, puntos.get(i) + xs.get(i));
                }
            }
        }
        for (int i = 0; i < puntos.size(); i++) puntos.set(i, puntos.get(i) + sumaValores.get(i));
        return puntos;
    }

    @Override public List<String> nombresJugadores() {
        List<String> n = new ArrayList<>();
        for (Jugador j : estado.getJugadores()) n.add(j.getNombre());
        return n;
    }

    // ---------------- Guardar/Cargar partida (simple) ----------------

    /** Serializa el {@link EstadoPartida} a disco. No valida jugadores conectados. */
    @Override
    public boolean guardarPartida(String archivo) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivo))) {
            oos.writeObject(estado);
            notificarObservadores(Evento.MENSAJE);
            return true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error guardando partida en " + archivo, e);
            return false;
        }
    }

    /** Carga el {@link EstadoPartida} desde disco y notifica inicio para que la UI reconstruya. */
    @Override
    public boolean cargarPartida(String archivo) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            EstadoPartida e = (EstadoPartida) ois.readObject();
            estado.getJugadores().clear(); estado.getJugadores().addAll(e.getJugadores());
            estado.getMazo().clear();      estado.getMazo().addAll(e.getMazo());
            estado.getCarnaval().clear();  estado.getCarnaval().addAll(e.getCarnaval());
            estado.setTurnoActual(e.getTurnoActual());
            estado.setUltimaRonda(e.isUltimaRonda());
            estado.setIniciada(true);

            notificarObservadores(Evento.INICIO_PARTIDA); // UI: reconstruir vistas con el snapshot
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            LOG.log(Level.SEVERE, "Error cargando partida desde " + archivo, ex);
            return false;
        }
    }

    // ---------------- Ranking ----------------

    /** Registra snapshot de resultados y persiste Top-5. */
    @Override
    public void registrarResultadoEnRanking() throws RemoteException {
        Map<String, Integer> puntajes = calcularPuntajesFinales();
        tablaRanking.registrarResultado(puntajes);
        tablaRanking.guardarEn(RANK_FILE);
        notificarObservadores(Evento.RANKING_ACTUALIZADO);
    }

    @Override public List<EntradaRanking> top5() { return tablaRanking.top5(); }
}
