package multithread;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class MServer {
    private static ThreadPoolExecutor threadPool;

    public static void main(String[] args) {
        int port = 8080;
        // Strict 3-thread pool: This is your "System Design" constraint
        threadPool = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try { port = Integer.parseInt(portEnv); } catch (NumberFormatException ignored) {}
        }

        ServerSocket serverSocket = null;
        int attempts = 0;
        while (attempts < 10) {
            try {
                serverSocket = new ServerSocket(port);
                break;
            } catch (IOException be) {
                if (be instanceof BindException) {
                    System.out.println("Port " + port + " in use, trying " + (port + 1));
                    port++;
                    attempts++;
                } else {
                    be.printStackTrace();
                    return;
                }
            }
        }

        if (serverSocket == null) {
            System.err.println("Failed to bind to a port after multiple attempts.");
            return;
        }

        try (ServerSocket s = serverSocket) {
            System.out.println("SERVER LIVE: Open http://localhost:" + s.getLocalPort() + " in your browser");
            while (true) {
                Socket client = s.accept();
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
                    // Logic: If 3 threads are busy, this task waits in the LinkedBlockingQueue
                    threadPool.submit(() -> processRequest(socket));
                } else {
                    serveFile(path, socket);
                }
            } catch (Exception e) { try { socket.close(); } catch (IOException ignored) {} }
        }).start();
    }

    private static void processRequest(Socket socket) {
        try {
            // Snapshot variables declared at start of method to capture peak concurrency
            int activeAtStart = threadPool.getActiveCount();
            int queueAtStart = threadPool.getQueue().size();

            // Immediate logging so the backend prints the concurrency snapshot
            System.out.println("[Backend] Received - Active: " + activeAtStart + " Queue: " + queueAtStart);

            // Artificial delay so you can actually see the queue happening
            Thread.sleep(6000);

            String threadName = Thread.currentThread().getName();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // JSON must have double quotes for keys and values to be valid
            String json = "{"
                + "\"status\":\"SUCCESS\"," 
                + "\"thread\":\"" + threadName + "\"," 
                + "\"time\":\"" + time + "\"," 
                + "\"active\":" + activeAtStart + "," 
                + "\"queue\":" + queueAtStart
                + "}";

            sendHttp(socket, "200 OK", "application/json", json);
            System.out.println("[Backend] Task completed by " + threadName + " (snapshot active=" + activeAtStart + ", queue=" + queueAtStart + ")");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void serveFile(String path, Socket socket) throws IOException {
        String loc = path.equals("/") ? "web/index.html" : "web" + path;
        File f = new File(loc);
        if (!f.exists()) { 
            sendHttp(socket, "404 Not Found", "text/plain", "File Not Found"); 
            return; 
        }
        String type = loc.endsWith(".css") ? "text/css" : "text/html";
        byte[] content = Files.readAllBytes(f.toPath());
        sendHttp(socket, "200 OK", type, new String(content));
    }

    private static void sendHttp(Socket s, String status, String type, String body) throws IOException {
        OutputStream out = s.getOutputStream();
        // Access-Control-Allow-Origin: * prevents the "Server Unreachable" error
        String res = "HTTP/1.1 " + status + "\r\n" +
                     "Content-Type: " + type + "\r\n" +
                     "Content-Length: " + body.getBytes().length + "\r\n" +
                     "Access-Control-Allow-Origin: *\r\n" + 
                     "Connection: close\r\n\r\n" + body;
        out.write(res.getBytes());
        out.flush();
        s.close();
    }
}