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
        // Core: 3 threads, Max: 3 threads. 
        // Queue: 10 (The 4th device stays here until a thread is free)
        threadPool = new ThreadPoolExecutor(
            3, 3, 0L, TimeUnit.MILLISECONDS, 
            new LinkedBlockingQueue<>(10)
        );

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try { port = Integer.parseInt(portEnv); } catch (NumberFormatException ignored) {}
        }

        // Bind to 0.0.0.0 for Render compatibility
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("SERVER LIVE ON PORT: " + port);
            
            while (true) {
                Socket client = serverSocket.accept();
                handleRequest(client);
            }
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket socket) {
        // This thread just reads the HTTP header so the main loop can accept the next person
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if (line == null || line.isEmpty()) { socket.close(); return; }
                
                String path = line.split(" ")[1].split("\\?")[0];

                if (path.equals("/status")) {
                    // Task is submitted to the pool. 
                    // If 3 are busy, this line "completes" but the task stays in the Queue.
                    threadPool.submit(() -> processRequest(socket));
                } else {
                    serveStaticFile(path, socket);
                }
            } catch (Exception e) { 
                try { socket.close(); } catch (IOException ignored) {} 
            }
        }).start();
    }

    private static void processRequest(Socket socket) {
        try {
            // These stats are captured ONLY when the task actually starts running
            int active = threadPool.getActiveCount();
            int queue = threadPool.getQueue().size();

            System.out.println("[Backend] Starting Task - Active: " + active + " | Waiting in Queue: " + queue);

            // 8 second delay so you have plenty of time to open the 4th device
            Thread.sleep(8000); 

            String threadName = Thread.currentThread().getName();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String json = String.format(
                "{\"status\":\"SUCCESS\",\"thread\":\"%s\",\"time\":\"%s\",\"active\":%d,\"queue\":%d}",
                threadName, time, active, queue
            );

            sendHttp(socket, "200 OK", "application/json", json);
            System.out.println("[Backend] Finished Task: " + threadName);
        } catch (Exception e) { 
            e.printStackTrace(); 
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void serveStaticFile(String path, Socket socket) throws IOException {
        String loc = path.equals("/") ? "web/index.html" : "web" + path;
        File f = new File(loc);
        if (!f.exists() || f.isDirectory()) { 
            sendHttp(socket, "404 Not Found", "text/plain", "Not Found"); 
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