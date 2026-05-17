package multithread; // <--- MUST BE LOWERCASE

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class MServer {
    private static ThreadPoolExecutor threadPool;

    public static void main(String[] args) {
        // Core: 3, Max: 3, Queue: 10
        threadPool = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("SERVER LIVE ON PORT: " + port);
            while (true) {
                Socket client = serverSocket.accept();
                handleRequest(client);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void handleRequest(Socket socket) {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if (line == null || line.isEmpty()) return;
                
                String path = line.split(" ")[1].split("\\?")[0];

                if (path.equals("/status")) {
                    threadPool.submit(() -> holdHostage(socket));
                } else {
                    serveFile(path, socket);
                }
            } catch (Exception e) { 
                try { socket.close(); } catch (IOException ignored) {} 
            }
        }).start();
    }

    private static void holdHostage(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            String tName = Thread.currentThread().getName();
            int active = threadPool.getActiveCount();
            int queued = threadPool.getQueue().size();
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // Headers with MIME type for JSON
            String headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\nConnection: keep-alive\r\n\r\n";
            out.write(headers.getBytes());

            String json = String.format("{\"thread\":\"%s\", \"active\":%d, \"queue\":%d, \"time\":\"%s\"}\n", 
                          tName, active, queued, time);
            out.write(json.getBytes());
            out.flush();

            while (true) {
                out.write(" ".getBytes()); 
                out.flush();
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            System.out.println("[Backend] Connection closed.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void serveFile(String path, Socket s) throws IOException {
        // Use relative path to 'web' folder in /app
        String fileName = path.equals("/") ? "index.html" : path.replace("/", "");
        File f = new File("web", fileName);

        if (!f.exists()) return;

        byte[] content = Files.readAllBytes(f.toPath());
        String contentType = fileName.endsWith(".css") ? "text/css" : "text/html";

        OutputStream out = s.getOutputStream();
        String response = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: " + contentType + "\r\n" +
                         "Content-Length: " + content.length + "\r\n\r\n";
        out.write(response.getBytes());
        out.write(content);
        out.flush();
        s.close();
    }
}