package fpuna.ebye;

import fpuna.ebye.server.TCPServer;
import fpuna.ebye.server.UDPServer;

public class Main {
    public static void main(String[] args) {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 6000;

        // Se instancian ambos servidores
        UDPServer servidorUdp = new UDPServer(puerto);
        TCPServer servidorTcp = new TCPServer(puerto);

        // Se inician en hilos independientes para que no se bloqueen mutuamente
        Thread hiloUdp = new Thread(servidorUdp, "Servidor-UDP");
        Thread hiloTcp = new Thread(servidorTcp, "Servidor-TCP");

        hiloUdp.start();
        hiloTcp.start();

        System.out.println("Servidores eBye iniciados en el puerto " + puerto + " (TCP y UDP).");
    }
}
