import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;


public final class WebServer {
    public static void main(String[] args) throws Exception {
        int port = 6789;
        ServerSocket serverSocket = new ServerSocket(port);
        String mimePath = "./mime.types";
        if (args.length == 2 && args[0].equals("-mime")) {
            mimePath = args[1];
        }
        HashMap mimeTypes = loadMimeTypes(mimePath);

        //For each incoming connection a new HttpRequest object (Finished)
        while (true) {
            HttpRequest request = new HttpRequest(serverSocket.accept(), mimeTypes);
            Thread thread = new Thread(request);
            thread.start();
        }
    }

    private static HashMap loadMimeTypes(String mimeFilePath) {
        HashMap mimetype = new HashMap();
        String key;
        String value;

        //Check if file is present
        File file = new File(mimeFilePath);
        if (!file.canRead() || !file.isFile()) {
            System.out.println("Could not find mime.types");
            System.exit(0);
        }

        StringTokenizer tokens;
        int tokencount;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(mimeFilePath));
            String zeile = null;
            System.out.println("Loading mime.types");
            while ((zeile = in.readLine()) != null) {
                if (!zeile.startsWith("#")) {
                    tokens = new StringTokenizer(zeile);
                    tokencount = tokens.countTokens();
                    if (tokencount < 2) {
                        continue;
                    }
                    value = tokens.nextToken();
                    while (tokens.hasMoreElements()) {
                        key = tokens.nextElement().toString();
                        mimetype.put(key, value);
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
                }
        }
        return mimetype;
    }
}

final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";
    Socket socket;
    HashMap mimeTypes;

    public HttpRequest(Socket socket, HashMap mt) throws Exception {
        this.socket = socket;
        mimeTypes = mt;
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
        String headerLine;
        String userAgent = null;
        while ((headerLine = br.readLine()).length() != 0) {
            if (headerLine.startsWith("User-Agent")) {
                userAgent = headerLine;
            }
            System.out.println(headerLine);
        }

        //Tokenize the request line to get the requested file
        StringTokenizer tokens = new StringTokenizer(requestLine);
        String method = tokens.nextToken();
        System.out.println(method);
        String fileName = tokens.nextToken();

        //if there is no specific requested URL, send default URL
        String defaultURL = "/index.html";
        if (fileName.equals("/")) {
            fileName = defaultURL;
        }

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
        String statusLine;
        String contentTypeLine;
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
                        "<address>Server at " + socket.getLocalAddress() + " Port "+socket.getLocalPort()+"</address>\n" +
                        "</body></html>";
            }
        }else{
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
                    "<address>Server at " + socket.getLocalAddress() + " Port 6789</address>\n" +
                    "</body></html>";
        }


        // Send the status line.
        os.writeBytes(statusLine + CRLF);

        // Send the content type line.
        os.writeBytes(contentTypeLine + ";" + CRLF);

        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);

        // Send the entity body.
        if (!method.equals("HEAD")) {
            if (fileExists) {
                sendBytes(fis, os);
                fis.close();
            } else {
                os.writeBytes(entityBody);
            }
        }


        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
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
        int bytes = 0;

        // Copy requested file into the socket's output stream.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }
}