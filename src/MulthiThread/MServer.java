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
        // Strict 3-thread pool for System Design demonstration
        threadPool = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try { port = Integer.parseInt(portEnv); } catch (NumberFormatException ignored) {}
        }

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("SERVER LIVE ON PORT: " + port);
            
            while (true) {
                Socket client = serverSocket.accept();
                // Direct execution for files, pooled execution for /status
                handleRequest(client);
            }
        } catch (IOException e) {
            System.err.println("Critical Server Error: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket socket) {
        // We use a temporary thread only to read the header so the main loop doesn't block
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if (line == null || line.isEmpty()) { socket.close(); return; }
                
                String path = line.split(" ")[1].split("\\?")[0];

                if (path.equals("/status")) {
                    // This is handled by our constrained pool
                    threadPool.submit(() -> processRequest(socket));
                } else {
                    // Static files handled immediately
                    serveFile(path, socket);
                }
            } catch (Exception e) { 
                try { socket.close(); } catch (IOException ignored) {} 
            }
        }).start();
    }

    private static void processRequest(Socket socket) {
        try {
            int activeAtStart = threadPool.getActiveCount();
            int queueAtStart = threadPool.getQueue().size();

            System.out.println("[Backend] Processing - Active: " + activeAtStart + " Queue: " + queueAtStart);

            Thread.sleep(6000); // Simulated work

            String threadName = Thread.currentThread().getName();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String json = String.format(
                "{\"status\":\"SUCCESS\",\"thread\":\"%s\",\"time\":\"%s\",\"active\":%d,\"queue\":%d}",
                threadName, time, activeAtStart, queueAtStart
            );

            sendHttp(socket, "200 OK", "application/json", json);
        } catch (Exception e) { 
            e.printStackTrace(); 
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void serveFile(String path, Socket socket) throws IOException {
        String loc = path.equals("/") ? "web/index.html" : "web" + path;
        File f = new File(loc);
        
        if (!f.exists() || f.isDirectory()) { 
            sendHttp(socket, "404 Not Found", "text/plain", "File Not Found"); 
            return; 
        }

        String type = loc.endsWith(".css") ? "text/css" : "text/html";
        byte[] content = Files.readAllBytes(f.toPath());
        sendHttp(socket, "200 OK", type, new String(content));
        socket.close();
    }

    private static void sendHttp(Socket s, String status, String type, String body) throws IOException {
        OutputStream out = s.getOutputStream();
        String res = "HTTP/1.1 " + status + "\r\n" +
                     "Content-Type: " + type + "\r\n" +
                     "Content-Length: " + body.getBytes().length + "\r\n" +
                     "Access-Control-Allow-Origin: *\r\n" + 
                     "Connection: close\r\n\r\n" + body;
        out.write(res.getBytes());
        out.flush();
    }
}