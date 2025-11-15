package com.xianchao.proxyserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientProcess implements Runnable {

    private final Socket client;

    public ClientProcess(Socket client) throws IOException {
        this.client = client;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            // 一行一行的读取客户端数据
            String line = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)).readLine();
            if (line == null) {
                closeQuietly( client);
                return;
            }

            String[] parts = line.split(" ");
            if (parts.length < 2) {
                System.err.println("Invalid request line: " + line);
                closeQuietly( client);
                return;
            }

            String[] hostPort = parts[1].split(":");
            if (hostPort.length < 2) {
                System.err.println("Invalid host:port format: " + parts[1]);
                closeQuietly( client);
                return;
            }

            // 连接到目标服务器
            try (Socket server = new Socket(hostPort[0], Integer.parseInt(hostPort[1]))) {
                //代理服务器给客户的响应成功
                client.getOutputStream().write("HTTP/1.1 200 Established\r\nProxy-Agent: Some-Proxy/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));

                // Create and start client-to-server thread
                Thread clientToServerThread = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while (!client.isClosed() && !server.isClosed() && (bytesRead = client.getInputStream().read(buffer)) != -1) {
                            server.getOutputStream().write(buffer, 0, bytesRead);
                            server.getOutputStream().flush();
                        }
                    } catch (IOException e) {
                        // Connection closed or error - normal condition
                    } finally {
                        closeQuietly(server);
                        closeQuietly(client);
                    }
                });

                // Create and start server-to-client thread
                Thread serverToClientThread = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while (!client.isClosed() && !server.isClosed() && (bytesRead = server.getInputStream().read(buffer)) != -1) {
                            client.getOutputStream().write(buffer, 0, bytesRead);
                            client.getOutputStream().flush();
                        }
                    } catch (IOException e) {
                        // Connection closed or error - normal condition
                    } finally {
                        closeQuietly(server);
                        closeQuietly(client);
                    }
                });

                clientToServerThread.start();
                serverToClientThread.start();

                try {
                    clientToServerThread.join();
                    serverToClientThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing client request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeQuietly(client);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}