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
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            String opcion = teclado.nextLine().trim();

            if (opcion.equals("1")) {
                consultarPedido(teclado);
            } else if (opcion.equals("2")) {
                solicitarTransporte(teclado);
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