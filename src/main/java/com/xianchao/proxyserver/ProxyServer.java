package com.xianchao.proxyserver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class ProxyServer implements CommandLineRunner {

    public void run(String... args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            System.out.println("Proxy server started on port 6666");
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                new Thread(new ClientProcess(client)).start();
            }
        }
    }
}

