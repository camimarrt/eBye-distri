package fpuna.ebye.servicio;

import fpuna.ebye.bd.ProductoDAO;
import fpuna.ebye.entidad.Producto;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class StockServicio {

    private ProductoDAO productoDAO = new ProductoDAO();
    public Producto getProducto(int idProducto) {
        return productoDAO.getProducto(idProducto);
    }
    public Producto reservarStock(int idProducto, int cantidad) {
        return productoDAO.consultarYReservar(idProducto, cantidad);
    }
    // Ajusta el puerto al que esté configurado en TCPServer de PedidosMasTarde
    private static final String HOST_PEDIDOS = "localhost";
    private static final int PUERTO_PEDIDOS = 9877; 

    public String realizarPedido(Integer idProducto, Integer cantidad, String destino, String cliente) {
        
        // 1. Intentar descontar stock
        Producto producto = productoDAO.consultarYReservar(idProducto, cantidad);
        if (producto == null) {
            return "ERROR: Stock insuficiente o producto inexistente.";
        }

        // 2. Conectarse a PedidosMasTarde para registrar el transporte
        boolean envioRegistrado = false;
        String respuestaServidor = "";

        try (Socket socket = new Socket(HOST_PEDIDOS, PUERTO_PEDIDOS);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Trama: SOLICITAR_TRANSPORTE;<idProducto>;<cantidad>;<destino>;<cliente>
            String mensaje = String.format("SOLICITAR_TRANSPORTE;%d;%d;%s;%s",
                    idProducto, cantidad, destino, cliente);
            out.println(mensaje);

            respuestaServidor = in.readLine();
            if (respuestaServidor != null && respuestaServidor.startsWith("OK")) {
                envioRegistrado = true;
            }

        } catch (Exception e) {
            System.err.println("Error comunicándose con PedidosMasTarde: " + e.getMessage());
        }

        // 3. Compensación si falló el servicio de envío
        if (!envioRegistrado) {
            productoDAO.devolverStock(idProducto, cantidad);
            return "ERROR: No se pudo coordinar el envio. Stock restablecido. Detalle: " + respuestaServidor;
        }

        return "OK: Pedido reservado y envio registrado exitosamente.";
    }
}