package model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    // storing our connection tools as variables so we can use them anywhere in this class
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // connects to the server and opens the object streams
    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    // generic method to send any request and get the response back
    public Response sendRequest(Request request) throws Exception {
        if (socket == null || socket.isClosed()) {
            throw new IllegalStateException("no connection to server!");
        }

        // packaging and sending the request box
        out.writeObject(request);
        out.flush();

        // waiting for the server to reply and returning that response box
        return (Response) in.readObject();
    }

    // closes everything safely
    public void disconnect() {
        try {
            if (out != null) {
                // saying goodbye so the server can close its side cleanly
                out.writeObject(new Request(Command.BYE, null));
                out.flush();
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            // ignoring errors during disconnect
        }
    }
}