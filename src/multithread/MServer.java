package multithread;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class MServer {
    private static ThreadPoolExecutor threadPool;

    public static void main(String[] args) {
        // 3 Workers, 10 in queue
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
                if (line == null) return;
                String path = line.split(" ")[1].split("\\?")[0];

                if (path.equals("/status")) {
                    threadPool.submit(() -> holdHostage(socket));
                } else {
                    serveFile(path, socket);
                }
            } catch (Exception e) { try { socket.close(); } catch (IOException ignored) {} }
        }).start();
    }

    private static void holdHostage(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            String tName = Thread.currentThread().getName();
            int active = threadPool.getActiveCount();
            int queued = threadPool.getQueue().size();
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 1. Send Headers
            String headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\nConnection: keep-alive\r\n\r\n";
            out.write(headers.getBytes());

            // 2. Send the REAL-TIME Metadata (The UI needs this)
            String json = String.format("{\"thread\":\"%s\", \"active\":%d, \"queue\":%d, \"time\":\"%s\"}\n", 
                          tName, active, queued, time);
            out.write(json.getBytes());
            out.flush();

            // 3. HOSTAGE LOOP - Thread stays here until browser closes
            while (true) {
                out.write(" ".getBytes()); // Keep-alive ping
                out.flush();
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            System.out.println("[Backend] User disconnected, releasing thread.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void serveFile(String path, Socket s) throws IOException {
        String loc = path.equals("/") ? "web/index.html" : "web" + path;
        File f = new File(loc);
        if (!f.exists()) return;
        byte[] content = Files.readAllBytes(f.toPath());
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + content.length + "\r\n\r\n").getBytes());
        out.write(content);
        out.flush();
        s.close();
    }
}