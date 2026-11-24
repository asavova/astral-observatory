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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tiny dependency-free local server: sky calculations + browser prototype. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Path project = Path.of("").toAbsolutePath();
        List<Star> catalog = loadStars(project.resolve("data/bright-stars-sample.csv"));
        int port = Integer.parseInt(System.getenv().getOrDefault("ASTRAL_PORT", "8766"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/stars", exchange -> {
            try {
                Map<String, String> params = query(exchange.getRequestURI().getRawQuery());
                double latitude = Double.parseDouble(params.getOrDefault("lat", "42.6977"));
                double longitude = Double.parseDouble(params.getOrDefault("lon", "23.3219"));
                Instant time = Instant.parse(params.getOrDefault("time", Instant.now().toString()));
                StringBuilder json = new StringBuilder("{\"stars\":[");
                boolean first = true;
                for (Star star : catalog) {
                    SkyMath.Horizontal position = SkyMath.toHorizontal(star, latitude, longitude, time);
                    if (position.altitudeDegrees() <= 0) continue;
                    if (!first) json.append(',');
                    first = false;
                    json.append(String.format(Locale.ROOT,
                            "{\"name\":\"%s\",\"alt\":%.4f,\"az\":%.4f,\"mag\":%.2f}",
                            star.name(), position.altitudeDegrees(), position.azimuthDegrees(), star.magnitude()));
                }
                json.append("]}");
                respond(exchange, 200, "application/json", json.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                respond(exchange, 400, "text/plain", ("Invalid query: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                respond(exchange, 500, "text/plain", "Internal error".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                respond(exchange, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            respond(exchange, 200, "text/html; charset=utf-8", Files.readAllBytes(project.resolve("web/index.html")));
        });
        server.start();
        System.out.println("Astral Observatory starter running at http://127.0.0.1:" + port);
        System.out.println("Loaded " + catalog.size() + " sample stars; Ctrl+C stops the server.");
    }

    private static List<Star> loadStars(Path file) throws IOException {
        List<Star> result = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("name,")) continue;
            String[] fields = line.split(",", -1);
            if (fields.length != 4) throw new IOException("Invalid catalogue row: " + line);
            result.add(new Star(fields[0], Double.parseDouble(fields[1]),
                    Double.parseDouble(fields[2]), Double.parseDouble(fields[3])));
        }
        return List.copyOf(result);
    }

    private static Map<String, String> query(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;
        for (String part : rawQuery.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
