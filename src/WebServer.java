import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JuFey on 09.11.2015.
 */
public final class WebServer {
    public static void main(String[] args) throws IOException {
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

    Socket connectionSocket;

    public HttpRequest(Socket socket) {
        connectionSocket = socket;
    }

    @Override
    public void run() {


    }
}