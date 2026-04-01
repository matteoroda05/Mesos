package it.polimi.ingsw.mesos.Network.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {
    public static void main(String[] args) {
        
        String host_name = "127.0.0.1";
        int port_number = 1234;
        Socket echo_socket = null;
        
        try {
            echo_socket = new Socket(host_name, port_number);
        }
        catch (IOException e) {
            System.err.println(e.toString() + " " + host_name);
            System.exit(1);
        }
        
        PrintWriter out = null;
        BufferedReader in = null;
        BufferedReader std_in = null;
        
        try {
            out = new PrintWriter(echo_socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echo_socket.getInputStream()));
            std_in = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            System.err.println(e.toString() + " " + host_name);
            System.exit(1);
        }
        String user_input = "";
        while (true) {
            try {
                if ((user_input = std_in.readLine()) == null) break;
                out.println(user_input);
                System.out.println("echo: " + in.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}