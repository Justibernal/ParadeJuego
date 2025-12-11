package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Valor inmutable del juego: color + valor (0..10).
 * Importante: implementa equals/hashCode para funcionar bien con
 * List#contains, List#removeAll y comparaciones por conjuntos en el Modelo.
 */
public final class Carta implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ColorCarta color;
    private final int valor; // 0..10

    public Carta(ColorCarta color, int valor) {
        if (color == null) throw new IllegalArgumentException("color no puede ser null");
        if (valor < 0 || valor > 10) throw new IllegalArgumentException("valor fuera de rango (0..10)");
        this.color = color;
        this.valor = valor;
    }

    public ColorCarta getColor() { return color; }
    public int getValor() { return valor; }

    @Override
    public String toString() { return color + "(" + valor + ")"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Carta c)) return false;
        return valor == c.valor && color == c.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, valor);
    }
}



