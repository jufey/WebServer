import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;


public final class WebServer {
    public static void main(String[] args) throws Exception {
        int port = 6789;
        ServerSocket serverSocket = new ServerSocket(port);

        //For each incoming connection a new HttpRequest object (Finished)
        while (true) {
            HttpRequest request = new HttpRequest(serverSocket.accept());
            Thread thread = new Thread(request);
            thread.start();
        }
    }
}

final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";

    Socket socket;

    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            processHttpRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processHttpRequest() throws Exception {

        //Socket input and output streams
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        //Filter to read the input
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // Get the request line of the HTTP request message.
        String requestLine = br.readLine();

        // Display the request line.
        System.out.println();
        System.out.println(requestLine);

        // Get and display the header lines.
        String headerLine = null;
        String userAgent = null;
        while ((headerLine = br.readLine()).length() != 0) {
            if (headerLine.startsWith("User-Agent")) {
                userAgent = headerLine;
            }
            System.out.println(headerLine);
        }

        //Tokenize the request line to get the requested file
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();
        String fileName = tokens.nextToken();

        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;

        System.out.println(fileName);

        // Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }


        // Construct the response message.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK";
            contentTypeLine = "Content-type: " +
                    contentType(fileName) + CRLF;
        } else {
            statusLine = "HTTP/1.0 404 Not Found";
            contentTypeLine = "text/html";
            entityBody = "<html><head>\n" +
                    "<title>404 Not Found</title>\n" +
                    "</head><body>\n" +
                    "<h1>Not Found</h1>\n" +
                    "<p>The requested URL " + fileName + " was not found on this server.</p>\n" +
                    "<p>Client-Information IP: " + socket.getInetAddress() + " </p>\n" +
                    "<p>Client-Information "+userAgent+" </p>\n" +
                    "<hr>\n" +
                    "<address>Server at " + socket.getLocalAddress() + " Port 6789</address>\n" +
                    "</body></html>";
        }

        // Send the status line.
        os.writeBytes(statusLine + CRLF);

        // Send the content type line.
        os.writeBytes("Content-Type: " + contentTypeLine + ";" + CRLF);

        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);

        // Send the entity body.
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(entityBody);
        }


        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
    }

    //TODO
    private String contentType(String fileName) {

        return "";
    }
    private static void sendBytes(FileInputStream fis, OutputStream os)
            throws Exception
    {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
            os.write(buffer, 0, bytes);
        }
    }
}