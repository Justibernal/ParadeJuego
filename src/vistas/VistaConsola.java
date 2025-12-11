package vistas;

import controladores.ControladorParade;
import modelo.Carta;
import modelo.ColorCarta;
import modelo.EntradaRanking;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class VistaConsola extends JFrame implements IVistaParade {
    // ===== Estado =====
    private final ControladorParade c;
    private final JTextArea out = new JTextArea();
    private final JTextField in = new JTextField();

    private String yoNombre   = null;
    private boolean partidaActiva = false;
    private String turnoActual = "-";

    // ===== Snapshots de líneas (para re-render limpio) =====
    private String lastJugadoresLine   = "(sin jugadores)";
    private String lastCarnavalLine    = "Carnaval: (vacío)";
    private String lastManoLine        = "Tu mano: (vacía)";
    private String lastPropiasLine     = "Tus recogidas: (vacías)";
    private String lastRivalesBlock    = "Rivales (recogidas):\n";

    // ===== Constructores =====
    public VistaConsola(ControladorParade c) {
        super("Parade - Consola");
        this.c = c;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 520);
        setLocationByPlatform(true);

        out.setEditable(false);
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        var p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(out), BorderLayout.CENTER);
        p.add(in, BorderLayout.SOUTH);
        setContentPane(p);

        in.addActionListener(e -> {
            String cmd = e.getActionCommand().trim();
            in.setText("");
            ejecutar(cmd);
        });
    }

    // ===== Ciclo de vida =====
    @Override
    public void iniciar() {
        setVisible(true);
        printHeader();

        String nombre = JOptionPane.showInputDialog(this, "Tu nombre:");
        if (nombre != null && !nombre.isBlank()) {
            nombre = nombre.trim();
            yoNombre = nombre;
            c.autoUnirConNombre(nombre);
        } else {
            println("Tip: reiniciá y colocá tu nombre para unirte.");
        }
    }

    // ===== Header (lista corta de comandos) =====
    private void printHeader() {
        limpiarPantalla();
        println("=== Parade ===");
        println("Comandos:");
        println("  iniciar           – iniciar partida (6 en carnaval / 5 por jugador)");
        println("  jugar <idx>       – jugar carta de tu mano por índice");
        println("  puntajes          – ver puntajes actuales");
        println("  ranking           – ver Top 5 del ranking");
        println("  guardar <archivo> – guardar partida");
        println("  cargar <archivo>  – cargar partida");
        println("────────────────────────────────────────────────────────");
        // bloque inicial vacío
        redraw();
    }

    // ===== Entrada de comandos =====
    private void ejecutar(String cmd) {
        if (cmd.isBlank()) return;

        String[] p = cmd.split("\\s+");
        switch (p[0].toLowerCase()) {
            case "iniciar" -> c.iniciarPartida(6, 5);

            case "jugar" -> {
                if (!partidaActiva) { toast("La partida no está iniciada."); break; }
                if (yoNombre == null || !yoNombre.equalsIgnoreCase(turnoActual)) {
                    toast("No es tu turno."); break;
                }
                if (p.length >= 2) {
                    try { c.jugarCarta(Integer.parseInt(p[1])); }
                    catch (NumberFormatException ex) { toast("Índice inválido."); }
                } else {
                    toast("Uso: jugar <idx>");
                }
            }

            case "puntajes" -> c.verPuntajes();
            case "ranking"  -> c.mostrarRanking();

            case "guardar" -> {
                if (p.length >= 2) c.guardar(cmd.substring(8).trim());
                else toast("Uso: guardar <archivo>");
            }

            case "cargar" -> {
                if (p.length >= 2) c.cargar(cmd.substring(7).trim());
                else toast("Uso: cargar <archivo>");
            }

            default -> toast("Comando no reconocido. Escribí: iniciar | jugar <idx> | puntajes | ranking | guardar | cargar");
        }
    }

    // ===== Helpers I/O =====
    private void print(String s){ out.append(s); out.setCaretPosition(out.getDocument().getLength()); }
    private void println(String s){ print(s + "\n"); }
    private void limpiarPantalla(){ out.setText(""); }
    private void toast(String s){ println(s); } // mensaje breve; será reemplazado en el próximo redraw

    // ===== Redibujado SIEMPRE automático =====
    private void redraw() {
        // Re-dibuja todo el tablero con lo último recibido
        limpiarPantalla();
        println("=== Parade ===");
        println("Comandos: iniciar | jugar <idx> | puntajes | ranking | guardar | cargar");
        println("────────────────────────────────────────────────────────");

        String tagTurno = (yoNombre != null && yoNombre.equalsIgnoreCase(turnoActual)) ? " (tu turno)" : "";
        println("Turno: " + turnoActual + tagTurno);
        println(lastJugadoresLine);
        println("────────────────────────────────────────────────────────");

        println(lastCarnavalLine);
        println(lastManoLine);
        println("");
        println(lastPropiasLine);
        print(lastRivalesBlock); // ya trae su \n final si corresponde
    }

    // ===== Formateo de listas =====
    private String indexar(List<Carta> xs) {
        var sb = new StringBuilder();
        for (int i = 0; i < xs.size(); i++) {
            sb.append("[").append(i).append("] ").append(xs.get(i));
            if (i < xs.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // ===== Agrupación por color (orden + total) =====
    private static final ColorCarta[] ORDEN_COLORES = {
            ColorCarta.VERDE, ColorCarta.AZUL, ColorCarta.AMARILLO,
            ColorCarta.ROJO, ColorCarta.MORADO, ColorCarta.NEGRO
    };

    private String etiqueta(ColorCarta c) {
        return switch (c) {
            case VERDE -> "VERDE";
            case AZUL -> "AZUL";
            case AMARILLO -> "AMARILLO";
            case ROJO -> "ROJO";
            case MORADO -> "MORADO";
            case NEGRO -> "NEGRO";
        };
    }

    private String agruparPorColor(List<Carta> cartas) {
        Map<ColorCarta, Integer> m = new EnumMap<>(ColorCarta.class);
        for (Carta c : cartas) m.merge(c.getColor(), 1, Integer::sum);

        var partes = new ArrayList<String>();
        int total = 0;
        for (ColorCarta col : ORDEN_COLORES) {
            Integer k = m.get(col);
            if (k != null && k > 0) {
                partes.add(etiqueta(col) + " × " + k);
                total += k;
            }
        }
        return partes.isEmpty() ? "(vacías)" : String.join(", ", partes) + "   [total: " + total + "]";
    }

    // ===== Implementación IVistaParade =====
    @Override
    public void mostrarMano(List<Carta> mano) {
        lastManoLine = "Tu mano: " + (mano.isEmpty() ? "(vacía)" : indexar(mano));
        redraw();
    }

    @Override
    public void mostrarCarnaval(List<Carta> carnaval) {
        lastCarnavalLine = "Carnaval: " + (carnaval.isEmpty() ? "(vacío)" : indexar(carnaval));
        redraw();
    }

    @Override
    public void mostrarTurno(String nombre) {
        turnoActual = nombre;
        redraw();
    }

    @Override
    public void mostrarPuntajes(List<String> jugadores, List<Integer> puntajes) {
        // Muestra un panel de puntajes (y luego re-dibuja el tablero)
        var dlg = new JDialog(this, "Puntajes", true);
        var area = new JTextArea();
        area.setEditable(false);
        var sb = new StringBuilder("PUNTAJES\n");
        for (int i = 0; i < jugadores.size(); i++) {
            int p = (i < puntajes.size() ? puntajes.get(i) : 0);
            sb.append(String.format("  - %-10s %3d%n", jugadores.get(i), p));
        }
        area.setText(sb.toString());
        dlg.add(new JScrollPane(area));
        dlg.setSize(260, 240);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        redraw();
    }

    @Override
    public void mostrarRanking(List<EntradaRanking> top) {
        var dlg = new JDialog(this, "Top 5 - Ranking", true);
        String[] cols = {"Posición", "Jugador", "Puntaje"};
        Object[][] data = new Object[top.size()][3];
        for (int i = 0; i < top.size(); i++) {
            var r = top.get(i);
            data[i][0] = i + 1;
            data[i][1] = r.getNombre();
            data[i][2] = r.getPuntaje();
        }
        JTable table = new JTable(data, cols);
        table.setEnabled(false);
        dlg.add(new JScrollPane(table));
        dlg.setSize(360, 220);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        redraw();
    }

    @Override public void mostrarError(String msg) { toast("[ERROR] " + msg); redraw(); }
    @Override public void mostrarInfo (String msg) { toast("[INFO] " + msg);  redraw(); }

    @Override
    public void mostrarRecolectadasPropias(List<Carta> propias) {
        lastPropiasLine = "Tus recogidas: " + agruparPorColor(propias);
        redraw();
    }

    @Override
    public void mostrarRecolectadasOponentes(List<String> nombres, List<List<Carta>> pilasPorJugador) {
        var sb = new StringBuilder();
        sb.append("Rivales (recogidas):\n");

        if (nombres != null) {
            for (int i = 0; i < nombres.size(); i++) {
                String nombre = nombres.get(i);

                // ⬇️ Filtrar mi propio nombre
                if (nombre.equalsIgnoreCase(yoNombre)) {
                    continue;
                }


                List<Carta> pila = (pilasPorJugador != null && i < pilasPorJugador.size())
                        ? pilasPorJugador.get(i)
                        : List.of();

                sb.append("  ").append(nombre).append(": ")
                        .append(agruparPorColor(pila)).append("\n");
            }
        }

        lastRivalesBlock = sb.toString();
        redraw();
    }


    @Override
    public void setControlesHabilitados(boolean on) {
        this.partidaActiva = on; // solo guardamos estado
        redraw();
    }

    @Override
    public void mostrarJugadores(List<String> nombres, String yo) {
        this.yoNombre = yo;
        var sb = new StringBuilder("Jugadores: ");
        for (int i = 0; i < nombres.size(); i++) {
            String n = nombres.get(i);
            if (n.equalsIgnoreCase(yo)) n = n + " (yo)";
            sb.append(n);
            if (i < nombres.size() - 1) sb.append(", ");
        }
        lastJugadoresLine = sb.toString();
        redraw();
    }
}


