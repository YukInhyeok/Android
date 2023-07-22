package com.example.myapplication.socket;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketClient {
    public void sendToPythonServer(final String message) {
        new Thread(new Runnable() { // 백그라운드 스레드를 생성하여 네트워크 작업을 진행합니다. 안드로이드에서 네트워크 작업은 메인 스레드에서 진행할 수 없으므로 백그라운드 스레드를 사용해야 합니다.
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("192.168.0.40", 8000); // 서버 IP 주소와 포트를 사용하여 소켓 객체를 생성합니다.
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream()); // 소켓 객체로부터 출력 스트림을 가져옵니다.
                    outputStream.writeUTF(message); // message를 출력 스트림에 씁니다.
                    outputStream.flush(); // 출력 스트림의 버퍼를 비웁니다.
                    outputStream.close(); // 출력 스트림을 닫습니다.
                    socket.close(); // 소켓을 닫습니다.
                } catch (IOException e) {
                    e.printStackTrace(); // 예외가 발생한 경우 스택 추적을 출력합니다.
                }
            }
        }).start(); // 백그라운드 스레드를 시작합니다.
    }
}
