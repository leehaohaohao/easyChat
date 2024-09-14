package com.lihao.redis.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(1024);
            System.out.println("服务已启动");
            Socket socket = serverSocket.accept();
            String ip = socket.getInetAddress().getHostAddress();
            System.out.println("客户端链接ip" + ip + ",端口" + socket.getPort());
            new Thread(() -> {
                while(true){
                    try {
                        InputStream inputStream = socket.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "GBK");
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String readData = bufferedReader.readLine();
                        System.out.println("客户端："+readData);

                        OutputStream outputStream = socket.getOutputStream();
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream,"GBK");
                        PrintWriter printWriter = new PrintWriter(outputStreamWriter);
                        printWriter.println("服务器端："+readData);
                        printWriter.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
