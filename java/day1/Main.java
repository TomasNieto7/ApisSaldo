import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) throws IOException {
        // Configura el servidor HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        System.out.println("Server is running in port 8000");
        // Agrega contextos y manejadores para cada endpoint de tu API
        server.createContext("/api", new ResourceHandler());

        server.setExecutor(null); // crea un ejecutor por defecto
        server.start();
    }

    // Clase para manejar solicitudes al endpoint /api/resource
    static class ResourceHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetRequest(exchange);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                handlePostRequest(exchange);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            Connection conn = DataBaseConnector.getConnection();
            StringBuilder response = new StringBuilder();
            String query = exchange.getRequestURI().getQuery();
            String numeroTelefono = query != null ? query.split("=")[1] : "";

            try {
                Statement stmt = conn.createStatement();
                String sql = "SELECT * FROM empleados WHERE numero_telefono = '" + numeroTelefono + "'";
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    int id = rs.getInt("id_empleado");
                    double saldo = rs.getDouble("saldo");
                    String nombre = rs.getString("nombre");

                    // Construye la respuesta en formato JSON
                    response.append("{")
                            .append("\"id_empleado\": ").append(id).append(", ")
                            .append("\"numero_telefono\": \"").append(numeroTelefono).append("\", ")
                            .append("\"saldo\": ").append(saldo).append(", ")
                            .append("\"nombre\": \"").append(nombre).append("\"")
                            .append("}, ");
                }

                // Elimina la última coma y espacio
                if (response.length() > 2) {
                    response.setLength(response.length() - 2);
                }

                response.insert(0, "[");
                response.append("]");
            } catch (Exception e) {
                e.printStackTrace();
                response = new StringBuilder("Error al obtener los datos.");
            }

            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.toString().getBytes());
            os.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            Connection conn = DataBaseConnector.getConnection();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            String numeroTelefono = json.getString("numero_telefono");
            double nuevoSaldo = json.getDouble("saldo");
            double saldoActual = 0;
            double saldoActualizado = 0;

            try {
                // Obtén el saldo actual
                String query = "SELECT saldo FROM empleados WHERE numero_telefono = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, numeroTelefono);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    saldoActual = rs.getDouble("saldo");
                    saldoActualizado = saldoActual + nuevoSaldo;

                    // Actualiza el saldo
                    String updateQuery = "UPDATE empleados SET saldo = ? WHERE numero_telefono = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setDouble(1, saldoActualizado);
                    updateStmt.setString(2, numeroTelefono);
                    updateStmt.executeUpdate();
                }

                // Devuelve el nuevo saldo
                String response = "Saldo Actualizado";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
                String response = "Error al actualizar los datos.";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } finally {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
