package Serveur;

import Securite.CryptoUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ClientTransferHandler implements Runnable {

    private static final Map<String, String> USERS = new HashMap<>();

    static {
        // Identifiants en dur pour le TP
        USERS.put("Bassam", "password1");
        USERS.put("Benhani", "password2");
    }

    private final Socket clientSocket;

    public ClientTransferHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            // Phase 1 : Authentification
            String login = in.readUTF();
            String password = in.readUTF();
            System.out.println("Tentative de connexion : login=" + login);

            if (!authenticate(login, password)) {
                System.out.println("Authentification échouée pour l'utilisateur " + login);
                out.writeUTF("AUTH_FAIL");
                out.flush();
                return;
            }

            out.writeUTF("AUTH_OK");
            out.flush();
            System.out.println("Authentification réussie pour l'utilisateur " + login);

            // Phase 2 : Négociation (métadonnées du fichier)
            String fileName = in.readUTF();
            long originalSize = in.readLong();
            String expectedHash = in.readUTF();

            System.out.println("Négociation fichier : " + fileName + " (" + originalSize + " octets)");
            System.out.println("Hash attendu (SHA-256) : " + expectedHash);

            out.writeUTF("READY_FOR_TRANSFER");
            out.flush();

            // Phase 3 : Réception / déchiffrement / vérification
            int encryptedSize = in.readInt();
            if (encryptedSize <= 0) {
                System.out.println("Taille de données chiffrées invalide : " + encryptedSize);
                out.writeUTF("TRANSFER_FAIL");
                out.flush();
                return;
            }

            byte[] encryptedData = new byte[encryptedSize];
            in.readFully(encryptedData);

            System.out.println("Données chiffrées reçues : " + encryptedSize + " octets");

            byte[] decryptedData;
            try {
                decryptedData = CryptoUtils.decryptAES(encryptedData);
            } catch (Exception e) {
                System.err.println("Erreur de déchiffrement AES : " + e.getMessage());
                out.writeUTF("TRANSFER_FAIL");
                out.flush();
                return;
            }

            // Sauvegarde du fichier déchiffré
            File receivedDir = new File("received");
            if (!receivedDir.exists()) {
                boolean created = receivedDir.mkdirs();
                if (!created) {
                    System.err.println("Impossible de créer le dossier 'received'.");
                }
            }

            File outputFile = new File(receivedDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(decryptedData);
            }

            System.out.println("Fichier sauvegardé sous : " + outputFile.getAbsolutePath());

            // Vérification d'intégrité (SHA-256)
            String receivedHash;
            try {
                byte[] fileBytes = Files.readAllBytes(outputFile.toPath());
                receivedHash = CryptoUtils.sha256Hex(fileBytes);
            } catch (Exception e) {
                System.err.println("Erreur lors du calcul du hash du fichier reçu : " + e.getMessage());
                out.writeUTF("TRANSFER_FAIL");
                out.flush();
                return;
            }

            System.out.println("Hash reçu (SHA-256) : " + receivedHash);

            if (receivedHash.equalsIgnoreCase(expectedHash)) {
                System.out.println("Intégrité vérifiée : TRANSFER_SUCCESS");
                out.writeUTF("TRANSFER_SUCCESS");
            } else {
                System.out.println("Intégrité NON vérifiée : TRANSFER_FAIL");
                out.writeUTF("TRANSFER_FAIL");
            }
            out.flush();

        } catch (IOException e) {
            System.err.println("Erreur de communication avec le client : " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean authenticate(String login, String password) {
        String expected = USERS.get(login);
        return expected != null && expected.equals(password);
    }
}

