package com.lihao.redis.test;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SocketClient {
    public static void main(String[] args) {
        Socket socket = null;
        try{
            socket = new Socket("127.0.0.1",1024);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            System.out.println("请输入内容");
            new Thread(()->{
                while(true){
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    printWriter.println(input);
                    printWriter.flush();
                }
            }).start();
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"GBK");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            new Thread(()->{
                while(true){
                    String readData = null;
                    try {
                        readData = bufferedReader.readLine();
                        System.out.println("服务端发送来消息"+readData);
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
