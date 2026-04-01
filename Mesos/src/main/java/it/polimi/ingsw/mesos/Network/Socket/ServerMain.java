package it.polimi.ingsw.mesos.Network.Socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    static int portNumber = 1234;


    public static void main(String[] args) {

        System.out.println("Starting Server");
        ServerSocket server_socket = null;
        try {
            server_socket = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Listening on port " + portNumber);

        //accepting loop
        while (true) {
            Socket client_socket = null;
            try {
                client_socket = server_socket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Accepted connection from " + client_socket.getInetAddress().getHostName());

            ClientHandler client_handler = new ClientHandler(client_socket);
            Thread thread = new Thread(client_handler);
            thread.start();
        }

    }
}
