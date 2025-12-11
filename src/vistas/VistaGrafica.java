package vistas;

import controladores.ControladorParade;
import modelo.Carta;
import modelo.EntradaRanking;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class VistaGrafica extends JFrame implements IVistaParade {
    private final ControladorParade controlador;

    // ===== Estado UI =====
    private String yoNombre = null;
    private boolean partidaActiva = false;

    // ===== Top bar / controles =====
    private final DefaultListModel<String> jugadoresModel = new DefaultListModel<>();
    private final JTextField nombreField = new JTextField(12);
    private final JButton btnIniciar  = new JButton("Iniciar");
    private final JButton btnPuntajes = new JButton("Puntajes");
    private final JButton btnRanking  = new JButton("Ranking");
    private final JButton btnGuardar  = new JButton("Guardar");
    private final JButton btnCargar   = new JButton("Cargar");
    private final JButton btnJugar    = new JButton("Jugar seleccionada");
    private final JLabel  turnoLbl    = new JLabel("-");

    // ===== Modelos y listas =====
    private final DefaultListModel<Carta> manoModel     = new DefaultListModel<>();
    private final DefaultListModel<Carta> carnavalModel = new DefaultListModel<>();
    private final JList<Carta> manoList     = new JList<>(manoModel);
    private final JList<Carta> carnavalList = new JList<>(carnavalModel);

    private final DefaultListModel<Carta> propiasRecModel = new DefaultListModel<>();
    private final JList<Carta> propiasRecList = new JList<>(propiasRecModel);

    // === NUEVO: contenedor de filas por rival (texto) ===
    private final JPanel rivalsContainer = new JPanel();
    private final Map<String, JLabel> rivalTextLabels = new HashMap<>();

    public VistaGrafica(ControladorParade c) {
        super("Parade");
        this.controlador = c;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationByPlatform(true);

        // ====== TOP (nombre, turno, jugadores) ======
        var topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topRow.add(new JLabel("Nombre:"));
        topRow.add(nombreField);
        topRow.add(new JLabel(" | Turno: "));
        topRow.add(turnoLbl);
        topRow.add(new JLabel(" | Jugadores: "));
        var jugadoresInline = new JList<>(jugadoresModel);
        jugadoresInline.setVisibleRowCount(1);
        jugadoresInline.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        jugadoresInline.setFixedCellHeight(18);
        jugadoresInline.setFixedCellWidth(80);
        jugadoresInline.setEnabled(false);
        topRow.add(jugadoresInline);

        // ====== TOOLBAR ======
        var toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.add(btnIniciar);
        toolbar.add(btnJugar);
        toolbar.add(btnPuntajes);
        toolbar.add(btnRanking);
        toolbar.add(btnGuardar);
        toolbar.add(btnCargar);

        btnPuntajes.setEnabled(false);
        btnRanking.setEnabled(true);
        btnJugar.setEnabled(false);

        // ====== Renderers ======
        var rndCarnaval = new CardRenderer(120);
        var rndMano     = new CardRenderer(120);
        var rndRec      = new CardRenderer(84);

        // ====== Config base ======
        configListBase(carnavalList);
        configListBase(manoList);
        configListBase(propiasRecList);

        // ====== Scroll panes ======
        JScrollPane spCarnaval = new JScrollPane(
                carnavalList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        JScrollPane spMano = new JScrollPane(
                manoList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        JScrollPane spPropias = new JScrollPane(
                propiasRecList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );

        // === NUEVO: contenedor vertical para todos los rivales (texto) ===
        rivalsContainer.setLayout(new GridLayout(0, 1, 0, 6)); // N filas
        JScrollPane spRivals = new JScrollPane(
                rivalsContainer,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );

        for (var sp : new JScrollPane[]{spCarnaval, spMano, spPropias, spRivals}) {
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.getViewport().setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(24);
            sp.getHorizontalScrollBar().setUnitIncrement(24);
        }

        // Setear renderers (solo donde hay imágenes)
        carnavalList.setCellRenderer(rndCarnaval);
        manoList.setCellRenderer(rndMano);
        propiasRecList.setCellRenderer(rndRec);

        // Habilitar "Jugar" solo si hay selección y es tu turno
        manoList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnJugar.setEnabled(
                        partidaActiva &&
                                turnoLbl.getText().equalsIgnoreCase(yoNombre) &&
                                manoList.getSelectedIndex() >= 0
                );
            }
        });

        // Autoscale (solo listas con imágenes)
        bindAutoScale(spCarnaval, carnavalList, rndCarnaval);
        bindAutoScale(spMano,     manoList,     rndMano);
        bindAutoScale(spPropias,  propiasRecList, rndRec);

        // Secciones
        var pCarnaval = wrap("Carnaval", spCarnaval);
        var pMano     = wrap("Tu mano",  spMano);
        var pPropias  = wrap("Tus recogidas",        spPropias);
        var pRivales  = wrap("Recogidas de rivales", spRivals);

        // Disposición
        var header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(topRow);
        header.add(toolbar);

        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));

        content.add(header);
        content.add(Box.createVerticalStrut(4));
        content.add(pCarnaval);
        content.add(Box.createVerticalStrut(6));
        content.add(pMano);
        content.add(Box.createVerticalStrut(6));

        var bottomRows = new JPanel(new GridLayout(1, 2, 10, 0));
        bottomRows.add(pPropias);
        bottomRows.add(pRivales);
        bottomRows.setPreferredSize(new Dimension(0, 160));
        content.add(bottomRows);

        var root = new JPanel(new BorderLayout());
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // --- listeners ---
        btnIniciar.addActionListener(ignored -> controlador.iniciarPartida(6, 5));
        btnPuntajes.addActionListener(ignored -> controlador.verPuntajes());
        btnRanking.addActionListener(ignored -> controlador.mostrarRanking());
        btnGuardar.addActionListener(ignored -> {
            var f = elegirArchivo(true);
            if (f != null) controlador.guardar(f);
        });
        btnCargar.addActionListener(ignored -> {
            var f = elegirArchivo(false);
            if (f != null) controlador.cargar(f);
        });
        btnJugar.addActionListener(ignored -> {
            int idx = manoList.getSelectedIndex();
            if (idx >= 0) controlador.jugarCarta(idx);
            else mostrarInfo("Seleccioná una carta de tu mano.");
        });
    }

    // ---------- helpers UI ----------
    private static JPanel wrap(String title, Component c) {
        var p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static <E> void configListBase(JList<E> list) {
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionBackground(new Color(0, 0, 255, 60));
        list.setSelectionForeground(list.getForeground());
        list.setFixedCellHeight(1);
        list.setFixedCellWidth(1);
    }

    private static final double CARD_ASPECT = 0.68;

    private static <E> void bindAutoScale(JScrollPane sp, JList<E> list, CardRenderer renderer) {
        Runnable apply = () -> {
            int vh = sp.getViewport().getHeight();   // altura visible real
            if (vh <= 0) return;

            final int cellPadding = 4;               // 2px arriba + 2px abajo del renderer
            int cellH = Math.max(32, vh);            // la celda ocupa TODO el viewport

            renderer.setTargetHeight(cellH - cellPadding);          // alto de la carta
            list.setFixedCellHeight(cellH);                         // alto de la celda = viewport
            list.setFixedCellWidth((int) Math.round((cellH - cellPadding) * CARD_ASPECT) + 8);

            list.revalidate();
            list.repaint();
        };

        // Escuchamos el viewport (no el scrollpane)
        sp.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { apply.run(); }
        });

        SwingUtilities.invokeLater(apply);
    }


    private String elegirArchivo(boolean guardar) {
        var fc = new JFileChooser();
        fc.setDialogTitle(guardar ? "Guardar partida" : "Cargar partida");
        int r = guardar ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
        return (r == JFileChooser.APPROVE_OPTION) ? fc.getSelectedFile().getAbsolutePath() : null;
    }

    // ======== Rivales (texto) ========
    private void ensureRivalRow(String nombreRival) {
        if (rivalTextLabels.containsKey(nombreRival)) return;

        // fila con nombre y texto de cartas
        var row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));

        var lblNombre = new JLabel(nombreRival);
        lblNombre.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
        var lblCartas = new JLabel("-");
        lblCartas.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lblCartas.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 2));

        row.add(lblNombre);
        row.add(lblCartas);

        rivalsContainer.add(row);
        rivalTextLabels.put(nombreRival, lblCartas);

        rivalsContainer.revalidate();
        rivalsContainer.repaint();
    }

    private static String fmtCarta(Carta c) {
        // COLOR-valor (en mayúsculas)
        return c.getColor().name() + "-" + c.getValor();
    }

    // ---------- IVistaParade ----------
    @Override
    public void iniciar() {
        setVisible(true);
        String nombre = JOptionPane.showInputDialog(this, "Tu nombre:");
        if (nombre != null && !nombre.isBlank()) {
            nombre = nombre.trim();
            nombreField.setText(nombre);
            nombreField.setEditable(false);
            controlador.autoUnirConNombre(nombre);
        } else {
            mostrarInfo("Ingresá un nombre para participar.");
        }
    }

    @Override
    public void mostrarMano(List<Carta> mano) {
        manoModel.clear();
        for (Carta c : mano) manoModel.addElement(c);
    }

    @Override
    public void mostrarCarnaval(List<Carta> carnaval) {
        carnavalModel.clear();
        for (Carta c : carnaval) carnavalModel.addElement(c);
    }

    @Override
    public void mostrarRecolectadasPropias(List<Carta> propias) {
        propiasRecModel.clear();
        for (Carta c : propias) propiasRecModel.addElement(c);
    }

    @Override
    public void mostrarRecolectadasOponentes(List<String> nombres, List<List<Carta>> pilasPorJugador) {
        // Asegurar filas y actualizar texto
        for (int i = 0; i < nombres.size(); i++) {
            String nombre = nombres.get(i);
            if (yoNombre != null && nombre.equalsIgnoreCase(yoNombre)) continue;

            ensureRivalRow(nombre);
            List<Carta> pila = (i < pilasPorJugador.size()) ? pilasPorJugador.get(i) : List.of();
            String texto = pila.isEmpty()
                    ? "(sin cartas)"
                    : String.join(", ", pila.stream().map(VistaGrafica::fmtCarta).toList());
            rivalTextLabels.get(nombre).setText(texto);
        }
    }

    @Override
    public void setControlesHabilitados(boolean on) {
        this.partidaActiva = on;
        btnPuntajes.setEnabled(on);
        btnJugar.setEnabled(on && turnoLbl.getText().equalsIgnoreCase(yoNombre));
        btnIniciar.setEnabled(!on);
    }

    @Override
    public void mostrarJugadores(List<String> nombres, String yo) {
        yoNombre = yo;
        jugadoresModel.clear();
        for (String n : nombres) jugadoresModel.addElement(n.equalsIgnoreCase(yo) ? n + " (yo)" : n);
    }

    @Override
    public void mostrarTurno(String nombre) {
        turnoLbl.setText(nombre);
        btnJugar.setEnabled(partidaActiva && nombre.equalsIgnoreCase(yoNombre));
    }

    @Override
    public void mostrarPuntajes(List<String> jugadores, List<Integer> puntajes) {
        var sb = new StringBuilder("Puntajes:\n");
        for (int i = 0; i < jugadores.size(); i++) {
            sb.append("- ").append(jugadores.get(i)).append(": ")
                    .append(i < puntajes.size() ? puntajes.get(i) : 0).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Puntajes", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void mostrarRanking(List<EntradaRanking> top) {
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
        JScrollPane sp = new JScrollPane(table);
        JDialog dlg = new JDialog(this, "Top 5 - Ranking", true);
        dlg.getContentPane().add(sp);
        dlg.setSize(360, 220);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    @Override public void mostrarError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    @Override public void mostrarInfo (String msg) { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }

    // ---------- Renderer (para listas con imágenes) ----------
    private static class CardRenderer extends DefaultListCellRenderer {
        private int targetHeight;
        private final Map<String, ImageIcon> cache = new HashMap<>();

        CardRenderer(int targetHeight) { this.targetHeight = targetHeight; }
        void setTargetHeight(int h) { if (h > 0 && h != this.targetHeight) { this.targetHeight = h; cache.clear(); } }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);

            if (value instanceof Carta c) {
                final int h = isSelected ? targetHeight + 8 : targetHeight;
                ImageIcon icon = loadIcon(c, h);
                if (icon != null) {
                    lbl.setIcon(icon);
                    lbl.setText("");
                } else {
                    lbl.setText(c.toString());
                    lbl.setHorizontalAlignment(CENTER);
                    lbl.setVerticalAlignment(CENTER);
                }
            }

            if (isSelected) {
                Color brand = new Color(25, 118, 210);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(brand, 2, true),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                ));
                lbl.setOpaque(false);
            } else {
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                lbl.setOpaque(false);
            }

            lbl.setBackground(list.getBackground());
            lbl.setForeground(list.getForeground());
            return lbl;
        }

        private ImageIcon loadIcon(Carta c, int height) {
            String key = c.getColor().name().toLowerCase() + "_" + c.getValor() + "_h" + height;
            if (cache.containsKey(key)) return cache.get(key);

            String color = c.getColor().name().toLowerCase();
            int v = c.getValor();
            String[] candidates = {
                    "/resources/cartas/%s_%d.jpeg", "/resources/cartas/%s_%d.jpg",
                    "/resources.cartas/%s_%d.jpeg", "/resources.cartas/%s_%d.jpg"
            };

            ImageIcon icon = null;
            for (String pat : candidates) {
                String path = String.format(pat, color, v);
                URL url = getClass().getResource(path);
                if (url != null) {
                    Image img = new ImageIcon(url).getImage();
                    int w = (int) Math.round(img.getWidth(null) * (height / (double) img.getHeight(null)));
                    Image scaled = img.getScaledInstance(w, height, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaled);
                    break;
                }
            }
            cache.put(key, icon);
            return icon;
        }
    }
}



