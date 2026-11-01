# LABO — GSI & ROM Engineering Platform

[![Build LABO Android APK](https://github.com/manankasinadan/LABO/actions/workflows/build.yml/badge.svg)](https://github.com/manankasinadan/LABO/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Jetpack%20Compose-green)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **LABO** est une plateforme avancée d'ingénierie Android conçue pour analyser, décompresser, modifier et reconstituer des images système Android (GSI, partitions ext4, EROFS, images Sparse et Raw), tout en préservant l'intégrité de démarrage (SELinux contextes, `fs_config`, AVB/VBMeta, chaînes de signature cryptographique).

---

## 📖 Sommaire

- [Présentation](#-présentation)
- [Architecture Fondamentale (11 Moteurs)](#-architecture-fondamentale-11-moteurs)
- [Écosystème LABX](#-écosystème-labx)
- [Fonctionnalités Principales](#-fonctionnalités-principales)
  - [FORGE (Unpack & Repack)](#1-forge)
  - [GESTIONNAIRE DE FICHIERS](#2-gestionnaire-de-fichiers)
  - [SIGNALAB & ROM SIGNER](#3-signalab--rom-signer)
  - [CONSOLE TERMINAL UNIVERSELLE](#4-console-terminal-universelle)
- [Documentation & Bible LABX](#-documentation--bible-labx)
- [Compilation & CI/CD](#-compilation--cicd)

---

## 🌟 Présentation

LABO résout le défi majeur de la modification de GSI (Generic System Image) : permettre la personnalisation profonde d'une partition système (ajouts, modifications d'APKs, suppressions, adaptations de scripts) tout en garantissant que l'image reconstruite respecte la structure, les permissions POSIX, les contextes SELinux et la chaîne de confiance requise pour démarrer sans bootloop.

L'application sépare physiquement :
1. **L'espace de travail isolé** (`workspace/projects/{id}`) pour zéro risque d'échec d'analyse ou d'écriture.
2. **L'espace d'exportation standard** (`/sdcard/LABO/REPACKED` et `/sdcard/LABO/ROMSIGN`).

---

## ⚙ Architecture Fondamentale (11 Moteurs)

1. **CORE ENGINE** : Cœur de coordination technique, ordonnancement des tâches asynchrones, bus d'événements et cycle de vie.
2. **FILE ENGINE** : Manipulation de flux volumineux, calcul d'empreintes (SHA-256, SHA-1, MD5), surveillance d'arborescences et attributs étendus.
3. **PROJECT ENGINE** : Cycle de vie des projets, persistance d'états et base de données relationnelle locale (`database/project.db`).
4. **ANALYST ENGINE** : Détection des signatures magiques (Sparse, ext4, EROFS), extraction des descripteurs de partitions et inspection de `build.prop`.
5. **FORGE IMG ENGINE** : Déballage (Unpack) avec capture des configurations de régénération (`fs_config`, `file_contexts`, `sparse_info.json`), et Reconstitution (Repack) intelligente avec préservation du statut bootable.
6. **FILESYSTEM ENGINE** : Gestion fine d'ext4 et EROFS, calcul des blocs, gestion des symlinks et application des règles de contextes.
7. **PARTITION ENGINE** : Interprétation des tables GPT et dimensionnement des sous-partitions dynamiques.
8. **ARCHIVE ENGINE** : Gestion des conteneurs ZIP, TAR, GZ et XZ.
9. **SIGNER ENGINE** : Analyse forensique V1/V2/V3/V4, générateur de clés, modes **CloneSign** et **Custom**, audit des chaînes AVB/VBMeta.
10. **ARTIFACT ENGINE** : Analyse et régénération sélective des artefacts ART (`.odex`, `.vdex`, `.art`).
11. **MAPPING ENGINE** : Cartographie des dépendances inter-partitions, shared user IDs et analyse d'impact avant recompilation.

---

## 🧩 Écosystème LABX

LABX transforme LABO en une plateforme hautement extensible grâce à :
- **LABX Engine Installer** : Validation cryptographique, vérification des manifestes TOML, installation et cycle de vie des extensions.
- **LABX Framework** : API ouverte permettant aux extensions d'appeler les moteurs natifs de LABO, d'exposer leurs propres moteurs et d'injecter des interfaces personnalisées.

---

## 🚀 Fonctionnalités Principales

### 1. FORGE
- **Unpack** : Sélection de l'image (raw, sparse, ext4, erofs) -> extraction sécurisée -> création automatique d'un projet avec capture intégrale de `config/fs_config` et `config/file_contexts`.
- **Repack** : Sélection du projet -> détection des modifications -> régénération des métadonnées -> recompilation de l'image prête au flash dans `/sdcard/LABO/REPACKED/`.

### 2. GESTIONNAIRE DE FICHIERS
- **Onglet Projet** : Navigation dans l'arborescence du projet avec indicateurs de statut diff (modifiés, ajoutés, supprimés marqués d'une croix rouge).
- **Onglet Stockage** : Explorateur `/sdcard` avec sélecteur modal intégré utilisable dans tous les écrans.

### 3. SIGNALAB & ROM SIGNER
- **VerifyAPP** : Rapport forensique complet sur n'importe quel APK (schémas V1 à V4, certificats X.509, DN, validité, clés publiques).
- **Keymaker** : Création de clés et profils cryptographiques (**CloneSign** pour répliquer la signature d'un APK existant ou **Custom**).
- **SignAPP** : Re-signature d'applications avec alignement zipalign.
- **ROMsignchain** : Analyse systémique globale de tous les APKs d'une GSI déballée, hachage complet et export de cartographies croisées en JSON.
- **ROM SIGNER** : Pipeline complet autonome (Unpack -> Analyse de chaîne de confiance -> Re-signature globale -> Mise à jour des permissions et métadonnées -> Repack -> Export `/sdcard/LABO/ROMSIGN/`).

### 4. CONSOLE TERMINAL UNIVERSELLE
- Console interactive disponible en incrustation sur toutes les fonctionnalités (modes minimisé, demi-écran et plein écran).
- Streaming temps réel des journaux de tous les moteurs, saisie de commandes interactives et diagnostic technique.

---

## 📚 Documentation & Bible LABX

La documentation technique complète décrivant l'architecture détaillée, le guide pas-à-pas de création d'extensions LABX, la spécification du format `manifest.toml` et les recettes de compatibilité GSI est disponible directement dans l'application (**Paramètres > Documentation & Bible LABX**) et dans le fichier du dépôt :

👉 **[Consulter la Bible Complète de Développement LABX (docs/LABX_BIBLE.md)](docs/LABX_BIBLE.md)**

---

## 🛠 Compilation & CI/CD

Le projet est configuré pour une compilation automatisée via GitHub Actions :
- **Java 17** (Temurin)
- **Gradle & Jetpack Compose**
- Exécution des tests unitaires et génération de l'APK `app-debug.apk` à chaque commit ou Pull Request.

Pour compiler en local :
```bash
gradle :app:assembleDebug
```
L'APK généré se trouvera dans `app/build/outputs/apk/debug/app-debug.apk`.
