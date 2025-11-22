package com.xianchao.proxyserver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * HTTP代理服务器主类
 */
@Component
public class HttpProxyServer implements CommandLineRunner {

    private static final int DEFAULT_PORT = 443;
    private volatile ServerSocket serverSocket;

    private static final ThreadPoolExecutor bossThreadPool =
            new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private static final ThreadPoolExecutor workerThreadPool =
            new ThreadPoolExecutor(10, 10, 0L, MILLISECONDS, new LinkedBlockingQueue<Runnable>());

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
        System.out.println("HTTP Proxy server started on port " + port);
        // 添加关闭钩子，在JVM关闭时停止代理服务器
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));
        new Thread(()->{
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    //clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                     //为每个客户端连接创建一个新的处理器
                    ConnectionHandler handler = new ClientConnectionHandler(clientSocket,workerThreadPool);
                    bossThreadPool.execute(handler);
                }
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + Arrays.toString(e.getStackTrace()));
            } finally {
                stopServer();
            }
        }).start();
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
        // 关闭线程池
        bossThreadPool.shutdown();
        workerThreadPool.shutdown();
    }
}