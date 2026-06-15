package vgu.pe2026.ttt.basis;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0] + ". Using default 8080.");
            }
        }
        HttpGameServer.start(port);
    }
}