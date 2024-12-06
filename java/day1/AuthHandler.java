import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AuthHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        // Agrega cabeceras CORS a todas las respuestas
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*"); // Permitir todos los orígenes
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
        } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleGetRequest(exchange);
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
            String sql = "SELECT id_user FROM users WHERE cellphone = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, numeroTelefono);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Usuario encontrado
                int idUser = rs.getInt("id_user");
                response.append("{")
                        .append("\"message\": \"userFound\", ")
                        .append("\"id_user\": ").append(idUser)
                        .append("}");
            } else {
                // Usuario no encontrado
                response.append("{")
                        .append("\"message\": \"userNotFound\"")
                        .append("}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = new StringBuilder("{ \"message\": \"Error al obtener los datos.\" }");
        }
    
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
    
}
