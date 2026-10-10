package fpuna.ebye;

import fpuna.ebye.server.UDPServer;

public class Main {
    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 6000;
        new UDPServer(puerto).ejecutar();
    }
}
