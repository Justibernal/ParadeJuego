package app;

import modelo.ModeloParade;
import ar.edu.unlu.rmimvc.RMIMVCException;
import ar.edu.unlu.rmimvc.servidor.Servidor;

import java.rmi.RemoteException;

public class AppServidorParade {
    public static void main(String[] args) {
        try {
            ModeloParade modelo = new ModeloParade();
            Servidor servidor = new Servidor("127.0.0.1", 8888);
            servidor.iniciar(modelo);
            System.out.println("Servidor Parade iniciado en 127.0.0.1:8888");
        } catch (RemoteException | RMIMVCException e) {
            e.printStackTrace();
        }
    }
}



