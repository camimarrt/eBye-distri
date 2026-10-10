package fpuna.ebye.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import fpuna.ebye.entidad.Producto;
import fpuna.ebye.servicio.StockServicio;

public class UDPServer {
    private final int puerto;

    public UDPServer(int puerto) {
        this.puerto = puerto;
    }

    public void ejecutar() throws Exception {
        DatagramSocket socket = new DatagramSocket(puerto);

        try {
            System.out.println("eBye escuchando en el puerto UDP " + puerto);
            byte[] buffer = new byte[2048];

            while (true) {
                // Espera un datagrama (se queda bloqueado hasta que llegue uno)
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);

                String solicitud = new String(paquete.getData(), 0,
                        paquete.getLength(), StandardCharsets.UTF_8);

                String respuesta = procesar(solicitud);

                // Responde a la misma direccion y puerto de donde vino el datagrama
                byte[] datos = respuesta.getBytes(StandardCharsets.UTF_8);
                DatagramPacket paqueteRespuesta = new DatagramPacket(datos,
                        datos.length, paquete.getAddress(), paquete.getPort());
                socket.send(paqueteRespuesta);
            }
        } finally {
            socket.close();
        }
    }

    private String procesar(String texto) {
        String requestId = null;

        try {
            Object valor = new JSONParser().parse(texto);
            if (!(valor instanceof JSONObject)) {
                return error(null, "SOLICITUD_INVALIDA", "La solicitud debe ser un objeto JSON");
            }

            JSONObject solicitud = (JSONObject) valor;
            String tipo = textoRequerido(solicitud, "tipo");
            requestId = textoRequerido(solicitud, "requestId");

            switch (tipo.toUpperCase()) {
                case "GET_STOCK":
                    int idProducto = enteroRequerido(solicitud, "idproducto");

                    StockServicio s = new StockServicio();
                    Producto p = s.getProducto(idProducto);

                    System.out.println("Request atendido: " + requestId);

                    if (p == null) {
                        return error(requestId, "PRODUCTO_NO_ENCONTRADO",
                                "No existe el producto " + idProducto);
                    }
                    return respuestaGetStock(requestId, p);

                // AGREGAR SERVICIOS ACA
                // case "SERVICIO":

                default:
                    return error(requestId, "TIPO_NO_SOPORTADO", "Solo se admite GET_STOCK");
            }

        } catch (SQLException e) {
            System.err.println("Error consultando stock: " + e.getMessage());
            return error(requestId, "ERROR_BD", "No se pudo consultar el stock");
        } catch (ParseException e) {
            return error(null, "JSON_INVALIDO", "No se pudo interpretar la solicitud");
        } catch (IllegalArgumentException e) {
            return error(requestId, "CAMPO_REQUERIDO", e.getMessage());
        }
    }

    private String textoRequerido(JSONObject solicitud, String campo) {
        Object valor = solicitud.get(campo);
        if (!(valor instanceof String) || ((String) valor).trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el campo " + campo);
        }
        return (String) valor;
    }

    private int enteroRequerido(JSONObject solicitud, String campo) {
        Object valor = solicitud.get(campo);
        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException("Falta el campo " + campo + " o no es un numero");
        }
        return ((Number) valor).intValue();
    }

    private String error(Object requestId, String codigo, String mensaje) {
        JSONObject respuesta = new JSONObject();
        respuesta.put("ok", false);
        respuesta.put("requestId", requestId);
        respuesta.put("codigo", codigo);
        respuesta.put("mensaje", mensaje);
        return respuesta.toJSONString();
    }

    private String respuestaGetStock(String requestId, Producto p) {
        JSONObject respuesta = new JSONObject();
        respuesta.put("ok", true);
        respuesta.put("requestId", requestId);
        respuesta.put("stock_disponible", p.getStock());

        // timestamp
        respuesta.put("timestamp", java.time.OffsetDateTime.now().toString());

        return respuesta.toJSONString();
    }
}
