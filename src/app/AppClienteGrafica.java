package app;

import controladores.ControladorParade;
import vistas.IVistaParade;
import vistas.VistaGrafica;
import ar.edu.unlu.rmimvc.RMIMVCException;
import ar.edu.unlu.rmimvc.cliente.Cliente;

import javax.swing.*;
import java.rmi.RemoteException;

public class AppClienteGrafica {
    public static void main(String[] args) {
        // Handler global (opcional pero recomendado)
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            try {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null,
                                "Error:\n" + e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage()),
                                "Excepción no capturada", JOptionPane.ERROR_MESSAGE));
            } catch (Throwable ignored) {}
        });

        String portStr = JOptionPane.showInputDialog(null, "Puerto del cliente:", "10000");
        if (portStr == null || portStr.isBlank()) return;
        int basePort = Integer.parseInt(portStr);

        ControladorParade c = new ControladorParade();
        IVistaParade v = new VistaGrafica(c);
        c.setVista(v);

        final String hostServidor = "127.0.0.1";
        final String hostCliente  = "127.0.0.1";
        final int    puertoServidorRegistry = 8888;

        boolean conectado = false;
        Exception ultimoError = null;

        for (int intento = 0; intento < 5 && !conectado; intento++) {
            int puertoClienteLocal = basePort + intento;
            try {
                ar.edu.unlu.rmimvc.cliente.Cliente cli =
                        new ar.edu.unlu.rmimvc.cliente.Cliente(hostServidor, puertoClienteLocal, hostCliente, puertoServidorRegistry);

                cli.iniciar(c);  // inyecta modelo
                v.iniciar();     // lanza UI
                conectado = true;
            } catch (java.rmi.RemoteException | ar.edu.unlu.rmimvc.RMIMVCException e) {
                ultimoError = e;
                // ¿Puerto ocupado?
                Throwable cause = e.getCause();
                if (cause instanceof java.net.BindException ||
                        (e.getMessage() != null && e.getMessage().toLowerCase().contains("address already in use"))) {
                    // probamos el siguiente puerto
                    continue;
                } else {
                    break; // otro tipo de error: salimos
                }
            }
        }

        if (!conectado) {
            String msg = (ultimoError == null) ? "No se pudo conectar." :
                    "No se pudo conectar al servidor.\n" + ultimoError.getClass().getSimpleName() + ": " + ultimoError.getMessage();
            JOptionPane.showMessageDialog(null, msg, "Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}

