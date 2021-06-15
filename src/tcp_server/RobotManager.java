package tcp_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RobotManager implements Runnable {
    PrintWriter out;
    BufferedReader in;
    Socket robotSocket;
    StateManager stateManager;

    public RobotManager(Socket robotSocket) throws Exception {
        this.out = new PrintWriter(robotSocket.getOutputStream());
        this.in = new BufferedReader(
                new InputStreamReader(robotSocket.getInputStream()));
        this.robotSocket = robotSocket;
        this.stateManager = new StateManager();
    }

    // TODO: logs (see lo4j for example)
    @Override
    public void run() {
        try {
            while (true) {
                robotSocket.setSoTimeout(stateManager.getTimeout());
                System.out.println("INFO: Socket timeout set to: " + stateManager.getTimeout());
                StringBuilder msgBuilder = readInput();
                System.out.println("INFO: Message received: " + msgBuilder.toString());
                Response response = createResponse(msgBuilder);
                System.out.println("INFO: Response message created: " + response.message);
                if (!response.message.equals(Response.EMPTY)) {
                    boolean closeConnectionAfterResponse = sendResponse(response);
                    System.out.println("INFO: Response sent");
                    if (closeConnectionAfterResponse)
                        break;
                }
            }
            System.out.println("INFO: Connection closed successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }


    private boolean sendResponse(Response response) {
        if (response.message.equals("200 OK\u0007\b")) {
            out.print(response.message);
            out.flush();
            response = stateManager.getResponse("");
            System.out.println("INFO: Response message created: " + response.message);
        }
        out.print(response.message);
        out.flush();
        return response.closeAfter;
    }


    private Response createResponse(StringBuilder msgBuilder) {
        Response response;
        String msg = msgBuilder.toString();
        if (msgBuilder.charAt(msgBuilder.length() - 2) == '\u0007' && msgBuilder.charAt(msgBuilder.length() - 1) == '\b') {
            msg = msg.substring(0, msg.length() - 2);
            response = stateManager.getResponse(msg);
        } else {
            response = stateManager.checkIfMessageValid(msg);
        }
        return response;
    }


    private StringBuilder readInput() throws IOException {
        StringBuilder msgBuilder = new StringBuilder();

        // Yeah, quite ugly.
        int firstChar;
        int nextChar;
        while ((firstChar = in.read()) != -1) {
            if ((char) firstChar == '\u0007') {
                if ((nextChar = in.read()) != -1) {
                    msgBuilder.append((char) firstChar);
                    if (nextChar == '\b') {
                        msgBuilder.append((char) nextChar);
                        break;
                    } else {
                        if (!stateManager.isInputValid(msgBuilder.toString())) {
                            break;
                        }
                        msgBuilder.append((char) nextChar);
                        if (!stateManager.isInputValid(msgBuilder.toString())) {
                            break;
                        }
                        continue;
                    }
                } else {
                    msgBuilder.append((char) firstChar);
                    break;
                }
            }
            msgBuilder.append((char) firstChar);
            if (!stateManager.isInputValid(msgBuilder.toString())) {
                break;
            }
        }
        return msgBuilder;
    }

    private void cleanup() {
        try {
            robotSocket.close();
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
