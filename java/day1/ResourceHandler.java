import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;

public class ResourceHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {
        // Agrega cabeceras CORS a todas las respuestas
        System.out.println(exchange.getRequestMethod());
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*"); // Permitir todos los orígenes
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PATCH");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
        } else if ("GET".equals(exchange.getRequestMethod())) {
            handleGetRequest(exchange);
        } else if ("PATCH".equals(exchange.getRequestMethod())) {
            handlePatchRequest(exchange);
        }
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        // Responder a la solicitud OPTIONS con las cabeceras CORS adecuadas
        exchange.sendResponseHeaders(204, -1);
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
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", 200);
            jsonResponse.put("message", response.toString());

            sendJsonResponse(exchange, jsonResponse, 200);
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", 500);
            jsonResponse.put("message", "Error al obtener los datos.");

            sendJsonResponse(exchange, jsonResponse, 500);
        }
    }

    private void handlePatchRequest(HttpExchange exchange) throws IOException {
        Connection conn = DataBaseConnector.getConnection();
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        JSONObject json = new JSONObject(sb.toString());
        String numeroTelefono = json.getString("cellphone");
        double nuevoSaldo = json.getDouble("balance");
        System.out.println(numeroTelefono);
        System.out.println(nuevoSaldo);

        // Validación de longitud del número de teléfono y saldo
        if (numeroTelefono.length() < 10) {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", 400);
            jsonResponse.put("message", "El número de teléfono debe tener al menos 10 dígitos.");
            sendJsonResponse(exchange, jsonResponse, 400);
            return;
        }
    
        if (nuevoSaldo <= 0) {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", 400);
            jsonResponse.put("message", "El saldo debe ser mayor que 0.");
            sendJsonResponse(exchange, jsonResponse, 400);
            return;
        }
    
        double saldoActual = 0;
        double saldoActualizado = 0;
    
        try {
            // Obtén el saldo actual
            String query = "SELECT balance FROM users WHERE cellphone = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, numeroTelefono);
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                saldoActual = rs.getDouble("balance");
                saldoActualizado = saldoActual + nuevoSaldo;
    
                // Actualiza el saldo
                String updateQuery = "UPDATE users SET balance = ? WHERE cellphone = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setDouble(1, saldoActualizado);
                updateStmt.setString(2, numeroTelefono);
                updateStmt.executeUpdate();
    
                // Devuelve el nuevo saldo
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", 200);
                jsonResponse.put("message", "Recarga realizada");
                sendJsonResponse(exchange, jsonResponse, 200);
            } else {
                System.out.println("llego aqui");
                // Si el número de teléfono no existe
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", 404);
                jsonResponse.put("message", "El número de teléfono no existe.");
                sendJsonResponse(exchange, jsonResponse, 202);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", 500);
            jsonResponse.put("message", "Error al actualizar los datos.");
            sendJsonResponse(exchange, jsonResponse, 500);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendJsonResponse(HttpExchange exchange, JSONObject jsonResponse, int statusCode) throws IOException {
        byte[] responseBytes = jsonResponse.toString().getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
