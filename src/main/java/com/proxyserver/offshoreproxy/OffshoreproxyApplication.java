package com.proxyserver.offshoreproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import static com.proxyserver.offshoreproxy.constants.OffshorreProxyCodes.PORT;

@SpringBootApplication
public class OffshoreproxyApplication {

    private static final Logger logger = LoggerFactory.getLogger(OffshoreproxyApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OffshoreproxyApplication.class, args);
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            logger.info("Offshore server listening on port {}", PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Offshore proxy accepted connection");
                new Thread(() -> {
                    try {

                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        StringBuilder request = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            request.append(line).append("\r\n");
                        }
                        logger.info("Offshore proxy received: {}", request);
                        String url = request.toString().split(" ")[1];
                        System.out.println("Fetching URL: " + url);
                        String response = fetchWebpage(url);

                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        writer.println(response);

                        logger.info("Sent response to shipproxy: {}", response);

                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fetchWebpage(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "curl/7.79.1");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n");
        response.append("Content-Type: text/html; charset=UTF-8\r\n");
        response.append("\r\n");
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\r\n");
        }
        reader.close();
        return response.toString();
    }
}
