package com.proxyserver.offshoreproxy;

import com.proxyserver.offshoreproxy.constants.OffshorreProxyCodes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

@SpringBootApplication
public class OffshoreproxyApplication {

    public static void main(String[] args) throws IOException {

        SpringApplication.run(OffshoreproxyApplication.class, args);
        startServer();;
    }

    private static void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(OffshorreProxyCodes.PORT)) {
            System.out.println("Offshore server listening on port " + OffshorreProxyCodes.PORT);
            Socket clientSocket = serverSocket.accept();
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                RestTemplate restTemplate = new RestTemplate();
                while (true) {
                    String request = in.readLine();
                    if (request == null) break;
                    String[] parts = request.split(" ", 2);
                    String url = parts[1];
                    String response = restTemplate.getForObject(url, String.class);
                    out.println(response);
                }
            }
        }
    }

    @Bean
    public RestTemplate restTemplate() {

        return new RestTemplate();
    }

}
