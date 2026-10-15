package fpuna.ebye.server;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPServer implements Runnable {
    private final int puerto;

    public TCPServer(int puerto) {
        this.puerto = puerto;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("eBye escuchando en el puerto TCP " + puerto);

            // Por cada cliente que se conecta se crea un hilo que lo atiende
            while (true) {
                new TCPServerHilo(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor TCP: " + e.getMessage());
        }
    }
}
