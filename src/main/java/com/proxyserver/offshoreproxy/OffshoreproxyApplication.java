package com.proxyserver.offshoreproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.proxyserver.offshoreproxy.constants.OffshorreProxyCodes.PORT;

@SpringBootApplication
public class OffshoreproxyApplication {

    private static final Logger logger = LoggerFactory.getLogger(OffshoreproxyApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OffshoreproxyApplication.class, args);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println();
            logger.info("Offshore server listening on port {}", PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Offshore proxy accepted connection");
                executor.submit(() -> {
                    try (Socket s = socket) {
                        long startTime = System.currentTimeMillis();

                        // Read request from shipproxy
                        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        StringBuilder request = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            request.append(line).append("\r\n");
                            logger.info("Received request line: {}", line);
                        }
                        logger.info("Full request received:  {}", request);

                        // Parse the URL
                        String url = request.toString().split(" ")[1];
                        logger.info("Fetching URL: {}", url);

                        // Fetch webpage
                        long fetchStartTime = System.currentTimeMillis();
                        String response = fetchWebpage(url);
                        long fetchEndTime = System.currentTimeMillis();
                        System.out.println((fetchEndTime - fetchStartTime) + " ms");

                        // Send response back to shipproxy
                        PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                        writer.println(response);
                        logger.info("Sent response to shipproxy: {}", response);

                        long totalTime = System.currentTimeMillis() - startTime;
                        System.out.println("Total time to process request in offshoreserver: " + totalTime + " ms");
                    } catch (IOException e) {
                        logger.error("Error processing request:  {}", e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static String fetchWebpage(String urlStr) throws IOException {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(urlStr)).header("User-Agent", "curl/7.79.1").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 200 OK\r\n");
            responseBuilder.append("Content-Type: text/html; charset=UTF-8\r\n");
            int contentLength = response.body().length();
            responseBuilder.append("Content-Length: ").append(contentLength).append("\r\n");
            responseBuilder.append("\r\n");
            responseBuilder.append(response.body());
            return responseBuilder.toString();
        } catch (Exception e) {
            logger.error("fetchWebpage | Error processing request:  {}", e.getMessage());
            return "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n";
        }
    }
}