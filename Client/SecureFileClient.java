package Client;

import Securite.CryptoUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class SecureFileClient {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("Adresse IP du serveur (ex: 127.0.0.1) : ");
            String serverIp = scanner.nextLine().trim();

            System.out.print("Port du serveur (ex: 5000) : ");
            int port = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Login : ");
            String login = scanner.nextLine().trim();

            System.out.print("Mot de passe : ");
            String password = scanner.nextLine().trim();

            System.out.print("Chemin du fichier à envoyer : ");
            String filePath = scanner.nextLine().trim();

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("Fichier introuvable : " + filePath);
                return;
            }

            byte[] fileBytes = readAllBytes(filePath);
            long originalSize = fileBytes.length;

            // Pré-traitement : hash + chiffrement
            String fileHash = CryptoUtils.sha256Hex(fileBytes);
            byte[] encryptedData = CryptoUtils.encryptAES(fileBytes);

            System.out.println("Taille fichier original : " + originalSize + " octets");
            System.out.println("Hash SHA-256 : " + fileHash);
            System.out.println("Taille données chiffrées : " + encryptedData.length + " octets");

            try (Socket socket = new Socket(serverIp, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                // Phase 1 : Authentification
                out.writeUTF(login);
                out.writeUTF(password);
                out.flush();

                String authResponse = in.readUTF();
                System.out.println("Réponse authentification serveur : " + authResponse);
                if (!"AUTH_OK".equals(authResponse)) {
                    System.out.println("Authentification refusée. Fin.");
                    return;
                }

                // Phase 2 : Négociation
                out.writeUTF(file.getName());
                out.writeLong(originalSize);
                out.writeUTF(fileHash);
                out.flush();

                String negoResponse = in.readUTF();
                System.out.println("Réponse négociation serveur : " + negoResponse);
                if (!"READY_FOR_TRANSFER".equals(negoResponse)) {
                    System.out.println("Serveur non prêt pour le transfert. Fin.");
                    return;
                }

                // Phase 3 : Envoi des données chiffrées
                out.writeInt(encryptedData.length);
                out.write(encryptedData);
                out.flush();
                System.out.println("Données chiffrées envoyées au serveur.");

                String transferResult = in.readUTF();
                System.out.println("Résultat transfert serveur : " + transferResult);

            }

        } catch (Exception e) {
            System.err.println("Erreur côté client : " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static byte[] readAllBytes(String path) throws IOException {
        // Utiliser Files.readAllBytes si disponible, sinon fallback avec FileInputStream
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            File file = new File(path);
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int totalRead = 0;
                while (totalRead < data.length) {
                    int read = fis.read(data, totalRead, data.length - totalRead);
                    if (read < 0) {
                        break;
                    }
                    totalRead += read;
                }
            }
            return data;
        }
    }
}

