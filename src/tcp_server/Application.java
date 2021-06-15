package tcp_server;

import java.net.ServerSocket;
import java.net.Socket;

public class Application {
    public static void main(String[] args) {
        ServerSocket socket;
        try {
            socket = new ServerSocket(6666);
            System.out.println("Server socket is opened on: " + socket.getLocalSocketAddress());
            while (true) {
                Socket robotSocket = socket.accept();
                RobotManager robotManager = new RobotManager(robotSocket);
                System.out.println("Robot socket is opened on: " + robotSocket.getLocalSocketAddress());
                new Thread(robotManager).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //TODO: close resources
        }
    }
}
