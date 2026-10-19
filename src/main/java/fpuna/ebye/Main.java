package fpuna.ebye;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import fpuna.ebye.bd.ProductoDAO;
import fpuna.ebye.entidad.Producto;
import fpuna.ebye.server.TCPServer;
import fpuna.ebye.server.UDPServer;

public class Main {

    // direccion del servidor de PedidosMasTarde
    private static String hostPmt = "127.0.0.1";
    private static int puertoPmt = 6001;

    public static void main(String[] args) {
        
        int puerto =  6000;

        // servidores: cada uno en su propio hilo para que no se bloqueen entre si
        Thread hiloUdp = new Thread(new UDPServer(puerto), "Servidor-UDP");
        Thread hiloTcp = new Thread(new TCPServer(puerto), "Servidor-TCP");
        hiloUdp.start();
        hiloTcp.start();

        System.out.println("Servidores eBye iniciados en el puerto " + puerto + " (TCP y UDP).");

        // menu de eBye como cliente de PedidosMasTarde
        Scanner teclado = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== eBye ===");
            System.out.println("1. Consultar pedido (PedidosMasTarde, UDP)");
            System.out.println("2. Cancelar envio de un pedido (PedidosMasTarde, UDP)");
            System.out.println("3. Realizar pedido y solicitar envio (eBye + PedidosMasTarde)");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            String opcion = teclado.nextLine().trim();

            if (opcion.equals("1")) {
                consultarPedido(teclado);
            } else if (opcion.equals("2")) {
                cancelarEnvio(teclado);
            } else if (opcion.equals("3")) {
                realizarPedido(teclado);
            } else if (opcion.equals("0")) {
                System.out.println("Hasta luego.");
                System.exit(0);
            } else {
                System.out.println("Opcion invalida.");
            }
        }
    }
    // pide el ID del pedido, crea un JSON para la solicitud y envia por UDP
    private static void consultarPedido(Scanner teclado) {
        System.out.print("Id del pedido: ");
        int idPedido;
        try {
            idPedido = Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("El id debe ser un numero.");
            return;
        }

        JSONObject solicitud = new JSONObject();
        solicitud.put("tipo", "CONSULTAR_PEDIDO");
        solicitud.put("id", idPedido);

        JSONObject r = enviar(solicitud);
        if (r == null) {
            return;
        }

        System.out.println("Pedido " + idPedido + ":");
        System.out.println("  Cliente: " + r.get("id_cliente"));
        System.out.println("  Destinatario: " + r.get("nombre_destinatario"));
        System.out.println("  Monto total: " + r.get("monto_total"));
        System.out.println("  Estado: " + r.get("estado"));
        System.out.println("  Direccion: " + r.get("direccion"));
        System.out.println("  Ruta: " + r.get("id_ruta"));
    }

    private static void realizarPedido(Scanner teclado) {
        System.out.println("\n--- Realizar pedido ---");

        int idProducto;
        int cantidad;
        try {
            System.out.print("Id del producto: ");
            idProducto = Integer.parseInt(teclado.nextLine().trim());

            System.out.print("Cantidad: ");
            cantidad = Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("El id y la cantidad deben ser numeros.");
            return;
        }

        if (cantidad <= 0) {
            System.out.println("La cantidad debe ser mayor a 0.");
            return;
        }

        System.out.print("CI: "); 
        int idCliente;
        try {
            idCliente = Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("El CI del debe ser un numero.");
            return;
        }

        System.out.print("Nombre del destinatario: ");
        String nombre = teclado.nextLine().trim();

        System.out.print("Direccion: ");
        String direccion = teclado.nextLine().trim();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            System.out.println("El nombre y la direccion no pueden estar vacios.");
            return;
        }

        // descontar el stock en la base de eBye
        ProductoDAO dao = new ProductoDAO();
        Producto producto = dao.consultarYReservar(idProducto, cantidad);
        if (producto == null) {
            System.out.println("Stock insuficiente o no existe el producto " + idProducto);
            return;
        }

        long montoTotal = producto.getPrecio() * cantidad;

        // mandar el pedido a PedidosMasTarde por TCP
        JSONObject solicitud = new JSONObject();
        solicitud.put("tipo", "REGISTRAR_PEDIDO");
        solicitud.put("requestId", UUID.randomUUID().toString());
        solicitud.put("id_cliente", idCliente);
        solicitud.put("nombre", nombre);
        solicitud.put("direccion", direccion);
        solicitud.put("monto_total", montoTotal);
        solicitud.put("id_producto", idProducto);
        solicitud.put("cantidad", cantidad);

        String texto = enviarTCP(hostPmt, puertoPmt, solicitud.toJSONString());

        // si PedidosMasTarde no pudo guardar el pedido, se devuelve el stock
        if (texto == null) {
            System.out.println("PedidosMasTarde no respondio. Se devuelve el stock.");
            dao.devolverStock(idProducto, cantidad);
            return;
        }

        try {
            JSONObject respuesta = (JSONObject) new JSONParser().parse(texto);
            if (Boolean.TRUE.equals(respuesta.get("ok"))) {
                System.out.println("Pedido registrado. Id del pedido: " + respuesta.get("idpedido"));
                System.out.println("Monto total: " + montoTotal);
            } else {
                System.out.println("PedidosMasTarde rechazo el pedido: " + respuesta.get("mensaje"));
                dao.devolverStock(idProducto, cantidad);
            }
        } catch (Exception e) {
            System.out.println("Respuesta invalida de PedidosMasTarde: " + texto);
            dao.devolverStock(idProducto, cantidad);
        }
    }

     // pide un id de pedido y manda ELIMINAR_PEDIDO por UDP. PedidosMasTarde borra la fila de la BD.
    private static void cancelarEnvio(Scanner teclado) {
        System.out.print("Id del pedido a cancelar: ");
        int idPedido;
        try {
            idPedido = Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("El id debe ser un numero.");
            return;
        }

        JSONObject solicitud = new JSONObject();
        solicitud.put("tipo", "ELIMINAR_PEDIDO");
        solicitud.put("id", idPedido);

        JSONObject r = enviar(solicitud);
        if (r != null) {
            System.out.println(">> " + r.get("mensaje"));

            // devolver el stock a la tabla producto de eBye
            Object idProd = r.get("id_producto");
            Object cant = r.get("cantidad");

            if (idProd instanceof Number && cant instanceof Number) {
                int idProducto = ((Number) idProd).intValue();
                int cantidad = ((Number) cant).intValue();

                if (idProducto > 0 && cantidad > 0) {
                    if (new ProductoDAO().devolverStock(idProducto, cantidad)) {
                        System.out.println(">> Stock devuelto: producto " + idProducto + " (+" + cantidad + ")");
                    } else {
                        System.out.println(">> No se pudo devolver el stock del producto " + idProducto);
                    }
                }
            }
        }
    }

    // agrega el requestId, envia la solicitud a Pedidos y devuelve la respuesta.
    // devuelve null si hubo algun problema o si Pedidos rechazo la solicitud.
    private static JSONObject enviar(JSONObject solicitud) {
        String requestId = UUID.randomUUID().toString();
        solicitud.put("requestId", requestId);

        String texto = enviarUDP(hostPmt, puertoPmt, solicitud.toJSONString());
        if (texto == null) {
            System.out.println("PedidosMasTarde no respondio en 3 segundos (servidor apagado o datagrama perdido).");
            return null;
        }

        try {
            JSONObject respuesta = (JSONObject) new JSONParser().parse(texto);
            if (!requestId.equals(respuesta.get("requestId"))) {
                System.out.println("El requestId de la respuesta no coincide.");
                return null;
            }
            if (!Boolean.TRUE.equals(respuesta.get("ok"))) {
                System.out.println("PedidosMasTarde rechazo la solicitud: " + respuesta.get("mensaje"));
                return null;
            }
            return respuesta;
        } catch (Exception e) {
            System.out.println("Respuesta invalida de PedidosMasTarde: " + texto);
            return null;
        }
    }

    // abre una conexion TCP, envia una linea y devuelve la linea de respuesta.
    // devuelve null si no hay respuesta en 3 segundos o si falla la conexion.
    private static String enviarTCP(String host, int puerto, String mensaje) {
        try (Socket socket = new Socket(host, puerto)) {
            socket.setSoTimeout(3000);

            BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader entrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));

            salida.write(mensaje);
            salida.write("\n");
            salida.flush();

            return entrada.readLine();
        } catch (SocketTimeoutException e) {
            return null;
        } catch (Exception e) {
            System.out.println("Error de red: " + e.getMessage());
            return null;
        }
    }

    // envia un datagrama y espera la respuesta
    private static String enviarUDP(String host, int puerto, String mensaje) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);

            byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
            DatagramPacket paquete = new DatagramPacket(datos, datos.length,
                    InetAddress.getByName(host), puerto);
            socket.send(paquete);

            byte[] buffer = new byte[2048];
            DatagramPacket paqueteRespuesta = new DatagramPacket(buffer, buffer.length);
            socket.receive(paqueteRespuesta);

            return new String(paqueteRespuesta.getData(), 0,
                    paqueteRespuesta.getLength(), StandardCharsets.UTF_8);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (Exception e) {
            System.out.println("Error de red: " + e.getMessage());
            return null;
        }
    }
}