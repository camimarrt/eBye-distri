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

    // Direccion del servidor de PedidosMasTarde (se puede cambiar con argumentos)
    private static String hostPmt = "127.0.0.1";
    private static int puertoPmt = 6001;

    public static void main(String[] args) {
        // Argumentos opcionales: puertoPropio hostPedidosMasTarde puertoPedidosMasTarde
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 6000;
        if (args.length > 1) {
            hostPmt = args[1];
        }
        if (args.length > 2) {
            puertoPmt = Integer.parseInt(args[2]);
        }

        // Servidores: cada uno en su propio hilo para que no se bloqueen entre si
        Thread hiloUdp = new Thread(new UDPServer(puerto), "Servidor-UDP");
        Thread hiloTcp = new Thread(new TCPServer(puerto), "Servidor-TCP");
        hiloUdp.start();
        hiloTcp.start();

        System.out.println("Servidores eBye iniciados en el puerto " + puerto + " (TCP y UDP).");

        // Menu: eBye como cliente de PedidosMasTarde
        Scanner teclado = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== eBye ===");
            System.out.println("1. Consultar pedido (PedidosMasTarde, UDP)");
            System.out.println("2. Solicitar transporte de un pedido (PedidosMasTarde, TCP)");
            System.out.println("3. Realizar pedido y solicitar envio (eBye + PedidosMasTarde)");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            String opcion = teclado.nextLine().trim();

            if (opcion.equals("1")) {
                consultarPedido(teclado);
            } else if (opcion.equals("2")) {
                solicitarTransporte(teclado);
            }else if (opcion.equals("3")) {
                realizarPedido(teclado);
            } else if (opcion.equals("0")) {
                System.out.println("Hasta luego.");
                System.exit(0);
            } else {
                System.out.println("Opcion invalida.");
            }
        }
    }

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
        // <--- AQUI INICIA LO NUEVO --->
        // Rescatamos cantidad e idproducto (asegúrate de que el servidor los envíe en la respuesta)
        long cantidadGuardada = 0;
        int idProductoGuardado = 0;
        
        if (r.containsKey("cantidad")) {
            cantidadGuardada = ((Number) r.get("cantidad")).longValue();
        }
        if (r.containsKey("idproducto")) {
            idProductoGuardado = ((Number) r.get("idproducto")).intValue();
        }

        System.out.println("\nSu paquete esta " + r.get("estado"));
        System.out.println("1. Continuar con el envio");
        System.out.println("2. Cancelar el pedido");
        System.out.print("Ingrese su opcion (1 o 2): ");
        
        String opcion = teclado.nextLine().trim();
        JSONObject solicitudAccion = new JSONObject();
        solicitudAccion.put("id", idPedido);

        if (opcion.equals("1")) {
            solicitudAccion.put("tipo", "ACTUALIZAR_ESTADO");
            solicitudAccion.put("estado", "Solicitado");
        } else if (opcion.equals("2")) {
            solicitudAccion.put("tipo", "ELIMINAR_PEDIDO");
            solicitudAccion.put("idproducto", idProductoGuardado);
            solicitudAccion.put("cantidad", cantidadGuardada);
        } else {
            System.out.println("Opcion no valida. Regresando al menu...");
            return;
        }

        // Enviamos la accion usando el mismo metodo de tu Main
        JSONObject respuestaAccion = enviar(solicitudAccion);
        if (respuestaAccion != null) {
            if (Boolean.TRUE.equals(respuestaAccion.get("ok"))) {
                System.out.println(">> Operacion exitosa: " + respuestaAccion.get("mensaje"));
            } else {
                System.out.println(">> Error: " + respuestaAccion.get("mensaje"));
            }
        }
        // <--- AQUI TERMINA LO NUEVO --->
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

        System.out.print("CI: "); // Corregido: antes decia "Direccion"
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

        // 1. Descontar el stock en la base de eBye
        ProductoDAO dao = new ProductoDAO();
        Producto producto = dao.consultarYReservar(idProducto, cantidad);
        if (producto == null) {
            System.out.println("Stock insuficiente o no existe el producto " + idProducto);
            return;
        }

        long montoTotal = producto.getPrecio() * cantidad;

        // 2. Mandar el pedido a PedidosMasTarde por TCP
        JSONObject solicitud = new JSONObject();
        solicitud.put("tipo", "REGISTRAR_PEDIDO");
        solicitud.put("requestId", UUID.randomUUID().toString());
        solicitud.put("id_cliente", idCliente);
        solicitud.put("nombre", nombre);
        solicitud.put("direccion", direccion);
        solicitud.put("monto_total", montoTotal);

        String texto = enviarTCP(hostPmt, puertoPmt, solicitud.toJSONString());

        // 3. Si PedidosMasTarde no pudo guardar el pedido, se devuelve el stock
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

    // Pide un id de pedido y manda "SOLICITAR_TRANSPORTE,<id>" por TCP (formato de texto, no JSON).
    private static void solicitarTransporte(Scanner teclado) {
        System.out.print("Id del pedido: ");
        int idPedido;
        try {
            idPedido = Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("El id debe ser un numero.");
            return;
        }

        String respuesta = enviarTCP(hostPmt, puertoPmt, "SOLICITAR_TRANSPORTE," + idPedido);
        if (respuesta == null) {
            System.out.println("PedidosMasTarde no respondio en 3 segundos (servidor apagado o sin respuesta).");
        } else {
            System.out.println("Respuesta: " + respuesta);
        }
    }

    // Agrega el requestId, envia la solicitud a PedidosMasTarde y devuelve la respuesta.
    // Devuelve null (y avisa por pantalla) si hubo algun problema o si PedidosMasTarde rechazo la solicitud.
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

    // Abre una conexion TCP, envia una linea y devuelve la linea de respuesta.
    // Devuelve null si no hay respuesta en 3 segundos o si falla la conexion.
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

    // Envia un datagrama y espera la respuesta. Devuelve null si pasan 3 segundos sin respuesta.
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