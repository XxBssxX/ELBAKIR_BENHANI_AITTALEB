package Serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SecureFileServer {

    public static void main(String[] args) {
        int port = 5000;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Port invalide, utilisation du port par défaut 5000.");
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SecureFileServer en écoute sur le port " + port + "...");

            // Boucle principale : accepter les clients et déléguer à un thread
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouveau client connecté depuis " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                ClientTransferHandler handler = new ClientTransferHandler(clientSocket);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

