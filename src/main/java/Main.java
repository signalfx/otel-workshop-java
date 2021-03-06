import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.Context;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class Main {

    private class HelloHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange he) throws IOException {

            // Process the request
            String response = "hello from java\n this is the end of the journey... for today";
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes(Charset.defaultCharset()));
            os.close();
            System.out.println("Served Client: " + he.getRemoteAddress());
        }

    }


    private com.sun.net.httpserver.HttpServer server;
    private static int port;
    private Main() throws IOException {
        Dotenv dotenv = Dotenv.load();
        port = Integer.parseInt(dotenv.get("SERVER_PORT"));
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        // Test urls
        server.createContext("/", new HelloHandler());
        server.start();
        System.out.println("Server ready on http://localhost:" + port);
    }

    private void stop() {
        server.stop(0);
    }

    /**
     * Main method to run the example.
     *
     * @param args It is not required.
     * @throws Exception Something might go wrong.
     */
    public static void main(String[] args) throws Exception {
        final Main s = new Main();
        // Gracefully close the server
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                s.stop();
                            }
                        });
    }
}
