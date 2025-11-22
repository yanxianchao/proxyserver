package com.xianchao.proxyserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 客户端连接处理器
 * 负责处理单个客户端连接的所有操作
 */
public record ClientConnectionHandler(Socket clientSocket) implements ConnectionHandler {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public void run() {
        try {
            System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
            // 解析目标主机和端口
            String[] hostPort = parseTargetHost(getRequestLine());
            // 创建服务套接字
            Socket serverSocket = new Socket(hostPort[0], Integer.parseInt(hostPort[1]));
            // 发送连接成功的响应给客户端
            sendConnectionEstablishedResponse();
            System.out.println("Connection established.");
            // 创建并启动客户端到服务器的数据传输线程
            Thread clientToServerThread = createRelayThread(clientSocket, serverSocket);
            // 创建并启动服务器到客户端的数据传输线程
            Thread serverToClientThread = createRelayThread(serverSocket, clientSocket);
            // 启动线程
            clientToServerThread.start();
            serverToClientThread.start();
        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
            closeQuietly(clientSocket);
        }
    }

    private String getRequestLine() throws IOException {
        // 读取客户端的第一行请求
        String requestLine = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)).readLine();
        if (requestLine == null || requestLine.isEmpty())
            throw new RuntimeException("Invalid request line: " + requestLine);
        return requestLine;
    }

    /**
     * 解析目标主机和端口
     *
     * @param requestLine 请求行
     * @return 包含主机和端口的数组
     */
    private String[] parseTargetHost(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2)
            throw new RuntimeException("Invalid request line: " + requestLine);

        String[] hostPort = parts[1].split(":");
        if (hostPort.length < 2)
            throw new RuntimeException("Invalid target host: " + parts[1]);

        return hostPort;
    }

    /**
     * 发送连接建立成功的响应
     *
     * @throws IOException IO异常
     */
    private void sendConnectionEstablishedResponse() throws IOException {
        String response = "HTTP/1.1 200 Established\r\nProxy-Agent: Simple-Http-Proxy/1.0\r\n\r\n";
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建数据转发线程
     *
     * @param source      源套接字
     * @param destination 目标套接字
     * @return 数据转发线程
     */
    private Thread createRelayThread(Socket source, Socket destination) {
        return new Thread(() -> {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = source.getInputStream().read(buffer)) != -1) {
                    destination.getOutputStream().write(buffer, 0, bytesRead);
                    destination.getOutputStream().flush();
                }
            } catch (Exception e) {
                System.err.println("Error relaying data: " + e.getMessage());
            } finally {
                closeQuietly(source);
                closeQuietly(destination);
            }
        });
    }

    /**
     * 安静地关闭套接字
     *
     * @param socket 套接字
     */
    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略异常
            }
        }
    }
}