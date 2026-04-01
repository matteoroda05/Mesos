package it.polimi.ingsw.mesos.Network.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket client_socket;

    public ClientHandler(Socket client_socket) {
        this.client_socket = client_socket;
    }

    @Override
    public void run() {
        clientLoop();
    }

    public void clientLoop() {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(
                    new InputStreamReader(client_socket.getInputStream()));
            out = new PrintWriter(client_socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // waits for data and reads it in until connection dies
        // readLine() blocks until the server receives
        // a new line from client
        String s = "";
        try {
            while ((s = in.readLine()) != null) {
                System.out.println(s);
                out.println(s.toUpperCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
