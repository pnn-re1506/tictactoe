package vgu.pe2026.ttt.basis;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpGameServer {

    public static void start(int port) throws IOException {
        GameHandler handler = new GameHandler();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/move", handler);
        server.setExecutor(null); // single-threaded (default executor)
        server.start();

        System.out.println("===========================================");
        System.out.println("  TicTacToe HTTP Server started");
        System.out.println("  http://localhost:" + port + "/api/move");
        System.out.println("===========================================");
    }
}
