package controladores;

import modelo.*;
import vistas.IVistaParade;
import ar.edu.unlu.rmimvc.cliente.IControladorRemoto;
import ar.edu.unlu.rmimvc.observer.IObservableRemoto;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador RMI-MVC: traduce acciones de la vista a llamadas del modelo remoto
 * y reacciona a eventos (ObservableRemoto) refrescando la UI en el EDT.
 */
public class ControladorParade implements IControladorRemoto {
    private static final Logger LOG = Logger.getLogger(ControladorParade.class.getName());

    private IParade modelo;
    private IVistaParade vista;
    private Integer idJugador;      // índice que asigna el modelo al unirse
    private String nombreJugador;   // alias local elegido en la vista

    public void setVista(IVistaParade vista) { this.vista = vista; }
    public void setNombreJugador(String nombre) { this.nombreJugador = nombre; }

    // === Acciones desde la vista ===

    /** Refresca lista de jugadores y habilita/deshabilita controles. */
    private void syncJugadoresYControles() {
        try {
            var nombres = modelo.nombresJugadores();
            vista.mostrarJugadores(nombres, nombreJugador);
            boolean enCurso = modelo.partidaIniciada();
            // Mejora: habilitar solo si partida en curso y el usuario ya está unido
            vista.setControlesHabilitados(enCurso && idJugador != null);
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    /** Atajo para setear nombre y unirse. */
    public void autoUnirConNombre(String nombre) {
        setNombreJugador(nombre);
        unirJugadorSiHaceFalta();
    }

    /** Se une si aún no se unió. */
    public void unirJugadorSiHaceFalta() {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        if (idJugador != null) { syncJugadoresYControles(); return; }
        if (nombreJugador == null || nombreJugador.isBlank()) {
            vista.mostrarError("Ingresá tu nombre.");
            return;
        }
        try {
            idJugador = modelo.unirJugador(nombreJugador);
            vista.mostrarInfo("Te uniste como " + nombreJugador + " (id=" + idJugador + ")");
            syncJugadoresYControles();
            refrescar();
        } catch (RemoteException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    // === Ranking ===
    public void mostrarRanking() {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            List<EntradaRanking> top = modelo.top5();
            vista.mostrarRanking(top);
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    // === Observer del modelo remoto ===
    @Override
    public void actualizar(IObservableRemoto o, Object arg) throws RemoteException {
        if (!(arg instanceof Evento ev)) return;

        Runnable r = () -> {
            try {
                switch (ev) {
                    case JUGADOR_UNIDO       -> syncJugadoresYControles();
                    case INICIO_PARTIDA      -> { syncJugadoresYControles(); refrescar(); }
                    case CARTA_JUGADA        -> refrescar();
                    case ULTIMA_RONDA        -> vista.mostrarInfo("¡Última ronda!");
                    case PUNTAJES_FINALES    -> verPuntajes();        // <- NUEVO: mostrar cierre apenas llega
                    case FIN_PARTIDA         -> { /* el modelo ya envió puntajes; nada extra acá */ }
                    case RANKING_ACTUALIZADO -> mostrarRanking();
                    case MENSAJE             -> { /* reservado para toasts/infos generales si querés */ }
                }
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Fallo en actualizar()", e);
                String msg = (e.getMessage() == null) ? e.getClass().getSimpleName() : e.getMessage();
                try { vista.mostrarError("Fallo en actualizar: " + msg); } catch (Throwable ignore) {}
            }
        };

        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    public void iniciarPartida(int cartasMesa, int cartasPorJugador) {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            modelo.iniciarPartida(cartasMesa, cartasPorJugador);
            syncJugadoresYControles();
            refrescar();
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    public void jugarCarta(int indiceEnMano) {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        if (idJugador == null) { vista.mostrarError("Primero unite a la partida."); return; }
        try {
            // Validación suave de índice (mejor UX)
            List<Carta> mano = modelo.obtenerMano(idJugador);
            if (indiceEnMano < 0 || indiceEnMano >= mano.size()) {
                vista.mostrarError("Elegí una carta válida de tu mano.");
                return;
            }
            modelo.jugarCarta(idJugador, indiceEnMano);
        } catch (IllegalStateException ex) {
            String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                    ? ex.getMessage() : "Jugada inválida según el reglamento.";
            vista.mostrarError(msg);
        } catch (RemoteException ex) {
            vista.mostrarError("Error de conexión");
        }
    }

    public void refrescar() {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            List<Carta> mano = (idJugador == null) ? List.of() : modelo.obtenerMano(idJugador);
            vista.mostrarMano(mano);
            vista.mostrarCarnaval(modelo.obtenerCarnaval());
            vista.mostrarTurno(modelo.obtenerTurnoNombre());

            if (idJugador != null) {
                var propias = modelo.obtenerRecolectadas(idJugador);
                vista.mostrarRecolectadasPropias(propias);

                var nombres = modelo.nombresJugadores();
                var pilas = modelo.obtenerRecolectadasTodos();
                vista.mostrarRecolectadasOponentes(nombres, pilas);
            }
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    public void guardar(String archivo) {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            boolean ok = modelo.guardarPartida(archivo);
            vista.mostrarInfo(ok ? "Partida guardada." : "No se pudo guardar.");
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    public void cargar(String archivo) {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            boolean ok = modelo.cargarPartida(archivo);
            vista.mostrarInfo(ok ? "Partida cargada." : "No se pudo cargar.");
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    public void verPuntajes() {
        if (modelo == null) { vista.mostrarError("No conectado al servidor."); return; }
        try {
            var pts  = modelo.puntajesPorJugador();
            var noms = modelo.nombresJugadores();
            vista.mostrarPuntajes(noms, pts);
        } catch (RemoteException e) {
            vista.mostrarError("Error de conexión");
        }
    }

    // === Utilidades ===

    /** Si se cargó una partida con otra lista de jugadores, reasigna mi id según mi nombre. */
    private void reidentificarId() {
        if (nombreJugador == null || modelo == null) return;
        try {
            List<String> nombres = modelo.nombresJugadores();
            int idx = nombres.indexOf(nombreJugador);
            this.idJugador = (idx >= 0) ? idx : null; // si no está, queda null y la vista lo indicará
        } catch (RemoteException e) {
            // si falla, lo dejamos como estaba
        }
    }

    // === RMI-MVC ===
    @Override
    public <T extends IObservableRemoto> void setModeloRemoto(T modeloRemoto) {
        this.modelo = (IParade) modeloRemoto;
    }
}
