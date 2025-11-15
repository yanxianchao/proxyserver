package com.xianchao.proxyserver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * HTTP代理服务器主类
 */
@Component
public class HttpProxyServer implements CommandLineRunner {
    private static final int DEFAULT_PORT = 6666;
    private volatile ServerSocket serverSocket;

    @Override
    public void run(String... args) throws Exception {
        startServer(DEFAULT_PORT);
    }

    /**
     * 启动代理服务器
     *
     * @param port 监听端口
     * @throws IOException IO异常
     */
    public void startServer(int port) throws IOException {

        if (serverSocket != null) return;

        serverSocket = new ServerSocket(port);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));

        System.out.println("HTTP Proxy server started on port " + port);

        Thread serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    // 为每个客户端连接创建一个新的处理器
                    ConnectionHandler handler = new ClientConnectionHandler(clientSocket);
                    Thread thread = new Thread(handler);
                    thread.start();
                }
            } catch (IOException e) {
                System.err.println("Error in proxy server: " + e.getMessage());
            }
        });

        serverThread.setDaemon(false);
        serverThread.start();
    }

    /**
     * 停止代理服务器
     */
    public void stopServer() {
        System.out.println("Stopping HTTP Proxy server...");
        if (serverSocket == null) return;
        // 关闭服务器套接字
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}