package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public final class TablaClasificacion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<EntradaRanking> entradas = new ArrayList<>();

    /** Registra un resultado completo: mapa nombre -> puntaje final. */
    public synchronized void registrarResultado(Map<String, Integer> puntajes) {
        if (puntajes == null || puntajes.isEmpty()) return;
        final long ahora = System.currentTimeMillis();
        for (var e : puntajes.entrySet()) {
            final String nombre = e.getKey();
            final Integer puntaje = e.getValue();
            if (nombre == null || puntaje == null || puntaje < 0) continue;
            entradas.add(new EntradaRanking(nombre, puntaje, ahora));
        }
    }

    /** Devuelve el Top-5 ordenado por compareTo (puntaje asc, empate por instante). */
    public synchronized List<EntradaRanking> top5() {
        var copia = new ArrayList<>(entradas);
        Collections.sort(copia);
        if (copia.size() > 5) copia = new ArrayList<>(copia.subList(0, 5));
        return List.copyOf(copia);
    }

    // === Persistencia simple a disco ===
    public synchronized boolean guardarEn(String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(new ArrayList<>(entradas)); // guardo una copia
            return true;
        } catch (IOException e) {
            return false; // si falla, no interrumpe el juego
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void cargarDe(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            var data = (List<EntradaRanking>) ois.readObject();
            entradas.clear();
            if (data != null) entradas.addAll(data);
        } catch (Exception ignore) {
            // si no existe o hay error, arrancamos vacío
        }
    }

}

