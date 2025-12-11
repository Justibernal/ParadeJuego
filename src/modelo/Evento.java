package modelo;

import java.io.Serializable;

/**
 * Eventos de la capa de aplicación (Observable).
 * Cada evento indica a las vistas qué se actualizó del modelo.
 */
public enum Evento implements Serializable {
    /** Se unió un jugador nuevo (actualizar listado y estado de lobby). */
    JUGADOR_UNIDO,

    /** Comenzó la partida (refrescar manos iniciales, carnaval y turno). */
    INICIO_PARTIDA,

    /** Se jugó una carta (actualizar carnaval, recogidas del jugador y mano). */
    CARTA_JUGADA,

    /** Se activó la última ronda (informar banner/diálogo y estado). */
    ULTIMA_RONDA,

    /** La partida terminó (bloquear acciones y mostrar resumen). */
    FIN_PARTIDA,

    /** El ranking cambió (refrescar Top-5). */
    RANKING_ACTUALIZADO,

    /** Mensaje informativo genérico (toast/alert). */
    MENSAJE,

    /** Están disponibles los puntajes finales (mostrar panel de puntajes). */
    PUNTAJES_FINALES
}

