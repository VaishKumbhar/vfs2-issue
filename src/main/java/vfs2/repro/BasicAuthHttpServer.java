package vfs2.repro;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal HTTP server with optional Basic auth. JDK built-in only (no extra deps).
 */
public final class BasicAuthHttpServer {

    private HttpServer server;
    private int port;
    private final Map<String, byte[]> paths = new HashMap<>();
    private String expectedBasicAuth; // Base64("user:pass") or null to disable auth

    public BasicAuthHttpServer withBasicAuth(String username, String password) {
        this.expectedBasicAuth = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public BasicAuthHttpServer addPath(String path, String content) {
        paths.put(path, content.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        for (Map.Entry<String, byte[]> e : paths.entrySet()) {
            String path = e.getKey();
            byte[] content = e.getValue();
            String expected = expectedBasicAuth;
            server.createContext(path, new HttpHandler() {
                @Override
                public void handle(HttpExchange ex) throws IOException {
                    if (expected != null) {
                        String auth = ex.getRequestHeaders().getFirst("Authorization");
                        if (!("Basic " + expected).equals(auth)) {
                            ex.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"test\"");
                            ex.sendResponseHeaders(401, -1);
                            ex.close();
                            return;
                        }
                    }
                    ex.getResponseHeaders().set("Content-Type", "text/plain");
                    ex.sendResponseHeaders(200, content.length);
                    try (OutputStream out = ex.getResponseBody()) {
                        out.write(content);
                    }
                    ex.close();
                }
            });
        }
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
