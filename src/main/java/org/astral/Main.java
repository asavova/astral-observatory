package org.astral;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Local demo server. Telescope controls and tracking are intentionally not implemented. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Path project = Path.of("").toAbsolutePath();
        List<Star> catalog = StarCatalog.loadBestAvailable(project.resolve("data"));
        int port = Integer.parseInt(System.getenv().getOrDefault("ASTRAL_PORT", "8766"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/stars", exchange -> {
            try {
                Map<String, String> params = query(exchange.getRequestURI().getRawQuery());
                double lat = Double.parseDouble(params.getOrDefault("lat", "42.6977"));
                double lon = Double.parseDouble(params.getOrDefault("lon", "23.3219"));
                double limit = Double.parseDouble(params.getOrDefault("maxMag", "6"));
                if (!Double.isFinite(limit) || limit < -2 || limit > 12) {
                    throw new IllegalArgumentException("maxMag must be between -2 and 12");
                }
                Instant time = Instant.parse(params.getOrDefault("time", Instant.now().toString()));

                StringBuilder json = new StringBuilder("{\"total\":")
                        .append(catalog.size()).append(",\"stars\":[");
                int count = 0;
                for (Star star : catalog) {
                    if (star.magnitude() > limit) continue;
                    SkyMath.Horizontal position = SkyMath.toHorizontal(star, lat, lon, time);
                    if (position.altitudeDegrees() <= 0) continue;
                    if (count++ > 0) json.append(',');
                    json.append("{\"name\":").append(quoted(star.name()));
                    json.append(String.format(Locale.ROOT,
                            ",\"alt\":%.4f,\"az\":%.4f,\"mag\":%.2f,\"ci\":%.2f}",
                            position.altitudeDegrees(), position.azimuthDegrees(),
                            star.magnitude(), star.colorIndex()));
                }
                json.append("]}");
                respond(exchange, 200, "application/json", json.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ex) {
                respond(exchange, 400, "text/plain; charset=utf-8",
                        ("Invalid query: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                ex.printStackTrace();
                respond(exchange, 500, "text/plain; charset=utf-8",
                        "Internal server error".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                respond(exchange, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            respond(exchange, 200, "text/html; charset=utf-8",
                    Files.readAllBytes(project.resolve("web/index.html")));
        });
        server.start();
        System.out.println("Astral Observatory at http://127.0.0.1:" + port);
        System.out.println("Loaded " + catalog.size() + " stars from local data.");
    }

    private static String quoted(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') out.append('\\').append(ch);
            else if (ch == '\n') out.append("\\n");
            else if (ch == '\r') out.append("\\r");
            else if (ch == '\t') out.append("\\t");
            else if (ch < 32) out.append(String.format("\\u%04x", (int)ch));
            else out.append(ch);
        }
        return out.append('"').toString();
    }

    private static Map<String, String> query(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;
        for (String part : rawQuery.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] bytes)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
