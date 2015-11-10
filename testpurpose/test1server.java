package testpurpose;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by jufey on 07.11.15.
 */
public class test1server {
    public static void main(String[] args) throws IOException {
        String clientSentence;
        String modifiedSentence;
        ServerSocket welcomeSocket = new ServerSocket(6789);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());


            clientSentence = inFromClient.readLine();
            modifiedSentence = clientSentence.toUpperCase()+"\n";
            outToClient.writeBytes("Shun muthafukaaaa\n");
            connectionSocket.close();

        }
    }


}
