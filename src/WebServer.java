import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;


public final class WebServer {
    public static void main(String[] args) throws Exception {
        int port = 6789;
        ServerSocket serverSocket = new ServerSocket(port);
        String mimePath = "./mime.types";
        if (args.length == 2 && args[0].equals("-mime")) {
            mimePath = args[1];
        }
        HashMap mimeTypes = loadMimeTypes(mimePath);
        double test2;

        //For each incoming connection a new HttpRequest object (Finished)
        boolean running = true;
        while (running) {
            HttpRequest request = new HttpRequest(serverSocket.accept(), mimeTypes);
            Thread thread = new Thread(request);
            thread.start();
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            System.out.println("Problem while closing the ServerSocket");
        }
    }

    private static HashMap<String, String> loadMimeTypes(String mimeFilePath) {
        HashMap<String, String> mimeType = new HashMap<>();
        String key;
        String value;

        //Check if file is present
        File file = new File(mimeFilePath);
        if (!file.canRead() || !file.isFile()) {
            System.out.println("Could not find mime.types");
            System.exit(0);
        }

        StringTokenizer tokens;
        int tokenCount;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(mimeFilePath));
            String line;
            System.out.println("Loading mime.types");
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("#")) {
                    tokens = new StringTokenizer(line);
                    tokenCount = tokens.countTokens();
                    if (tokenCount < 2) {
                        continue;
                    }
                    value = tokens.nextToken();
                    while (tokens.hasMoreElements()) {
                        key = tokens.nextElement().toString();
                        mimeType.put(key, value);
                        System.out.println(key + " " + value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return mimeType;
    }
}

final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";
    Socket socket;
    HashMap mimeTypes;
    InputStream is;
    DataOutputStream os;
    BufferedReader br;

    public HttpRequest(Socket socket, HashMap mt) throws Exception {
        this.socket = socket;
        mimeTypes = mt;
    }

    @Override
    public void run() {
        try {
            processHttpRequest();
        } catch (Exception e) {
            System.out.println("Error: Connection Closed");
        }
    }

    private void processHttpRequest() throws Exception {

        //Socket input and output streams
        is = socket.getInputStream();
        os = new DataOutputStream(socket.getOutputStream());

        //Filter to read the input
        br = new BufferedReader(new InputStreamReader(is));

        //A timeout time, set to 10Sec.
        // In this time frame the server has to read
        // After this time,the server is closing all streams

        HttpTimeout timeout = new HttpTimeout(this);
        Thread thread = new Thread(timeout);
        thread.start();

        // Get the request line of the HTTP request message.
        String requestLine;
        if ((requestLine = br.readLine()).length() == 0) {
            System.out.println("Empty Message: Connection closed");
            closeConnections();
            return;
        }
        // Display the request line.
        System.out.println();
        System.out.println(requestLine);

        //Tokenize the request line to get the requested file
        StringTokenizer tokens = new StringTokenizer(requestLine);
        String method = tokens.nextToken();
        String fileName = tokens.nextToken();

        // Get and display the header lines.
        String headerLine;
        String userAgent = null;
        int contentLength = 0;
        String content;
        boolean badRequest = false;
        boolean isError = false;
        while ((headerLine = br.readLine()).length() != 0) {
            if (headerLine.startsWith("User-Agent")) {
                userAgent = headerLine;
            }
            if (headerLine.startsWith("Content-Length:")) {
                StringTokenizer contentLengthLine = new StringTokenizer(headerLine);
                contentLengthLine.nextToken();
                try {
                    //Content-Length has to be a positive number or Zero
                    contentLength = Integer.parseInt(contentLengthLine.nextToken());
                    if (contentLength < 0) {
                        badRequest = true;
                    }
                } catch (NumberFormatException e) {
                    badRequest = true;
                    isError = true;
                }
            }
            System.out.println(headerLine);
        }
        //If the the method is POST, read the body
        if (method.equalsIgnoreCase("POST") && !badRequest) {
            isError = true;
            char[] contentArray = new char[contentLength];
            if (br.read(contentArray, 0, contentLength) == -1) {
                System.out.println("Problem while reading ContentBody");
            }
            content = new String(contentArray);
            System.out.println("Post-Body-Content: " + content);
        }

        //At this point everything is read, therefore the time stops here
        timeout.stopTimer();


        //if there is no specific requested URL, send default URL
        String defaultURL = "/index.html";
        if (fileName.equals("/")) {
            fileName = defaultURL;
        }

        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;
        System.out.println("Requested URL: " + fileName);

        // Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("File does not exist.");
            fileExists = false;
        }

        // Construct the response message.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        //Allowed methods are GET and HEAD
        if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD")) {
            if (fileExists) {
                statusLine = "HTTP/1.0 200 OK";
                contentTypeLine = "Content-type: " +
                        contentType(fileName);
            } else {
                statusLine = "HTTP/1.0 404 Not Found";
                contentTypeLine = "Content-type: " + "text/html";
                entityBody = "<html><head>\n" +
                        "<title>404 Not Found</title>\n" +
                        "</head><body>\n" +
                        "<h1>Not Found</h1>\n" +
                        "<p>The requested URL " + fileName + " was not found on this server.</p>\n" +
                        "<p>Client-Information IP: " + socket.getInetAddress() + " </p>\n" +
                        "<p>Client-Information " + userAgent + " </p>\n" +
                        "<hr>\n" +
                        "<address>Server at " + socket.getLocalAddress() + " Port " + socket.getLocalPort() + "</address>\n" +
                        "</body></html>";
            }
        }
        if (isError) {
            //Not allowed method get "501 Not Implemented"-Response
            statusLine = "HTTP/1.0 501 Not Implemented";
            contentTypeLine = "Content-type: " + "text/html";
            entityBody = "<html><head>\n" +
                    "<title>501 Not Implemented</title>\n" +
                    "</head><body>\n" +
                    "<h1>Not Implemented</h1>\n" +
                    "<p>The request-method you use is not implemented for now.</p>\n" +
                    "<p>We are continuously improving our WebServer.</p>\n" +
                    "<p>Contact: justin.marks@hhu.de.</p>\n" +
                    "<hr>\n" +
                    "<address>Server at " + socket.getLocalAddress() + " Port " + socket.getLocalPort() + "</address>\n" +
                    "</body></html>";
        }
        if (badRequest) {
            //"Bad Request"-Response
            statusLine = "HTTP/1.0 400 Bad Request";
            contentTypeLine = "Content-type: " + "text/html";
            entityBody = "<html><head>\n" +
                    "<title>400 Bad Request</title>\n" +
                    "</head><body>\n" +
                    "<h1>Bad Request</h1>\n" +
                    "<p>The request-method you use contains a faulty line.</p>\n" +
                    "<p>Contact: justin.marks@hhu.de.</p>\n" +
                    "<hr>\n" +
                    "<address>Server at " + socket.getLocalAddress() + " Port " + socket.getLocalPort() + "</address>\n" +
                    "</body></html>";
        }

        // Send the status line.
        os.writeBytes(statusLine + CRLF);

        // Send the content type line.
        os.writeBytes(contentTypeLine + ";" + CRLF);


        //
        String dateLine = "Date : " + getTime();
        os.writeBytes(dateLine + CRLF);

        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);

        // Send the entity body.
        if (!method.equals("HEAD")) {
            if (fileExists && !isError) {
                sendBytes(fis, os);
                try {
                    fis.close();
                } catch (Exception e) {
                    System.out.println("Problem while closing the FileInputStream");
                }
            } else {
                if (entityBody != null) {
                    os.writeBytes(entityBody);
                }
            }
        }
        //Close all streams
        closeConnections();


    }

    private String contentType(String fileName) {
        try {
            return mimeTypes.get(fileName.substring(fileName.lastIndexOf('.') + 1)).toString();
        } catch (NullPointerException e) {
            return "application/octet-stream";
        }
    }

    private static void sendBytes(FileInputStream fis, OutputStream os)
            throws Exception {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes;

        // Copy requested file into the socket's output stream.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    public void closeConnections() {
        //Close streams and socket.
        //Close BufferedReader
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (Exception e) {
            System.out.println("Problem while closing the BufferedReader");
        }
        //Close InputStream
        try {
            is.close();
        } catch (Exception e) {
            System.out.println("Problem while closing the InputStream");
        }
        //Close DataOutputStream
        try {
            os.close();
        } catch (Exception e) {
            System.out.println("Problem while closing the DataOutputStream");
        }

    }

}

final class HttpTimeout implements Runnable {
    private boolean isStop = false;
    private HttpRequest parent;

    public HttpTimeout(HttpRequest p) {
        this.parent = p;
    }

    @Override
    public void run() {
        final long timeStart = System.currentTimeMillis();

        int timeout = 15000;
        while (System.currentTimeMillis()-timeStart< timeout) {
            //waiting
        }
        if (!isStop) {
            parent.closeConnections();
        }
    }

    public void stopTimer() {
        isStop = true;
    }
}