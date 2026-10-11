package fpuna.ebye.server;

import fpuna.ebye.entidad.Producto;
import fpuna.ebye.servicio.StockServicio;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable {
    private final int puerto;
    private final StockServicio stockServicio;

    public TCPServer(int puerto) {
        this.puerto = puerto;
        this.stockServicio = new StockServicio();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("[TCP] Servidor eBye escuchando en el puerto " + puerto);

            while (!Thread.currentThread().isInterrupted()) {
                Socket socketCliente = serverSocket.accept();
                // Cada conexión entrante se procesa en un hilo independiente
                new Thread(() -> manejarCliente(socketCliente)).start();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error en el servidor: " + e.getMessage());
        }
    }

    private void manejarCliente(Socket socket) {
    try (
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
    ) {
        String peticion;
        while ((peticion = in.readLine()) != null) {
            String linea = peticion.trim();
            if (linea.equalsIgnoreCase("SALIR")) {
                break;
            }

            try {
                // Parsear el ID enviado por el cliente
                int id = Integer.parseInt(linea);
                
                // Llamar al método real de StockServicio
                Producto producto = stockServicio.getProducto(id);

                if (producto != null) {
                    // Puedes devolver producto.toString() o el formato que use tu protocolo (ej. JSON o texto)
                    out.println("OK: " + producto.toString());
                } else {
                    out.println("ERROR: Producto no encontrado para ID " + id);
                }
            } catch (NumberFormatException e) {
                out.println("ERROR: Formato de ID invalido. Envie un numero entero.");
            } catch (java.sql.SQLException e) {
                out.println("ERROR BD: " + e.getMessage());
            }
        }
    } catch (IOException e) {
        System.err.println("[TCP] Error atendiendo cliente: " + e.getMessage());
    } finally {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error al cerrar socket: " + e.getMessage());
        }
        }
    }
}
