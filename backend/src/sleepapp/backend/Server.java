package sleepapp.backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import sleepapp.backend.endpoint.Endpoint;
import sleepapp.backend.endpoint.SleepyTestHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;


public class Server {

    private static final int BACKLOG = 10;
    private static final int PORT = 3333;

    private static final InetAddress LISTEN_ADDRESS = InetAddress.getLoopbackAddress();
    private static final Endpoint[] ENDPOINTS = {
            new SleepyTestHandler()
    };


    public static void main(String[] args) throws UnknownHostException, IOException {
        Scanner stdin = new Scanner(System.in);
        HttpServer server = HttpServer.create(new InetSocketAddress(LISTEN_ADDRESS, PORT), BACKLOG);

        for (Endpoint e: ENDPOINTS) {
            server.createContext(e.getPath(), e);
        }

        server.start();
        System.out.println("Server running, stopping on input...");
        stdin.nextLine();
        server.stop(0);
        System.out.println("Bye then");
    }

}
