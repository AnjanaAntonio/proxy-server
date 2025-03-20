package proxyserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ShipProxy {
    private static final int LOCAL_PROXY_PORT = 8080;
    private static final String OFFSHORE_SERVER_HOST = "127.0.0.1";
    private static final int OFFSHORE_SERVER_PORT = 9090;
    
    private static Socket offshoreSocket;
    private static BlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_PROXY_PORT)) {
            System.out.println("Ship Proxy running on port " + LOCAL_PROXY_PORT);

            connectToOffshoreProxy();

            new Thread(ShipProxy::processRequests).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New request from: " + clientSocket.getInetAddress());
                requestQueue.put(clientSocket);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void connectToOffshoreProxy() {
        try {
            offshoreSocket = new Socket(OFFSHORE_SERVER_HOST, OFFSHORE_SERVER_PORT);
            System.out.println("Connected to Offshore Proxy at " + OFFSHORE_SERVER_HOST + ":" + OFFSHORE_SERVER_PORT);
        } catch (IOException e) {
            System.err.println(" Failed to connect to Offshore Proxy. Ensure it's running.");
            e.printStackTrace();
        }
    }

    private static void processRequests() {
        while (true) {
            try {
                Socket clientSocket = requestQueue.take();
                handleClient(clientSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream clientInput = clientSocket.getInputStream();
             OutputStream clientOutput = clientSocket.getOutputStream();
             OutputStream offshoreOutput = offshoreSocket.getOutputStream();
             InputStream offshoreInput = offshoreSocket.getInputStream()) {

            byte[] buffer = new byte[4096];

            int bytesRead = clientInput.read(buffer);
            if (bytesRead > 0) {
                System.out.println("Request received from browser: \n" + new String(buffer, 0, bytesRead));
                offshoreOutput.write(buffer, 0, bytesRead);
                offshoreOutput.flush();
            }

            bytesRead = offshoreInput.read(buffer);
            if (bytesRead > 0) {
                System.out.println("Response received from Offshore Proxy: \n" + new String(buffer, 0, bytesRead));
                clientOutput.write(buffer, 0, bytesRead);
                clientOutput.flush();
            } else {
                System.err.println("No response received from Offshore Proxy!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
