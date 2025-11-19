import com.sun.net.httpserver.HttpServer;
import httputils.HistoryRequestHandler;
import httputils.Responses;
import httputils.TaskRequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;


public class HttpTaskServerFileBackedRefactor {

    public static void main(String[] args) throws IOException {
        HttpServer commonServer = HttpServer.create(new InetSocketAddress(8080), 0);

        commonServer.createContext("/tasks",exchange -> {
            try {
                switch (exchange.getRequestMethod()) {
                    case "GET" ->{TaskRequestHandler.getTaskHandler(exchange);}
                    case "POST" -> {TaskRequestHandler.postTaskHandler(exchange);}
                    case "PUT" -> {TaskRequestHandler.putTaskHandler(exchange);}
                    case "DELETE" -> {TaskRequestHandler.deleteTaskHandler(exchange);}
                    default -> Responses.sendError(exchange, 400, "Method not supported");
                }
            } catch (Exception e) {
                String body = "Task API Error:" + e.getMessage();
                Responses.sendError(exchange,500,body);
            }

        });

        commonServer.createContext("/history",exchange -> {
            try {
                switch (exchange.getRequestMethod()) {
                    case "GET" ->{HistoryRequestHandler.getHistoryHandler(exchange);}
                    default -> Responses.sendError(exchange, 400, "Method not supported");
                }
            } catch (Exception e) {
                String body = "History API Error:" + e.getMessage();
                Responses.sendError(exchange,500,body);
            }
        });

        commonServer.start();

    }

}
