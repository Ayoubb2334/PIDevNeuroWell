# 🧠 NeuroWell — Plateforme de Santé Mentale

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=openjfx)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**Application de gestion de santé mentale avec interface Back-Office et Front-Office**

</div>

---

## 📋 Table des matières

- [Présentation](#-présentation)
- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Base de données](#-base-de-données)
- [Lancement](#-lancement)
- [Modules](#-modules)
- [Technologies](#-technologies)
- [Équipe](#-équipe)

---

## 🌟 Présentation

**NeuroWell** est une application desktop JavaFX de gestion de santé mentale. Elle permet à des administrateurs, psychologues et coachs de gérer des événements, consultations, évaluations et ressources, tandis que les utilisateurs peuvent s'inscrire, participer aux événements et effectuer des paiements en ligne.

```
┌─────────────────────────────────────────────┐
│              NEUROWELL                      │
│                                             │
│  Back-Office  ←→  Base de données  ←→  Front-Office  │
│  (Admin/Psy/Coach)    (MySQL)       (Utilisateurs)    │
└─────────────────────────────────────────────┘
```

---

## ✨ Fonctionnalités

### 🔐 Authentification
- Connexion multi-rôles : **Admin**, **Psychologue**, **Coach**, **Utilisateur**
- Gestion de session avec `SessionManager`
- Déconnexion avec redirection vers la page de login

### 🗓 Événements
- Création, modification et suppression d'événements
- Validation/rejet d'événements par les admins
- Filtres par type, prix, date et mot-clé
- Affichage des événements valides en Front-Office

### 👥 Participations
- Inscription aux événements (réservé aux utilisateurs)
- Choix du mode : **présentiel** ou **distanciel**
- Vérification de la capacité maximale
- Protection par rôle (psy/coach/admin ne peuvent pas participer)

### 💳 Paiements
- Paiement en ligne via **Stripe**
- Génération de factures PDF
- Envoi automatique par email (Gmail SMTP)
- Scan de carte bancaire par IA (Claude Vision + Tesseract OCR)
- Tableau de bord des statistiques de paiement

### 🧠 Consultations
- Gestion des demandes de consultation
- Suivi des statuts : `planifiee`, `en_cours`, `terminee`, `annulee`
- Comptes rendus

### 📊 Évaluations
- Création et affichage d'évaluations psychologiques

### 📚 Ressources
- Ajout et consultation de ressources documentaires

### 🏖 Congés
- Gestion des demandes de congés
- Réponse admin avec tableau récapitulatif

### 👤 Utilisateurs
- Administration des comptes utilisateurs

---

## 🏗 Architecture

```
src/
├── main/
│   ├── java/
│   │   ├── controllers/          # Contrôleurs JavaFX (FXML)
│   │   │   ├── DashboardController.java
│   │   │   ├── LoginController.java
│   │   │   ├── ShowEventController.java
│   │   │   ├── PaymentController.java
│   │   │   ├── CardScannerDialog.java
│   │   │   └── ...
│   │   ├── entities/             # Modèles de données
│   │   │   ├── Evenement.java
│   │   │   ├── Participation.java
│   │   │   ├── Paiement.java
│   │   │   ├── Facture.java
│   │   │   └── ...
│   │   ├── services/             # Logique métier & accès BDD
│   │   │   ├── ServiceEvenement.java
│   │   │   ├── ServiceParticipation.java
│   │   │   ├── PaiementService.java
│   │   │   ├── MailService.java
│   │   │   ├── FactureService.java
│   │   │   ├── CardScannerService.java
│   │   │   ├── AuthService.java
│   │   │   └── SessionManager.java
│   │   └── utils/
│   │       └── MyDatabase.java   # Connexion MySQL singleton
│   └── resources/
│       ├── views/                # Fichiers FXML
│       │   ├── Dashboard.fxml
│       │   ├── Login.fxml
│       │   ├── front.fxml
│       │   └── ...
│       ├── style/                # Fichiers CSS
│       │   └── dashboard.css
│       └── images/               # Ressources visuelles
```

---

## ⚙️ Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java JDK | 17+ |
| JavaFX | 21.0.2 |
| MySQL | 8.0+ |
| Maven | 3.6+ |
| Tesseract OCR | 5.x (optionnel, pour le scan de carte) |

---

## 🚀 Installation

### 1. Cloner le projet

```bash
git clone https://github.com/votre-username/neurowell.git
cd neurowell
```

### 2. Installer les dépendances Maven

```bash
mvn clean install
```

### 3. Configurer la base de données

Importer le script SQL (voir section [Base de données](#-base-de-données)).

### 4. Configurer la connexion BDD

Dans `src/main/java/utils/MyDatabase.java` :

```java
private static final String URL      = "jdbc:mysql://localhost:3306/projet";
private static final String USER     = "root";
private static final String PASSWORD = "votre_mot_de_passe";
```

---

## 🔧 Configuration

### Gmail SMTP (envoi d'emails)

Dans `services/MailService.java` :

```java
private static final String EMAIL_FROM     = "votre_email@gmail.com";
private static final String EMAIL_PASSWORD = "votre_app_password_16_chars";
```

> ⚠️ Utiliser un **App Password** Gmail (pas le mot de passe principal).  
> Activer la validation en 2 étapes → Sécurité → Mots de passe des applications.

### Stripe (paiements)

Dans `services/PaiementService.java` ou `PaymentController.java` :

```java
Stripe.apiKey = "sk_test_votre_cle_stripe";
```

### Tesseract OCR (scan carte bancaire)

```java
tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
// ou chemin relatif :
tesseract.setDatapath("src/main/resources/tessdata");
```

---

## 🗄 Base de données

### Schéma principal

```sql
-- Créer la base
CREATE DATABASE IF NOT EXISTS projet;
USE projet;

-- Table utilisateurs
CREATE TABLE users (
    id_user       INT AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(50)  NOT NULL,
    prenom        VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    telephone     VARCHAR(20),
    role          VARCHAR(20)  DEFAULT 'user'
);

-- Table événements
CREATE TABLE evenements (
    id_e          INT AUTO_INCREMENT PRIMARY KEY,
    titre_e       VARCHAR(150) NOT NULL,
    description_e TEXT,
    date_e        DATETIME     NOT NULL,
    localisation_e VARCHAR(200),
    type_e        VARCHAR(50),
    capacitemax_e INT          DEFAULT 50,
    prix_e        VARCHAR(20),
    statut_e      VARCHAR(30)  DEFAULT 'En attente',
    image         VARCHAR(255)
);

-- Table participation
CREATE TABLE participation (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    id_user       INT NOT NULL,
    id_e          INT NOT NULL,
    modeparticipation VARCHAR(20) NOT NULL,
    objectif      TEXT,
    FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_e)    REFERENCES evenements(id_e) ON DELETE CASCADE
);

-- Table paiement
CREATE TABLE paiement (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    reference     VARCHAR(50)  NOT NULL,
    montant       DOUBLE       NOT NULL,
    mode_paiement VARCHAR(30)  NOT NULL,
    statut        VARCHAR(20)  DEFAULT 'EN_ATTENTE',
    date_paiement DATETIME     DEFAULT CURRENT_TIMESTAMP,
    id_user       INT,
    id_evenement  INT,
    FOREIGN KEY (id_user)      REFERENCES users(id_user)      ON DELETE SET NULL,
    FOREIGN KEY (id_evenement) REFERENCES evenements(id_e)    ON DELETE SET NULL
);
```

---

## ▶️ Lancement

### Via Maven

```bash
mvn javafx:run
```

### Via IntelliJ IDEA

```
Run → Edit Configurations → Maven → Goals: javafx:run
```

---

## 📦 Modules

| Module | Description | Statut |
|--------|-------------|--------|
| Authentification | Login multi-rôles + session | ✅ Complet |
| Dashboard Admin | KPIs + navigation + animations | ✅ Complet |
| Événements | CRUD + validation + filtres | ✅ Complet |
| Participations | Inscription + contrôle rôle/capacité | ✅ Complet |
| Paiements | Stripe + facture PDF + email | ✅ Complet |
| Scan carte IA | Webcam + Claude Vision + OCR | ✅ Complet |
| Consultations | Gestion statuts + comptes rendus | ✅ Complet |
| Évaluations | Création + affichage | ✅ Complet |
| Ressources | Ajout + consultation | ✅ Complet |
| Congés | Demandes + réponses admin | ✅ Complet |
| Utilisateurs | Administration comptes | ✅ Complet |
| Maps | Localisation événements | ✅ Complet |

---

## 🛠 Technologies

| Technologie | Usage |
|-------------|-------|
| **JavaFX 21** | Interface graphique |
| **MySQL 8** | Base de données |
| **JDBC** | Connexion base de données |
| **Stripe Java SDK** | Paiements en ligne |
| **iText PDF** | Génération de factures |
| **JavaMail (javax.mail)** | Envoi d'emails SMTP |
| **Tess4J** | OCR Tesseract (lecture carte) |
| **webcam-capture** | Flux vidéo webcam |
| **Claude Vision API** | Analyse IA des images de carte |
| **Maven** | Gestion des dépendances |

---

## 👨‍💻 Équipe

| Nom | Module |
|-----|--------|
| **Khalil Felhi** | Paiements, Scan IA, Emails |
| Membre 2 | Événements, Participations |
| Membre 3 | Consultations, Congés |
| Membre 4 | Évaluations, Ressources |
| Membre 5 | Authentification, Utilisateurs |

---

## 📄 Licence

Ce projet est développé dans le cadre d'un projet académique.  
© 2026 NeuroWell — Tous droits réservés.

---

<div align="center">
  <sub>Fait avec ❤️ par l'équipe NeuroWell</sub>
</div>
