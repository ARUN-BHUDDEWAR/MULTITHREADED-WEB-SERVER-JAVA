package MulthiThread;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.*;

public class MServer {
    private static ThreadPoolExecutor threadPool;

    public static void main(String[] args) {
        // 3 Workers, 10 in the waiting room
        threadPool = new ThreadPoolExecutor(
            3, 3, 0L, TimeUnit.MILLISECONDS, 
            new LinkedBlockingQueue<>(10)
        );

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try { port = Integer.parseInt(portEnv); } catch (NumberFormatException ignored) {}
        }

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("SERVER LIVE - PERSISTENT MODE ON PORT: " + port);
            while (true) {
                Socket client = serverSocket.accept();
                handleRequest(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket socket) {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if (line == null || line.isEmpty()) { socket.close(); return; }
                
                String path = line.split(" ")[1].split("\\?")[0];

                if (path.equals("/status")) {
                    // Push to thread pool - this will block if 3 are busy
                    threadPool.submit(() -> holdThreadHostage(socket));
                } else {
                    serveFile(path, socket);
                }
            } catch (Exception e) { 
                try { socket.close(); } catch (IOException ignored) {} 
            }
        }).start();
    }

    private static void holdThreadHostage(Socket socket) {
        try {
            String threadName = Thread.currentThread().getName();
            System.out.println("[Backend] " + threadName + " is now BUSY. Holding until tab closes...");

            // 1. Send the initial HTTP headers to the browser so it doesn't timeout
            OutputStream out = socket.getOutputStream();
            String header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/event-stream\r\n" + // Use stream to keep connection open
                            "Cache-Control: no-cache\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: keep-alive\r\n\r\n";
            out.write(header.getBytes());
            out.flush();

            // 2. The Hostage Loop
            // This loop runs FOREVER as long as the socket is open
            InputStream in = socket.getInputStream();
            while (true) {
                // Try to send a small "ping" to the browser
                out.write(": ping\n\n".getBytes());
                out.flush();

                // Check if the connection is still alive by trying to read
                // If the user closes the tab, this throws an IOException
                if (socket.isClosed() || !socket.isConnected()) {
                    break;
                }

                Thread.sleep(2000); // Check every 2 seconds
            }

        } catch (Exception e) {
            System.out.println("[Backend] User left. Releasing " + Thread.currentThread().getName());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void serveFile(String path, Socket socket) throws IOException {
        String loc = path.equals("/") ? "web/index.html" : "web" + path;
        File f = new File(loc);
        if (!f.exists() || f.isDirectory()) {
            sendSimpleResponse(socket, "404 Not Found", "File Not Found");
            return;
        }
        String type = loc.endsWith(".css") ? "text/css" : "text/html";
        byte[] content = Files.readAllBytes(f.toPath());
        
        OutputStream out = socket.getOutputStream();
        String res = "HTTP/1.1 200 OK\r\n" +
                     "Content-Type: " + type + "\r\n" +
                     "Content-Length: " + content.length + "\r\n\r\n";
        out.write(res.getBytes());
        out.write(content);
        out.flush();
        socket.close();
    }

    private static void sendSimpleResponse(Socket s, String status, String body) throws IOException {
        OutputStream out = s.getOutputStream();
        String res = "HTTP/1.1 " + status + "\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
        out.write(res.getBytes());
        out.flush();
        s.close();
    }
}