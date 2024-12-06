import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            // Configura el servidor HTTP
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            System.out.println("Server is running in port 8000");
            // Agrega contextos y manejadores para cada endpoint de tu API
            server.createContext("/api", new ResourceHandler());
            server.createContext("/api/login", new AuthHandler());

            server.setExecutor(null); // crea un ejecutor por defecto
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
