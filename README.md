## Système de transfert de fichiers sécurisé (TCP + AES + SHA-256)

Réalisé par:
-ELBAKIR Bassam
-BENHANI Mohammed
-AIT TALEB Marouane

Ce projet implémente une application **Client / Serveur** pour le **transfert de fichiers sécurisé** en Java, en utilisant :

- **TCP** pour la communication réseau
- **AES** (via `javax.crypto`) pour le chiffrement des fichiers
- **SHA-256** (via `java.security.MessageDigest`) pour l’intégrité
- Un **protocole en 3 phases** : Authentification → Négociation → Transfert
- Un serveur **multi-threads** (un thread par client)

---

## 1. Structure du projet

Le code est organisé en trois packages :

- **`Serveur/`**
  - **`SecureFileServer.java`** : lance le serveur TCP, écoute sur un port (par défaut `5000`), accepte les connexions et crée un thread `ClientTransferHandler` par client.
  - **`ClientTransferHandler.java`** : logique de traitement d’un client (authentification, négociation, réception, déchiffrement, vérification d’intégrité, sauvegarde).

- **`Client/`**
  - **`SecureFileClient.java`** : client en ligne de commande. Demande les paramètres à l’utilisateur, prépare le fichier (hash + chiffrement) puis suit le protocole en 3 phases avec le serveur.

- **`Securite/`**
  - **`CryptoUtils.java`** : fonctions utilitaires pour :
    - **`encryptAES(byte[] data)`** : chiffrement AES (`AES/ECB/PKCS5Padding`)
    - **`decryptAES(byte[] encryptedData)`** : déchiffrement AES
    - **`sha256Hex(byte[] data)`** : calcul de hash SHA-256 en hexadécimal
    - Clé **AES** partagée en dur (clé de session simple, uniquement pour le TP)

- **Dossier `received/`** (créé à l’exécution côté serveur)
  - Contient les fichiers **reçus et déchiffrés** par le serveur.

---

## 2. Protocole de communication (3 phases)

Le client et le serveur communiquent via `DataInputStream` / `DataOutputStream` au-dessus de TCP.

### 2.1 Phase 1 : Authentification

1. Le **client** envoie au serveur (UTF) :
   - `login`
   - `password`
2. Le **serveur** vérifie les identifiants dans une `Map` interne, par exemple :
   - `Bassam` → `password1`
   - `Benhani` → `password2`
3. Le serveur répond :
   - `"AUTH_OK"` si les identifiants sont valides
   - `"AUTH_FAIL"` sinon, puis ferme la connexion

### 2.2 Phase 2 : Négociation

Si l’authentification est acceptée :

1. Le **client** envoie :
   - **Nom du fichier** (ex : `document.pdf`)
   - **Taille du fichier original** en octets
   - **Hash SHA-256** du fichier original (en hexadécimal)
2. Le **serveur** répond :
   - `"READY_FOR_TRANSFER"` s’il est prêt à recevoir les données chiffrées

### 2.3 Phase 3 : Transfert sécurisé et vérification

1. Le **client** :
   - Chiffre les octets du fichier avec **AES/ECB/PKCS5Padding** (clé partagée)
   - Envoie :
     - La **taille** des données chiffrées (en octets)
     - Le **tableau d’octets chiffrés**
2. Le **serveur** :
   - Reçoit les données chiffrées
   - Les **déchiffre** avec la même clé AES
   - Sauvegarde le fichier déchiffré dans le dossier `received/`
   - Recalcule le **SHA-256** du contenu reçu
   - Compare avec le hash attendu envoyé pendant la négociation
   - Renvoie au client :
     - `"TRANSFER_SUCCESS"` si les hash sont égaux
     - `"TRANSFER_FAIL"` sinon

---

## 3. Sécurité : chiffrement et intégrité

### 3.1 Chiffrement / Déchiffrement (AES)

- **Algorithme** : AES (Advanced Encryption Standard)
- **Mode** : `AES/ECB/PKCS5Padding` (simple à implémenter pour un TP)
- **Implémentation** : via `javax.crypto.Cipher` et `SecretKeySpec`
- **Clé** : tableau de 16 octets (AES‑128) **partagée** entre client et serveur, codée en dur dans `CryptoUtils`

> ⚠️ Pour un système réel, la clé ne devrait pas être codée en dur mais **échangée dynamiquement** (RSA, Diffie‑Hellman, TLS, etc.).

### 3.2 Intégrité (SHA-256)

- Utilisation de `MessageDigest.getInstance("SHA-256")`
- Côté client : le hash SHA‑256 est calculé sur le **fichier original** avant chiffrement
- Côté serveur : le hash SHA‑256 est recalculé sur les **données déchiffrées**
- Si les deux valeurs sont identiques → l’intégrité du fichier est garantie

---

## 4. Compilation et exécution

Assure-toi d’être à la racine du projet (là où se trouvent les dossiers `Client`, `Serveur`, `Securite`).

### 4.1 Compilation

```bash
javac Securite/*.java Serveur/*.java Client/*.java
```

### 4.2 Lancer le serveur

Port par défaut : **5000**

```bash
java Serveur.SecureFileServer
```

Ou avec un port spécifique (ex : 6000) :

```bash
java Serveur.SecureFileServer 6000
```

Tu devrais voir :

```text
SecureFileServer en écoute sur le port 5000...
```

### 4.3 Lancer le client (console)

Dans un autre terminal, depuis la racine du projet :

```bash
java Client.SecureFileClient
```

Le programme demande alors :

- **Adresse IP du serveur** (ex : `127.0.0.1`)
- **Port** (ex : `5000`)
- **Login** (ex : `Bassam`)
- **Mot de passe** (ex : `password1`)
- **Chemin du fichier à envoyer** (chemin complet vers un fichier existant)

Le fichier reçu en clair apparaîtra dans le dossier :

```text
received/<nom_du_fichier>
```

---

## 5. Idées d’améliorations

- **Échange de clé sécurisé** :
  - Utiliser RSA ou Diffie‑Hellman pour échanger une clé AES de session
  - Ou utiliser directement **TLS** (comme HTTPS) au lieu de gérer la crypto à la main
- **Mode de chiffrement plus sûr** :
  - Remplacer `AES/ECB/PKCS5Padding` par `AES/CBC/PKCS5Padding` avec un IV aléatoire
- **Gestion des utilisateurs** :
  - Charger les logins / mots de passe depuis un fichier chiffré ou une base de données
  - Hacher les mots de passe (bcrypt, PBKDF2, etc.) au lieu de les stocker en clair

Ce projet illustre :

- **Le fonctionnement d’un serveur TCP multi-threads**
- **Un protocole de session en plusieurs phases**
- **L’utilisation de l’API de sécurité Java (`javax.crypto`, `MessageDigest`)**
- **Les problématiques réseau de base** (IP, ports, clients distants)

