package httputils;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Responses {

    static Gson gson = new Gson();

    public static void sendError(HttpExchange hex, Integer code, String body) throws IOException {
        byte[] bytesToSend = body.getBytes(StandardCharsets.UTF_8);
        hex.getResponseHeaders().set("Content-Type", "text/plain");
        hex.sendResponseHeaders(code,bytesToSend.length);
        OutputStream os = hex.getResponseBody();
        os.write(bytesToSend);
        os.close();
    }

    public static void sendJson(HttpExchange hex,int code, Object body) throws IOException {
        byte[] bytesToSend = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        hex.getResponseHeaders().set("Content-Type", "Application/JSON");
        hex.sendResponseHeaders(code,bytesToSend.length);
        OutputStream os = hex.getResponseBody();
        os.write(bytesToSend);
        os.close();
    }

}
