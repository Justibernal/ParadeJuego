package vistas;

import modelo.Carta;
import modelo.EntradaRanking;
import java.util.List;

public interface IVistaParade {
    void iniciar();
    void mostrarMano(List<Carta> mano);
    void mostrarCarnaval(List<Carta> carnaval);
    void mostrarTurno(String nombre);
    void mostrarPuntajes(List<String> jugadores, List<Integer> puntajes);
    void mostrarError(String msg);
    void mostrarInfo(String msg);
    void mostrarRecolectadasPropias(List<Carta> propias);
    void mostrarRecolectadasOponentes(List<String> nombres, List<List<Carta>> pilasPorJugador);
    
    void setControlesHabilitados(boolean on);
    void mostrarJugadores(List<String> nombres, String yo);
    void mostrarRanking(List<EntradaRanking> top);

}
