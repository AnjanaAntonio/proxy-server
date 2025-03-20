package proxyserver;

import java.io.*;
import java.net.*;


public class OffshoreProxy {
    private static final int OFFSHORE_PROXY_PORT = 9090;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(OFFSHORE_PROXY_PORT)) {
            System.out.println("Offshore Proxy running on port " + OFFSHORE_PROXY_PORT);

            Socket shipSocket = serverSocket.accept();
            System.out.println("Connected to Ship Proxy: " + shipSocket.getInetAddress());

            handleShipRequests(shipSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleShipRequests(Socket shipSocket) {
        try (
            InputStream shipInput = shipSocket.getInputStream();
            OutputStream shipOutput = shipSocket.getOutputStream()
        ) {
            byte[] buffer = new byte[8192];

            while (true) {
                int bytesRead = shipInput.read(buffer);
                if (bytesRead <= 0) break;

                String httpRequest = new String(buffer, 0, bytesRead);
                String url = extractURL(httpRequest);

                System.out.println("Fetching URL: " + url);

                String response = fetchHttpResponse(url);

                shipOutput.write(response.getBytes());
                shipOutput.flush();
            }
        } catch (IOException e) {
            System.err.println("Error processing Ship Proxy request.");
            e.printStackTrace();
        }
    }

    private static String extractURL(String httpRequest) {
        try {
            String firstLine = httpRequest.split("\n")[0];
            String[] parts = firstLine.split(" ");
            return parts.length > 1 ? parts[1] : "";
        } catch (Exception e) {
            System.err.println("Error extracting URL.");
            return "";
        }
    }

    private static String fetchHttpResponse(String url) {
        try {
            System.out.println("Attempting to fetch: " + url);

            URI uri = new URI("http://httpforever.com");
            URL website = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) website.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();

            System.out.println(" Response fetched successfully");
            return "HTTP/1.1 200 OK\n\n" + response.toString();

        } catch (IOException e) {
            System.err.println(" ERROR: Failed to fetch the URL: " + url);
            e.printStackTrace();
            return " Error In fetching response";
        } catch (URISyntaxException e) {
			e.printStackTrace();
			return " Error: Invalid URL format";
		}
    }

}
